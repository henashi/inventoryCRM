package com.henashi.inventorycrm.exception;

import com.henashi.inventorycrm.dto.CustomerUpdateDTO;

public class CustomerException extends BusinessException {
    public CustomerException(String code, String message) {
        super(code, message);
    }

    // 静态工厂方法
    public static CustomerException alreadyExists(String phone) {
        return new CustomerException("CUSTOMER_ALREADY_EXISTS",
                String.format("客户手机号 %s 已存在", phone));
    }

    public static CustomerException notFound(Long id) {
        return new CustomerException("CUSTOMER_NOT_FOUND",
                String.format("客户(ID: %d)不存在", id));
    }

    public static CustomerException invalidCustomer(CustomerUpdateDTO dto) {
        return new CustomerException("INVALID_CUSTOMER",
                String.format("客户数据异常: %s", dto));
    }
    public static CustomerException referrerNotFound(Long id) {
        return new CustomerException("REFERRER_NOT_FOUND",
                String.format("您选择的推荐人(ID: %d)不存在，请重新选择", id));
    }

    public static CustomerException disabled() {
        return new CustomerException("CUSTOMER_DISABLED", "客户已被禁用");
    }

    public static CustomerException idMismatch() {
        return new CustomerException("PATH_BODY_ID_MISMATCH", "客户ID 不匹配");
    }

    public static CustomerException invalidPhone() {
        return new CustomerException("CUSTOMER_INVALID_PHONE", "手机号格式不正确");
    }

    public static CustomerException invalidId(Long id) {
        return new CustomerException("CUSTOMER_INVALID_ID",
                String.format("ID: %d不存在，请重新选择", id));
    }

    public static CustomerException alreadyDeleted(Long id) {
        return new CustomerException("CUSTOMER_INVALID_ID",
                String.format("该客户(ID: %d)已删除", id));
    }

    public static CustomerException underage() {
        return new CustomerException("CUSTOMER_UNDERAGE", "客户年龄未满18岁");
    }
}
