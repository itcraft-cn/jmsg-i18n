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