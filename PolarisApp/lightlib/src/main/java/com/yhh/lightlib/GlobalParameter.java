package com.yhh.lightlib;

import android.content.Context;

public class GlobalParameter {//全局参数类

    public static double MaxY=0;
    public static double MinY=100;
    public static double LampTrackThreshold = 20;//灯轨阈值

    public static final long MICRO_SECOND = 1000;//微秒
    public static final long MILLI_SECOND = MICRO_SECOND * 1000;//毫秒
    public static final long ONE_SECOND = MILLI_SECOND * 1000;//秒

    public static int byte2int(byte b) {//把byte转换为int
        return 0x00 << 24 | b & 0xff;
    }
    public static String getDir(Context context) {//返回存储路径（全路径）
        return context.getFilesDir().getAbsolutePath();
    }
}
