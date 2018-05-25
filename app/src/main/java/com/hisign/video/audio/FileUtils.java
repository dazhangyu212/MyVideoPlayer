package com.hisign.video.audio;

import android.os.Environment;
import android.text.TextUtils;

import com.hisign.video.finalvalues.ConstPath;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述：管理录音文件的类
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/24
 */

class FileUtils {
    private static String rootPath = "pauseRecord";

    /**
     *原始文件
     */
    public static final String AUDIO_PCM_BASEPATH = ConstPath.PROJECT_ROOT_PATH+rootPath+ "/pcm/";

    /**
     *可播放的高质量音频文件
     */
     static final String AUDIO_WAV_BASEPATH = ConstPath.PROJECT_ROOT_PATH+rootPath+ "/wav/";

     private static void setRootPath(String path){
         rootPath = path;
     }

     public static String getPcmFileAbsolutePath(String fileName){
         if (TextUtils.isEmpty(fileName)){
             throw new NullPointerException("fileName is empty");
         }

         if (!isSdcardExist()){
             throw new IllegalStateException("SD card not found");
         }
         String mAudioRawPath = "";
         if (isSdcardExist()){
             if (!fileName.endsWith(".pcm")){
                 fileName = fileName+".pcm";
             }
             String fileBasePath = AUDIO_PCM_BASEPATH;
             File file = new File(fileBasePath);
             if (!file.exists()){
                 file.mkdirs();
             }
             mAudioRawPath = fileBasePath+ fileName;
         }
         return mAudioRawPath;
     }

     public static String getWavFileAbsolutePath(String fileName){
         if (fileName == null){
             throw new NullPointerException("fileName can't be null");
         }
         if (!isSdcardExist()){
             throw new IllegalStateException("SD card not found");
         }
         String mAudioWavPath = "";
         if (isSdcardExist()){
             if (!fileName.endsWith(".wav")){
                 fileName = fileName+".wav";
             }
             String fileBasePath = AUDIO_WAV_BASEPATH;
             File file = new File(fileBasePath);
             if (!file.exists()){
                 file.mkdirs();
             }
             mAudioWavPath = fileBasePath+fileName;
         }
         return  mAudioWavPath;
     }

    /**
     * 判断是否有外部存储设备sdcard
     * @return
     */
    private static boolean isSdcardExist() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            return true;
        }
        return false;
    }

    /**
     * 获取全部pcm文件列表
     * @return
     */
    public static List<File> getPcmFiles(){
        List<File> list = new ArrayList<>();
        String fileBasePath = AUDIO_PCM_BASEPATH;
        File dirFile = new File(fileBasePath);
        if (dirFile.exists()){
            File[] files = dirFile.listFiles();
            for (File file :
                    files) {
                list.add(file);
            }
        }
        return list;
    }

    public static List<File> getWavFiles(){
        List<File> list = new ArrayList<>();
        String fileBasePath = AUDIO_WAV_BASEPATH;
        File dirFile = new File(fileBasePath);
        if (dirFile.exists()){
            File[] files = dirFile.listFiles();
            for (File file:files){
                list.add(file);
            }
        }
        return list;
    }
}
