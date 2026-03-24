package com.henashi.inventorycrm.aspect;

import ch.qos.logback.core.util.StringUtil;
import com.henashi.inventorycrm.dto.ProductDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.InventoryLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.henashi.inventorycrm.annotation.InventoryAudit;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.repository.ProductRepository;
import com.henashi.inventorycrm.service.InventoryLogService;
import com.henashi.inventorycrm.utils.ReflectionUtils;
import com.henashi.inventorycrm.utils.SecurityUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class InventoryLogAspect {

    private final InventoryLogService inventoryLogService;
    private final ProductRepository productRepository;
    private final SecurityUtils securityUtils;
    private final InventoryLogMapper inventoryLogMapper;

    /**
     * 环绕通知，处理库存操作日志
     */
    @Around("@annotation(inventoryAudit)")
    @Transactional(rollbackFor = Exception.class)
    public Object logInventoryOperation(ProceedingJoinPoint joinPoint,
                                        InventoryAudit inventoryAudit) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 1. 提取参数
        Map<String, Object> params = extractParameters(method, args, inventoryAudit);

        // 2. 获取商品ID和操作数量
        Long productId = (Long) params.get("productId");
        Integer quantity = (Integer) params.get("quantity");
        String reason = (String) params.get("reason");
        String operator = (String) params.get("operator");
        Object operationTypeObject = params.get("operationType");
        InventoryLog.LogType operationType;
        if (operationTypeObject instanceof String) {
            operationType = InventoryLog.LogType.valueOf((String) operationTypeObject);
        }
        else {
            operationType = (InventoryLog.LogType) operationTypeObject;
        }

        if (productId == null && !InventoryLog.LogType.CREATE.equals(operationType)) {
            throw new IllegalArgumentException("无法获取商品ID参数");
        }

        // 3. 获取操作前的库存
        Integer beforeStock = null;
        Product product = null;
        if (inventoryAudit.recordStockChange() && !InventoryLog.LogType.CREATE.equals(operationType)) {
            product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("商品不存在"));
            beforeStock = product.getCurrentStock();
        }

        // 4. 执行原方法
        Object result;
        try {
            result = joinPoint.proceed();
            // 5. 记录成功日志
            Integer afterStock = null;
            if (InventoryLog.LogType.CREATE.equals(operationType)) {
                ResponseEntity<ProductDTO> responseEntity = (ResponseEntity<ProductDTO>) result;
                if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                    productId = responseEntity.getBody().id();
                } else {
                    throw new RuntimeException("创建商品失败，无法获取商品ID");
                }
                product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("创建商品失败"));
                beforeStock = 0;
                afterStock = product.getCurrentStock();
            }
            if (inventoryAudit.recordStockChange() && product != null && !InventoryLog.LogType.CREATE.equals(operationType)) {
                // 刷新获取最新的库存
                product = productRepository.findById(productId).orElse(product);
                afterStock = product.getCurrentStock();
            }

            // 创建日志记录
            InventoryLog logRecord = createLogRecord(
                    inventoryAudit,
                    productId,
                    beforeStock,
                    afterStock,
                    quantity,
                    reason,
                    operator,
                    operationType
            );

            inventoryLogService.saveInventoryLog(inventoryLogMapper.createFromEntity(logRecord));

        } catch (Throwable e) {

            // 6. 记录失败日志（如果配置）
            if (inventoryAudit.logOnFailure()) {
                InventoryLog logRecord = createLogRecord(
                        inventoryAudit,
                        productId,
                        beforeStock,
                        null,
                        quantity,
                        reason,
                        operator,
                        operationType
                );

                inventoryLogService.saveInventoryLog(inventoryLogMapper.createFromEntity(logRecord));
            }

            throw e;
        }

        return result;
    }

    /**
     * 提取方法参数
     */
    private Map<String, Object> extractParameters(Method method, Object[] args,
                                                  InventoryAudit annotation) {
        Map<String, Object> params = new HashMap<>();

        // 1. 提取商品ID
        if (!annotation.productIdParam().isEmpty()) {
            params.put("productId", ReflectionUtils.getFieldValue(
                    method, args, annotation.productIdParam()));
        } else if (args.length > 0 && args[0] instanceof Long) {
            // 默认第一个参数为商品ID
            params.put("productId", args[0]);
        }

        // 2. 提取数量
        if (!annotation.quantityParam().isEmpty()) {
            params.put("quantity", ReflectionUtils.getFieldValue(
                    method, args, annotation.quantityParam()));
        }

        // 3. 提取原因
        if (!annotation.reasonParam().isEmpty()) {
            params.put("reason", ReflectionUtils.getFieldValue(
                    method, args, annotation.reasonParam()));
        }

        // 4. 提取操作人
        String operator = null;
        if (!annotation.operatorParam().isEmpty()) {
            operator = (String) ReflectionUtils.getFieldValue(
                    method, args, annotation.operatorParam());
        }
        if (operator == null) {
            operator = securityUtils.getCurrentUsername();
        }
        params.put("operator", operator);

        // 提取操作类型
        if (InventoryLog.LogType.PARAM.equals(annotation.operationType())) {
            if (StringUtil .notNullNorEmpty(annotation.reasonParam())) {
                params.put("operationType", ReflectionUtils.getFieldValue(
                        method, args, annotation.operationTypeParam()));
            }
            else {
                throw new BusinessException("参数异常：配置参数获取库存操作类型但是未传入配置");
            }
        }
        else {
            params.put("operationType", annotation.operationType());
        }
        return params;
    }

    /**
     * 创建日志记录
     */
    private InventoryLog createLogRecord(
            InventoryAudit annotation,
            Long productId,
            Integer beforeStock,
            Integer afterStock,
            Integer quantity,
            String reason,
            String operator,
            InventoryLog.LogType type
    ) {
        InventoryLog logRecord = new InventoryLog();
        logRecord.setProduct(productRepository.findById(productId).orElseThrow(() -> new BusinessException("商品不存在")));
        logRecord.setType(annotation.operationType());
        logRecord.setBeforeStock(beforeStock);
        logRecord.setAfterStock(afterStock);
        logRecord.setQuantity(quantity);
        logRecord.setReason(reason != null ? reason : annotation.description());
        logRecord.setOperator(operator);
        logRecord.setCreatedTime(LocalDateTime.now());
        logRecord.setType(type);
        return logRecord;
    }
}