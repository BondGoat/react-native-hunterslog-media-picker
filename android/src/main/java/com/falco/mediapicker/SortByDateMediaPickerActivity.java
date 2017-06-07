package com.falco.mediapicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;

public class SortByDateMediaPickerActivity extends Activity {
    private boolean isScrollEnabled = true;
    ProgressDialog mProgressDialog;
    Picasso picassoInstance;
    Button btnBack, btnAdd, btnCapture;
    int max_photo = 10, max_video = 1, max_video_duration = 10;
    int selected_photo = 0, selected_video = 0;
    Dialog mDialog;
    String mCurrentPhotoPath;
    String imageTaken;// Save image URl after take photo and send it to mSelectedMediaList
    String imageLat;// Save image lat code after take photo and send it to mSelectedMediaList
    String imageLong;// Save image long code after take photo and send it to mSelectedMediaList
    String videoTaken;// Save video URl after take photo and send it to mSelectedMediaList
    String videoLat;// Save video lat code after take photo and send it to mSelectedMediaList
    String videoLong;// Save video long code after take photo and send it to mSelectedMediaList
    int deviceW, deviceH, imageW, deviceWPx, deviceHPx;
    boolean isCaptureVideo = true;

    public static List<MediaItem> mSelectedMediaList = new ArrayList<>();
    public static List<MediaItem> mMediaList = new ArrayList<>();

    public final String TAG = SortByDateMediaPickerActivity.this.getClass().getSimpleName();

    boolean isReadExternalStoragePermissionAccepted = false;
    boolean isWriteExternalStoragePermissionAccepted = false;

