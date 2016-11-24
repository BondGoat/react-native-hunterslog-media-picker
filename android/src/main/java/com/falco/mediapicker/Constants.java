package com.falco.mediapicker;

/**
 * Created by nhbao on 10/14/2016.
 */

public class Constants {
    // Permission list request code
    public final static int READ_EXTERNAL_STORAGE = 0;
    public final static int WRITE_EXTERNAL_STORAGE = 1;
    // --------------------------

    // Config value
    public final static String MAX_UPLOADABLE_PHOTO = "MAX_UPLOADABLE_PHOTO";
    public final static String MAX_UPLOADABLE_VIDEO = "MAX_UPLOADABLE_VIDEO";
    public final static String MAX_UPLOADABLE_VIDEO_DURATION = "MAX_UPLOADABLE_VIDEO_DURATION";
    public final static int MEDIA_RESULT_CODE = 0;
    public final static String MEDIA_RESULT = "MEDIA_RESULT";
    public final static int MAX_SCALED_SIZE = 500;
    public final static String IS_CAPTURE_VIDEO = "IS_CAPTURE_VIDEO";
    // --------------------------

    // Camera intent
    public final static  int REQUEST_IMAGE_CAPTURE = 2;
    public final static int REQUEST_VIDEO_CAPTURE = REQUEST_IMAGE_CAPTURE + 1;
    public final static int REQUEST_IMAGE_PREVIEW = REQUEST_VIDEO_CAPTURE + 1;
    public final static int REQUEST_VIDEO_PREVIEW = REQUEST_IMAGE_PREVIEW + 1;
    public final static int REQUEST_TAKE_PHOTO = REQUEST_VIDEO_PREVIEW + 1;

    // --------------------------
}
