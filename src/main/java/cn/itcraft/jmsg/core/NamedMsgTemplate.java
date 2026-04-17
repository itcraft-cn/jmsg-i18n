package cn.itcraft.jmsg.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NamedMsgTemplate implements MsgTemplate, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final String name;
    private final String description;
    private final int order;
    private final Map<String, CompiledTemplate> compiledTemplates;
    
    public NamedMsgTemplate(String code, String name, String description, int order,
                            Map<String, CompiledTemplate> compiledTemplates) {
        this.code = code;
        this.name = name;
        this.description = description != null ? description : "";
        this.order = order;
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
    
    public String renderMap(Locale locale, Map<String, Object> namedArgs) {
        CompiledTemplate compiled = getCompiled(locale);
        return TemplateRenderer.renderNamedMap(compiled, namedArgs);
    }
    
    public String renderBean(Locale locale, Object bean) {
        CompiledTemplate compiled = getCompiled(locale);
        return TemplateRenderer.renderNamedBean(compiled, bean);
    }
    
    public CompiledTemplate getCompiled(Locale locale) {
        return compiledTemplates.getOrDefault(
            locale.getLanguage(),
            compiledTemplates.getOrDefault(MsgTemplateConfig.getDefaultLocale().getLanguage(), 
                compiledTemplates.getOrDefault("en", CompiledTemplate.EMPTY))
        );
    }
    
    public Set<String> getSupportedLocales() {
        return compiledTemplates.keySet();
    }
    
    public static NamedMsgTemplate fromValueString(String code, String valueString) {
        String[] parts = valueString.split("\\|", -1);
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                "Invalid format. Expected: name_zh|name_en|template_zh|template_en|order");
        }
        
        String nameZh = parts[0].trim();
        String nameEn = parts[1].trim();
        int order = parts.length >= 5 ? Integer.parseInt(parts[4].trim()) : 0;
        
        Map<String, CompiledTemplate> compiled = new HashMap<>();
        compiled.put("zh", TemplateCompiler.compileNamed(parts[2].trim()));
        compiled.put("en", TemplateCompiler.compileNamed(parts[3].trim()));
        
        String displayName = nameZh.isEmpty() ? nameEn : 
                             nameEn.isEmpty() ? nameZh : nameZh + "/" + nameEn;
        
        return new NamedMsgTemplate(code, displayName, "", order, compiled);
    }
}