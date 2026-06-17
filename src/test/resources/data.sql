INSERT INTO customer (id, name, phone, gift_level, deleted) VALUES (1, '测试客户', '13800000000', 0, FALSE);
INSERT INTO product (id, name, code, current_stock, safe_stock, unit, deleted) VALUES (1, '测试商品', 'PRO_TEST_001', 100, 10, '个', FALSE);
INSERT INTO gift (id, name, code, type, product_id, limit_enabled, limit_per_person, status, deleted) VALUES (1, '测试礼品', 'GIFT_TEST_001', 'NEW', 1, FALSE, 1, 'ACTIVE', FALSE);
