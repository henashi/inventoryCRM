package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.CustomerUpdateDTO;
import com.henashi.inventorycrm.event.ReferrerGiftEvent;
import com.henashi.inventorycrm.exception.CustomerException;
import com.henashi.inventorycrm.mapper.CustomerMapper;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.dto.CustomerCreateDTO;
import com.henashi.inventorycrm.dto.CustomerDTO;
import com.henashi.inventorycrm.repository.CustomerRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
//    private final GiftLogRepository giftLogRepository;
//    private final DataDictService dataDictService;
//    private final GiftService giftService;
    private final CustomerMapper customerMapper;
    private final ApplicationEventPublisher eventPublisher;

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
    public CustomerDTO saveCustomer(CustomerCreateDTO customerCreateDTO) {
        log.debug("创建客户详情: {}", customerCreateDTO);
        validateCustomerData(customerCreateDTO);
        Customer customer;
        if (customerCreateDTO.hasReferrer()) {
            customer = getCustomerWithReferrer(customerCreateDTO);
        }
        else {
            customer = customerMapper.createToEntity(customerCreateDTO);
//                    customerCreateDTO.toEntity(customerRepository);
        }
        customer.setStatus("1");
        return customerMapper.fromEntity(customerRepository.save(customer));
    }

    private @NonNull Customer getCustomerWithReferrer(CustomerCreateDTO customerCreateDTO) {
        Customer customer;
        // 查找推荐人
        Customer referrer = customerRepository.findById(customerCreateDTO.referrerId())
                .orElseThrow(() -> {
                    log.warn("传入推荐人 ID:{} 但是未查询到对应客户", customerCreateDTO.referrerId());
                    return CustomerException.referrerNotFound(customerCreateDTO.referrerId());          });

        // 创建带推荐人的客户
        log.debug("推荐人: {}", referrer.getName());
        customer = customerMapper.createToEntity(customerCreateDTO);
//                customerCreateDTO.toEntityWithReferrer(customerRepository, referrer);
        eventPublisher.publishEvent(new ReferrerGiftEvent(referrer));
//        handleReferrerGiftStatus(referrer);
        return customer;
    }

//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void handleReferrerGiftStatus(Customer referrer) {
//        if (referrer.getGiftLevel() == null || referrer.getGiftLevel() < 3) {
//            int giftLevel = referrer.getGiftLevel() == null ? 1 : referrer.getGiftLevel() + 1;
//            log.info("推荐人 {} 礼品未发满，标记为待发放", referrer.getName());
//            Optional<DataDict> inviteNewGift = dataDictService.findByGroupCodeAndParamCode("INVITE_NEW", "INVITE_NEW" + giftLevel);
//            if (inviteNewGift.isEmpty()) {
//                log.warn("未配置级别为：{}的邀新礼品", giftLevel);
//                return;
//            }
//            String giftCode = inviteNewGift.get().getParamValue();
//            if (StringUtil.isNullOrEmpty(giftCode)) {
//                log.warn("级别为：{}的邀新礼品未配置值", giftCode);
//                return;
//            }
//            Optional<Gift> giftByCode = giftService.findGiftByCode(giftCode);
//            if (giftByCode.isEmpty()) {
//                log.warn("级别为：{}的邀新礼品配置有误", giftCode);
//                return;
//            }
//
//            // 这里可以设置一个默认的待发放礼品，或者在后续处理时根据业务规则分配礼品;
//            GiftLog giftLog = GiftLog.builder()
//                    .customer(referrer)
//                    .gift(giftByCode.get())
//                    .giftStatus(GiftLog.GiftLogStatus.PENDING)
//                    .quantity(1)
//                    .build();
//            giftLogRepository.save(giftLog);
//        }
//    }

    private void validateCustomerData(CustomerCreateDTO dto) {
        // 验证手机号是否重复
        if (customerRepository.existsByPhone(dto.phone())) {
            throw CustomerException.alreadyExists(dto.phone());
        }

        // 其他验证...
    }

    @Transactional
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
        // 只更新允许修改的字段
        Customer customer = customerMapper.partialUpdate(dto, existingCustomer);
        Customer saved = customerRepository.save(customer);
        log.info("更新客户成功: {} ({})", saved, id);

        return customerMapper.fromEntity(saved);
    }

    @Transactional
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

    /**
     * 更新允许修改的字段
     */
//    private void updateAllowedFields(Customer customer, CustomerUpdateDTO dto) {
//        if (dto.name() != null) {
//            customer.setName(dto.name());
//        }
//        if (dto.phone() != null && !customer.getPhone().equals(dto.phone())) {
//            // 检查手机号是否重复
//            if (customerRepository.existsByPhone(dto.phone())) {
//                throw CustomerException.alreadyExists(dto.phone());
//            }
//            customer.setPhone(dto.phone());
//        }
//        if (dto.address() != null) {
//            customer.setAddress(dto.address());
//        }
//        if (dto.email() != null) {
//            customer.setEmail(dto.email());
//        }
//        if (dto.remark() != null) {
//            customer.setRemark(dto.remark());
//        }
//        if (dto.giftStatus() != null) {
//            customer.setStatus(dto.giftStatus());
//        }
//        if (dto.gender() != null) {
//            customer.setGender(dto.gender());
//        }
//        if (dto.birthday() != null) {
//            customer.setBirthday(dto.birthday());
//        }
//        if (dto.referrerId() != null) {
//            customer.setReferrer(customerRepository.findById(dto.referrerId())
//                    .orElseThrow(() -> {
//                        log.warn("传入推荐人 ID:{} 但是未查询到对应客户", dto.referrerId());
//                        return CustomerException.referrerNotFound(dto.referrerId());
//                    }));
//        }
//    }


    public Page<CustomerDTO> findAllCustomers(PageRequest pageRequest, String keyword, String sort, Integer status, Integer giftLevel, Integer gender, String direction, LocalDate startDate, LocalDate endDate) {
        log.debug("分页查询客户列表: page={}, size={}, keyword={}, sort={}, giftStatus={}, giftLevel={}, gender={}, startDate={}, endDate={}, direction={}",
                pageRequest.getPageNumber(), pageRequest.getPageSize(), keyword, sort, status, giftLevel, gender, startDate, endDate, direction);
        // 解析sort参数
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            if (direction != null && !direction.isEmpty()) {
                sortObj = Sort.by(Sort.Direction.fromString(direction), sort);
            }
            else {
                sortObj = Sort.by(Sort.Direction.ASC, sort);
            }
        }

        // 创建带有排序的PageRequest
        PageRequest sortedPageRequest = PageRequest.of(
                pageRequest.getPageNumber(),
                pageRequest.getPageSize(),
                sortObj
        );

        // 使用Specification构建动态查询条件
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 关键字查询条件
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + kw.toLowerCase() + "%"),
                    cb.like(root.get("phone"), "%" + kw),
                    cb.like(cb.lower(root.get("email")), "%" + kw.toLowerCase() + "%")
                ));
            }

            // 礼品等级查询条件
            if (giftLevel != null) {
                predicates.add(cb.equal(root.get("giftLevel"), giftLevel));
            }

            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("giftStatus"), status));
            }

            // 注册日期范围查询条件
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("registeredAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("registeredAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return customerRepository.findAll(spec, sortedPageRequest).map(customerMapper::fromEntity);
    }

    public Customer getById(@NotNull @Min(1) Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("客户不存在: id={}", id);
                    return CustomerException.notFound(id);
                });
    }
}
