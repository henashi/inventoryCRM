package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.GiftLogCreateDTO;
import com.henashi.inventorycrm.dto.GiftLogDTO;
import com.henashi.inventorycrm.dto.GiftLogUpdateDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.GiftLogMapper;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.pojo.GiftLog;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.GiftLogRepository;
import com.henashi.inventorycrm.repository.GiftRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GiftLogService {

    private final Logger log = LoggerFactory.getLogger(GiftLogService.class);
    private final GiftLogRepository giftLogRepository;
    private final CustomerRepository customerRepository;
    private final GiftRepository giftRepository;
    private final GiftLogMapper giftLogMapper;

    public GiftLogDTO findGiftLogDTOById(Long logId) {
        log.debug("查询礼品日志详情: id={}", logId);
        if (logId == null || logId <= 0) {
            log.warn("日志ID无效: id={}", logId);
            throw new IllegalArgumentException("日志ID无效");
        }
        return giftLogRepository.findById(logId)
                .map(logEntity -> {
                    log.info("找到礼品日志: {}", logEntity.getId());
                    return giftLogMapper.fromEntity(logEntity);
                })
                .orElseThrow(() -> {
                    log.warn("礼品日志不存在: id={}", logId);
                    return new BusinessException("GIFT_LOG_NOT_FOUND",
                            String.format("礼品日志(ID: %d)不存在", logId));
                });
    }

    @Transactional
    public GiftLogDTO saveGiftLog(GiftLogCreateDTO logCreateDTO) {
        log.debug("创建礼品日志: 客户ID={}, 礼品标识={}",
                logCreateDTO.customerId(), logCreateDTO.giftId());

        // 验证客户是否存在
        Customer customer = customerRepository.findById(logCreateDTO.customerId())
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND",
                        String.format("客户(ID: %d)不存在", logCreateDTO.customerId())));
        // 验证礼品是否存在
        Gift gift = giftRepository.findById(logCreateDTO.giftId())
                .orElseThrow(() -> new BusinessException("GIFT_NOT_FOUND",
                        String.format("礼品(ID: %d)不存在", logCreateDTO.giftId())));

        // 验证客户是否可领取礼品
        boolean isNewTypeGift = Gift.GiftType.NEW.equals(gift.getType());
        if (isNewTypeGift && customer.getGiftLevel() != null && customer.getGiftLevel() >= 3) {
            throw new BusinessException("GIFT_LIMIT_EXCEEDED",
                    String.format("客户 %s 已达到礼品领取上限", customer.getName()));
        }

        // 创建礼品日志
        GiftLog giftLog = giftLogMapper.createToEntity(logCreateDTO);
        giftLog.setCustomer(customer);
        if (isNewTypeGift) {
            // 更新客户的礼品等级
            customer.setGiftLevel(customer.getGiftLevel() != null ? customer.getGiftLevel() + 1 : 1);
            customerRepository.save(customer);
        }

        giftLog.setStatus(GiftLog.GiftLogStatus.ISSUED);
        giftLog.setIsDeleted(false);
        GiftLog saved = giftLogRepository.save(giftLog);
        log.info("礼品日志创建成功: 客户 {} 获得 礼品 {}",
                customer.getName(), logCreateDTO.giftId());

        return giftLogMapper.fromEntity(saved);
    }

    public Page<GiftLogDTO> getLogsByCustomerId(Long customerId, Pageable pageable) {
        return giftLogRepository.findByCustomerId(customerId, pageable)
                .map(giftLogMapper::fromEntity);
    }

//    public Page<GiftLogDTO> getLogsByGiftLevel(Integer giftLevel, Pageable pageable) {
//        return giftLogRepository.findByGiftLevel(giftLevel, pageable)
//                .map(GiftLogDTO::fromEntity);
//    }

    public Integer getCustomerGiftLevel(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "客户不存在"));
        return customer.getGiftLevel();
    }

    public Page<GiftLogDTO> loadGiftLogPage(Pageable pageable) {
        return giftLogRepository.findAll(pageable)
                .map(giftLogMapper::fromEntity);
    }

    public GiftLogDTO updateGiftLog(Long id, GiftLogUpdateDTO giftLogUpdateDTO) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id");
        }
        Optional<GiftLog> giftLogOptional = giftLogRepository.findById(id);
        if (giftLogOptional.isEmpty()) {
            throw new BusinessException("id 数据不合法,{}", String.valueOf(id));
        }
        GiftLog giftLogFind = giftLogOptional.get();
        GiftLog giftLog = giftLogMapper.partialUpdate(giftLogUpdateDTO, giftLogFind);

        if (GiftLog.GiftLogStatus.ISSUED.equals(giftLog.getStatus())) {
            giftLog.setIssueAt(LocalDateTime.now());
        }

        GiftLog saved = giftLogRepository.save(giftLog);
        return giftLogMapper.fromEntity(saved);
    }
}
