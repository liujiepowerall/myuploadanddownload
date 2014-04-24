package com.powerall.plugin.upanddown;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

import android.util.Log;

public class FileInfo {
	
	private static final String TAG = "UploadAndDownLoadPlugin";
	
	public static long getFileSizes(File file){
		  long s = 0;
		  if (file.exists()){
			  try{
			   FileInputStream fis = null;
			   fis = new FileInputStream(file);
			   s = fis.available();
			   fis.close();
			  }catch(Exception e){
				  Log.d(TAG, e.toString());
			  }
		  }else{
			   Log.d(TAG,"the file is not exists");
		  }
		  return s;
	}
	
	public static String getFileMD5(File file) {			
		   if (!file.isFile()) {
				Log.d(TAG,"the file is not file");
				return null;
		   }
		   MessageDigest digest = null;
		   FileInputStream in = null;
		   byte buffer[] = new byte[1024];
		   int len;
		   try {
		    digest = MessageDigest.getInstance("MD5");
		    in = new FileInputStream(file);
		    while ((len = in.read(buffer, 0, 1024)) != -1) {
			 digest.update(buffer, 0, len);
		    }
		    in.close();
		   } catch (Exception e) {
		    e.printStackTrace();
		    return null;
		   }
		   BigInteger bigInt = new BigInteger(1, digest.digest());
		   return bigInt.toString(16);
		  }
}
