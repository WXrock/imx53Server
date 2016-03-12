package com.wx.imx53server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.EthernetManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {

	private SharedPreferences pref = null;
	EthernetManager mEthManager =null;
	private boolean isWifi = false;
	private boolean isEthernet = true;
	
	private static final String TAG = "startupReceiver";
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		
		this.pref = PreferenceManager.getDefaultSharedPreferences(arg0);
		this.isWifi = pref.getBoolean(Imx53ServerApplication.WIFI, false);
		this.isEthernet = pref.getBoolean(Imx53ServerApplication.Ethernet, true);
		this.mEthManager =(EthernetManager) arg0.getSystemService(Context.ETHERNET_SERVICE);
		
		if(isWifi && !isEthernet){
			configWifi(arg0);
			start(arg0);
		}else if(isEthernet && !isWifi){
			configEthernet();
			start(arg0);
		}else{
			Log.e(TAG,"NETWORK SETTING WRONG!!!");
		}		

	}
	
	private void start(Context context) {
		Intent i = new Intent(context,MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
	
	
	private void configEthernet(){
		if(mEthManager.getEthernetState()==EthernetManager.ETHERNET_STATE_DISABLED){
			mEthManager.setEthernetEnabled(true);
			Log.d(TAG,"set Ethernet enable!");
		}else{
			Log.d(TAG,"Ethernet already enabled!");
		}
		
	}
	
	private void configWifi(Context context){
		if(mEthManager.getEthernetState()==EthernetManager.ETHERNET_STATE_ENABLED){
			mEthManager.disconnect();
			mEthManager.setEthernetEnabled(false);
			Log.d(TAG,"KILL ETHERNET");
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WifiAdmin wifiAdmin = new WifiAdmin(context);  
        wifiAdmin.openWifi();  
        wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo("Test256", "256256256", 3));
        Log.d(TAG,"WIFI enabled!");
	}

}
