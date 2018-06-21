package com.hisign.video.mediamuser.utils;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
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
    public static final String TAG = FileUtils.DIR_TAG+"Muxer";

    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;
    private static int photoWidth;
    private static int photoHeight;

    private final Object lock = new Object();

    private static MediaMuxerThread mediaMuxerThread;

    private AudioEncodeThread audioThread;
    private VideoEncoderThread videoThread;

    private MediaMuxer mediaMuxer;
    private Vector<MuxerData> muxerDatas;

    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    /**
     * 视频宽
     */
    private static int videoWidth;
    /**
     * 视频高
     */
    private static int videoHeight;

    private FileUtils fileSwapHelper;
    /**
     * 音轨添加状态
     */
    private volatile boolean isVideoTrackAdd;

    private volatile boolean isAudioTrackAdd;

    private volatile  boolean isExit = false;

    private MediaMuxerThread() {
    }

    /**
     * 开始音视频混合任务
     */
    public static void startMuxer(int width,int height,int picWidth,int picHeight){
        if (mediaMuxerThread == null){
            synchronized (MediaMuxerThread.class){
                if (mediaMuxerThread == null ){
                    mediaMuxerThread = new MediaMuxerThread();
                    Log.i(TAG,"mediaMuxerThread.start();");
                    mediaMuxerThread.start();
                }
            }
        }
        videoWidth = width;
        videoHeight = height;
        photoWidth = picWidth;
        photoHeight = picHeight;
    }

    /**
     * 停止音视频混合任务
     */
    public static void stopMuxer(){
        if (mediaMuxerThread != null){
            mediaMuxerThread.exit();
            try {
                mediaMuxerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaMuxerThread = null;
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
            Log.e(TAG, "readyStart: media fail to init", e);
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
     * 添加视频/音频轨
     * @param index
     * @param mediaFormat
     */
    public synchronized void addTrackIndex(int index, MediaFormat mediaFormat) {
        if (isMuxerStart()){
            return;
        }

        /* 如果已经添加了,就不做处理了 */
        if ((index == TRACK_AUDIO && isAudioTrackAdd()) || (index == TRACK_VIDEO && isVideoTrackAdd())){
            return;
        }
        if (mediaMuxer != null){
            int track = 0;
            try {
                track = mediaMuxer.addTrack(mediaFormat);
            }catch (RuntimeException e){
                Log.i(TAG, "addTrackIndex: Exception:"+e.toString());
                return;
            }
            if (index == TRACK_VIDEO){
                videoTrackIndex = track;
                isVideoTrackAdd = true;
                Log.w(TAG, "addTrackIndex: 添加视频轨完成" );
            }else {
                audioTrackIndex = track;
                isAudioTrackAdd = true;
                Log.w(TAG, "addTrackIndex: 添加音频轨完成" );
            }
            requestStart();
        }
    }

    /**
     * 请求混合器开始启动
     */
    private void requestStart() {
        synchronized (lock){
            if (isMuxerStart()){
                mediaMuxer.start();
                Log.w(TAG, "requestStart: 启动混合器..开始等待数据输入");
                lock.notify();
            }
        }
    }


    private boolean isAudioTrackAdd() {
        return isAudioTrackAdd;
    }

    public boolean isVideoTrackAdd() {
        return isVideoTrackAdd;
    }

    /**
     * 当前视频合成器是否运行了
     * @return
     */
    public boolean isMuxerStart() {
        return isAudioTrackAdd && isVideoTrackAdd;
    }

    private void addVideoData(byte[] data) {
        if (videoThread != null){
            videoThread.add(data);
        }
    }

    /**
     * 初始化混合器
     */
    private void initMuxer() {
        muxerDatas = new Vector<>();
        fileSwapHelper = new FileUtils();
        audioThread = new AudioEncodeThread((new WeakReference<MediaMuxerThread>(this)));
        videoThread = new VideoEncoderThread(videoWidth,videoHeight,photoWidth,photoHeight,new WeakReference<MediaMuxerThread>(this));
        audioThread.start();
        videoThread.start();
        try {
            readyStart();
        } catch (IOException e) {
            Log.e(TAG, "initMuxer: "+e.toString(), e);
        }
    }

    @Override
    public void run() {
        super.run();
        //初始化混合器
        initMuxer();
        while (!isExit){
            if (isMuxerStart()){
                if (muxerDatas.isEmpty()){
                    synchronized (lock){
                        try {
                            Log.i(TAG, "run: 等待混合数据...");
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    if (fileSwapHelper.requestSwapFile()){
                        //需要切换文件
                        String nextFileName = fileSwapHelper.getNextFileName();
                        Log.i(TAG, "run: 正在重启混合器..."+nextFileName);
                        restart(nextFileName);
                    }else {
                        MuxerData data = muxerDatas.remove(0);
                        int track;
                        if (data.trackIndex == TRACK_VIDEO){
                            track = videoTrackIndex;
                        }else {
                            track = audioTrackIndex;
                        }
                        Log.i(TAG, "run: 写入混合数据"+data.bufferInfo.size);
                        try {
                            mediaMuxer.writeSampleData(track,data.byteBuffer,data.bufferInfo);
                        }catch (RuntimeException e){
                            Log.e(TAG, "run: 写入混合数据失败"+e.toString(),e );
                        }
                    }
                }
            } else {
                synchronized (lock) {
                    try {
                        Log.e(TAG, "等待音视轨添加...");
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e(TAG, "run: "+e.toString(),e );
                    }
                }
            }
            readyStop();
            Log.w(TAG, "run: 混合器退出");
        }
    }

    private void restart() {
        fileSwapHelper.requestSwapFile(true);
        String nextFileName = fileSwapHelper.getNextFileName();
        restart(nextFileName);
    }

    private void restart(String filePath) {
        restartAudioVideo();
        readyStop();
        try {
            readyStart(filePath);
        }catch (RuntimeException e){
            Log.e(TAG, "restart: 重启混合器失败,尝试再次重启", e);
            restart();
            return;
        }
        Log.i(TAG, "restart: 重启混合器完成");
    }


    private void readyStop(){
        if (mediaMuxer != null){
            try {
                mediaMuxer.stop();
                mediaMuxer.release();
            }catch (IllegalArgumentException e){
                Log.e(TAG, "readyStop: "+e.toString(),e );
            }finally {
                mediaMuxer = null;
            }
        }
    }

    private void restartAudioVideo(){
        if (audioThread != null){
            audioTrackIndex = -1;
            isAudioTrackAdd = false;
            audioThread.restart();
        }
        if (videoThread != null){
            videoTrackIndex = -1;
            isVideoTrackAdd = false;
            videoThread.restart();
        }
    }

    private void exit() {
        if (videoThread != null){
            videoThread.exit();
            try {
                videoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (audioThread != null){
            audioThread.exit();
            try {
                audioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isExit = true;
        synchronized (lock){
            lock.notify();
        }
    }



    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {
        int trackIndex;
        ByteBuffer byteBuffer;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuffer = byteBuffer;
            this.bufferInfo = bufferInfo;
        }
    }
}
