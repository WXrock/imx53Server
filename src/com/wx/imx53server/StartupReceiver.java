package com.wx.imx53server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Intent i = new Intent(arg0,MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		arg0.startActivity(i);

	}

}
