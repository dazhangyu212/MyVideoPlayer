package com.hisign.video.audio;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.hisign.video.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/25
 */

public class AudioListActivity extends AppCompatActivity {
    ListView listView;
    List<File> list  = new ArrayList<>();
    FileListAdapter adapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_list);
        initView();
    }

    private void initView() {
        listView = findViewById(R.id.listView);
        if ("pcm".equals(getIntent().getStringExtra("type"))){
            list = FileUtils.getPcmFiles();
        }else {
            list = FileUtils.getWavFiles();
        }
        adapter= new FileListAdapter(this,list);
        listView.setAdapter(adapter);
    }
}
