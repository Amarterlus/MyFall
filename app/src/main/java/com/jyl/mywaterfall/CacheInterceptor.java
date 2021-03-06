package com.jyl.mywaterfall;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.jyl.mywaterfall.utils.ToastUtil;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Jyl on 2016/8/3.
 */
public class CacheInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        if (!isNetworkReachable(AppConfig.sContext)) {
            AppConfig.sHandler.post(new Runnable() {
                @Override
                public void run() {
                    ToastUtil.showToast(AppConfig.sContext,"暂无网络");
                }
            });
            request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)//无网络时只从缓存中读取
                    .build();
        }

        Response response = chain.proceed(request);
        if (isNetworkReachable(AppConfig.sContext)) {
            int maxAge = 60 * 60; // 有网络时 设置缓存超时时间1个小时
            response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, max-age=" + maxAge)
                    .build();
        } else {
            int maxStale = 60 * 60 * 24 * 28; // 无网络时，设置超时为4周
            response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                    .build();
        }
        return response;
    }

    /**
     * 判断网络是否可用
     *
     * @param context Context对象
     */
    public static Boolean isNetworkReachable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo current = cm.getActiveNetworkInfo();
        if (current == null) {
            return false;
        }
        return (current.isAvailable());
    }
}
