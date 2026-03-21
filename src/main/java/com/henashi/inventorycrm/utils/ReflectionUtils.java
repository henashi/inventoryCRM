package com.henashi.inventorycrm.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.StringUtils;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 反射工具类
 * 专门用于AOP切面中的参数提取和反射操作
 * 使用 @UtilityClass 确保无状态且不可实例化
 */
@Slf4j
@UtilityClass
public class ReflectionUtils {

    // Spring提供的参数名发现器（需要编译时开启 -parameters 参数）
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    /**
     * 从切面连接点提取方法参数名和值的映射
     *
     * @param method      目标方法
     * @param args        方法参数值数组
     * @return 参数名到参数值的映射
     */
    public static Map<String, Object> extractParameterMap(Method method, Object[] args) {
        Map<String, Object> paramMap = new HashMap<>();

        if (method == null || args == null) {
            return paramMap;
        }

        String[] paramNames = getParameterNames(method);
        if (paramNames == null || paramNames.length != args.length) {
            log.warn("方法参数名解析异常，方法: {}，参数数量: {}，参数名数量: {}",
                    method.getName(), args.length,
                    paramNames != null ? paramNames.length : 0);
            return paramMap;
        }

        for (int i = 0; i < paramNames.length; i++) {
            paramMap.put(paramNames[i], args[i]);
        }

        return paramMap;
    }

    /**
     * 从方法参数中获取指定名称的参数值
     *
     * @param method      目标方法
     * @param args        方法参数值数组
     * @param paramName   要获取的参数名
     * @return 参数值，如果不存在则返回null
     */
    public static Object getParameterValue(Method method, Object[] args, String paramName) {
        if (!StringUtils.hasText(paramName)) {
            return null;
        }

        Map<String, Object> paramMap = extractParameterMap(method, args);
        return paramMap.get(paramName);
    }

    /**
     * 智能获取字段值
     * 支持直接参数、嵌套对象属性、Map键值等多种方式
     *
     * @param method      目标方法
     * @param args        方法参数值数组
     * @param fieldPath   字段路径，如：
     *                    "quantity" - 直接参数
     *                    "request.quantity" - 对象属性
     *                    "request.items[0].quantity" - 集合元素属性
     * @return 字段值
     */
    public static Object getFieldValue(Method method, Object[] args, String fieldPath) {
        if (!StringUtils.hasText(fieldPath)) {
            return null;
        }

        // 如果是简单的参数名，直接获取
        Object value = getParameterValue(method, args, fieldPath);
        if (value != null) {
            return value;
        }

        // 解析点号路径，如 "request.quantity"
        String[] pathParts = fieldPath.split("\\.");
        if (pathParts.length < 2) {
            return null;
        }

        // 获取根对象
        Object rootObject = getParameterValue(method, args, pathParts[0]);
        if (rootObject == null) {
            return null;
        }

        // 递归获取嵌套属性
        return getNestedFieldValue(rootObject, pathParts, 1);
    }

