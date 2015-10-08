package com.wx.imx53server;

import android.util.Log;

public class JniCamera {
	
	private static final String TAG = "JniCamera";
	
//	public JniCamera(){
////		if(this.initGpio() < 0 ){
////			Log.d(TAG, "gpio init failed");
////		}else{
////			Log.d(TAG,"gpio init succeed");
////		}
//	}
	public static native int takePicture();
	public static native int prepareBuffer();
	public static native int camSel();
	public static native int setMode(int mode);
	public static native int setFlip();

	static {
		System.loadLibrary("picture");
	}


}