package com.hisign.video.record_video;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.hisign.video.R;

/**
 * 描述：录视频
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/6/21
 */

public class RecordVideoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_video);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA},5);
        }
        if (savedInstanceState == null){
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new CameraFragment()).commit();
        }

    }
}
