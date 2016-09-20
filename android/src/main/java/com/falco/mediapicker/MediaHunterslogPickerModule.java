package com.falco.mediapicker;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.gson.Gson;

/**
 * Created by Bond Nguyen on 9/18/16.
 */
public class MediaHunterslogPickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    Callback mCallback;

    // Config value
    public final String MAX_UPLOADABLE_PHOTO = "MAX_UPLOADABLE_PHOTO";
    public final String MAX_UPLOADABLE_VIDEO = "MAX_UPLOADABLE_VIDEO";
    public final String MAX_UPLOADABLE_VIDEO_DURATION = "MAX_UPLOADABLE_VIDEO_DURATION";
    public final static int MEDIA_RESULT_CODE = 0;
    public final static String MEDIA_RESULT = "MEDIA_RESULT";
    // --------------------------

    public MediaHunterslogPickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MEDIA_RESULT_CODE:
                if (data != null && data.hasExtra(MEDIA_RESULT)) {

                    String jsonArr = data.getStringExtra(MEDIA_RESULT);
                    Log.e("MEDIA", jsonArr);

                    mCallback.invoke(jsonArr);
                }

                break;
        }
    }

    @Override
    public String getName() {
        return "MediaHunterslogPicker";
    }

    @ReactMethod
    public void showMediaPicker(int max_photo, int max_video, int max_video_duration, Object selectedList, Callback callback) {
        mCallback = callback;
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {

            Intent intent = new Intent(getReactApplicationContext(), MediaPickerActivity.class);
            intent.putExtra(MAX_UPLOADABLE_PHOTO, max_photo);
            intent.putExtra(MAX_UPLOADABLE_VIDEO, max_video);
            intent.putExtra(MAX_UPLOADABLE_VIDEO_DURATION, max_video_duration);

            Gson gson = new Gson();
            MediaItem[] mediaList = gson.fromJson(selectedList.toString(), MediaItem[].class);

            intent.putExtra(MEDIA_RESULT, mediaList);

            currentActivity.startActivityForResult(intent, MEDIA_RESULT_CODE);
        }
    }

   @Override
   public void onNewIntent(Intent intent) {

   }
}
