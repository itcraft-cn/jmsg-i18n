package cn.itcraft.jmsg.builder;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.MsgTemplateConfig;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import java.util.Locale;

public class SimpleBuilder {
    
    private final String code;
    private final Locale locale;
    private Object[] args;
    
    SimpleBuilder(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
    }
    
    public SimpleBuilder args(Object... args) {
        this.args = args;
        return this;
    }
    
    public String render() {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = findTemplate(code);
        if (template.getStyle() != MsgTemplate.Style.SIMPLE) {
            throw new IllegalArgumentException("Template is not SIMPLE style: " + code);
        }
        
        Locale renderLocale = locale != null ? locale : MsgTemplateConfig.getDefaultLocale();
        return template.render(renderLocale, args);
    }
    
    public String render(Locale locale) {
        if (code == null) {
            throw new IllegalStateException("Code is required");
        }
        
        MsgTemplate template = findTemplate(code);
        return template.render(locale, args);
    }
    
    private MsgTemplate findTemplate(String code) {
        MsgTemplate template = EnumRegistry.valueOf(SimpleMsgTemplate.class, code)
            .orElse(null);
        if (template == null) {
            template = EnumRegistry.valueOf(MsgTemplate.class, code)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + code));
        }
        return template;
    }
}