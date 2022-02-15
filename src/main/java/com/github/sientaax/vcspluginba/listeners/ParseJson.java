package com.github.sientaax.vcspluginba.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ParseJson {

    private JsonObject jsonObject = new JsonObject();
    private String type = "";
    private String data = "";

    public ParseJson(String message){
        parseJsonMethod(message);
    }

    private void parseJsonMethod(String message){
        try {
            jsonObject = new JsonParser().parse(message).getAsJsonObject();
            type = jsonObject.get("id").getAsString();
            data = jsonObject.get("data").getAsString();
        } catch(ClassCastException e){
            e.printStackTrace();
        }
    }

    public String getType(){
        return type;
    }

    public String getData(){
        return data;
    }
}
