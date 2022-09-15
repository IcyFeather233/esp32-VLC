
package com.yhh.lightlib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static androidx.core.app.ActivityCompat.requestPermissions;
import static com.yhh.lightlib.GlobalParameter.LampTrackThreshold;
import static com.yhh.lightlib.GlobalParameter.MICRO_SECOND;
import static com.yhh.lightlib.GlobalParameter.MILLI_SECOND;
import static com.yhh.lightlib.GlobalParameter.MaxY;
import static com.yhh.lightlib.GlobalParameter.MinY;
import static com.yhh.lightlib.GlobalParameter.ONE_SECOND;
import static com.yhh.lightlib.GlobalParameter.byte2int;
import static com.yhh.lightlib.GlobalParameter.getDir;

public class StreamCameraBasicF implements SurfaceHolder.Callback {
    private Activity activity;
    private LightIdCallback lightIdCallback;
    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * 从屏幕旋转到 JPEG 方向的转换
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    public static Integer mShortSensitivy =300;
    public static boolean BeginDecode = true;
    public static boolean AutoISO = true;
    public static boolean RearCamera=true;  //默认是后置摄像头
    private static boolean TAKE = false;

    int maxImages = 51;//缓存的最大帧数
    byte[] bytes;//存储图片的像素矩阵
    int RectNumber = 0;
    Handler mUiHandler;
    Mat binaryImage;

    boolean ShowRectFlag = true;
    byte[] data = new byte[1];

    private long mShortExposure = ONE_SECOND / 6000;
    private long mFrameDuration = ONE_SECOND / 30;
    private int area_threshold = 5000;
    private int height_threshold = 100;
    private String fileName;

    private long frameNumber = -1;
    private ArrayList<Rect> RECT = new ArrayList<Rect>();
    private ArrayList<Rect> lastRECT = new ArrayList<Rect>();


    private String mCameraId;


    private CameraCaptureSession mCaptureSession;


    private CameraDevice mCameraDevice;


