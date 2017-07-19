package com.kremor.karadio.main;

import android.content.Context;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public String toString() {
        return "Station " + id;
    }

    public static int getPrevStation(int i) {
        if (i <= 0) return 0;
        else
            return i-1;
    }

    public static int getNextStation(int i) {
        if (i >= stations.size()) return stations.size();
        else
            return i+1;
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

    public static List<Station> readStationList(Context context) {
        String filePath = null;
        JSONObject jObj;
        filePath = PreferenceManager.getDefaultSharedPreferences(context).getString(MyPreferencesActivity.STATION_LIST_PATH, "def_web.txx");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"), 8);
            String line = null;
            while ((line = reader.readLine()) != null) {
                jObj = new JSONObject(line);
                if (!jObj.getString("Name").isEmpty()) {
                    stations.add(new Station(0, jObj.getString("Name"),
                            jObj.getString("URL"),
                            jObj.getString("File"),
                            jObj.getString("Port"), 100));
                }
            }
            return Station.stations;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        return createMockStationList();
    }

}
