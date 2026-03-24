package com.henashi.inventorycrm.event;

import com.henashi.inventorycrm.pojo.GiftLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GiftLogIssueEvent {
    private final GiftLog giftLog;
}
