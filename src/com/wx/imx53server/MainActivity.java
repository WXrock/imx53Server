package com.wx.imx53server;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
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
	
	private Thread server = null;
	
	private static final String TAG = "MainActivity";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.mPreviewBut = (Button) findViewById(R.id.preview);
		this.mChangeBut = (Button) findViewById(R.id.change);
		this.mPictureBut  = (Button) findViewById(R.id.take_picture);
		this.mModeBut = (Button) findViewById(R.id.mode);
		this.mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
		this.camInfo = (TextView) findViewById(R.id.cam_info);
		this.str = new StringBuilder(camInfo.getText());
		this.mCamera = new MyCamera(mSurfaceView);
		this.mCamera.setNum(0);
		
		mPreviewBut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mCamera.isPriv()){
					mCamera.stopPreview();
					mPreviewBut.setText("start preview");
				}else{
					mCamera.startPreview();
					mPreviewBut.setText("stop preview");
				}
				
			}
		});
		
		mPictureBut.setOnClickListener(new OnClickListener() {
			private String path;
			@Override
			public void onClick(View v) {
				if(mCamera.getMode() == 0){ //use android's take picture method
					if(mCamera.isPriv()){

						path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/test"+mCamera.getNum()+".jpg";
						mCamera.shootAt(path);
					}else{
						Toast.makeText(MainActivity.this, "start prewview before take a picture", Toast.LENGTH_SHORT).show();
					}
				}else{          //use jni take picture method
					mCamera.stopPreview();
					mCamera.release();
					if(mCamera.takePicture() <0){
						Toast.makeText(MainActivity.this, "take picture error", Toast.LENGTH_SHORT).show();
					}else{
						Toast.makeText(MainActivity.this, "take picture success", Toast.LENGTH_SHORT).show();
					}
					mCamera.setMode(0);
					mCamera.prepareAndroidCamera(mCamera.getHolder());
					mCamera.startPreview();
				}
				
				
			}
		});
		
		mChangeBut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mCamera.changeCam() < 0){
					Log.e(TAG,"write gpio failed");
				}else{
					MainActivity.this.str.replace(str.length()-1, str.length(), 
							String.valueOf(MainActivity.this.mCamera.getNum()));
					MainActivity.this.camInfo.setText(MainActivity.this.str.toString());
				}		
			}
		});
		
		mModeBut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
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
		});
		
		this.server = new Thread(new ServerThread());
		this.server.start();
		
	}


	@Override
	protected void onDestroy() {
		mCamera.setMode(0);
		mCamera.clearNum();
		super.onDestroy();
	}
	
	
	
	


}
