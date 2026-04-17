package cn.itcraft.jmsg.core;

import java.util.ArrayList;
import java.util.List;

public final class TemplateCompiler {
    
    private static final char PLACEHOLDER_START = '{';
    private static final char PLACEHOLDER_END = '}';
    
    private TemplateCompiler() {}
    
    public static CompiledTemplate compileSimple(String template) {
        if (template == null || template.isEmpty()) {
            return CompiledTemplate.EMPTY;
        }
        
        List<String> fragments = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        StringBuilder currentFragment = new StringBuilder();
        int index = 0;
        
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            
            if (c == PLACEHOLDER_START && i + 1 < template.length() 
                && template.charAt(i + 1) == PLACEHOLDER_END) {
                fragments.add(currentFragment.toString());
                currentFragment.setLength(0);
                indices.add(index++);
                i++;
            } else {
                currentFragment.append(c);
            }
        }
        fragments.add(currentFragment.toString());
        
        return new CompiledTemplate(
            fragments.toArray(new String[0]),
            indices.stream().mapToInt(Integer::intValue).toArray(),
            null,
            template
        );
    }
    
    public static CompiledTemplate compileNamed(String template) {
        if (template == null || template.isEmpty()) {
            return CompiledTemplate.EMPTY;
        }
        
        List<String> fragments = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<String> names = new ArrayList<>();
        
        StringBuilder currentFragment = new StringBuilder();
        int index = 0;
        
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            
            if (c == PLACEHOLDER_START) {
                int endPos = findPlaceholderEnd(template, i);
                if (endPos > i + 1) {
                    String paramName = template.substring(i + 1, endPos);
                    if (!paramName.isEmpty()) {
                        fragments.add(currentFragment.toString());
                        currentFragment.setLength(0);
                        
                        names.add(paramName);
                        indices.add(index++);
                        i = endPos;
                        continue;
                    }
                }
            }
            currentFragment.append(c);
        }
        fragments.add(currentFragment.toString());
        
        return new CompiledTemplate(
            fragments.toArray(new String[0]),
            indices.stream().mapToInt(Integer::intValue).toArray(),
            names.toArray(new String[0]),
            template
        );
    }
    
    private static int findPlaceholderEnd(String template, int start) {
        for (int i = start + 1; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == PLACEHOLDER_END) {
                return i;
            }
            if (!isValidParamChar(c)) {
                return -1;
            }
        }
        return -1;
    }
    
    private static boolean isValidParamChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }
}