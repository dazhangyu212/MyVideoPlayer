package com.hisign.video.mediamuser;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hisign.video.R;
import com.hisign.video.mediamuser.utils.MediaMuxerThread;
import com.hisign.video.widget.CameraView;

/**
 * 描述：MediaMuxer类API调用合成MP4
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/29
 */

public class MediaMuxerActivity extends AppCompatActivity implements Camera.PreviewCallback, CameraView.ISurfaceCallBack {
    CameraView cameraView;
    Camera camera;
    Button btnControl;

    FrameLayout fltContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mediamuxer);
        fltContainer = findViewById(R.id.flt_container);
        cameraView = new CameraView(this);
        int[] screenXY = getScreenWidthAndHeight();
        cameraView.getPreviewSize(screenXY);
        fltContainer.addView(cameraView);
        cameraView.setiSurfaceCallBack(this);
        btnControl = findViewById(R.id.btn_control);
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getTag().toString().equalsIgnoreCase("stop")) {
                    view.setTag("start");
                    ((TextView) view).setText("开始");
                    MediaMuxerThread.stopMuxer();
                    stopCamera();
                } else {
                    startCamera();
                    view.setTag("stop");
                    ((TextView) view).setText("停止");
                    MediaMuxerThread.startMuxer();
                }
            }
        });
        requirePermission();
    }

    private void requirePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "申请权限", Toast.LENGTH_SHORT).show();
            // 申请 相机 麦克风权限
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }

    /**
     * 启动摄像头
     */
    private void startCamera(){
        camera = cameraView.getCamera();
        //这个宽高的设置必须和后面编解码的设置一样，否则不能正常处理
//        parameters.setPreviewSize(1920,1080);
        camera.setPreviewCallback(this);
        camera.startPreview();
    }

    /**
     * 停止摄像头
     */
    private void stopCamera(){
        if (camera != null){
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
    /**
     * 获取屏幕宽度和高度
     *
     * @return int[0] is width;int[1] is height;
     */
    public static int[] getScreenWidthAndHeight() {
        int[] wh = new int[2];
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        int screenWidth = (int) (widthPixels);
        int screenHeight = (int) (heightPixels);
        wh[0] = screenWidth;
        wh[1] = screenHeight;
        return wh;
    }
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        MediaMuxerThread.addVideoFrameData(bytes);
    }

    @Override
    public void onCreate(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void onDestory(SurfaceHolder holder) {
        MediaMuxerThread.stopMuxer();
        stopCamera();
    }
}
