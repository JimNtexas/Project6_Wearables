package com.example.android.sunshine.app.wearsupport;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * Created by Jim on 5/9/2016.
 */
public class WxProvider extends BroadcastReceiver {
    private static final String TAG = "WxProvider";
    @Override
    public void onReceive(Context context, Intent intent) {

        if(SunshineSyncAdapter.ACTION_DATA_UPDATED.equals(SunshineSyncAdapter.ACTION_DATA_UPDATED)) {
            Log.d(TAG, "received SunshineSyncAdapter update ");

            //SunshineSyncAdapter.syncImmediately(context); for msg receive

            SharedPreferences prefs = context.getSharedPreferences("WX_DATA", Context.MODE_PRIVATE);
            String high = prefs.getString("high_temp", "-99");
            String low = prefs.getString("low_temp","-99");
            String desc = prefs.getString("desc","-99");
            Log.d(TAG, "Send to watch:");
            Log.d(TAG, "high: " + high + " low: " + low + " desc: " + desc);
            Intent sendWxData = new Intent(context, SendWearDataService.class);
  //          intent.setAction(Intent.ACTION_SEND);
            sendWxData.putExtra(SendWearDataService.HIGH_TEMP, high );
            sendWxData.putExtra(SendWearDataService.LOW_TEMP, low );
            sendWxData.putExtra(SendWearDataService.WX_DESC, desc);
            ComponentName serviceName = context.startService(sendWxData);
            if(serviceName == null){
                Log.d(TAG, "SendWearDataService failed to start!" );
            }

        }
    }
}