    MediaItem currentSelectedItem = null;
    int currentSelectedPosition = -1;
    List<MediaList> mSortByDateMediaList = new ArrayList<MediaList>();
    LinearLayout expandableListView_container;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_sort_by_date);

        if (Constants.SELECTED_MEDIA_ITEM_LIST != null) {
            Constants.SELECTED_MEDIA_ITEM_LIST.clear();
        } else {
            Constants.SELECTED_MEDIA_ITEM_LIST = new ArrayList<>();
        }

        if(mMediaList != null){
            mMediaList.clear();
        } else {
            mMediaList = new ArrayList<>();
        }

        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            if (receivedIntent.hasExtra(Constants.MAX_UPLOADABLE_PHOTO))
                max_photo = receivedIntent.getIntExtra(Constants.MAX_UPLOADABLE_PHOTO, 10);
            if (receivedIntent.hasExtra(Constants.MAX_UPLOADABLE_VIDEO))
                max_video = receivedIntent.getIntExtra(Constants.MAX_UPLOADABLE_VIDEO, 1);
            if (receivedIntent.hasExtra(Constants.MAX_UPLOADABLE_VIDEO_DURATION))
                max_video_duration = receivedIntent.getIntExtra(Constants.MAX_UPLOADABLE_VIDEO_DURATION, 10);
            if (receivedIntent.hasExtra(Constants.IS_CAPTURE_VIDEO))
                isCaptureVideo = receivedIntent.getBooleanExtra(Constants.IS_CAPTURE_VIDEO, true);
            if (receivedIntent.hasExtra(Constants.MEDIA_RESULT)) {
                String jsonArr = receivedIntent.getStringExtra(Constants.MEDIA_RESULT);

                Gson gson = new Gson();
                MediaItem[] mediaList = gson.fromJson(jsonArr, MediaItem[].class);

                if (Constants.SELECTED_MEDIA_ITEM_LIST == null)
                    Constants.SELECTED_MEDIA_ITEM_LIST = new ArrayList<>();
                else
                    Constants.SELECTED_MEDIA_ITEM_LIST.clear();

                if (mediaList != null && mediaList.length > 0) {
                    for (MediaItem item : mediaList) {
                        // if(mMediaList != null && mMediaList.size() > 0){
                        //     for(int i=0; i<mMediaList.size(); i++){
                        //         Log.v(TAG, "mMediaList.get(" + i + ").RealUrl: " + mMediaList.get(i).RealUrl);
                        //         Log.v(TAG, "item.RealUrl: " + item.RealUrl);
                        //         if(mMediaList.get(i).RealUrl.equals(item.RealUrl)){
                                    SelectedMediaItem tmpSelectedItem = new SelectedMediaItem();
                                    //tmpSelectedItem.Id = i;
                                    tmpSelectedItem.MediaItem = item;
                                    tmpSelectedItem.MediaItem.IsChecked = true;
                                    Constants.SELECTED_MEDIA_ITEM_LIST.add(tmpSelectedItem);
                                    if (item.RealUrl.toLowerCase().contains("mp4") ||
                                            item.RealUrl.toLowerCase().contains("m4v") ||
                                            item.RealUrl.toLowerCase().contains("mov") ||
                                            item.RealUrl.toLowerCase().contains("3gp"))
                                        Constants.MEDIA_LIST_TYPE = 2;

                                    if (item.RealUrl.toLowerCase().contains("jpg") ||
                                            item.RealUrl.toLowerCase().contains("png") ||
                                            item.RealUrl.toLowerCase().contains("jpeg") ||
                                            item.RealUrl.toLowerCase().contains("gif"))
                                        Constants.MEDIA_LIST_TYPE = 1;
                        //         }
                        //     }
                        // }

                        if (item.RealUrl.toLowerCase().contains("mp4") ||
                                item.RealUrl.toLowerCase().contains("m4v") ||
                                item.RealUrl.toLowerCase().contains("mov") ||
                                item.RealUrl.toLowerCase().contains("3gp"))
                            selected_video++;

                        if (item.RealUrl.toLowerCase().contains("jpg") ||
                                item.RealUrl.toLowerCase().contains("png") ||
                                item.RealUrl.toLowerCase().contains("jpeg") ||
                                item.RealUrl.toLowerCase().contains("gif"))
                            selected_photo++;
                    }
                }
            }
        }

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        deviceWPx = displaymetrics.widthPixels;
        deviceHPx = displaymetrics.heightPixels;
        deviceH = (int) Utils.convertPixelsToDp(displaymetrics.heightPixels, getApplicationContext());
        deviceW = (int) Utils.convertPixelsToDp(displaymetrics.widthPixels, getApplicationContext());

        picassoInstance = new Picasso.Builder(getApplicationContext())
                .memoryCache(new LruCache(2 * 1024 * 1024))
                .addRequestHandler(new VideoRequestHandler())
                .build();

        imageW = deviceWPx/3;

        btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                constantsDestructor();
                finish();
            }
        });

        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Constants.SELECTED_MEDIA_ITEM_LIST.size() > 0)
                    new PrepareSendingData().execute();
                else {
                    showWarningDialog(getString(R.string.txt_limit_add));
                }
            }
        });
        if (!isCaptureVideo && max_video == 0) {
            btnAdd.setEnabled(false);
        }

        btnCapture = (Button) findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(btnCaptureListener);

        expandableListView_container = (LinearLayout)findViewById(R.id.expandableListView_container);
    }

    protected void constantsDestructor(){
        Log.v(TAG, "run constantsDestructor()");
        setResult(Constants.MEDIA_RESULT_CODE, null);
        Constants.STEP = 0;
        Constants.MEDIA_LIST_TYPE = 0;

//        if (Constants.SELECTED_MEDIA_ITEM_LIST != null) {
//            Constants.SELECTED_MEDIA_ITEM_LIST.clear();
//        } else {
//            Constants.SELECTED_MEDIA_ITEM_LIST = new ArrayList<>();
//        }

        if (mMediaList != null) {
            mMediaList.clear();
        } else {
            mMediaList = new ArrayList<>();
        }

        if (mSelectedMediaList != null) {
            mSelectedMediaList.clear();
        } else {
            mSelectedMediaList = new ArrayList<>();
        }

        if(mSortByDateMediaList != null){
            mSortByDateMediaList.clear();
        } else {
            mSortByDateMediaList = new ArrayList<>();
        }

        if(expandableListView_container.getChildCount() > 0){
            expandableListView_container.removeAllViews();
        }
    }

    private View.OnClickListener btnCaptureListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (isCaptureVideo) {
                showActionDialog();
            } else {
                dispatchTakePictureIntent();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        requestPermissions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_IMAGE_CAPTURE:
                dispatchTakePictureIntent();

                break;

            case Constants.REQUEST_VIDEO_CAPTURE:
                if (data != null) {
                    String[] videolist = new String[]{
                            MediaStore.Video.VideoColumns._ID,
                            MediaStore.Video.VideoColumns.DATA,
                            MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME,
                            MediaStore.Video.VideoColumns.DATE_TAKEN,
                            MediaStore.Video.VideoColumns.MIME_TYPE,
                            MediaStore.Video.VideoColumns.LATITUDE,
                            MediaStore.Video.VideoColumns.LONGITUDE,
                    };
                    final Cursor cursorvideo = getContentResolver()
                            .query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videolist, null,
                                    null, MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC");

                    // Put it in the image view
                    if (cursorvideo.moveToFirst()) {
                        videoTaken = cursorvideo.getString(cursorvideo.getColumnIndex(MediaStore.Video.VideoColumns.DATA));
                        videoLat = cursorvideo.getString(cursorvideo.getColumnIndex(MediaStore.Video.VideoColumns.LATITUDE));
                        videoLong = cursorvideo.getString(cursorvideo.getColumnIndex(MediaStore.Video.VideoColumns.LONGITUDE));
                    }
                    cursorvideo.close();

                    if (videoTaken != null) {
                        Intent videoPreview = new Intent(this, VideoPreviewActivity.class);
                        videoPreview.putExtra("video", videoTaken);
                        videoPreview.putExtra(Constants.MAX_UPLOADABLE_VIDEO_DURATION, max_video_duration);
                        startActivityForResult(videoPreview, Constants.REQUEST_VIDEO_PREVIEW);
                    }
                }

                break;

            case Constants.REQUEST_IMAGE_PREVIEW:
                if (resultCode == RESULT_OK) {

                    showWaitingDialog();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Utils.addImageToGallery(mCurrentPhotoPath.replace("file:/", ""), getApplicationContext());
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            //Get latest image from gallery - Chien Nguyen
                            Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            String[] projection = {MediaStore.MediaColumns.DATA,
                                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                                    MediaStore.Images.Media.LATITUDE,
                                    MediaStore.Images.Media.LONGITUDE,
                                    MediaStore.Video.Media.DATE_ADDED};

                            final Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC LIMIT 1");

                            // Put it in the image view
                            assert cursor != null;
                            if (cursor.moveToLast()) {
                                imageTaken = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                                imageLat = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.LATITUDE));
                                imageLong = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.LONGITUDE));
                            }

                            MediaItem item = new MediaItem();
                            LocationItem location = new LocationItem();

                            item.Id = cursor.getCount();
                            item.RealUrl = "file://" + imageTaken;
                            item.Url = imageTaken;
                            item.ThumbUrl = imageTaken;
                            location.Lat = imageLat;
                            location.Lng = imageLong;
                            item.Location = location;
                            item.IsChecked = true;

                            SelectedMediaItem itemSelected = new SelectedMediaItem();
                            itemSelected.Id = 0;
                            itemSelected.MediaItem = item;

                            if (Constants.SELECTED_MEDIA_ITEM_LIST == null)
                                Constants.SELECTED_MEDIA_ITEM_LIST = new ArrayList<>();

                            Constants.SELECTED_MEDIA_ITEM_LIST.clear();
                            Constants.SELECTED_MEDIA_ITEM_LIST.add(itemSelected);

                            cursor.close();

                            mHandler.sendEmptyMessage(0);
                        }
                    }).start();

                } else {
                    new GetMediaFiles().execute();
                }
                break;

            case Constants.REQUEST_VIDEO_PREVIEW:
                if (resultCode == RESULT_OK) {

                    showWaitingDialog();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Get latest image from gallery - Chien Nguyen
                            Uri uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            String[] projection = {MediaStore.MediaColumns.DATA,
                                    MediaStore.Video.Media.LATITUDE,
                                    MediaStore.Video.Media.LONGITUDE,
                                    MediaStore.Video.Media.DATE_ADDED};

                            final Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC LIMIT 1");

                            // Put it in the image view
                            assert cursor != null;

                            MediaItem item = new MediaItem();
                            LocationItem location = new LocationItem();

                            item.Id = cursor.getCount();
                            item.RealUrl = "file://" + videoTaken;
                            item.Url = videoTaken;
                            item.ThumbUrl = "";
                            location.Lat = videoLat;
                            location.Lng = videoLong;
                            item.Location = location;
                            item.IsChecked = true;

                            SelectedMediaItem itemSelected = new SelectedMediaItem();
                            itemSelected.Id = 0;
                            itemSelected.MediaItem = item;

                            if (Constants.SELECTED_MEDIA_ITEM_LIST == null)
                                Constants.SELECTED_MEDIA_ITEM_LIST = new ArrayList<>();

                            Constants.SELECTED_MEDIA_ITEM_LIST.clear();
                            Constants.SELECTED_MEDIA_ITEM_LIST.add(itemSelected);

                            cursor.close();

                            mHandler.sendEmptyMessage(0);
                        }
                    }).start();

                } else {
                    new GetMediaFiles().execute();
                }

                break;

            case Constants.REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK && !TextUtils.isEmpty(mCurrentPhotoPath)) {

                    Intent photoPreview = new Intent(this, PhotoPreviewActivity.class);

                    photoPreview.putExtra("picture", mCurrentPhotoPath);
                    startActivityForResult(photoPreview, Constants.REQUEST_IMAGE_PREVIEW);
                }

                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(Constants.MEDIA_RESULT_CODE, null);

            if (mMediaList != null) {
                mMediaList.clear();
                mMediaList = null;
            }
            if (mSelectedMediaList != null) {
                mSelectedMediaList.clear();
                mSelectedMediaList = null;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private android.os.Handler mHandler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            new PrepareSendingData().execute();
        }
    };

    private void showActionDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new Dialog(SortByDateMediaPickerActivity.this);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.layout_dialog_capture);

        RadioButton btnPhoto = (RadioButton) mDialog.findViewById(R.id.btnTakePhoto);
        RadioButton btnVideo = (RadioButton) mDialog.findViewById(R.id.btnCaptureVideo);

        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dispatchTakePictureIntent();
                mDialog.dismiss();

            }
        });

        btnVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchCaptureVideoIntent();
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            try {
                photoFile = Utils.createImageFile(this);
                mCurrentPhotoPath = "file:" + photoFile.getAbsolutePath();

                Log.v(TAG, "SAVING VIA INTENT");

                // Continue only if the File was successfully created
                if (!photoFile.exists()) {
                    photoFile.mkdirs();
                }

                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, Constants.REQUEST_TAKE_PHOTO);
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatchCaptureVideoIntent() {
        try {
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, max_video_duration);
            takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);//Set quality when record video
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, "mp4");
            if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takeVideoIntent, Constants.REQUEST_VIDEO_CAPTURE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showWarningDialog(String message) {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new Dialog(SortByDateMediaPickerActivity.this);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.layout_dialog_warning);

        TextView tvWarningText = (TextView) mDialog.findViewById(R.id.tvWarningText);
        tvWarningText.setText(message);

        Button btnOk = (Button) mDialog.findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDialog.dismiss();
            }
        });

        mDialog.show();
    }

    private void showWaitingDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.txt_loading));
            mProgressDialog.setCancelable(false);
        }

        if (!mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    private void hideWaitingDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    isReadExternalStoragePermissionAccepted = true;
                    if (isWriteExternalStoragePermissionAccepted) {
                        new GetMediaFiles().execute();
                    }

                }
                break;

            case Constants.WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    isWriteExternalStoragePermissionAccepted = true;
                    if (isReadExternalStoragePermissionAccepted) {
                        new GetMediaFiles().execute();
                    }

                }
                break;

            default:
                break;
        }
    }

    /**
     * Request for each permission
     */
    private void requestPermissions() {
        requestPermission(Constants.READ_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        requestPermission(Constants.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * Request permission
     *
     * @param permissionResultCode Result code
     * @param permission           permission name
     */
    private void requestPermission(int permissionResultCode, String permission) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                AlertDialog.Builder builder = new AlertDialog.Builder(SortByDateMediaPickerActivity.this);
                builder.setMessage(R.string.app_name + " need to access to your Photo, Video, Music folder");
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing;
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{permission}, permissionResultCode);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            if (permission.equals(android.Manifest.permission.READ_EXTERNAL_STORAGE))
                isReadExternalStoragePermissionAccepted = true;
            if (permission.equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                isWriteExternalStoragePermissionAccepted = true;

            if (isReadExternalStoragePermissionAccepted && isWriteExternalStoragePermissionAccepted) {
                if (mMediaList != null)
                    mMediaList.clear();
                Log.e(TAG, "GET DATA : " + ((mMediaList == null) ? "NULL" : mMediaList.size()));
                new GetMediaFiles().execute();
            }
        }
    }

    private void organizeMediaItems(){
        Log.v(TAG, "mediaList: ");
        String previousItemCreatedTime = null;
        String previousFile = "";
        String currentItemCreatedTime;
        MediaList tmpMediaList = new MediaList();
        Calendar mRealTime = Calendar.getInstance();
        Calendar mCurrentTime = Calendar.getInstance();
        if(mSortByDateMediaList.size() == 0){
            for(int i = 0; i< mMediaList.size(); i++){
                currentItemCreatedTime = DateFormat.format("MMMM dd, yyyy", Long.parseLong(mMediaList.get(i).Created_At)).toString();

                Log.v(TAG, "------------------");
                Log.v(TAG, "mediaList[" + i + "].RealUrl: " + mMediaList.get(i).RealUrl + " | Created_At = " + currentItemCreatedTime);

                if (!previousFile.equals(mMediaList.get(i).RealUrl)) {

                    // if Create_At older than 1 month but less than 12 => Group by Months
                    // if Create_At older than 1 year => Group by Years
//                    try {
//                        mCurrentItemCreatedTime.setTimeInMillis(Long.parseLong(mMediaList.get(i).Created_At));
//                        int mMonthsDifferenceInUnixTime =
//                                (mRealTime.get(Calendar.YEAR) - mCurrentItemCreatedTime.get(Calendar.YEAR))* 12 +
//                                        Math.abs(mRealTime.get(Calendar.MONTH) - mCurrentItemCreatedTime.get(Calendar.MONTH));
//                        Log.v(TAG, "mMonthsDifferenceInUnixTime = " + mMonthsDifferenceInUnixTime);
////                        Log.v(TAG, "mRealTime.get(Calendar.YEAR) = " + mRealTime.get(Calendar.YEAR));
////                        Log.v(TAG, "mCurrentItemCreatedTime.get(Calendar.YEAR) = " + mCurrentItemCreatedTime.get(Calendar.YEAR));
////                        Log.v(TAG, "mRealTime.get(Calendar.MONTH) = " + mRealTime.get(Calendar.MONTH));
////                        Log.v(TAG, "mCurrentItemCreatedTime.get(Calendar.MONTH) = " + mCurrentItemCreatedTime.get(Calendar.MONTH));
//                        if(mMonthsDifferenceInUnixTime >=1 && mMonthsDifferenceInUnixTime <12){
//                            currentItemCreatedTime = DateFormat.format("MMMM yyyy", Long.parseLong(mMediaList.get(i).Created_At)).toString();
//                        } else if(mMonthsDifferenceInUnixTime >= 12){
//                            currentItemCreatedTime = DateFormat.format("yyyy", Long.parseLong(mMediaList.get(i).Created_At)).toString();
//                        }
//                    }catch (Exception e){
//                        e.printStackTrace();
//                    }
                    //

                    if(previousItemCreatedTime == null || !previousItemCreatedTime.equals(currentItemCreatedTime)){
                        previousItemCreatedTime = currentItemCreatedTime;
                        if(tmpMediaList.Id != null){
                            mSortByDateMediaList.add(tmpMediaList);
                            tmpMediaList = new MediaList();
                            Log.v(TAG, "------------------");
                            Log.v(TAG, "ADDED tmpMediaList & STARTED new tmpMediaList");
                        }
                        tmpMediaList = new MediaList();
                        tmpMediaList.Id = currentItemCreatedTime;
                        MediaItem tmpMediaItem;
                        tmpMediaItem = mMediaList.get(i);
                        // Process Pre-Selected items
                        if(Constants.SELECTED_MEDIA_ITEM_LIST != null && Constants.SELECTED_MEDIA_ITEM_LIST.size() > 0){
                            for(int k=0; k<Constants.SELECTED_MEDIA_ITEM_LIST.size(); k++){
                                if(tmpMediaItem.RealUrl.equals(Constants.SELECTED_MEDIA_ITEM_LIST.get(k).MediaItem.RealUrl)){
                                    tmpMediaItem.IsChecked = true;
                                    Log.v(TAG, "CHECKED ITEM L1: " + tmpMediaItem.RealUrl);
                                }
                            }
                        }
                        //
                        tmpMediaItem.OrderNumber = i;
                        List<MediaItem> tmpMediaItemList = new ArrayList<MediaItem>();
                        tmpMediaItemList.add(mMediaList.get(i));
                        tmpMediaList.mediaList = tmpMediaItemList;
                        Log.v(TAG, "ADDED mediaItem, order: " + i);
                    } else {
                        MediaItem tmpMediaItem;
                        tmpMediaItem = mMediaList.get(i);
                        // Process Pre-Selected items
                        if(Constants.SELECTED_MEDIA_ITEM_LIST != null && Constants.SELECTED_MEDIA_ITEM_LIST.size() > 0){
                            for(int k=0; k<Constants.SELECTED_MEDIA_ITEM_LIST.size(); k++){
                                if(tmpMediaItem.RealUrl.equals(Constants.SELECTED_MEDIA_ITEM_LIST.get(k).MediaItem.RealUrl)){
                                    tmpMediaItem.IsChecked = true;
                                    Log.v(TAG, "CHECKED ITEM L2: " + tmpMediaItem.RealUrl);
                                }
                            }
                        }
                        //
                        tmpMediaItem.OrderNumber = i;
                        tmpMediaList.mediaList.add(tmpMediaItem);
                        Log.v(TAG, "ADDED mediaItem, order: " + i);
                    }
                    if(i==mMediaList.size()-1){
                        previousItemCreatedTime = currentItemCreatedTime;
                        mSortByDateMediaList.add(tmpMediaList);
                        tmpMediaList = new MediaList();
                        Log.v(TAG, "ADDED tmpMediaList END");
                        Log.v(TAG, "------------------");
                    }
                    previousFile = mMediaList.get(i).RealUrl;
                } else if(i==mMediaList.size()-1 && tmpMediaList.mediaList.size() > 0){
                    previousItemCreatedTime = currentItemCreatedTime;
                    mSortByDateMediaList.add(tmpMediaList);
                    tmpMediaList = new MediaList();
                    Log.v(TAG, "ADDED tmpMediaList END");
                    Log.v(TAG, "------------------");
                }
            }

            //TEST
            if(mSortByDateMediaList != null && mSortByDateMediaList.size()>0){
                //for(int m = 0; m<mMediaList.size(); m++){
                    Log.v(TAG, "mSortByDateMediaList.size(): " + mSortByDateMediaList.size());
                    int mDupeTime = 0;
                    for(int i=0; i<mSortByDateMediaList.size(); i++){
                        Log.v(TAG, "mSortByDateMediaList[" + i + "].Id = " + mSortByDateMediaList.get(i).Id);
                        Log.v(TAG, "mSortByDateMediaList[" + i + "].mediaList.size() = " + mSortByDateMediaList.get(i).mediaList.size());
                        for(int j=0; j<mSortByDateMediaList.get(i).mediaList.size(); j++){
                            MediaItem tmpMediaItem = mSortByDateMediaList.get(i).mediaList.get(j);
                            Log.v(TAG, "mSortByDateMediaList[" + i + "].mediaList[" + j + "].URL= " +
                                    tmpMediaItem.Url + " | " +
                                    DateFormat.format("MMMM dd, yyyy", Long.parseLong(tmpMediaItem.Created_At)).toString() +
                                    ", Order: " + tmpMediaItem.OrderNumber + ", IsCheck: " + tmpMediaItem.IsChecked
                            );
//                            if(tmpMediaItem.RealUrl != null && mMediaList.get(m).RealUrl != null){
//                                String mFileNameFromMediaList = mMediaList.get(m).RealUrl.substring(mMediaList.get(m).RealUrl.lastIndexOf("/")+1);
//                                String mFileNameOftmpMediaItem = tmpMediaItem.RealUrl.substring(tmpMediaItem.RealUrl.lastIndexOf("/")+1);
//                                if(mFileNameFromMediaList.equals(mFileNameOftmpMediaItem) && m != tmpMediaItem.OrderNumber){
//                                    Log.v(TAG, "DUPLICATE: " + tmpMediaItem.RealUrl);
//                                    //mSortByDateMediaList.get(i).mediaList.remove(j+1);
//                                    //tmpMediaItem.RealUrl = null;
//                                    //Log.v(TAG, "REMOVED DUPLICATED FILE");
//                                }
//                            }
                        }
                    }
                    if(mMediaList != null && mMediaList.size() > 0){
                        Log.v(TAG, "mMediaList.size()= " + mMediaList.size());
                        Log.v(TAG, "mSortByDateMediaList.get(0).mediaList.size()= " + mSortByDateMediaList.get(0).mediaList.size());
                    }
                //}

            }
            //----

            initiateMediaListExpandableView();
            hideWaitingDialog();
        }
    }

    public void expandChosenGroups() {
        final Handler handler = new Handler() {};
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                int l = 0;
                while (l < Constants.EXPAND_GROUPS.size()) {
                        if(expandableListView_container != null && expandableListView_container.getChildAt(Constants.EXPAND_GROUPS.get(l)) != null){
                            Constants.STEP = 0;
                            ExpandableListView ex = (ExpandableListView) expandableListView_container.getChildAt(Constants.EXPAND_GROUPS.get(l)).findViewById(R.id.expandableListView);
                            ex.expandGroup(0);
                        }
                    l++;
                    }
                }
        }, 1000);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub

        if(event.getPointerCount() > 1) {
            Log.v(TAG, "Multitouch detected!");
            return true;
        }
        else
            return super.onTouchEvent(event);
    }

    public void initiateMediaListExpandableView(){
        int i = 0;
        while(i < mSortByDateMediaList.size()){
            boolean haveSelectedItems = false;
            Constants.STEP = 0;
            View view = getLayoutInflater().inflate(R.layout.layout_expandable_sorted_photos, null, true);
            //
            final ExpandableListView expandableListView = (ExpandableListView) view.findViewById(R.id.expandableListView);
            //expandableListDetail = ExpandableListDataPump.getData();
            final HashMap<String, List<MediaItem>> expandableListDetail = ExpandableListDataPump.setData(mSortByDateMediaList.get(i));
            final String[] expandableListTitle = expandableListDetail.keySet().toArray(new String[expandableListDetail.keySet().size()]);
            //Log.v(TAG, "expandableListTitle.length = " + expandableListTitle.length);
            Log.v(TAG, "expandableListTitle[0] = " + expandableListTitle[0]);
            ExpandableListAdapter expandableListAdapter = new CustomExpandableListAdapter(SortByDateMediaPickerActivity.this, expandableListTitle[0], expandableListDetail, max_photo, max_video, imageW);
            expandableListView.setAdapter(expandableListAdapter);
            expandableListView.setFocusable(false);
            expandableListView.setFocusableInTouchMode(false);
            expandableListView.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    return (event.getAction() == MotionEvent.ACTION_MOVE);
                }
            });
            expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

                @Override
                public void onGroupExpand(int groupPosition) {                    
                    Constants.STEP = 0;
                    Log.v(TAG, "imageW: " + imageW);
                    int numberOfRows;
                    int height = 0;
                    int numberOfItems = expandableListDetail.get(expandableListTitle[0]).size();
                    if(numberOfItems > 0){
                        if(numberOfItems <= 3 ){
                            numberOfRows = 1;
                        } else {
                            if(numberOfItems % 3 == 0){
                                numberOfRows = numberOfItems / 3;
                            } else {
                                numberOfRows = numberOfItems / 3 + 1;
                            }
                        }
                        Log.v(TAG, "numberOfRows * imageW = " + numberOfRows + " * " + imageW + " = " + numberOfRows * (imageW+expandableListView.getDividerHeight()));
                        height +=numberOfRows*(imageW+expandableListView.getDividerHeight())+135;
                    }
                    expandableListView.setLayoutParams(new LinearLayout.LayoutParams(expandableListView.getWidth(), height));
//                    Toast.makeText(getApplicationContext(),
//                            expandableListTitle[0] + ". Height = " + height,
//                            Toast.LENGTH_SHORT).show();
                }
            });
            int j = 0;
            while(j < mSortByDateMediaList.get(i).mediaList.size()){
                if(mSortByDateMediaList.get(i).mediaList.get(j).IsChecked){
                    //Log.v(TAG, "expandableListView.getCount() = " + expandableListView.getCount());
                    //expandableListView.expandGroup(0);
                    haveSelectedItems = true;
                    break;
                }
                j++;
            }

            expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {

                @Override
                public void onGroupCollapse(int groupPosition) {
//                    Toast.makeText(getApplicationContext(),
//                            expandableListTitle[groupPosition] + " List Collapsed.",
//                            Toast.LENGTH_SHORT).show();
                    expandableListView.getLayoutParams().height = 137;
                }
            });

            expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v,
                                            int groupPosition, int childPosition, long id) {
//                    Toast.makeText(
//                            getApplicationContext(),
//                            expandableListTitle[groupPosition]
//                                    + " -> "
//                                    + expandableListDetail.get(
//                                    expandableListTitle[groupPosition]).get(
//                                    childPosition), Toast.LENGTH_SHORT
//                    ).show();
                    return false;
                }
            });
            //
            expandableListView_container.addView(view);
            if(haveSelectedItems){
                Constants.EXPAND_GROUPS.add(expandableListView_container.getChildCount()-1);
            }
            //Log.v(TAG, "expandableListView_containter.size() = " + expandableListView_container.getChildCount());
            i++;
        }
        expandChosenGroups();
    }

    /**
     * Async Task Class - used to get all media paths in background
     */
    class GetMediaFiles extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.v(TAG, "GetMediaFiles onPreExcute");
            constantsDestructor();

            // Pre setup media list to have first item as Capture button
