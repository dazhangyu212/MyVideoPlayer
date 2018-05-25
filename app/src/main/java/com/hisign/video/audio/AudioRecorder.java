package com.hisign.video.audio;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.hisign.video.app.CrashApplication;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * 描述：录音和Wav文件合成,再添加播放功能
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

    /**
     * 音频输入-麦克风
     */
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    /**
     * 采用频率
     * 44100是目前的标准,但是某些设备仍然支持22050,16000,11025
     * 采样频率一般共分为22.05KHz,44.1KHz,48KHz
     */
    private final static int AUDIO_SAMPLE_RATE = 44100;
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
    public void createDefaultAudio(String name){
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,AUDIO_CHANNEL,
                AUDIO_ENCODING);
        audioRecord = new AudioRecord(AUDIO_INPUT,AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,AUDIO_ENCODING,bufferSizeInBytes);
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
        StringBuilder  currentFileName = new StringBuilder(filename);
        if (status == Status.STATUS_PAUSE){
            //假如暂停录音,在文件名之后加数,放置重名文件内容被覆盖
            currentFileName.append("_").append(filesName.size());
        }
        filesName.add(currentFileName.toString());
        try {
            File file = new File(FileUtils.getPcmFileAbsolutePath(currentFileName.toString()));
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
            Log.i(TAG,"audio is writing,readSize = "+readSize);
            if (AudioRecord.ERROR_INVALID_OPERATION != readSize && fos != null){
                Log.i(TAG,"audio is writing");
                try {
                    fos.write(audioData);
                    if (listener != null){
                        listener.recordOfByte(audioData,0,audioData.length);
                    }
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
    private void mergePCMFilesToWAVFile(final List<String> filePaths){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (PcmToWav.mergePCMFilesToWAVFile(filePaths,FileUtils.getWavFileAbsolutePath(filename))){
                    Log.d(TAG,"操作成功");
                }else {
                    Log.e(TAG,"PCM合成WAV失败");
                    throw new IllegalStateException("fail to mergePCMFilesToWAVFile ");
                }
                filename = null;
            }
        }).start();
    }

    private void makePCMFileToWAVFile(){
        new  Thread(new Runnable() {
            @Override
            public void run() {
                if (PcmToWav.makePCMFileToWAVFile(FileUtils.getPcmFileAbsolutePath(filename)
                ,FileUtils.getWavFileAbsolutePath(filename),true)){
                    Log.d(TAG,"操作成功");
                }else {
                    Log.e(TAG,"PCM合成WAV失败");
                    throw new IllegalStateException("fail to mergePCMFilesToWAVFile ");
                }
                filename = null;
            }
        }).start();
    }

    /**
     * 获取录音状态
     * @return
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 获取本次录音的文件个数
     * @return
     */
    public int getPcmFilesCount(){
        return filesName.size();
    }

    /**
     * 暂停录音
     */
    public void pauseRecord(){
        Log.d(TAG,"=== pause recordd ===");
        if (status != Status.STATUS_START){
         throw new IllegalStateException("没有再录音");
        }else {
            audioRecord.stop();
            status = Status.STATUS_PAUSE;
        }
    }

    public void stopRecord(){
        Log.d(TAG,"=== stop record===");
        if (status == Status.STATUS_NOT_READY || status == Status.STATUS_READY){
            throw new IllegalStateException("录音尚未开始");
        }else {
            audioRecord.stop();
            status = Status.STATUS_STOP;
            release();
        }
    }

    /**
     * 释放资源
     */
    private void release() {
        Log.d(TAG,"=== release===");
        if(filesName.size() > 0){
            List<String> filesPath = new ArrayList<>();
            for (String fileName:filesName){
                filesPath.add(FileUtils.getPcmFileAbsolutePath(fileName));
            }
            //清除
            filesName.clear();
            //将多个pcm文件转化为wav文件
            mergePCMFilesToWAVFile(filesPath);
        }else {
            //这里由于只要录音过filesName.size都会大于0,没录音时fileName为null
            //会报空指针 NullPointerException
            // 将单个pcm文件转化为wav文件
            //Log.d("AudioRecorder", "=====makePCMFileToWAVFile======");
            //makePCMFileToWAVFile();
        }
        if (audioRecord != null){
            audioRecord.release();
            audioRecord = null;
        }
        status = Status.STATUS_NOT_READY;
    }

    /**
     * 取消录音
     */
    public void cancel(){
        filesName.clear();
        filename = null;
        if (audioRecord != null){
            audioRecord.release();
            audioRecord = null;
        }
        status = Status.STATUS_NOT_READY;
    }

    //--------------------------------------------播放

    /**
     * 检测必要的权限
     * @return
     */
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(CrashApplication.getInstance().getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(CrashApplication.getInstance().getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    public void playPcm(){
        PlayTask playTask = new PlayTask();
        playTask.execute();

    }

    boolean mAudioPlaying;
    class PlayTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            if (filesName == null ||filesName.size()==0){
                return null;
            }
            mAudioPlaying = true;
            int bufferSize = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_STEREO,AudioFormat.ENCODING_PCM_16BIT);
            short[] buffer = new short[bufferSize];
            DataInputStream dis = null;
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,AUDIO_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_STEREO,AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,AudioTrack.MODE_STREAM);
            try {
                dis = new DataInputStream(new BufferedInputStream(new FileInputStream(FileUtils.AUDIO_PCM_BASEPATH+filesName.get(0)+".pcm")));
                audioTrack.play();//开始播放
                //由于AudioTrack播放的是流,所以,我们需要一边播放一边读取
                while(mAudioPlaying && dis.available() > 0){
                    int i = 0;
                    while (dis.available()>0 && i<buffer.length){
                        buffer[i] = dis.readByte();
                        i++;
                    }
                    // 然后将数据写入到AudioTrack中
                    audioTrack.write(buffer,0,buffer.length);
                }
                //停止播放
                audioTrack.stop();
                dis.close();
            } catch (FileNotFoundException e) {
                Log.i(TAG,"error:"+e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Log.i(TAG,"error:"+e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }

}

