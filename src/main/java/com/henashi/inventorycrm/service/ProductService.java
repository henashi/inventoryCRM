package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.ProductCreateDTO;
import com.henashi.inventorycrm.dto.ProductDTO;
import com.henashi.inventorycrm.dto.ProductSearchOptionDTO;
import com.henashi.inventorycrm.dto.ProductStockStatisticsDTO;
import com.henashi.inventorycrm.dto.ProductUpdateDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.ProductMapper;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.ProductRepository;
import com.henashi.inventorycrm.utils.CsvUtils;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "products", cacheManager = "shortCache")
public class ProductService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 50;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final Validator validator;

    @Cacheable(key = "#p0", unless = "#result == null")
    public ProductDTO findProductDTOById(Long productId) {
        log.debug("查询商品详情: id={}", productId);
        return productMapper.fromEntity(findProductEntityById(productId));
    }

    @Transactional(readOnly = true)
    public Product findProductEntityById(Long productId) {
        if (productId == null || productId <= 0) {
            log.warn("商品ID无效: id={}", productId);
            throw new IllegalArgumentException("商品ID无效");
        }
        return productRepository.findById(productId)
                .map(product -> {
                    log.info("找到商品: {} ({})", product.getName(), product.getCode());
                    return product;
                })
                .orElseThrow(() -> {
                    log.warn("商品不存在: id={}", productId);
                    return new BusinessException("PRODUCT_NOT_FOUND",
                            String.format("商品(ID: %d)不存在", productId));
                });
    }

    @Transactional
    public ProductDTO saveProduct(ProductCreateDTO productCreateDTO) {
        log.debug("创建商品: {}", productCreateDTO.name());
        validateProductCreateData(productCreateDTO);

        Product product = productMapper.createToEntity(productCreateDTO);
        if (product.getCode() != null) {
            product.setCode(product.getCode().trim());
        }
        if (product.getCategory() != null) {
            product.setCategory(product.getCategory().trim());
        }
        product.setStatus("1");

        Product saved = productRepository.save(product);
        log.info("商品创建成功: {} ({})", saved.getName(), saved.getCode());

        return productMapper.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(key = "#p0")
    public ProductDTO updateProduct(Long id, ProductUpdateDTO dto) {
        if (id == null || id <= 0L) {
            log.warn("更新商品信息异常: {}", dto);
            throw new IllegalArgumentException("商品ID无效");
        }

        Product existingProduct = findProductEntityById(id);
        validateProductUpdateData(dto, existingProduct);
        Product product = productMapper.partialUpdate(dto, existingProduct);
        if (product.getCode() != null) {
            product.setCode(product.getCode().trim());
        }
        if (product.getCategory() != null) {
            product.setCategory(product.getCategory().trim());
        }
        Product saved = productRepository.save(product);
        log.info("商品更新成功: {} ({})", saved.getName(), saved.getCode());

        return productMapper.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(key = "#p0")
    public void deleteById(Long id) {
        if (id == null || id <= 0L) {
            log.warn("商品ID异常: {}", id);
            throw new IllegalArgumentException("商品ID无效");
        }

        Product product = findProductEntityById(id);
        productRepository.deleteById(id);
        log.info("商品删除成功: {} ({})", product.getName(), product.getCode());
    }

    public void checkStock(Long productId, Integer requiredQuantity) {
        Product product = findProductEntityById(productId);

        if (defaultStock(product.getCurrentStock()) < requiredQuantity) {
            throw new BusinessException("INSUFFICIENT_STOCK",
                    String.format("商品 %s 库存不足: 当前 %d, 需要 %d",
                            product.getName(), product.getCurrentStock(), requiredQuantity));
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> findProductPage(Pageable pageable, String keyword, String category) {
        Specification<Product> specification = buildProductSpecification(keyword, category, null);
        return productRepository.findAll(specification, pageable).map(productMapper::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ProductSearchOptionDTO> searchProducts(String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, normalizeSearchLimit(limit), buildDefaultSort());
        return productRepository.findAll(buildProductSpecification(keyword, null, null), pageable)
                .stream()
                .map(product -> new ProductSearchOptionDTO(
                        product.getId(),
                        product.getName(),
                        product.getCode(),
                        product.getCategory(),
                        defaultStock(product.getCurrentStock()),
                        product.getUnit(),
                        resolveStatus(product.getStatus())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportProducts(String keyword, String category, Integer status) {
        Specification<Product> specification = buildProductSpecification(keyword, category, status);
        List<Product> products = productRepository.findAll(specification, buildDefaultSort());

        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("名称,编码,分类,价格,成本,当前库存,安全库存,状态,更新时间\n");

        for (Product product : products) {
            builder.append(CsvUtils.csv(product.getName()))
                    .append(',').append(CsvUtils.csv(product.getCode()))
                    .append(',').append(CsvUtils.csv(product.getCategory()))
                    .append(',').append(CsvUtils.csv(formatAmount(product.getPrice())))
                    .append(',').append(CsvUtils.csv(formatAmount(product.getCost())))
                    .append(',').append(CsvUtils.csv(defaultStock(product.getCurrentStock())))
                    .append(',').append(CsvUtils.csv(defaultStock(product.getSafeStock())))
                    .append(',').append(CsvUtils.csv(formatStatus(product.getStatus())))
                    .append(',').append(CsvUtils.csv(formatDateTime(resolveLastUpdateTime(product))))
                    .append('\n');
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public ProductStockStatisticsDTO getStockStatistics() {
        List<Product> products = productRepository.findAll();
        long totalProducts = products.size();
        long activeProducts = products.stream()
                .filter(product -> resolveStatus(product.getStatus()) == 1)
                .count();
        long lowStockProducts = products.stream()
                .filter(product -> defaultStock(product.getCurrentStock()) < defaultStock(product.getSafeStock()))
                .count();
        long outOfStockProducts = products.stream()
                .filter(product -> defaultStock(product.getCurrentStock()) <= 0)
                .count();
        long totalStockQuantity = products.stream()
                .mapToLong(product -> defaultStock(product.getCurrentStock()))
                .sum();
        BigDecimal totalStockValue = products.stream()
                .map(product -> safePrice(product.getPrice()).multiply(BigDecimal.valueOf(defaultStock(product.getCurrentStock()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new ProductStockStatisticsDTO(
                totalProducts,
                activeProducts,
                lowStockProducts,
                outOfStockProducts,
                totalStockQuantity,
                totalStockValue
        );
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> findLowStockProducts(Integer threshold) {
        if (threshold != null && threshold < 0) {
            throw new BusinessException("PRODUCT_LOW_STOCK_THRESHOLD_INVALID", "threshold 不能为负数");
        }
        return productRepository.findAll(buildLowStockSpecification(threshold), buildDefaultSort()).stream()
                .sorted(Comparator
                        .comparingInt((Product product) -> lowStockGap(product, threshold)).reversed()
                        .thenComparingInt(product -> defaultStock(product.getCurrentStock()))
                        .thenComparing(Product::getId))
                .map(productMapper::fromEntity)
                .toList();
    }

    public List<String> getCategories() {
        return productRepository.findDistinctCategories().stream()
                .map(String::trim)
                .filter(category -> !category.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Transactional
    @CacheEvict(key = "#product.id")
    public Product changeStock(Product product, InventoryLog.LogType type, Integer quantity) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        validatePositiveQuantity(quantity);

        if (InventoryLog.LogType.IN.equals(type)) {
            product.increaseStock(quantity);
        }
        else if (InventoryLog.LogType.OUT.equals(type)) {
            product.decreaseStock(quantity);
        }
        else {
            throw new IllegalArgumentException("库存变更类型无效: " + type);
        }

        Product saved = productRepository.save(product);
        log.info("商品库存更新成功: productId={}, type={}, currentStock={}", saved.getId(), type, saved.getCurrentStock());
        return saved;
    }

    @Transactional
    @CacheEvict(key = "#product.id")
    public Product adjustStock(Product product, Integer actualQuantity) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (actualQuantity == null || actualQuantity < 0) {
            throw new IllegalArgumentException("实际库存不能为负数");
        }

        product.setCurrentStock(actualQuantity);
        Product saved = productRepository.save(product);
        log.info("商品库存调整成功: productId={}, currentStock={}", saved.getId(), saved.getCurrentStock());
        return saved;
    }

    @CacheEvict(key = "#p0")
    public ProductDTO updateStock(Long id, String type, Integer quantity, String reason) {
        InventoryLog.LogType operationType = resolveStockOperationType(type);
        Product product = findProductEntityById(id);
        Product saved = changeStock(product, operationType, quantity);
        return productMapper.fromEntity(saved);
    }

    private void validateProductCreateData(ProductCreateDTO dto) {
        if (dto.code() != null && !dto.code().isBlank() && productRepository.existsByCode(dto.code().trim())) {
            throw new BusinessException("PRODUCT_CODE_EXISTS", "商品编码已存在");
        }
        validateInventory(dto.currentStock(), dto.safeStock(), dto.price(), dto.cost());
    }

    private void validateProductUpdateData(ProductUpdateDTO dto, Product existingProduct) {
        if (dto == null) {
            throw new IllegalArgumentException("商品更新数据不能为空");
        }
        if (dto.code() != null && !dto.code().isBlank()) {
            String normalizedCode = dto.code().trim();
            if (!normalizedCode.equals(existingProduct.getCode()) && productRepository.existsByCode(normalizedCode)) {
                throw new BusinessException("PRODUCT_CODE_EXISTS", "商品编码已存在");
            }
        }
        validateInventory(dto.currentStock(), dto.safeStock(), dto.price(), dto.cost());
    }

    private void validateInventory(Integer currentStock, Integer safeStock, BigDecimal price, BigDecimal cost) {
        if (currentStock == null || currentStock < 0) {
            throw new BusinessException("INVALID_STOCK", "当前库存不能为负数");
        }
        if (safeStock == null || safeStock < 0) {
            throw new BusinessException("INVALID_SAFE_STOCK", "安全库存不能为负数");
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INVALID_PRICE", "价格不能为负数");
        }
        if (cost != null && cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INVALID_COST", "成本不能为负数");
        }
        if (currentStock < safeStock) {
            log.warn("商品当前库存({})低于安全库存({})", currentStock, safeStock);
        }
    }

    private void validatePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("库存数量必须大于0");
        }
    }

    private InventoryLog.LogType resolveStockOperationType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("库存变更类型无效");
        }
        return switch (type.trim().toUpperCase()) {
            case "IN" -> InventoryLog.LogType.IN;
            case "OUT" -> InventoryLog.LogType.OUT;
            default -> throw new IllegalArgumentException("库存变更类型无效: " + type);
        };
    }

    private Specification<Product> buildProductSpecification(String keyword, String category, Integer status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeKeyword),
                        cb.like(cb.lower(root.get("code")), likeKeyword),
                        cb.like(cb.lower(cb.coalesce(root.get("category"), "")), likeKeyword)
                ));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(cb.coalesce(root.get("category"), "")), category.trim().toLowerCase()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), String.valueOf(status)));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Product> buildLowStockSpecification(Integer threshold) {
        return (root, query, cb) -> {
            Expression<Integer> currentStock = cb.coalesce(root.get("currentStock"), 0);
            Expression<Integer> safeStock = cb.coalesce(root.get("safeStock"), 0);
            if (threshold != null) {
                return cb.lessThan(currentStock, threshold);
            }
            return cb.lessThan(currentStock, safeStock);
        };
    }

    private Sort buildDefaultSort() {
        return Sort.by(Sort.Order.desc("contentUpdatedTime"), Sort.Order.desc("id"));
    }

    private int normalizeSearchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }
        return Math.min(limit, MAX_SEARCH_LIMIT);
    }

    private int defaultStock(Integer stock) {
        return stock == null ? 0 : stock;
    }

    private int resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return 1;
        }
        return "0".equals(status.trim()) ? 0 : 1;
    }

    private int lowStockGap(Product product, Integer threshold) {
        int currentStock = defaultStock(product.getCurrentStock());
        int compareValue = threshold != null ? threshold : defaultStock(product.getSafeStock());
        return compareValue - currentStock;
    }

    private BigDecimal safePrice(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatAmount(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatStatus(String status) {
        return resolveStatus(status) == 1 ? "在售" : "停售";
    }

    private LocalDateTime resolveLastUpdateTime(Product product) {
        if (product.getContentUpdatedTime() != null) {
            return product.getContentUpdatedTime();
        }
        if (product.getStatusUpdatedTime() != null) {
            return product.getStatusUpdatedTime();
        }
        return product.getCreatedTime();
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "" : DATE_TIME_FORMATTER.format(time);
    }
}
