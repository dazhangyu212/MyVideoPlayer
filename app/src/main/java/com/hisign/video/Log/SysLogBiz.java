package com.hisign.video.Log;

import android.os.Environment;


import com.hisign.video.finalvalues.ConstPath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @描述：系统错误日志类
 *
 * @公司：北京海鑫科金高科技股份有限公司
 * @作者：刘硕
 * @创建时间 2015年11月20日
 */
public class SysLogBiz {

	static String TAG = "SysLogService";

	/**
	 * 保存错误信息到文件中
	 * 
	 * @return 返回文件名称,便于将文件传送到服务器
	 */
	public static String saveCrashInfoFile(String content) {
		try {
			String fileName = null;
			File configFile = new File(ConstPath.CAUGHT_EXCEPTION_FILE);
			if (!configFile.exists()) {
				configFile.mkdirs();
			}
			
			if (configFile.exists()) {
				DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
				long timestamp = System.currentTimeMillis();
				String time = formatter.format(new Date());
				fileName = "CaughtException-" + time + "-" + timestamp + ".txt";
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					String path = ConstPath.PROJECT_ROOT_PATH + "LogForCaughtException/";
					File dir = new File(path);
					if (!dir.exists()) {
						dir.mkdirs();
					}
					FileOutputStream fos = new FileOutputStream(path + fileName);
					fos.write(content.getBytes());
					fos.close();
				}
			}
			return fileName;
		} catch (Exception e) {
			SysLogBiz.saveCrashInfoFile(SysLogBiz.getExceptionInfo(e));
		}
		return null;
	}

	/**
	 * 获取Exception中的信息
	 * 
	 * @param e
	 * @return
	 */
	public static String getExceptionInfo(Exception e) {
		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		e.printStackTrace(printWriter);
		Throwable cause = e.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		printWriter.close();
		return writer.toString();
	}
}
