package com.falco.mediapicker;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_video_preview);

        videoPreview = (VideoView ) findViewById(R.id.videoPreview);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnUse = (Button) findViewById(R.id.btnUse);
        btnplay = (ImageButton) findViewById(R.id.btnPlayVideo);
        Intent data = getIntent();
        mCurrentVideoPath =  data.getExtras().getString("video");

        Uri uri = Uri.parse(mCurrentVideoPath);
        videoPreview.setMediaController(new MediaController(this));
        videoPreview.setVideoURI(uri);
        videoPreview.requestFocus();
        videoPreview.seekTo(100);
        //videoPreview.start();
        btnBack.setOnClickListener(backListener);
        btnUse.setOnClickListener(useListener);

        //Move status bar to start point when video is finished
        videoPreview.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer)
            {
                videoPreview.seekTo(50);
                btnplay.setVisibility(View.VISIBLE);
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


    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
        }
    };


    private View.OnClickListener useListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.putExtra("videoTaken",mCurrentVideoPath);
            setResult(RESULT_OK, intent);
            finish();
        }
    };
}
