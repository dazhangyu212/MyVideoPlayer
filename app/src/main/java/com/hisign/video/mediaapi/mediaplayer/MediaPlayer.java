package com.hisign.video.mediaapi.mediaplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/28
 */

public class MediaPlayer {

    /**
     *
     */
    public static final String TAG = "MediaPlayer";
    private static final long TIMEOUT_US = 10000;
    private IPlayerCallBack callBack;
    private VideoThread videoThread;
    private AudioThread audioThread;
    private boolean isPlaying;
    private String filePath;
    private Surface surface;

    public MediaPlayer(Surface surface,String filePath ) {
        this.filePath = filePath;
        this.surface = surface;
    }

    public boolean isPlaying(){
        return isPlaying;
    }

    public void play(){
        isPlaying = true;
        if (videoThread == null){
            videoThread = new VideoThread();
            videoThread.start();
        }
        if (audioThread == null){
            audioThread = new AudioThread();
            audioThread.start();
        }
    }

    public void stop(){
        isPlaying = false;
    }

    public void destory(){
        stop();
        if (audioThread != null){
            audioThread.interrupt();
        }
        if (videoThread != null){
            videoThread.interrupt();
        }
    }

    public void setCallBack(IPlayerCallBack callBack) {
        this.callBack = callBack;
    }

    /**
     * 将缓冲区传递至解码器
    * 如果到了文件末尾，返回true;否则返回false
    */
    private boolean putBufferToCoder(MediaExtractor mediaExtractor, MediaCodec mediaCodec, ByteBuffer[] inputBuffers) {
        boolean isMediaEOS = false;
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            int sampleSize = mediaExtractor.readSampleData(inputBuffer,0);
            if (sampleSize < 0){
                mediaCodec.queueInputBuffer(inputBufferIndex,0,0,0
                ,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                isMediaEOS = true;
                Log.i(TAG,"media EOS");
            }else {
                mediaCodec.queueInputBuffer(inputBufferIndex,0,sampleSize,mediaExtractor.getSampleTime(),0);;
                mediaExtractor.advance();
            }
        }
        return  isMediaEOS;

    }

