package com.plugin.download;
 
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;
 
public final class FileDownloader {

	private final String PATH = "/data/data/de.house-of-slaves.app/";  //put the downloaded file here
	
	public static String fromUrl(String imageURL, String fileName) {  //this is the downloader method
		try {
			// URL url = new URL("http://yoursite.com/&quot; + imageURL); //you can write here any link
			String result = "";
			URL url = new URL(imageURL); //you can write here any link
			File file = new File("/data/data/de.house-of-slaves.app/"+fileName);

			long startTime = System.currentTimeMillis();
			result += "download begining\n";
			result += "download url:" + url + " \n";
			result += "downloaded file name:" + fileName + "\n";
			/* Open a connection to that URL. */
			URLConnection ucon = url.openConnection();

			/*
			 * Define InputStreams to read from the URLConnection.
			 */
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
			result += "ImageManager", "Error: " + e);
		}
		
		return result;
	}
}