    private Size mPreviewSize;


    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            System.gc();
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage()));
        }

    };

    private ImageReader mImageReader;

    private CaptureRequest.Builder mPreviewRequestBuilder;


    private long StartTime;
    private Surface mPreviewSurface;

    private ArrayList<CaptureRequest> mPreviewRequest = new ArrayList<CaptureRequest>(2);


    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {


        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            // process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            frameNumber = result.getFrameNumber();//获取该CaptureResult对应的Frame Number

            if (frameNumber >= 10000) frameNumber = 0;

            Log.i("CameraCaptureSession", "get a new frame= " + frameNumber);

            long exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
            String exposureText;
            if (exposureTime > ONE_SECOND) {
                exposureText = String.format("%.2f s", exposureTime / 1e9);
            } else if (exposureTime > MILLI_SECOND) {
                exposureText = String.format("%.2f ms", exposureTime / 1e6);
            } else if (exposureTime > MICRO_SECOND) {
                exposureText = String.format("%.2f us", exposureTime / 1e3);
            } else {
                exposureText = String.format("%d ns", exposureTime);
            }


        }

    };


    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            //当相机打开时调用此方法，在这里开始相机预览
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
            mCameraOpenCloseLock.release();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };


    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * 阻止应用程序在关闭相机之前退出。
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * 一个额外的线程，用于运行那些不应该阻塞用户界面的任务。
     */
    private HandlerThread mBackgroundThread;


    private Handler mBackgroundHandler;


    public static Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mResultView.append((String) msg.obj);
            int offset = mResultView.getLineCount() * mResultView.getLineHeight();

            if (offset > mResultView.getHeight()) {
                mResultView.scrollTo(0, offset - mResultView.getHeight());
            }
        }
    };


    public static Handler mMessageHandler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mResultMessage.setText((String) msg.obj);
        }
    };


    private TextView mPExposureText;
    private TextView mExposureText;
    private TextView mPSeninfoText;
    private TextView mSeninfoText;
    private TextView mTresholdText;
    private TextView mRectinfoText;



    private AutoFitTextureView mTextureView;
    private ImageView mImageView;
    public static TextView mResultMessage;
    private static TextView mResultView;

    private SlidingPaneLayout slidingPaneLayout;

    private DecodeImage mDecodeImage;



    public void onResume() {
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public void onPause() {
        closeCamera();
        stopBackgroundThread();
    }

    public void onStop() {
        closeCamera();
        stopBackgroundThread();


    }


    public static void showResult(String text) {

        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler.sendMessage(message);
    }


    public static void showResultMessage(String text) {

        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler1.sendMessage(message);
    }


    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {

        //收集支持的分辨率，至少要和预览的Surface一样大
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        //假设我们找到了，挑选其中最小的一个
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static StreamCameraBasicF newInstance() {
        StreamCameraBasicF fragment = new StreamCameraBasicF();
        return fragment;
    }


    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);


                //使用前置或后置摄像头
                if(RearCamera) {


                    if (characteristics.get(CameraCharacteristics.LENS_FACING)
                            == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue;
                    }
                }else {


                    if (characteristics.get(CameraCharacteristics.LENS_FACING)
                            == CameraCharacteristics.LENS_FACING_BACK) {
                        continue;
                    }
                }
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                //对于静态图像的捕捉，我们使用最大的可用尺寸
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(1920, 1440, ImageFormat.YUV_420_888, maxImages);
                //图片格式是yuv



                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                //试图使用过大的预览尺寸可能会超出相机总线的带宽限制，导致华丽的预览，但存储的是垃圾捕获数据
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);
                Log.i("size=", " " + mPreviewSize.getWidth() + " x " + mPreviewSize.getHeight());

                // 我们将TextureView的长宽比与我们挑选的预览的尺寸相适应

                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            //目前，当使用Camera2API但不支持该代码运行的设备时，会抛出一个NPE。
        }
    }


    private void openCamera(int width, int height) {
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            //为相机添加权限并让用户授予权限
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }


    private void closeCamera() {
        BeginDecode = false;

        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }


    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    private void stopBackgroundThread() {
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
        }
            try {
                if(mBackgroundThread != null) {
                    mBackgroundThread.join();
                    mBackgroundThread = null;
                }

                if(mBackgroundHandler != null) {
                    mBackgroundHandler = null;
                }

                if (mDecodeImage != null)
                    mDecodeImage.stopDecodeImage();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();

            assert texture != null;


            //我们将默认缓冲区的大小配置为我们想要的摄像机预览的大小。
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());


            //这就是我们需要开始预览的输出Surface。


            Surface mPreviewSurface = new Surface(texture);


            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.addTarget(mPreviewSurface);
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());



            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {

                            if (null == mCameraDevice) {
                                return;
                            }


                            mCaptureSession = cameraCaptureSession;
                            setPreviewBurst();

                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            showResult("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    private void takePicture() {
        TAKE = true;

    }

    public void setPreviewBurst() {

        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);



        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, mShortSensitivy);
        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, mFrameDuration);

        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mShortExposure);
        mPreviewRequest.set(0, mPreviewRequestBuilder.build());

        mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mShortExposure);
        mPreviewRequest.set(1, mPreviewRequestBuilder.build());
        try {

            mCaptureSession.setRepeatingBurst(mPreviewRequest, mCaptureCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public Boolean setPreviewISO(Integer ISOvalue)
    {

        try {
            mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, ISOvalue);

            mPreviewRequest.set(0, mPreviewRequestBuilder.build());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }

        if(mCaptureSession != null) {
            try {

                mCaptureSession.setRepeatingBurst(mPreviewRequest, mCaptureCallback, mBackgroundHandler);

            } catch (CameraAccessException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }


    private void SetLampTrackThreshold() {
        for (int j = 0; j < bytes.length; j++) {
            int temp = byte2int(bytes[j]);
            if (MaxY < temp)
                MaxY = temp;
            else if (MinY > temp)
                MinY = temp;

        }
        LampTrackThreshold = (int) (MaxY * 0.5);


    }

    private void ExtractLamp(int h, int w) {
        if (ShowRectFlag)
        {

        }

        long t = System.currentTimeMillis();

        binaryImage = new Mat(h, w, CvType.CV_8UC1);//MAT，图像容器，构建指定大小和类型的二维矩阵，这里类型是8位无符号的灰度图片
        binaryImage.put(0, 0, bytes);//将刚刚提取的图片的明度数据放进去
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();//轮廓
        Mat mHierarchy = new Mat();//
        int scale = 8;//比例

        t = System.currentTimeMillis();
        Mat down_binaryImage_4 = new Mat(binaryImage.height() / (scale), binaryImage.width() / (scale), CvType.CV_8UC1);
        Imgproc.resize(binaryImage, down_binaryImage_4, down_binaryImage_4.size(), 0, 0, Imgproc.INTER_NEAREST);

        t = System.currentTimeMillis();


        org.opencv.core.Size size = new org.opencv.core.Size(1, 7);
        Imgproc.GaussianBlur(down_binaryImage_4, down_binaryImage_4, size, 1, 50);
        Imgproc.blur(down_binaryImage_4, down_binaryImage_4, size);

        t = System.currentTimeMillis();
        Mat show = down_binaryImage_4.clone();


        Imgproc.threshold(down_binaryImage_4, down_binaryImage_4, LampTrackThreshold, 255, Imgproc.THRESH_BINARY);
        Imgproc.findContours(down_binaryImage_4, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        t = System.currentTimeMillis();



        if (RECT != null)
            RECT.clear();
        RectNumber = 0;

        for (int k = 0; k < contours.size(); k++) {
            Rect r1 = Imgproc.boundingRect(contours.get(k));

            if (r1.area() < area_threshold / (scale ^ 2) || r1.height < height_threshold / scale)
                continue;


            t = System.currentTimeMillis();

            double[] d = {scale * r1.x, (double) scale * r1.y, (double) scale * r1.width, (double) scale * r1.height};

            r1.set(d);

            RECT.add(r1);
        }


        if (ShowRectFlag) {



            for (Rect r1 : RECT) {
                Point tl = new Point(r1.tl().x / scale, r1.tl().y / scale);
                Point br = new Point(r1.br().x / scale, r1.br().y / scale);

                Imgproc.rectangle(show, tl, br, new Scalar(255, 255, 255), 3);
            }
            try {
                File f = new File(getDir(activity)+ "/LY/");
                if (!f.exists())
                    f.mkdirs();
                fileName = getDir(activity) +//.getPath()
                        "/LY/rectangle" + ".png";
                Imgcodecs.imwrite(fileName, show);
                Log.i("ImageSaver", "  write done");
            } finally {

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ALPHA_8;
                final Bitmap myBitmap = BitmapFactory.decodeFile(fileName);



            }

            ShowRectFlag = false;
        }


    }


    public void onClick(View view) {

        Log.i("ImageView01 ", "click: " + BeginDecode);
        if (slidingPaneLayout != null) {
            slidingPaneLayout.closePane();
        }

        mDecodeImage = new DecodeImage(activity);
        takePicture();
        ShowRectFlag = true;
        BeginDecode = true;
        mResultView.setText("Decode Process\n");
        mResultMessage.setText("");

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mPreviewSurface = holder.getSurface();

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mPreviewSurface = null;
    }


    private class ImageSaver implements Runnable {

        private final Image mImage;
        private final long mframeNumber;
        int h, w;

        public ImageSaver(Image image) {
            mImage = image;
            mframeNumber = frameNumber;

        }

        @Override
        public void run() {
            mDecodeImage = new DecodeImage(activity);
            ShowRectFlag = true;

            if (BeginDecode)
            {
                BeginDecode =true;

                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();//获取图片明度
                bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                h = mImage.getHeight();
                w = mImage.getWidth();


                mImage.close();
                takePicture();


                if (!setPreviewISO(mShortSensitivy)) {
                    BeginDecode = false;
                    return;
                }

                ExtractLamp(h, w);

                try {
                    int lightID = 0;
                    lightID = mDecodeImage.DecodeImage(bytes, RECT, w, mframeNumber);
                    if (lightID != 0) {
                        lightIdCallback.run(lightID);
                        BeginDecode = false;
                    } else {
                        DemodulateImage.StopDemod = false;
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }


                if (((!lastRECT.isEmpty()) && lastRECT != null) && (RECT.isEmpty() || RECT == null)) {
                    RECT = lastRECT;
                }
                lastRECT = RECT;


            } else
                mImage.close();
        }

    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setmTextureView(AutoFitTextureView mTextureView) {
        this.mTextureView = mTextureView;
    }

    public void init() {
        DemodulateImage.StopDemod = true;
        mDecodeImage = new DecodeImage(activity);
        BeginDecode = false;
        onResume();
    }
    public void start(LightIdCallback lightIdCallback) {
        mPreviewRequest.clear();
        mPreviewRequest.add(null);
        mPreviewRequest.add(null);
        init();
        this.lightIdCallback = lightIdCallback;
        DemodulateImage.StopDemod = false;
        BeginDecode = true;
    }
    public void stop() {
        BeginDecode = false;
        onStop();
    }
    public Boolean requestPermission() {

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(activity, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }
}
