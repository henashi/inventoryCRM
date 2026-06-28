package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QueryExecutor 意图查询执行器测试。
 * 验证 6 种意图的 DB 查询逻辑和格式化输出。
 */
@SpringBootTest
@Transactional
class QueryExecutorTest {

    @Autowired
    private QueryExecutor queryExecutor;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("greeting — 返回欢迎语")
    void greeting() {
        String result = queryExecutor.execute("{\"intent\":\"greeting\",\"params\":{}}");
        assertThat(result).contains("你好").contains("库存CRM");
    }

    @Test
    @DisplayName("help — 返回帮助信息")
    void help() {
        String result = queryExecutor.execute("{\"intent\":\"help\",\"params\":{}}");
        assertThat(result).contains("商品").contains("客户").contains("礼品");
    }

    @Test
    @DisplayName("stock_query listAll — 列出所有商品")
    void stockQueryListAll() {
        String result = queryExecutor.execute(
                "{\"intent\":\"stock_query\",\"params\":{\"listAll\":true},\"requiresQuery\":true}");
        assertThat(result).contains("测试商品").contains("PRO_TEST_001").contains("100");
    }

    @Test
    @DisplayName("stock_query lowStockOnly — 无低库存商品时返回提示")
    void stockQueryLowStockNone() {
        // 当前 currentStock=100 > safeStock=10，不应命中
        String result = queryExecutor.execute(
                "{\"intent\":\"stock_query\",\"params\":{\"lowStockOnly\":true},\"requiresQuery\":true}");
        assertThat(result).contains("没有库存不足");
    }

    @Test
    @DisplayName("stock_query lowStockOnly — 有低库存商品时返回详情")
    void stockQueryLowStockFound() {
        // 将商品库存设到安全库存以下
        Product p = productRepository.findById(1L).orElseThrow();
        p.setCurrentStock(3);
        p.setSafeStock(10);
        productRepository.save(p);

        String result = queryExecutor.execute(
                "{\"intent\":\"stock_query\",\"params\":{\"lowStockOnly\":true},\"requiresQuery\":true}");
        assertThat(result).contains("测试商品").contains("3");
    }

    @Test
    @DisplayName("stock_query logType IN/OUT — 近7天出入库记录")
    void stockQueryLogType() {
        String result = queryExecutor.execute(
                "{\"intent\":\"stock_query\",\"params\":{\"logType\":\"IN\"},\"requiresQuery\":true}");
        assertThat(result).contains("近7天");
    }

    @Test
    @DisplayName("stock_query 默认 — 返回商品总数")
    void stockQueryDefault() {
        String result = queryExecutor.execute(
                "{\"intent\":\"stock_query\",\"params\":{},\"requiresQuery\":true}");
        assertThat(result).contains("共有");
    }

    @Test
    @DisplayName("customer_query listAll — 列出所有客户")
    void customerQueryListAll() {
        String result = queryExecutor.execute(
                "{\"intent\":\"customer_query\",\"params\":{\"listAll\":true},\"requiresQuery\":true}");
        assertThat(result).contains("测试客户").contains("13800000000");
    }

    @Test
    @DisplayName("customer_query recentFirst — 最近新增客户")
    void customerQueryRecent() {
        String result = queryExecutor.execute(
                "{\"intent\":\"customer_query\",\"params\":{\"recentFirst\":true},\"requiresQuery\":true}");
        assertThat(result).contains("最近新增");
    }

    @Test
    @DisplayName("gift_query — 返回可发放礼品")
    void giftQueryActive() {
        String result = queryExecutor.execute(
                "{\"intent\":\"gift_query\",\"params\":{\"activeOnly\":true},\"requiresQuery\":true}");
        assertThat(result).contains("测试礼品");
    }

    @Test
    @DisplayName("stat_query — 默认返回近30天统计")
    void statQueryDefault() {
        String result = queryExecutor.execute(
                "{\"intent\":\"stat_query\",\"params\":{},\"requiresQuery\":true}");
        assertThat(result).contains("近30天");
    }

    @Test
    @DisplayName("未知意图 — 返回帮助提示作为 fallback")
    void unknownIntent() {
        String result = queryExecutor.execute(
                "{\"intent\":\"unknown_intent\",\"params\":{}}");
        assertThat(result).contains("可以这样问我");
    }

    @Test
    @DisplayName("无效 JSON — 返回 fallback")
    void invalidJson() {
        String result = queryExecutor.execute("not json at all");
        assertThat(result).contains("换个问法试试");
    }
}
