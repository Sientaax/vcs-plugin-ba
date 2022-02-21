package com.github.sientaax.vcspluginba.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class CreateJson {

    public static JsonObject createJsonRefresh(List<String> commitMessageLog, List<String> dateLog){
        JsonArray jsonArray = new JsonArray();
        for(int i = 0; i < commitMessageLog.size(); i++){
            if(!commitMessageLog.get(i).equals("interimCommit")) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("message", commitMessageLog.get(i));
                jsonObject.addProperty("date", dateLog.get(i));
                jsonArray.add(jsonObject);
            }
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

    public static JsonObject createJsonLogCounter(String message){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("logCounter", message);
        return jsonObject;
    }

    public static JsonObject createJsonBranchExists(String message){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("branchExists", message);
        return jsonObject;
    }
}
