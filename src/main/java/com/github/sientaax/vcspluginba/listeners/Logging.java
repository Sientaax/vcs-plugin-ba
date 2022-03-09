package com.github.sientaax.vcspluginba.listeners;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class Logging {

    private File file;
    private File newFile;
    private File newTextFile;
    private BufferedWriter bufferedWriter;

    public Logging(String fileName) {
        file = new File(System.getProperty("user.home"));
        newFile = new File(file, ".VersionBuddy-Log");
        newTextFile = new File(newFile.getAbsolutePath(), fileName + ".txt");
        if (!newFile.exists()) {
            newFile.mkdir();
            try {
                newTextFile.createNewFile();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        try{
            bufferedWriter = new BufferedWriter(new java.io.FileWriter(newTextFile.getAbsoluteFile(), true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendToFile(String message){
        if(newTextFile.exists()) {
            try{
                bufferedWriter.write(message);
                bufferedWriter.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void close(){
        try{
            bufferedWriter.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
