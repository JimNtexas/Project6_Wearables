package com.example.android.sunshine.app;
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class WatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final String TAG = "CanvasWatchFaceService";
    public final String SYNCH_REQUEST = "/synch_request";

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private Bitmap mWxIconBm;
    private String mHighTemp = "";
    private String mLowTemp = "";
    private boolean isRound = false;

    public static final String WX_CLEAR = "Clear";
    public static final String WX_CLOUDY = "Clouds";
    public static final String WX_FOG = "Fog";
    public static final String WX_SCATTERED = "Light_clouds";
    public static final String WX_LIGHT_RAIN = "Light_rain";
    public static final String WX_RAIN = "Rain";
    public static final String WX_SNOW = "Snow";
    public static final String WX_STORM = "Storm";

    private GoogleApiClient apiClient;
    private String remoteNodeId;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<WatchFace.Engine> mWeakReference;

        public EngineHandler(WatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mRegisterdMsgReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };


        final BroadcastReceiver mMsgReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String desc = intent.getStringExtra("wx_desc");
                setWxIconBm(desc);
                mHighTemp = intent.getStringExtra("wx_high");
                mLowTemp = intent.getStringExtra("wx_low");
                Log.d(TAG, "Watch wx update: " + desc + " " + mHighTemp + " / " + mLowTemp);
                invalidate();
            }
        };


        int mTapCount;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Log.d(TAG, "WatchFace onCreate");

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .setHotwordIndicatorGravity(Gravity.BOTTOM)
                    .build());
            Resources resources = WatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background_turquoise1));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));
            // Create the Paint for later use
            mTextPaint = new Paint();
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setAntiAlias(true);

            mTime = new Time();
            initGoogleApiClient();
            registerMsgReceiver();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if(apiClient!= null && apiClient.isConnected()) {
                apiClient.disconnect();
            }
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                if(apiClient == null) {
                    Log.d(TAG, "api client is null");
                }else {
                    Log.d(TAG, "api connected: " + (apiClient.isConnected() ? "CONNECTED" : "DISCONNECTED"));
                    Log.d(TAG, "remote node id: " + remoteNodeId);
                    if(mHighTemp.isEmpty() || mLowTemp.isEmpty()) {
                        RequestSynchFromDevice();
                    }
                }

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void registerMsgReceiver() {
            if(mRegisterdMsgReceiver) {
                return;
            }
            mRegisterdMsgReceiver = true;
            IntentFilter filter = new IntentFilter(WearDataService.MSG_WX_DATA);
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMsgReceiver, filter);

        }

        private void unregisterMsgReceiver() {
            if(mRegisterdMsgReceiver) {
                mRegisterdMsgReceiver = false;
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMsgReceiver);
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = WatchFace.this.getResources();
            isRound = insets.isRound();
            Log.d(TAG, "Watch is round? : " + isRound);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);

            Log.i(TAG, "text size: " + textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Resources resources = WatchFace.this.getResources();
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }

                if (inAmbientMode){
                    mBackgroundPaint.setColor(Color.BLACK);
                }
                else {
                    mBackgroundPaint.setColor(resources.getColor(R.color.background_turquoise1));
                }
                invalidate();
            }
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) { //request wx update?
            Resources resources = WatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            float timeSize = mAmbient ? mTextPaint.measureText("12:00")  :  mTextPaint.measureText("12:00:00");
            float xOffset = (bounds.width() - timeSize)/2;
            float yOffset =  bounds.centerY() - (mTextPaint.getTextSize() / 2.0f) +8f; //8f is to clear the google settings icon on the watch face
            if(isRound) {
                yOffset -= 15f;
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();

            String AmbFormat = "%d:%02d";
            String VisFormat = "%d:%02d:%02d";
            //add leading zero between midnight and 9:59:59
            if(mTime.hour >= 0 && mTime.hour < 10) {
                AmbFormat = "0%d:%02d";
                VisFormat =  "0%d:%02d:%02d";
            }

            String text = mAmbient
                    ? String.format(AmbFormat, mTime.hour, mTime.minute)
                    : String.format(VisFormat, mTime.hour, mTime.minute, mTime.second);
            canvas.drawText(text, xOffset, yOffset, mTextPaint);

         //   canvas.drawText("|", bounds.centerX(), 10f, mTextPaint);  //todo: debug only, remove before release

            if(!mAmbient && mWxIconBm != null) {

                float iconX = bounds.centerX() - (mWxIconBm.getScaledWidth(canvas) / 2f) + 7f;
                canvas.drawBitmap(mWxIconBm, iconX, yOffset + 10, null);

                float currentTextSize = mTextPaint.getTextSize();
                mTextPaint.setTextSize(currentTextSize * 0.3f);

                float highXoffset = iconX - 40f;
                canvas.drawText(mHighTemp, highXoffset, yOffset + 35f, mTextPaint);

                float lowXoffset = iconX + 70f;
                mTextPaint.setColor(getResources().getColor(R.color.lowtemp_color));
                canvas.drawText(mLowTemp, lowXoffset, yOffset + 35f, mTextPaint);
                mTextPaint.setColor(Color.WHITE);
                mTextPaint.setTextSize(currentTextSize);
            }
        }


        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }



    private Bitmap setWxIconBm(String desc) {
        Log.d(TAG, "Wear wx icon: " + desc);
        switch (desc) {
            case WX_CLEAR:
                mWxIconBm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_clear);
                break;
            case WX_CLOUDY:
                mWxIconBm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_cloudy);
                break;
            case WX_FOG:
                mWxIconBm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_fog);
                break;
            case WX_SCATTERED :
                mWxIconBm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_light_clouds);
                break;
            case WX_RAIN :
                mWxIconBm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_rain);
                break;
            case WX_SNOW :
                mWxIconBm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_snow);
                break;
            case WX_STORM :
                mWxIconBm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_storm);
                break;
            default:
                if(desc.isEmpty()) {
                    mWxIconBm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_blank);
                } else {
                    Log.e(TAG, "Unknown weather icon!!!!");
                    mWxIconBm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_unknown);
                }
                break;
        }

        return mWxIconBm;
    }

    private void initGoogleApiClient() {

        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "ConnectionCallback onConnected");
                        Wearable.NodeApi.getConnectedNodes(apiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                            @Override
                            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                                if (getConnectedNodesResult.getStatus().isSuccess() && getConnectedNodesResult.getNodes().size() > 0) {
                                    remoteNodeId = getConnectedNodesResult.getNodes().get(0).getId();
                                    Log.d(TAG, "Remote node id: " + remoteNodeId);
                                } else {
                                    Log.d(TAG, "Get node discovery status: " + getConnectedNodesResult.getStatus().toString());
                                }
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "ConnectionCallback onConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "ConnectionCallback onConnectionFailed");
                        //TODO do something on connection failed
                    }
                })
                .build();
        apiClient.connect();

    }

    private void RequestSynchFromDevice() {
        Wearable.MessageApi.sendMessage(apiClient, remoteNodeId, SYNCH_REQUEST, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                Log.e(TAG, "Wearable.MessageApi.sendMessage result: " + sendMessageResult.getStatus().toString());
                if (sendMessageResult.getStatus().isSuccess()) {
                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "WATCH_REQUESTS_SYNCH");
                }
            }
        });
    }

}