    /**
     * 递归获取嵌套属性值
     *
     * @param rootObject  根对象
     * @param pathParts   路径分割数组
     * @param startIndex  开始索引
     * @return 属性值
     */
    private static Object getNestedFieldValue(Object rootObject, String[] pathParts, int startIndex) {
        if (rootObject == null || startIndex >= pathParts.length) {
            return rootObject;
        }

        String currentPath = pathParts[startIndex];

        try {
            // 处理数组/列表索引，如 items[0]
            if (currentPath.contains("[") && currentPath.endsWith("]")) {
                int bracketIndex = currentPath.indexOf("[");
                String fieldName = currentPath.substring(0, bracketIndex);
                String indexStr = currentPath.substring(bracketIndex + 1, currentPath.length() - 1);

                // 获取数组/列表对象
                Object arrayOrList = getFieldValue(rootObject, fieldName);
                if (arrayOrList == null) {
                    return null;
                }

                // 解析索引
                int index = Integer.parseInt(indexStr);

                // 处理List
                if (arrayOrList instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) arrayOrList;
                    if (index >= 0 && index < list.size()) {
                        return getNestedFieldValue(list.get(index), pathParts, startIndex + 1);
                    }
                }
                // 处理数组
                else if (arrayOrList.getClass().isArray()) {
                    Object[] array = (Object[]) arrayOrList;
                    if (index >= 0 && index < array.length) {
                        return getNestedFieldValue(array[index], pathParts, startIndex + 1);
                    }
                }

                return null;
            }

            // 普通属性
            Object value = getFieldValue(rootObject, currentPath);
            return getNestedFieldValue(value, pathParts, startIndex + 1);

        } catch (Exception e) {
            log.debug("获取嵌套属性失败: path={}, error={}",
                    String.join(".", pathParts), e.getMessage());
            return null;
        }
    }

    /**
     * 获取对象属性值（通过反射）
     *
     * @param target      目标对象
     * @param fieldName   属性名
     * @return 属性值
     */
    public static Object getFieldValue(Object target, String fieldName) {
        if (target == null || !StringUtils.hasText(fieldName)) {
            return null;
        }

        // 处理Map
        if (target instanceof Map) {
            return ((Map<?, ?>) target).get(fieldName);
        }

        // 处理JavaBean属性
        try {
            Field field = getDeclaredField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(target);
            }
        } catch (Exception e) {
            log.debug("通过反射获取属性失败: class={}, field={}, error={}",
                    target.getClass().getSimpleName(), fieldName, e.getMessage());

            // 尝试通过Getter方法获取
            return getPropertyViaGetter(target, fieldName);
        }

        return null;
    }

    /**
     * 通过Getter方法获取属性值
     *
     * @param target      目标对象
     * @param property    属性名
     * @return 属性值
     */
    private static Object getPropertyViaGetter(Object target, String property) {
        if (!StringUtils.hasText(property)) {
            return null;
        }

        String getterName = "get" + capitalize(property);
        String booleanGetterName = "is" + capitalize(property);

        try {
            // 尝试普通getter
            Method getter = target.getClass().getMethod(getterName);
            return getter.invoke(target);
        } catch (NoSuchMethodException e1) {
            try {
                // 尝试布尔类型的isGetter
                Method booleanGetter = target.getClass().getMethod(booleanGetterName);
                return booleanGetter.invoke(target);
            } catch (NoSuchMethodException e2) {
                // 两个getter都不存在
                return null;
            } catch (Exception e2) {
                log.debug("通过isGetter获取属性失败: class={}, property={}, error={}",
                        target.getClass().getSimpleName(), property, e2.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.debug("通过Getter获取属性失败: class={}, property={}, error={}",
                    target.getClass().getSimpleName(), property, e.getMessage());
            return null;
        }
    }

    /**
     * 获取类中声明的字段（包括父类）
     *
     * @param clazz      目标类
     * @param fieldName  字段名
     * @return 字段对象
     */
    private static Field getDeclaredField(Class<?> clazz, String fieldName) {
        for (Class<?> superClass = clazz; superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                return superClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // 继续在父类中查找
            }
        }
        return null;
    }

    /**
     * 获取方法的参数名数组
     *
     * @param method 目标方法
     * @return 参数名数组
     */
    public static String[] getParameterNames(Method method) {
        if (method == null) {
            return null;
        }

        // 优先使用Spring的发现器
        String[] paramNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        if (paramNames != null) {
            return paramNames;
        }

        // 如果Spring发现器返回null，尝试从Java 8+的Parameter获取
        try {
            Parameter[] parameters = method.getParameters();
            if (parameters.length > 0 && parameters[0].isNamePresent()) {
                paramNames = new String[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    paramNames[i] = parameters[i].getName();
                }
                return paramNames;
            }
        } catch (Exception e) {
            log.debug("从Parameter获取参数名失败: {}", e.getMessage());
        }

        // 生成默认参数名
        paramNames = new String[method.getParameterCount()];
        for (int i = 0; i < paramNames.length; i++) {
            paramNames[i] = "arg" + i;
        }

        return paramNames;
    }

    /**
     * 从MethodSignature获取参数名
     * 这是AspectJ的更可靠方式
     *
     * @param signature MethodSignature
     * @return 参数名数组
     */
    public static String[] getParameterNames(MethodSignature signature) {
        if (signature == null) {
            return null;
        }
        return signature.getParameterNames();
    }

    /**
     * 首字母大写
     *
     * @param str 输入字符串
     * @return 首字母大写后的字符串
     */
    private static String capitalize(String str) {
        if (!StringUtils.hasLength(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 安全类型转换
     *
     * @param value 原始值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    public static <T> Optional<T> safeCast(Object value, Class<T> targetType) {
        if (value == null || targetType == null) {
            return Optional.empty();
        }

        if (targetType.isInstance(value)) {
            return Optional.of(targetType.cast(value));
        }

        // 尝试基本类型转换
        try {
            if (targetType == Integer.class || targetType == int.class) {
                if (value instanceof Number) {
                    return Optional.of(targetType.cast(((Number) value).intValue()));
                } else if (value instanceof String) {
                    return Optional.of(targetType.cast(Integer.parseInt((String) value)));
                }
            } else if (targetType == Long.class || targetType == long.class) {
                if (value instanceof Number) {
                    return Optional.of(targetType.cast(((Number) value).longValue()));
                } else if (value instanceof String) {
                    return Optional.of(targetType.cast(Long.parseLong((String) value)));
                }
            } else if (targetType == String.class) {
                return Optional.of(targetType.cast(value.toString()));
            }
        } catch (Exception e) {
            log.debug("类型转换失败: value={}, targetType={}, error={}",
                    value, targetType.getName(), e.getMessage());
        }

        return Optional.empty();
    }
}