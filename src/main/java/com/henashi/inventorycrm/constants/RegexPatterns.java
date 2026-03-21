package com.henashi.inventorycrm.constants;

public class RegexPatterns {

    // 邮箱正则（RFC 5322标准）
    public static final String EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    // 可选邮箱正则
    public static final String OPTIONAL_EMAIL = "^(|" + EMAIL + ")$";

    // 手机号正则
    public static final String PHONE = "^1[3-9]\\d{9}$";

    // 可选手机号正则
    public static final String OPTIONAL_PHONE = "^(|" + PHONE + ")$";
}
