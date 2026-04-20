package cn.itcraft.jmsg.util;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ReflectCache {
    
    private static final int DEFAULT_MAX_SIZE = 1024;
    
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Method>> cache = 
        new ConcurrentHashMap<>();
    
    private static volatile int maxSize = DEFAULT_MAX_SIZE;
    
    private ReflectCache() {}
    
    public static void setMaxSize(int size) {
        maxSize = size > 0 ? size : DEFAULT_MAX_SIZE;
    }
    
    public static int getMaxSize() {
        return maxSize;
    }
    
    public static Object getProperty(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isEmpty()) {
            return null;
        }
        
        Class<?> clazz = bean.getClass();
        Method getter = getGetterMethod(clazz, propertyName);
        
        if (getter == null) {
            return null;
        }
        
        try {
            return getter.invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Object getPropertyNoCache(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isEmpty()) {
            return null;
        }
        
        Method getter = findGetterMethod(bean.getClass(), propertyName);
        
        if (getter == null) {
            return null;
        }
        
        try {
            return getter.invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Method getGetterMethod(Class<?> clazz, String propertyName) {
        ConcurrentMap<String, Method> classCache = cache.computeIfAbsent(clazz, k -> {
            if (cache.size() >= maxSize) {
                evictOldest();
            }
            return new ConcurrentHashMap<>();
        });
        
        return classCache.computeIfAbsent(propertyName, name -> findGetterMethod(clazz, name));
    }
    
    private static Method findGetterMethod(Class<?> clazz, String propertyName) {
        String capitalizedName = capitalize(propertyName);
        
        Method getter = tryGetMethod(clazz, "get" + capitalizedName);
        if (getter != null && isAccessibleGetter(getter)) {
            return getter;
        }
        
        getter = tryGetMethod(clazz, "is" + capitalizedName);
        if (getter != null && isBooleanGetter(getter)) {
            return getter;
        }
        
        getter = tryGetMethod(clazz, propertyName);
        if (getter != null && isAccessibleGetter(getter)) {
            return getter;
        }
        
        return null;
    }
    
    private static Method tryGetMethod(Class<?> clazz, String methodName) {
        try {
            Method method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            try {
                return clazz.getMethod(methodName);
            } catch (NoSuchMethodException ex) {
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null && superClass != Object.class) {
                    return tryGetMethod(superClass, methodName);
                }
                return null;
            }
        }
    }
    
    private static boolean isAccessibleGetter(Method method) {
        return method.getParameterCount() == 0 && method.getReturnType() != void.class;
    }
    
    private static boolean isBooleanGetter(Method method) {
        Class<?> returnType = method.getReturnType();
        return method.getParameterCount() == 0 && 
               (returnType == Boolean.class || returnType == boolean.class);
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    private static void evictOldest() {
        Iterator<Class<?>> it = cache.keySet().iterator();
        if (it.hasNext()) {
            it.next();
            it.remove();
        }
    }
    
    public static void clear() {
        cache.clear();
    }
    
    public static int size() {
        return cache.size();
    }
    
    public static boolean contains(Class<?> clazz, String propertyName) {
        ConcurrentMap<String, Method> classCache = cache.get(clazz);
        return classCache != null && classCache.containsKey(propertyName);
    }
}