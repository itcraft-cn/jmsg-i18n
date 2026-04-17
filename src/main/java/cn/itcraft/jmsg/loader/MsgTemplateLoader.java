package cn.itcraft.jmsg.loader;

import cn.itcraft.dyenums.loader.DyEnumsLoader;
import cn.itcraft.jmsg.core.MsgTemplate;
import java.util.function.BiFunction;

public interface MsgTemplateLoader extends DyEnumsLoader<MsgTemplate> {
    
    @Override
    int load(Class<MsgTemplate> enumClass, BiFunction<String, String, MsgTemplate> factory);
    
    @Override
    boolean validateSource();
}