package com.jyl.mywaterfall.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

	// 设置本地缓存文件
	public static File getDiskCacheDir(Context context, String uniqueName) {
		String path = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			// sdcard/Android/包名/cache/
			//官方认定的外部存储路径，应用卸载之后会随之删除
			path = context.getExternalCacheDir().getPath();

		} else {
			// data/data/包名/cache/
			// 内部sd卡缓存路径，应用卸载之后随之清空
			path = context.getCacheDir().getPath();
		}
		File file = new File(path + File.separator + uniqueName);
		if (!file.exists()) {
			file.mkdirs();
		}

		return file;
	}

	// 拿到App版本号
	public static int getAppVersion(Context context) {

		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;

	}

	// MD5加密
	public static String hashKeyForDisk(String key) {

		String hashkey = null;

		try {
			MessageDigest instance = MessageDigest.getInstance("MD5");
			instance.update(key.getBytes());
			byte[] digest = instance.digest();
			// 将byte数组转换成16进制字符串
			hashkey = bytesToHexString(digest);
		} catch (NoSuchAlgorithmException e) {
			// 如果有异常，直接hash值
			hashkey = String.valueOf(key.hashCode());
		}
		return hashkey;
	}

	// 将byte数组转换成16进制字符串
	private static String bytesToHexString(byte[] digest) {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < digest.length; i++) {
			// 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
			String hexString = Integer.toHexString(0xff & digest[i]);
			// 每个字节8位，转为16进制标志，2个16进制位
			if (hexString.length() == 1) {
				sb.append(0);
			}
			sb.append(hexString);
		}

		return sb.toString();
	}
	
	/**
	 *  建立HTTP请求，获取图片
	 *  
	 * @param urlString  
	 *                图片的URL地址
	 * @param outputStream
	 *                文件输入流
	 * @return 成功返回true
	 */
	public static boolean downloadUrlToStream(String urlString,
			OutputStream outputStream) {
		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		BufferedInputStream in = null;
		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(),
					8 * 1024);
			out = new BufferedOutputStream(outputStream, 8 * 1024);
			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
			}
			return true;
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth) {
		// 源图片的宽度
		final int width = options.outWidth;
		int inSampleSize = 1;
		if (width > reqWidth) {
			// 计算出实际宽度和目标宽度的比率
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = widthRatio;
		}
		return inSampleSize;
	}
	
	//返回压缩的图片
	public static Bitmap decodeSampledBitmapFromFileDescriptor(FileDescriptor fd,
			int reqWidth) {
		// First decode with inJustDecodeBounds=true to check dimensions
		//不加入到内存中，但可以获取图片的参数
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fd, null, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFileDescriptor(fd, null, options);
	}

}
