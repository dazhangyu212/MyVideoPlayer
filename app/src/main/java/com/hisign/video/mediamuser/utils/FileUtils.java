package com.hisign.video.mediamuser.utils;

import android.util.Log;

import com.hisign.video.finalvalues.ConstPath;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/29
 */

class FileUtils {
    /**
     *
     */
    public static final String DIR_TAG = "MediaXuser-";
    /**
     *
     */
    public static final String TAG = DIR_TAG+"FileUtils";

    private static final String MAIN_DIR_NAME = "/android_records";

    private static final String BASE_VIDEO = "/video/";

    private static final String BASE_EXT = ".mp4";

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");

    private String currentFileName = "-";

    private String nextFileName;

    public boolean requestSwapFile(boolean force) {
        String fileName = getFileName();
        boolean isChanged = false;
        if (!currentFileName.equalsIgnoreCase(fileName)){
            isChanged = true;
        }
        if (isChanged || force){
            nextFileName = getSaveFilePath(fileName);
            return true;
        }
        return false;
    }

    private String getSaveFilePath(String fileName) {
        currentFileName = fileName;
        StringBuilder fullPath = new StringBuilder(ConstPath.ROOT_PATH);
        //检查内置卡的空间剩余容量,bong清理
        checkSpace();
        fullPath.append(MAIN_DIR_NAME);
        fullPath.append(BASE_VIDEO);
        fullPath.append(fileName);
        fullPath.append(BASE_EXT);

        String string = fullPath.toString();
        File file = new File(string);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()){
            parentFile.mkdirs();
        }
        return string;
    }

    /**
     * 检查剩余空间
     */
    private void checkSpace() {
        StringBuilder fullpath = new StringBuilder();
        String checkPath = ConstPath.ROOT_PATH;
        fullpath.append(checkPath);
        fullpath.append(MAIN_DIR_NAME);
        fullpath.append(BASE_VIDEO);
        if (checkCardSpace(checkPath)){
            File file = new File(fullpath.toString());
            if (!file.exists()){
                file.mkdirs();
            }
            String[] fileNames = file.list();
            if (fileNames.length <1){
                return;
            }
            List<String> fileNameLists = Arrays.asList(fileNames);
            Collections.sort(fileNameLists);
            for (int i = 0;i<fileNameLists.size()&&checkCardSpace(checkPath);i++){
                //清理视频
                String removeFileName = fileNameLists.get(i);
                File removeFile = new File(file,removeFileName);
                boolean result = removeFile.delete();
                if (result){
                    Log.i(TAG, "checkSpace: 删除文件"+removeFileName);
                }else {
                    Log.e(TAG, "删除文件失败 " + removeFile.getAbsolutePath());
                }
            }
        }
    }

    private boolean checkCardSpace(String filePath) {
        File dir = new File(filePath);
        double totalSpace = dir.getTotalSpace();//总大小
        double freeSpace = dir.getFreeSpace();//剩余大小
        if (freeSpace < totalSpace*0.2){
            return true;
        }
        return false;
    }

    private String getFileName() {
        String format = simpleDateFormat.format(System.currentTimeMillis());
        return format;
    }

    public String getNextFileName() {
        return nextFileName;



    }

    public boolean requestSwapFile() {
        return requestSwapFile(false);
    }
}
