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

public class MainActivity extends Activity implements OnClickListener{

	private Button mChangeBut;
	private Button mPictureBut;
	private Button mPreviewBut;
	private Button mModeBut;
	public SurfaceView mSurfaceView;
	private TextView camInfo;
	
	//private MyCamera mCamera;
	private StringBuilder str;
	
	private ServerThread server = null;
	private FileServerThread fileServer = null;
	private Handler mServerHandler = null;
	
	private RtspServer mRtspServer;
	private Imx53ServerApplication mApplication;
	
	private static final String TAG = "MainActivity";
	private static final String PATH = "/sdcard/";
	private static final String PIC_DONE = "pic_done";
	private static final String PIC_FAIL = "pic_fail";
	
	private boolean isFlip = false;

	
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
		
		//this.mCamera = MyCamera.getInstance();
		//MyCamera.getInstance().openCamera();
		MyCamera.getInstance().setSurface(mSurfaceView);
		//MyCamera.getInstance().prepareAndroidCamera();
		MyCamera.getInstance().setNum(0);
		
		SessionBuilder.getInstance().setSurfaceHolder(MyCamera.getInstance().getHolder());
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

				}else if(str.equals("flip")){
					Log.d(TAG,str);
					flipFun();
				}
			}
			
		};
		
		this.server = new ServerThread(mServerHandler);
		this.server.start();
		
		mPreviewBut.setOnClickListener(this);
		mPictureBut.setOnClickListener(this);
		mChangeBut.setOnClickListener(this);
		mModeBut.setOnClickListener(this);
	
	}

	private void modeFun(){
//		if(MyCamera.getInstance().getMode() == 0){
			MyCamera.getInstance().setMode(1);
//			//JniCamera.setMode(1);
//			MainActivity.this.mModeBut.setText("Preview Mode");
//			MainActivity.this.mChangeBut.setClickable(false);
//		}else{
//			MyCamera.getInstance().setMode(0);
//			//JniCamera.setMode(0);
//			MainActivity.this.mModeBut.setText("Picture Mode");
//			MainActivity.this.mChangeBut.setClickable(true);
//		}
	}
	
	private void changeCamFun(){
		if(MyCamera.getInstance().changeCam() < 0){
			Log.e(TAG,"write gpio failed");
		}else{
			MainActivity.this.str.replace(str.length()-1, str.length(), 
					String.valueOf(MyCamera.getInstance().getNum()));
			MainActivity.this.camInfo.setText(MainActivity.this.str.toString());
		}	
	}
	
	private void takePicFun(){
//		if(MyCamera.getInstance().getMode() == 0){ //use android's take picture method
//			if(MyCamera.getInstance().isPriv()){
//				Log.d(TAG,"take pcture android");
//				String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/test"+MyCamera.getInstance().getNum()+".jpg";
//				MyCamera.getInstance().shootAt(path);
//			}else{
//				Toast.makeText(MainActivity.this, "start prewview before take a picture", Toast.LENGTH_SHORT).show();
//			}
//		}else{          //use jni take picture method
			Log.d(TAG,"take pcture jni");
			if(MyCamera.getInstance().takePicture() <0){
				Log.e(TAG,PIC_FAIL);
				server.piture_done(PIC_FAIL);
			}else{
				Log.d(TAG,PIC_DONE);
				server.piture_done(PIC_DONE);
			}
//		}
	}
	
	private void flipFun(){
		if(this.isFlip == false){
			MyCamera.getInstance().flip();
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
		MyCamera.getInstance().setMode(0);
		MyCamera.getInstance().clearNum();
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

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.change:
			changeCamFun();
			break;
		case R.id.mode:
			modeFun();
			break;
		case R.id.preview:
			//previewFun();
			break;
		case R.id.take_picture:
			takePicFun();
			break;
		
		}
		
	}

	

}
