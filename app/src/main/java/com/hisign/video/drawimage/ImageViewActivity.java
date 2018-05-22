package com.hisign.video.drawimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.hisign.video.R;
import com.hisign.video.finalvalues.ConstPath;

import java.io.File;

/**
 * 描述：音视频学习第一步,用ImageView显示本地图片
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/22
 */

public class ImageViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitv_imageview);
        ImageView ivBitmap = findViewById(R.id.iv_bitmap);
        File file = new File(ConstPath.ROOT_PATH+"/DCIM/Screenshot.png");
        boolean isExist = file.exists();
        Bitmap bitmap = BitmapFactory.decodeFile(ConstPath.ROOT_PATH+"/DCIM/Screenshot.png");
        ivBitmap.setImageBitmap(bitmap);
    }
}
