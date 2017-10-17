package com.plugin.download;
 
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.DownloadManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
 
public final class FileDownloader {

	private final String PATH = "/data/data/de.house-of-slaves.app/";  //put the downloaded file here
	
	public static File fromUrl(Context context,String imageURL, String fileName) {  //this is the downloader method
		File downloadedFile = null;
		try {
			// URL url = new URL("http://yoursite.com/&quot; + imageURL); //you can write here any link
			URL url = new URL(imageURL); //you can write here any link
			// String path = context.getCacheDir().getAbsolutePath();
			// String path = Environment.getExternalStorageDirectory().getAbsolutePath();
			String path = context.getExternalCacheDir().getAbsolutePath();
			downloadedFile = new File(path+"/"+fileName);
			long startTime = System.currentTimeMillis();
			// result += "download begining\n";
			// result += "download url:" + url + " \n";
			// result += "downloaded file name:" + fileName + "\n";
			URLConnection ucon = url.openConnection();

			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			 //We create an array of bytes
			 byte[] data = new byte[50];
			 int current = 0;

			 while((current = bis.read(data,0,data.length)) != -1){
				   buffer.write(data,0,current);
			 }

			 FileOutputStream fos = new FileOutputStream(downloadedFile);
			 fos.write(buffer.toByteArray());
			 fos.close();
			
			// result += "download ready in"
							// + ((System.currentTimeMillis() - startTime) / 1000)
							// + " sec";

		} catch (Exception e) {}
		
		return downloadedFile;
	}
	
	public static File update(Context context,String imageURL, String fileName) {  //this is the downloader method
		File downloadedFile = null;
		try {
			downloadedFile = fromUrl(context,imageURL,fileName);
			
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(downloadedFile), "application/vnd.android.package-archive");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
			context.startActivity(intent);
		} catch (Exception e) {
			downloadedFile = null;
		}
		
		return downloadedFile;
	}
}
