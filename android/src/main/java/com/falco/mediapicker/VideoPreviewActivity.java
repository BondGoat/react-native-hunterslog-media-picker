package com.falco.mediapicker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * Created by ncchien on 10/3/16.
 */
public class VideoPreviewActivity extends Activity {

    VideoView videoPreview;
    Button btnBack, btnUse;
    ImageButton btnplay;
    String mCurrentVideoPath;
    int max_video_duration = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_video_preview);

        videoPreview = (VideoView) findViewById(R.id.videoPreview);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnUse = (Button) findViewById(R.id.btnUse);
        btnplay = (ImageButton) findViewById(R.id.btnPlayVideo);
        Intent data = getIntent();
        mCurrentVideoPath = data.getStringExtra("video");
        max_video_duration = data.getIntExtra(Constants.MAX_UPLOADABLE_VIDEO_DURATION, 10);

        Uri uri = Uri.parse(mCurrentVideoPath);
        videoPreview.setMediaController(new MediaController(this));
        videoPreview.setVideoURI(uri);
        videoPreview.requestFocus();
        videoPreview.seekTo(100);
        //videoPreview.start();
        btnBack.setOnClickListener(backListener);
        btnUse.setOnClickListener(useListener);

        //Move status bar to start point when video is finished
        videoPreview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                videoPreview.seekTo(50);
//                btnplay.setVisibility(View.VISIBLE);
            }
        });


        btnplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoPreview.start();
                btnplay.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (videoPreview.isPlaying()) {
                videoPreview.stopPlayback();
//                btnplay.setVisibility(View.VISIBLE);
            }
            dispatchCaptureVideoIntent();

            return true;
        }
        return false;
    }

    private void dispatchCaptureVideoIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, max_video_duration);
        takePictureIntent.putExtra("EXTRA_VIDEO_QUALITY", 1);//Set quality when record video
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Constants.REQUEST_VIDEO_CAPTURE);
        }
    }


    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (videoPreview.isPlaying()) {
                videoPreview.stopPlayback();
//                btnplay.setVisibility(View.VISIBLE);
            }
            dispatchCaptureVideoIntent();
        }
    };


    private View.OnClickListener useListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (videoPreview.isPlaying()) {
                videoPreview.stopPlayback();
            }

            Intent intent = new Intent();
            intent.putExtra("videoTaken", mCurrentVideoPath);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
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
                        mCurrentVideoPath = cursorvideo.getString(cursorvideo.getColumnIndex(MediaStore.Video.VideoColumns.DATA));
                    }
                    cursorvideo.close();

                    videoPreview.setVideoURI(Uri.parse(mCurrentVideoPath));
                    videoPreview.requestFocus();
                    videoPreview.seekTo(50);
                }

                break;
        }
    }
}
