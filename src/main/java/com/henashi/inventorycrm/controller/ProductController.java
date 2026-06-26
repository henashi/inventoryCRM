package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.annotation.InventoryAudit;
import com.henashi.inventorycrm.dto.ImportResultDTO;
import com.henashi.inventorycrm.dto.ProductCreateDTO;
import com.henashi.inventorycrm.dto.ProductDTO;
import com.henashi.inventorycrm.dto.ProductSearchOptionDTO;
import com.henashi.inventorycrm.dto.ProductStockStatisticsDTO;
import com.henashi.inventorycrm.dto.ProductUpdateDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.service.ProductDataExchangeService;
import com.henashi.inventorycrm.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Tag(name = "商品管理控制器", description = "提供商品相关的CRUD操作接口")
public class ProductController {

    private final ProductService productService;
    private final ProductDataExchangeService productDataExchangeService;

    @GetMapping("/search")
    public List<ProductSearchOptionDTO> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") Integer limit) {
        return productService.searchProducts(keyword, limit);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        byte[] content = productService.exportProducts(keyword, category, status);
        String fileName = "products_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        return buildCsvResponse(fileName, content);
    }

    @GetMapping("/import/template")
    public ImportResultDTO importTemplate() {
        return productDataExchangeService.getImportTemplateMeta();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultDTO importProducts(@RequestParam("file") MultipartFile file) {
        return productDataExchangeService.importProducts(file);
    }

    @GetMapping("/stock/statistics")
    public ProductStockStatisticsDTO getStockStatistics() {
        return productService.getStockStatistics();
    }

    @GetMapping("/low-stock")
    public List<ProductDTO> getLowStockProducts(@RequestParam(required = false) Integer threshold) {
        return productService.findLowStockProducts(threshold);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取商品分类列表", description = "返回去重后的商品分类列表")
    public List<String> getCategories() {
        return productService.getCategories();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取商品信息", description = "通过商品ID查询商品的详细信息")
    public ProductDTO getProduct(
            @PathVariable @NotNull @Min(1) Long id) {
        return productService.findProductDTOById(id);
    }

    @PostMapping
    @InventoryAudit(
            operationType = InventoryLog.LogType.CREATE,
            description = "创建商品",
            productIdParam = "productCreateDTO.id",
            quantityParam = "productCreateDTO.currentStock",
            reasonParam = "productCreateDTO.description"
    )
    @Operation(summary = "创建新商品", description = "根据提供的商品信息创建一个新的商品")
    public ResponseEntity<ProductDTO> createProduct(
            @Valid @RequestBody ProductCreateDTO productCreateDTO) {
        ProductDTO savedProduct = productService.saveProduct(productCreateDTO);
        URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(savedProduct.id())
                        .toUri();
         return ResponseEntity.created(location).body(savedProduct);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "更新商品信息", description = "根据提供的商品信息更新指定ID的商品")
    public ProductDTO updateProduct(
            @PathVariable @NotNull @Min(1) Long id,
            @RequestBody ProductUpdateDTO productUpdateDTO) {
        return productService.updateProduct(id, productUpdateDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除商品", description = "通过商品ID删除对应的商品信息")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable @NotNull @Min(1) Long id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "获取商品列表", description = "分页查询所有商品信息，支持根据关键字搜索")
    public Page<ProductDTO> getProduct(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        Sort sort = Sort.by("contentUpdatedTime").descending();
        return productService.findProductPage(PageRequest.of(page, size, sort), keyword, category);
    }

    public record StockOperationRequest(
            @NotBlank String type,
            @Min(1) Integer quantity,
            String reason
    ) {}

    @PatchMapping("/{id}/stock")
    @InventoryAudit(
            operationType = InventoryLog.LogType.PARAM,
            description = "出入库",
            productIdParam = "id",
            operationTypeParam = "stockOperationRequest.type",
            quantityParam = "stockOperationRequest.quantity",
            reasonParam = "stockOperationRequest.reason"
    )
    @Operation(summary = "更新商品库存", description = "根据提供的商品ID和库存操作信息更新商品库存")
    public ProductDTO updateProducts(@PathVariable @NotNull @Min(1) Long id, @RequestBody StockOperationRequest stockOperationRequest) {
        return productService.updateStock(id, stockOperationRequest.type, stockOperationRequest.quantity, stockOperationRequest.reason);

    }

    private ResponseEntity<byte[]> buildCsvResponse(String fileName, byte[] content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }
}
