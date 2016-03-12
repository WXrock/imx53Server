package com.wx.imx53server;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;

public class WifiAdmin { 
    // ¶šÒåWifiManager¶ÔÏó  
    private WifiManager mWifiManager; 
    // ¶šÒåWifiInfo¶ÔÏó  
    private WifiInfo mWifiInfo; 
    // ÉšÃè³öµÄÍøÂçÁ¬œÓÁÐ±í  
    private List<ScanResult> mWifiList; 
    // ÍøÂçÁ¬œÓÁÐ±í  
    private List<WifiConfiguration> mWifiConfiguration; 
    // ¶šÒåÒ»žöWifiLock  
    WifiLock mWifiLock; 

 
    // ¹¹ÔìÆ÷  
    public WifiAdmin(Context context) { 
        // È¡µÃWifiManager¶ÔÏó  
        mWifiManager = (WifiManager) context 
                .getSystemService(Context.WIFI_SERVICE); 
        // È¡µÃWifiInfo¶ÔÏó  
        mWifiInfo = mWifiManager.getConnectionInfo(); 
    } 
 
    // Žò¿ªWIFI  
    public void openWifi() { 
        if (!mWifiManager.isWifiEnabled()) { 
            mWifiManager.setWifiEnabled(true); 
        } 
        while(mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED){
        	try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    } 
 
    // ¹Ø±ÕWIFI  
    public void closeWifi() { 
        if (mWifiManager.isWifiEnabled()) { 
            mWifiManager.setWifiEnabled(false); 
        } 
    } 
 
    // Œì²éµ±Ç°WIFI×ŽÌ¬  
    public int checkState() { 
        return mWifiManager.getWifiState(); 
    } 
 
    // Ëø¶šWifiLock  
    public void acquireWifiLock() { 
        mWifiLock.acquire(); 
    } 
 
    // œâËøWifiLock  
    public void releaseWifiLock() { 
        // ÅÐ¶ÏÊ±ºòËø¶š  
        if (mWifiLock.isHeld()) { 
            mWifiLock.acquire(); 
        } 
    } 
 
    // ŽŽœšÒ»žöWifiLock  
    public void creatWifiLock() { 
        mWifiLock = mWifiManager.createWifiLock("Test"); 
    } 
 
    // µÃµœÅäÖÃºÃµÄÍøÂç  
    public List<WifiConfiguration> getConfiguration() { 
        return mWifiConfiguration; 
    } 
 
    // Öž¶šÅäÖÃºÃµÄÍøÂçœøÐÐÁ¬œÓ  
    public void connectConfiguration(int index) { 
        // Ë÷ÒýŽóÓÚÅäÖÃºÃµÄÍøÂçË÷Òý·µ»Ø  
        if (index > mWifiConfiguration.size()) { 
            return; 
        } 
        // Á¬œÓÅäÖÃºÃµÄÖž¶šIDµÄÍøÂç  
        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId, 
                true); 
    } 
 
    public void startScan() { 
        mWifiManager.startScan(); 
        // µÃµœÉšÃèœá¹û  
        mWifiList = mWifiManager.getScanResults(); 
        // µÃµœÅäÖÃºÃµÄÍøÂçÁ¬œÓ  
        mWifiConfiguration = mWifiManager.getConfiguredNetworks(); 
    } 
 
    // µÃµœÍøÂçÁÐ±í  
    public List<ScanResult> getWifiList() { 
        return mWifiList; 
    } 
 
    // ²é¿ŽÉšÃèœá¹û  
    public StringBuilder lookUpScan() { 
        StringBuilder stringBuilder = new StringBuilder(); 
        for (int i = 0; i < mWifiList.size(); i++) { 
            stringBuilder 
                    .append("Index_" + new Integer(i + 1).toString() + ":"); 
            // œ«ScanResultÐÅÏ¢×ª»»³ÉÒ»žö×Ö·ûŽ®°ü  
            // ÆäÖÐ°Ñ°üÀš£ºBSSID¡¢SSID¡¢capabilities¡¢frequency¡¢level  
            stringBuilder.append((mWifiList.get(i)).toString()); 
            stringBuilder.append("/n"); 
        } 
        return stringBuilder; 
    }
 
