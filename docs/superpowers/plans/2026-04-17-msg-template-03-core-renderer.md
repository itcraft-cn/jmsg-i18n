# jmsg-i18n 实现计划 - Part 3: Renderer

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

---

## Task 06: StringBuilderPool

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/util/StringBuilderPool.java`
- Test: `src/test/java/cn/itcraft/jmsg/util/StringBuilderPoolTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.util;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class StringBuilderPoolTest {
    
    @After
    public void tearDown() {
        StringBuilderPool.clear();
    }
    
    @Test
    public void testAcquireBasic() {
        StringBuilder sb = StringBuilderPool.acquire();
        
        assertNotNull(sb);
        assertEquals(0, sb.length());
    }
    
    @Test
    public void testAcquireWithSize() {
        StringBuilder sb = StringBuilderPool.acquire(500);
        
        assertNotNull(sb);
        assertTrue(sb.capacity() >= 500);
        assertEquals(0, sb.length());
    }
    
    @Test
    public void testAcquireReuse() {
        StringBuilder sb1 = StringBuilderPool.acquire();
        sb1.append("test content");
        String result1 = StringBuilderPool.releaseAndToString(sb1);
        
        StringBuilder sb2 = StringBuilderPool.acquire();
        
        assertEquals(0, sb2.length());
        assertEquals(sb1, sb2);
        assertEquals("test content", result1);
    }
    
    @Test
    public void testAcquireExpandCapacity() {
        StringBuilder sb1 = StringBuilderPool.acquire(100);
        int initialCapacity = sb1.capacity();
        
        StringBuilder sb2 = StringBuilderPool.acquire(500);
        
        assertTrue(sb2.capacity() >= 500);
    }
    
    @Test
    public void testReleaseAndToString() {
        StringBuilder sb = StringBuilderPool.acquire();
        sb.append("Hello").append(" ").append("World");
        
        String result = StringBuilderPool.releaseAndToString(sb);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testGetCurrentCapacity() {
        StringBuilderPool.acquire(100);
        int capacity = StringBuilderPool.getCurrentCapacity();
        
        assertTrue(capacity >= 256);
    }
    
    @Test
    public void testClear() {
        StringBuilder sb1 = StringBuilderPool.acquire(1000);
        int cap1 = sb1.capacity();
        
        StringBuilderPool.clear();
        
        StringBuilder sb2 = StringBuilderPool.acquire();
        int cap2 = sb2.capacity();
        
        assertTrue(cap2 < cap1 || cap2 == 256);
    }
    
    @Test
    public void testMultipleAcquiresSameThread() {
        StringBuilder sb1 = StringBuilderPool.acquire();
        sb1.append("first");
        
        StringBuilder sb2 = StringBuilderPool.acquire();
        sb2.append("second");
        
        assertEquals(sb1, sb2);
        assertEquals("second", sb2.toString());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=StringBuilderPoolTest -q`
Expected: FAIL (StringBuilderPool class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.util;

public final class StringBuilderPool {
    
    private static final int DEFAULT_INITIAL_CAPACITY = 256;
    
    private static final ThreadLocal<StringBuilder> pool = ThreadLocal.withInitial(
        () -> new StringBuilder(DEFAULT_INITIAL_CAPACITY)
    );
    
    private StringBuilderPool() {}
    
    public static StringBuilder acquire(int estimatedSize) {
        StringBuilder sb = pool.get();
        sb.setLength(0);
        if (estimatedSize > sb.capacity()) {
            sb.ensureCapacity(estimatedSize);
        }
        return sb;
    }
    
    public static StringBuilder acquire() {
        StringBuilder sb = pool.get();
        sb.setLength(0);
        return sb;
    }
    
    public static String releaseAndToString(StringBuilder sb) {
        return sb.toString();
    }
    
    public static int getCurrentCapacity() {
        return pool.get().capacity();
    }
    
    public static void clear() {
        pool.remove();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=StringBuilderPoolTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/util/StringBuilderPool.java
git add src/test/java/cn/itcraft/jmsg/util/StringBuilderPoolTest.java
git commit -m "feat: add ThreadLocal StringBuilderPool for performance optimization"
```

---

## Task 07: TemplateRenderer.renderSimple()

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/core/TemplateRenderer.java`
- Test: `src/test/java/cn/itcraft/jmsg/core/TemplateRendererTest.java`

- [ ] **Step 1: Write the failing test**

```java
package cn.itcraft.jmsg.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class TemplateRendererTest {
    
    @Test
    public void testRenderSimpleNoParams() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Hello World");
        
        String result = TemplateRenderer.renderSimple(compiled, null);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderSimpleEmpty() {
        CompiledTemplate compiled = CompiledTemplate.EMPTY;
        
        String result = TemplateRenderer.renderSimple(compiled, null);
        
        assertEquals("", result);
    }
    
    @Test
    public void testRenderSimpleSingleArg() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Hello {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{"World"});
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderSimpleMultipleArgs() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{} + {} = {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{1, 2, 3});
        
        assertEquals("1 + 2 = 3", result);
    }
    
    @Test
    public void testRenderSimpleArgNull() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Value: {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{null});
        
        assertEquals("Value: {}", result);
    }
    
    @Test
    public void testRenderSimpleNotEnoughArgs() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("{} + {} = {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{1});
        
        assertEquals("1 + {} = {}", result);
    }
    
    @Test
    public void testRenderSimpleTooManyArgs() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Value: {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{1, 2, 3});
        
        assertEquals("Value: 1", result);
    }
    
    @Test
    public void testRenderSimpleEmptyArgs() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("Value: {}");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{});
        
        assertEquals("Value: {}", result);
    }
    
    @Test
    public void testRenderSimpleChinese() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("用户{}于{}登录成功");
        
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{"admin", "2024-04-17"});
        
        assertEquals("用户admin于2024-04-17登录成功", result);
    }
    
    @Test
    public void testRenderSimpleObjectArg() {
        CompiledTemplate compiled = TemplateCompiler.compileSimple("The object is {}");
        
        Object obj = new TestObject("test", 123);
        String result = TemplateRenderer.renderSimple(compiled, new Object[]{obj});
        
        assertTrue(result.contains("TestObject"));
    }
    
    private static class TestObject {
        private String name;
        private int value;
        
        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
        
        @Override
        public String toString() {
            return "TestObject{name='" + name + "', value=" + value + "}";
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=TemplateRendererTest -q`
Expected: FAIL (TemplateRenderer class not found)

- [ ] **Step 3: Write minimal implementation**

```java
package cn.itcraft.jmsg.core;

import cn.itcraft.jmsg.util.StringBuilderPool;

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
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=TemplateRendererTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/TemplateRenderer.java
git add src/test/java/cn/itcraft/jmsg/core/TemplateRendererTest.java
git commit -m "feat: add TemplateRenderer.renderSimple() with StringBuilderPool"
```

---

## Task 08: TemplateRenderer.renderNamedMap()

**Files:**
- Modify: `src/main/java/cn/itcraft/jmsg/core/TemplateRenderer.java`
- Modify: `src/test/java/cn/itcraft/jmsg/core/TemplateRendererTest.java`

- [ ] **Step 1: Add failing tests for renderNamedMap**

追加到 `TemplateRendererTest.java`：

```java
    @Test
    public void testRenderNamedMapNoParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello World");
        
        String result = TemplateRenderer.renderNamedMap(compiled, null);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderNamedMapSingleParam() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("name", "World");
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderNamedMapMultipleParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "User {userId} logged in at {time}");
        
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("userId", "admin");
        args.put("time", "2024-04-17 10:30:00");
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("User admin logged in at 2024-04-17 10:30:00", result);
    }
    
    @Test
    public void testRenderNamedMapMissingParam() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("Hello {name}", result);
    }
    
    @Test
    public void testRenderNamedMapNullParam() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("name", null);
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("Hello {name}", result);
    }
    
    @Test
    public void testRenderNamedMapNullArgs() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        String result = TemplateRenderer.renderNamedMap(compiled, null);
        
        assertEquals("Hello {name}", result);
    }
    
    @Test
    public void testRenderNamedMapExtraArgs() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("name", "World");
        args.put("extra", "ignored");
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderNamedMapChinese() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "用户{userId}于{time}登录成功");
        
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("userId", "管理员");
        args.put("time", "2024-04-17");
        
        String result = TemplateRenderer.renderNamedMap(compiled, args);
        
        assertEquals("用户管理员于2024-04-17登录成功", result);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=TemplateRendererTest -q`
Expected: FAIL (renderNamedMap method not found)

- [ ] **Step 3: Add renderNamedMap implementation**

追加到 `TemplateRenderer.java`：

```java
    public static String renderNamedMap(CompiledTemplate compiled, 
                                         java.util.Map<String, Object> namedArgs) {
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
    
    private static int estimateNamedSize(String[] fragments, String[] paramNames,
                                          java.util.Map<String, Object> namedArgs) {
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=TemplateRendererTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/core/TemplateRenderer.java
git add src/test/java/cn/itcraft/jmsg/core/TemplateRendererTest.java
git commit -m "feat: add TemplateRenderer.renderNamedMap() for Map parameters"
```

---

## Task 09: TemplateRenderer.renderNamedBean() + ReflectCache

**Files:**
- Create: `src/main/java/cn/itcraft/jmsg/util/ReflectCache.java`
- Modify: `src/main/java/cn/itcraft/jmsg/core/TemplateRenderer.java`
- Modify: `src/test/java/cn/itcraft/jmsg/core/TemplateRendererTest.java`
- Test: `src/test/java/cn/itcraft/jmsg/util/ReflectCacheTest.java`

- [ ] **Step 1: Write ReflectCache tests first**

```java
package cn.itcraft.jmsg.util;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReflectCacheTest {
    
    @After
    public void tearDown() {
        ReflectCache.clear();
    }
    
    @Test
    public void testGetPropertyBasicGetter() {
        TestBean bean = new TestBean();
        bean.name = "test";
        
        Object value = ReflectCache.getProperty(bean, "name");
        
        assertEquals("test", value);
    }
    
    @Test
    public void testGetPropertyIsGetter() {
        TestBean bean = new TestBean();
        bean.active = true;
        
        Object value = ReflectCache.getProperty(bean, "active");
        
        assertEquals(true, value);
    }
    
    @Test
    public void testGetPropertyNullBean() {
        Object value = ReflectCache.getProperty(null, "name");
        
        assertNull(value);
    }
    
    @Test
    public void testGetPropertyNullPropertyName() {
        TestBean bean = new TestBean();
        
        Object value = ReflectCache.getProperty(bean, null);
        
        assertNull(value);
    }
    
    @Test
    public void testGetPropertyEmptyPropertyName() {
        TestBean bean = new TestBean();
        
        Object value = ReflectCache.getProperty(bean, "");
        
        assertNull(value);
    }
    
    @Test
    public void testGetPropertyNonExistent() {
        TestBean bean = new TestBean();
        
        Object value = ReflectCache.getProperty(bean, "nonexistent");
        
        assertNull(value);
    }
    
    @Test
    public void testGetPropertyCached() {
        ReflectCache.clear();
        
        TestBean bean = new TestBean();
        bean.name = "first";
        
        ReflectCache.getProperty(bean, "name");
        boolean cached = ReflectCache.contains(TestBean.class, "name");
        
        assertTrue(cached);
        
        TestBean bean2 = new TestBean();
        bean2.name = "second";
        
        Object value = ReflectCache.getProperty(bean2, "name");
        
        assertEquals("second", value);
    }
    
    @Test
    public void testGetMaxSize() {
        assertEquals(1024, ReflectCache.getMaxSize());
    }
    
    @Test
    public void testSetMaxSize() {
        ReflectCache.setMaxSize(500);
        
        assertEquals(500, ReflectCache.getMaxSize());
    }
    
    @Test
    public void testSetMaxSizeInvalid() {
        ReflectCache.setMaxSize(-100);
        
        assertEquals(1024, ReflectCache.getMaxSize());
    }
    
    @Test
    public void testClear() {
        TestBean bean = new TestBean();
        ReflectCache.getProperty(bean, "name");
        
        ReflectCache.clear();
        
        assertEquals(0, ReflectCache.size());
    }
    
    @Test
    public void testGetPropertyFromSuperclass() {
        ChildBean child = new ChildBean();
        child.name = "child";
        
        Object value = ReflectCache.getProperty(child, "name");
        
        assertEquals("child", value);
    }
    
    public static class TestBean {
        public String name;
        public boolean active;
        
        public String getName() { return name; }
        public boolean isActive() { return active; }
    }
    
    public static class ChildBean extends TestBean {
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ReflectCacheTest -q`
Expected: FAIL (ReflectCache class not found)

- [ ] **Step 3: Write ReflectCache implementation**

```java
package cn.itcraft.jmsg.util;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ReflectCache {
    
    private static final int DEFAULT_MAX_SIZE = 1024;
    
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Method>> cache = 
        new ConcurrentHashMap<>();
    
    private static volatile int maxSize = DEFAULT_MAX_SIZE;
    
    private ReflectCache() {}
    
    public static void setMaxSize(int size) {
        maxSize = size > 0 ? size : DEFAULT_MAX_SIZE;
    }
    
    public static int getMaxSize() {
        return maxSize;
    }
    
    public static Object getProperty(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isEmpty()) {
            return null;
        }
        
        Class<?> clazz = bean.getClass();
        Method getter = getGetterMethod(clazz, propertyName);
        
        if (getter == null) {
            return null;
        }
        
        try {
            return getter.invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Object getPropertyNoCache(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isEmpty()) {
            return null;
        }
        
        Method getter = findGetterMethod(bean.getClass(), propertyName);
        
        if (getter == null) {
            return null;
        }
        
        try {
            return getter.invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Method getGetterMethod(Class<?> clazz, String propertyName) {
        ConcurrentMap<String, Method> classCache = cache.computeIfAbsent(clazz, k -> {
            if (cache.size() >= maxSize) {
                evictOldest();
            }
            return new ConcurrentHashMap<>();
        });
        
        return classCache.computeIfAbsent(propertyName, name -> findGetterMethod(clazz, name));
    }
    
    private static Method findGetterMethod(Class<?> clazz, String propertyName) {
        String capitalizedName = capitalize(propertyName);
        
        Method getter = tryGetMethod(clazz, "get" + capitalizedName);
        if (getter != null && isAccessibleGetter(getter)) {
            return getter;
        }
        
        getter = tryGetMethod(clazz, "is" + capitalizedName);
        if (getter != null && isBooleanGetter(getter)) {
            return getter;
        }
        
        getter = tryGetMethod(clazz, propertyName);
        if (getter != null && isAccessibleGetter(getter)) {
            return getter;
        }
        
        return null;
    }
    
    private static Method tryGetMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            try {
                Method method = clazz.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ex) {
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null && superClass != Object.class) {
                    return tryGetMethod(superClass, methodName);
                }
                return null;
            }
        }
    }
    
    private static boolean isAccessibleGetter(Method method) {
        return method.getParameterCount() == 0 && method.getReturnType() != void.class;
    }
    
    private static boolean isBooleanGetter(Method method) {
        Class<?> returnType = method.getReturnType();
        return method.getParameterCount() == 0 && 
               (returnType == Boolean.class || returnType == boolean.class);
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    private static void evictOldest() {
        Iterator<Class<?>> it = cache.keySet().iterator();
        if (it.hasNext()) {
            it.next();
            it.remove();
        }
    }
    
    public static void clear() {
        cache.clear();
    }
    
    public static int size() {
        return cache.size();
    }
    
    public static boolean contains(Class<?> clazz, String propertyName) {
        ConcurrentMap<String, Method> classCache = cache.get(clazz);
        return classCache != null && classCache.containsKey(propertyName);
    }
}
```

- [ ] **Step 4: Run ReflectCache tests**

Run: `mvn test -Dtest=ReflectCacheTest -q`
Expected: PASS

- [ ] **Step 5: Add renderNamedBean tests**

追加到 `TemplateRendererTest.java`：

```java
    @Test
    public void testRenderNamedBeanNoParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello World");
        
        Object bean = new Object();
        String result = TemplateRenderer.renderNamedBean(compiled, bean);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderNamedBeanNullBean() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        String result = TemplateRenderer.renderNamedBean(compiled, null);
        
        assertEquals("Hello {name}", result);
    }
    
    @Test
    public void testRenderNamedBeanSingleParam() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        RenderTestBean bean = new RenderTestBean();
        bean.name = "World";
        
        String result = TemplateRenderer.renderNamedBean(compiled, bean);
        
        assertEquals("Hello World", result);
    }
    
    @Test
    public void testRenderNamedBeanMultipleParams() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed(
            "User {userId} logged in at {time}");
        
        LoginBean bean = new LoginBean();
        bean.userId = "admin";
        bean.time = "2024-04-17 10:30:00";
        
        String result = TemplateRenderer.renderNamedBean(compiled, bean);
        
        assertEquals("User admin logged in at 2024-04-17 10:30:00", result);
    }
    
    @Test
    public void testRenderNamedBeanMissingProperty() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Hello {name}");
        
        Object bean = new Object();
        
        String result = TemplateRenderer.renderNamedBean(compiled, bean);
        
        assertEquals("Hello {name}", result);
    }
    
    @Test
    public void testRenderNamedBeanBooleanProperty() {
        CompiledTemplate compiled = TemplateCompiler.compileNamed("Status: {active}");
        
        BooleanBean bean = new BooleanBean();
        bean.active = true;
        
        String result = TemplateRenderer.renderNamedBean(compiled, bean);
        
        assertEquals("Status: true", result);
    }
    
    public static class RenderTestBean {
        public String name;
        
        public String getName() { return name; }
    }
    
    public static class LoginBean {
        public String userId;
        public String time;
        
        public String getUserId() { return userId; }
        public String getTime() { return time; }
    }
    
    public static class BooleanBean {
        public boolean active;
        
        public boolean isActive() { return active; }
    }
