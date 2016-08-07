package com.jyl.mywaterfall.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by taojin on 2016/7/7.10:41
 */
public class ToastUtil {

    private static Toast toast;

    public static  void  showToast(Context context, String msg){
        if (toast==null){
            toast = Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_LONG);
        }
        toast.setText(msg);
        toast.show();

    }
}
