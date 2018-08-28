package com.example.e.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mobgeek.adsdk.evideoview.EVideoView;

public class VideoActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        getSupportActionBar().hide();
        EVideoView viewById = (EVideoView) findViewById(R.id.video);
        viewById.setVideoPath("rtmp://live.hkstv.hk.lxdns.com/live/hks");
    }
}
