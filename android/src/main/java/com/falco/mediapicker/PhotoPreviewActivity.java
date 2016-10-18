package com.falco.mediapicker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;

/**
 * Created by Admin on 9/19/16.
 */
public class PhotoPreviewActivity extends Activity {

    Bitmap bitmapPhotoPreview;
    ProgressDialog mProgressDialog;

    TouchImageView imgPreview;
    Button btnBack, btnUse;
    String mCurrentPhotoPath;

    public final int REQUEST_TAKE_PHOTO = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_photo_preview);

        imgPreview = (TouchImageView) findViewById(R.id.imgPreview);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnUse = (Button) findViewById(R.id.btnUse);
        Intent data = getIntent();
        if (data.hasExtra("picture"))
            mCurrentPhotoPath = data.getExtras().getString("picture");

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

        if (bitmapPhotoPreview != null)
            bitmapPhotoPreview.recycle();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!TextUtils.isEmpty(mCurrentPhotoPath))
                Utils.deleteFile(mCurrentPhotoPath);

            dispatchTakePictureIntent();

            return true;
        }
        return false;
    }

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!TextUtils.isEmpty(mCurrentPhotoPath))
                Utils.deleteFile(mCurrentPhotoPath);

            dispatchTakePictureIntent();
        }
    };

    private View.OnClickListener useListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Utils.deleteFile(mCurrentPhotoPath);

            Intent intent = new Intent();
            intent.putExtra("imageTaken", mCurrentPhotoPath);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            try {
                photoFile = Utils.createImageFile(this);
                mCurrentPhotoPath = "file:" + photoFile.getAbsolutePath();

                // Continue only if the File was successfully created
                if (!photoFile.exists()) {
                    photoFile.mkdirs();
                }

                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK && !TextUtils.isEmpty(mCurrentPhotoPath)) {
                    bitmapPhotoPreview = Utils.rotaionImage(mCurrentPhotoPath.replace("file:/", ""));
                    if (bitmapPhotoPreview != null && !bitmapPhotoPreview.isRecycled())
                        imgPreview.setImageBitmap(bitmapPhotoPreview);
                }
                break;
        }
    }
}
