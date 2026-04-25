package com.henashi.inventorycrm.mapper.reference;

import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.repository.GiftRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GiftReferenceMapper {

    private final GiftRepository giftRepository;

    @Cacheable(value = "gifts", key = "#giftId",
            unless = "#result == null",
            cacheManager = "shortCache")
    @Named("idToGift")
    public Gift idToGift(Long giftId) {
        if (giftId == null) {
            return null;
        }
        return giftRepository.findById(giftId)
                .orElseThrow(() -> new BusinessException("Mapper转换 礼品不存在: " + giftId));
    }
}
