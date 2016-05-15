package com.example.android.sunshine.app;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class WearDataService extends WearableListenerService {

    private static final String TAG = "WearDataService";
    public static final String MSG_WX_DATA = "com.example.android.sunshine.app.MSG_WX_DATA";

    public WearDataService() {
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        Log.d(TAG, "onDataChanged");
        for(DataEvent dataEvent : dataEvents) {
            if(dataEvent.getType() == DataEvent.TYPE_CHANGED){
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if(path.equals("/wx_icon")) {
                    String wx_desc = dataMap.getString("wx_desc", "");
                    Log.d(TAG, "wx desc: " + wx_desc);
                    String lowTemp = dataMap.getString("low_temp", "");
                    Log.d(TAG, "low temp: " + lowTemp);
                    String highTemp = dataMap.getString("high_temp", "");
                    PostDataToWatch(wx_desc,lowTemp,highTemp);
                }
            }
        }
    }

    private void PostDataToWatch(String desc, String low, String high) {
        Intent intent = new Intent(MSG_WX_DATA);
        Log.d(TAG, "Posting data to watch");
        intent.putExtra("wx_desc", desc);
        intent.putExtra("wx_high", high);
        intent.putExtra("wx_low", low);
        final boolean b = LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        Log.d(TAG, "broadcast result: " + b);
    }

}
