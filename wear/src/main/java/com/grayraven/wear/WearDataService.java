package com.grayraven.wear;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class WearDataService extends WearableListenerService {

    private static final String TAG = "WearableListenerService";
    public static final String MSG_WX_DATA = "com.grayraven.swarmwatchface.MSG_WX_DATA";

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
                    int wxIcon = dataMap.getInt("wx_icon_number");
                    Log.d(TAG, "new icon number: " + wxIcon);
                    int temp = dataMap.getInt("wx_temp");
                    Log.d(TAG, "watch temp: " + temp);
                    PostDataToWatch(wxIcon, temp);
                }
            }
        }
    }

    private void PostDataToWatch(int wxIcon, int wxTemp) {
        Intent intent = new Intent(MSG_WX_DATA);
        Log.d(TAG, "Posting data to watch");
        intent.putExtra("wxIcon", wxIcon);
        intent.putExtra("wxTemp", wxTemp);
        final boolean b = LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        Log.d(TAG, "broadcast result: " + b);
    }

}
