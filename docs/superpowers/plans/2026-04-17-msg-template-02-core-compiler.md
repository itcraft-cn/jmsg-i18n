# jmsg-i18n 实现计划 - Part 2: TemplateCompiler

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

---

## Task 04: TemplateCompiler.compileSimple()

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/core/TemplateCompiler.java`
- Test: `src/test/java/cn/itcraft/jmsg/core/TemplateCompilerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class TemplateCompilerTest {
    
    @Test
    public void testCompileSimpleNull() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple(null);
        assertEquals(CompiledTemplate.EMPTY, compiled);
    }
    
    @Test
    public void testCompileSimpleEmpty() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("");
        assertEquals(CompiledTemplate.EMPTY, compiled);
    }
    
    @Test
    public void testCompileSimpleNoPlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Hello World");
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Hello World", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
        assertFalse(compiled.hasParams());
        assertEquals("Hello World", compiled.getOriginalTemplate());
    }
    
    @Test
    public void testCompileSimpleSinglePlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Hello {}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("Hello ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
        assertEquals(0, compiled.getParamIndices()[0]);
        assertNull(compiled.getParamNames());
    }
    
    @Test
    public void testCompileSimpleMultiplePlaceholders() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{} + {} = {}");
        
        assertEquals(4, compiled.getFragments().length);
        assertEquals("", compiled.getFragments()[0]);
        assertEquals(" + ", compiled.getFragments()[1]);
        assertEquals(" = ", compiled.getFragments()[2]);
        assertEquals("", compiled.getFragments()[3]);
        assertEquals(3, compiled.getParamCount());
        assertEquals(0, compiled.getParamIndices()[0]);
        assertEquals(1, compiled.getParamIndices()[1]);
        assertEquals(2, compiled.getParamIndices()[2]);
    }
    
    @Test
    public void testCompileSimplePlaceholderAtEnd() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Value: {}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("Value: ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
    }
    
    @Test
    public void testCompileSimplePlaceholderAtStart() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{} is the value");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("", compiled.getFragments()[0]);
        assertEquals(" is the value", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
    }
    
    @Test
    public void testCompileSimpleConsecutivePlaceholders() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{}{}{}");
        
        assertEquals(4, compiled.getFragments().length);
        assertEquals("", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals("", compiled.getFragments()[2]);
        assertEquals("", compiled.getFragments()[3]);
        assertEquals(3, compiled.getParamCount());
    }
    
    @Test
    public void testCompileSimpleWithCurlyBraceInText() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("JSON: {key: {}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("JSON: {key: ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
    }
    
    @Test
    public void testCompileSimpleRealWorld() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple(
            "用户{}于{}登录成功，IP：{}");
        
        assertEquals(4, compiled.getFragments().length);
        assertEquals("用户", compiled.getFragments()[0]);
        assertEquals("于", compiled.getFragments()[1]);
        assertEquals("登录成功，IP：", compiled.getFragments()[2]);
        assertEquals("", compiled.getFragments()[3]);
        assertEquals(3, compiled.getParamCount());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=TemplateCompilerTest -q`
Expected: FAIL (TemplateCompiler class not found)

- [ ] **Step 3: Write minimal implementation**

```java
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
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=TemplateCompilerTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/TemplateCompiler.java
git add src/test/java/cn/itcraft/jmsg/core/TemplateCompilerTest.java
git commit -m "feat: add TemplateCompiler.compileSimple() for {} style templates"
```

---

## Task 05: TemplateCompiler.compileNamed()

**Files:**
- Modify: `src/main/java/cn/itcraft/jmsg/core/TemplateCompiler.java`
- Modify: `src/test/java/cn/itcraft/jmsg/core/TemplateCompilerTest.java`

- [ ] **Step 1: Add failing tests for compileNamed**

追加到 `TemplateCompilerTest.java`：

```java
    @Test
    public void testCompileNamedNull() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(null);
        assertEquals(CompiledTemplate.EMPTY, compiled);
    }
    
    @Test
    public void testCompileNamedEmpty() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("");
        assertEquals(CompiledTemplate.EMPTY, compiled);
    }
    
    @Test
    public void testCompileNamedNoPlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello World");
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Hello World", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
        assertNull(compiled.getParamNames());
    }
    
    @Test
    public void testCompileNamedSingleParam() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("Hello ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals(1, compiled.getParamCount());
        assertEquals("name", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedMultipleParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "User {userId} logged in at {time}");
        
        assertEquals(3, compiled.getFragments().length);
        assertEquals("User ", compiled.getFragments()[0]);
        assertEquals(" logged in at ", compiled.getFragments()[1]);
        assertEquals("", compiled.getFragments()[2]);
        assertEquals(2, compiled.getParamCount());
        assertEquals("userId", compiled.getParamNames()[0]);
        assertEquals("time", compiled.getParamNames()[1]);
    }
    
    @Test
    public void testCompileNamedParamWithUnderscore() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Value: {user_id}");
        
        assertEquals(1, compiled.getParamCount());
        assertEquals("user_id", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedParamWithDash() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Value: {user-name}");
        
        assertEquals(1, compiled.getParamCount());
        assertEquals("user-name", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedParamAtStart() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("{name} is here");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("", compiled.getFragments()[0]);
        assertEquals(" is here", compiled.getFragments()[1]);
        assertEquals("name", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedParamAtEnd() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("The value is {value}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals("The value is ", compiled.getFragments()[0]);
        assertEquals("", compiled.getFragments()[1]);
        assertEquals("value", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedInvalidPlaceholderNoClose() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Test {name");
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Test {name", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
    }
    
    @Test
    public void testCompileNamedInvalidPlaceholderWithSpace() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Test {name space}");
        
        assertEquals(1, compiled.getFragments().length);
        assertEquals("Test {name space}", compiled.getFragments()[0]);
        assertEquals(0, compiled.getParamCount());
    }
    
    @Test
    public void testCompileNamedEmptyPlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Test {}");
        
        assertEquals(2, compiled.getFragments().length);
        assertEquals(0, compiled.getParamCount());
    }
    
    @Test
    public void testCompileNamedWithSimplePlaceholder() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Test {} and {name}");
        
        assertEquals(3, compiled.getFragments().length);
        assertEquals(1, compiled.getParamCount());
        assertEquals("name", compiled.getParamNames()[0]);
    }
    
    @Test
    public void testCompileNamedRealWorld() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "用户{userId}于{time}在{location}执行了{action}操作，结果：{result}");
        
        assertEquals(7, compiled.getFragments().length);
        assertEquals(6, compiled.getParamCount());
        assertEquals("userId", compiled.getParamNames()[0]);
        assertEquals("time", compiled.getParamNames()[1]);
        assertEquals("location", compiled.getParamNames()[2]);
        assertEquals("action", compiled.getParamNames()[3]);
        assertEquals("result", compiled.getParamNames()[4]);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=TemplateCompilerTest -q`
Expected: FAIL (compileNamed method not found)

- [ ] **Step 3: Add compileNamed implementation**

追加到 `TemplateCompiler.java`：

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=TemplateCompilerTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/TemplateCompiler.java
git add src/test/java/cn/itcraft/jmsg/core/TemplateCompilerTest.java
git commit -m "feat: add TemplateCompiler.compileNamed() for {name} style templates"
```

---

## 完成检查

- [ ] Task 04-05 全部通过
- [ ] 运行全部core测试: `mvn test -Dtest=*TemplateCompiler*,*CompiledTemplate* -q`
- [ ] 推送到下个计划: 03-core-renderer.md