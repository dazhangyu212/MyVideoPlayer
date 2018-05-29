package com.hisign.video.mediamuser.utils;

import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/29
 */

public class MediaMuxerThread extends Thread{

    /**
     *
     */
    public static final String TAG = "MediaMuxerThread";

    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;

    private final Object lock = new Object();

    private static MediaMuxerThread mediaMuxerThread;

    private AudioEncodeThread audioThread;
    private VideoEncodeThread videoThread;

    private MediaMuxer mediaMuxer;
    private Vector<MuxerData> muxerDatas;

    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;

    private FileUtils fileSwapHelper;
    /**
     * 音轨添加状态
     */
    private volatile boolean isVideoTrackAdd;

    private volatile boolean isAudioTrackAdd;

    private volatile  boolean isExit = false;

    private class MuxerData {
        int trackIndex;
        ByteBuffer byteBuffer;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        public MuxerData(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuffer = byteBuffer;
            this.bufferInfo = bufferInfo;
        }
    }

    private MediaMuxerThread() {
    }

    /**
     * 开始音视频混合任务
     */
    public static void startMuxer(){
        if (mediaMuxerThread == null){
            synchronized (MediaMuxerThread.class){
                if (mediaMuxerThread == null ){
                    mediaMuxerThread = new MediaMuxerThread();
                    Log.i(TAG,"mediaMuxerThread.start();");
                    mediaMuxerThread.start();
                }
            }
        }
    }

    /**
     * 停止音视频混合任务
     */
    public static void stopMuxer(){
        if (mediaMuxerThread != null){
            mediaMuxerThread.exit();
        }
    }

    private void readyStart() throws IOException{
        fileSwapHelper.requestSwapFile(true);
        readyStart(fileSwapHelper.getNextFileName());
    }

    private void readyStart(String filePath) {
        isExit = false;
        isVideoTrackAdd = false;
        isAudioTrackAdd = false;
        muxerDatas.clear();
        try {
            mediaMuxer = new MediaMuxer(filePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (audioThread != null){
            audioThread.setMuxerReady(true);
        }
        if (videoThread != null){
            videoThread.setMuxerReady(true);
        }
        Log.e(TAG, "readyStart(String filePath, boolean restart) 保存至:" + filePath);
    }

    /**
     * 添加视频帧数据
     * @param data
     */
    public static void addVideoFrameData(byte[] data){
        if (mediaMuxerThread != null){
            mediaMuxerThread.addVideoData(data);
        }
    }

    private void addVideoData(byte[] data) {
        if (videoThread != null){
            videoThread.add(data);
        }
    }

    public void addMuxerData(MuxerData data){
        if (!isMuxerStart()){
            return;
        }
        muxerDatas.add(data);
        synchronized (lock){
            lock.notify();
        }
    }

    /**
     * 当前视频合成器是否运行了
     * @return
     */
    private boolean isMuxerStart() {
        return isAudioTrackAdd && isVideoTrackAdd;
    }

    private void exit() {
        if (videoThread != null){
            videoThread.exit();
            videoThread.join();
        }
        if (audioThread != null){
            audioThread.exit();
            audioThread.join();
        }
        isExit = true;
        synchronized (lock){
            lock.notify();
        }
    }
}
