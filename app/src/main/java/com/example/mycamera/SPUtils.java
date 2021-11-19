package com.example.mycamera;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class SPUtils {
    private static final String mFileName="myData";
    private static SharedPreferences mSP;

    public SPUtils(Context context) {
        if (mSP == null) {
            mSP = context.getSharedPreferences(mFileName, MODE_PRIVATE);//spfiles.xml
        }
    }

    public static void setPutBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = mSP.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean getGetBoolean(String key) {
        boolean value = mSP.getBoolean(key, false);
        return value;
    }
    public static void deleteContent(String key){
        SharedPreferences.Editor edit = mSP.edit();
        edit.remove(key);
        edit.commit();
    }


}
