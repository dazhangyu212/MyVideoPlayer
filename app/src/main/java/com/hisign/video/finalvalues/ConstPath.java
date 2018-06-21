package com.hisign.video.finalvalues;

import android.os.Environment;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/22
 */

public class ConstPath {
    /**
     * Sd卡路径
     */
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getPath();

    /**
     *
     */
    public static final String PROJECT_ROOT_PATH = ROOT_PATH+"/MyVideoPlayer/";

    /**
     * 此文件如果存在,就开启被捕获异常的日志输出
     */
    public static String CAUGHT_EXCEPTION_FILE = PROJECT_ROOT_PATH + "LogForCaughtException/";

    /**
     * 存放MP4等视频文件
     */
    public static final String VIDEO_DIRECTORY_PATH = PROJECT_ROOT_PATH+"video/";

    /**
     *
     */
    public static final String AUDIO_DIRECTORY_PATH = PROJECT_ROOT_PATH+"audio/";
}