//            MediaItem fakeItem = new MediaItem();
//            fakeItem.Url = null;
//
//            mMediaList.add(0, fakeItem);

            showWaitingDialog();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if(mMediaList.size() == 0){
                getAllShownImagesPath(SortByDateMediaPickerActivity.this);
                if (isCaptureVideo && max_video > 0)
                    getAllShownVideosPath(SortByDateMediaPickerActivity.this);
            }

            //checkSelectedItems();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);


//            if (imageAdapter == null) {
//                imageAdapter = new ImageAdapter(SortByDateMediaPickerActivity.this, 0, 0, mMediaList);
//            }
            try{
                Collections.sort(mMediaList, new Comparator<MediaItem>() {
                    @Override
                    public int compare(MediaItem lhs, MediaItem rhs) {
                        if (lhs.Created_At != null && rhs.Created_At != null) {
                            try {
                                long lhTime = Long.parseLong(lhs.Created_At);
                                long rhTime = Long.parseLong(rhs.Created_At);

                                if (lhTime > rhTime)
                                    return -1;

                                if (lhTime < rhTime)
                                    return 1;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        return 0;
                    }
                });
            } catch(ConcurrentModificationException e){
                e.printStackTrace();
                Log.v(TAG, "mMediaList.size(): " + mMediaList.size());
                if(mMediaList != null && mMediaList.size() > 0){
                    organizeMediaItems();
                }
            } catch(ArrayIndexOutOfBoundsException e){
                e.printStackTrace();
                Log.v(TAG, "mMediaList.size(): " + mMediaList.size());
                if(mMediaList != null && mMediaList.size() > 0){
                    organizeMediaItems();
                }
            }

            Log.v(TAG, "mMediaList.size(): " + mMediaList.size());
            if(mMediaList != null && mMediaList.size() > 0){
                organizeMediaItems();
            }

            hideWaitingDialog();
            //imageAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Getting All Images Path
     *
     * @param activity current activity
     */
    private void getAllShownImagesPath(Activity activity) {
            Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.MediaColumns.DATA,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.LATITUDE,
                    MediaStore.Images.Media.LONGITUDE,
                    MediaStore.Images.Media.DATE_ADDED};

            runQuery(activity, uri, projection);
    }

    /**
     * Getting All Videos Path
     *
     * @param activity current activity
     */
    public void getAllShownVideosPath(Activity activity) {


        Uri uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.MediaColumns.DATA,
                MediaStore.Video.Media.LATITUDE,
                MediaStore.Video.Media.LONGITUDE,
                MediaStore.Video.Media.DATE_ADDED};
        runQuery(activity, uri, projection);

    }

    private void runQuery(Activity activity, Uri uri, String[] projection) {
        int column_index_data;
        String absolutePathOfImage, lat, lng;
            Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC");

            assert cursor != null;
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);

                lat = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE));
                lng = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE));

                LocationItem location = new LocationItem();
                location.Lat = TextUtils.isEmpty(lat) ? "0" : lat;
                location.Lng = TextUtils.isEmpty(lng) ? "0" : lng;

                MediaItem item = new MediaItem();
                item.Id = mMediaList.size();
                item.RealUrl = "file://" + absolutePathOfImage;
                item.Url = "file://" + absolutePathOfImage;
                item.ThumbUrl = "";
                item.Location = location;
                item.Created_At = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)) + "000";
                //item.Created_At = Utils.getImageDateTaken(absolutePathOfImage);

                mMediaList.add(item);
            }
            cursor.close();
    }

    class PrepareSendingData extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showWaitingDialog();
        }

        @Override
        protected String doInBackground(Void... voids) {
            List<MediaItem> tmpData = new ArrayList<>();
            for(int i = 0; i < Constants.SELECTED_MEDIA_ITEM_LIST.size(); i++) {
                MediaItem item = Constants.SELECTED_MEDIA_ITEM_LIST.get(i).MediaItem;
                if (item.RealUrl.toLowerCase().contains("http") || item.RealUrl.toLowerCase().contains("https")) {
                    continue;
                }

                if (item.RealUrl.toLowerCase().contains("mp4") ||
                        item.RealUrl.toLowerCase().contains("m4v") ||
                        item.RealUrl.toLowerCase().contains("mov") ||
                        item.RealUrl.toLowerCase().contains("3gp")) {
                    Bitmap thumbBit = ThumbnailUtils.createVideoThumbnail(item.Url.replace("file://", ""), MediaStore.Video.Thumbnails.MICRO_KIND);

                    item.ThumbUrl = "file://" + Utils.saveImage(getApplicationContext(), thumbBit, "thumb_" + item.Url);

                    File trimmedVideo = Utils.getOutputMediaFile(getApplicationContext(), item.Url, "mp4");
                    try {
                        long duration = Utils.getVideoDuration(getApplicationContext(), new File(item.Url.replace("file://", "")));
                        if (duration > max_video_duration) {
                            Utils.startTrim(new File(item.Url.replace("file://", "")), trimmedVideo, 0, max_video_duration * 1000);

                            item.Url = "file://" + trimmedVideo.getAbsolutePath();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        thumbBit.recycle();
                    }

                }

                if (item.RealUrl.toLowerCase().contains("jpg") ||
                        item.RealUrl.toLowerCase().contains("jpeg") ||
                        item.RealUrl.toLowerCase().contains("png") ||
                        item.RealUrl.toLowerCase().contains("gif")) {

                    Bitmap matrixBitmap = Utils.rotateImage(item.RealUrl.replace("file://", ""));

                    String url = item.RealUrl;

                    int scaleW = matrixBitmap.getWidth(), scaleH = matrixBitmap.getHeight();

                    if (scaleW > Constants.MAX_SCALED_SIZE) {
                        scaleW = Constants.MAX_SCALED_SIZE;
                        scaleH = (Constants.MAX_SCALED_SIZE * matrixBitmap.getHeight()) / matrixBitmap.getWidth();

                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(matrixBitmap, scaleW, scaleH, true);
                        url = Utils.saveImage(getApplicationContext(), scaledBitmap, item.RealUrl);

                        scaledBitmap.recycle();
                    }

                    item.Url = "file://" + url;
                    item.ThumbUrl = "file://" + url;

                    matrixBitmap.recycle();
                }
                tmpData.add(item);
            }

            Gson gson = new Gson();
            Constants.STEP=0;
            return gson.toJson(tmpData);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            hideWaitingDialog();

            Log.e(TAG, s);

            if (s != null) {
                Intent data = new Intent();
                data.putExtra(Constants.MEDIA_RESULT, s);
                setResult(Constants.MEDIA_RESULT_CODE, data);

                // Reset Constants
                Constants.STEP = 0;
                Constants.MEDIA_LIST_TYPE = 0;

                //        if (Constants.SELECTED_MEDIA_ITEM_LIST != null) {
                //            Constants.SELECTED_MEDIA_ITEM_LIST.clear();
                //        } else {
                //            Constants.SELECTED_MEDIA_ITEM_LIST = new ArrayList<>();
                //        }

                if (mMediaList != null) {
                    mMediaList.clear();
                } else {
                    mMediaList = new ArrayList<>();
                }

                if (mSelectedMediaList != null) {
                    mSelectedMediaList.clear();
                } else {
                    mSelectedMediaList = new ArrayList<>();
                }

                if(mSortByDateMediaList != null){
                    mSortByDateMediaList.clear();
                } else {
                    mSortByDateMediaList = new ArrayList<>();
                }

                if(expandableListView_container.getChildCount() > 0){
                    expandableListView_container.removeAllViews();
                }

                finish();
            }
        }
    }
}
