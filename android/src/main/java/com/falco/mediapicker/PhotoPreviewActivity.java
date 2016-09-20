package com.falco.mediapicker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Admin on 9/19/16.
 */
public class PhotoPreviewActivity extends Activity {

    Bitmap bitmapPhotoPreview;
    ProgressDialog mProgressDialog;

    ImageView imgPreview;
    Button btnBack, btnUse;

    public final String IS_BACK_FROM_PREVIEW = "IS_BACK_FROM_PREVIEW";
    public final String PHOTO_PATH = "PHOTO_PATH";
    public final int REQUEST_IMAGE_PREVIEW = 4;
    public final int REQUEST_VIDEO_PREVIEW = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_photo_preview);

        Intent data = getIntent();
        if (data != null) {
            Bundle extras = data.getExtras();
            bitmapPhotoPreview = (Bitmap) extras.get("data");
        }

        imgPreview = (ImageView) findViewById(R.id.imgPreview);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnUse = (Button) findViewById(R.id.btnUse);

        imgPreview.setImageBitmap(bitmapPhotoPreview);
        btnBack.setOnClickListener(backListener);
        btnUse.setOnClickListener(useListener);
    }

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent data = new Intent();
            data.putExtra(IS_BACK_FROM_PREVIEW, false);

            setResult(REQUEST_IMAGE_PREVIEW, data);
            finish();
        }
    };

    private View.OnClickListener useListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

        }
    };

    private void showWaitingDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(PhotoPreviewActivity.this);
            mProgressDialog.setMessage(getString(R.string.txt_loading));
            mProgressDialog.setCancelable(false);
        }

        mProgressDialog.show();
    }

    private void hideWaitingDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    class WriteBitmapToFile extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showWaitingDialog();
        }

        @Override
        protected String doInBackground(Void... voids) {
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            hideWaitingDialog();

            Intent data = new Intent();
            data.putExtra(PHOTO_PATH, s);

            setResult(REQUEST_IMAGE_PREVIEW, data);
            finish();
        }
    }
}
