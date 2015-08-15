package com.wx.imx53server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ServerThread extends Thread implements Runnable {
	
	public static final String TAG = "ServerThrread";
    private Socket client = null;
//    private String str ;
    private ServerSocket server = null;
    private BufferedReader buf = null;
    private OutputStream out = null;
    public boolean bye = false;
    private boolean isConnected = false;
    public boolean stop = false;
    
    private Handler mHandler = null; 
    
    
    public ServerThread(Handler handler) {
    	this.mHandler = handler;
    	try {
			this.server = new ServerSocket(8888); 
			Log.d(TAG,"server init");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    @Override
    public void run(){
		if(this.server == null){
			Log.e(TAG,"server is null");
			this.stop = true;
		}
    	while(!stop){
    		try {   				
    			this.client = server.accept();
    			if(client.isConnected()){
    				Log.d(TAG,"client connected!");    	
    				this.isConnected = true;
    				bye = false;
    			}
    		} catch (IOException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}   	
    		try{
    			out = client.getOutputStream();
    			buf = new BufferedReader(new InputStreamReader(client.getInputStream()));
    			while(!bye){
    				String str = buf.readLine();
    				if(str == null || str.equals("") || str.equals("bye")){
    					bye = true;
    					Log.d(TAG,"client disconnect!!");
    				}else{
    					Message mMessage = new Message();
    					mMessage.obj = str;
    					Log.d(TAG,str);
    					mHandler.sendMessage(mMessage);
    				}
    			}
    			client.close();
    			this.isConnected = false;
    		}catch(Exception e){
    			e.printStackTrace();
    		}
        }
    }
    
    public boolean isConnect(){
    	return this.isConnected;
    }
    
    public void release(){
    	try {
			server.close();   //it will cost some time for the system to release port;
			client.close();
			server = null;
			client = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	this.bye = true;
    	this.isConnected = false;
    	this.stop = true;
    }

    public void piture_done(String com){
    	if(client == null){
    		Log.e(TAG,"CLIENT IS NULL");
    		return;
    	}
    	try {
			out.write(com.getBytes("UTF-8"));
			out.write('\n');
			Log.d(TAG,"Send picture done");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
}
