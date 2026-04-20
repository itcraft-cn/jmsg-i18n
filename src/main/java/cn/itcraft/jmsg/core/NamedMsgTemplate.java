package cn.itcraft.jmsg.core;

import cn.itcraft.jmsg.util.LocaleHelper;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NamedMsgTemplate implements MsgTemplate, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final String name;
    private final String description;
    private final int order;
    private final Locale defaultLocale;
    private final Map<String, CompiledTemplate> compiledTemplates;
    
    public NamedMsgTemplate(String code, String name, String description, int order,
                            Locale defaultLocale, Map<String, CompiledTemplate> compiledTemplates) {
        this.code = code;
        this.name = name;
        this.description = description != null ? description : "";
        this.order = order;
        this.defaultLocale = defaultLocale;
        this.compiledTemplates = compiledTemplates;
    }
    
    @Override
    public String getCode() { return code; }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDescription() { return description; }
    
    @Override
    public int getOrder() { return order; }
    
    @Override
    public Locale getLocale() {
        return MsgTemplateConfig.getDefaultLocale();
    }
    
    @Override
    public Locale getDefaultLocale() {
        return defaultLocale;
    }
    
    @Override
    public MsgTemplate.Style getStyle() { return MsgTemplate.Style.NAMED; }
    
    @Override
    public String render(Locale locale, Object... args) {
        if (args == null || args.length == 0) {
            CompiledTemplate compiled = getCompiled(locale);
            return TemplateRenderer.renderNamedMap(compiled, null);
        }
        
        Object arg = args[0];
        if (arg instanceof Map) {
            return renderMap(locale, (Map<String, Object>) arg);
        } else {
            return renderBean(locale, arg);
        }
    }
    
    @Override
    public String render(Object... args) {
        return render(MsgTemplateConfig.getDefaultLocale(), args);
    }
    
    @Override
    public Set<String> getSupportedLocales() {
        return compiledTemplates.keySet();
    }
    
    @Override
    public boolean hasLocale(Locale locale) {
        String key = LocaleHelper.toUnderscore(locale);
        return compiledTemplates.containsKey(key);
    }
    
    public String renderMap(Locale locale, Map<String, Object> namedArgs) {
        CompiledTemplate compiled = getCompiled(locale);
        return TemplateRenderer.renderNamedMap(compiled, namedArgs);
    }
    
    public String renderBean(Locale locale, Object bean) {
        CompiledTemplate compiled = getCompiled(locale);
        return TemplateRenderer.renderNamedBean(compiled, bean);
    }
    
    public CompiledTemplate getCompiled(Locale locale) {
        String key = LocaleHelper.toUnderscore(locale);
        
        CompiledTemplate compiled = compiledTemplates.get(key);
        if (compiled != null) {
            return compiled;
        }
        
        String langKey = locale.getLanguage();
        compiled = compiledTemplates.get(langKey);
        if (compiled != null) {
            return compiled;
        }
        
        String defaultKey = LocaleHelper.toUnderscore(defaultLocale);
        compiled = compiledTemplates.get(defaultKey);
        if (compiled != null) {
            return compiled;
        }
        
        return CompiledTemplate.EMPTY;
    }
}