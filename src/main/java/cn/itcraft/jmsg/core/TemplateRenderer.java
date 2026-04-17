package cn.itcraft.jmsg.core;

import cn.itcraft.jmsg.util.StringBuilderPool;
import cn.itcraft.jmsg.util.ReflectCache;
import java.util.Map;

public final class TemplateRenderer {
    
    private TemplateRenderer() {}
    
    public static String renderSimple(CompiledTemplate compiled, Object[] args) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        String[] fragments = compiled.getFragments();
        int[] indices = compiled.getParamIndices();
        int paramCount = compiled.getParamCount();
        
        int estimatedSize = estimateSize(fragments, args);
        StringBuilder result = StringBuilderPool.acquire(estimatedSize);
        
        for (int i = 0; i < fragments.length; i++) {
            result.append(fragments[i]);
            if (i < paramCount) {
                int argIndex = indices[i];
                if (argIndex < args.length && args[argIndex] != null) {
                    result.append(args[argIndex]);
                } else {
                    result.append("{}");
                }
            }
        }
        
        return StringBuilderPool.releaseAndToString(result);
    }
    
    public static String renderNamedMap(CompiledTemplate compiled, Map<String, Object> namedArgs) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        String[] fragments = compiled.getFragments();
        String[] paramNames = compiled.getParamNames();
        int paramCount = compiled.getParamCount();
        
        int estimatedSize = estimateNamedSize(fragments, paramNames, namedArgs);
        StringBuilder result = StringBuilderPool.acquire(estimatedSize);
        
        for (int i = 0; i < fragments.length; i++) {
            result.append(fragments[i]);
            if (i < paramCount) {
                String paramName = paramNames[i];
                Object value = namedArgs != null ? namedArgs.get(paramName) : null;
                if (value != null) {
                    result.append(value);
                } else {
                    result.append('{').append(paramName).append('}');
                }
            }
        }
        
        return StringBuilderPool.releaseAndToString(result);
    }
    
    public static String renderNamedBean(CompiledTemplate compiled, Object bean) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        if (bean == null) {
            return renderOriginal(compiled);
        }
        
        if (!MsgTemplateConfig.isReflectCacheEnabled()) {
            return renderNamedBeanNoCache(compiled, bean);
        }
        
        String[] fragments = compiled.getFragments();
        String[] paramNames = compiled.getParamNames();
        int paramCount = compiled.getParamCount();
        
        int estimatedSize = estimateBeanSize(fragments, paramNames, bean);
        StringBuilder result = StringBuilderPool.acquire(estimatedSize);
        
        for (int i = 0; i < fragments.length; i++) {
            result.append(fragments[i]);
            if (i < paramCount) {
                String paramName = paramNames[i];
                Object value = ReflectCache.getProperty(bean, paramName);
                if (value != null) {
                    result.append(value);
                } else {
                    result.append('{').append(paramName).append('}');
                }
            }
        }
        
        return StringBuilderPool.releaseAndToString(result);
    }
    
    private static String renderNamedBeanNoCache(CompiledTemplate compiled, Object bean) {
        String[] fragments = compiled.getFragments();
        String[] paramNames = compiled.getParamNames();
        int paramCount = compiled.getParamCount();
        
        StringBuilder result = StringBuilderPool.acquire();
        
        for (int i = 0; i < fragments.length; i++) {
            result.append(fragments[i]);
            if (i < paramCount) {
                String paramName = paramNames[i];
                Object value = ReflectCache.getPropertyNoCache(bean, paramName);
                if (value != null) {
                    result.append(value);
                } else {
                    result.append('{').append(paramName).append('}');
                }
            }
        }
        
        return StringBuilderPool.releaseAndToString(result);
    }
    
    private static String renderOriginal(CompiledTemplate compiled) {
        StringBuilder result = StringBuilderPool.acquire();
        for (String fragment : compiled.getFragments()) {
            result.append(fragment);
        }
        String[] paramNames = compiled.getParamNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < compiled.getParamCount(); i++) {
                result.append('{').append(paramNames[i]).append('}');
            }
        }
        return StringBuilderPool.releaseAndToString(result);
    }
    
    private static int estimateSize(String[] fragments, Object[] args) {
        int size = 0;
        for (String f : fragments) {
            size += f.length();
        }
        if (args != null) {
            for (Object arg : args) {
                size += arg != null ? 32 : 2;
            }
        }
        return size;
    }
    
    private static int estimateNamedSize(String[] fragments, String[] paramNames, Map<String, Object> namedArgs) {
        int size = 0;
        for (String f : fragments) {
            size += f.length();
        }
        if (namedArgs != null && paramNames != null) {
            for (String name : paramNames) {
                Object val = namedArgs.get(name);
                size += val != null ? String.valueOf(val).length() : name.length() + 2;
            }
        }
        return size;
    }
    
    private static int estimateBeanSize(String[] fragments, String[] paramNames, Object bean) {
        int size = 0;
        for (String f : fragments) {
            size += f.length();
        }
        if (paramNames != null) {
            for (String name : paramNames) {
                size += 32;
            }
        }
        return size;
    }
}