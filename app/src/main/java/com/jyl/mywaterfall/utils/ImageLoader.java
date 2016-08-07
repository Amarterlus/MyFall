package com.jyl.mywaterfall.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.LruCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by teacher on 2016/8/6.
 */
public class ImageLoader {
    private static ImageLoader mImageLoader;

    private LruCache<String, Bitmap> mLruCache;
    private DiskLruCache mDiskLruCache;

    private ImageLoader(Context context) {
        //初始化内存缓存
        initMemoryCache();
        //初始化本地缓存
        initDiskCache(context);

    }
    //懒汉可以传参，饿汉不可以
    public static ImageLoader getInstance(Context context){
        if (mImageLoader == null){
            mImageLoader = new ImageLoader(context);
        }
        return mImageLoader;
    }

    private void initDiskCache(Context context) {
        File directory = Utils.getDiskCacheDir(context,"thumb");
        int appVersion = Utils.getAppVersion(context);
        int valueCount = 1;
        long maxSize = 32 * 1024 * 1024;//32M
        try {
            mDiskLruCache = DiskLruCache.open(directory, appVersion, valueCount, maxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getFromDiskCache(String url,int displayWidth){
        FileInputStream fis = null;
        FileDescriptor fd = null;
        //key会被用作文件名，故md5加密
        String key = Utils.hashKeyForDisk(url);
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot == null){
                //3.从网络上获取
                DiskLruCache.Editor edit = mDiskLruCache.edit(key);
                OutputStream outputStream = edit.newOutputStream(0);

                boolean success = Utils.downloadUrlToStream(url, outputStream);
                if (success){
                    edit.commit();
                }else{
                    edit.abort();
                }
                //下载完成之后，从本地重新获取
                snapshot = mDiskLruCache.get(key);
            }

            if (snapshot != null){
                fis = (FileInputStream) snapshot.getInputStream(0);
                //文件描述符，相当于文件的指针
                fd = fis.getFD();
                Bitmap bitmap = Utils.decodeSampledBitmapFromFileDescriptor(fd, displayWidth);
                if (bitmap != null){
                    //往内存里存一份
                    addToMemoryCache(url,bitmap);

                    return bitmap;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fd == null && fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private void initMemoryCache() {
        //当前APP占用的运行内存大小
        int maxSize = (int) (Runtime.getRuntime().maxMemory() / 8);
        //告知LruCache图片的大小
        mLruCache = new LruCache<String,Bitmap>(maxSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //告知LruCache图片的大小
                return value.getByteCount();
            }
        };
    }

    //从内存缓存中取
    public Bitmap getFromMemoryCache(String url){
        if (!TextUtils.isEmpty(url)){
            Bitmap bitmap = mLruCache.get(url);
            return bitmap;
        }

        return null;
    }
    //保存到内存缓存
    public void addToMemoryCache(String url,Bitmap bitmap){
        if (!TextUtils.isEmpty(url)){
            mLruCache.put(url,bitmap);
        }
    }

    //加载图片
    public Bitmap loadImage(String url,int columnWidth){
        //1.从内存取
        Bitmap bitmap = getFromMemoryCache(url);
        if (bitmap == null){
            //2.从本地取，保存到内存中
            //3.从网络加载，保存到本地和内存
            bitmap = getFromDiskCache(url,columnWidth);
        }

        return bitmap;
    }
}
