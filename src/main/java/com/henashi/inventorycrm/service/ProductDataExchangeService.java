package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.ImportFailureDetailDTO;
import com.henashi.inventorycrm.dto.ImportResultDTO;
import com.henashi.inventorycrm.dto.ProductCreateDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.ProductMapper;
import com.henashi.inventorycrm.repository.ProductRepository;
import com.henashi.inventorycrm.utils.CsvUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductDataExchangeService {

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
    private final ProductService productService;

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

        List<List<String>> csvRows = CsvUtils.readCsvRows(file, "PRODUCT_IMPORT_READ_FAILED");
        if (csvRows.isEmpty()) {
            throw new BusinessException("PRODUCT_IMPORT_EMPTY", "导入文件为空，请上传包含表头的 CSV 文件");
        }

        Map<String, Integer> headerIndex = CsvUtils.buildHeaderIndex(csvRows.get(0), PRODUCT_IMPORT_REQUIRED_FIELDS, "PRODUCT_IMPORT_HEADER_INVALID");
        List<ImportFailureDetailDTO> failures = new ArrayList<>();
        Set<String> seenCodes = new LinkedHashSet<>();
        int successCount = 0;

        for (int rowIndex = 1; rowIndex < csvRows.size(); rowIndex++) {
            int rowNumber = rowIndex + 1;
            List<String> row = csvRows.get(rowIndex);
            if (CsvUtils.isBlankRow(row)) {
                continue;
            }

            String code = normalizeCode(CsvUtils.getCell(row, headerIndex, "code"));
            String name = CsvUtils.getCell(row, headerIndex, "name");
            String identifier = code != null ? code : name;

            try {
                ProductCreateDTO dto = buildProductImportDTO(row, headerIndex, seenCodes);
                productService.saveProduct(dto);
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

    private ProductCreateDTO buildProductImportDTO(List<String> row, Map<String, Integer> headerIndex, Set<String> seenCodes) {
        String name = CsvUtils.getCell(row, headerIndex, "name");
        String code = normalizeCode(CsvUtils.getCell(row, headerIndex, "code"));
        String category = CsvUtils.getCell(row, headerIndex, "category");
        String unit = CsvUtils.getCell(row, headerIndex, "unit");
        String description = CsvUtils.getCell(row, headerIndex, "description");
        String remark = CsvUtils.getCell(row, headerIndex, "remark");

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

        BigDecimal price = parseRequiredDecimal(CsvUtils.getCell(row, headerIndex, "price"), "售价");
        BigDecimal cost = parseRequiredDecimal(CsvUtils.getCell(row, headerIndex, "cost"), "成本");
        Integer currentStock = parseRequiredInteger(CsvUtils.getCell(row, headerIndex, "currentStock"), "当前库存");
        Integer safeStock = parseRequiredInteger(CsvUtils.getCell(row, headerIndex, "safeStock"), "安全库存");

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

    private static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
