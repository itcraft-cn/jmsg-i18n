package cn.itcraft.jmsg.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SimpleMsgTemplate implements MsgTemplate, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final String name;
    private final String description;
    private final int order;
    private final Map<String, CompiledTemplate> compiledTemplates;
    
    public SimpleMsgTemplate(String code, String name, String description, int order,
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
    public MsgTemplate.Style getStyle() { return MsgTemplate.Style.SIMPLE; }
    
    @Override
    public String render(Locale locale, Object... args) {
        CompiledTemplate compiled = getCompiled(locale);
        return TemplateRenderer.renderSimple(compiled, args);
    }
    
    @Override
    public String render(Object... args) {
        return render(MsgTemplateConfig.getDefaultLocale(), args);
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
    
    public static SimpleMsgTemplate fromValueString(String code, String valueString) {
        String[] parts = valueString.split("\\|", -1);
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                "Invalid format. Expected: name_zh|name_en|template_zh|template_en|order");
        }
        
        String nameZh = parts[0].trim();
        String nameEn = parts[1].trim();
        int order = parts.length >= 5 ? Integer.parseInt(parts[4].trim()) : 0;
        
        Map<String, CompiledTemplate> compiled = new HashMap<>();
        compiled.put("zh", TemplateCompiler.compileSimple(parts[2].trim()));
        compiled.put("en", TemplateCompiler.compileSimple(parts[3].trim()));
        
        String displayName = nameZh.isEmpty() ? nameEn : 
                             nameEn.isEmpty() ? nameZh : nameZh + "/" + nameEn;
        
        return new SimpleMsgTemplate(code, displayName, "", order, compiled);
    }
}