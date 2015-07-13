package com.wx.imx53server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class MyCamera implements SurfaceHolder.Callback {
	
	private static MyCamera singleton = null;
	private boolean ispriv = false;
	private static Camera mCamera = null;
	private SurfaceView mSurfaceView = null;
	private static final String TAG = "MyCamera";
	//private JniCamera mJniCameara = null;
	private static SurfaceHolder mholder = null;
	private int camNum = 0;
	private int mode = 0;  //0 for preview,1 for picture
	
	private MyCamera(){
		if(mCamera == null){
			mCamera = Camera.open();
			Log.d(TAG,"open!!!!!!!!!!!!");
		}
	}
	
	public static MyCamera getInstance(){
		if(singleton == null){
			singleton = new MyCamera();
		}
		return singleton;
	}
	
	public void prepareAndroidCamera(){
			if(mCamera == null)
				Log.d(TAG,"test!!!!!!");
			Parameters parameter = mCamera.getParameters();
			parameter.setPreviewFrameRate(15);
			//parameter.setPreviewFormat(ImageFormat.YV12);
			parameter.setPreviewSize(640, 480);
			parameter.setPictureSize(640, 480);
			parameter.setPictureFormat(ImageFormat.JPEG);
			mCamera.setParameters(parameter);
		
	}
	
	public void startPreview(){
		ispriv = true;
		mCamera.startPreview();
	}
	
	public void stopPreview(){
		ispriv = false;
		mholder.removeCallback(this);
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
	}
	
	public void release(){
		mCamera.release();
		mCamera = null;
		singleton = null;
	}
	
	public int takePicture(){
		int ret = 0;
		if((ret = JniCamera.prepareBuffer())<0){
			return ret;
		}
		ret = JniCamera.takePicture();
		return ret;
	}
	
	public void shootAt(final String path){
		Log.d(TAG,"begin"+System.currentTimeMillis());
		mCamera.takePicture(null,null, new PictureCallback() {
			
			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				Log.d(TAG,Integer.toString(data.length));
				Log.d(TAG,"decode begin"+System.currentTimeMillis());
				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				Log.d(TAG,"decode end"+System.currentTimeMillis());
				savePicture(bitmap, path);
				//mCamera.stopPreview();
				mCamera.startPreview();
			}
		});	
		Log.d(TAG,"end"+System.currentTimeMillis());
		
	}
	
	private void savePicture(Bitmap bitmap ,String path){
		File file = new File(path);
		boolean ret = false;
		Log.d(TAG,"PIC SAVE BEGIN:"+System.currentTimeMillis());
		try {
			ret = bitmap.compress(CompressFormat.JPEG, 100, new FileOutputStream(file));
			Log.d(TAG,Boolean.toString(ret));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d(TAG,"PIC SAVE END:"+System.currentTimeMillis());
	}
	
	public int changeCam(){
		if(this.camNum < 7){
			this.camNum ++;
		}else{
			this.camNum = 0;
		}
		return JniCamera.camSel();
	}
	
	public void clearNum(){
		int i;
		Log.i(TAG,"CLEAR NUM");
		for(i=0;i<8-camNum;i++){
			JniCamera.camSel();
		}
	}
	
	public void setMode(int val){
		this.mode = val;
		JniCamera.setMode(mode);
	}
	
	public void setSurface(SurfaceView surfaceView){
		this.mSurfaceView = surfaceView;
		this.mholder = mSurfaceView.getHolder();
		mholder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mholder.addCallback(this);
		try {
			mCamera.setPreviewDisplay(mholder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		prepareAndroidCamera();
		Log.d(TAG,"surface created,prepare for android camera!");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		stopPreview();
		this.ispriv = false;
		mCamera.release();
		Log.d(TAG,"surface destoryed,release resource");
	}
	
	public SurfaceHolder getHolder(){
		return mholder;
	}
	
	public boolean isPriv(){
		return ispriv;
	}

	public void setNum(int num){
		this.camNum = num;
	}
	
	public int getNum(){
		return this.camNum;
	}
	
	public int getMode(){
		return this.mode;
	}
	
	public Camera getCamera(){
		return mCamera;
		
	}
}
