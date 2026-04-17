# jmsg-i18n 实现计划 - Part 5: Util

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

---

## Task 12: LocaleHelper

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/util/LocaleHelper.java`
- Test: `src/test/java/cn/itcraft/jmsg/util/LocaleHelperTest.java`

- [ ] **Step 1: Write the failing test**

```java
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
        Locale locale = LocaleHelper.parse(null);
        
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
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=LocaleHelperTest -q`
Expected: FAIL (LocaleHelper class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.util;

import java.util.Locale;

public final class LocaleHelper {
    
    private LocaleHelper() {}
    
    public static Locale parse(String localeCode) {
        if (localeCode == null || localeCode.trim().isEmpty()) {
            return Locale.getDefault();
        }
        
        try {
            String normalized = localeCode.replace("-", "_").trim();
            String[] parts = normalized.split("_");
            
            String language = parts[0].toLowerCase();
            String country = parts.length > 1 ? parts[1].toUpperCase() : "";
            String variant = parts.length > 2 ? parts[2] : "";
            
            if (variant.isEmpty() && country.isEmpty()) {
                return new Locale(language);
            } else if (variant.isEmpty()) {
                return new Locale(language, country);
            } else {
                return new Locale(language, country, variant);
            }
        } catch (Exception e) {
            return Locale.getDefault();
        }
    }
    
    public static Locale parse(Locale locale) {
        return locale != null ? locale : Locale.getDefault();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=LocaleHelperTest -q`
Expected: PASS

- [ ] **Step 5: Update MsgTemplateConfig to use LocaleHelper**

修改 `MsgTemplateConfig.java`：

```java
    public static void setDefaultLocale(String localeCode) {
        if (localeCode != null) {
            defaultLocale = cn.itcraft.jmsg.util.LocaleHelper.parse(localeCode);
        }
    }
```

- [ ] **Step 6: Run tests**

Run: `mvn test -Dtest=LocaleHelperTest,MsgTemplateConfigTest -q`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/util/LocaleHelper.java
git add src/test/java/cn/itcraft/jmsg/util/LocaleHelperTest.java
git add src/main/java/cn/itcraft/jmsg/core/MsgTemplateConfig.java
git commit -m "feat: add LocaleHelper for locale parsing"
```

---

## 完成检查

- [ ] Task 12 通过
- [ ] 运行全部util测试: `mvn test -Dtest=*LocaleHelper*,*StringBuilderPool*,*ReflectCache* -q`
- [ ] 推送到下个计划: 06-loader.md