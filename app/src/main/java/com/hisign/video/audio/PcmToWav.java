package com.hisign.video.audio;

import java.io.File;
import java.util.List;

/**
 * 描述：将pcm文件转化为wav文件
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/24
 */

class PcmToWav {
    /**
     * 合并多个PCM文件为一个wav文件
     * @param filePathList    pcm文件路径集合
     * @param destinationPath 目标wav文件路径
     * @return true|false
     */
    public static boolean mergePCMFilesToWAVFile(List<String> filePathList,String destinationPath){
        File[] files = new File[filePathList.size()];
        byte[] buffer = null;
        int  TOTAL_SIZE = 0;
        int fileNum = filePathList.size();
        for (int i= 0;i<fileNum;i++){
            files[i] = new File(filePathList.get(i));
            TOTAL_SIZE += files[i].length();
        }

        WaveHeader waveHeader = new WaveHeader();
        return true;
    }
}
