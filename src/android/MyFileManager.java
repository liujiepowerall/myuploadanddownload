package com.powerall.plugin.upanddown;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.cordova.CallbackContext;

import com.phonegap.ecommunity.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;
public class MyFileManager extends Dialog {
	
	private final static String TAG = "UploadAndDownLoadPlugin";
	private List<String> items = null;
	private List<String> paths = null;
	private String rootPath = "/";
	private String curPath = "/";
	private ListView mListView;
	private Context mContext;
	private UploadAndDownloadPlugin mPlugin;
	private CallbackContext mCallbackContext;
	private String downloadPath;
	
	public int mStatus;
	private File mSourceFile;
	
	public MyFileManager(Context context,CallbackContext callbackContext,UploadAndDownloadPlugin plugin) {
		super(context);
		// TODO Auto-generated constructor stub
		mContext = context;
		mPlugin = plugin;
		mCallbackContext = callbackContext;
	}
	
	@Override
	protected void onCreate(Bundle icicle) {
		//super.onCreate(icicle);
		setContentView(R.layout.fileselect);
		mListView = (ListView) findViewById(R.id.file_list);
		mListView.setOnItemClickListener(new OnItemClickListener(){   
		   @Override  
		   public void onItemClick(AdapterView<?> parent, View view,int position, long id) {  
			   	File file = new File(paths.get(position));
				if (file.isDirectory()) {
					curPath = paths.get(position);
					getFileDir(paths.get(position));
				} else {
					if(mStatus == UploadAndDownloadPlugin.STATUS_UPLOAD){
						view.setBackgroundColor(Color.YELLOW);
						mSourceFile = file;
					}
				}
		   }  
		});  
		Button buttonConfirm = (Button) findViewById(R.id.buttonConfirm);
		buttonConfirm.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(mStatus == UploadAndDownloadPlugin.STATUS_UPLOAD){
					mPlugin.uploadFile("http://192.168.10.241", mSourceFile);
				}else{
					mContext.registerReceiver(mPlugin.receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));  
					mPlugin.setStoragePath(curPath);
					mPlugin.downloadFile(curPath, downloadPath);
				}				
				dismiss();
			}
		});
		Button buttonCancle = (Button) findViewById(R.id.buttonCancle);
		buttonCancle.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				dismiss();
			}
		});
		getFileDir(rootPath);
	}

	private void getFileDir(String filePath) {
		
		Log.d(TAG,"filePath=>"+filePath);
		File f = new File(filePath);
		File[] files = f.listFiles();
		Log.d(TAG,"files is null?=>"+(files == null));
		if(null == files){
			createDialog().show();
			return;
		}
		//mPath.setText(filePath);
		items = new ArrayList<String>();
		paths = new ArrayList<String>();
		if (!filePath.equals(rootPath)) {
			items.add("b1");
			paths.add(rootPath);
			items.add("b2");
			paths.add(f.getParent());
		}
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			items.add(file.getName());
			paths.add(file.getPath());
		}

		mListView.setAdapter(new MyAdapter(mContext, items, paths));
	}
	
	
	private AlertDialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("error");
        //builder.setIcon(R.drawable.icon);
        builder.setMessage("you dont have premmison,please check else!");
        return builder.create();
    }
	
	public String getCurrentPath(){
		return curPath;
	}
	
	public void setDownloadPath(String path){
		downloadPath = path;
	}
	
	public void setStatus(int status){
		mStatus = status;
	}
}