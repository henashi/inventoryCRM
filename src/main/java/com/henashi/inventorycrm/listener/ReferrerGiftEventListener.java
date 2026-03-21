package com.henashi.inventorycrm.listener;

import ch.qos.logback.core.util.StringUtil;
import com.henashi.inventorycrm.event.ReferrerGiftEvent;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.DataDict;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.pojo.GiftLog;
import com.henashi.inventorycrm.repository.GiftLogRepository;
import com.henashi.inventorycrm.service.DataDictService;
import com.henashi.inventorycrm.service.GiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferrerGiftEventListener {

    private final GiftService giftService;
    private final GiftLogRepository giftLogRepository;
    private final DataDictService dataDictService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(ReferrerGiftEvent referrerGiftEvent) {
        try {
            Customer referrer = referrerGiftEvent.getReferrer();
            if (referrer.getGiftLevel() != null && referrer.getGiftLevel() >= 3) {
                log.info("推荐人 {} 礼品已发满，不再发放", referrer.getName());
                return;
            }
            int giftLevel = referrer.getGiftLevel() == null ? 1 : referrer.getGiftLevel() + 1;
            log.info("推荐人 {} 礼品未发满，标记为待发放", referrer.getName());
            Optional<DataDict> inviteNewGift = dataDictService.findByGroupCodeAndParamCode("INVITE_NEW", "INVITE_NEW_" + giftLevel);
            if (inviteNewGift.isEmpty()) {
                log.warn("未配置级别为：{}的邀新礼品", giftLevel);
                return;
            }
            String giftCode = inviteNewGift.get().getParamValue();
            if (StringUtil.isNullOrEmpty(giftCode)) {
                log.warn("级别为：{}的邀新礼品未配置值", giftCode);
                return;
            }
            Optional<Gift> giftByCode = giftService.findGiftByCode(giftCode);
            if (giftByCode.isEmpty()) {
                log.warn("级别为：{}的邀新礼品配置有误", giftCode);
                return;
            }

            // 这里设置一个默认的待发放礼品，或者在后续处理时根据业务规则分配礼品;
            GiftLog giftLog = GiftLog.builder()
                    .customer(referrer)
                    .gift(giftByCode.get())
                    .status(GiftLog.GiftLogStatus.PENDING)
                    .quantity(1)
                    .build();
            giftLogRepository.save(giftLog);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            throw new BusinessException("礼品派发到邀新客户失败，请查看日志:");
        }
    }
}
