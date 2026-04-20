package cn.itcraft.jmsg.core;

import cn.itcraft.dyenums.core.DyEnum;
import java.util.Locale;
import java.util.Set;

public interface MsgTemplate extends DyEnum {
    
    Locale getLocale();
    
    Locale getDefaultLocale();
    
    MsgTemplate.Style getStyle();
    
    String render(Locale locale, Object... args);
    
    String render(Object... args);
    
    Set<String> getSupportedLocales();
    
    boolean hasLocale(Locale locale);
    
    enum Style {
        SIMPLE,
        NAMED
    }
}