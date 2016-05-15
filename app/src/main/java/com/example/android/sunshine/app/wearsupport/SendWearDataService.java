package com.example.android.sunshine.app.wearsupport;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */

public class SendWearDataService extends IntentService {
    public static final String HIGH_TEMP = "com.example.android.sunshine.app.wearsupport.extra.HIGH_TEMP";
    public static final String LOW_TEMP = "com.example.android.sunshine.app.wearsupport.extra.LOW_TEMP";
    public static final String WX_DESC = "com.example.android.sunshine.app.wearsupport.extra.WX_DESC";
    public static final String WX_TIME = "com.example.android.sunshine.app.wearsupport.extra.TIME";
    private static final String TAG = "SendWearDataService";

    private GoogleApiClient client = null;
    public SendWearDataService() {
        super("SendWearDataService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        client.disconnect();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Starts this service
     */

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

                final String highTemp = intent.getStringExtra(HIGH_TEMP);
                final String lowTemp = intent.getStringExtra(LOW_TEMP);
                final String wxDesc = intent.getStringExtra(WX_DESC);
                final long wxTime = intent.getLongExtra(WX_TIME, 0);
                client = SetupApiClient();

                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/wx_icon");
                putDataMapRequest.getDataMap().putString("wx_desc", wxDesc);
                putDataMapRequest.getDataMap().putString("low_temp", lowTemp);
                putDataMapRequest.getDataMap().putString("high_temp", highTemp);
                putDataMapRequest.getDataMap().putLong("time", wxTime);
                putDataMapRequest.setUrgent();
                Log.d(TAG, "mobile sending icon/temp: " + lowTemp + " - " + highTemp);

                PutDataRequest request = putDataMapRequest.asPutDataRequest();

                if(!client.isConnected()) {
                    Log.e(TAG, "GoogleApiClient NOT CONNECTED!");
                    return;
                }
                Wearable.DataApi.putDataItem(client, request)
                        .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                Log.d(TAG, dataItemResult.getStatus().isSuccess() ? "Mobile putdata success" : "Mobile putdata failed");
                            }
                        });

                client.disconnect();
            }

        }

    private GoogleApiClient SetupApiClient() {
        GoogleApiClient client = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        client.blockingConnect(2, TimeUnit.SECONDS);
        return client;

    }
}
