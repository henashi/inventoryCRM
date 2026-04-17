package com.henashi.inventorycrm.aspect.detector;

import com.henashi.inventorycrm.pojo.BaseEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SafeFieldDetector {

    // 缓存每个实体类的字段信息
    private final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();

    /**
     * 检测实体变化
     */
    public ChangeResult detectChanges(BaseEntity original, BaseEntity current) {
        ChangeResult result = new ChangeResult();

        // 1. 检查status和isDeleted字段变化
        boolean statusChanged = !Objects.equals(original.getStatus(), current.getStatus())
                || !Objects.equals(original.isDeleted(), current.isDeleted());
        result.setStatusChanged(statusChanged);

        // 2. 检查其他字段变化
        boolean otherFieldsChanged = detectOtherFieldsChanged(original, current);
        result.setOtherFieldsChanged(otherFieldsChanged);

        // 3. 记录变化的字段详情（可选）
        if (log.isDebugEnabled()) {
            List<String> changedFields = getChangedFieldDetails(original, current);
            if (!changedFields.isEmpty()) {
                log.debug("实体 {} ID {} 变化的字段: {}",
                        original.getClass().getSimpleName(),
                        original.getId(),
                        changedFields);
            }
        }

        return result;
    }

    /**
     * 检测其他字段变化
     */
    private boolean detectOtherFieldsChanged(BaseEntity original, BaseEntity current) {
        List<Field> fields = getCachedFields(original.getClass());

        for (Field field : fields) {
            if (skipField(field)) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object oldValue = field.get(original);
                Object newValue = field.get(current);

                if (!Objects.equals(oldValue, newValue)) {
                    return true;
                }
            }
            catch (IllegalAccessException e) {
                log.error("无法访问字段 {}: {}", field.getName(), e.getMessage());
            }
        }
        return false;
    }

    /**
     * 获取变化的字段详情
     */
    private List<String> getChangedFieldDetails(BaseEntity original, BaseEntity current) {
        List<String> changedFields = new ArrayList<>();
        List<Field> fields = getCachedFields(original.getClass());

        for (Field field : fields) {
            if (skipField(field)) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object oldValue = field.get(original);
                Object newValue = field.get(current);

                if (!Objects.equals(oldValue, newValue)) {
                    changedFields.add(field.getName());
                }
            }
            catch (IllegalAccessException e) {
                log.error("无法访问字段 {}: {}", field.getName(), e.getMessage());
            }
        }
        return changedFields;
    }

    /**
     * 获取缓存的字段列表
     */
    private List<Field> getCachedFields(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, this::extractSimpleFields);
    }

    /**
     * 提取简单类型字段
     */
    private List<Field> extractSimpleFields(Class<?> clazz) {
        List<Field> simpleFields = new ArrayList<>();

        // 从当前类开始，遍历到BaseEntity
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (isSimpleType(field.getType())) {
                    simpleFields.add(field);
                }
            }
            current = current.getSuperclass();
        }

        return simpleFields;
    }

    /**
     * 判断是否为简单类型
     */
    private boolean isSimpleType(Class<?> type) {
        // 基本类型
        if (type.isPrimitive()) {
            return true;
        }

        // 常用包装类型
        return type == String.class ||
                type == Integer.class || type == int.class ||
                type == Long.class || type == long.class ||
                type == Double.class || type == double.class ||
                type == Boolean.class || type == boolean.class ||
                type == Character.class || type == char.class ||
                type == Float.class || type == float.class ||
                type == Short.class || type == short.class ||
                type == Byte.class || type == byte.class ||
                type == BigDecimal.class || type == BigInteger.class ||
                type == LocalDateTime.class || type == LocalDate.class || type == LocalTime.class ||
                type == Date.class || type == java.sql.Date.class || type == java.sql.Timestamp.class ||
                Enum.class.isAssignableFrom(type);
    }

    /**
     * 判断是否跳过该字段
     */
    private boolean skipField(Field field) {
        String fieldName = field.getName();

        // 跳过审计字段
        return fieldName.equals("id") ||
                fieldName.equals("status") ||  // status 单独处理
                fieldName.equals("deleted") ||
                fieldName.toLowerCase().contains("time") ||
                fieldName.toLowerCase().contains("by") ||
                fieldName.toLowerCase().contains("operator") ||
                fieldName.toLowerCase().contains("operation");
    }

    @Data
    public class ChangeResult {

        /**
         * giftStatus 字段是否变化
         */
        private boolean statusChanged = false;

        /**
         * 其他字段是否变化
         */
        private boolean otherFieldsChanged = false;

        /**
         * 变化的字段详情
         */
        private List<String> changedFields = new ArrayList<>();

        /**
         * 是否有任何变化
         */
        public boolean hasAnyChange() {
            return statusChanged || otherFieldsChanged;
        }
    }
}
