package cn.itcraft.jmsg.builder;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NamedBuilder {
    
    private final String code;
    private final Locale locale;
    private Map<String, Object> namedArgs;
    private Object bean;
    
    NamedBuilder(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
    }
    
    public NamedBuilder args(Map<String, Object> namedArgs) {
        this.namedArgs = namedArgs;
        this.bean = null;
        return this;
    }
    
    public NamedBuilder arg(String name, Object value) {
        if (this.namedArgs == null) {
            this.namedArgs = new HashMap<>();
        }
        this.namedArgs.put(name, value);
        this.bean = null;
        return this;
    }
    
    public NamedBuilder bean(Object bean) {
        this.bean = bean;
        this.namedArgs = null;
        return this;
    }
    
    public String render() {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = findTemplate(code);
        if (template.getStyle() != MsgTemplate.Style.NAMED) {
            throw new IllegalArgumentException("Template is not NAMED style: " + code);
        }
        
        Locale renderLocale = locale != null ? locale : MsgTemplateConfig.getDefaultLocale();
        
        if (bean != null) {
            return template.render(renderLocale, bean);
        } else {
            return template.render(renderLocale, namedArgs);
        }
    }
    
    public String render(Locale locale) {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = findTemplate(code);
        
        if (bean != null) {
            return template.render(locale, bean);
        } else {
            return template.render(locale, namedArgs);
        }
    }
    
    private MsgTemplate findTemplate(String code) {
        MsgTemplate template = EnumRegistry.valueOf(NamedMsgTemplate.class, code)
            .orElse(null);
        if (template == null) {
            template = EnumRegistry.valueOf(MsgTemplate.class, code)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + code));
        }
        return template;
    }
}