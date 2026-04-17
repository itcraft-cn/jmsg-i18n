package cn.itcraft.jmsg.builder;

import cn.itcraft.jmsg.util.LocaleHelper;
import java.util.Locale;

public class MsgTemplateBuilder {
    
    private String code;
    private Locale locale;
    
    public MsgTemplateBuilder code(String code) {
        this.code = code;
        return this;
    }
    
    public MsgTemplateBuilder locale(Locale locale) {
        this.locale = locale;
        return this;
    }
    
    public MsgTemplateBuilder locale(String localeCode) {
        this.locale = LocaleHelper.parse(localeCode);
        return this;
    }
    
    public SimpleBuilder simple() {
        return new SimpleBuilder(code, locale);
    }
    
    public NamedBuilder named() {
        return new NamedBuilder(code, locale);
    }
    
    public static MsgTemplateBuilder create() {
        return new MsgTemplateBuilder();
    }
}