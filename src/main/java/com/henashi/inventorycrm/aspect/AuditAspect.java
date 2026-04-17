package com.henashi.inventorycrm.aspect;

import com.henashi.inventorycrm.aspect.detector.SafeFieldDetector;
import com.henashi.inventorycrm.pojo.BaseEntity;
import com.henashi.inventorycrm.service.EntitySnapshotService;
import jakarta.persistence.Entity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final SafeFieldDetector fieldDetector;

    private final EntitySnapshotService entitySnapshotService;

    // 缓存原始实体，避免重复查询
    private final ThreadLocal<Map<String, BaseEntity>> originalCache = ThreadLocal.withInitial(HashMap::new);

    /**
     * 拦截所有 Repository 的 save 方法
     */
    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository.save(..))")
    public Object interceptSave(ProceedingJoinPoint joinPoint)
           throws Throwable {
        // 获取目标对象
        Object entity = joinPoint.getArgs()[0];

        if (entity instanceof BaseEntity baseEntity) {
            try {
                // 1.创建操作
                if (baseEntity.getId() == null) {
                    handleCreate(baseEntity);
                }
                else {
                    handleUpdate(baseEntity);
                }
            }
            catch (Exception e) {
                log.warn("审计处理失败，继续执行保存操作: {}", e.getMessage());
            }
        }

        // 3. 执行原始的save方法
        return joinPoint.proceed();
    }

    /**
     * 拦截所有 Repository 的 saveAll 方法
     */
    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository.saveAll(..))")
    public Object interceptSaveAll(ProceedingJoinPoint joinPoint) throws Throwable {
        Collection<?> entities = (Collection<?>) joinPoint.getArgs()[0];

        List<Object> processedEntities = new ArrayList<>();
        for (Object entity : entities) {
            if (entity instanceof BaseEntity baseEntity) {
                try {
                    if (baseEntity.getId() == null) {
                        handleCreate(baseEntity);
                    } else {
                        handleUpdate(baseEntity);
                    }
                } catch (Exception e) {
                    log.warn("审计处理失败，继续执行: {}", e.getMessage());
                }
            }
            processedEntities.add(entity);
        }

        // 执行原始的saveAll
        return joinPoint.proceed(new Object[]{processedEntities});
    }

    /**
     * 处理创建操作
     */
    private void handleCreate(BaseEntity entity) {
        String operator = getCurrentOperator();
        LocalDateTime now = LocalDateTime.now();

        // 设置创建时间
        entity.setCreatedTime(now);
        entity.setCreatedBy(operator);

        // 同步设置
        entity.setContentUpdatedTime(now);
        entity.setContentUpdatedBy(operator);
        entity.setStatusUpdatedTime(now);
        entity.setStatusUpdatedBy(operator);
    }

    /**
     * 处理更新操作
     */
    private void handleUpdate(BaseEntity entity) {
        // 获取原始实体
        BaseEntity original = getOriginalEntity(entity);

        if (original == null) {
            log.debug("无法获取原始实体，跳过审计: {}", entity.getId());
            return;
        }

        // 调用ChangeDetector检测变化
        SafeFieldDetector.ChangeResult result = fieldDetector.detectChanges(original, entity);

        // 根据检测结果设置审计字段
        applyAuditFields(entity, result);
    }

    /**
     * 获取原始实体（带缓存）
     */
    private BaseEntity getOriginalEntity(BaseEntity entity) {
        String cacheKey = entity.getClass().getName() + ":" + entity.getId();

        // 从缓存获取
        Map<String, BaseEntity> cache = originalCache.get();
        BaseEntity cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 从数据库查询
        BaseEntity original = entitySnapshotService.getOldDataInNewTransaction(entity.getClass(), entity.getId());
        if (original != null) {

            // 将关联字段设为null，防止访问
            clearAssociations(original);

            // 放入缓存
            cache.put(cacheKey, original);
        }

        return original;
    }

    /**
     * 应用审计字段
     */
    private void applyAuditFields(BaseEntity entity, SafeFieldDetector.ChangeResult result) {
        String operator = getCurrentOperator();
        LocalDateTime now = LocalDateTime.now();

        // 1. 如果有状态变化
        if (result.isStatusChanged()) {
            entity.setStatusUpdatedTime(now);
            entity.setStatusUpdatedBy(operator);
        }

        // 2. 如果有内容变化
        if (result.isOtherFieldsChanged()) {
            entity.setContentUpdatedTime(now);
            entity.setContentUpdatedBy(operator);
        }

        // 3. 如果无变化但需要记录最后操作
        if (!result.isStatusChanged() && !result.isOtherFieldsChanged()) {
            // 可以选择不更新，或者更新content时间
            entity.setContentUpdatedTime(now);
            entity.setContentUpdatedBy(operator);
        }
    }

    /**
     * 清理关联字段
     */
    private void clearAssociations(BaseEntity entity) {
        Field[] fields = entity.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (isAssociationField(field)) {
                field.setAccessible(true);
                try {
                    field.set(entity, null);
                } catch (IllegalAccessException e) {
                    // 忽略
                }
            }
        }
    }

    /**
     * 判断是否为关联字段
     */
    private boolean isAssociationField(Field field) {
        return field.getType().isAnnotationPresent(Entity.class) ||
                Collection.class.isAssignableFrom(field.getType()) ||
                Map.class.isAssignableFrom(field.getType());
    }

    /**
     * 获取当前操作人
     */
    private String getCurrentOperator() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // 忽略
        }
        return "system";
    }

    /**
     * 清理缓存（事务后清理）
     */
    @AfterReturning("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void clearCache() {
        originalCache.remove();
    }
}
