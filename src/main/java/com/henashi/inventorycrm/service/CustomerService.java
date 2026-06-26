package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.CustomerBatchStatusUpdateDTO;
import com.henashi.inventorycrm.dto.CustomerBatchStatusUpdateResultDTO;
import com.henashi.inventorycrm.dto.CustomerCreateDTO;
import com.henashi.inventorycrm.dto.CustomerDTO;
import com.henashi.inventorycrm.dto.CustomerSearchOptionDTO;
import com.henashi.inventorycrm.dto.CustomerStatisticsDTO;
import com.henashi.inventorycrm.dto.CustomerUpdateDTO;
import com.henashi.inventorycrm.event.ReferrerGiftEvent;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.exception.CustomerException;
import com.henashi.inventorycrm.mapper.CustomerMapper;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.utils.CsvUtils;
import jakarta.persistence.criteria.Predicate;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "customers", cacheManager = "shortCache")
public class CustomerService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 50;

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
            builder.append(CsvUtils.csv(customer.getId()))
                    .append(',').append(CsvUtils.csv(customer.getName()))
                    .append(',').append(CsvUtils.csv(customer.getPhone()))
                    .append(',').append(CsvUtils.csv(customer.getEmail()))
                    .append(',').append(CsvUtils.csv(formatGender(customer.getGender())))
                    .append(',').append(CsvUtils.csv(formatGiftLevel(customer.getGiftLevel())))
                    .append(',').append(CsvUtils.csv(formatCustomerStatus(customer.getStatus())))
                    .append(',').append(CsvUtils.csv(customer.getReferrer() != null ? customer.getReferrer().getName() : null))
                    .append(',').append(CsvUtils.csv(formatDate(customer.getRegisteredAt())))
                    .append(',').append(CsvUtils.csv(formatDate(customer.getBirthday())))
                    .append(',').append(CsvUtils.csv(customer.getAddress()))
                    .append(',').append(CsvUtils.csv(customer.getRemark()))
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

    private int normalizeSearchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }
        return Math.min(limit, MAX_SEARCH_LIMIT);
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
