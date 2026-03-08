package src.counterstrike.Version;

import java.security.SecureRandom;

public class MathUtils
{
    private static SecureRandom random;
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    
    public static String randomString(final int len) {
        final char[] chars = new char[len];
        for (int i = 0; i < len; ++i) {
            chars[i] = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(MathUtils.random.nextInt("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length()));
        }
        return new String(chars);
    }
    
    public static SecureRandom random() {
        return MathUtils.random;
    }
    
    public static int randomRange(final int start, final int end) {
        return start + MathUtils.random.nextInt(end - start + 1);
    }
    
    public static double toDegrees(final double value) {
        return (value > 179.9) ? (-180.0 + (value - 179.9)) : value;
    }
    
    public static int abs(final int value) {
        return (value < 0) ? (-value) : value;
    }
    
    static {
        MathUtils.random = new SecureRandom();
    }
}
