package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.constants.RegexPatterns;
import com.henashi.inventorycrm.dto.CustomerBatchStatusUpdateDTO;
import com.henashi.inventorycrm.dto.CustomerBatchStatusUpdateResultDTO;
import com.henashi.inventorycrm.dto.CustomerCreateDTO;
import com.henashi.inventorycrm.dto.CustomerDTO;
import com.henashi.inventorycrm.dto.CustomerSearchOptionDTO;
import com.henashi.inventorycrm.dto.CustomerStatisticsDTO;
import com.henashi.inventorycrm.dto.CustomerUpdateDTO;
import com.henashi.inventorycrm.dto.ImportFailureDetailDTO;
import com.henashi.inventorycrm.dto.ImportResultDTO;
import com.henashi.inventorycrm.event.ReferrerGiftEvent;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.exception.CustomerException;
import com.henashi.inventorycrm.mapper.CustomerMapper;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.repository.CustomerRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "customers", cacheManager = "shortCache")
public class CustomerService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern PHONE_PATTERN = Pattern.compile(RegexPatterns.PHONE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(RegexPatterns.EMAIL);
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 50;
    private static final long MAX_IMPORT_FILE_SIZE = 5L * 1024 * 1024;
    private static final List<String> CUSTOMER_IMPORT_TEMPLATE_FIELDS = List.of(
            "name", "phone", "email", "address", "birthday", "gender", "giftLevel", "remark", "referrerPhone", "registeredAt"
    );
    private static final List<String> CUSTOMER_IMPORT_REQUIRED_FIELDS = List.of("name", "phone");
    private static final String CUSTOMER_IMPORT_DUPLICATE_STRATEGY = "按手机号判重，已存在或文件内重复的数据会跳过并记录失败原因";
    private static final List<String> CUSTOMER_IMPORT_NOTES = List.of(
            "仅支持 UTF-8 编码的 CSV 文件",
            "giftLevel 为空时默认导入为 0",
            "status 固定导入为正常(1)"
    );

    private final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Validator validator;

    @Cacheable(key = "#p0", unless = "#result == null")
    public CustomerDTO findCustomerDTOById(Long customerId) {
        log.debug("查询客户详情: id={}", customerId);
        if (customerId == null || customerId <= 0) {
            log.debug("客户ID 无效: id={}", customerId);
            throw new IllegalArgumentException("客户ID 无效");
        }
        return customerRepository.findById(customerId)
                .map(customer -> {
                    log.info("找到客户: {}", customer.getName());
                    return customerMapper.fromEntity(customer);
                })
                .orElseThrow(() -> {
                    log.warn("客户不存在: id={}", customerId);
                    return CustomerException.notFound(customerId);
                });
    }

    @Transactional
    public CustomerDTO createCustomer(CustomerCreateDTO customerCreateDTO) {
        log.debug("创建客户详情: {}", customerCreateDTO);
        validateCustomerData(customerCreateDTO);
        Customer customer;
        if (customerCreateDTO.hasReferrer()) {
            customer = getCustomerWithReferrer(customerCreateDTO);
        }
        else {
            customer = customerMapper.createToEntity(customerCreateDTO);
        }
        customer.setStatus("1");
        return customerMapper.fromEntity(customerRepository.save(customer));
    }

    @CacheEvict(key = "#p0.id")
    public Customer updateEntity(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerSearchOptionDTO> searchCustomers(String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        PageRequest pageRequest = PageRequest.of(
                0,
                normalizeSearchLimit(limit),
                Sort.by(Sort.Direction.DESC, "registeredAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        return customerRepository.findAll(buildSpecification(keyword, null, null, null, null, null), pageRequest)
                .stream()
                .map(customer -> new CustomerSearchOptionDTO(
                        customer.getId(),
                        customer.getName(),
                        customer.getPhone(),
                        customer.getEmail(),
                        customer.getGiftLevel(),
                        resolveCustomerStatus(customer.getStatus())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerStatisticsDTO getStatistics() {
        List<Customer> customers = customerRepository.findAll();
        long totalCustomers = customers.size();
        long normalCustomers = customers.stream()
                .filter(customer -> "1".equals(customer.getStatus()) || customer.getStatus() == null || customer.getStatus().isBlank())
                .count();
        long disabledCustomers = customers.stream()
                .filter(customer -> "0".equals(customer.getStatus()))
                .count();

        Map<Integer, Long> giftLevelDistribution = new LinkedHashMap<>();
        for (int level = 0; level <= 3; level++) {
            final int currentLevel = level;
            giftLevelDistribution.put(currentLevel, customers.stream()
                    .filter(customer -> normalizeGiftLevel(customer.getGiftLevel()) == currentLevel)
                    .count());
        }

        return new CustomerStatisticsDTO(totalCustomers, normalCustomers, disabledCustomers, giftLevelDistribution);
    }

    @Transactional(readOnly = true)
    public ImportResultDTO getImportTemplateMeta() {
        return new ImportResultDTO(
                0,
                0,
                List.of(),
                CUSTOMER_IMPORT_TEMPLATE_FIELDS,
                CUSTOMER_IMPORT_REQUIRED_FIELDS,
                CUSTOMER_IMPORT_DUPLICATE_STRATEGY,
                CUSTOMER_IMPORT_NOTES
        );
    }

    @Transactional
    public ImportResultDTO importCustomers(MultipartFile file) {
        validateImportFile(file);

        List<List<String>> csvRows = readCsvRows(file);
        if (csvRows.isEmpty()) {
            throw new BusinessException("CUSTOMER_IMPORT_EMPTY", "导入文件为空，请上传包含表头的 CSV 文件");
        }

        Map<String, Integer> headerIndex = buildHeaderIndex(csvRows.get(0), CUSTOMER_IMPORT_REQUIRED_FIELDS);
        List<ImportFailureDetailDTO> failures = new ArrayList<>();
        Set<String> seenPhones = new LinkedHashSet<>();
        int successCount = 0;

        for (int rowIndex = 1; rowIndex < csvRows.size(); rowIndex++) {
            int rowNumber = rowIndex + 1;
            List<String> row = csvRows.get(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }

            String phone = getCell(row, headerIndex, "phone");
            String name = getCell(row, headerIndex, "name");
            String identifier = !phone.isBlank() ? phone : name;

            try {
                CustomerCreateDTO dto = buildCustomerImportDTO(row, headerIndex, seenPhones);
                createCustomer(dto);
                seenPhones.add(dto.phone());
                successCount++;
            }
            catch (BusinessException | IllegalArgumentException ex) {
                failures.add(new ImportFailureDetailDTO(rowNumber, identifier, ex.getMessage()));
            }
        }

        return new ImportResultDTO(
                successCount,
                failures.size(),
                failures,
                CUSTOMER_IMPORT_TEMPLATE_FIELDS,
                CUSTOMER_IMPORT_REQUIRED_FIELDS,
                CUSTOMER_IMPORT_DUPLICATE_STRATEGY,
                CUSTOMER_IMPORT_NOTES
        );
    }

    private @NonNull Customer getCustomerWithReferrer(CustomerCreateDTO customerCreateDTO) {
        Customer customer;
        Customer referrer = customerRepository.findById(customerCreateDTO.referrerId())
                .orElseThrow(() -> {
                    log.warn("传入推荐人 ID:{} 但是未查询到对应客户", customerCreateDTO.referrerId());
                    return CustomerException.referrerNotFound(customerCreateDTO.referrerId());
                });

        log.debug("推荐人: {}", referrer.getName());
        customer = customerMapper.createToEntity(customerCreateDTO);
        eventPublisher.publishEvent(new ReferrerGiftEvent(referrer));
        return customer;
    }

    private void validateCustomerData(CustomerCreateDTO dto) {
        if (customerRepository.existsByPhone(dto.phone())) {
            throw CustomerException.alreadyExists(dto.phone());
        }
    }

    @Transactional
    @CacheEvict(key = "#p0")
    public CustomerDTO updateCustomer(Long id, CustomerUpdateDTO dto) {
        if (id == null || id <= 0L) {
            log.warn("更新客户信息异常: {}", dto);
            throw CustomerException.invalidCustomer(dto);
        }
        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("未找到客户: {}", id);
                    return CustomerException.notFound(id);
                });

        log.info("找到客户信息: {}", existingCustomer);
        Customer customer = customerMapper.partialUpdate(dto, existingCustomer);
        Customer saved = customerRepository.save(customer);
        log.info("更新客户成功: {} ({})", saved, id);

        return customerMapper.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(key = "#p0")
    public void deleteById(Long id) {
        if (id == null || id <= 0L) {
            log.warn("客户信息异常: {}", id);
            throw CustomerException.invalidId(id);
        }
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("根据ID未找到客户: {}", id);
                    return CustomerException.notFound(id);
                });

        customerRepository.deleteById(id);
        log.info("逻辑删除客户成功: {}", customer.getName());
    }

    public Page<CustomerDTO> findAllCustomers(PageRequest pageRequest, String keyword, String sort, Integer status, Integer giftLevel, Integer gender, String direction, LocalDate startDate, LocalDate endDate) {
        log.debug("分页查询客户列表: page={}, size={}, keyword={}, sort={}, giftStatus={}, giftLevel={}, gender={}, startDate={}, endDate={}, direction={}",
                pageRequest.getPageNumber(), pageRequest.getPageSize(), keyword, sort, status, giftLevel, gender, startDate, endDate, direction);
        Sort sortObj = buildSort(sort, direction);
        PageRequest sortedPageRequest = PageRequest.of(
                pageRequest.getPageNumber(),
                pageRequest.getPageSize(),
                sortObj
        );
        Specification<Customer> spec = buildSpecification(keyword, status, giftLevel, gender, startDate, endDate);
        return customerRepository.findAllWithReferrer(spec, sortedPageRequest).map(customerMapper::fromEntity);
    }

    @Transactional(readOnly = true)
    public byte[] exportCustomers(String keyword, Integer status, Integer giftLevel, Integer gender, LocalDate startDate, LocalDate endDate) {
        Specification<Customer> spec = buildSpecification(keyword, status, giftLevel, gender, startDate, endDate);
        Sort exportSort = Sort.by(Sort.Direction.DESC, "registeredAt").and(Sort.by(Sort.Direction.DESC, "id"));
        List<Customer> customers = customerRepository.findAll(spec, exportSort);

        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("客户ID,客户姓名,手机号,邮箱,性别,礼品等级,状态,推荐人,注册日期,生日,地址,备注\n");

        for (Customer customer : customers) {
            builder.append(csv(customer.getId()))
                    .append(',').append(csv(customer.getName()))
                    .append(',').append(csv(customer.getPhone()))
                    .append(',').append(csv(customer.getEmail()))
                    .append(',').append(csv(formatGender(customer.getGender())))
                    .append(',').append(csv(formatGiftLevel(customer.getGiftLevel())))
                    .append(',').append(csv(formatCustomerStatus(customer.getStatus())))
                    .append(',').append(csv(customer.getReferrer() != null ? customer.getReferrer().getName() : null))
                    .append(',').append(csv(formatDate(customer.getRegisteredAt())))
                    .append(',').append(csv(formatDate(customer.getBirthday())))
                    .append(',').append(csv(customer.getAddress()))
                    .append(',').append(csv(customer.getRemark()))
                    .append('\n');
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public CustomerBatchStatusUpdateResultDTO batchUpdateStatus(CustomerBatchStatusUpdateDTO request) {
        Set<Long> ids = new LinkedHashSet<>(request.ids());
        if (ids.isEmpty()) {
            throw new BusinessException("CUSTOMER_IDS_EMPTY", "客户ID列表不能为空");
        }
        if (request.status() == null || (request.status() != 0 && request.status() != 1)) {
            throw new BusinessException("CUSTOMER_STATUS_INVALID", "客户状态不合法");
        }

        List<Customer> customers = customerRepository.findAllById(ids);
        if (customers.size() != ids.size()) {
            throw new BusinessException("CUSTOMER_NOT_FOUND", "存在不存在的客户记录");
        }

        String targetStatus = String.valueOf(request.status());
        LocalDateTime now = LocalDateTime.now();
        for (Customer customer : customers) {
            customer.setStatus(targetStatus);
            customer.setStatusUpdatedTime(now);
        }
        customerRepository.saveAll(customers);

        return new CustomerBatchStatusUpdateResultDTO(
                true,
                customers.size(),
                request.status(),
                "批量更新客户状态成功"
        );
    }

    private CustomerCreateDTO buildCustomerImportDTO(List<String> row, Map<String, Integer> headerIndex, Set<String> seenPhones) {
        String name = getCell(row, headerIndex, "name");
        String phone = getCell(row, headerIndex, "phone");
        String email = getCell(row, headerIndex, "email");
        String address = getCell(row, headerIndex, "address");
        String remark = getCell(row, headerIndex, "remark");
        String referrerPhone = getCell(row, headerIndex, "referrerPhone");

        if (name.isBlank()) {
            throw new BusinessException("CUSTOMER_IMPORT_NAME_REQUIRED", "客户姓名不能为空");
        }
        if (phone.isBlank()) {
            throw new BusinessException("CUSTOMER_IMPORT_PHONE_REQUIRED", "手机号不能为空");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("CUSTOMER_IMPORT_PHONE_INVALID", "手机号格式不正确");
        }
        if (!email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("CUSTOMER_IMPORT_EMAIL_INVALID", "邮箱格式不正确");
        }
        if (seenPhones.contains(phone)) {
            throw new BusinessException("CUSTOMER_IMPORT_PHONE_DUPLICATED", "文件内手机号重复，已跳过");
        }
        if (customerRepository.existsByPhone(phone)) {
            throw new BusinessException("CUSTOMER_IMPORT_PHONE_EXISTS", "手机号已存在，已跳过");
        }

        Integer gender = parseOptionalInteger(getCell(row, headerIndex, "gender"), "性别");
        if (gender != null && gender != 0 && gender != 1) {
            throw new BusinessException("CUSTOMER_IMPORT_GENDER_INVALID", "性别仅支持 0 或 1");
        }

        Integer giftLevel = parseOptionalInteger(getCell(row, headerIndex, "giftLevel"), "礼品等级");
        if (giftLevel == null) {
            giftLevel = 0;
        }
        if (giftLevel < 0 || giftLevel > 3) {
            throw new BusinessException("CUSTOMER_IMPORT_GIFT_LEVEL_INVALID", "礼品等级仅支持 0-3");
        }

        LocalDate birthday = parseOptionalDate(getCell(row, headerIndex, "birthday"), "生日");
        LocalDate registeredAt = parseOptionalDate(getCell(row, headerIndex, "registeredAt"), "注册日期");
        Long referrerId = resolveReferrerId(referrerPhone, phone);

        CustomerCreateDTO dto = new CustomerCreateDTO(
                name,
                phone,
                address,
                email,
                1,
                registeredAt,
                referrerId,
                giftLevel,
                null,
                remark,
                1,
                gender,
                birthday
        );

        Set<ConstraintViolation<CustomerCreateDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new BusinessException("CUSTOMER_IMPORT_VALIDATION_FAILED", buildValidationMessage(violations));
        }
        return dto;
    }

    private Long resolveReferrerId(String referrerPhone, String customerPhone) {
        if (referrerPhone == null || referrerPhone.isBlank()) {
            return null;
        }
        if (referrerPhone.equals(customerPhone)) {
            throw new BusinessException("CUSTOMER_IMPORT_REFERRER_INVALID", "推荐人手机号不能与客户手机号相同");
        }
        return customerRepository.findByPhone(referrerPhone)
                .map(Customer::getId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_IMPORT_REFERRER_NOT_FOUND", "推荐人手机号不存在"));
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("CUSTOMER_IMPORT_FILE_EMPTY", "导入文件不能为空");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new BusinessException("CUSTOMER_IMPORT_FILE_TOO_LARGE", "导入文件不能超过 5MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessException("CUSTOMER_IMPORT_FILE_TYPE_INVALID", "仅支持导入 CSV 文件");
        }
    }

    private List<List<String>> readCsvRows(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<List<String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(parseCsvLine(line));
            }
            return rows;
        }
        catch (IOException ex) {
            throw new BusinessException("CUSTOMER_IMPORT_READ_FAILED", "导入文件解析失败，请检查文件编码或内容格式");
        }
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headerRow, List<String> requiredFields) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int columnIndex = 0; columnIndex < headerRow.size(); columnIndex++) {
            String header = normalizeHeader(headerRow.get(columnIndex));
            if (!header.isBlank()) {
                headerIndex.put(header, columnIndex);
            }
        }

        List<String> missingHeaders = requiredFields.stream()
                .filter(requiredField -> !headerIndex.containsKey(requiredField))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new BusinessException("CUSTOMER_IMPORT_HEADER_INVALID", "导入模板缺少必填列: " + String.join(", ", missingHeaders));
        }
        return headerIndex;
    }

    private String getCell(List<String> row, Map<String, Integer> headerIndex, String fieldName) {
        Integer columnIndex = headerIndex.get(fieldName);
        if (columnIndex == null || columnIndex >= row.size()) {
            return "";
        }
        return normalizeText(row.get(columnIndex));
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(value -> value == null || value.trim().isEmpty());
    }

    private Integer parseOptionalInteger(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        }
        catch (NumberFormatException ex) {
            throw new BusinessException("CUSTOMER_IMPORT_NUMBER_INVALID", fieldName + "格式不正确");
        }
    }

    private LocalDate parseOptionalDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        }
        catch (DateTimeParseException ex) {
            throw new BusinessException("CUSTOMER_IMPORT_DATE_INVALID", fieldName + "格式错误，应为 yyyy-MM-dd");
        }
    }

    private String buildValidationMessage(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("；"));
    }

    private int normalizeSearchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }
        return Math.min(limit, MAX_SEARCH_LIMIT);
    }

    private String normalizeHeader(String header) {
        return normalizeText(header).replace("\uFEFF", "").toLowerCase();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            values.add("");
            return values;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                }
                else {
                    inQuotes = !inQuotes;
                }
            }
            else if (currentChar == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            }
            else {
                current.append(currentChar);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static @NonNull Sort buildSort(String sort, String direction) {
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            if (direction != null && !direction.isEmpty()) {
                sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
            }
            else {
                sortObj = Sort.by(Sort.Direction.ASC, sort);
            }
        }
        return sortObj;
    }

    private static @NonNull Specification<Customer> buildSpecification(String keyword, Integer status, Integer giftLevel, Integer gender, LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + kw.toLowerCase() + "%"),
                        cb.like(root.get("phone"), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("email")), "%" + kw.toLowerCase() + "%")
                ));
            }

            if (giftLevel != null) {
                predicates.add(cb.equal(root.get("giftLevel"), giftLevel));
            }

            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), String.valueOf(status)));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("registeredAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("registeredAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public Customer getById(@NotNull @Min(1) Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("客户不存在: id={}", id);
                    return CustomerException.notFound(id);
                });
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    private static String formatGender(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        return gender == 1 ? "男" : "女";
    }

    private static String formatGiftLevel(Integer giftLevel) {
        if (giftLevel == null || giftLevel <= 0) {
            return "普通客户";
        }
        return "等级" + giftLevel;
    }

    private static String formatCustomerStatus(String status) {
        if ("1".equals(status)) {
            return "正常";
        }
        if ("0".equals(status)) {
            return "停用";
        }
        return status == null ? "" : status;
    }

    private static Integer resolveCustomerStatus(String status) {
        if (status == null || status.isBlank()) {
            return 1;
        }
        return "0".equals(status.trim()) ? 0 : 1;
    }

    private static int normalizeGiftLevel(Integer giftLevel) {
        return giftLevel == null ? 0 : giftLevel;
    }
}
