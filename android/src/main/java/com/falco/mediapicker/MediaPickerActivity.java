package com.falco.mediapicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by Bond Nguyen on 9/18/16.
 */
public class MediaPickerActivity extends Activity {

    List<String> mMediaPaths = new ArrayList<>();
    Map<Integer, Boolean> mapCheckPosition = new ArrayMap<>();
    ProgressDialog mDialog;
    Picasso picassoInstance;
    GridView gridMedia;
    Button btnBack, btnAdd;
    ImageAdapter imageAdapter;
    int max_photo = 10, max_video = 1, max_video_duration = 10;
    int selected_photo = 0, selected_video = 0;

    // Permission list request code
    public final static int READ_EXTERNAL_STORAGE = 0;
    public final static int WRITE_EXTERNAL_STORAGE = 1;
    // --------------------------

    // Config value
    public final String MAX_UPLOADABLE_PHOTO = "MAX_UPLOADABLE_PHOTO";
    public final String MAX_UPLOADABLE_VIDEO = "MAX_UPLOADABLE_VIDEO";
    public final String MAX_UPLOADABLE_VIDEO_DURATION = "MAX_UPLOADABLE_VIDEO_DURATION";
    public final static int MEDIA_RESULT_CODE = 0;
    // --------------------------

    public final String TAG = MediaPickerActivity.this.getClass().getSimpleName();

