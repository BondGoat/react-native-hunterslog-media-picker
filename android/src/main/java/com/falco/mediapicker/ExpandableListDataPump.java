package com.falco.mediapicker;

/**
 * Created by pqthuy on 04/24/2017.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExpandableListDataPump {
    public static HashMap<String, List<String>> getData() {
        HashMap<String, List<String>> expandableListDetail = new HashMap<String, List<String>>();

        List<String> cricket = new ArrayList<String>();
        cricket.add("India");
        cricket.add("Pakistan");
        cricket.add("Australia");
        cricket.add("England");
        cricket.add("South Africa");

        List<String> football = new ArrayList<String>();
        football.add("Brazil");
        football.add("Spain");
        football.add("Germany");
        football.add("Netherlands");
        football.add("Italy");

        List<String> basketball = new ArrayList<String>();
        basketball.add("United States");
        basketball.add("Spain");
        basketball.add("Argentina");
        basketball.add("France");
        basketball.add("Russia");

        expandableListDetail.put("CRICKET TEAMS", cricket);
        expandableListDetail.put("FOOTBALL TEAMS", football);
        expandableListDetail.put("BASKETBALL TEAMS", basketball);
        return expandableListDetail;
    }

    public static HashMap<String, List<MediaItem>> setData(MediaList mediaList) {
        HashMap<String, List<MediaItem>> expandableListDetail = new HashMap<String, List<MediaItem>>();

        for(int i=0; i< mediaList.mediaList.size(); i++){
            List<MediaItem> list = new ArrayList<MediaItem>();
            for(int j=0; j< mediaList.mediaList.size() ; j++){
                list.add(mediaList.mediaList.get(j));
            }
            expandableListDetail.put(mediaList.Id, list);
        }
        return expandableListDetail;
    }
}