package com.falco.mediapicker;

/**
 * Created by pqthuy on 04/24/2017.
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private String expandableListTitle;
    private HashMap<String, List<MediaItem>> expandableListDetail;
    private List<MediaItem> mediaItemList = new ArrayList<>();
    public final String TAG = CustomExpandableListAdapter.this.getClass().getSimpleName();
    //private final List<MediaItem> expandableListDetail;
    public final int deviceW, deviceH, deviceWPx, deviceHPx, imageW, max_photo, max_video;
    Picasso picassoInstance;
    Dialog mDialog;
    public CustomExpandableListAdapter(Context context, String expandableListTitle,
                                       HashMap<String, List<MediaItem>> expandableListDetail, int max_photo, int max_video) {
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
        this.max_photo = max_photo;
        this.max_video = max_video;

        picassoInstance = new Picasso.Builder(context)
                .memoryCache(new LruCache(2 * 1024 * 1024))
                .addRequestHandler(new VideoRequestHandler())
                .build();

        DisplayMetrics displaymetrics = Resources.getSystem().getDisplayMetrics();
        deviceWPx = displaymetrics.widthPixels;
        deviceHPx = displaymetrics.heightPixels;
        deviceH = (int) Utils.convertPixelsToDp(displaymetrics.heightPixels, context);
        deviceW = (int) Utils.convertPixelsToDp(displaymetrics.widthPixels, context);
        imageW = deviceWPx /3;
        //Log.v(TAG, "deviceW= " + deviceW + " | deviceH = " + deviceH);

        if(expandableListDetail != null && expandableListDetail.values().size() > 0){
            //Getting Collection of values from HashMap
            Collection<List<MediaItem>> values = expandableListDetail.values();
            //Creating an ArrayList of values
            ArrayList<List<MediaItem>> listOfValues = new ArrayList<List<MediaItem>>(values);
//            Log.v(TAG, "listOfValues.size(): " + listOfValues.size());
//            Log.v(TAG, "listOfValues.get(0).size(): " + listOfValues.get(0).size());
//            Log.v(TAG, "listOfValues.get(0).get(0): " + listOfValues.get(0).get(0));
            mediaItemList = listOfValues.get(0);
        }
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        return this.expandableListDetail.get(this.expandableListTitle);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public boolean isPhoto(MediaItem item){
        if(item.RealUrl.toLowerCase().contains("mp4") ||
                item.RealUrl.toLowerCase().contains("mov") ||
                item.RealUrl.toLowerCase().contains("m4v") ||
                item.RealUrl.toLowerCase().contains("3gp")){
            return false;
        }
        return true;
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        LayoutInflater layoutInflater = (LayoutInflater) this.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.layout_list_item, null);
            LinearLayout viewRow = (LinearLayout) convertView.findViewById(R.id.ll_rowItem);
            LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(  ViewGroup.LayoutParams.WRAP_CONTENT,   ViewGroup.LayoutParams.WRAP_CONTENT);
            viewRow.setLayoutParams(linearLayoutParams);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(imageW, imageW);
            int i = 0;
            while(i < 3){
                if(Constants.STEP < (mediaItemList.size()) && mediaItemList.get(Constants.STEP).RealUrl != null){
                    final MediaItem item = mediaItemList.get(Constants.STEP);
                    Log.v(TAG, "item.RealUrl: " + item.RealUrl);
                    Log.v(TAG, "Constants.STEP= " + Constants.STEP);

                    View viewItem = layoutInflater.inflate(R.layout.layout_photo, null);
                    viewItem.setLayoutParams(layoutParams);
                    boolean isPhoto = isPhoto(item);
                    ImageView imgView = (ImageView) viewItem.findViewById(R.id.imgView);
                    imgView.setLayoutParams(layoutParams);
                    ImageView imgSelected = (ImageView) viewItem.findViewById(R.id.ic_selected);
                    imgView.setTag(imgSelected);
                    ImageView imgPlay = (ImageView) viewItem.findViewById(R.id.ic_play);

                    imgSelected.setVisibility((item.IsChecked) ? View.VISIBLE : View.GONE);
                    if(item.IsChecked){
                        Log.v(TAG, "CHECKED PRE_SELECTED ITEM: " + item.RealUrl);
                    }

                    imgPlay.setVisibility((isPhoto) ? View.GONE : View.VISIBLE);

                    viewItem.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.v(TAG, "PRESSED ITEM: " + item.RealUrl);
                            if(Constants.MEDIA_LIST_TYPE == 0){
                                if (isPhoto(item)){
                                    Constants.MEDIA_LIST_TYPE = 1;
                                } else {
                                    Constants.MEDIA_LIST_TYPE = 2;
                                }
                            } else if((!isPhoto(item) && Constants.MEDIA_LIST_TYPE == 1) ||
                                      (isPhoto(item) && Constants.MEDIA_LIST_TYPE == 2)){
                                showWarningDialog(context, context.getResources().getString(R.string.txt_warning));
                            }
                            if((Constants.SELECTED_MEDIA_ITEM_LIST.size() < max_photo && Constants.MEDIA_LIST_TYPE == 1 && isPhoto(item)) ||
                               (Constants.SELECTED_MEDIA_ITEM_LIST.size() < max_video && Constants.MEDIA_LIST_TYPE == 2 && !isPhoto(item))){

                                SelectedMediaItem tmpSelectedMediaItem = new SelectedMediaItem();
                                tmpSelectedMediaItem.Id = item.OrderNumber;
                                tmpSelectedMediaItem.MediaItem = item;
                                ImageView itemImgSelected = (ImageView) v.findViewById(R.id.ic_selected);
                                if(Constants.SELECTED_MEDIA_ITEM_LIST.size() == 0 && !item.IsChecked){
                                    item.IsChecked = true;
                                    itemImgSelected.setVisibility(View.VISIBLE);
                                    Constants.SELECTED_MEDIA_ITEM_LIST.add(tmpSelectedMediaItem);
                                } else {
                                    boolean isNew = true;
                                    int j1 = 0;
                                    while( j1 < Constants.SELECTED_MEDIA_ITEM_LIST.size()){
                                      if(Constants.SELECTED_MEDIA_ITEM_LIST.get(j1).MediaItem.RealUrl.equals(tmpSelectedMediaItem.MediaItem.RealUrl)){
                                            item.IsChecked = false;
                                            itemImgSelected.setVisibility(View.GONE);
                                          Constants.SELECTED_MEDIA_ITEM_LIST.remove(Constants.SELECTED_MEDIA_ITEM_LIST.get(j1));
                                            isNew = false;
                                            break;
                                        }
                                      j1++;
                                    }
                                    if(isNew){
                                        item.IsChecked = true;
                                        itemImgSelected.setVisibility(View.VISIBLE);
                                        Constants.SELECTED_MEDIA_ITEM_LIST.add(tmpSelectedMediaItem);
                                        Log.v(TAG, "ADDED ITEM L2");
                                    }
                                }
                            } else if((Constants.SELECTED_MEDIA_ITEM_LIST.size() == max_photo && Constants.MEDIA_LIST_TYPE == 1 && isPhoto(item)) ||
                                    (Constants.SELECTED_MEDIA_ITEM_LIST.size() == max_video && Constants.MEDIA_LIST_TYPE == 2 && !isPhoto(item))){
                                SelectedMediaItem tmpSelectedMediaItem = new SelectedMediaItem();
                                tmpSelectedMediaItem.Id = item.OrderNumber;
                                tmpSelectedMediaItem.MediaItem = item;
                                ImageView itemImgSelected = (ImageView) v.findViewById(R.id.ic_selected);
                                boolean IsNew = true;
                                int j2 = 0;
                                while( j2 < Constants.SELECTED_MEDIA_ITEM_LIST.size()){
                                  if(Constants.SELECTED_MEDIA_ITEM_LIST.get(j2).MediaItem.RealUrl.equals(tmpSelectedMediaItem.MediaItem.RealUrl)){
                                        item.IsChecked = false;
                                        itemImgSelected.setVisibility(View.GONE);
                                      Constants.SELECTED_MEDIA_ITEM_LIST.remove(Constants.SELECTED_MEDIA_ITEM_LIST.get(j2));
                                        IsNew = false;
                                        break;
                                    }
                                  j2++;
                                }
                                if(IsNew){
                                    String tmpMessage;
                                    if(Constants.MEDIA_LIST_TYPE == 1){
                                        tmpMessage = context.getResources().getString(R.string.txt_warning_photo).replace("#P", String.valueOf(max_photo));
                                    } else {
                                        tmpMessage = context.getResources().getString(R.string.txt_warning_video).replace("#V", String.valueOf(max_video));
                                    }
                                    showWarningDialog(context, tmpMessage);
                                }
                            }
                            Log.v(TAG, "Constants.SELECTED_MEDIA_ITEM_LIST.size() = " + Constants.SELECTED_MEDIA_ITEM_LIST.size());
                            if(Constants.SELECTED_MEDIA_ITEM_LIST.size() ==0){
                                Constants.MEDIA_LIST_TYPE = 0;
                            }
                        }
                    });

                    if (isPhoto) {
                        if (item.RealUrl.toLowerCase().contains("http") || item.RealUrl.toLowerCase().contains("https")) {
                            picassoInstance.with(context)
                                    .load(Uri.parse(item.RealUrl))
                                    .resize(imageW/2, imageW/2)
                                    .centerCrop()
                                    .into(imgView);
                        } else {
                            picassoInstance.with(context)
                                    .load(item.RealUrl)
                                    .resize(imageW/2, imageW/2)
                                    .centerCrop()
                                    .into(imgView);
                        }
                    } else {
                        picassoInstance.load(VideoRequestHandler.SCHEME_VIEDEO + ":" + ((item.RealUrl.toLowerCase().contains("file://")) ? item.RealUrl.replace("file://", "") : item.RealUrl))
                                .resize(imageW/2, imageW/2)
                                .centerCrop()
                                .into(imgView);
                    }
                    viewRow.addView(viewItem);
                    Constants.STEP++;
                }
                i++;
            }
        }

        return convertView;
    }

    private void showWarningDialog(Context mContext, String message) {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new Dialog(mContext);
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

    @Override
    public int getChildrenCount(int listPosition) {
        return this.expandableListDetail.get(this.expandableListTitle).size();
    }

    @Override
    public Object getGroup(int listPosition) {
        return this.expandableListTitle;
    }

    @Override
    public int getGroupCount() {
        return expandableListDetail.size();
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        Constants.STEP = 0;
        String listTitle = getGroup(listPosition) + " - " + getChildrenCount(listPosition) + " item";
        if(getChildrenCount(listPosition)>1){
            listTitle += "s";
        }
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.layout_list_group, null);
        }
        TextView listTitleTextView = (TextView) convertView
                .findViewById(R.id.listTitle);
        listTitleTextView.setTypeface(null, Typeface.BOLD);
        listTitleTextView.setText(listTitle);
        Constants.STEP = 0;
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }
}
