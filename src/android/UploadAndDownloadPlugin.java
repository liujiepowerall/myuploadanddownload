package com.powerall.plugin.upanddown;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import com.phonegap.ecommunity.R;

import android.widget.Toast;

public class UploadAndDownloadPlugin extends CordovaPlugin {

	private final String TAG = "UploadAndDownLoadPlugin";
	private Context mContext;
    private SharedPreferences mPrefs;  
    private DownloadManager mDownloadManager;
    private String downloadPath;
    private static final String DL_ID = "upandroiddown.downloadId";
    //private static final String HEADER = "file://";  
    private String mStoragePath;
    
    public static final int STATUS_UPLOAD = 1;
    public static final int STATUS_DOWNLOAD = 2;
    //private boolean mIsStorage = false;
	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        android.util.Log.d(TAG,"action=>"+action); 
        mContext = this.cordova.getActivity();
		if (action.equals("upload")) {
			MyFileManager dg = new MyFileManager(cordova.getActivity(), callbackContext,this);
			dg.setStatus(STATUS_UPLOAD);
			dg.show();	
		}else if(action.equals("download")) {
			downloadPath = args.getString(0);
			//MyFileManager dg = new MyFileManager(cordova.getActivity(), callbackContext,this);
			//dg.setDownloadPath(downloadPath);
			//dg.show();

			downloadFile(null,downloadPath);
			mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));  
			callbackContext.success();
			notification(R.string.download_start);
		}
        return false;
    }
	
	/**
     * 下载apk文件
     */
    @SuppressLint("NewApi")
	public void downloadFile(String storagePath,String path){
    	String fileName = path.substring(path.lastIndexOf('/') + 1);  
    	if(mDownloadManager == null)
    		mDownloadManager = (DownloadManager)mContext.getSystemService(Context.DOWNLOAD_SERVICE);
    	if(mPrefs == null)
    		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    	Log.e(TAG,"mPrefs.contains(DL_ID)=>"+mPrefs.contains(DL_ID));
    	if(!mPrefs.contains(DL_ID)){
    		//deleteFile();  
    		Log.d(TAG,"downloadPath=>"+downloadPath);
            Uri resource = Uri.parse(downloadPath);   
            DownloadManager.Request request = new DownloadManager.Request(resource);   
            request.setAllowedNetworkTypes(Request.NETWORK_MOBILE | Request.NETWORK_WIFI);   
            request.setAllowedOverRoaming(false);   

            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();  
            String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(downloadPath)); 
            Log.d(TAG,"mimeString=>"+mimeString);
            request.setMimeType(mimeString);  

            request.setNotificationVisibility (request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);  
            request.setVisibleInDownloadsUi(true);  
            //Log.d(TAG,"storagePath=>"+storagePath+"==fileName=>"+fileName);
            //request.setDestinationInExternalPublicDir(storagePath, fileName); 

            //request.setDestinationInExternalPublicDir(storagePath, fileName); 
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            //request.setTitle(mContext.getResources().getString(R.string.download_title_in_background) + apkName); 
            long id = mDownloadManager.enqueue(request);
            mPrefs.edit().putLong(DL_ID, id).commit();   
    	}else{
    		queryDownloadStatus();
    	}  	
    }
    
    private void queryDownloadStatus() {   
        DownloadManager.Query query = new DownloadManager.Query();   
        query.setFilterById(mPrefs.getLong(DL_ID, 0));   
        Cursor c = mDownloadManager.query(query); 
        Log.d(TAG,"c.getCount=>"+c.getCount());
        if(c.getCount() == 0){
        	mPrefs.edit().clear().commit(); 
        	downloadFile(mStoragePath,downloadPath);
        }
        if(c.moveToFirst()) {   
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));   
            switch(status) {   
            case DownloadManager.STATUS_PAUSED:   
                Log.d(TAG, "STATUS_PAUSED");  
            case DownloadManager.STATUS_PENDING:   
                Log.d(TAG, "STATUS_PENDING");  
            case DownloadManager.STATUS_RUNNING:   
                Log.d(TAG, "STATUS_RUNNING");  
                break;   
            case DownloadManager.STATUS_SUCCESSFUL:   
                Log.d(TAG, "STATUS_SUCCESSFUL");  
                mContext.unregisterReceiver(receiver);  
                mPrefs.edit().clear().commit(); 
				notification(R.string.download_end);
                break;   
            case DownloadManager.STATUS_FAILED:   
                Log.d(TAG, "STATUS_FAILED");  
                mDownloadManager.remove(mPrefs.getLong(DL_ID, 0));   
                mPrefs.edit().clear().commit(); 
                mContext.unregisterReceiver(receiver); 
				notification(R.string.download_error);
                break;   
            }   
        }  
    }  
    
    public BroadcastReceiver receiver = new BroadcastReceiver() {   
        @Override   
        public void onReceive(Context context, Intent intent) {   
            Log.d(TAG,"download complete");  
            queryDownloadStatus();   
        }   
    };
    
    public void setStoragePath(String path){
    	mStoragePath = path;
    }
    /*
    public void setIsStorage(boolean is){
    	mIsStorage = is;
    }
    */
    
    /**
     * 上传文件至Server的方法
     * @param urlStr 服务器对应的路径
     * @param serverFileName 上传服务器后在服务器上的文件名称 如：image.jpg
     * @param uploadFile 要上传的文件路径 如：/sdcard/a.jpg
     */
    public void uploadFile(String urlStr,File uploadFile){
      String end = "\r\n";
      String twoHyphens = "--";
      String boundary = "*****";
      String fileMd5 = FileInfo.getFileMD5(uploadFile);
	  long fileSize = FileInfo.getFileSizes(uploadFile);
      try{
        URL url =new URL(urlStr);
        HttpURLConnection con=(HttpURLConnection)url.openConnection();
        /* 允许Input、Output，不使用Cache */
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        /* 设置传送的method=POST */
        con.setRequestMethod("POST");
        /* setRequestProperty */
        con.setRequestProperty("Connection", "Keep-Alive");
        //con.setRequestProperty("Charset", "UTF-8");
        con.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);      	
        
        con.setRequestProperty("Authorization", "d:ODdiZThhNjU5MmJiNmM5MjkyZWQ3OTVkMzg3MTYyNTk2MzY1MjRjZg==");
        con.setRequestProperty("Date", new Date().toString());
        con.setRequestProperty("Accept-Encoding", "gzip, deflate");
        con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        con.setRequestProperty("Pragma", "no-cache");
        con.setRequestProperty("Cache-Control", "no-cache");
        con.setRequestProperty("Accept-Charset", "GB2312,utf-8;q=0.7,*;q=0.7");
        con.setRequestProperty("Content-MD5", fileMd5);
        con.setRequestProperty("Range", "0-"+fileSize);
        con.setRequestProperty("Method", "?action=uploadfile");
        con.setRequestProperty("Content-Length", "fileSize");
        
        /* 设置DataOutputStream */
        DataOutputStream ds = new DataOutputStream(con.getOutputStream());
        ds.writeBytes(twoHyphens + boundary + end);
        ds.writeBytes("Content-Disposition: form-data; " + "name=\"file1\";filename=\"" + fileMd5 +"\"" + end);
        ds.writeBytes(end);   

        /* 取得文件的FileInputStream */
        FileInputStream fStream = new FileInputStream(uploadFile);
        /* 设置每次写入1024bytes */
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int length = -1;
        /* 从文件读取数据至缓冲区 */
        while((length = fStream.read(buffer)) != -1){
          /* 将资料写入DataOutputStream中 */
          ds.write(buffer, 0, length);
        }
        ds.writeBytes(end);
        ds.writeBytes(twoHyphens + boundary + twoHyphens + end);

        /* close streams */
        fStream.close();
        ds.flush();

        /* 取得Response内容 */
        InputStream is = con.getInputStream();
        int ch;
        StringBuffer b =new StringBuffer();
        while( ( ch = is.read() ) != -1 ){
          b.append( (char)ch );
        }
        ds.close();
      }
      catch(Exception e){
    	  e.printStackTrace();
      }
    }
	private void notification(int resId){
		Toast toast = Toast.makeText(cordova.getActivity(), cordova.getActivity().getResources().getString(resId), Toast.LENGTH_SHORT); 
		toast.show();
	}
	
}