    // µÃµœMACµØÖ·  
    public String getMacAddress() { 
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress(); 
    } 
 
    // µÃµœœÓÈëµãµÄBSSID  
    public String getBSSID() { 
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID(); 
    } 
 
    // µÃµœIPµØÖ·  
    public int getIPAddress() { 
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress(); 
    } 
 
    // µÃµœÁ¬œÓµÄID  
    public int getNetworkId() { 
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId(); 
    } 
 
    // µÃµœWifiInfoµÄËùÓÐÐÅÏ¢°ü  
    public String getWifiInfo() { 
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString(); 
    } 
 
    // ÌíŒÓÒ»žöÍøÂç²¢Á¬œÓ  
    public void addNetwork(WifiConfiguration wcg) { 
	 int wcgID = mWifiManager.addNetwork(wcg); 
     boolean b =  mWifiManager.enableNetwork(wcgID, true); 
     System.out.println("a--" + wcgID);
     System.out.println("b--" + b);
    } 
 
    // ¶Ï¿ªÖž¶šIDµÄÍøÂç  
    public void disconnectWifi(int netId) { 
        mWifiManager.disableNetwork(netId); 
        mWifiManager.disconnect(); 
    } 
 
//È»ºóÊÇÒ»žöÊµŒÊÓŠÓÃ·œ·š£¬Ö»ÑéÖ€¹ýÃ»ÓÐÃÜÂëµÄÇé¿ö£º
 
    public WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type) 
    { 
          WifiConfiguration config = new WifiConfiguration();   
           config.allowedAuthAlgorithms.clear(); 
           config.allowedGroupCiphers.clear(); 
           config.allowedKeyManagement.clear(); 
           config.allowedPairwiseCiphers.clear(); 
           config.allowedProtocols.clear(); 
          config.SSID = "\"" + SSID + "\"";   
          
          WifiConfiguration tempConfig = this.IsExsits(SSID);           
          if(tempConfig != null) {  
        	  mWifiManager.removeNetwork(tempConfig.networkId);  
          }
          
          if(Type == 1) //WIFICIPHER_NOPASS
          { 
               config.wepKeys[0] = ""; 
               config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); 
               config.wepTxKeyIndex = 0; 
          } 
          if(Type == 2) //WIFICIPHER_WEP
          { 
              config.hiddenSSID = true;
              config.wepKeys[0]= "\""+Password+"\""; 
              config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED); 
              config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP); 
              config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP); 
              config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40); 
              config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104); 
              config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); 
              config.wepTxKeyIndex = 0; 
          } 
          if(Type == 3) //WIFICIPHER_WPA
          { 
          config.preSharedKey = "\""+Password+"\""; 
          config.hiddenSSID = true;   
          config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);   
          config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);                         
          config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);                         
          config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);                    
          //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);  
          config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
          config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
          config.status = WifiConfiguration.Status.ENABLED;   
          }
           return config; 
    } 
    
    private WifiConfiguration IsExsits(String SSID)  
    {  
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();  
        if(existingConfigs == null)
        	Log.d("test","existongConfig is null");
           for (WifiConfiguration existingConfig : existingConfigs)   
           {  
        	 Log.d("test",existingConfig.SSID);
             if (existingConfig.SSID.equals("\""+SSID+"\""))  
             {  
                 return existingConfig;  
             }  
           }  
        return null;   
    }
  
}
//·ÖÎªÈýÖÖÇé¿ö£º1Ã»ÓÐÃÜÂë2ÓÃwepŒÓÃÜ3ÓÃwpaŒÓÃÜ
