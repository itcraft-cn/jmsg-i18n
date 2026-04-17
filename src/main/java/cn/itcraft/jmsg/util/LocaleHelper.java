package cn.itcraft.jmsg.util;

import java.util.Locale;

public final class LocaleHelper {
    
    private LocaleHelper() {}
    
    public static Locale parse(String localeCode) {
        if (localeCode == null || localeCode.trim().isEmpty()) {
            return Locale.getDefault();
        }
        
        String normalized = localeCode.replace("-", "_").trim();
        String[] parts = normalized.split("_");
        
        String language = parts[0].toLowerCase();
        
        if (!isValidLanguageCode(language)) {
            return Locale.getDefault();
        }
        
        String country = parts.length > 1 ? parts[1].toUpperCase() : "";
        if (!country.isEmpty() && !isValidCountryCode(country)) {
            country = "";
        }
        
        String variant = parts.length > 2 ? parts[2] : "";
        
        if (variant.isEmpty() && country.isEmpty()) {
            return new Locale(language);
        } else if (variant.isEmpty()) {
            return new Locale(language, country);
        } else {
            return new Locale(language, country, variant);
        }
    }
    
    private static boolean isValidLanguageCode(String code) {
        if (code.length() < 2 || code.length() > 3) {
            return false;
        }
        for (char c : code.toCharArray()) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean isValidCountryCode(String code) {
        if (code.length() != 2) {
            return false;
        }
        for (char c : code.toCharArray()) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }
    
    public static Locale parse(Locale locale) {
        return locale != null ? locale : Locale.getDefault();
    }
}