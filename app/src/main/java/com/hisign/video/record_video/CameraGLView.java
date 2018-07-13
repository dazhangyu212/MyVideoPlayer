package com.hisign.video.record_video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.hisign.video.record_video.encoder.MediaVideoEncoder;
import com.hisign.video.record_video.glutils.GLDrawer2D;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 描述：照相机预览控件
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/6/21
 */

public final class CameraGLView extends GLSurfaceView{

    private static final boolean DEBUG = true;

    private static final String TAG = "CameraGLView";

    private static final int CAMERA_ID = 0;

    private static final int SCALE_STRETCH_FIT = 0;

    private static final int SCALE_KEEP_ASPECT_VIEWPORT = 1;

    private static final int SCALE_KEEP_ASPECT = 2;

    private static final int SCALE_CROP_CENTER = 3;

    private CameraSurfaceRender mRender;

    private boolean mHasSurface;

    private CameraHandler mCameraHandler = null;

    private int mVideoWidth,mVideoHeight;

    private int mRotation;

    private int mScaleMode = SCALE_STRETCH_FIT;


    public CameraGLView(Context context) {
        this(context,null);
    }

    public CameraGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (DEBUG) Log.i(TAG, "CameraGLView: ");
        //GLES 2.0 ,API >= 8
         mRender = new CameraSurfaceRender(this);
        setEGLContextClientVersion(2);
        /*
        the frequency of refreshing of camera preview is at most 15 fps
		and RENDERMODE_WHEN_DIRTY is better to reduce power consumption
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); */
        setRenderer(mRender);
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.i(TAG, "onResume: ");
        super.onResume();
        if (mHasSurface){
            if (mCameraHandler == null){
                if (DEBUG) Log.i(TAG, "surface already exist");
                startPreview(getWidth(),getHeight());
            }
        }
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.i(TAG, "onPause: ");
        if (mCameraHandler != null){
            mCameraHandler.stopPreview(false);
        }
        super.onPause();
    }

    public void setScaleMode(final int mode){
        if (mScaleMode != mode){
            mScaleMode = mode;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRender.updateViewport();
                }
            });
        }
    }

    private void setVideoSize(int width, int height) {
        if (mRotation % 180 == 0){
            mVideoWidth = width;
            mVideoHeight = height;

        }else {
            mVideoWidth = height;
            mVideoHeight = width;
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRender.updateViewport();
            }
        });
    }

    public int getVideoWidth (){
        return mVideoWidth;
    }

    public int getmVideoHeight(){
        return mVideoHeight;
    }

    public SurfaceTexture getSurfaceTexture(){
        if (DEBUG) Log.i(TAG, "getSurfaceTexture: ");
        return mRender != null?mRender.mSTexture:null;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (DEBUG) Log.i(TAG, "surfaceDestroyed: ");
        if (mCameraHandler != null){
            //等待结束预览,否则照相机将发生异常
            mCameraHandler.stopPreview(true);
        }
        mCameraHandler = null;
        mHasSurface = false;
        mRender.onSurfaceDestroyed();
        super.surfaceDestroyed(holder);

    }

    public void setVideoEncoder(final MediaVideoEncoder encoder){
        if (DEBUG) Log.i(TAG, "setVideoEncoder:tex_id=" + mRender.hText + ",encoder=" + encoder);
        queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (mRender){
                    if (encoder != null){
                        encoder.setEglContext(EGL14.eglGetCurrentContext(),mRender.hText);
                    }
                    mRender.mVideoEncoder = encoder;
                }
            }
        });
    }

    //*******************************************************************************
    private synchronized void startPreview(final int width, final int height) {
        if (mCameraHandler == null) {
            final CameraThread thread = new CameraThread(this);
            thread.start();
            mCameraHandler = thread.getHandler();
        }
        mCameraHandler.startPreview(1280,720);
    }

    private class CameraSurfaceRender implements Renderer,SurfaceTexture.OnFrameAvailableListener {
        private WeakReference<CameraGLView> mWeakParent;
        public SurfaceTexture mSTexture;
        public int hText;
        private GLDrawer2D mDrawer;
        public MediaVideoEncoder mVideoEncoder;
        private final float[] mStMatrix = new float[16];
        private final float[] mMvpMatrix = new float[16];

        public CameraSurfaceRender(final CameraGLView parent) {
            if (DEBUG) Log.i(TAG, "CameraSurfaceRender: ");
            mWeakParent = new WeakReference<CameraGLView>(parent);
            Matrix.setIdentityM(mMvpMatrix,0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            if (DEBUG) Log.i(TAG, "onSurfaceCreated: ");
            //this render required OES_EGL_IMAGE_EXTERNAL
            final String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
            if (!extensions.contains("OES_EGL_image_external")){
                throw new RuntimeException("This system does not support OES_EGL_image_external.");
            }
            //create texture ID
            hText = GLDrawer2D.initTex();
            //create surfaceTexture with texture ID;
            mSTexture= new SurfaceTexture(hText);
            mSTexture.setOnFrameAvailableListener(this);
            //clear screen with yellow color so that you can see rendering rectangle
            GLES20.glClearColor(1.0f,1.0f,1.0f,1.0f);
            final CameraGLView cameraGLView = mWeakParent.get();
            if (cameraGLView != null){
                cameraGLView.mHasSurface = true;
            }
            //create object for preview display
            mDrawer = new GLDrawer2D();
            mDrawer.setMatrix(mMvpMatrix,0);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            if (DEBUG) Log.i(TAG, String.format("onSurfaceChanged:(%d,%d)",width,height));
            //if at least with or height is zero,initialization of this view is still progress.
            if ((width == 0) || (height == 0)){
                return;
            }
            updateViewport();
            CameraGLView parent = mWeakParent.get();
            if (parent != null){
                parent.startPreview(width,height);
            }
        }

        /**
         * when GLSurface context is soon destroyed
         */
        public void onSurfaceDestroyed() {
            if (DEBUG) Log.i(TAG, "onSurfaceDestroyed: ");
            if (mDrawer != null){
                mDrawer.release();
                mDrawer = null;
            }
            if (mSTexture != null){
                mSTexture.release();
                mSTexture = null;
            }
            GLDrawer2D.deleteTex(hText);
        }

        private volatile  boolean requestUpdateTex = false;
        private boolean flip = true;
        /**
         * drawing to GLSurface
         * we set renderMode to GLSurfaceView.RENDERMODE_WHEN_DIRTY,
         * this method is only called when #requestRender is called(= when texture is required to update)
         * if you don't set RENDERMODE_WHEN_DIRTY, this method is called at maximum 60fps
         */
        @Override
        public void onDrawFrame(GL10 gl10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            if (requestUpdateTex){
                requestUpdateTex = false;
                //update texture(came from camera)
                mSTexture.updateTexImage();
                //get texture matrix
                mSTexture.getTransformMatrix(mStMatrix);
            }
            //draw to preview screen
            mDrawer.draw(hText,mStMatrix);
            flip = !flip;
            if (flip){
                synchronized (this){
                    if (mVideoEncoder != null){
                        /*
                        notify to capturing thread that the camera frame is available.
						mVideoEncoder.frameAvailableSoon(mStMatrix);
                         */
                        mVideoEncoder.frameAvailableSoon(mStMatrix,mMvpMatrix);
                    }
                }
            }
        }

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            requestUpdateTex = true;

        }

        public void updateViewport() {
            final CameraGLView parent = mWeakParent.get();
            if (parent != null){
                final int view_width = parent.getWidth();
                final int view_height = parent.getHeight();
                GLES20.glViewport(0,0,view_width,view_height);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                final double video_width = parent.mVideoWidth;
                final double video_height = parent.mVideoHeight;
                if (video_width == 0|| video_height == 0){
                    return;
                }
                Matrix.setIdentityM(mMvpMatrix,0);
                final double view_aspect = view_width/view_height;
                Log.i(TAG, String.format("updateViewport: view(%d,%d)%f,video(%1.0f,%1.0f)", view_width, view_height, view_aspect, video_width, video_height));
                switch (parent.mScaleMode){
                    case SCALE_STRETCH_FIT:
                        break;
                    case SCALE_KEEP_ASPECT_VIEWPORT:
                    {
                        final double req = video_width / video_height;
                        int x, y;
                        int width, height;
                        if (view_aspect > req) {
                            //if view is wider than camera image ,, calc width of drawing area based on view height
                            y = 0;
                            height = view_height;
                            width = (int) (req * view_height);
                            x = (view_width - height) / 2;

                        } else {
                            // if view is higher than camera image,calc height of drawing area based on view width
                            x = 0;
                            width = view_width;
                            height = (int) (view_width / req);
                            y = (view_height - height) / 2;
                        }
                        //set viewport to draw keeping aspect ration of camera image
                        Log.i(TAG, "updateViewport: " + String.format("xy(%d,%d),size(%d,%d)", x, y, width, height));
                        GLES20.glViewport(x, y, width, height);
                    }
                    break;
                    case SCALE_KEEP_ASPECT:
                    case SCALE_CROP_CENTER:
                    {
                        double scale_x = view_width/video_width;
                        double scale_y = view_height/video_height;
                        double scale = (parent.mScaleMode== SCALE_CROP_CENTER
                                ? Math.max(scale_x,  scale_y) : Math.min(scale_x, scale_y));
                        double width = scale * video_width;
                        double height = scale*video_height;
                        Log.v(TAG, String.format("size(%1.0f,%1.0f),scale(%f,%f),mat(%f,%f)",
                                width, height, scale_x, scale_y, width / view_width, height / view_height));
                        Matrix.scaleM(mMvpMatrix, 0, (float)(width / view_width), (float)(height / view_height), 1.0f);
                    }
                    break;
                }
                if (mDrawer != null){
                    mDrawer.setMatrix(mMvpMatrix,0);
                }
            }
        }

    }

    private class CameraHandler extends Handler {
        private static final int MSG_PREVIEW_START = 1;
        private static final int MSG_PREVIEW_STOP = 2;
        private CameraThread mThread;

        public CameraHandler(CameraThread thread) {
            mThread = thread;
        }

        public void startPreview(int width, int height) {
            sendMessage(obtainMessage(MSG_PREVIEW_START, width, height));
        }

        public void stopPreview(boolean needWait) {
            synchronized (this){
                sendEmptyMessage(MSG_PREVIEW_STOP);
                if (needWait && mThread.mIsRunning){
                    try {
                        if (DEBUG){
                            Log.d(TAG, "stopPreview: wait for terminating of camera thread");
                        }
                        wait();
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_PREVIEW_START:
                    mThread.startPreview(msg.arg1,msg.arg2);
                    break;
                case MSG_PREVIEW_STOP:
                    mThread.stopPreview();
                    synchronized (this){
                        notifyAll();
                    }
                    Looper.myLooper().quit();
                    mThread = null;
                    break;
                default:
                    throw new RuntimeException("unknown message:what= "+msg.what);
            }
        }
    }

    /**
     * 异步操作相机
     */
    private class CameraThread extends Thread {
        private final Object mReadyFence = new Object();
        private WeakReference<CameraGLView> mWeakParent;
        private CameraHandler handler;
        public volatile boolean mIsRunning = false;
        private Camera mCamera;
        private boolean mIsFrontFace;

        public CameraThread(CameraGLView parent) {
            super("Camera thread");
            mWeakParent = new WeakReference<CameraGLView>(parent);
        }

        public CameraHandler getHandler() {
            synchronized (mReadyFence){
                try {
                    mReadyFence.wait();
                } catch (InterruptedException e) {
                    Log.i(TAG, "getHandler: "+e.getMessage());
                }
            }
            return handler;
        }

        @Override
        public void run() {
            if (DEBUG){
                Log.i(TAG, "run: Camera Thread");
            }
            Looper.prepare();
            synchronized (mReadyFence){
                handler = null;
                mIsRunning = false;
            }
        }

        /**
         * 开始预览
         * @param width
         * @param height
         */
        public void startPreview(int width, int height) {
            if (DEBUG) {
                Log.i(TAG, "startPreview: ");
            }
            final CameraGLView parent =  mWeakParent.get();
            if (parent != null && mCamera == null){
                mCamera = Camera.open(CAMERA_ID);
                Camera.Parameters params = mCamera.getParameters();
                List<String> focusModes = params.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                    params.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
                }else {
                    if (DEBUG) Log.i(TAG, "Camera does not support autofocus");
                }
                //可以试试最快的帧率.接近60fps,你的手机很快便会发烫.
                final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();
//					final int n = supportedFpsRange != null ? supportedFpsRange.size() : 0;
//					int[] range;
//					for (int i = 0; i < n; i++) {
//						range = supportedFpsRange.get(i);
//						Log.i(TAG, String.format("supportedFpsRange(%d)=(%d,%d)", i, range[0], range[1]));
//					}
                final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size()-1);
                Log.i(TAG, "startPreview: "+String.format("fps:%d-%d",max_fps[0],max_fps[1]));
                params.setPreviewFpsRange(max_fps[0],max_fps[1]);
                params.setRecordingHint(true);
                //预览尺寸
                final Camera.Size closestSize = getClosestSupportedSize(params.getSupportedPreviewSizes(),width,height);
                params.setPreviewSize(closestSize.width,closestSize.height);
                Camera.Size pictureSize = getClosestSupportedSize(params.getSupportedPictureSizes(),width,height);
                params.setPictureSize(pictureSize.width,pictureSize.height);
                //根据设备朝向旋转相机预览画面
                setRotation(params);
                mCamera.setParameters(params);
//                获取实际预览尺寸
                final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                Log.i(TAG, "startPreview: "+String.format("previewSize(%d, %d)", previewSize.width, previewSize.height));
                // adjust view size with keeping the aspect ration of camera preview.
                // here is not a UI thread and we should request parent view to execute.
                parent.post(new Runnable() {
                    @Override
                    public void run() {
                        parent.setVideoSize(previewSize.width,previewSize.height);
                    }
                });
                SurfaceTexture st = parent.getSurfaceTexture();
                st.setDefaultBufferSize(previewSize.width,previewSize.height);
                try {
                    mCamera.setPreviewTexture(st);
                } catch (IOException e) {
                    Log.e(TAG, "startPreview: ",e );
                    if (mCamera != null){
                        mCamera.release();
                        mCamera = null;
                    }
                }
                if (mCamera != null){
                    mCamera.startPreview();
                }
            }
        }



        private Camera.Size getClosestSupportedSize(List<Camera.Size> supportedPreviewSizes, final int requestedWidth, final int requestedHeight) {
            Camera.Size minSize = Collections.min(supportedPreviewSizes, new Comparator<Camera.Size>() {

                @Override
                public int compare(Camera.Size lhs, Camera.Size rhs) {
                    return diff(lhs)-diff(rhs);
                }

                private int diff(Camera.Size size){
                    return Math.abs(requestedWidth-size.width)+ Math.abs(requestedHeight - size.height);

                }
            });
            return minSize;
        }

        public void stopPreview() {
            if (DEBUG) Log.i(TAG, "stopPreview: ");
            if (mCamera != null){
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }

        private void setRotation(Camera.Parameters params) {
            if (DEBUG) Log.i(TAG, "setRotation: ");
            CameraGLView parent = mWeakParent.get();
            if (parent == null){
                return;
            }
            final Display display = ((WindowManager) parent.getContext()
                    .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            final int rotation = display.getRotation();
            int degrees = 0;
            switch (rotation){
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
            //get whether the camera is front or back camera
            final Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(CAMERA_ID,info);
            mIsFrontFace = (info.orientation == Camera.CameraInfo.CAMERA_FACING_FRONT);
            if (mIsFrontFace){// front camera
                degrees = (info.orientation +degrees)%360;
                degrees = (360-degrees)%360;
            }else {//back camera
                degrees = (info.orientation - degrees+360)%360;

            }
            //apply rotation setting
            mCamera.setDisplayOrientation(degrees);
            parent.mRotation = degrees;
            //xxx this method fails to call and camera stop
//            params.setRotation(degrees);
        }
    }
}
