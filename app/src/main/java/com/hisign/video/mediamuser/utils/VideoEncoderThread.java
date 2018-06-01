package com.hisign.video.mediamuser.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
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

class VideoEncoderThread extends Thread{

    public static final int IMAGE_HEIGHT = 1080;
    public static final int IMAGE_WIDTH = 1920;

    private static final String TAG = "MediaXuser-Video";
    /**
     * 编码相关参数
     * H.264 Advanced Video
     */
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;//"video/avc";
    /**
     * 帧率
     */
    private static final int FRAME_RATE = 25;
    /**
     * I帧间隔(GOP)
     */
    private static final int IFRAME_INTERVAL = 10;
    /**
     * 编码超时时间
     */
    private static final int TIMEOUT_USEC = 10000;
    /**
     * 视频宽度
     */
    private int mWidth;
    /**
     * 视频高度
     */
    private int mHeight;
    /**
     * 存储每一帧的数据Vector自增数组
     */
    private Vector<byte[]> frameBytes;
    /**
     *
     */
    private byte[] mFrameData;

    private static final int COMPRESS_RATIO = 256;
    /**
     * bit rate CameraWrapper.
     */
    private static final int BIT_RATE = IMAGE_HEIGHT*IMAGE_WIDTH * 3 * 8 * FRAME_RATE / COMPRESS_RATIO;

    private final Object lock = new Object();

    private MediaCodecInfo mCodecInfo;
    /**
     * Android硬编解码器
     */
    private MediaCodec mMediaCodec;
    /**
     * 编解码buffer相关信息
     */
    private MediaCodec.BufferInfo mBufferInfo;
    /**
     * 音视频混合器
     */
    private WeakReference<MediaMuxerThread> mediaMuxers;
    /**
     * 音视频格式
     */
    private MediaFormat mediaFormat;

    private volatile boolean isStart = false;

    private volatile boolean isExit = false;

    private volatile boolean isMuxerReady = false;


    public VideoEncoderThread(int mWidth, int mHeight, WeakReference<MediaMuxerThread> mediaMuxers) {
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mediaMuxers = mediaMuxers;
        frameBytes = new Vector<>();
        prepare();
    }


