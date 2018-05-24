package com.hisign.video.audio;

/**
 * 描述：获取录音的音频流,用于拓展的处理
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/24
 */

public interface RecordStreamListener {
    void recordOfByte(byte[] data,int begin,int end);
}
