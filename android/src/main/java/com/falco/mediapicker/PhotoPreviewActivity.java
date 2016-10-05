package com.falco.mediapicker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by Admin on 9/19/16.
 */
public class PhotoPreviewActivity extends Activity {

    Bitmap bitmapPhotoPreview;
    ProgressDialog mProgressDialog;

    TouchImageView  imgPreview;
    Button btnBack, btnUse;
    String mCurrentPhotoPath;

    public final String PHOTO_PATH = "PHOTO_PATH";
    public final int REQUEST_IMAGE_PREVIEW = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_photo_preview);

        imgPreview = (TouchImageView ) findViewById(R.id.imgPreview);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnUse = (Button) findViewById(R.id.btnUse);
        Intent data = getIntent();
        if (data.hasExtra("picture"))
            mCurrentPhotoPath =  data.getExtras().getString("picture");

        imgPreview.setMaxZoom(3f);

        bitmapPhotoPreview = Utils.rotaionImage(mCurrentPhotoPath.replace("file:/", ""));
        if (bitmapPhotoPreview != null && !bitmapPhotoPreview.isRecycled())
            imgPreview.setImageBitmap(bitmapPhotoPreview);

        btnBack.setOnClickListener(backListener);
        btnUse.setOnClickListener(useListener);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        bitmapPhotoPreview.recycle();
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
            intent.putExtra("imageTaken",mCurrentPhotoPath);
            setResult(RESULT_OK, intent);
            finish();
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
