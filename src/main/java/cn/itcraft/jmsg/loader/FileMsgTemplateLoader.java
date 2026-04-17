package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.BiFunction;

public class FileMsgTemplateLoader implements MsgTemplateLoader {
    
    private final String filePath;
    private final MsgTemplate.Style style;
    
    public FileMsgTemplateLoader(String filePath, MsgTemplate.Style style) {
        this.filePath = filePath;
        this.style = style;
    }
    
    public FileMsgTemplateLoader(String filePath) {
        this(filePath, MsgTemplate.Style.SIMPLE);
    }
    
    public static FileMsgTemplateLoader forSimple(String filePath) {
        return new FileMsgTemplateLoader(filePath, MsgTemplate.Style.SIMPLE);
    }
    
    public static FileMsgTemplateLoader forNamed(String filePath) {
        return new FileMsgTemplateLoader(filePath, MsgTemplate.Style.NAMED);
    }
    
    public MsgTemplate.Style getStyle() {
        return style;
    }
    
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        Properties props = new Properties();
        try (InputStream is = getResourceAsStream(filePath)) {
            if (is == null) {
                throw new IOException("Template file not found: " + filePath);
            }
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            props.load(reader);
            
            BiFunction<String, String, MsgTemplate> actualFactory = getFactory();
            
            int count = 0;
            for (String code : props.stringPropertyNames()) {
                String valueString = props.getProperty(code);
                MsgTemplate template = actualFactory.apply(code, valueString);
                registerTemplate(template);
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load templates from: " + filePath, e);
        }
    }
    
    @Override
    public boolean validateSource() {
        return getResourceAsStream(filePath) != null;
    }
    
    private BiFunction<String, String, MsgTemplate> getFactory() {
        return style == MsgTemplate.Style.NAMED 
            ? NamedMsgTemplate::fromValueString 
            : SimpleMsgTemplate::fromValueString;
    }
    
    @SuppressWarnings("unchecked")
    private void registerTemplate(MsgTemplate template) {
        if (style == MsgTemplate.Style.NAMED) {
            EnumRegistry.register(NamedMsgTemplate.class, (NamedMsgTemplate) template);
        } else {
            EnumRegistry.register(SimpleMsgTemplate.class, (SimpleMsgTemplate) template);
        }
    }
    
    private InputStream getResourceAsStream(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream(path);
        }
        return is;
    }
}