    /**
     * 执行相关准备工作
     */
    private void prepare() {
        Log.i(TAG,"VideoEncoderThread().prepare");
        mFrameData = new byte[this.mWidth*this.mHeight*3/2];
        mBufferInfo = new MediaCodec.BufferInfo();
        mCodecInfo = selectCodec(MIME_TYPE);
        if (mCodecInfo == null){
            Log.e(TAG,"Unable to find an appropriate codec for "+MIME_TYPE);
            return;
        }
        mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,this.mWidth,this.mHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,IFRAME_INTERVAL);
    }

    /**
     * 选择硬解码器
     * @param mimeType
     * @return
     */
    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0;i<numCodecs;i++){
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()){
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0;j < types.length;j++){
                if (types[j].equalsIgnoreCase(mimeType)){
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * 开始视频编码
     */
    private void startMediaCodec() throws IOException {
        mMediaCodec = MediaCodec.createByCodecName(mCodecInfo.getName());
        mMediaCodec.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        isStart = true;
        Log.i(TAG, "startMediaCodec: ");
    }

    public void setMuxerReady(boolean muxerReady) {
        synchronized(lock){
            Log.e(TAG,Thread.currentThread().getId()+"video == setMuxerReady");
            isMuxerReady = muxerReady;
            lock.notifyAll();
        }
    }

    public void add(byte[] data) {
        int len = 0;
        if (data != null){
            len = data.length;
        }
        Log.i(TAG, "add: "+len+";isMuxerReady:"+isMuxerReady);
        if (frameBytes != null && isMuxerReady){
            frameBytes.add(data);
        }
    }

    public synchronized void restart() {
        isStart = false;
        isMuxerReady = false;
        frameBytes.clear();
    }

    @Override
    public void run() {
        super.run();
        while (!isExit){
            if (!isStart){
                stopMediaCodec();
                if (!isMuxerReady){
                    synchronized (lock){
                        Log.e(TAG,"video -- 等待混合器准备...");
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    Log.i(TAG, "video -- startMediaCodec...");
                    try {
                        startMediaCodec();
                    } catch (IOException e) {
                        isStart = false;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }

            }else if (!frameBytes.isEmpty()){
                byte[] bytes = this.frameBytes.remove(0);
                Log.d(TAG, "run: 解码视频数据:"+bytes.length);
                encodeFrame(bytes);
            }
        }
        Log.i(TAG, "run: 录制视频线程 退出");
    }

    public void exit() {
        isExit = true;
    }

    /**
     * 编码每一帧的数据
     * @param bytes 每一帧的数据
     */
    private void encodeFrame(byte[] bytes) {
        Log.w(TAG, "VideoEncoderThread.encodeFrame()");
        //将原始的N21数据转为I420
        convertNV21ToI420SemiPlanar(bytes,mFrameData,this.mWidth,this.mHeight);

        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();

        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufferIndex >= 0){
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(mFrameData);
            mMediaCodec.queueInputBuffer(inputBufferIndex,0,mFrameData.length,System.nanoTime()/1000,0);
        }else {
            Log.e(TAG,"input buffer not available");
        }

        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo,TIMEOUT_USEC);
        Log.i(TAG, "encodeFrame: outputBufferIndex -->"+outputBufferIndex);
        do {
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER){

            }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                outputBuffers = mMediaCodec.getOutputBuffers();
            }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                MediaMuxerThread mediaMuxerThread = this.mediaMuxers.get();
                if (mediaMuxerThread != null){
                    mediaMuxerThread.addTrackIndex(MediaMuxerThread.TRACK_VIDEO,newFormat);
                }
            }else if (outputBufferIndex < 0){
                Log.e(TAG, "encodeFrame: outputBufferIndex < 0" );
            }else {
                Log.d(TAG, "encodeFrame: perform encoding");
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if (outputBuffer == null){
                    throw new RuntimeException("encoderOutputBuffer "+outputBufferIndex+" was null ");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                    Log.d(TAG, "encodeFrame: ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0){
                    MediaMuxerThread mediaMuxerThread = this.mediaMuxers.get();
                    if (mediaMuxerThread != null && !mediaMuxerThread.isVideoTrackAdd()){
                        MediaFormat newFormat = mMediaCodec.getOutputFormat();
                        mediaMuxerThread.addTrackIndex(MediaMuxerThread.TRACK_VIDEO,newFormat);
                    }
                    //adjust the ByteBuffer values to match BufferInfo(not needed?)
                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset+mBufferInfo.size);
                    if (mediaMuxerThread != null && mediaMuxerThread.isMuxerStart()){
                        mediaMuxerThread.addMuxerData(new MediaMuxerThread.MuxerData(MediaMuxerThread.TRACK_VIDEO,outputBuffer,mBufferInfo));
                    }
                    Log.d(TAG, "encodeFrame: sent "+ mBufferInfo.size+" frameBytes to muxer");
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex,false);
            }
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo,TIMEOUT_USEC);
        }while (outputBufferIndex >=0);
    }

    /**
     * 停止视频编码
     */
    private void stopMediaCodec() {
        if (mMediaCodec != null){
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        isStart = false;
        Log.e(TAG, "stopMediaCodec: stop vider 录制");
    }
    /**
     * Camera可获取NV21视频码,此处将缓存转换成i420格式
     * @param nv21Bytes NV21视频流
     * @param i420Bytes 目标流
     * @param width 视频宽
     * @param height 视频高
     */
    private void convertNV21ToI420SemiPlanar(byte[] nv21Bytes, byte[] i420Bytes, int width, int height) {
        System.arraycopy(nv21Bytes,0,i420Bytes,0,width*height);
        for (int i = width*height;i<nv21Bytes.length;i+=2){
            i420Bytes[i] = nv21Bytes[i+1];
            i420Bytes[i+1] = nv21Bytes[i];
        }
    }

}
