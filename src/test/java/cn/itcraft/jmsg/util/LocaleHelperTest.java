package cn.itcraft.jmsg.util;

import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class LocaleHelperTest {
    
    @Test
    public void testParseLanguageOnly() {
        Locale locale = LocaleHelper.parse("zh");
        
        assertEquals("zh", locale.getLanguage());
        assertEquals("", locale.getCountry());
    }
    
    @Test
    public void testParseLanguageCountry() {
        Locale locale = LocaleHelper.parse("zh_CN");
        
        assertEquals("zh", locale.getLanguage());
        assertEquals("CN", locale.getCountry());
    }
    
    @Test
    public void testParseWithDash() {
        Locale locale = LocaleHelper.parse("en-US");
        
        assertEquals("en", locale.getLanguage());
        assertEquals("US", locale.getCountry());
    }
    
    @Test
    public void testParseLanguageCountryVariant() {
        Locale locale = LocaleHelper.parse("en_US_POSIX");
        
        assertEquals("en", locale.getLanguage());
        assertEquals("US", locale.getCountry());
        assertEquals("POSIX", locale.getVariant());
    }
    
    @Test
    public void testParseNull() {
        Locale locale = LocaleHelper.parse((String) null);
        
        assertEquals(Locale.getDefault(), locale);
    }
    
    @Test
    public void testParseEmpty() {
        Locale locale = LocaleHelper.parse("");
        
        assertEquals(Locale.getDefault(), locale);
    }
    
    @Test
    public void testParseInvalid() {
        Locale locale = LocaleHelper.parse("invalid_format$$$");
        
        assertEquals(Locale.getDefault(), locale);
    }
    
    @Test
    public void testParseToLowercase() {
        Locale locale = LocaleHelper.parse("ZH-cn");
        
        assertEquals("zh", locale.getLanguage());
        assertEquals("CN", locale.getCountry());
    }
    
    @Test
    public void testParsePtBR() {
        Locale locale = LocaleHelper.parse("pt_BR");
        
        assertEquals("pt", locale.getLanguage());
        assertEquals("BR", locale.getCountry());
    }
    
    @Test
    public void testParseRuRU() {
        Locale locale = LocaleHelper.parse("ru_RU");
        
        assertEquals("ru", locale.getLanguage());
        assertEquals("RU", locale.getCountry());
    }
    
    @Test
    public void testParseLocaleObject() {
        Locale input = Locale.JAPANESE;
        Locale locale = LocaleHelper.parse(input);
        
        assertEquals(Locale.JAPANESE, locale);
    }
    
    @Test
    public void testParseNullLocaleObject() {
        Locale locale = LocaleHelper.parse((Locale) null);
        
        assertEquals(Locale.getDefault(), locale);
    }
}