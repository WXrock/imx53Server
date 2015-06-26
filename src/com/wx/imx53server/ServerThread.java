package com.wx.imx53server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
    private PrintStream out = null;
    public boolean bye = false;
    private boolean isConnected = false;
    private boolean isStoped = false;
    
    private Handler mHandler = null; 
    private Message mMessage = null;
    
    
    public ServerThread(Handler handler) {
    	this.mHandler = handler;
    	this.mMessage = new Message();
    	try {
			this.server = new ServerSocket(8888);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    @Override
    public void run(){
		if(this.server == null){
			Log.e(TAG,"server is null");
			this.isStoped = true;
		}
    	while(!isStoped){
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
    			out = new PrintStream(client.getOutputStream());
    			buf = new BufferedReader(new InputStreamReader(client.getInputStream()));
    			while(!bye){
    				String str = buf.readLine();
    				if(str == null || str.equals("") || str.equals("bye")){
    					bye = true;
    					Log.d(TAG,"client disconnect!!");
    				}else{
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
    	this.isStoped = true;
    }

}
