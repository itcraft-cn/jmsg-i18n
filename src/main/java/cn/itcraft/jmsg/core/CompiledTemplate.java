package cn.itcraft.jmsg.core;

import java.io.Serializable;

public class CompiledTemplate implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public static final CompiledTemplate EMPTY = 
        new CompiledTemplate(new String[]{""}, new int[0], null, "");
    
    private final String[] fragments;
    private final int[] paramIndices;
    private final String[] paramNames;
    private final int paramCount;
    private final String originalTemplate;
    
    CompiledTemplate(String[] fragments, int[] paramIndices, 
                     String[] paramNames, String originalTemplate) {
        this.fragments = fragments;
        this.paramIndices = paramIndices;
        this.paramNames = paramNames;
        this.paramCount = paramIndices.length;
        this.originalTemplate = originalTemplate;
    }
    
    public String[] getFragments() {
        return fragments;
    }
    
    public int[] getParamIndices() {
        return paramIndices;
    }
    
    public String[] getParamNames() {
        return paramNames;
    }
    
    public int getParamCount() {
        return paramCount;
    }
    
    public String getOriginalTemplate() {
        return originalTemplate;
    }
    
    public boolean hasParams() {
        return paramCount > 0;
    }
}