package com.example.myfirstapp;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cc.noharry.blelib.util.L;

public class AdcDataManager {

    public static int num = 0;

    public static boolean saveToSd(String path,String data) {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            L.i("SD CARD NOT FOUND");
            return false;
        }

        if(path=="No Path"){
            L.i(path);
            return false;
        }

        File dataLog = new File(path+"/"+"dataLog.txt");
        if(!dataLog.exists()) {
            try {
                dataLog.createNewFile();
                FileOutputStream dataLogWriteStream = new FileOutputStream(dataLog);
                dataLogWriteStream.write("1".getBytes());
                num=1;
                dataLogWriteStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }else{
            try {
                FileInputStream dataLogReadStream = new FileInputStream(dataLog);
                // 代表从开头写
                FileOutputStream dataLogWriteStream = new FileOutputStream(dataLog,false);
                byte[] b = new byte[10];
                dataLogReadStream.read(b);
                num=Integer.getInteger(b.toString());
                num++;
                dataLogWriteStream.write(String.valueOf(num).getBytes());
                dataLogReadStream.close();
                dataLogWriteStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        String filename = "ADCDATA"+num+".txt";
        File dataFile = new File(path+"/"+filename);
        if(!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        try {
            FileOutputStream dataOutputStream = new FileOutputStream(dataFile,true);
            dataOutputStream.write(data.getBytes());
            dataOutputStream.close();
            L.i(filename+"WRITE COMPLETED!");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
