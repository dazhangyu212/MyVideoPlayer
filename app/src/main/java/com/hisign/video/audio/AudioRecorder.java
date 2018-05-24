package com.hisign.video.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/23
 */

public class AudioRecorder {

    /**
     *
     */
    public static final String TAG = "AudioRecorder";
    private AudioRecord audioRecord = null;

    private int recordBufSize = 0;
    /**
     * 音频输入-麦克风
     */
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    /**
     * 采用频率
     * 44100是目前的标准,但是某些设备仍然支持22050,16000,11025
     * 采样频率一般共分为22.05KHz,44.1KHz,48KHz
     */
    private final static int AUDIO_SAMPLE_RATE = 16000;
    /**
     * 声道 单声道
     */
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 编码
     */
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    /**
     * 缓冲区字节大小
     */
    private int bufferSizeInBytes = 0;
    /**
     *文件名称
     */
    private String filename;

    /**
     * 录音状态
     */
    private Status status = Status.STATUS_NOT_READY;
    /**
     * 录音文件
     */
    private List<String> filesName = new ArrayList<>();




    private static AudioRecorder mAudioRecorder = null;
    private AudioRecorder(){}
    public static AudioRecorder getInstance(){
        if(mAudioRecorder == null){
            synchronized(AudioRecorder.class){
                if(mAudioRecorder == null){
                    mAudioRecorder = new AudioRecorder();
                }
            }
        }
        return mAudioRecorder;
    }
    /**
     * 创建默认的录音对象
     * @param name 文件名
     */
    private void createDefaultAudio(String name){
        recordBufSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,AUDIO_CHANNEL,AUDIO_ENCODING);
        audioRecord = new AudioRecord(AUDIO_INPUT,AUDIO_SAMPLE_RATE,AUDIO_CHANNEL,AUDIO_ENCODING,bufferSizeInBytes);
        this.filename = name;
        status = Status.STATUS_READY;

    }

    /**
     * 创建录音对象
     * @param filename 文件名
     * @param audioSource
     * @param sampleRateInHz 采音频率
     * @param channelConfig 声道设置
     * @param audioFormat 音频格式
     */
    public void createAudio(String filename,int audioSource, int sampleRateInHz, int channelConfig, int audioFormat){
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig,audioFormat);
        audioRecord = new AudioRecord(audioSource,sampleRateInHz,channelConfig,audioFormat,bufferSizeInBytes);
        this.filename = filename;
    }

    /**
     * 录音对象的状态
     */
    public  enum Status {
        //未开始
        STATUS_NOT_READY,
        //预备
        STATUS_READY,
        //录音
        STATUS_START,
        //暂停
        STATUS_PAUSE,
        //停止
        STATUS_STOP
    }


    public void startRecorder(final RecordStreamListener listener){
        if (status == Status.STATUS_NOT_READY || TextUtils.isEmpty(filename)){
            throw new IllegalArgumentException("uninitialized");
        }
        if (status == Status.STATUS_START){
            throw new IllegalArgumentException("Is recording");
        }
        Log.d("AudioRecorder","===startRecord==="+audioRecord.getState());
        audioRecord.startRecording();
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeVoiceToFile(listener);
            }
        }).start();
    }

    /**
     * 将声音写入文件
     * @param listener 音频流监听
     */
    private void writeVoiceToFile(RecordStreamListener listener) {
        //byte数组存储一些字节数据,大小为缓冲区大小
        byte[] audioData = new byte[bufferSizeInBytes];
        FileOutputStream fos = null;
        int readSize = 0;
        String  currentFileName = filename;
        if (status == Status.STATUS_PAUSE){
            //假如暂停录音,在文件名之后加数,放置重名文件内容被覆盖
            currentFileName += filesName.size();
        }
        filesName.add(currentFileName);
        try {
            File file = new File(FileUtils.getPcmFileAbsolutePath(currentFileName));
            if (file.exists()){
                file.delete();
            }
            //建立一个可存取字节的文件流
            fos = new FileOutputStream(file);
        }catch (IllegalArgumentException e){
            Log.e(TAG,e.getMessage());
            throw new IllegalStateException(e.getMessage());
        } catch (FileNotFoundException e) {
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
        }
        //将录音状态设置成正在录音状态
        status = Status.STATUS_START;
        while (status == Status.STATUS_START){
            readSize = audioRecord.read(audioData,0,bufferSizeInBytes);
            if (AudioRecord.ERROR_INVALID_OPERATION != readSize && fos != null){
                try {
                    fos.write(audioData);
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage());
                }
            }
        }
        if (fos != null){
            try {
                fos.close();//记得关闭
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
            }
        }
    }

    /**
     * 将pcm合并成wav
     * @param filePaths 音频流的监听
     */
    private void mergePCMFilesToWAVFile(List<String> filePaths){
        new Thread(new Runnable() {
            @Override
            public void run() {
            }
        }).start();
    }
}

