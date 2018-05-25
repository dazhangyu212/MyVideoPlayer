package com.hisign.video.audio;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 描述：将pcm文件转化为wav文件
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/24
 */

class PcmToWav {

    /**
     *
     */
    public static final String TAG = "PcmToWav";
    /**
     * 合并多个PCM文件为一个wav文件
     * @param filePathList    pcm文件路径集合
     * @param destinationPath 目标wav文件路径
     * @return true|false
     */
    public static boolean mergePCMFilesToWAVFile(List<String> filePathList,String destinationPath){
        Log.i(TAG,"is merging PCM files");
        File[] files = new File[filePathList.size()];
        byte[] buffer = null;
        int  TOTAL_SIZE = 0;
        int fileNum = filePathList.size();
        for (int i= 0;i<fileNum;i++){
            files[i] = new File(filePathList.get(i));
            TOTAL_SIZE += files[i].length();
        }
        //填入参数,比特率等等.这里用的是16bit单声道8000hz
        WaveHeader waveHeader = new WaveHeader();
        /*
            长度字段 = 内容的大小(TOTAL_SIZE)
            头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
         */
        waveHeader.fileLength = TOTAL_SIZE + (44 - 8);
        waveHeader.fmtHdrLength = 16;
        waveHeader.bitsPerSample = 16;
        waveHeader.channels = 2;
        waveHeader.formatTag = 0x0001;
        waveHeader.samplesPerSec = 8000;
        waveHeader.blockAlign = (short) (waveHeader.channels*waveHeader.bitsPerSample/8);
        waveHeader.avgBytesPerSec = waveHeader.blockAlign*waveHeader.samplesPerSec;
        waveHeader.dataHdrLength = TOTAL_SIZE;

        byte[] bytes = null;
        try {
            bytes = waveHeader.getHeader();
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
            return false;
        }
        if (bytes.length != 44){
            //WAV标准,头部应该是44字节,如果不是44字节则不进行转换文件
            return false;
        }
        //先删除目标文件
        File destFile = new File(destinationPath);
        if (destFile.exists()){
            destFile.delete();
        }
        //合成所有的PCM文件的数据,写到目标文件

        buffer = new byte[1024*4];
        InputStream inStream = null;
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(destinationPath));
            outputStream.write(bytes,0,bytes.length);
            for (int j = 0;j<fileNum;j++){
                inStream = new BufferedInputStream(new FileInputStream(files[j]));
                int size = inStream.read(buffer);
                while (size != -1){
                    outputStream.write(buffer);
                    size = inStream.read(buffer);
                }
                inStream.close();
            }
            outputStream.close();
        }catch (FileNotFoundException e){
            Log.e(TAG,e.getMessage());
            return false;
        }catch (IOException e){
            Log.e(TAG,e.getMessage());
            return false;
        }
        clearFiles(filePathList);
        Log.i(TAG, "mergePCMFilesToWAVFile  success!" + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(new Date()));
        return true;
    }

    /**
     * 将一个PCM文件转化为wav文件
     * @param pcmPath pcm文件路径
     * @param destinationPath 目标文件路径(wav)
     * @param deletePcmFiles 是否删除源文件
     * @return
     */
    public static boolean makePCMFileToWAVFile(String pcmPath,String destinationPath,boolean deletePcmFiles){
        byte[] buffer = null;
        int totalSize = 0;
        File file = new File(pcmPath);
        if (!file.exists()){
            return false;
        }
        totalSize = (int)file.length();
        //16bit单声道,8000hz
        WaveHeader waveHeader = new WaveHeader();
        //长度字段 = 内容的大小
        //头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        waveHeader.fileLength = totalSize+(44-8);
        waveHeader.fmtHdrLength = 16;
        waveHeader.bitsPerSample = 16;
        waveHeader.channels = 2;
        waveHeader.formatTag = 0x0001;
        waveHeader.samplesPerSec = 8000;
        waveHeader.blockAlign = (short) (waveHeader.channels*waveHeader.bitsPerSample/8);
        waveHeader.avgBytesPerSec = waveHeader.blockAlign*waveHeader.samplesPerSec;
        waveHeader.dataHdrLength = totalSize;

        byte[] bytes = null;
        try {
            bytes = waveHeader.getHeader();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        if (bytes.length != 44){
            // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
            return false;
        }
        //先删除目标文件
        File destFile = new File(destinationPath);
        if (!destFile.getParentFile().exists()){
            destFile.getParentFile().mkdirs();
            Log.i(TAG,"create dirs");
        }
        if (destFile.exists()){
            destFile.delete();
        }

        buffer = new byte[1024*4];
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(destinationPath));
            outputStream.write(bytes,0,bytes.length);
            inputStream = new BufferedInputStream(new FileInputStream(file));
            int size = inputStream.read(buffer);
            while (size != -1){
                outputStream.write(buffer);
                size = inputStream.read(buffer);
            }
            inputStream.close();
            outputStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG,e.getMessage());
            return false;
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
            return false;
        }
        if (deletePcmFiles){
            file.delete();
        }
        Log.i(TAG, "makePCMFileToWAVFile  success!" + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(new Date()));
        return true;
    }

    /**
     * 清除文件
     * @param filePathList
     */
    private static void clearFiles(List<String> filePathList) {
        for (int i = 0;i<filePathList.size();i++){
            File file = new File(filePathList.get(i));
            if (file.exists()){
                file.delete();
            }
        }
    }
}
