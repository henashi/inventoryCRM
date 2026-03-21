package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.annotation.InventoryAudit;
import com.henashi.inventorycrm.dto.ProductCreateDTO;
import com.henashi.inventorycrm.dto.ProductDTO;
import com.henashi.inventorycrm.dto.ProductUpdateDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
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

    @PutMapping("/{id}")
    public ProductDTO updateProduct(
            @PathVariable @NotNull @Min(1) Long id,
            @RequestBody ProductUpdateDTO productUpdateDTO) {
        return productService.updateProduct(id, productUpdateDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable @NotNull @Min(1) Long id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public Page<ProductDTO> getProduct(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword) {
        return productService.findProductPage(PageRequest.of(page, size), keyword);
    }

    public record StockOperationRequest(
            @NotBlank String type,
            @Min(1) Integer quantity,
            String reason
    ) {}

    @PatchMapping("/{id}/stock")
    public ProductDTO updateProducts(@PathVariable @NotNull @Min(1) Long id, @RequestBody StockOperationRequest stockOperationRequest) {
        return productService.updateStock(id, stockOperationRequest.type, stockOperationRequest.quantity, stockOperationRequest.reason);

    }
}