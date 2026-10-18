package com.example.sariManang;

import android.content.Context;
import android.content.SharedPreferences;

public class userIDManager {
    private final String name = "MyUserPrefs";
    private final String userID = "userID";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public userIDManager(Context context){
      sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
      editor = sharedPreferences.edit();
    }

    public void saveUserId(String userId){
      editor.putString(userID, userId);
      editor.apply();
    }

    public String getUserID(){
      return sharedPreferences.getString(userID,null);
    }

    public void logout(){
      editor.clear();
      editor.apply();
    }

  }
