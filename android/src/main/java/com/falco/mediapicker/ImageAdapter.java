package com.falco.mediapicker;

/**
 * Created by pqthuy on 04/26/2017.
 */
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.falco.mediapicker.R;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ImageAdapter extends BaseAdapter {
    private Context context;
    private final List<MediaItem> expandableListDetail;
    public final String TAG = ImageAdapter.this.getClass().getSimpleName();
    public final int deviceW, deviceH, deviceWPx, deviceHPx, imageW;
    Picasso picassoInstance;

    public ImageAdapter(Context context, List<MediaItem> expandableListDetail) {
        this.context = context;
        this.expandableListDetail = expandableListDetail;

        picassoInstance = new Picasso.Builder(context)
                .memoryCache(new LruCache(2 * 1024 * 1024))
                .addRequestHandler(new VideoRequestHandler())
                .build();

        DisplayMetrics displaymetrics = Resources.getSystem().getDisplayMetrics();
        deviceWPx = displaymetrics.widthPixels;
        deviceHPx = displaymetrics.heightPixels;
        deviceH = (int) Utils.convertPixelsToDp(displaymetrics.heightPixels, context);
        deviceW = (int) Utils.convertPixelsToDp(displaymetrics.widthPixels, context);
        imageW = deviceW / 3 + deviceW % 3;
        //Log.v(TAG, "deviceW= " + deviceW + " | deviceH = " + deviceH);
        Log.v(TAG, "expandableListDetail.size(): " + this.expandableListDetail.size());
    }


    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if(Constants.STEP < (expandableListDetail.size())){
                MediaItem item = expandableListDetail.get(Constants.STEP);
                Log.v(TAG, "item.RealUrl: " + item.RealUrl);
                Log.v(TAG, "Constants.STEP= " + Constants.STEP);

                boolean isPhoto = true;

                if (item != null) {
                    if (item.RealUrl.toLowerCase().contains("mp4") ||
                            item.RealUrl.toLowerCase().contains("mov") ||
                            item.RealUrl.toLowerCase().contains("m4v") ||
                            item.RealUrl.toLowerCase().contains("3gp"))
                        isPhoto = false;

                    if(convertView == null){
                        convertView = inflater.inflate(R.layout.layout_photo, null);

                        ImageView imgView = (ImageView) convertView.findViewById(R.id.imgView);
                        ImageView imgSelected = (ImageView) convertView.findViewById(R.id.ic_selected);
                        ImageView imgPlay = (ImageView) convertView.findViewById(R.id.ic_play);

                        imgSelected.setVisibility((item.IsChecked) ? View.VISIBLE : View.GONE);

                        imgPlay.setVisibility((isPhoto) ? View.GONE : View.VISIBLE);
                        imgView.setTag(item.Url);

//                        ExifInterface is a class for reading and writing Exif tags in a JPEG file or a RAW image file.
//                        Supported formats are: JPEG, DNG, CR2, NEF, NRW, ARW, RW2, ORF, PEF, SRW and RAF.
                        if (isPhoto) {
                            if (item.RealUrl.toLowerCase().contains("http") || item.RealUrl.toLowerCase().contains("https")) {
                                picassoInstance.with(context)
                                        .load(Uri.parse(item.RealUrl))
                                        .resize(imageW, imageW)
                                        .centerCrop()
                                        .into(imgView);
                            } else {
                                picassoInstance.with(context)
                                        .load(item.RealUrl)
                                        .resize(imageW, imageW)
                                        .centerCrop()
                                        .into(imgView);
                            }
                        } else {
                            picassoInstance.load(VideoRequestHandler.SCHEME_VIEDEO + ":" + ((item.RealUrl.toLowerCase().contains("file://")) ? item.RealUrl.replace("file://", "") : item.RealUrl))
                                    .resize(imageW, imageW)
                                    .centerCrop()
                                    .into(imgView);
                        }
                    }

                }
                Constants.STEP++;
            }
//        }

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.layout_photo, null);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return expandableListDetail.size();
    }

    @Override
    public Object getItem(int position) {
        return expandableListDetail.get(position);
    }

    @Override
    public long getItemId(int position) {
        return expandableListDetail.get(position).Id;
    }

}
