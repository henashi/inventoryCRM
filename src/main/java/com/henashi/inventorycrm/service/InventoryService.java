package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.InventoryAdjustDTO;
import com.henashi.inventorycrm.dto.InventoryChangeDTO;
import com.henashi.inventorycrm.dto.InventoryDTO;
import com.henashi.inventorycrm.dto.InventoryDetailDTO;
import com.henashi.inventorycrm.dto.InventoryInDTO;
import com.henashi.inventorycrm.dto.InventoryOutDTO;
import com.henashi.inventorycrm.mapper.InventoryMapper;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.ProductRepository;
import com.henashi.inventorycrm.utils.SecurityUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final InventoryLogService inventoryLogService;
    private final InventoryMapper inventoryMapper;

    @Transactional(readOnly = true)
    public Page<InventoryDTO> findInventories(int page, int size, String keyword, Boolean lowStockOnly,
                                              Integer status, Long productId, Integer minStock, Integer maxStock) {
        Specification<Product> specification = buildInventorySpecification(keyword, lowStockOnly, status, productId, minStock, maxStock);
        return productRepository.findAll(specification, buildInventoryPage(page, size))
                .map(product -> toInventoryDTO(product, null));
    }

    @Transactional(readOnly = true)
    public InventoryDetailDTO findInventoryDetail(Long productId) {
        Product product = productService.findProductEntityById(productId);
        InventoryDTO inventory = toInventoryDTO(product, null);
        List<InventoryChangeDTO> recentChanges = inventoryLogService.findRecentLogs(productId, 5).stream()
                .map(inventoryMapper::toInventoryChangeDTO)
                .toList();
        return new InventoryDetailDTO(
                inventory.id(),
                inventory.productId(),
                inventory.productCode(),
                inventory.productName(),
                inventory.category(),
                inventory.warehouseId(),
                inventory.warehouseName(),
                inventory.currentStock(),
                inventory.safeStock(),
                inventory.maxStock(),
                inventory.unit(),
                inventory.status(),
                inventory.lastUpdateTime(),
                inventory.lowStock(),
                inventory.outOfStock(),
                inventory.alertReason(),
                recentChanges
        );
    }

    @Transactional
    public InventoryDTO stockIn(InventoryInDTO request) {
        Product product = productService.findProductEntityById(request.productId());
        int beforeStock = defaultStock(product.getCurrentStock());
        Product saved = productService.changeStock(product, InventoryLog.LogType.IN, request.quantity());
        inventoryLogService.recordInventoryChange(
                saved,
                InventoryLog.LogType.IN,
                request.quantity(),
                beforeStock,
                defaultStock(saved.getCurrentStock()),
                request.reason(),
                request.remark(),
                SecurityUtils.getCurrentUsername()
        );
        return toInventoryDTO(saved, null);
    }

    @Transactional
    public InventoryDTO stockOut(InventoryOutDTO request) {
        Product product = productService.findProductEntityById(request.productId());
        int beforeStock = defaultStock(product.getCurrentStock());
        Product saved = productService.changeStock(product, InventoryLog.LogType.OUT, request.quantity());
        inventoryLogService.recordInventoryChange(
                saved,
                InventoryLog.LogType.OUT,
                request.quantity(),
                beforeStock,
                defaultStock(saved.getCurrentStock()),
                request.reason(),
                request.remark(),
                SecurityUtils.getCurrentUsername()
        );
        return toInventoryDTO(saved, null);
    }

    @Transactional
    public InventoryDTO adjustStock(Long productId, InventoryAdjustDTO request) {
        Product product = productService.findProductEntityById(productId);
        int beforeStock = defaultStock(product.getCurrentStock());
        Product saved = productService.adjustStock(product, request.actualQuantity());
        int afterStock = defaultStock(saved.getCurrentStock());
        inventoryLogService.recordInventoryChange(
                saved,
                InventoryLog.LogType.ADJUST,
                afterStock - beforeStock,
                beforeStock,
                afterStock,
                request.reason(),
                request.remark(),
                SecurityUtils.getCurrentUsername()
        );
        return toInventoryDTO(saved, null);
    }

    @Transactional(readOnly = true)
    public Page<InventoryChangeDTO> findInventoryHistory(int page, int size, Long productId, String type,
                                                         String operator, LocalDate startTime, LocalDate endTime) {
        return inventoryLogService.findEntityPage(page, size, productId, type, operator, startTime, endTime)
                .map(inventoryMapper::toInventoryChangeDTO);
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> findAlerts(Integer threshold) {
        Specification<Product> specification = buildAlertSpecification(threshold);
        return productRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "currentStock").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(product -> toInventoryDTO(product, threshold))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportInventories(String keyword, Boolean lowStockOnly, Integer status,
                                    Long productId, Integer minStock, Integer maxStock) {
        Specification<Product> specification = buildInventorySpecification(keyword, lowStockOnly, status, productId, minStock, maxStock);
        List<InventoryDTO> inventories = productRepository.findAll(specification, buildInventorySort())
                .stream()
                .map(product -> toInventoryDTO(product, null))
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("商品编码,商品名称,当前库存,安全库存,最大库存,单位,状态,最近变更时间,预警状态,预警原因\n");

        for (InventoryDTO inventory : inventories) {
            builder.append(csv(inventory.productCode()))
                    .append(',').append(csv(inventory.productName()))
                    .append(',').append(csv(inventory.currentStock()))
                    .append(',').append(csv(inventory.safeStock()))
                    .append(',').append(csv(inventory.maxStock()))
                    .append(',').append(csv(inventory.unit()))
                    .append(',').append(csv(formatStatus(inventory.status())))
                    .append(',').append(csv(formatTime(inventory.lastUpdateTime())))
                    .append(',').append(csv(inventory.alertReason() == null ? "正常" : "预警"))
                    .append(',').append(csv(inventory.alertReason()))
                    .append('\n');
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private PageRequest buildInventoryPage(int page, int size) {
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.max(size, 1);
        return PageRequest.of(pageNumber, pageSize, buildInventorySort());
    }

    private Sort buildInventorySort() {
        return Sort.by(Sort.Order.desc("contentUpdatedTime"), Sort.Order.desc("id"));
    }

    private Specification<Product> buildInventorySpecification(String keyword, Boolean lowStockOnly, Integer status,
                                                               Long productId, Integer minStock, Integer maxStock) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Expression<Integer> currentStock = cb.coalesce(root.get("currentStock"), 0);
            Expression<Integer> safeStock = cb.coalesce(root.get("safeStock"), 0);

            if (productId != null && productId > 0L) {
                predicates.add(cb.equal(root.get("id"), productId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeKeyword),
                        cb.like(cb.lower(root.get("code")), likeKeyword)
                ));
            }
            if (Boolean.TRUE.equals(lowStockOnly)) {
                predicates.add(cb.lessThan(currentStock, safeStock));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), String.valueOf(status)));
            }
            if (minStock != null) {
                predicates.add(cb.greaterThanOrEqualTo(currentStock, minStock));
            }
            if (maxStock != null) {
                predicates.add(cb.lessThanOrEqualTo(currentStock, maxStock));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Product> buildAlertSpecification(Integer threshold) {
        return (root, query, cb) -> {
            Expression<Integer> currentStock = cb.coalesce(root.get("currentStock"), 0);
            Expression<Integer> safeStock = cb.coalesce(root.get("safeStock"), 0);
            if (threshold != null) {
                return cb.lessThan(currentStock, threshold);
            }
            return cb.lessThan(currentStock, safeStock);
        };
    }

    private InventoryDTO toInventoryDTO(Product product, Integer threshold) {
        InventoryDTO base = inventoryMapper.toInventoryDTO(product);
        return new InventoryDTO(
                base.id(),
                base.productId(),
                base.productCode(),
                base.productName(),
                base.category(),
                base.warehouseId(),
                base.warehouseName(),
                defaultStock(base.currentStock()),
                defaultStock(base.safeStock()),
                base.maxStock(),
                base.unit(),
                base.status(),
                base.lastUpdateTime(),
                base.lowStock(),
                base.outOfStock(),
                resolveAlertReason(product, threshold)
        );
    }

    private String resolveAlertReason(Product product, Integer threshold) {
        int currentStock = defaultStock(product.getCurrentStock());
        int safeStock = defaultStock(product.getSafeStock());

        if (currentStock <= 0) {
            return "当前库存为0，已缺货";
        }
        if (threshold != null) {
            if (currentStock < threshold) {
                return "当前库存低于统一预警阈值" + threshold;
            }
            return null;
        }
        if (currentStock < safeStock) {
            return "当前库存低于安全库存" + safeStock;
        }
        return null;
    }

    private int defaultStock(Integer stock) {
        return stock == null ? 0 : stock;
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

    private static String formatStatus(Integer status) {
        return Integer.valueOf(0).equals(status) ? "停用" : "正常";
    }
}
