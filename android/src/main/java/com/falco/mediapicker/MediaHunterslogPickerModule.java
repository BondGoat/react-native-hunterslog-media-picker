package com.falco.mediapicker;

import android.app.Activity;
import android.content.Intent;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

/**
 * Created by Bond Nguyen on 9/18/16.
 */
public class MediaHunterslogPickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    Callback mCallback;

    // --------------------------

    public MediaHunterslogPickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.MEDIA_RESULT_CODE:
                if (data != null && data.hasExtra(Constants.MEDIA_RESULT)) {

                    String jsonArr = data.getStringExtra(Constants.MEDIA_RESULT);

                    mCallback.invoke(jsonArr);
                } else {
                    mCallback.invoke("");
                }

                break;
        }
    }

    @Override
    public String getName() {
        return "MediaHunterslogPicker";
    }

    @ReactMethod
    public void showMediaPicker(boolean isCaptureVideo, int max_photo, int max_video, int max_video_duration, String selectedList, Callback callback) {
        mCallback = callback;
        Activity currentActivity = getCurrentActivity();
        if (currentActivity != null) {

            Intent intent = new Intent(getReactApplicationContext(), SortByDateMediaPickerActivity.class);
            intent.putExtra(Constants.MAX_UPLOADABLE_PHOTO, max_photo);
            intent.putExtra(Constants.MAX_UPLOADABLE_VIDEO, max_video);
            intent.putExtra(Constants.MAX_UPLOADABLE_VIDEO_DURATION, max_video_duration);
            intent.putExtra(Constants.IS_CAPTURE_VIDEO, isCaptureVideo);


            intent.putExtra(Constants.MEDIA_RESULT, selectedList);

            if (SortByDateMediaPickerActivity.mMediaList != null)
                SortByDateMediaPickerActivity.mMediaList.clear();

            currentActivity.startActivityForResult(intent, Constants.MEDIA_RESULT_CODE);
        }
    }

   @Override
   public void onNewIntent(Intent intent) {

   }
}
