package com.wx.imx53server;

import java.io.File;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.rtsp.RtspServer.CallbackListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Button mChangeBut;
	private Button mPictureBut;
	private Button mPreviewBut;
	private Button mModeBut;
	private SurfaceView mSurfaceView;
	private TextView camInfo;
	
	private MyCamera mCamera;
	private StringBuilder str;
	
	private ServerThread server = null;
	private FileServerThread fileServer = null;
	private Handler mServerHandler = null;
	
	private RtspServer mRtspServer;
	private Imx53ServerApplication mApplication;
	
	private static final String TAG = "MainActivity";
	private static final String PATH = "/sdcard/";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mApplication = (Imx53ServerApplication) getApplication();
		
		this.mPreviewBut = (Button) findViewById(R.id.preview);
		this.mChangeBut = (Button) findViewById(R.id.change);
		this.mPictureBut  = (Button) findViewById(R.id.take_picture);
		this.mModeBut = (Button) findViewById(R.id.mode);
		this.mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
		this.camInfo = (TextView) findViewById(R.id.cam_info);
		this.str = new StringBuilder(camInfo.getText());
		
		this.mCamera = MyCamera.getInstance();
		mCamera.setSurface(mSurfaceView);
		mCamera.prepareAndroidCamera();
		this.mCamera.setNum(0);
		
		SessionBuilder.getInstance().setSurfaceHolder(mCamera.getHolder());
		this.startService(new Intent(MainActivity.this,RtspServer.class));
		bindService(new Intent(MainActivity.this,RtspServer.class), mRtspServerConnection, BIND_AUTO_CREATE);
		
		this.fileServer = new FileServerThread();
		this.fileServer.start();//call setFile before start thread
		
		this.mServerHandler = new Handler(){
			
			File[] files = new File[8];
			int[] fileLength = new int[8];
			String[] fileName = new String[8];
			
			@Override
			public void handleMessage(Message msg) {
				String str = String.valueOf(msg.obj);
				if(str.equals("change")){
					Log.d(TAG,str);
					changeCamFun();
				}else if(str.equals("pic")){
					Log.d(TAG,str);
					//mRtspServer.stop();
					takePicFun();
				}else if(str.equals("preview")){
					Log.d(TAG,str);
					//previewFun();
				}else if(str.equals("mode")){
					Log.d(TAG,str);
					modeFun();
				}else if(str.equals("send")){
					Log.d(TAG,str);
					File dir = new File(PATH);
					if(dir.exists() && dir.list()!= null){
						for(int i=0;i<8;i++){
					fileName[i] = PATH+"frame_jpeg_new"+i+".JPG";
					Log.d(TAG,fileName[i]);
							files[i] = new File(fileName[i]);
							fileLength[i] = (int)files[i].length();
						}
						fileServer.setFile(fileLength, fileName);
					}

				}
			}
			
		};
		
		this.server = new ServerThread(mServerHandler);
		this.server.start();
		
		mPreviewBut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				previewFun();			
			}
		});
		
		mPictureBut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				takePicFun();								
			}
		});
		
		mChangeBut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				changeCamFun();
			}
		});
		
		mModeBut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				modeFun();
			}
		});
		
		
	}

	private void modeFun(){
		if(mCamera.getMode() == 0){
			mCamera.setMode(1);
			MainActivity.this.mModeBut.setText("Preview Mode");
			MainActivity.this.mChangeBut.setClickable(false);
		}else{
			mCamera.setMode(0);
			MainActivity.this.mModeBut.setText("Picture Mode");
			MainActivity.this.mChangeBut.setClickable(true);
		}
	}
	
	private void changeCamFun(){
		if(mCamera.changeCam() < 0){
			Log.e(TAG,"write gpio failed");
		}else{
			MainActivity.this.str.replace(str.length()-1, str.length(), 
					String.valueOf(MainActivity.this.mCamera.getNum()));
			MainActivity.this.camInfo.setText(MainActivity.this.str.toString());
		}	
	}
	
	private void takePicFun(){
		if(mCamera.getMode() == 0){ //use android's take picture method
			if(mCamera.isPriv()){

				String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/test"+mCamera.getNum()+".jpg";
				mCamera.shootAt(path);
			}else{
				Toast.makeText(MainActivity.this, "start prewview before take a picture", Toast.LENGTH_SHORT).show();
			}
		}else{          //use jni take picture method
			//mCamera.stopPreview();
			//mCamera.release();
			if(mCamera.takePicture() <0){
				Toast.makeText(MainActivity.this, "take picture error", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(MainActivity.this, "take picture success", Toast.LENGTH_SHORT).show();
			}
			//mCamera = MyCamera.getInstance();
			//mCamera.setMode(0);
			//mCamera.setSurface(mSurfaceView);
			//mCamera.prepareAndroidCamera();
			//mCamera.startPreview();
		}
	}
	
	private void previewFun(){
		this.mCamera = MyCamera.getInstance();
		mCamera.setSurface(mSurfaceView);
		mCamera.prepareAndroidCamera();
		if(mCamera.isPriv()){
			mCamera.stopPreview();
			mPreviewBut.setText("start preview");
		}else{
			mCamera.startPreview();
			mPreviewBut.setText("stop preview");
		}
	}
	
private ServiceConnection mRtspServerConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG,"STOP");
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRtspServer = ((RtspServer.LocalBinder)service).getService();
			mRtspServer.addCallbackListener(mRtspCallbackListener);
			mRtspServer.start();
			//displayIpAddress();
			Log.d(TAG,"START");
		}
	};
	
private RtspServer.CallbackListener mRtspCallbackListener = new CallbackListener() {
		
		@Override
		public void onMessage(RtspServer server, int message) {
			if (message==RtspServer.MESSAGE_STREAMING_STARTED) {
				Log.d(TAG,"MESSAGE_STREAMING_STARTED");
			} else if (message==RtspServer.MESSAGE_STREAMING_STOPPED) {
				Log.d(TAG,"MESSAGE_STREAMING_STOPPED");
			}
			
		}
		
		@Override
		public void onError(RtspServer server, Exception e, int error) {
			if (error == RtspServer.ERROR_BIND_FAILED) {
				Log.d(TAG,"ERROR_BIND_FAILED");
			}
			
		}
	};
	
	@Override
	protected void onDestroy() {
		mCamera.setMode(0);
		mCamera.clearNum();
		this.server.release();
		stopService(new Intent(this,RtspServer.class));
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		if(mRtspServer!= null){
			mRtspServer.stop();
			mRtspServer.removeCallbackListener(mRtspCallbackListener);
			unbindService(mRtspServerConnection);
		}
		super.onStop();
	}

	

}
