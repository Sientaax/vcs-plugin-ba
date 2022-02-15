package com.github.sientaax.vcspluginba.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class CreateJson {

    public static JsonObject createJsonRefresh(List<String> commitMessageLog, List<String> dateLog){
        //JsonObject jsonObject = new JsonObject();
        //jsonObject.addProperty("commitMessageLog", "commitMessageLog");
        //for(int i = 0; i < commitMessageLog.size(); i++){
            //jsonObject.addProperty(Integer.toString(i), commitMessageLog.get(i));
        //}
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        for(int i = 0; i < commitMessageLog.size(); i++){
            jsonObject.addProperty("message", commitMessageLog.get(i));
            jsonObject.addProperty("date", dateLog.get(i));
            jsonArray.add(jsonObject);
        }
        JsonObject mainJsonObject = new JsonObject();
        mainJsonObject.add("log", jsonArray);
        return mainJsonObject;
    }

    public static JsonObject createJsonFileObserverNewFile(String message){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("createNewFile", message);
        return jsonObject;
    }

    public static JsonObject createJsonFileObserverDeleteFile(String message){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("deleteAFile", message);
        return jsonObject;
    }

    public static JsonObject createJsonWorkingTime(String message){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("countWorkingTime", message);
        return jsonObject;
    }
}
