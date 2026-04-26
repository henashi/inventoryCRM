package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.GiftCreateDTO;
import com.henashi.inventorycrm.dto.GiftDTO;
import com.henashi.inventorycrm.dto.GiftUpdateDTO;
import com.henashi.inventorycrm.mapper.GiftMapper;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.repository.GiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "gifts", cacheManager = "shortCache")
public class GiftService {

    private final GiftRepository giftRepository;

    private final GiftMapper giftMapper;

    @Cacheable(key = "giftId", unless = "#result == null")
    public GiftDTO findGiftById(Long giftId) {
        return giftRepository.findById(giftId)
                .map(giftMapper::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("礼品不存在: id=" + giftId));
    }

    public Optional<Gift> findGiftByCode(String code) {
        return giftRepository.findByCode(code);
    }

    public Page<GiftDTO> findGifts(Pageable pageable) {
        return giftRepository.findAll(pageable)
                .map(giftMapper::fromEntity);
    }

    @CacheEvict(value = "gifts", key = "#giftId", cacheManager = "shortCache")
    public void deleteGiftById(Long giftId) {
        if (!giftRepository.existsById(giftId)) {
            throw new IllegalArgumentException("礼品不存在: id=" + giftId);
        }
        giftRepository.deleteById(giftId);
    }

    public GiftDTO saveGift(GiftCreateDTO giftDTO) {
        if (giftDTO == null) {
            throw new IllegalArgumentException("礼品数据不能为空");
        }
        Gift entity = giftMapper.createToEntity(giftDTO);
        return giftMapper.fromEntity(giftRepository.save(entity));
    }

    @CacheEvict(key = "#giftId")
    public GiftDTO updateGift(Long giftId, GiftUpdateDTO giftDTO) {
        if (giftId == null || giftId <= 0) {
            throw new IllegalArgumentException("礼品ID 无效: id=" + giftId);
        }
        if (giftDTO == null) {
            throw new IllegalArgumentException("礼品数据不能为空");
        }
        return giftRepository.findById(giftId)
                .map(existingGift -> {
                    existingGift = giftMapper.partialUpdate(giftDTO, existingGift);
                    return giftMapper.fromEntity(giftRepository.save(existingGift));
                })
                .orElseThrow(() -> new IllegalArgumentException("礼品不存在: id=" + giftId));
    }
}
