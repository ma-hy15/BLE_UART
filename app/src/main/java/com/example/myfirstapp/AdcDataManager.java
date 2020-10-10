package com.example.myfirstapp;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;

import cc.noharry.blelib.util.L;

public class AdcDataManager {

    public static int num = 0;

    public static boolean saveToSd(String path,String data) {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            L.i("SD CARD NOT FOUND");
            return false;
        }

        if(path=="No Path"){
            L.i(path);
            return false;
        }

        L.i("PATH:"+path);

        File pathDir = new File(path);
        if(!pathDir.exists()){
            pathDir.mkdir();
        }

        File dataLog = new File(path+"/"+"dataLog.txt");
        if(!dataLog.exists()) {
            try {
                dataLog.createNewFile();
                FileWriter writer = new FileWriter(dataLog);
                writer.write("1");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }else{
            try {
                BufferedReader reader =new BufferedReader(new FileReader(dataLog.getAbsolutePath()));
                String numStr=reader.readLine();
                num = Integer.parseInt(numStr);
                reader.close();
                num++;
                // 代表从开头写
                FileWriter writer = new FileWriter(dataLog);
                writer.write(String.valueOf(num));
                writer.close();
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
