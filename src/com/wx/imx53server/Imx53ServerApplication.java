package com.wx.imx53server;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

public class Imx53ServerApplication extends Application {
	public static final String TAG = "RtspServerApplication";
	
	/** Default quality of video streams. */
	public VideoQuality videoQuality = new VideoQuality(640,480,15,5000000);
          
	/** By default AMR is the audio encoder. */
	public int audioEncoder = SessionBuilder.AUDIO_NONE;

	/** By default H.263 is the video encoder. */
	public int videoEncoder = SessionBuilder.VIDEO_H264;
	
	private static Imx53ServerApplication sRtspServerApplication;

	@Override
	public void onCreate() {
		sRtspServerApplication = this;
		super.onCreate();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		//��SharedPreferences�л�ȡ����Ƶ�ı����ʽ 
		this.audioEncoder = Integer.parseInt(settings.getString("audio_encoder", String.valueOf(audioEncoder)));
		this.videoEncoder = Integer.parseInt(settings.getString("video_encoder", String.valueOf(videoEncoder)));
		//��ȡvideoQuality
		videoQuality = VideoQuality.merge(
				new VideoQuality(
						settings.getInt("video_resX", 0),
						settings.getInt("video_resY", 0), 
						Integer.parseInt(settings.getString("video_framerate", "0")), 
						Integer.parseInt(settings.getString("video_bitrate", "0"))*1000),
						this.videoQuality);
		SessionBuilder.getInstance().setContext(getApplicationContext())
									.setAudioEncoder(!settings.getBoolean("stream_audio", true)?0:audioEncoder)
									.setVideoEncoder(!settings.getBoolean("stream_video", true)?0:videoEncoder)
									.setVideoQuality(videoQuality);//????no audio quality??
		
		settings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
		
	}
	
	public static Imx53ServerApplication getInstance(){
		return sRtspServerApplication;
	}
	
	private OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			if (key.equals("video_resX") || key.equals("video_resY")) {
				videoQuality.resX = sharedPreferences.getInt("video_resX", 0);
				videoQuality.resY = sharedPreferences.getInt("video_resY", 0);
			}

			else if (key.equals("video_framerate")) {
				videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
			}

			else if (key.equals("video_bitrate")) {
				videoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
			}

			else if (key.equals("audio_encoder") || key.equals("stream_audio")) { 
				audioEncoder = Integer.parseInt(sharedPreferences.getString("audio_encoder", String.valueOf(audioEncoder)));
				SessionBuilder.getInstance().setAudioEncoder( audioEncoder );
				if (!sharedPreferences.getBoolean("stream_audio", true)) 
					SessionBuilder.getInstance().setAudioEncoder(0);
			}

			else if (key.equals("stream_video") || key.equals("video_encoder")) {
				videoEncoder = Integer.parseInt(sharedPreferences.getString("video_encoder", String.valueOf(videoEncoder)));
				SessionBuilder.getInstance().setVideoEncoder( videoEncoder );
				if (!sharedPreferences.getBoolean("stream_video", true)) 
					SessionBuilder.getInstance().setVideoEncoder(0);
			}

		}  
			
	};
	
}
