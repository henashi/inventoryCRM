package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.constants.RegexPatterns;
import com.henashi.inventorycrm.dto.CustomerCreateDTO;
import com.henashi.inventorycrm.dto.ImportFailureDetailDTO;
import com.henashi.inventorycrm.dto.ImportResultDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.CustomerMapper;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.utils.CsvUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerDataExchangeService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern PHONE_PATTERN = Pattern.compile(RegexPatterns.PHONE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(RegexPatterns.EMAIL);
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

    private final Logger log = LoggerFactory.getLogger(CustomerDataExchangeService.class);

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final Validator validator;
    private final CustomerService customerService;

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

        List<List<String>> csvRows = CsvUtils.readCsvRows(file, "CUSTOMER_IMPORT_READ_FAILED");
        if (csvRows.isEmpty()) {
            throw new BusinessException("CUSTOMER_IMPORT_EMPTY", "导入文件为空，请上传包含表头的 CSV 文件");
        }

        Map<String, Integer> headerIndex = CsvUtils.buildHeaderIndex(csvRows.get(0), CUSTOMER_IMPORT_REQUIRED_FIELDS, "CUSTOMER_IMPORT_HEADER_INVALID");
        List<ImportFailureDetailDTO> failures = new ArrayList<>();
        Set<String> seenPhones = new LinkedHashSet<>();
        int successCount = 0;

        for (int rowIndex = 1; rowIndex < csvRows.size(); rowIndex++) {
            int rowNumber = rowIndex + 1;
            List<String> row = csvRows.get(rowIndex);
            if (CsvUtils.isBlankRow(row)) {
                continue;
            }

            String phone = CsvUtils.getCell(row, headerIndex, "phone");
            String name = CsvUtils.getCell(row, headerIndex, "name");
            String identifier = !phone.isBlank() ? phone : name;

            try {
                CustomerCreateDTO dto = buildCustomerImportDTO(row, headerIndex, seenPhones);
                customerService.createCustomer(dto);
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

    private CustomerCreateDTO buildCustomerImportDTO(List<String> row, Map<String, Integer> headerIndex, Set<String> seenPhones) {
        String name = CsvUtils.getCell(row, headerIndex, "name");
        String phone = CsvUtils.getCell(row, headerIndex, "phone");
        String email = CsvUtils.getCell(row, headerIndex, "email");
        String address = CsvUtils.getCell(row, headerIndex, "address");
        String remark = CsvUtils.getCell(row, headerIndex, "remark");
        String referrerPhone = CsvUtils.getCell(row, headerIndex, "referrerPhone");

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

        Integer gender = parseOptionalInteger(CsvUtils.getCell(row, headerIndex, "gender"), "性别");
        if (gender != null && gender != 0 && gender != 1) {
            throw new BusinessException("CUSTOMER_IMPORT_GENDER_INVALID", "性别仅支持 0 或 1");
        }

        Integer giftLevel = parseOptionalInteger(CsvUtils.getCell(row, headerIndex, "giftLevel"), "礼品等级");
        if (giftLevel == null) {
            giftLevel = 0;
        }
        if (giftLevel < 0 || giftLevel > 3) {
            throw new BusinessException("CUSTOMER_IMPORT_GIFT_LEVEL_INVALID", "礼品等级仅支持 0-3");
        }

        LocalDate birthday = parseOptionalDate(CsvUtils.getCell(row, headerIndex, "birthday"), "生日");
        LocalDate registeredAt = parseOptionalDate(CsvUtils.getCell(row, headerIndex, "registeredAt"), "注册日期");
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

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
