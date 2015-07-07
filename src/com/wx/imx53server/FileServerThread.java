package com.wx.imx53server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

public class FileServerThread extends Thread implements Runnable{
	
	public static final String TAG = "FileServer"; 
	private ServerSocket fileServer = null;
	private boolean isConnected = false;
	public boolean stop = false;
	private int totalLength = 0;
	private int[] fileLength;
	private byte[] buffer = new byte[1024];
	private String[] fileName;
	private boolean setFileDone = false;
	
	
	public FileServerThread(){
		try {
			this.fileServer = new ServerSocket(9999);
			Log.d(TAG,"FileServer init");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	public void setFile(int[] fileLength,String[] fileName){
		this.fileLength = fileLength;
		for(int i=0;i<8;i++){
			this.totalLength += totalLength +fileLength[i];
		}
        System.out.println(totalLength);
		this.fileName = fileName;
		
		this.setFileDone = true;
		
	}
	
	@Override
	public void run() {
		
		int read;
		Log.d(TAG,"test");
		if(this.fileServer == null){
			Log.d(TAG,"FileServer is null");
			this.stop = true;
		}
		while(!stop){
			try {
				Log.d(TAG,"waiting for clinet");
				Socket client = fileServer.accept();
				if(client.isConnected()){
					this.isConnected = true;
					Log.d(TAG,"File cilent conected");
				}
				DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                DataInputStream dis = new DataInputStream(client.getInputStream());
                
                while(!setFileDone);
                
                Log.d(TAG,String.valueOf(totalLength));
				dos.writeInt(totalLength);
				dos.flush();
				
				for(int i=0;i<8;i++){
					dos.writeInt(fileLength[i]);
					dos.flush();
                    Log.d(TAG,i+":"+fileLength[i]);
					BufferedInputStream fis = new BufferedInputStream(
							new FileInputStream(new File(fileName[i])));
					while((read=fis.read(buffer))!=-1){
						dos.write(buffer,0,read);
					}
					dos.flush();
					fis.close();
                    if(dis.readUTF().equals("ok")){
                        Log.d(TAG,"client received file"+i);    
                    }
				}
				//send 8 file finish
				dos.close();
                dis.close();
				client.close();
				Log.d(TAG,"send finish");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}						
		}

	}
	
	public boolean isConnected(){
		return this.isConnected;
	}
	

}