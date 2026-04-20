package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import cn.itcraft.jmsg.core.TemplateCompiler;
import cn.itcraft.jmsg.core.CompiledTemplate;
import cn.itcraft.jmsg.util.LocaleHelper;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.function.BiFunction;

public class YamlMsgTemplateLoader implements MsgTemplateLoader {
    
    private final String filePath;
    private final MsgTemplate.Style style;
    
    public YamlMsgTemplateLoader(String filePath, MsgTemplate.Style style) {
        this.filePath = filePath;
        this.style = style;
    }
    
    public static YamlMsgTemplateLoader forSimple(String filePath) {
        return new YamlMsgTemplateLoader(filePath, MsgTemplate.Style.SIMPLE);
    }
    
    public static YamlMsgTemplateLoader forNamed(String filePath) {
        return new YamlMsgTemplateLoader(filePath, MsgTemplate.Style.NAMED);
    }
    
    public MsgTemplate.Style getStyle() {
        return style;
    }
    
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        InputStream is = getResourceAsStream(filePath);
        if (is == null) {
            throw new RuntimeException("YAML template file not found: " + filePath);
        }
        
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(reader);
            
            Map<String, Object> templates = (Map<String, Object>) data.get("templates");
            if (templates == null) {
                throw new RuntimeException("YAML missing 'templates' section: " + filePath);
            }
            
            int count = 0;
            for (Map.Entry<String, Object> entry : templates.entrySet()) {
                String code = entry.getKey();
                Map<String, Object> templateData = (Map<String, Object>) entry.getValue();
                
                MsgTemplate template = createTemplate(code, templateData);
                registerTemplate(template);
                count++;
            }
            return count;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML templates from: " + filePath, e);
        }
    }
    
    private MsgTemplate createTemplate(String code, Map<String, Object> data) {
        int order = data.containsKey("order") ? ((Number) data.get("order")).intValue() : 0;
        String defaultLocaleStr = (String) data.get("default");
        Locale defaultLocale = LocaleHelper.parse(defaultLocaleStr);
        
        Map<String, Object> messages = (Map<String, Object>) data.get("messages");
        if (messages == null) {
            throw new IllegalArgumentException("Template " + code + " missing 'messages' section");
        }
        
        Map<String, CompiledTemplate> compiledMap = new HashMap<>();
        for (Map.Entry<String, Object> msgEntry : messages.entrySet()) {
            String localeKey = msgEntry.getKey();
            String templateStr = (String) msgEntry.getValue();
            Locale locale = LocaleHelper.parse(localeKey);
            String localeId = LocaleHelper.toUnderscore(locale);
            
            CompiledTemplate compiled = style == MsgTemplate.Style.SIMPLE 
                ? TemplateCompiler.compileSimple(templateStr)
                : TemplateCompiler.compileNamed(templateStr);
            compiledMap.put(localeId, compiled);
        }
        
        if (style == MsgTemplate.Style.SIMPLE) {
            return new SimpleMsgTemplate(code, code, "", order, defaultLocale, compiledMap);
        } else {
            return new NamedMsgTemplate(code, code, "", order, defaultLocale, compiledMap);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void registerTemplate(MsgTemplate template) {
        if (style == MsgTemplate.Style.NAMED) {
            EnumRegistry.register(NamedMsgTemplate.class, (NamedMsgTemplate) template);
        } else {
            EnumRegistry.register(SimpleMsgTemplate.class, (SimpleMsgTemplate) template);
        }
    }
    
    @Override
    public boolean validateSource() {
        return getResourceAsStream(filePath) != null;
    }
    
    private InputStream getResourceAsStream(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream(path);
        }
        return is;
    }
}