package com.holybuckets.foundation.util;

import com.holybuckets.foundation.LoggerBase;

import java.util.HashMap;
import java.util.Map;

public class MixinManager {

    static final Map<String, Integer> ERROR_COUNT = new HashMap<>();
    static final Map<String, Boolean> IS_ENABLED = new HashMap<>();

    public static boolean isEnabled(String id) {
        if( IS_ENABLED.get(id) == null ) IS_ENABLED.put(id, true);
        return IS_ENABLED.get(id);
    }

    public static void reEnable(String id) {
        IS_ENABLED.put(id, true);
    }

    public static void recordError(String id, Exception e)
    {
        if( ERROR_COUNT.get(id) == null ) ERROR_COUNT.put(id, 0);
        int count = ERROR_COUNT.get(id)+1;
        ERROR_COUNT.put(id, count);

        StringBuilder err = new StringBuilder("Error in " + id);
        if( e != null ) {
            err.append(": ").append(e.getMessage()).append("\n");
            for (StackTraceElement element : e.getStackTrace()) {
                err.append("\n").append(element.toString());
            }
        }
        LoggerBase.logError(null,"999000", err.toString());

        if( count > 5 ) IS_ENABLED.put(id, false);
        if(!isEnabled(id)) {
            String msg = id +" disabled after several exceptions thrown, please restart " +
            "the server to resume normal processing and report this error";
                LoggerBase.logInfo(null,"999001",msg);
        }
    }

}
