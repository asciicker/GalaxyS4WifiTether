package com.asciickers.wifitether;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.R.id;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;


/*	Required Permissions:
 *		android.permission.ACCESS_SUPERUSER
 *		android.permission.ACCESS_WIFI_STATE
 *		android.permission.CHANGE_WIFI_STATE
 */

public class MainActivity extends Activity {
	/*****COPIED FROM android.net.wifi.WifiManager.class*****/
	
	/**
     * Wi-Fi AP is currently being disabled. The state will change to
     * {@link #WIFI_AP_STATE_DISABLED} if it finishes successfully.
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiApState()
     *
     * @hide
     */
	public static final int WIFI_AP_STATE_DISABLING = 10;
    /**
     * Wi-Fi AP is disabled.
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiState()
     *
     * @hide
     */
    public static final int WIFI_AP_STATE_DISABLED = 11;
    /**
     * Wi-Fi AP is currently being enabled. The state will change to
     * {@link #WIFI_AP_STATE_ENABLED} if it finishes successfully.
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiApState()
     *
     * @hide
     */
    public static final int WIFI_AP_STATE_ENABLING = 12;
    /**
     * Wi-Fi AP is enabled.
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiApState()
     *
     * @hide
     */
    public static final int WIFI_AP_STATE_ENABLED = 13;
    /**
     * Wi-Fi AP is in a failed state. This state will occur when an error occurs during
     * enabling or disabling
     *
     * @see #WIFI_AP_STATE_CHANGED_ACTION
     * @see #getWifiApState()
     *
     * @hide
     */
    public static final int WIFI_AP_STATE_FAILED = 14;
    
	/************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    	updateButtonState();	
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
    
    public void updateButtonState()
    {
    	int state = getWifiTetherState();
    	boolean tetherEnabled = state == WIFI_AP_STATE_ENABLED || state == WIFI_AP_STATE_ENABLING;
    	Log.v(ACTIVITY_SERVICE, "WifiTether state is " + String.valueOf(tetherEnabled));
    	
		View enableButton = this.findViewById(R.id.button1);
		enableButton.setEnabled(!tetherEnabled);
		View disableButton = this.findViewById(R.id.button2);
		disableButton.setEnabled(tetherEnabled);
    }

    public void setWifiState(boolean enabled)
    {
    	WifiManager wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
    	wifiManager.setWifiEnabled(enabled);
    }

    public void setWifiTethering(boolean enabled) 
    {
        WifiManager wifiManager = (WifiManager)this.getSystemService(WIFI_SERVICE);

        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    method.invoke(wifiManager, null, enabled);
                } catch (Exception ex) {
                }
                break;
            }
        }
    }
    
    public int getWifiTetherState()
    {
    	int result = WIFI_AP_STATE_DISABLED;
    	WifiManager wifiManager = (WifiManager)this.getSystemService(WIFI_SERVICE);

        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("getWifiApState")) {
                try {
                    result = (Integer)method.invoke(wifiManager);
                } catch (Exception ex) {
                }
                break;
            }
        }
        
    	return result; 
    }

    public static void doCmds(List<String> cmds) throws Exception 
    {
        Process process = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(process.getOutputStream());

        for (String tmpCmd : cmds) {
                os.writeBytes(tmpCmd+"\n");
        }

        os.writeBytes("exit\n");
        os.flush();
        os.close();

        process.waitFor();
    }
    
    public void tetherOn(View clicked) throws Exception
    {
    	List<String> cmds = new ArrayList<String>();
    	cmds.add("rmmod dhd");
    	cmds.add("insmod /system/lib/modules/dhd.ko \"firmware_path=/system/etc/wifi/bcmdhd_apsta.bin nvram_path=/system/etc/wifi/nvram_net.txt\"");
    	setWifiState(false);
    	doCmds(cmds);
    	setWifiTethering(true);        
        updateButtonState();
    }

    public void tetherOff(View clicked) throws Exception
    {
    	List<String> cmds = new ArrayList<String>();
    	cmds.add("rmmod dhd");
    	cmds.add("insmod /system/lib/modules/dhd.ko \"firmware_path=/system/etc/wifi/bcmdhd_sta.bin nvram_path=/system/etc/wifi/nvram_net.txt\"");
    	setWifiTethering(false);
    	doCmds(cmds);
    	setWifiState(true);
        updateButtonState();
    }
}
