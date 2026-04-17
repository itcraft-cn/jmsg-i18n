package cn.itcraft.jmsg.core;

import cn.itcraft.dyenums.core.DyEnum;
import java.util.Locale;

public interface MsgTemplate extends DyEnum {
    
    Locale getLocale();
    
    MsgTemplate.Style getStyle();
    
    String render(Locale locale, Object... args);
    
    String render(Object... args);
    
    enum Style {
        SIMPLE,
        NAMED
    }
}