package com.henashi.inventorycrm.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.GiftRepository;
import com.henashi.inventorycrm.repository.InventoryLogRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 意图查询执行器
 * <p>
 * 根据 LLM 解析出的意图和参数，执行数据库查询并返回格式化结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExecutor {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final GiftRepository giftRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 执行查询
     *
     * @param intentJson LLM 返回的意图 JSON
     * @return 格式化的查询结果文本
     */
    public String execute(String intentJson) {
        try {
            JsonNode root = objectMapper.readTree(intentJson);
            String intent = root.path("intent").asText();
            JsonNode params = root.path("params");

            return switch (intent) {
                case "stock_query" -> handleStockQuery(params);
                case "customer_query" -> handleCustomerQuery(params);
                case "gift_query" -> handleGiftQuery(params);
                case "stat_query" -> handleStatQuery(params);
                default -> "你好！我是库存CRM AI助手。你可以问我：\n"
                        + "• 哪些商品库存不足？\n"
                        + "• 最近新增的客户\n"
                        + "• 有哪些礼品可以发放\n"
                        + "• 上月出库总量是多少";
            };
        } catch (Exception e) {
            log.warn("查询执行失败: {}", e.getMessage());
            return "抱歉，我没有理解你的问题，请换个问法试试。";
        }
    }

    private String handleStockQuery(JsonNode params) {
        boolean lowStock = params.path("lowStockOnly").asBoolean(false);
        String logType = params.path("logType").asText("");

        if (lowStock) {
            List<Product> lowStockProducts = productRepository.findAll().stream()
                    .filter(p -> p.getCurrentStock() != null && p.getSafeStock() != null
                            && p.getCurrentStock() < p.getSafeStock())
                    .collect(Collectors.toList());

            if (lowStockProducts.isEmpty()) {
                return "✅ 当前没有库存不足的商品。";
            }
            StringBuilder sb = new StringBuilder("⚠️ 以下商品库存不足：\n");
            for (Product p : lowStockProducts) {
                sb.append("• ").append(p.getName())
                        .append("（编码:").append(p.getCode())
                        .append("）当前库存 ").append(p.getCurrentStock())
                        .append(" ").append(p.getUnit())
                        .append("，安全库存 ").append(p.getSafeStock())
                        .append("\n");
            }
            return sb.toString();
        }

        if (!logType.isEmpty()) {
            LocalDateTime since = LocalDate.now().minusDays(7).atStartOfDay();
            List<InventoryLog> logs = inventoryLogRepository.findAll().stream()
                    .filter(l -> l.getType() != null && l.getType().name().equals(logType))
                    .filter(l -> l.getCreatedTime() != null && l.getCreatedTime().isAfter(since))
                    .limit(10)
                    .collect(Collectors.toList());

            if (logs.isEmpty()) {
                return "近7天没有" + ("IN".equals(logType) ? "入库" : "出库") + "记录。";
            }
            StringBuilder sb = new StringBuilder("📋 近7天" + ("IN".equals(logType) ? "入库" : "出库") + "记录：\n");
            for (InventoryLog log : logs) {
                sb.append("• ").append(log.getProduct() != null ? log.getProduct().getName() : "未知")
                        .append(" × ").append(log.getQuantity())
                        .append(" (").append(log.getCreatedTime() != null ? log.getCreatedTime().format(FMT) : "").append(")")
                        .append("\n");
            }
            return sb.toString();
        }

        // 默认：返回商品总数
        long count = productRepository.count();
        return "📊 当前共有 " + count + " 个商品。";
    }

    private String handleCustomerQuery(JsonNode params) {
        boolean recentFirst = params.path("recentFirst").asBoolean(false);

        if (recentFirst) {
            List<Customer> recentCustomers = customerRepository.findAll().stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedTime() == null) return 1;
                        if (b.getCreatedTime() == null) return -1;
                        return b.getCreatedTime().compareTo(a.getCreatedTime());
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            if (recentCustomers.isEmpty()) {
                return "暂无客户数据。";
            }
            StringBuilder sb = new StringBuilder("👥 最近新增的客户：\n");
            for (Customer c : recentCustomers) {
                sb.append("• ").append(c.getName())
                        .append(" (").append(c.getPhone()).append(")")
                        .append(" 等级:").append(c.getGiftLevel() != null ? c.getGiftLevel() : 0)
                        .append("\n");
            }
            return sb.toString();
        }

        long count = customerRepository.count();
        return "👥 当前共有 " + count + " 位客户。";
    }

    private String handleGiftQuery(JsonNode params) {
        boolean activeOnly = params.path("activeOnly").asBoolean(true);

        List<Gift> gifts = giftRepository.findAll().stream()
                .filter(g -> !activeOnly || (g.getStatus() != null && "ACTIVE".equals(g.getStatus())))
                .collect(Collectors.toList());

        if (gifts.isEmpty()) {
            return activeOnly ? "当前没有可发放的礼品。" : "暂无礼品数据。";
        }
        StringBuilder sb = new StringBuilder(activeOnly ? "🎁 当前可发放的礼品：\n" : "🎁 全部礼品：\n");
        for (Gift g : gifts) {
            sb.append("• ").append(g.getName())
                    .append("（").append(g.getType() != null ? g.getType().getDescription() : "未知").append("）");
            if (!activeOnly && g.getStatus() != null) {
                sb.append(" [").append(g.getGiftStatus() != null ? g.getGiftStatus().getDescription() : g.getStatus()).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String handleStatQuery(JsonNode params) {
        String statType = params.path("statType").asText("");

        LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
        List<InventoryLog> logs = inventoryLogRepository.findAll().stream()
                .filter(l -> l.getCreatedTime() != null && l.getCreatedTime().isAfter(since))
                .collect(Collectors.toList());

        long inCount = logs.stream().filter(l -> l.getType() == InventoryLog.LogType.IN).count();
        long outCount = logs.stream().filter(l -> l.getType() == InventoryLog.LogType.OUT).count();
        long inQty = logs.stream().filter(l -> l.getType() == InventoryLog.LogType.IN)
                .mapToLong(l -> l.getQuantity() != null ? l.getQuantity() : 0).sum();
        long outQty = logs.stream().filter(l -> l.getType() == InventoryLog.LogType.OUT)
                .mapToLong(l -> l.getQuantity() != null ? l.getQuantity() : 0).sum();

        if ("total_out".equals(statType)) {
            return "📊 近30天出库统计：共 " + outCount + " 次，总量 " + outQty + " 件。";
        }
        if ("total_in".equals(statType)) {
            return "📊 近30天入库统计：共 " + inCount + " 次，总量 " + inQty + " 件。";
        }

        return "📊 近30天库存统计：\n"
                + "• 入库：" + inCount + " 次，共 " + inQty + " 件\n"
                + "• 出库：" + outCount + " 次，共 " + outQty + " 件\n"
                + "• 净变化：" + (inQty - outQty) + " 件";
    }
}
