package com.hisign.video.drawimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.hisign.video.R;
import com.hisign.video.finalvalues.ConstPath;
import com.hisign.video.widget.CustomView;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/22
 */

public class CustomViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_view);
        CustomView customView = findViewById(R.id.custom);
        Bitmap bitmap = BitmapFactory.decodeFile(ConstPath.ROOT_PATH+"/DCIM/Screenshot.png");
        customView.setBitmap(bitmap);
    }
}
