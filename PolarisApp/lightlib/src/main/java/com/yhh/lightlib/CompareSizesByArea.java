package com.yhh.lightlib;

import android.util.Size;//以像素为单位描述宽高尺寸的不可变类

import java.util.Comparator;


 //用来防止溢出
public class CompareSizesByArea implements Comparator<Size> {

    @Override
    public int compare(Size lhs, Size rhs) {
        return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                (long) rhs.getWidth() * rhs.getHeight());
        //两个面积相减，如果是负数就返回-1，0返回0，正数返回1
    }

}