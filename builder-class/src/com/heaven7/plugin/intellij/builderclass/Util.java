package com.heaven7.plugin.intellij.builderclass;


public class Util {

    public static void logError(Object... objs) {
        log(true, objs);
    }
    public static void log(Object... objs) {
        log(false, objs);
    }
    private static void log(boolean error, Object... objs) {
        StringBuilder sb = new StringBuilder();
        if (objs != null) {
            for (Object obj : objs) {
                sb.append(obj != null ? obj.toString() : null);
                sb.append("\r\n");
            }
        }
        if (error) {
            System.err.print(sb.toString());
        } else {
            System.out.print(sb.toString());
        }
    }
    public static void logNewLine(){
        System.out.println("================================================\r\n");
    }

}
