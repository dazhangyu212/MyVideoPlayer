package com.hisign.video.video;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/22
 */

public class VideoPlayer {

    static {
        System.loadLibrary("native-lib");
    }

    public static native int play(String path,Object surface);
}
