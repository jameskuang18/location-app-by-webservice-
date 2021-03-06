package com.example.t_tazhan.production.util;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;

public class FileSave {

    private static final String webResponseData = "/Web_Response_Data";
    public static void saveFile(String str) {
        String filePath;
        boolean hasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (hasSDCard) {
            filePath = Environment.getExternalStorageDirectory().toString()
                    + webResponseData
                    + File.separator
                    + "Web返回的数据"
                    + "_test.txt";
        } else  // 系统下载缓存根目录的hello.text
            filePath = Environment.getDownloadCacheDirectory().toString()
                    + webResponseData
                    + File.separator
                    + "Web返回的数据"
                    + "_test.txt";

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            FileOutputStream outStream = new FileOutputStream(file);
            outStream.write(str.getBytes());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}