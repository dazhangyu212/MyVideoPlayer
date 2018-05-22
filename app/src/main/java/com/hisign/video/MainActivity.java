package com.hisign.video;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.hisign.video.drawimage.CustomViewActivity;
import com.hisign.video.drawimage.ImageViewActivity;
import com.hisign.video.drawimage.SurfaceViewActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
        initListener();
    }

    private void initListener() {
        findViewById(R.id.btn_imageview_activity).setOnClickListener(this);
        findViewById(R.id.btn_surfaceview_activity).setOnClickListener(this);
        findViewById(R.id.btn_custom_view_activity).setOnClickListener(this);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()){
            case R.id.btn_imageview_activity:
                intent.setClass(this, ImageViewActivity.class);
                break;
            case  R.id.btn_surfaceview_activity:
                intent.setClass(this, SurfaceViewActivity.class);
                break;
            case R.id.btn_custom_view_activity:
                intent.setClass(this, CustomViewActivity.class);
                break;
        }
        startActivity(intent);
    }
}