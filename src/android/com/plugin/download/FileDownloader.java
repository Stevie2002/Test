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
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
 
public final class FileDownloader {

	private final String PATH = "/data/data/de.house-of-slaves.app/";  //put the downloaded file here
	
	public static String fromUrl(Context context,String imageURL, String fileName) {  //this is the downloader method
		/*
		String result = "";
		try {
			// URL url = new URL("http://yoursite.com/&quot; + imageURL); //you can write here any link
			URL url = new URL(imageURL); //you can write here any link
			// String path = context.getCacheDir().toString();
			String path = Environment.getExternalStorageDirectory().toString();
			File file = new File(path+"/"+fileName);
			long startTime = System.currentTimeMillis();
			result += "download begining\n";
			result += "download url:" + url + " \n";
			result += "downloaded file name:" + fileName + "\n";
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

			 FileOutputStream fos = new FileOutputStream(file);
			 fos.write(buffer.toByteArray());
			 fos.close();
			
			result += "download ready in"
							+ ((System.currentTimeMillis() - startTime) / 1000)
							+ " sec";

		} catch (IOException e) {
			result = "Error: " + e;
		}
		
		return result;
		*/
	}
	
	public static String getUpdate(Contect context, String updateURL,String fileName) {
		String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
		final Uri uri = Uri.parse("file://" + destination+fileName);

		//Delete update file if exists
		File file = new File(destination+fileName);
		if (file.exists())
			file.delete();

		//get url of app on server
		String url = context.getString(updateURL);

		//set downloadmanager
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
		request.setDescription(context.getString(R.string.notification_description));
		request.setTitle(context.getString(R.string.app_name));

		//set destination
		request.setDestinationUri(uri);

		// get download service and enqueue file
		final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		final long downloadId = manager.enqueue(request);

		//set BroadcastReceiver to install app when .apk is downloaded
		BroadcastReceiver onComplete = new BroadcastReceiver() {
			public void onReceive(Context ctxt, Intent intent) {
				Intent install = new Intent(Intent.ACTION_VIEW);
				install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				install.setDataAndType(uri,
						manager.getMimeTypeForDownloadedFile(downloadId));
				startActivity(install);

				unregisterReceiver(this);
				finish();
			}
		};
		//register receiver for when .apk download is compete
		registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}
}
