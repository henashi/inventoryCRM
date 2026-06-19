package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.ImportFailureDetailDTO;
import com.henashi.inventorycrm.dto.ImportResultDTO;
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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "products", cacheManager = "shortCache")
public class ProductService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 50;
    private static final long MAX_IMPORT_FILE_SIZE = 5L * 1024 * 1024;
    private static final List<String> PRODUCT_IMPORT_TEMPLATE_FIELDS = List.of(
            "name", "code", "category", "price", "cost", "currentStock", "safeStock", "unit", "description", "remark"
    );
    private static final List<String> PRODUCT_IMPORT_REQUIRED_FIELDS = List.of(
            "name", "category", "price", "cost", "currentStock", "safeStock", "unit"
    );
    private static final String PRODUCT_IMPORT_DUPLICATE_STRATEGY = "按编码判重；编码为空时按系统规则自动生成；重复编码记录跳过并写入失败原因";
    private static final List<String> PRODUCT_IMPORT_NOTES = List.of(
            "仅支持 UTF-8 编码的 CSV 文件",
            "code 为空时按系统现有规则自动生成",
            "导入商品默认状态为在售(1)"
    );

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
            builder.append(csv(product.getName()))
                    .append(',').append(csv(product.getCode()))
                    .append(',').append(csv(product.getCategory()))
                    .append(',').append(csv(formatAmount(product.getPrice())))
                    .append(',').append(csv(formatAmount(product.getCost())))
                    .append(',').append(csv(defaultStock(product.getCurrentStock())))
                    .append(',').append(csv(defaultStock(product.getSafeStock())))
                    .append(',').append(csv(formatStatus(product.getStatus())))
                    .append(',').append(csv(formatDateTime(resolveLastUpdateTime(product))))
                    .append('\n');
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public ImportResultDTO getImportTemplateMeta() {
        return new ImportResultDTO(
                0,
                0,
                List.of(),
                PRODUCT_IMPORT_TEMPLATE_FIELDS,
                PRODUCT_IMPORT_REQUIRED_FIELDS,
                PRODUCT_IMPORT_DUPLICATE_STRATEGY,
                PRODUCT_IMPORT_NOTES
        );
    }

    @Transactional
    public ImportResultDTO importProducts(MultipartFile file) {
        validateImportFile(file);

        List<List<String>> csvRows = readCsvRows(file);
        if (csvRows.isEmpty()) {
            throw new BusinessException("PRODUCT_IMPORT_EMPTY", "导入文件为空，请上传包含表头的 CSV 文件");
        }

        Map<String, Integer> headerIndex = buildHeaderIndex(csvRows.get(0), PRODUCT_IMPORT_REQUIRED_FIELDS);
        List<ImportFailureDetailDTO> failures = new ArrayList<>();
        Set<String> seenCodes = new LinkedHashSet<>();
        int successCount = 0;

        for (int rowIndex = 1; rowIndex < csvRows.size(); rowIndex++) {
            int rowNumber = rowIndex + 1;
            List<String> row = csvRows.get(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }

            String code = normalizeCode(getCell(row, headerIndex, "code"));
            String name = getCell(row, headerIndex, "name");
            String identifier = code != null ? code : name;

            try {
                ProductCreateDTO dto = buildProductImportDTO(row, headerIndex, seenCodes);
                saveProduct(dto);
                if (dto.code() != null && !dto.code().isBlank()) {
                    seenCodes.add(dto.code().trim());
                }
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
                PRODUCT_IMPORT_TEMPLATE_FIELDS,
                PRODUCT_IMPORT_REQUIRED_FIELDS,
                PRODUCT_IMPORT_DUPLICATE_STRATEGY,
                PRODUCT_IMPORT_NOTES
        );
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

    private ProductCreateDTO buildProductImportDTO(List<String> row, Map<String, Integer> headerIndex, Set<String> seenCodes) {
        String name = getCell(row, headerIndex, "name");
        String code = normalizeCode(getCell(row, headerIndex, "code"));
        String category = getCell(row, headerIndex, "category");
        String unit = getCell(row, headerIndex, "unit");
        String description = getCell(row, headerIndex, "description");
        String remark = getCell(row, headerIndex, "remark");

        if (name.isBlank()) {
            throw new BusinessException("PRODUCT_IMPORT_NAME_REQUIRED", "商品名称不能为空");
        }
        if (category.isBlank()) {
            throw new BusinessException("PRODUCT_IMPORT_CATEGORY_REQUIRED", "商品分类不能为空");
        }
        if (unit.isBlank()) {
            throw new BusinessException("PRODUCT_IMPORT_UNIT_REQUIRED", "商品单位不能为空");
        }
        if (code != null) {
            if (seenCodes.contains(code)) {
                throw new BusinessException("PRODUCT_IMPORT_CODE_DUPLICATED", "文件内商品编码重复，已跳过");
            }
            if (productRepository.existsByCode(code)) {
                throw new BusinessException("PRODUCT_IMPORT_CODE_EXISTS", "商品编码已存在，已跳过");
            }
        }

        BigDecimal price = parseRequiredDecimal(getCell(row, headerIndex, "price"), "售价");
        BigDecimal cost = parseRequiredDecimal(getCell(row, headerIndex, "cost"), "成本");
        Integer currentStock = parseRequiredInteger(getCell(row, headerIndex, "currentStock"), "当前库存");
        Integer safeStock = parseRequiredInteger(getCell(row, headerIndex, "safeStock"), "安全库存");

        if (currentStock < 0) {
            throw new BusinessException("PRODUCT_IMPORT_CURRENT_STOCK_INVALID", "当前库存不能为负数");
        }
        if (safeStock < 0) {
            throw new BusinessException("PRODUCT_IMPORT_SAFE_STOCK_INVALID", "安全库存不能为负数");
        }

        ProductCreateDTO dto = new ProductCreateDTO(
                name,
                code,
                category,
                currentStock,
                safeStock,
                unit,
                price,
                cost,
                description,
                remark
        );

        Set<ConstraintViolation<ProductCreateDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new BusinessException("PRODUCT_IMPORT_VALIDATION_FAILED", buildValidationMessage(violations));
        }
        return dto;
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

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("PRODUCT_IMPORT_FILE_EMPTY", "导入文件不能为空");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new BusinessException("PRODUCT_IMPORT_FILE_TOO_LARGE", "导入文件不能超过 5MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessException("PRODUCT_IMPORT_FILE_TYPE_INVALID", "仅支持导入 CSV 文件");
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
            throw new BusinessException("PRODUCT_IMPORT_READ_FAILED", "导入文件解析失败，请检查文件编码或内容格式");
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
            throw new BusinessException("PRODUCT_IMPORT_HEADER_INVALID", "导入模板缺少必填列: " + String.join(", ", missingHeaders));
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

    private Integer parseRequiredInteger(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("PRODUCT_IMPORT_NUMBER_REQUIRED", fieldName + "不能为空");
        }
        try {
            return Integer.valueOf(value.trim());
        }
        catch (NumberFormatException ex) {
            throw new BusinessException("PRODUCT_IMPORT_NUMBER_INVALID", fieldName + "格式不正确");
        }
    }

    private BigDecimal parseRequiredDecimal(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("PRODUCT_IMPORT_DECIMAL_REQUIRED", fieldName + "不能为空");
        }
        try {
            return new BigDecimal(value.trim());
        }
        catch (NumberFormatException ex) {
            throw new BusinessException("PRODUCT_IMPORT_DECIMAL_INVALID", fieldName + "格式不正确");
        }
    }

    private String buildValidationMessage(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("；"));
    }

    private String normalizeHeader(String header) {
        return normalizeText(header).replace("\uFEFF", "").toLowerCase();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeCode(String code) {
        String normalized = normalizeText(code);
        return normalized.isEmpty() ? null : normalized;
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

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