    /**
     * 获取指定类型媒体文件所在轨道
     * @param mediaExtractor MediaExtractor
     * @param type 类型
     * @return
     */
    private int getMediaTrackIndex(MediaExtractor mediaExtractor, String type) {
        int trackIndex = -1;
        for (int i = 0;i< mediaExtractor.getTrackCount();i++){
            MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(type)){
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }

    /**
     * 延迟渲染
     * @param bufferInfo 音频
     * @param startMs
     */
    private void sleepRender(MediaCodec.BufferInfo bufferInfo, long startMs) {
        while (bufferInfo.presentationTimeUs/1000>System.currentTimeMillis()){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class VideoThread extends Thread {
        @Override
        public void run() {
            MediaExtractor mediaExtractor = new MediaExtractor();
            MediaCodec mediaCodec = null;
            try {
                mediaExtractor.setDataSource(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //获取视频所在轨道
            int videoStackTrack = getMediaTrackIndex(mediaExtractor,"video/");
            if (videoStackTrack >= 0){
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(videoStackTrack);
                int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                float time = mediaFormat.getLong(MediaFormat.KEY_DURATION)/1000000;
                callBack.videoAspect(width,height,time);
                mediaExtractor.selectTrack(videoStackTrack);
                try {
                    mediaCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
                    mediaCodec.configure(mediaFormat,surface,null,0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mediaCodec == null){
                Log.i(TAG,"MediaCodec null");
                return;
            }
            mediaCodec.start();
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            boolean isVideoEOS = false;

            long startMs = System.currentTimeMillis();
            while (!Thread.interrupted()){
                if (!isPlaying){
                    continue;
                }
                //将资源传递到解码器
                if (!isVideoEOS){
                    isVideoEOS = putBufferToCoder(mediaExtractor,mediaCodec,inputBuffers);
                }
                int  outputBufferIndex = mediaCodec.dequeueOutputBuffer(videoBufferInfo,TIMEOUT_US);
                switch(outputBufferIndex){
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.i(TAG,"format changed");
                        break ;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.i(TAG,"time out");
                        break ;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.i(TAG,"output buffers changed");
                        break;
                    default:
                        //直接渲染到Surface时使用不到outputBuffer
                        //ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        //延时操作
                        //如果缓冲区里的可展示时间>当前视频播放的进度，就休眠一下
                        sleepRender(videoBufferInfo, startMs);
                        //渲染
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                        Log.i(TAG,"MediaPlayer buffered");
                        break;

                }
                if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.v(TAG, "buffer stream end");
                    break;
                }
            }//end while
            mediaCodec.stop();
            mediaCodec.release();
            mediaExtractor.release();

        }
    }
    private class AudioThread extends Thread{
        private int audioInputBufferSize;

        private AudioTrack audioTrack;

        @Override
        public void run() {
            MediaExtractor audioExtractor = new MediaExtractor();
            MediaCodec audioCodec = null;
            try {
                audioExtractor.setDataSource(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0;i < audioExtractor.getTrackCount();i++){
                MediaFormat mediaFormat = audioExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")){
                    audioExtractor.selectTrack(i);
                    int audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int audioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate,
                            (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT);
                    int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    audioInputBufferSize = minBufferSize>0?minBufferSize*4:maxInputSize;
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            audioSampleRate,
                            (audioChannels == 1?AudioFormat.CHANNEL_OUT_MONO:AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT,
                            audioInputBufferSize,
                            AudioTrack.MODE_STREAM);
                    audioTrack.play();
                    Log.i(TAG,"audio play");
                    try {
                        audioCodec = MediaCodec.createDecoderByType(mime);
                        audioCodec.configure(mediaFormat,null,null,0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            if (audioCodec == null){
                Log.i(TAG,"audio decoder null");
                return;
            }
            audioCodec.start();
            final ByteBuffer[] buffers = audioCodec.getOutputBuffers();
            int size = buffers[0].capacity();
            if (size <= 0){
                size = audioInputBufferSize;
            }
            byte[] mAudioOutTempBuf = new byte[size];
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = audioCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = audioCodec.getOutputBuffers();
            boolean isAudioEOS = false;
            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()){
                if (!isPlaying){
                    continue;
                }
                if (!isAudioEOS){
                    isAudioEOS = putBufferToCoder(audioExtractor,audioCodec,inputBuffers);
                }
                int outputBufferIndex = audioCodec.dequeueOutputBuffer(audioBufferInfo,TIMEOUT_US);
                switch (outputBufferIndex){
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.i(TAG,"audio format changed");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.i(TAG,"audio time out");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        outputBuffers = audioCodec.getOutputBuffers();
                        Log.i(TAG,"output buffers changed");
                        break;
                    default:
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        //延时操作
                        //如果缓冲区里的可展示时间>当前视频播放的进度，就休眠一下
                        sleepRender(audioBufferInfo,startMs);
                        if (audioBufferInfo.size > 0){
                            if (mAudioOutTempBuf.length < audioBufferInfo.size){
                                mAudioOutTempBuf = new byte[audioBufferInfo.size];
                            }
                            outputBuffer.position(0);
                            outputBuffer.get(mAudioOutTempBuf,0,audioBufferInfo.size);
                            outputBuffer.clear();
                            if (audioTrack != null){
                                audioTrack.write(mAudioOutTempBuf,0,audioBufferInfo.size);
                            }
                        }
                        audioCodec.releaseOutputBuffer(outputBufferIndex,false);
                        break;
                }
                if ((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.i(TAG,"buffer stream end");
                    break;
                }
            }//end while
            audioCodec.stop();
            audioCodec.release();
            audioExtractor.release();
            audioTrack.stop();
            audioTrack.release();
        }
    }
}
