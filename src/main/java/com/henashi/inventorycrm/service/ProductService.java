package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.ProductCreateDTO;
import com.henashi.inventorycrm.dto.ProductDTO;
import com.henashi.inventorycrm.dto.ProductUpdateDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.ProductMapper;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductDTO findProductDTOById(Long productId) {
        log.debug("查询商品详情: id={}", productId);
        if (productId == null || productId <= 0) {
            log.warn("商品ID无效: id={}", productId);
            throw new IllegalArgumentException("商品ID无效");
        }
        return productRepository.findById(productId)
                .map(product -> {
                    log.info("找到商品: {} ({})", product.getName(), product.getCode());
                    return productMapper.fromEntity(product);
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

        // 验证库存合理性
        validateInventory(productCreateDTO);

        Product product = productMapper.createToEntity(productCreateDTO);
        product.setStatus(1); // 默认上架

        Product saved = productRepository.save(product);
        log.info("商品创建成功: {} ({})", saved.getName(), saved.getCode());

        return productMapper.fromEntity(saved);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductUpdateDTO dto) {
        if (id == null || id <= 0L) {
            log.warn("更新商品信息异常: {}", dto);
            throw new IllegalArgumentException("商品ID无效");
        }

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("未找到商品: {}", id);
                    return new BusinessException("PRODUCT_NOT_FOUND",
                            String.format("商品(ID: %d)不存在", id));
                });

        log.info("找到商品信息: {} ({})", existingProduct.getName(), existingProduct.getCode());

        existingProduct = productMapper.partialUpdate(dto, existingProduct);
        // 只更新允许修改的字段
//        updateAllowedFields(existingProduct, dto);

        Product saved = productRepository.save(existingProduct);
        log.info("商品更新成功: {} ({})", saved.getName(), saved.getCode());

        return productMapper.fromEntity(saved);
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null || id <= 0L) {
            log.warn("商品ID异常: {}", id);
            throw new IllegalArgumentException("商品ID无效");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("根据ID未找到商品: {}", id);
                    return new BusinessException("PRODUCT_NOT_FOUND",
                            String.format("商品(ID: %d)不存在", id));
                });

        if (product.getStatus() == 2) {
            log.warn("该商品(ID: {})已删除", id);
            throw new BusinessException("PRODUCT_ALREADY_DELETE",
                    String.format("商品(ID: %d)已删除", id));
        }

        product.setStatus(2); // 下架
        productRepository.save(product);
        log.info("商品删除成功: {} ({})", product.getName(), product.getCode());
    }

    private void validateInventory(ProductCreateDTO dto) {
        if (dto.currentStock() < 0) {
            throw new BusinessException("INVALID_STOCK", "当前库存不能为负数");
        }
        if (dto.safeStock() < 0) {
            throw new BusinessException("INVALID_SAFE_STOCK", "安全库存不能为负数");
        }
        if (dto.currentStock() < dto.safeStock()) {
            log.warn("商品 {} 当前库存({})低于安全库存({})",
                    dto.name(), dto.currentStock(), dto.safeStock());
        }
    }

//    private void updateAllowedFields(Product product, ProductUpdateDTO dto) {
//        if (dto.name() != null) {
//            product.setName(dto.name());
//        }
//        if (dto.code() != null && !product.getCode().equals(dto.code())) {
//            product.setCode(dto.code());
//        }
//        if (dto.currentStock() != null) {
//            product.setCurrentStock(dto.currentStock());
//        }
//        if (dto.safeStock() != null) {
//            product.setSafeStock(dto.safeStock());
//        }
//        if (dto.unit() != null) {
//            product.setUnit(dto.unit());
//        }
//        if (dto.price() != null) {
//            product.setPrice(dto.price());
//        }
//        if (dto.description() != null) {
//            product.setDescription(dto.description());
//        }
//        if (dto.remark() != null) {
//            product.setRemark(dto.remark());
//        }
//        if (dto.status() != null) { // 只允许上架或下架
//            product.setStatus(dto.status());
//        }
//    }

    // 新增：库存检查方法
    public void checkStock(Long productId, Integer requiredQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "商品不存在"));

        if (product.getCurrentStock() < requiredQuantity) {
            throw new BusinessException("INSUFFICIENT_STOCK",
                    String.format("商品 %s 库存不足: 当前 %d, 需要 %d",
                            product.getName(), product.getCurrentStock(), requiredQuantity));
        }
    }

    public Page<ProductDTO> findProductPage(Pageable pageable, String keyword) {
        Page<Product> products;
        if (keyword == null) {
            products = productRepository.findByStatusNot(2, pageable);
        }
        else {
            products = productRepository.findByNameContainingOrCodeContainingIgnoreCaseAndStatusNot(keyword, keyword, 2, pageable);
        }
        return products.map(productMapper::fromEntity);
    }

    public ProductDTO updateStock(Long id, String type, Integer quantity, String reason) {
        if (id == null || id <= 0L) {
            log.warn("商品ID异常: {}", id);
            throw new IllegalArgumentException("商品ID 无效");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "商品不存在"));

        if ("IN".equalsIgnoreCase(type)) {
            product.increaseStock(quantity);
        }
        else if ("OUT".equalsIgnoreCase(type)) {
            product.decreaseStock(quantity);
        }
        else {
            throw new IllegalArgumentException("库存变更类型无效: " + type);
        }

        Product saved = productRepository.save(product);
        return productMapper.fromEntity(saved);
    }
}