    boolean isReadExternalStoragePermissionAccepted = false;
    boolean isWriteExternalStoragePermissionAccepted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            if (receivedIntent.hasExtra(MAX_UPLOADABLE_PHOTO))
                max_photo = receivedIntent.getIntExtra(MAX_UPLOADABLE_PHOTO, 10);
            if (receivedIntent.hasExtra(MAX_UPLOADABLE_VIDEO))
                max_video = receivedIntent.getIntExtra(MAX_UPLOADABLE_VIDEO, 1);
            if (receivedIntent.hasExtra(MAX_UPLOADABLE_VIDEO_DURATION))
                max_video_duration = receivedIntent.getIntExtra(MAX_UPLOADABLE_VIDEO_DURATION, 10);
        }

        picassoInstance = new Picasso.Builder(getApplicationContext())
                .memoryCache(new LruCache(2 * 1024 * 1024))
                .addRequestHandler(new VideoRequestHandler())
                .build();

        gridMedia = (GridView) findViewById(R.id.gridMedia);
        gridMedia.setOnItemClickListener(mediaItemClickListener);

        btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();

                setResult(MEDIA_RESULT_CODE, intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        requestPermissions();
    }

    private AdapterView.OnItemClickListener mediaItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String path = mMediaPaths.get(position);
            if (view != null) {
                boolean isChecked = mapCheckPosition.get(position);
                ImageView imgSelected = (ImageView) view.findViewById(R.id.ic_selected);

                if (isChecked) {
                    imgSelected.setVisibility(View.GONE);

                    if (path.toLowerCase().contains("mp4")) {
                        selected_video--;
                    } else if (path.toLowerCase().contains("png") ||
                            path.toLowerCase().contains("jpg") ||
                            path.toLowerCase().contains("jpeg")) {
                        selected_photo--;
                    }
                } else {
                    if (path.toLowerCase().contains("mp4") && selected_video < max_video) {
                        selected_video++;
                    } else if ((path.toLowerCase().contains("png") ||
                            path.toLowerCase().contains("jpg") ||
                            path.toLowerCase().contains("jpeg")) && selected_photo < max_photo) {
                        selected_photo++;
                    } else {
                        return;
                    }

                    imgSelected.setVisibility(View.VISIBLE);
                }

                boolean value = mapCheckPosition.remove(position);
                value = !value;
                mapCheckPosition.put(position, value);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE:
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

            case WRITE_EXTERNAL_STORAGE:
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

            default: break;
        }
    }

    /**
     * Request for each permission
     */
    private void requestPermissions() {
        requestPermission(READ_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        requestPermission(WRITE_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * Request permission
     * @param permissionResultCode Result code
     * @param permission permission name
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
                if (mMediaPaths == null || mMediaPaths.size() == 0)
                    new GetMediaFiles().execute();
            }
        }
    }

    /**
     * Getting All Images Path
     *
     * @param activity current activity
     */
    private void getAllShownImagesPath(Activity activity) {
        Uri uri;
        Cursor cursor;
        int column_index_data;
        String absolutePathOfImage;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

        cursor = activity.getContentResolver().query(uri, projection, null,
                null, null);

        assert cursor != null;
        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);

            try {
                ExifInterface exifInterface = new ExifInterface(absolutePathOfImage);
                String lat = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                String lng = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);

                Log.e(TAG, absolutePathOfImage + " - " + lat + "," + lng);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mapCheckPosition.put(mMediaPaths.size(), false);
            mMediaPaths.add("file://" + absolutePathOfImage);
        }

        cursor.close();
    }

    /**
     * Getting All Videos Path
     *
     * @param activity current activity
     */
    public void getAllShownVideosPath(Activity activity) {
        Uri uri;
        Cursor cursor;
        int column_index_data;
        String absolutePathOfImage;
        uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.MediaColumns.DATA,
                MediaStore.Video.Thumbnails.DATA };

        cursor = activity.getContentResolver().query(uri, projection, null, null, null);

        assert cursor != null;
        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA);
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);

            try {
                ExifInterface exifInterface = new ExifInterface(absolutePathOfImage);
                String lat = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                String lng = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);

                Log.e(TAG, absolutePathOfImage + " - " + lat + "," + lng);

                mapCheckPosition.put(mMediaPaths.size(), false);
                mMediaPaths.add("file://" + absolutePathOfImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cursor.close();
    }

    private void showWaitingDialog() {
        if (mDialog == null) {
            mDialog = new ProgressDialog(MediaPickerActivity.this);
            mDialog.setMessage("Loading...");
            mDialog.setCancelable(false);
        }

        mDialog.show();
    }

    private void hideWaitingDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    /**
     * Async Task Class - used to get all media paths in background
     */
    class GetMediaFiles extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mMediaPaths != null)
                mMediaPaths.clear();

            showWaitingDialog();
        }

        @Override
        protected Void doInBackground(Void... params) {

            getAllShownImagesPath(MediaPickerActivity.this);
            getAllShownVideosPath(MediaPickerActivity.this);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            hideWaitingDialog();
            if (imageAdapter == null) {
                imageAdapter = new ImageAdapter(MediaPickerActivity.this, 0, 0, mMediaPaths);
                imageAdapter.sort(new Comparator<String>() {
                    @Override
                    public int compare(String s, String t1) {
                        return (s.equals(t1)) ? 0 : 1;
                    }
                });
                gridMedia.setAdapter(imageAdapter);
            }

            imageAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Adapter class - used to load all images into Grid
     */
    class ImageAdapter extends ArrayAdapter<String> {

        Context mContext;
        List<String> paths;

        public ImageAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
            super(context, resource, textViewResourceId, objects);

            mContext = context;
            paths = objects;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String path = paths.get(position);
            boolean isPhoto = true;

            if (!TextUtils.isEmpty(path)) {
                if (path.toLowerCase().contains("mp4"))
                    isPhoto = false;

                if (convertView == null) {
                    LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = li.inflate(R.layout.layout_photo, null);
                }

                ImageView imgView = (ImageView) convertView.findViewById(R.id.imgView);
                ImageView imgSelected = (ImageView) convertView.findViewById(R.id.ic_selected);
                ImageView imgPlay = (ImageView) convertView.findViewById(R.id.ic_play);

                boolean isChecked = mapCheckPosition.get(position);
                imgSelected.setVisibility((isChecked) ? View.VISIBLE : View.GONE);

                imgPlay.setVisibility((isPhoto) ? View.GONE : View.VISIBLE);
                imgView.setTag(path);

                if (isPhoto) {
                    picassoInstance.load(path)
                            .resize(100,100)
                            .centerCrop()
                            .into(imgView);
                } else {
                    picassoInstance.load(VideoRequestHandler.SCHEME_VIEDEO + ":" + path.replace("file://", ""))
                            .resize(100,100)
                            .centerCrop()
                            .into(imgView);
                }

            }

            return convertView;
        }
    }
}
