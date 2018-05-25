package com.hisign.video.audio;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.hisign.video.R;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 描述：录音部分
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/25
 */

public class AudioActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnStart;
    private Button btnPause;
    private Button btnPcmList;
    private Button btnWavList;
    private AudioRecorder audioRecorder;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO,Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS},5);
        }
        init();
        addListener();
    }

    private void addListener() {
        btnStart.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnPcmList.setOnClickListener(this);
        btnWavList.setOnClickListener(this);
        findViewById(R.id.btn_play_pcm).setOnClickListener(this);
    }

    private void init() {
        btnStart=findViewById(R.id.start);
        btnPause=findViewById(R.id.pause);
        btnPcmList= findViewById(R.id.pcmList);
        btnWavList = findViewById(R.id.wavList);
        btnPause.setVisibility(View.GONE);
        audioRecorder = AudioRecorder.getInstance();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.start:
                startRecord();
                break;
            case R.id.pause:
                pauseRecord();
                break;
            case R.id.pcmList:
                Intent showPcmList = new Intent(this, AudioListActivity.class);
                showPcmList.putExtra("type", "pcm");
                startActivity(showPcmList);
                break;
            case R.id.wavList:
                Intent showWavList = new Intent(this, AudioListActivity.class);
                showWavList.putExtra("type", "wav");
                startActivity(showWavList);
                break;
            case R.id.btn_play_pcm:
                audioRecorder.playPcm();
                break;
        }
    }

    private void pauseRecord() {
        if (audioRecorder.getStatus() == AudioRecorder.Status.STATUS_START){
            audioRecorder.pauseRecord();
            btnPause.setText(R.string.str_continue_record);
        }else {
            audioRecorder.startRecorder(null);
            btnPause.setText(R.string.str_pause_record);
        }
    }

    private void startRecord() {
        if (audioRecorder.getStatus() == AudioRecorder.Status.STATUS_NOT_READY){
            String fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
            audioRecorder.createDefaultAudio(fileName);
            audioRecorder.startRecorder(null);
            btnStart.setText("停止录音");
            btnPause.setVisibility(View.VISIBLE);
        }else {
            audioRecorder.stopRecord();
            btnStart.setText("开始录音");
            btnPause.setText("停止录音");
            btnPause.setVisibility(View.GONE);
        }
    }
}
