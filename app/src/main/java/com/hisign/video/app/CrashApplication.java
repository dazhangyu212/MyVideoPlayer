package com.hisign.video.app;

import android.app.Application;

import com.hisign.video.exception.CrashHandler;
import com.hisign.video.finalvalues.ConstPath;

import java.io.File;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/22
 */

public class CrashApplication extends Application {
    private static CrashApplication crashApplication = null;

    public void onCreate() {
        super.onCreate();
        crashApplication = this;
        init();
    }

    private void init() {
        initDirs();
        CrashHandler crashHandlerBiz = CrashHandler.getInstance();
        crashHandlerBiz.init(getApplicationContext());
    }

    private void initDirs() {
        File file = new File(ConstPath.PROJECT_ROOT_PATH);
        if (!file.exists()){
            file.mkdirs();
        }
    }
}
