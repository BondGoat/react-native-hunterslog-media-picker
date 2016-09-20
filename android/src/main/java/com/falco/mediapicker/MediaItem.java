package com.falco.mediapicker;

/**
 * Created by Admin on 9/18/16.
 */
//{
//        "location":{"lat":10.21545,"lng":101.24548},
//        "thumbUrl":"file:\/\/\/storage\/emulated\/0\/DCIM\/100MEDIA\/VIDEO0003.jpg",
//        "Url":"file:\/\/\/storage\/emulated\/0\/DCIM\/100MEDIA\/VIDEO0003.jpg",
//        "isTrophyAlbum":false
//        },

public class MediaItem {
    public String Url;
    public String ThumbUrl;
    public LocationItem Location;
}

class LocationItem {
    public String Lat;
    public String Lng;
}