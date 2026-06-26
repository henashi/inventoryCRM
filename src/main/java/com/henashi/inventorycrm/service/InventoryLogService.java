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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryLogService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        return findEntityPage(page, size, productId, type, operator, startTime, endTime)
                .map(inventoryLogMapper::fromEntity);
    }

    public Page<InventoryLog> findEntityPage(int page, int size, Long productId, String type, String operator, LocalDate startTime, LocalDate endTime) {
        log.debug("查询库存日志实体列表: page={}, size={}", page, size);
        Specification<InventoryLog> specification = buildSpecification(productId, type, operator, startTime, endTime);
        return inventoryLogRepository.findAll(specification, buildHistoryPage(page, size));
    }

    public List<InventoryLog> findRecentLogs(Long productId, int limit) {
        if (productId == null || productId <= 0L) {
            throw new IllegalArgumentException("商品ID无效");
        }
        int pageSize = Math.max(limit, 1);
        return inventoryLogRepository.findByProductId(
                        productId,
                        PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdTime").and(Sort.by(Sort.Direction.DESC, "id")))
                )
                .getContent();
    }

    @Transactional(readOnly = true)
    public byte[] exportInventoryLogs(Long productId, String type, String operator, LocalDate startTime, LocalDate endTime) {
        Specification<InventoryLog> specification = buildSpecification(productId, type, operator, startTime, endTime);
        List<InventoryLog> logs = inventoryLogRepository.findAll(
                specification,
                Sort.by(Sort.Direction.DESC, "createdTime").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("商品编码,商品名称,操作类型,变更数量,操作前库存,操作后库存,操作人,时间,备注/原因,结果状态\n");

        for (InventoryLog logEntity : logs) {
            builder.append(csv(logEntity.getProduct() != null ? logEntity.getProduct().getCode() : null))
                    .append(',').append(csv(logEntity.getProduct() != null ? logEntity.getProduct().getName() : null))
                    .append(',').append(csv(formatLogType(logEntity.getType())))
                    .append(',').append(csv(logEntity.getQuantity()))
                    .append(',').append(csv(logEntity.getBeforeStock()))
                    .append(',').append(csv(logEntity.getAfterStock()))
                    .append(',').append(csv(logEntity.getOperator()))
                    .append(',').append(csv(formatTime(logEntity.getCreatedTime())))
                    .append(',').append(csv(formatReason(logEntity)))
                    .append(',').append(csv(formatResultStatus(logEntity.getStatus())))
                    .append('\n');
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
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

        Product product = productRepository.findById(logCreateDTO.productId())
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND",
                        String.format("商品(ID: %d)不存在", logCreateDTO.productId())));

        int beforeStock = defaultStock(product.getCurrentStock());
        int afterStock = beforeStock;

        switch (logCreateDTO.type()) {
            case "IN" -> afterStock = beforeStock + logCreateDTO.quantity();
            case "OUT" -> {
                if (beforeStock < logCreateDTO.quantity()) {
                    throw new BusinessException("INSUFFICIENT_STOCK",
                            String.format("商品 %s 库存不足: 当前 %d, 需要 %d",
                                    product.getName(), beforeStock, logCreateDTO.quantity()));
                }
                afterStock = beforeStock - logCreateDTO.quantity();
            }
            case "ADJUST" -> afterStock = logCreateDTO.quantity();
            case "CREATE" -> beforeStock = 0;
            default -> throw new BusinessException("INVENTORY_LOG_TYPE_INVALID", "库存日志类型不合法");
        }

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

    @Transactional
    public InventoryLog recordInventoryChange(Product product, InventoryLog.LogType type, Integer quantity,
                                              Integer beforeStock, Integer afterStock,
                                              String reason, String remark, String operator) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (type == null) {
            throw new IllegalArgumentException("库存日志类型不能为空");
        }

        InventoryLog inventoryLog = InventoryLog.builder()
                .product(product)
                .type(type)
                .quantity(quantity == null ? 0 : quantity)
                .beforeStock(beforeStock)
                .afterStock(afterStock)
                .reason(reason)
                .remark(remark)
                .operator(normalizeOperator(operator))
                .status("SUCCESS")
                .build();

        InventoryLog saved = inventoryLogRepository.save(inventoryLog);
        log.info("记录库存日志成功: productId={}, type={}, beforeStock={}, afterStock={}",
                product.getId(), type, beforeStock, afterStock);
        return saved;
    }

    public Page<InventoryLogDTO> getLogsByProductId(Long productId, Pageable pageable) {
        return inventoryLogRepository.findByProductId(productId, pageable)
                .map(inventoryLogMapper::fromEntity);
    }

    public Page<InventoryLogDTO> getLogsByType(String type, Pageable pageable) {
        return inventoryLogRepository.findByType(resolveLogType(type), pageable)
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
        Long totalOperations = inCount + outCount;
        Long successCount = inventoryLogRepository.countByStatus("SUCCESS");
        Long failureCount = inventoryLogRepository.countByStatus("FAIL");
        double successRate = totalOperations > 0
            ? Math.round((double) successCount / totalOperations * 1000.0) / 10.0
            : 0.0;
        return new InventoryLogStatsDTO(inCount, outCount, inQuantity, outQuantity,
                totalOperations, successCount, failureCount, successRate, 0L);
    }

    private PageRequest buildHistoryPage(int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.max(size, 1);
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    private Specification<InventoryLog> buildSpecification(Long productId, String type, String operator, LocalDate startTime, LocalDate endTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (productId != null && productId > 0) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), resolveLogType(type)));
            }
            if (operator != null && !operator.isBlank()) {
                predicates.add(cb.equal(root.get("operator"), operator.trim()));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), startTime.atStartOfDay()));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdTime"), LocalDateTime.of(endTime, LocalTime.MAX)));
            }

            if (!predicates.isEmpty()) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }
            return cb.conjunction();
        };
    }

    private InventoryLog.LogType resolveLogType(String type) {
        try {
            return InventoryLog.LogType.valueOf(type.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException("INVENTORY_LOG_TYPE_INVALID", "库存日志类型不合法");
        }
    }

    private String getOperationName(String type) {
        return switch (type) {
            case "IN" -> "入库";
            case "OUT" -> "出库";
            case "ADJUST" -> "调整";
            default -> "未知操作";
        };
    }

    private int defaultStock(Integer stock) {
        return stock == null ? 0 : stock;
    }

    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return "system";
        }
        return operator.trim();
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private static String formatTime(LocalDateTime time) {
        return time == null ? "" : TIME_FORMATTER.format(time);
    }

    private static String formatReason(InventoryLog logEntity) {
        String reason = logEntity.getReason();
        String remark = logEntity.getRemark();
        if (reason == null || reason.isBlank()) {
            return remark == null ? "" : remark;
        }
        if (remark == null || remark.isBlank()) {
            return reason;
        }
        return reason + " / " + remark;
    }

    private static String formatResultStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        return switch (status) {
            case "SUCCESS", "1" -> "成功";
            case "FAIL", "0" -> "失败";
            default -> status;
        };
    }

    private static String formatLogType(InventoryLog.LogType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case CREATE -> "新建商品";
            case IN -> "入库";
            case OUT -> "出库";
            case ADJUST -> "调整";
            case PARAM -> "盘点调整";
        };
    }
}
