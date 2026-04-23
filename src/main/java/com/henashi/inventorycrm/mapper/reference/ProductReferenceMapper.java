package com.henashi.inventorycrm.mapper.reference;

import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductReferenceMapper {

    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#productId",
            unless = "#result == null",
            cacheManager = "shortCache")
    @Named("idToProduct")
    public Product idToProduct(Long productId) {
        if (productId == null) {
            return null;
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Mapper转换 商品不存在: " + productId));
    }
}
