package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.core.EnumRegistry;
import cn.itcraft.jmsg.core.MsgTemplate;
import cn.itcraft.jmsg.core.NamedMsgTemplate;
import cn.itcraft.jmsg.core.SimpleMsgTemplate;
import java.util.Properties;
import java.util.function.BiFunction;

public class PropMsgTemplateLoader implements MsgTemplateLoader {
    
    private final Properties properties;
    private final MsgTemplate.Style style;
    
    public PropMsgTemplateLoader(Properties properties, MsgTemplate.Style style) {
        this.properties = properties;
        this.style = style;
    }
    
    @Override
    public int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory) {
        BiFunction<String, String, MsgTemplate> actualFactory = getFactory();
        
        int count = 0;
        for (String code : properties.stringPropertyNames()) {
            String valueString = properties.getProperty(code);
            MsgTemplate template = actualFactory.apply(code, valueString);
            registerTemplate(template);
            count++;
        }
        return count;
    }
    
    @Override
    public boolean validateSource() {
        return properties != null && !properties.isEmpty();
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
}