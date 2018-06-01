package com.hisign.video.mediamuser.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static android.os.Process.THREAD_PRIORITY_URGENT_AUDIO;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/29
 */

class AudioEncodeThread extends Thread {
    /**
     * TAG
     */
    public static final String TAG = FileUtils.DIR_TAG+"Audio";

    /**
     *每帧采样
     */
    public static final int SAMPLES_PER_FRAME = 1024;
    /**
     *
     */
    public static final int FRAMES_PER_BUFFER = 25;
    /**
     * 超时控制
     */
    private static final int TIMEOUT_USEC = 10000;
    /**
     * 音频格式
     */
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    /**
     * 采样频率
     */
    private static final int SAMPLE_RATE = 16000;
    /**
     *
     */
    private static final int BIT_RATE = 64000;

    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.DEFAULT
    };
    /**
     * 锁
     */
    private final Object lock = new Object();

    private MediaCodec mMediaCodec;

    private volatile boolean isExit = false;

    private WeakReference<MediaMuxerThread> mediaMuxerThreads;

    private AudioRecord audioRecord;

    private MediaCodec.BufferInfo mBufferInfo;

    private volatile boolean isStart = false;

    private volatile boolean isMuxerReady = false;

    private long prevOutputPTSUs = 0;

    private MediaFormat audioFormat;

    public AudioEncodeThread(WeakReference<MediaMuxerThread> weakReference) {
        this.mediaMuxerThreads = weakReference;
        mBufferInfo = new MediaCodec.BufferInfo();
        prepare();
    }

    private void prepare() {
        MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null){
            Log.e(TAG, "prepare: unable to find an appropriate codec for "+MIME_TYPE);
            return;
        }
        Log.i(TAG, "prepare: select codec "+audioCodecInfo.getName() );
        audioFormat = MediaFormat.createAudioFormat(MIME_TYPE,SAMPLE_RATE,1);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE,BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT,1);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE,SAMPLE_RATE);
        Log.i(TAG, "prepare: format="+audioFormat);
    }

    private MediaCodecInfo selectAudioCodec(String mimeType) {
        MediaCodecInfo result = null;
        final int numCodecs = MediaCodecList.getCodecCount();
        Log.i(TAG, "selectAudioCodec: numCodes = "+numCodecs);
        for (int i = 0;i<numCodecs;i++){
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()){
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0;j<types.length;j++){
                Log.i(TAG, "selectAudioCodec: supportType="+codecInfo.getName()+",MIME="+types[j]);
                if (types[j].equalsIgnoreCase(mimeType)){
                    if (result == null){
                        result = codecInfo;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private void startMediaCodec() {
        if (mMediaCodec != null){
            return;
        }
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaCodec.configure(audioFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);;
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            isStart = false;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        prepareAudioRecord();
        isStart = true;
    }

    private void stopMediaCodec(){
        if (audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mMediaCodec != null){
            mMediaCodec.start();
            mMediaCodec.stop();
            mMediaCodec = null;
        }
        isStart = false;
        Log.i(TAG, "stopMediaCodec: stop audio 录制");
    }


    public void exit() {
        isExit = true;
    }


    public void setMuxerReady(boolean muxerReady) {
        synchronized (lock){
            Log.w(TAG, "setMuxerReady: Thread -"+Thread.currentThread().getId()+" audio --setMuxerReady..."+muxerReady );
            isMuxerReady = muxerReady;
            lock.notifyAll();
        }
    }

    public void restart() {
        isStart= false;
        isMuxerReady = false;
    }

    private void prepareAudioRecord(){
        if (audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        android.os.Process.setThreadPriority(THREAD_PRIORITY_URGENT_AUDIO);
        final int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int bufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
        if (bufferSize < minBufferSize){
            bufferSize = ((minBufferSize/SAMPLES_PER_FRAME)+1)*SAMPLES_PER_FRAME*2;
        }
        audioRecord = null;
        for (final int source : AUDIO_SOURCES){
            audioRecord = new AudioRecord(source,SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED){
                audioRecord = null;
            }
            if (audioRecord != null){
                break;
            }
        }
        if (audioRecord != null){
            audioRecord.startRecording();
        }
    }

    @Override
    public void run() {
        super.run();
        final ByteBuffer buffer = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
        int readBytes;
        while (!isExit){
            /*启动或者重启*/
            if (!isStart){
                stopMediaCodec();
                Log.i(TAG, "run: id = "+Thread.currentThread().getId()+"audio -- "+isMuxerReady);

                if (!isMuxerReady){
                    synchronized (lock){
                        try {
                            Log.i(TAG, "run: audio 等待回合器准备");
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    startMediaCodec();
                }

            }else if (audioRecord != null){
                buffer.clear();
                readBytes = audioRecord.read(buffer,SAMPLES_PER_FRAME);
                if (readBytes > 0){
                    //set audio data to encoder
                    buffer.position(readBytes);
                    buffer.flip();
                    Log.i(TAG, "run: 解码音频数据:"+readBytes);
                    encode(buffer,readBytes, getPTSUs());
                }
            }
        }
        Log.i(TAG, "run: Audio 录制线程 退出");
    }

    private void encode(ByteBuffer buffer, int length, long presentationTimeUs) {
        if (isExit) return;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        //向编码器输入数据
        if (inputBufferIndex >= 0){
            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            if (buffer != null){
                inputBuffer.put(buffer);
            }
            if (length<=0){
                Log.i(TAG, "encode: send BUFFER_FLAG_END_OF_STEAM");
                mMediaCodec.queueInputBuffer(inputBufferIndex,0,0,presentationTimeUs,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }else {
                mMediaCodec.queueInputBuffer(inputBufferIndex,0,length,presentationTimeUs,0);;
            }
        }else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER){
            /*
                wait for MediaCodec encoder is ready to encode
             nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
             will wait for maximum TIMEOUT_USEC(10msec) on each call
             */
        }
        /*获取解码后的数据*/
        final MediaMuxerThread muxer = mediaMuxerThreads.get();
        if (muxer == null){
            Log.e(TAG, "encode: mediaMuxerThreads is null");
            return;
        }
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        int encoderStatus;
        do {
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo,TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER){

            }else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            }else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                final MediaFormat format = mMediaCodec.getOutputFormat();
                MediaMuxerThread mediaMuxerThread = this.mediaMuxerThreads.get();
                if (mediaMuxerThread != null){
                    Log.i(TAG, "encode: 添加音轨 "+ format.toString());
                    mediaMuxerThread.addTrackIndex(MediaMuxerThread.TRACK_AUDIO,format);
                }
            }else if (encoderStatus < 0){
                Log.e(TAG, "encode: encoderstatus < 0" );
            }else {
                final ByteBuffer encodeData = encoderOutputBuffers[encoderStatus];
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0 && muxer != null && muxer.isMuxerStart()){
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    Log.i(TAG, "encode: 发送音频数据 "+ mBufferInfo.size);
                    muxer.addMuxerData(new MediaMuxerThread.MuxerData(MediaMuxerThread.TRACK_AUDIO,encodeData,mBufferInfo));
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                mMediaCodec.releaseOutputBuffer(encoderStatus,false);
            }
        }while (encoderStatus >= 0);
    }

    /**
     * 获取下一个encoding presentationTimeUs
     * @return
     */
    private long getPTSUs(){
        long result = System.nanoTime()/1000L;
        if (result < prevOutputPTSUs){
            result = (prevOutputPTSUs - result) + result;
        }
        return result;
    }
}
