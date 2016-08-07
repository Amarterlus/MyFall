package com.jyl.mywaterfall;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Jyl on 2016/8/6.
 */
public class AppConfig extends Application {
    public static Context  sContext;
    public static Handler  sHandler;
    public static Retrofit sRetrofit;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        sHandler = new Handler();
        initRetrofit();
    }


    private void initRetrofit() {
        File cacheFile = new File(this.getCacheDir(), "CacheData");
        Cache cache = new Cache(cacheFile, 1024 * 1024 * 100); //100Mb
        sRetrofit = new Retrofit.Builder()
                .baseUrl("http://gank.io/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .addInterceptor(new CacheInterceptor())
                        .cache(cache)
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }
}