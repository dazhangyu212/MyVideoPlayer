package com.hisign.video.cameraapi;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.hisign.video.R;

import java.io.IOException;

/**
 * 描述：用SurfaceView 预览Camera
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/25
 */

public class SurfaceViewActivity extends AppCompatActivity {
    SurfaceView surfaceView;
    private Camera camera;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surfaceview);
        surfaceView = findViewById(R.id.surfaceview);
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        initListener();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    camera.setPreviewDisplay(surfaceHolder);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                camera.release();
            }
        });
    }

    private void initListener() {
        Camera.Parameters parameters = camera.getParameters();
        /*
        Android 中Google支持的 Camera Preview Callback的YUV常用格式有两种：
        一个是NV21，一个是YV12。Android一般默认使用YCbCr_420_SP的格式（NV21）。
         */
        parameters.setPreviewFormat(ImageFormat.NV21);
        camera.setParameters(parameters);
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
//                这里面的Bytes的数据就是NV21格式的数据。
            }
        });
    }
}
