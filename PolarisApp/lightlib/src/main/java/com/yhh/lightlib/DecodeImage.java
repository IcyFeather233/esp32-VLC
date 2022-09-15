package com.yhh.lightlib;

import android.app.Activity;

import org.opencv.core.Rect;

import java.io.BufferedWriter;//字符缓冲输出流，通过字符数组来缓冲数据
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static com.yhh.lightlib.GlobalParameter.getDir;


public class DecodeImage {
//图片解码

    long frameNumber;

    //用于自动解调的全局参数
    int HeaderTH_W = 0;
    int WhiteTH_W = 0;
    static String DemodOut = " ";
    static int GsTH = 0;
    static int ReDemod = 2;  //重复解调计数
    static boolean StopDemod = false;

    static int[] DemID1p = new int[20];
    static int[] DemID2p = new int[20];
    static int DemID1pcnt = 0;
    static int DemID2pcnt = 0;
    static int DemID = 0;
    static int[] HeaderGs = new int[500];
    static int[] HeaderIdx = new int[500];
    static int[] DemIDTemp = new int[2];
    private Activity activity;

    //当缓冲区满或者调用它的flush()函数时，
    //它就会将缓冲区的数据写入到输出流中
    BufferedWriter mWriter;
    BufferedWriter mWriterGs;

    //将byte型转换为int型
    private int byte2int(byte b) {
        return 0x00 << 24 | b & 0xff;
    }

    public DecodeImage(Activity activity)
    {
        this.activity = activity;
        File f = new File(getDir(activity) + "/LY/");
        File fGs;
        mWriter = null;


          String   timestamp  ="Test";


        if (!f.exists())
            f.mkdirs();

        f = new File(getDir(activity) + "/LY/" + "band.txt");
        fGs = new File(getDir(activity) + "/LY/" + "GS" + timestamp + ".txt");

        try {//读进缓冲区
            mWriter = new BufferedWriter(new FileWriter(f.getAbsoluteFile(), false));
            mWriterGs = new BufferedWriter(new FileWriter(fGs.getAbsoluteFile(), false));

        } catch (IOException e) {

        }
    }

    public void stopDecodeImage() {//结束解码

        try {
            mWriter.close();
            mWriterGs.close();
        } catch (IOException e) {
            e.printStackTrace();//在命令行打印异常信息在程序中出错的位置及原因
        }
    }

    public static int[] findMaxMin(ArrayList<Integer> ALin) {//找边界值
        int[] result = new int[4];

        result[0] = 0;
        result[1] = 100;
        for (int i = 1; i < ALin.size(); i++) {
            //	System.out.print (GrayValue[i]+" ");
            if (ALin.get(i) > result[0]) {
                result[0] = ALin.get(i);//更新下界
            }

            if (ALin.get(i) < result[1]) {
                result[1] = ALin.get(i);//更新上界
            }
        }

        return result;
    }

    public int DecodeImage(byte[] bytes, ArrayList<Rect> rect, int w, long frameNumber) throws IOException {


        int lightID = 0;

        long t = System.currentTimeMillis();
        for (int i = 0; i < rect.size(); i++) {
            lightID = ExtractBand(rect.get(i), bytes, w, i);
            if (lightID != 0) {
                break;
            }
        }


        return lightID;
    }

    ArrayList<Integer> GrayValue = new ArrayList<Integer>();//存灰度值，用来计算lightID
    static ArrayList<Character> GsHorL = new ArrayList<Character>();
    static ArrayList<Integer> GsWidth = new ArrayList<Integer>();
    static ArrayList<Integer> GsIndex = new ArrayList<Integer>();

    private int ExtractBand(Rect rect, byte[] buffer, int width, int rectIndex)
    {
        /*
           1. 提取光带
      */
        boolean b = true;
        ArrayList<Integer> Band = new ArrayList<Integer>();
        ArrayList<Character> Color = new ArrayList<Character>();

        int mark = -1;
        int threshold;



        mark = (int) (rect.tl().x+(rect.br().x-rect.tl().x)/14);


        int mean = 0;

        int maxgrayscale=0;//最大灰度值

        GrayValue.clear();
        System.out.println("(int) rect.tl().y=" + (int) rect.tl().y + " rect.br().y=" + rect.br().y);
        //打印了矩形上下的Y值，Y值是下边的更大（可以理解成坐标原点在平面的左上角，y轴向下为正向，x轴向右为正向）

        for (int j = (int) rect.tl().y; j < rect.br().y; j++) {
            mean = byte2int(buffer[mark + j * width]);

            GrayValue.add(mean);

            if(maxgrayscale<mean)//更新最大灰度值
            {
                maxgrayscale=mean;
            }

            try {
                mWriterGs.write(mean + " ");   // 写灰度值
                LightManager.GsWriter.write(mean + " ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        try {
            mWriterGs.write("\r\n");
            LightManager.GsWriter.write("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        int LightID=0;

        LightID=DemodulateImage.DemodImage(GrayValue);//计算lightID

       if(LightID!=0) {
            try {
                mWriter.close();
                mWriterGs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return LightID;

    }


}
