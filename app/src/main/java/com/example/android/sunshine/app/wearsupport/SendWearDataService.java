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

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */

public class SendWearDataService extends IntentService {
    static final String HIGH_TEMP = "com.example.android.sunshine.app.wearsupport.extra.HIGH_TEMP";
    static final String LOW_TEMP = "com.example.android.sunshine.app.wearsupport.extra.LOW_TEMP";
    static final String WX_DESC = "com.example.android.sunshine.app.wearsupport.extra.WX_DESC";
    private static final String TAG = "SendWearDataService";
    public SendWearDataService() {
        super("SendWearDataService");
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

                GoogleApiClient client = SetupApiClient();

                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/wx_icon");
                putDataMapRequest.getDataMap().putString("wx_desc", wxDesc);
                putDataMapRequest.getDataMap().putString("low_temp", lowTemp);
                putDataMapRequest.getDataMap().putString("high_temp", highTemp);
                Log.d(TAG, "mobile sending icon/temp: " + lowTemp + " - " + highTemp);

                PutDataRequest request = putDataMapRequest.asPutDataRequest();
                Wearable.DataApi.putDataItem(client, request)
                        .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                Log.d(TAG, dataItemResult.getStatus().isSuccess() ? "Mobile putdata success" : "Mobile putdata failed");
                            }
                        });
            }

        }

    private GoogleApiClient SetupApiClient() {
        GoogleApiClient client = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        client.connect();
        return client;

    }
}
