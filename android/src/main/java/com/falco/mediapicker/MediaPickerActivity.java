package com.falco.mediapicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Bond Nguyen on 9/18/16.
 */
public class MediaPickerActivity extends Activity {

    ProgressDialog mProgressDialog;
    Picasso picassoInstance;
    GridView gridMedia;
    Button btnBack, btnAdd;
    ImageAdapter imageAdapter;
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

    public static List<MediaItem> mSelectedMediaList = new ArrayList<>();
    public static List<MediaItem> mReceivedMediaList = new ArrayList<>();
    public static List<MediaItem> mMediaList = new ArrayList<>();

    public final String TAG = MediaPickerActivity.this.getClass().getSimpleName();

    boolean isReadExternalStoragePermissionAccepted = false;
    boolean isWriteExternalStoragePermissionAccepted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            if (receivedIntent.hasExtra(Constants.MAX_UPLOADABLE_PHOTO))
                max_photo = receivedIntent.getIntExtra(Constants.MAX_UPLOADABLE_PHOTO, 10);
            if (receivedIntent.hasExtra(Constants.MAX_UPLOADABLE_VIDEO))
                max_video = receivedIntent.getIntExtra(Constants.MAX_UPLOADABLE_VIDEO, 1);
            if (receivedIntent.hasExtra(Constants.MAX_UPLOADABLE_VIDEO_DURATION))
                max_video_duration = receivedIntent.getIntExtra(Constants.MAX_UPLOADABLE_VIDEO_DURATION, 10);
            if (receivedIntent.hasExtra(Constants.MEDIA_RESULT)) {
                String jsonArr = receivedIntent.getStringExtra(Constants.MEDIA_RESULT);

                Gson gson = new Gson();
                MediaItem[] mediaList = gson.fromJson(jsonArr, MediaItem[].class);

                if (mReceivedMediaList == null)
                    mReceivedMediaList = new ArrayList<>();
                else
                    mReceivedMediaList.clear();

                if (mediaList != null && mediaList.length > 0) {
                    for (MediaItem item : mediaList) {
                        Log.e(TAG, "" + item.Id);
                        mReceivedMediaList.add(item);

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

        gridMedia = (GridView) findViewById(R.id.gridMedia);
        gridMedia.setOnItemClickListener(mediaItemClickListener);

        imageW = deviceW / 3;

        btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                setResult(Constants.MEDIA_RESULT_CODE, null);

                if (mMediaList != null) {
                    mMediaList.clear();
                    mMediaList = null;
                }
                if (mSelectedMediaList != null) {
                    mSelectedMediaList.clear();
                    mSelectedMediaList = null;
                }
                if (mReceivedMediaList != null) {
                    mReceivedMediaList.clear();
                    mReceivedMediaList = null;
                }

                finish();
            }
        });

        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedMediaList.size() > 0 || mReceivedMediaList.size() > 0)
                    new PrepareSendingData().execute();
                else {
                    showWarningDialog(getString(R.string.txt_limit_add));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

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

                            final Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC LIMIT 1");

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

                            if (mSelectedMediaList == null)
                                mSelectedMediaList = new ArrayList<>();

                            if (mReceivedMediaList != null)
                                mReceivedMediaList.clear();

                            mSelectedMediaList.clear();
                            mSelectedMediaList.add(item);

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

                            final Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC LIMIT 1");

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

                            if (mSelectedMediaList == null)
                                mSelectedMediaList = new ArrayList<>();

                            if (mReceivedMediaList != null)
                                mReceivedMediaList.clear();

                            mSelectedMediaList.clear();
                            mSelectedMediaList.add(item);

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

    private AdapterView.OnItemClickListener mediaItemClickListener = new AdapterView.OnItemClickListener() {
        /**
         * Requirement: Only can select #max_photo Photo file(s) and #max_video Video file(s)
         * When reaching to limitation, user cannot select any item
         * Select Photo files, then selecing Video file => warning and Otherwise
         * @param parent Parent view
         * @param view Selected view
         * @param position selected view position
         * @param id selected view id
         */
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            MediaItem item = mMediaList.get(position);
            if (view != null && item != null) {
                ImageView imgSelected = (ImageView) view.findViewById(R.id.ic_selected);

                if (item.IsChecked) {
                    imgSelected.setVisibility(View.GONE);

                    if (item.Url.toLowerCase().contains("mp4") ||
                            item.Url.toLowerCase().contains("mov") ||
                            item.Url.toLowerCase().contains("3gp") ||
                            item.Url.toLowerCase().contains("m4v")) {
                        selected_video--;
                    } else if (item.Url.toLowerCase().contains("png") ||
                            item.Url.toLowerCase().contains("jpg") ||
                            item.Url.toLowerCase().contains("jpeg")) {
                        selected_photo--;
                    }
                    item.IsChecked = false;

                    int i = 0;
                    while (i < mSelectedMediaList.size()) {
                        if (item.Id == mSelectedMediaList.get(i).Id) {
                            mSelectedMediaList.remove(i);
                            break;
                        }

                        i++;
                    }
                } else {
                    if ((item.Url.toLowerCase().contains("mp4") || item.Url.toLowerCase().contains("mov") || item.Url.toLowerCase().contains("3gp") || item.Url.toLowerCase().contains("m4v"))
                            && selected_video < max_video) {

                        // Case user already select photo then they select video
                        if (selected_photo > 0) {
                            showWarningDialog(getString(R.string.txt_warning));
                            return;
                        }


                        selected_video++;
                        mSelectedMediaList.add(item);
                        imgSelected.setVisibility(View.VISIBLE);
                        item.IsChecked = true;

                    } else if ((item.Url.toLowerCase().contains("png") ||
                            item.Url.toLowerCase().contains("jpg") ||
                            item.Url.toLowerCase().contains("jpeg")) && selected_photo < max_photo) {

                        // Case user already select video then they select photo
                        if (selected_video > 0) {
                            showWarningDialog(getString(R.string.txt_warning));
                            return;
                        }


                        selected_photo++;
                        mSelectedMediaList.add(item);
                        imgSelected.setVisibility(View.VISIBLE);
                        item.IsChecked = true;

                    } else {
                        if (item.Url.toLowerCase().contains("mp4") ||
                                item.Url.toLowerCase().contains("mov") ||
                                item.Url.toLowerCase().contains("3gp") ||
                                item.Url.toLowerCase().contains("m4v"))
                            showWarningDialog(getString(R.string.txt_warning_video).replace("#V", "" + max_video));
                        else
                            showWarningDialog(getString(R.string.txt_warning_photo).replace("#P", "" + max_photo));
                        return;
                    }
                }

                mMediaList.get(position).IsChecked = item.IsChecked;
            }
        }
    };

    private View.OnClickListener btnCaptureListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showActionDialog();
        }
    };

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

                AlertDialog.Builder builder = new AlertDialog.Builder(MediaPickerActivity.this);
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
                if (mMediaList == null || mMediaList.size() == 0) {
                    Log.e(TAG, "GET DATA : " + ((mMediaList == null) ? "NULL" : mMediaList.size()));
                    new GetMediaFiles().execute();
                }
            }
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
                MediaStore.Video.Media.DATE_ADDED};

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

        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC");

        assert cursor != null;
        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);

            lat = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE));
            lng = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE));

            LocationItem location = new LocationItem();
            location.Lat = TextUtils.isEmpty(lat) ? "0" : lat;
            location.Lng = TextUtils.isEmpty(lng) ? "0" : lng;;

            MediaItem item = new MediaItem();
            item.Id = mMediaList.size();
            item.RealUrl = "file://" + absolutePathOfImage;
            item.Url = "file://" + absolutePathOfImage;
            item.ThumbUrl = "";
            item.Location = location;
            item.Created_At = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED));

            mMediaList.add(item);
        }
        cursor.close();
    }

    private void checkSelectedItems() {

        for (MediaItem item : mReceivedMediaList) {
            if (item != null) {
//                if (item.RealUrl.toLowerCase().contains("http") ||
//                        item.RealUrl.toLowerCase().contains("https")) {
//
//                    MediaItem itemFromServer = new MediaItem();
//                    itemFromServer.Id = mMediaList.size();
//                    itemFromServer.RealUrl = item.RealUrl;
//                    itemFromServer.Url = item.Url;
//                    itemFromServer.ThumbUrl = item.ThumbUrl;
//                    itemFromServer.Location = item.Location;
//                    itemFromServer.Created_At = String.valueOf(System.currentTimeMillis());
//                    itemFromServer.IsChecked = true;
//                    itemFromServer.isTrophyAlbum = item.isTrophyAlbum;
//
//                    mMediaList.add(1, itemFromServer);
//
//                } else {
                    for (MediaItem originItem : mMediaList) {
                        if (originItem != null &&
                                originItem.RealUrl != null &&
                                originItem.RealUrl.equals(item.RealUrl)) {
                            originItem.IsChecked = item.IsChecked;
                            break;
                        }
                    }
//                }
            }
        }
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
        Intent takePictureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, max_video_duration);
        takePictureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);//Set quality when record video
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, "mp4");
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Constants.REQUEST_VIDEO_CAPTURE);
        }
    }

    private void showActionDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new Dialog(MediaPickerActivity.this);
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

    private void showWarningDialog(String message) {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new Dialog(MediaPickerActivity.this);
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

    /**
     * Async Task Class - used to get all media paths in background
     */
    class GetMediaFiles extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (mMediaList != null) {
                mMediaList.clear();
            } else {
                mMediaList = new ArrayList<>();
            }

            if (mSelectedMediaList == null)
                mSelectedMediaList = new ArrayList<>();

            // Pre setup media list to have first item as Capture button
            MediaItem fakeItem = new MediaItem();
            fakeItem.Url = null;

            mMediaList.add(0, fakeItem);

            showWaitingDialog();
        }

        @Override
        protected Void doInBackground(Void... params) {

            getAllShownImagesPath(MediaPickerActivity.this);
            getAllShownVideosPath(MediaPickerActivity.this);

            checkSelectedItems();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            hideWaitingDialog();
            if (imageAdapter == null) {

                imageAdapter = new ImageAdapter(MediaPickerActivity.this, 0, 0, mMediaList);

                gridMedia.setAdapter(imageAdapter);
            }

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

            imageAdapter.notifyDataSetChanged();
        }
    }

    class PrepareSendingData extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showWaitingDialog();
        }

        @Override
        protected String doInBackground(Void... voids) {
            for (MediaItem item : mSelectedMediaList) {
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

                    Bitmap matrixBitmap = Utils.rotaionImage(item.RealUrl.replace("file://", ""));

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
            }

            Gson gson = new Gson();
            List<MediaItem> newList = new ArrayList<>();
            if (mReceivedMediaList != null ) {
                newList.addAll(mReceivedMediaList);
                newList.addAll(mSelectedMediaList);
            }

            return gson.toJson(newList);
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

                if (mMediaList != null) {
                    mMediaList.clear();
                    mMediaList = null;
                }
                if (mSelectedMediaList != null) {
                    mSelectedMediaList.clear();
                    mSelectedMediaList = null;
                }
                if (mReceivedMediaList != null) {
                    mReceivedMediaList.clear();
                    mReceivedMediaList = null;
                }

                finish();
            }
        }
    }

    /**
     * Adapter class - used to load all images into Grid
     */
    class ImageAdapter extends ArrayAdapter<MediaItem> {

        Context mContext;
        List<MediaItem> mediaList;

        public ImageAdapter(Context context, int resource, int textViewResourceId, List<MediaItem> objects) {
            super(context, resource, textViewResourceId, objects);

            mContext = context;
            mediaList = objects;
        }


        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (position == 0) {
                convertView = li.inflate(R.layout.layout_btn_capture, null);

                Button btnCapture = (Button) convertView.findViewById(R.id.btnCapture);
                btnCapture.setOnClickListener(btnCaptureListener);

            } else {

                MediaItem item = mediaList.get(position);
                boolean isPhoto = true;

                if (item != null) {
                    if (item.RealUrl.toLowerCase().contains("mp4") ||
                            item.RealUrl.toLowerCase().contains("mov") ||
                            item.RealUrl.toLowerCase().contains("m4v") ||
                            item.RealUrl.toLowerCase().contains("3gp"))
                        isPhoto = false;

                    convertView = li.inflate(R.layout.layout_photo, null);

                    ImageView imgView = (ImageView) convertView.findViewById(R.id.imgView);
                    ImageView imgSelected = (ImageView) convertView.findViewById(R.id.ic_selected);
                    ImageView imgPlay = (ImageView) convertView.findViewById(R.id.ic_play);

                    imgSelected.setVisibility((item.IsChecked) ? View.VISIBLE : View.GONE);

                    imgPlay.setVisibility((isPhoto) ? View.GONE : View.VISIBLE);
                    imgView.setTag(item.Url);

                    if (isPhoto) {
                        if (item.RealUrl.toLowerCase().contains("http") || item.RealUrl.toLowerCase().contains("https")) {
                            picassoInstance.with(getApplicationContext())
                                    .load(Uri.parse(item.RealUrl))
                                    .resize(100, 100)
                                    .centerCrop()
                                    .into(imgView);
                        } else {
                            picassoInstance.with(getApplicationContext())
                                    .load(item.RealUrl)
                                    .resize(100, 100)
                                    .centerCrop()
                                    .into(imgView);
                        }


                    } else {
                        picassoInstance.load(VideoRequestHandler.SCHEME_VIEDEO + ":" + ((item.RealUrl.toLowerCase().contains("file://")) ? item.RealUrl.replace("file://", "") : item.RealUrl))
                                .resize(100, 100)
                                .centerCrop()
                                .into(imgView);
                    }

                }
            }

            return convertView;
        }
    }
}
