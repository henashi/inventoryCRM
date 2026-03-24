package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.InventoryLogCreateDTO;
import com.henashi.inventorycrm.dto.InventoryLogDTO;
import com.henashi.inventorycrm.dto.InventoryLogStatsDTO;
import com.henashi.inventorycrm.dto.InventoryLogTypeStatsDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.InventoryLogMapper;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.InventoryLogRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryLogService {

    private final Logger log = LoggerFactory.getLogger(InventoryLogService.class);
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;
    private final InventoryLogMapper inventoryLogMapper;

    public Page<InventoryLogDTO> findPage(Pageable pageable) {
        log.debug("查询库存日志列表: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return inventoryLogRepository.findAll(pageable)
                .map(inventoryLogMapper::fromEntity);
    }


    public Page<InventoryLogDTO> findPage(int page, int size, Long productId, String type, String operator, LocalDate startTime, LocalDate endTime) {
        log.debug("查询库存日志列表: page={}, size={}", page, size);
        return inventoryLogRepository.findAll((root, query, cb) -> {
            // 使用列表收集所有条件
            List<Predicate> predicates = new ArrayList<>();

            if (productId != null && productId > 0) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(root.get("type"), InventoryLog.LogType.valueOf(type)));
            }
            if (operator != null && !operator.isEmpty()) {
                predicates.add(cb.equal(root.get("operator"), operator));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdTime"), endTime));
            }

            // 如果有条件，就组合；没有条件，就返回空
            if (!predicates.isEmpty()) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }
            return cb.conjunction(); // 无条件时返回true
        }, PageRequest.of(page, size, Sort.by("id").descending())).map(inventoryLogMapper::fromEntity);
    }

    public InventoryLogDTO findInventoryLogDTOById(Long logId) {
        log.debug("查询库存日志详情: id={}", logId);
        if (logId == null || logId <= 0) {
            log.warn("日志ID无效: id={}", logId);
            throw new IllegalArgumentException("日志ID无效");
        }
        return inventoryLogRepository.findById(logId)
                .map(logEntity -> {
                    log.info("找到库存日志: {}", logEntity.getId());
                    return inventoryLogMapper.fromEntity(logEntity);
                })
                .orElseThrow(() -> {
                    log.warn("库存日志不存在: id={}", logId);
                    return new BusinessException("INVENTORY_LOG_NOT_FOUND",
                            String.format("库存日志(ID: %d)不存在", logId));
                });
    }

    @Transactional
    public InventoryLogDTO saveInventoryLog(InventoryLogCreateDTO logCreateDTO) {
        log.debug("创建库存日志: 商品ID={}, 操作类型={}, 数量={}",
                logCreateDTO.productId(), logCreateDTO.type(), logCreateDTO.quantity());

        // 验证商品是否存在
        Product product = productRepository.findById(logCreateDTO.productId())
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND",
                        String.format("商品(ID: %d)不存在", logCreateDTO.productId())));

        // 记录操作前的库存
        int beforeStock = product.getCurrentStock();
        int afterStock = beforeStock;

        // 根据操作类型更新库存
        switch (logCreateDTO.type()) {
            case "IN" -> {
                afterStock = beforeStock + logCreateDTO.quantity();
//                product.setCurrentStock(afterStock);
            }
            case "OUT" -> {
                // 检查库存是否充足
                if (beforeStock < logCreateDTO.quantity()) {
                    throw new BusinessException("INSUFFICIENT_STOCK",
                            String.format("商品 %s 库存不足: 当前 %d, 需要 %d",
                                    product.getName(), beforeStock, logCreateDTO.quantity()));
                }
                afterStock = beforeStock - logCreateDTO.quantity();
//                product.setCurrentStock(afterStock);
            }
            case "ADJUST" -> {
                afterStock = logCreateDTO.quantity();
//                product.setCurrentStock(afterStock);
            }
            case "CREATE" -> {
                // 创建新商品时，初始库存为0
                beforeStock = 0;
            }
        }
        // 保存商品库存更新
//        productRepository.save(product);

        // 创建库存日志
        InventoryLog inventoryLog = inventoryLogMapper.createToEntity(logCreateDTO);
        inventoryLog.setProduct(product);
        inventoryLog.setBeforeStock(beforeStock);
        inventoryLog.setAfterStock(afterStock);

        InventoryLog saved = inventoryLogRepository.save(inventoryLog);
        log.info("库存日志创建成功: 商品 {} {} {} 件, 库存 {} -> {}",
                product.getName(),
                getOperationName(logCreateDTO.type()),
                logCreateDTO.quantity(),
                beforeStock, afterStock);

        return inventoryLogMapper.fromEntity(saved);
    }

    public Page<InventoryLogDTO> getLogsByProductId(Long productId, Pageable pageable) {
        return inventoryLogRepository.findByProductId(productId, pageable)
                .map(inventoryLogMapper::fromEntity);
    }

    public Page<InventoryLogDTO> getLogsByType(String type, Pageable pageable) {
        return inventoryLogRepository.findByType(InventoryLog.LogType.valueOf(type), pageable)
                .map(inventoryLogMapper::fromEntity);
    }

    public InventoryLogStatsDTO countStats() {
        Long inCount = 0L;
        Long outCount = 0L;
        Long inQuantity = 0L;
        Long outQuantity = 0L;
        List<InventoryLogTypeStatsDTO> typeStatsDTOList = inventoryLogRepository.countStats();
        if (!typeStatsDTOList.isEmpty()) {
            for (InventoryLogTypeStatsDTO item : typeStatsDTOList) {
                if (InventoryLog.LogType.IN.equals(item.type())
                    || InventoryLog.LogType.CREATE.equals(item.type())) {
                    inCount += item.count();
                    inQuantity += item.quantityCount();
                }
                if (InventoryLog.LogType.OUT.equals(item.type())) {
                    outCount = item.count();
                    outQuantity = item.quantityCount();
                }
            }
        }
        return new InventoryLogStatsDTO(inCount, outCount, inQuantity, outQuantity);
    }

    private String getOperationName(String type) {
        return switch (type) {
            case "IN" -> "入库";
            case "OUT" -> "出库";
            case "ADJUST" -> "调整";
            default -> "未知操作";
        };
    }
}