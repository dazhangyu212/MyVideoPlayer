package com.hisign.video.audio;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/24
 */

public class WaveHeader {
    public  final char fileID[] = {'R','I','F','F'};
    public int fileLength;
    public char wavTag[] = {'W','A','V','E'};
    public char fmtHdrID[] = {'f','m','t',' '};
    public int fmtHdrLength;
    public short formatTag;
    public short channels;
    public int samplesPerSec;
    public int avgBytesPerSec;
    public short blockAlign;
    public short bitsPerSample;
    public char dataHdrID[] = {'d','a','t','a'};
    public int dataHdrLength;

    public byte[] getHeader() throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeChar(baos,fileID);
        writeInt(baos,fileLength);
        writeChar(baos,wavTag);
        writeChar(baos,fmtHdrID);
        writeInt(baos,fmtHdrLength);
        writeShort(baos,formatTag);
        writeShort(baos,channels);
        writeInt(baos,samplesPerSec);
        writeInt(baos,avgBytesPerSec);
        writeShort(baos,blockAlign);
        writeShort(baos,bitsPerSample);
        writeChar(baos,dataHdrID);
        writeInt(baos,dataHdrLength);
        Log.i("WAVEHEADER","header:"+baos.toString());
        baos.flush();
        byte[] buf = baos.toByteArray();
        baos.close();
        return buf;
    }

    private void writeShort(ByteArrayOutputStream baos, short num) throws IOException {
        byte[] buffer = new byte[2];
        buffer[1] = (byte) ((num << 16) >> 24);
        buffer[0] = (byte) ((num<<24)>>24);
        baos.write(buffer);
    }

    private void writeInt(ByteArrayOutputStream baos, int num) throws IOException {
        byte[] buffer = new byte[4];
        buffer[1] = (byte) ((num << 16)>>24);
        buffer[0] = (byte) ((num << 24) >> 24);
        baos.write(buffer);
    }

    private void writeChar(ByteArrayOutputStream baos, char[] chars) {
        for (int i=0;i<chars.length;i++){
            char c = chars[i];
            baos.write(c);
        }
    }

}
