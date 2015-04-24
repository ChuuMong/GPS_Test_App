package com.example.locationtestapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.locationtestapp.service.FusedLocationService;
import com.example.locationtestapp.service.RestartService;
import com.example.locationtestapp.stack.HttpStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements Response.Listener<String>, Response.ErrorListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private LinearLayout gps_info_layout;
    private RestartService receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gps_info_layout = (LinearLayout) findViewById(R.id.gps_info_layout);

        //        if (PreferenceManager.getDefaultSharedPreferences(this).getString("USER_ID", null) == null) {
        //            StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://bold-meridian-91808.appspot.com/user", this, this);
        //            HttpStack.getInstance(this).addToRequestQueue(stringRequest);
        //        }

        //        new Handler().postDelayed(new Runnable() {
        //            @Override
        //            public void run() {
        //                goGpsServiceReg();
        //            }
        //        }, 1000);
        goGpsServiceReg();

        LocalBroadcastManager.getInstance(this).registerReceiver(mHandleMessageReceiver, new IntentFilter(AppConstant.GPS_INFO_BROADCAST));
    }

    private void showGPSInfo(Intent intent) {
        if (intent.getExtras().get("LOCATION") != null) {
            final Location location = (Location) intent.getExtras().get("LOCATION");
            gps_info_layout.addView(new GpsInfoView(this, location).getView());
        }
        if (intent.getExtras().get("OLD_GOOGLE_PLAY_VERSION") != null) {
            Toast.makeText(this, "Google Play의 버전이 오래되어 위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }


    private void goGpsServiceReg() {
        receiver = new RestartService();        // 리시버 등록

        try {
            IntentFilter mainFilter = new IntentFilter("com.example.locationtestapp.ssss");      // xml에서 정의해도 됨
            registerReceiver(receiver, mainFilter);                                          // 리시버 저장
            startService(new Intent(this, FusedLocationService.class));                      // 서비스 시작
        }
        catch (Exception e) {
            Log.d("RestartService", e.getMessage() + "");
        }
    }


    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AppConstant.GPS_INFO_BROADCAST)) {
                Log.i(TAG, "mHandleMessageReceiver GROUP_INVITED_ACTION 호출");
                showGPSInfo(intent);
            }
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onResponse(String response) {
        Log.i(TAG, "USER_ID : " + response);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("USER_ID", response).commit();
    }

    @Override
    public void onErrorResponse(VolleyError error) {

    }

}
