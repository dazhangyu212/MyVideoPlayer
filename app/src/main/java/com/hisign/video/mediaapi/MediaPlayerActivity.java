package com.hisign.video.mediaapi;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.hisign.video.R;
import com.hisign.video.finalvalues.ConstPath;
import com.hisign.video.mediaapi.mediaplayer.IPlayerCallBack;
import com.hisign.video.mediaapi.mediaplayer.MediaPlayer;
import com.hisign.video.mediaapi.mediaplayer.PlayerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 描述：MediaExtractor和MediaCodec播放视频的使用
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/28
 */

public class MediaPlayerActivity extends AppCompatActivity implements IPlayerCallBack {
    private MediaPlayer mediaPlayer;
    private PlayerView playerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        playerView = findViewById(R.id.player_view);
        ToggleButton btnControl = findViewById(R.id.btn_control);
        init();
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer == null){
                    Toast.makeText(MediaPlayerActivity.this,"初始化中,请稍候",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                }else {
                    mediaPlayer.play();
                }
            }
        });
    }

    private void init() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String film = initData();
                mediaPlayer = new MediaPlayer(playerView.getHolder().getSurface(),film);
                mediaPlayer.setCallBack(MediaPlayerActivity.this);
            }
        }).start();
    }

    /**
     * 将raw中的资源文件,拷贝到指定文件夹下
     * @return
     */
    private String initData() {
        File dir = new File(ConstPath.VIDEO_DIRECTORY_PATH);
        if (!dir.exists()){
            dir.mkdirs();
        }
        File path = new File(dir,"shape.mp4");
        final InputStream in = getResources().openRawResource(R.raw.shape_of_my_heart);
        final FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(path);
            byte[] buf = new byte[1024];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int size = 0;
            while ((size = in.read(buf)) != -1){
                outputStream.write(buf,0,size);
            }
            byte[] bs = outputStream.toByteArray();
            fileOutputStream.write(bs);
            outputStream.close();
            in.close();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path.toString();
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null){
            mediaPlayer.destory();
        }
        super.onDestroy();
    }

    @Override
    public void videoAspect(final int width, final int height, float time) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playerView.setAspect((float)width/height);
            }
        });
    }
}
