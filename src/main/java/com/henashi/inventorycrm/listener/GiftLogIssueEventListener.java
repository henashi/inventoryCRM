package com.henashi.inventorycrm.listener;

import com.henashi.inventorycrm.event.GiftLogIssueEvent;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.GiftLog;
import com.henashi.inventorycrm.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class GiftLogIssueEventListener {

    private final CustomerRepository customerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(GiftLogIssueEvent giftLogIssueEvent) {
        try {
            GiftLog giftLog = giftLogIssueEvent.getGiftLog();
            Customer customer = giftLog.getCustomer();
            if (customer.getGiftLevel() != null && customer.getGiftLevel() > 2) {
                log.info("推荐人 {} 礼品已发满，不再发放", customer.getName());
                return;
            }
            int giftLevel = customer.getGiftLevel() == null ? 1 : customer.getGiftLevel() + 1;
            customer.setGiftLevel(giftLevel);
            customerRepository.save(customer);
        }
        catch (Exception e) {
            log.error("更新客户礼品等级失败：{}", giftLogIssueEvent.getGiftLog());
        }
    }
}
