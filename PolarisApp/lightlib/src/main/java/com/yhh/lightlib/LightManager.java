package com.yhh.lightlib;

import android.app.Activity;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LightManager {
    private String TAG = "LightManger";
    private StreamCameraBasicF cameraBasicF;
    private static LightManager lightManager = null;
    private static volatile Boolean initFlag = false;

    public static BufferedWriter GsWriter;
    File fGs;

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private LightManager(Activity activity) {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialize success");
        } else {
            Log.i(TAG, "OpenCV initialize failed");
        }

        cameraBasicF = new StreamCameraBasicF();
        cameraBasicF.setActivity(activity);
    }

    public static synchronized LightManager getInstance(Activity activity) {
        if (lightManager == null) {
            lightManager = new LightManager(activity);
        }
        return lightManager;
    }
    public static LightManager getInstance() {
        return lightManager;
    }

    public void setmTextureView(AutoFitTextureView textureView) {
        cameraBasicF.setmTextureView(textureView);
    }
    public void startTracking(LightIdCallback callback) throws IOException {
        if (!cameraBasicF.requestPermission()) {
            return;
        }
        if (initFlag) {
            return;
        }


        fGs=makeFilePath( "/sdcard/Gyt/", "data.txt");
        GsWriter = new BufferedWriter(new FileWriter(fGs.getAbsoluteFile(), false));

        cameraBasicF.start(callback);
        initFlag = true;
    }

    private File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }

    public void stopTracking() {
        if (initFlag) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    cameraBasicF.stop();
                    try {
                        GsWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    initFlag = false;
                }
            }).start();
        }
    }
}
