package wang.unclecat.agora.utils;

import android.util.Log;

/**
 * NetPhone日志包装类,以NetPhone-为前缀
 *
 * @author 喵叔catuncle    19-12-28
 */
public final class Logger {

    private static final String TAG_PRE = "NetPhone-";

    public static void d(String message) {
        Log.d(TAG_PRE + tag(), message);
    }

    public static void e(String message) {
        Log.e(TAG_PRE + tag(), message);
    }

    public static void e(String message, Throwable throwable) {
        Log.e(TAG_PRE + tag(), message, throwable);
    }

    private static String tag() {
        final Throwable t = new Throwable();
        final StackTraceElement methodCaller = t.getStackTrace()[2];
        String className = methodCaller.getClassName();
        int i = className.lastIndexOf('.');
        return i > 0 ? className.substring(i + 1) : className;
    }
}
