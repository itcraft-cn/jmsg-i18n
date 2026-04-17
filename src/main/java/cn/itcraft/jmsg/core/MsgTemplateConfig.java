package cn.itcraft.jmsg.core;

import java.util.Locale;

public final class MsgTemplateConfig {
    
    private static volatile Locale defaultLocale = Locale.getDefault();
    
    private static volatile int reflectCacheMaxSize = 1024;
    
    private static volatile boolean reflectCacheEnabled = true;
    
    private MsgTemplateConfig() {}
    
    public static Locale getDefaultLocale() {
        return defaultLocale;
    }
    
    public static void setDefaultLocale(Locale locale) {
        if (locale != null) {
            defaultLocale = locale;
        }
    }
    
    public static void setDefaultLocale(String localeCode) {
        if (localeCode != null) {
            defaultLocale = parseLocale(localeCode);
        }
    }
    
    private static Locale parseLocale(String localeCode) {
        String[] parts = localeCode.replace("-", "_").split("_");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else {
            return new Locale(parts[0], parts[1], parts[2]);
        }
    }
    
    public static int getReflectCacheMaxSize() {
        return reflectCacheMaxSize;
    }
    
    public static void setReflectCacheMaxSize(int maxSize) {
        reflectCacheMaxSize = maxSize > 0 ? maxSize : 1024;
    }
    
    public static boolean isReflectCacheEnabled() {
        return reflectCacheEnabled;
    }
    
    public static void setReflectCacheEnabled(boolean enabled) {
        reflectCacheEnabled = enabled;
    }
    
    public static void reset() {
        defaultLocale = Locale.getDefault();
        reflectCacheMaxSize = 1024;
        reflectCacheEnabled = true;
    }
}