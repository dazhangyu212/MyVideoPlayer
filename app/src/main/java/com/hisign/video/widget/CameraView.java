package com.hisign.video.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * 拍照界面
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, OnTouchListener {
    private final String TAG = CameraView.class.getSimpleName();
    private Context context;
    private Size picutreSize;
    private Size previewSize;
    private SurfaceHolder surfaceHolder;
    private Parameters parameters;

    /**
     * 照片分辨率最大高度
      */
    public int maxPictureSizeHeight = 0;
    /**
     * 照片分辨率最大宽度
     */
    public int maxPictureSizeWidth = 0;
    /**
     * 预览图像最大高度
     */
    public int maxPreviewSizeHeight = 0;
    /**
     * 预览图像最大宽度
     */
    public int maxPreviewSizeWidth = 0;
    /**
     * 开始角度
     */
    public int degreeOriginal;
    /**
     * 旋转角度
     */
    public int degreeResult;

    /**
     * 相机控件
     */
    private Camera camera;

    /**
     * 设置对焦区域大小
     */
    private final float FOCUS_AREA_SIZE = 100f * getScreenDensity();

    /**
     * 缩放手势检测
     */
    private ScaleGestureDetector scaleGestureDetector;

    /**
     * 单击手势检测
     */
    private GestureDetector gestrDetector;

    private void implementGestureListener(Context context) {
        //单击手势检测
        gestrDetector = new GestureDetector(context, new SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent event) {
                Log.d(TAG, "onSingleTapConfirmed detected");
                setUpFocusAndMeteringArea(event);

                if (null != onSingleTapListener) {
                    onSingleTapListener.onSingleTap(event);
                }
                return false;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(context, new OnScaleGestureListener() {

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                Log.d(TAG, "onScale detected");
                if (detector.getScaleFactor() > 1.0f) {
                    GestureZoomIn();
                } else if (detector.getScaleFactor() < 1.0f) {
                    GestureZoomOut();
                }

                if (null != onChangeZoomListener) {
                    int zoomNum = camera.getParameters().getZoom();
                    onChangeZoomListener.onChangeZoom(zoomNum);
                }
                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                Log.d(TAG, "onScale detected2");
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
            }

        });
    }

    public CameraView(Context context) {
        super(context);
        this.context = context;
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setOnTouchListener(this);
        implementGestureListener(context);
    }

    private OnAutoFocusListener onAutoFocusListener;
    private OnSingleTapListener onSingleTapListener;
    private OnChangeZoomListener onChangeZoomListener;
    public OnTakePictureListener onTakePictureListener;
    private ISurfaceCallBack iSurfaceCallBack;

    public void setiSurfaceCallBack(ISurfaceCallBack iSurfaceCallBack) {
        this.iSurfaceCallBack = iSurfaceCallBack;
    }

    /**
     * 相机对焦结果监听
     */
    public interface OnAutoFocusListener {
        void onAutoFocus(boolean state);
    }

    /**
     * 单击控件监听
     */
    public interface OnSingleTapListener {
        void onSingleTap(MotionEvent event);
    }

    /**
     * 获取屏幕密度
     * @return
     */
    public static float getScreenDensity() {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return metrics.density;
    }
    
    /**
     * 相机变焦事件监听
     */
    public interface OnChangeZoomListener {
        void onChangeZoom(int currZoomNum);
    }

    /**
     * 拍照结果监听
     */
    public interface OnTakePictureListener {
        void OnTakePicture(byte[] image);
    }

    public interface ISurfaceCallBack{

        void onCreate(SurfaceHolder surfaceHolder);

        void onDestory(SurfaceHolder holder);
    }

    /**
     * 设置监听
     */
    public void setAutoFocusListener(OnAutoFocusListener onAutoFocusListener) {
        this.onAutoFocusListener = onAutoFocusListener;
    }

    public void setSingleTapListener(OnSingleTapListener onSingleTapListener) {
        this.onSingleTapListener = onSingleTapListener;
    }

    public void setChangeZoomListener(OnChangeZoomListener onChangeZoomListener) {
        this.onChangeZoomListener = onChangeZoomListener;
    }

    public void setTakePictureListener(OnTakePictureListener onTakePictureListener) {
        this.onTakePictureListener = onTakePictureListener;
    }

    /**
     * 得到系统camera对象
     */
    public Camera getCamera() {
        if (camera == null){
            camera = Camera.open();
        }
        return camera;
    }
    
    public Size getPictureSize(Size cameraParamters) {
        parameters = camera.getParameters();
        List<Size> pictureSizesList = parameters.getSupportedPictureSizes();
        if (pictureSizesList != null && pictureSizesList.size() > 0) {
            Collections.sort(pictureSizesList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    if (lhs.width - rhs.width == 0) {
                        return rhs.height - lhs.height;
                    } else {
                        return rhs.width - lhs.width;
                    }
                }
            });
            Size sizeStart=pictureSizesList.get(0);
            Size sizeEnd=pictureSizesList.get(pictureSizesList.size()-1);
            if (sizeStart.height > sizeEnd.height && sizeStart.width > sizeEnd.width) {
                float targetRatio = (float)cameraParamters.height / (float)cameraParamters.width;
//                int doubleMiddleLocation = pictureSizesList.size();
//                if (pictureSizesList.size() >= 4 && pictureSizesList.size() <= 18) {
//                    doubleMiddleLocation = pictureSizesList.size() * 2 / 3;
//                } else if (pictureSizesList.size() > 18){
//                    doubleMiddleLocation = pictureSizesList.size() * 3 / 4;
//                }
                double minDiff = Double.MAX_VALUE;
                for (int i = 0; i <pictureSizesList.size(); i++) {
                    Size size = pictureSizesList.get(i);
                    float scale = (float) size.height / (float) size.width;
                    if (Math.abs(scale - targetRatio) > 0.1) continue;

                    if (Math.abs(size.height - cameraParamters.height) < minDiff) {
                        maxPictureSizeHeight = pictureSizesList.get(i).height;
                        maxPictureSizeWidth = pictureSizesList.get(i).width;
                        picutreSize = pictureSizesList.get(i);
                        minDiff = Math.abs(size.height - cameraParamters.height);
                    }
//                    if (size.width >= screenXY[1] && Math.abs(scale - targetRatio) < 0.02f) {
//                        maxPictureSizeHeight = pictureSizesList.get(i).height;
//                        maxPictureSizeWidth = pictureSizesList.get(i).width;
//                        picutreSize = pictureSizesList.get(i);
//                        break;
//                    }
                }
            }
            if (picutreSize != null) {
                return picutreSize;
            } else {
                //判断数组开始分辨率是否小于接收分辨率，小于取数组1/4，大于取数组3/4

                int doubleMiddleLocation = pictureSizesList.size() / 4;
                if(pictureSizesList.size()<=18){
                    doubleMiddleLocation = pictureSizesList.size() / 3;
                }
                if (sizeStart.height > sizeEnd.height || sizeStart.width > sizeEnd.width) {
                    if (pictureSizesList.size() <= 18) {
                        doubleMiddleLocation = pictureSizesList.size() * 2 / 3;
                    } else {
                        doubleMiddleLocation = pictureSizesList.size() * 3 / 4;
                    }
                }
                maxPictureSizeHeight = pictureSizesList.get(doubleMiddleLocation).height;
                maxPictureSizeWidth = pictureSizesList.get(doubleMiddleLocation).width;
                picutreSize = pictureSizesList.get(doubleMiddleLocation);
            }
        }
        return picutreSize;
    }
    
    public Size getPreviewSize(int[] screenXY) {
        parameters = getCamera().getParameters();
        double targetRatio = (double) screenXY[0]/(double) screenXY[1];
        List<Size> previewSizesList = parameters.getSupportedPreviewSizes();
        if (previewSizesList != null && previewSizesList.size() > 0) {
            Iterator<Size> it = previewSizesList.iterator();
            double minDiff = Double.MAX_VALUE;
            while (it.hasNext()) {
                Size size = it.next();
                float ratio = (float) size.height / (float) size.width;
                if (Math.abs(ratio - targetRatio) > 0.1) continue;

                if (Math.abs(size.height - screenXY[1]) < minDiff) {
                    maxPreviewSizeHeight = size.height;
                    maxPreviewSizeWidth = size.width;
                    previewSize = size;
                    minDiff = Math.abs(size.height - screenXY[1]);
                }
            }
        }
        getPictureSize(previewSize);
        return previewSize;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("yang", "enter sufaceCreadted");
        try {
            if (camera == null) {
                camera = Camera.open(0);
            }
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            camera.release();
            camera = null;
        }
        if (iSurfaceCallBack != null){
            iSurfaceCallBack.onCreate(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            setUpParameters();
            setCameraDisplayOrientation((Activity) context, 0, camera);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            camera.release();
            camera = null;
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (iSurfaceCallBack != null){
            iSurfaceCallBack.onDestory(holder);
        }
    }

    private void setUpParameters() {
        parameters = camera.getParameters();

        // 设置zoomBar的最大刻度
        if (parameters.isZoomSupported()) {
            // zoomBar.setMax(parameters.getMaxZoom());
        }
        // 设置照片格式为JPEG
        parameters.setPictureFormat(ImageFormat.JPEG);
        // 设置照片质量
        parameters.setJpegQuality(100);
        // 设置EXIF缩略图质量
        parameters.setJpegThumbnailQuality(100);
        // 设置场景模式
        parameters.setSceneMode(Parameters.SCENE_MODE_AUTO);
        // 设置白平衡
        parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
        // 设置曝光补偿
        parameters.setExposureCompensation(0);
        // 设置反冲带
        parameters.setAntibanding(Parameters.ANTIBANDING_AUTO);
        // 设置颜色效果
        parameters.setColorEffect(Parameters.EFFECT_NONE);

        // 设置初始对焦模式为自动对焦模式
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else {
            // 若无自动对焦则设置为默认对焦模式
            parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        }

        // 设置初始闪光灯为自动
        // parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
        parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);

        parameters.setPictureSize(maxPictureSizeWidth, maxPictureSizeHeight);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        camera.setParameters(parameters);
        camera.setDisplayOrientation(90);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestrDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    public void setUpFocusAndMeteringArea(final MotionEvent event) {

        camera.cancelAutoFocus();
        parameters = camera.getParameters();
        if (parameters.getMaxNumFocusAreas() > 0) {
            // 对焦区域
            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1d);
            Camera.Area focusArea = new Camera.Area(focusRect, 1000);
            List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
            focusAreas.add(focusArea);
            // 设置对焦区域
            parameters.setFocusAreas(focusAreas);
            camera.autoFocus(new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, final Camera camera) {
                    if (success) {
                        // 对焦成功
                        //focus = FOCUS.SUCCESS;
                        onAutoFocusListener.onAutoFocus(true);
                    } else {
                        // 对焦失败
                        //focus = FOCUS.FAILED;
                        onAutoFocusListener.onAutoFocus(false);
                    }
                    // 对焦完毕后取消对焦框动画
                    // 触摸对焦完毕后5s之后变回自动对焦
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setUpContinuousFocus();
                        }
                    }, 5000);
                    // 延迟清空rectView，根据对焦是否成功提示框变色
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                        }
                    }, 250);

                }
            });
        }

        camera.setParameters(parameters);
    }

    /**
     * 计算点击区域大小 Camera.Area坐标系与常用坐标系不同： 左上角为(-1000, -1000)，右下角为(1000,
     * 1000)，屏幕中央为(0, 0)； 坐标不可超过(-1000, 1000)。
     *
     * @param x
     * @param y
     * @param coefficient
     * @return
     */
    private Rect calculateTapArea(double x, double y, double coefficient) {
        double areaSize = FOCUS_AREA_SIZE * coefficient;

        double areaX = x - areaSize / 2;
        double areaY = y - areaSize / 2;

        double left = clamp(areaX * 2000 / getWidth() - 1000, -1000, 1000 - areaSize);
        double top = clamp(areaY * 2000 / getHeight() - 1000, -1000, 1000 - areaSize);

        // Log.e("calculateTapArea", left + " " + top + " " + left + areaSize +
        // " " + top + areaSize);
        return new Rect((int) left, (int) top, (int) (left + areaSize), (int) (top + areaSize));
    }

    /**
     * 限定范围
     *
     * @param x
     * @param min
     * @param max
     * @return
     */
    private double clamp(double x, double min, double max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * 设置对焦模式，用于触摸对焦模式
     */
    public void setUpAutoFocus() {
        if (camera == null) {
            return;
        }
        parameters = camera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        }
        camera.setParameters(parameters);
    }

    /**
     * 设置对焦模式，自动对焦模式
     */
    private void setUpContinuousFocus() {
        if (camera == null) {
            return;
        }
        parameters = camera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else {
            // 若无自动对焦则设置为默认对焦模式
            setUpAutoFocus();
        }
        camera.setParameters(parameters);
    }

    /**
     * 连续拍照功能
     */
    public void takePhoto(int picNum) {
        snapTime = picNum;
        camera.takePicture(shutterCallback, null, pictureCallBack);
    }

    public void takePhoto() {
        takePhoto(1);
    }

    /**
     * 振动和声音回调接口
     */
    ShutterCallback shutterCallback = new ShutterCallback() {

        @Override
        public void onShutter() {
            // 添加拍照声音
            AudioManager mgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
        }

    };

    private int snapTime = 10;
    /**
     * 照相机回调接口
     */
    Camera.PictureCallback pictureCallBack = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, final Camera camera) {
            if (null != onTakePictureListener) {
                onTakePictureListener.OnTakePicture(data.clone());
            }
            Log.d(TAG, "snapTime: " + snapTime);
            new Thread() {
                @Override
                public void run() {
                    if (--snapTime > 0) {
                        camera.takePicture(null, null, pictureCallBack);
                    } else {
                        camera.startPreview();
                    }
                }
            }.start();
        }
    };


    /**
     * 当前缩放等级
     */
    private int currentZoomLevel = 0;

    /**
     * 张开双指，放大取景范围
     */
    private void GestureZoomIn() {
        if (camera == null) {
            return;
        }
        parameters = camera.getParameters();
        if (parameters.isZoomSupported()) {
            int MAX_ZOOM = parameters.getMaxZoom();
            if (currentZoomLevel < MAX_ZOOM) {
                currentZoomLevel++;
            }
            if (currentZoomLevel > MAX_ZOOM) {
                currentZoomLevel = MAX_ZOOM;
            }
            parameters.setZoom(currentZoomLevel);
            camera.setParameters(parameters);
        }
    }

    /**
     * 合拢双指，缩小取景范围
     */
    private void GestureZoomOut() {
        if (camera == null) {
            return;
        }
        parameters = camera.getParameters();
        if (parameters.isZoomSupported()) {
            if (currentZoomLevel > 0) {
                currentZoomLevel--;
            }
            if (currentZoomLevel < 0) {
                currentZoomLevel = 0;
            }
            parameters.setZoom(currentZoomLevel);
            camera.setParameters(parameters);
        }
    }


    /**
     * 设置拍照角度
     *
     * @param activity
     * @param cameraId
     * @param camera
     */
    private void setCameraDisplayOrientation(Activity activity,
                                             int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        degreeOriginal = degrees;
        degreeResult = result;

        Log.d("degreestime", "degrees: " + degrees);
        Log.d("degreestime", "result: " + result);
        Log.d("degreestime", "info.orientation: " + info.orientation);

        camera.setDisplayOrientation(result);
    }

}
