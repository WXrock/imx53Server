package com.wx.imx53server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import android.util.Log;

public class ServerThread implements Runnable{
	
	public static final String TAG = "ServerThrread";
    private Socket client = null;
    private String str ;
    private ServerSocket server = null;
    private BufferedReader buf = null;
    private PrintStream out = null;
    public boolean bye = false;
    public boolean isConnected = false;
    
    public void run(){
    	try {
			this.server = new ServerSocket(8888);
			this.client = server.accept();
			this.isConnected = true;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	

        try{
             out = new PrintStream(client.getOutputStream());
             buf = new BufferedReader(new InputStreamReader(client.getInputStream()));
             while(!bye){
                str = buf.readLine();
                if(str.equals("bye")){
                	bye = true;
                }else if(str.equals("change")){
                	Log.d(TAG,"change");
                }else if(str.equals("pic")){
                	Log.d(TAG,"pic");
                }else if(str.equals("preview")){
                	Log.d(TAG,"preview");
                }else{
                	
                }
             }
             client.close();
             this.isConnected = false;
        }catch(Exception e){
        	e.printStackTrace();
        }
    }
    
    public boolean isConnect(){
    	return this.isConnected;
    }

}
