package com.kremor.karadio.main;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.OkHttpClient;

/**
 * Created by kremor on 13.07.2017.
 */

class Station {
    private int id;
    private String name;
    private String url;
    private String file;
    private String port;
    private int stationVolume;
    private static List<Station> stations = new ArrayList<>();

    public Station(int id, String name, String url, String file, String port, int stationVolume) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.file = file;
        this.port = port;
        this.stationVolume = stationVolume;
    }

    public Station(int i) {
        this.id = i;
        this.name = "Station";
        this.stationVolume = 50;
    }

    public Station(String name, int id) {
        this.id = id;
        this.name = name;
        this.stationVolume = 50;
    }

    @Override
    public String toString() {
        return "Station " + id;
    }

    public static int getPrevStation(int i) {
        if (i <= 0) return 0;
        else
            return i - 1;
    }

    public static int getNextStation(int i) {
        if (i >= stations.size()) return stations.size();
        else
            return i + 1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStationVolume() {
        return stationVolume;
    }

    public void setStationVolume(int stationVolume) {
        this.stationVolume = stationVolume;
    }

    public static List<Station> createMockStationList() {
        List<Station> list = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            list.add(new Station(i + 1));
        }
        return list;
    }

}
