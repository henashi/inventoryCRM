package com.henashi.inventorycrm.event;


import com.henashi.inventorycrm.pojo.Customer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ReferrerGiftEvent {
    private final Customer referrer;
}
