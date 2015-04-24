package com.example.locationtestapp;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by JongHunLee on 2015-04-21.
 */
public class GpsInfoView {

    private final Context context;
    private Location location;
    private View view;

    public GpsInfoView(Context context, Location location) {
        this.context = context;
        this.location = location;
        init();
    }

    private void init() {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_row_gpsinfo_item, null);
        }

        ((TextView) view.findViewById(R.id.gps_info_time)).setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        ((TextView) view.findViewById(R.id.gps_info_latitude)).setText(String.valueOf(location.getLatitude()));
        ((TextView) view.findViewById(R.id.gps_info_longitude)).setText(String.valueOf(location.getLongitude()));
    }

    public View getView() {
        return view;
    }
}
