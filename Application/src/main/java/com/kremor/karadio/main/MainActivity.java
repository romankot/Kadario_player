/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kremor.karadio.main;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class MainActivity extends ListActivity  {
    private static final Uri DOCS_URI = Uri.parse(
            "https://github.com/karawin/Ka-Radio");
    public static final String FIRSTRUN = "firstrun";
    public static final String PREFERENCE = "PREFERENCE";

    public static final String DEFAULT_RADIO_IP = "http://192.168.1.110";
    String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()
    String station = "1";
    String volume = "50";
    boolean radioPlaying = false;
    private ImageButton playButton;
    private TextView statusBar;
    public int mCurrentStation = 0;
    List<Station> mStationList;
    private RequestQueue mRequestQueue;
    private int mCurrentVolume = 80;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mRequestQueue = Volley.newRequestQueue(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mStationList = readStationList(this, mRequestQueue);
        setListAdapter(mListAdapter);
        sendCommand("infos", 0);
        statusBar = (TextView) findViewById(R.id.statusBar);

        findViewById(R.id.prev_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int prevStation = prevStation(mCurrentStation);
                if (radioPlaying) {
                    statusBar.setText(mStationList.get(prevStation) + " is playing");
                    sendCommand("play", prevStation);
                }
            }
        });
        findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int nextStation = nextStation(mCurrentStation);
                if (radioPlaying) {
                    sendCommand("play", nextStation);
                    statusBar.setText(mStationList.get(nextStation) + " is playing");
                }
            }
        });
        playButton = (ImageButton) findViewById(R.id.play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            //@RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(View view) {
                if (!radioPlaying){
                    //send get reqeust to start
                    sendCommand("play", mCurrentStation);
                    statusBar.setText(mStationList.get(mCurrentStation) + " is playing");
                    playButton.setImageResource(R.drawable.profile_pause);
                    radioPlaying = true;
                } else {
                    // pausing play
                    sendCommand("stop", 0);
                    statusBar.setText(mStationList.get(mCurrentStation) + " has stop");
                    playButton.setImageResource(R.drawable.profile_play);
                    radioPlaying = false;
                }
            }
        });

        findViewById(R.id.tipTextView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //when showButton is clicked show hidden_view
                v.setVisibility(View.GONE);
            }
        });

        boolean firstrun = getSharedPreferences(PREFERENCE, MODE_PRIVATE).getBoolean(FIRSTRUN, true);
        if (firstrun){
            //... Display instructions
            // Save the state
            findViewById(R.id.tipTextView).setVisibility(View.VISIBLE);
            getSharedPreferences(PREFERENCE, MODE_PRIVATE)
                    .edit()
                    .putBoolean(FIRSTRUN, false)
                    .apply();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("currentStation", mCurrentStation);
        outState.putString("status", String.valueOf(statusBar.getText()));
        outState.putBoolean("playing", radioPlaying);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        statusBar.setText(savedInstanceState.getString("status"));
        if (savedInstanceState.getBoolean("playing")) {
            playButton.setImageResource(R.drawable.profile_play);
        } else playButton.setImageResource(R.drawable.profile_pause);
    }

    private int nextStation(int mCurrentStation) {
        if (mCurrentStation < 0 || mCurrentStation + 1 > mStationList.size())
            return mCurrentStation;
        return mCurrentStation + 1;
    }

    private int prevStation(int mCurrentStation) {
        if (mCurrentStation <= 0) return mCurrentStation;
            return mCurrentStation - 1;
    }

    private void sendCommand(final String command, int StationToPlay) {
        String ipaddress = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("IP", DEFAULT_RADIO_IP);
        String url = "http://" + ipaddress + "/" + "?" + command + "=" + StationToPlay + "&" + "volume=" + mCurrentVolume;

        // Request a string response from the provided DEFAULT_RADIO_IP.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (command.equals("infos")) {
                            String[] arr = response.split("\n");
                            List<String> list = new ArrayList<>();
                            for (String s: arr){
                                list.add(s.substring( s.indexOf(":")+1));
                                list.set(list.size()-1, list.get(list.size()-1).trim());
                            }
                            if (radioPlaying = Integer.parseInt(list.get(4)) > 0) {
                                mCurrentVolume= Integer.parseInt(list.get(0));
                                mCurrentStation = Integer.parseInt(list.get(1));
                                statusBar.setText(mStationList.get(mCurrentStation) + " is playing");
                            };
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //statusBar.setText("That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
    }

    private BaseAdapter mListAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return mStationList.size();
        }

        @Override
        public Object getItem(int position) {
            return mStationList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item, container, false);
            }

            Station station = (Station) getItem(position);
            TextView textViewStationName = (TextView) convertView.findViewById(R.id.stationName);
            textViewStationName.setText(station.toString());

            // Because the list item contains multiple touch targets, you should not override
            // onListItemClick. Instead, set a click listener for each target individually.

            convertView.findViewById(R.id.primary_target).setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            sendCommand("play", position);
                            mCurrentStation = position;
                            playButton.setImageResource(R.drawable.profile_pause);
                            statusBar.setText(mStationList.get(position) + " is playing");
                            return false;
                        }
                    });
            return convertView;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.docs_link:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, DOCS_URI));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            case R.id.preferences_menu:
                try {
                    startActivity(new Intent(this, MyPreferencesActivity.class));
                } catch (ActivityNotFoundException ignored) {}
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop () {
        super.onStop();
        if (mRequestQueue != null) {
            mRequestQueue.stop();
        }
    }
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event){
//
//        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
//            Toast.makeText(this, "Volume Up", Toast.LENGTH_LONG).show();
//            return true;
//        }
//
//        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
//            Toast.makeText(this, "Volume Down", Toast.LENGTH_LONG).show();
//            return true;
//        }
//
//        return super.onKeyDown(keyCode, event);
//    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    mCurrentVolume += 10;
                    Toast.makeText(this, "Volume Up " + mCurrentVolume, Toast.LENGTH_SHORT).show();
                    sendCommand("", mCurrentStation);
                    return true;
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    mCurrentVolume -= 10;
                    Toast.makeText(this, "Volume Down " + mCurrentVolume, Toast.LENGTH_SHORT).show();
                    sendCommand("", mCurrentStation);
                    return true;
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public AlertDialog alarmVolume(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.volume_dialog, (ViewGroup) findViewById(R.id.volume_dialog_root_element));


        SeekBar seekbarVolume = (SeekBar)v.findViewById(R.id.dialog_seekbar);
        seekbarVolume.setMax(255);
        //seekbarVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_ALARM));
        seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurrentVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        return  builder.create();
    }

    private class DownloadStationTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String ipaddress = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("IP", MainActivity.DEFAULT_RADIO_IP);
            OkHttpClient client = new OkHttpClient();
            for (int i = 0; i < 255; i++) {
                HttpUrl url = new HttpUrl.Builder()
                            .scheme("http")
                            .host(ipaddress)
                            .addQueryParameter("list", String.valueOf(i))
                            .build();
                okhttp3.Request request =
                        new okhttp3.Request.Builder()
                                .url(url)
                                .build();
                okhttp3.Response response = null;
                try {
                    response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        mStationList.add(new Station(response.body().string(), 0));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return "Download failed";
        }

        @Override
        protected void onPostExecute(String result)        {

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public List<Station> readStationList(Context context, RequestQueue mRequestQueue) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MainActivity.FIRSTRUN, true)) {
            try {
                DownloadStationTask task = new DownloadStationTask();
                task.execute();
                mListAdapter.notifyDataSetChanged();

            } finally {

            }
        }
        else {
            // just convert local file to StationList
            File internalStorageDir = context.getFilesDir();
            File filestations = new File(internalStorageDir, "stations.csv");
            try (BufferedReader br = new BufferedReader(new FileReader(filestations))) {
                String line;
                while ((line = br.readLine()) != null) {
                    mStationList.add(new Station(line, 1));
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Station.createMockStationList();
    }
}
