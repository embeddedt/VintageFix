package org.embeddedt.vintagefix.util;

public class ExceptionHelper {
    public static boolean isTypeInStackTrace(Throwable e, Class<? extends Throwable> clz) {
        while(e != null) {
            if(clz.isAssignableFrom(e.getClass()))
                return true;
            e = e.getCause();
        }
        return false;
    }
}
