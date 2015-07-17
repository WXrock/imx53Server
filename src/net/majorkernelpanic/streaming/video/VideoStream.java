/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.streaming.video;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import com.wx.imx53server.MainActivity;
import com.wx.imx53server.MyCamera;

import net.majorkernelpanic.streaming.MediaStream;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;

/** 
 * Don't use this class directly.
 */
public abstract class VideoStream extends MediaStream {

	protected final static String TAG = "VideoStream";

	protected VideoQuality mQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
	protected SurfaceHolder.Callback mSurfaceHolderCallback = null;
	protected SurfaceHolder mSurfaceHolder = null;
	protected int mVideoEncoder, mCameraId = 0;
	//protected MyCamera mCamera;
	protected boolean mCameraOpenedManually = true;
	protected boolean mFlashState = false;
	protected boolean mSurfaceReady = true;
	protected boolean mUnlocked = false;
	protected boolean mPreviewStarted = false;

	/** 
	 * Don't use this class directly.
	 * Uses CAMERA_FACING_BACK by default.
	 */
	public VideoStream() {
		this(CameraInfo.CAMERA_FACING_BACK);
	}	

	/** 
	 * Don't use this class directly
	 * @param camera Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
	 */
	public VideoStream(int camera) {
		super();
		// TODO: Remove this when encoding with the MediaCodec API is ready
		setMode(MODE_MEDIARECORDER_API);
	}


	/** 
	 * Modifies the resolution of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * {@link #setVideoQuality(VideoQuality)} may be more convenient.
	 * @param width Width of the stream
	 * @param height height of the stream
	 */
	public void setVideoSize(int width, int height) {
		if (mQuality.resX != width || mQuality.resY != height) {
			mQuality.resX = width;
			mQuality.resY = height;
		}
	}

	/** 
	 * Modifies the framerate of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * {@link #setVideoQuality(VideoQuality)} may be more convenient.
	 * @param rate Framerate of the stream
	 */	
	public void setVideoFramerate(int rate) {
		if (mQuality.framerate != rate) {
			mQuality.framerate = rate;
		}
	}

	/** 
	 * Modifies the bitrate of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * {@link #setVideoQuality(VideoQuality)} may be more convenient.
	 * @param bitrate Bitrate of the stream in bit per second
	 */	
	public void setVideoEncodingBitrate(int bitrate) {
		if (mQuality.bitrate != bitrate) {
			mQuality.bitrate = bitrate;
		}
	}

	/** 
	 * Modifies the quality of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * @param videoQuality Quality of the stream
	 */
	public void setVideoQuality(VideoQuality videoQuality) {
		if (!mQuality.equals(videoQuality)) {
			mQuality = videoQuality;
		}
	}

	/** 
	 * Returns the quality of the stream.  
	 */
	public VideoQuality getVideoQuality() {
		return mQuality;
	}	

	/** 
	 * Modifies the videoEncoder of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #start()}.
	 * @param videoEncoder Encoder of the stream
	 */
	protected void setVideoEncoder(int videoEncoder) {
		this.mVideoEncoder = videoEncoder;
	}

	/**
	 * Starts the stream.
	 * This will also open the camera and dispay the preview 
	 * if {@link #startPreview()} has not aready been called.
	 */
	public synchronized void start() throws IllegalStateException, IOException {
		if (!mPreviewStarted) mCameraOpenedManually = false;
		super.start();
	}	

	/** Stops the stream. */
	public synchronized void stop() {
		if (MyCamera.getInstance() != null) {
			super.stop();
			destroyCamera();

		}
	}

	/**
	 * Encoding of the audio/video is done by a MediaRecorder.
	 */
	protected void encodeWithMediaRecorder() throws IOException {

		// We need a local socket to forward data output by the camera to the packetizer
		createSockets();

		// Opens the camera if needed
		createCamera();
		
		Log.d(TAG,"create MediaRecorder!!!!!!!!!");
		mMediaRecorder = new MediaRecorder();
		//mMediaRecorder.setCamera(MyCamera.getInstance().getCamera());
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setVideoEncoder(mVideoEncoder);
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		mMediaRecorder.setVideoSize(mQuality.resX,mQuality.resY);
		mMediaRecorder.setVideoFrameRate(mQuality.framerate);
		mMediaRecorder.setVideoEncodingBitRate(mQuality.bitrate);

		// We write the ouput of the camera in a local socket instead of a file !			
		// This one little trick makes streaming feasible quiet simply: data from the camera
		// can then be manipulated at the other end of the socket
		mMediaRecorder.setOutputFile(mSender.getFileDescriptor());

		mMediaRecorder.prepare();
		mMediaRecorder.start();

		try {
			// mReceiver.getInputStream contains the data from the camera
			// the mPacketizer encapsulates this stream in an RTP stream and send it over the network
			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.setInputStream(mReceiver.getInputStream());
			mPacketizer.start();
			mStreaming = true;
		} catch (IOException e) {
			stop();
			throw new IOException("Something happened with the local sockets :/ Start failed !");
		}

	}


	public abstract String generateSessionDescription() throws IllegalStateException, IOException;

	protected synchronized void createCamera() throws RuntimeException, IOException {
			if(mSurfaceHolder == null){
				Log.d(TAG,"SURFACE HOLDER");
				mSurfaceHolder = MyCamera.getInstance().getHolder();
			}		
			mUnlocked = false;		
	}

	protected synchronized void destroyCamera() {
		if (MyCamera.getInstance() != null) {
			if (mStreaming) super.stop();
			mUnlocked = false;
			mPreviewStarted = false;
		}	
	}	

	/** 
	 * Checks if the resolution and the framerate selected are supported by the camera.
	 * If not, it modifies it by supported parameters. 
	 * FIXME: Not reliable, more or less useless :(
	 **/
	private void getClosestSupportedQuality(Camera.Parameters parameters) {

		// Resolutions
		String supportedSizesStr = "Supported resolutions: ";
		List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
		for (Iterator<Size> it = supportedSizes.iterator(); it.hasNext();) {
			Size size = it.next();
			supportedSizesStr += size.width+"x"+size.height+(it.hasNext()?", ":"");
		}
		Log.v(TAG,supportedSizesStr);

		// Frame rates
		String supportedFrameRatesStr = "Supported frame rates: ";
		List<Integer> supportedFrameRates = parameters.getSupportedPreviewFrameRates();
		for (Iterator<Integer> it = supportedFrameRates.iterator(); it.hasNext();) {
			supportedFrameRatesStr += it.next()+"fps"+(it.hasNext()?", ":"");
		}
		//Log.v(TAG,supportedFrameRatesStr);

		int minDist = Integer.MAX_VALUE, newFps = mQuality.framerate;
		if (!supportedFrameRates.contains(mQuality.framerate)) {
			for (Iterator<Integer> it = supportedFrameRates.iterator(); it.hasNext();) {
				int fps = it.next();
				int dist = Math.abs(fps - mQuality.framerate);
				if (dist<minDist) {
					minDist = dist;
					newFps = fps;
				}
			}
			Log.v(TAG,"Frame rate modified: "+mQuality.framerate+"->"+newFps);
			//mQuality.framerate = newFps;
		}

	}

}