```

- [ ] **Step 6: Add renderNamedBean implementation**

追加到 `TemplateRenderer.java`：

```java
    public static String renderNamedBean(CompiledTemplate compiled, Object bean) {
        if (!compiled.hasParams()) {
            return compiled.getFragments()[0];
        }
        
        if (bean == null) {
            return renderOriginal(compiled);
        }
        
        if (!cn.itcraft.jmsg.core.MsgTemplateConfig.isReflectCacheEnabled()) {
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
```

- [ ] **Step 7: Run all tests**

Run: `mvn test -Dtest=TemplateRendererTest,ReflectCacheTest -q`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/cn/itcraft/jmsg/util/ReflectCache.java
git add src/test/java/cn/itcraft/jmsg/util/ReflectCacheTest.java
git add src/main/java/cn/itcraft/jmsg/core/TemplateRenderer.java
git add src/test/java/cn/itcraft/jmsg/core/TemplateRendererTest.java
git commit -m "feat: add ReflectCache and TemplateRenderer.renderNamedBean() for bean parameters"
```

---

## 完成检查

- [ ] Task 06-09 全部通过
- [ ] 运行全部util和core测试: `mvn test -Dtest=*StringBuilderPool*,*ReflectCache*,*TemplateRenderer* -q`
- [ ] 推送到下个计划: 04-core-template.md