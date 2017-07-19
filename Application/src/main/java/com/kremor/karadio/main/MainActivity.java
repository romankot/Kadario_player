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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.List;

/**
 * This activity demonstrates the <b>borderless button</b> styling from the Holo visual language.
 * The most interesting bits in this sample are in the layout files (res/layout/).
 * <p>
 * See <a href="http://developer.android.com/design/building-blocks/buttons.html#borderless">
 * borderless buttons</a> at the Android Design guide for a discussion of this visual style.
 */
public class MainActivity extends ListActivity  {
    private static final Uri DOCS_URI = Uri.parse(
            "https://github.com/karawin/Ka-Radio");
    public static final String FIRSTRUN = "firstrun";
    public static final String PREFERENCE = "PREFERENCE";

    public static final String DEFAULT_RADIO_IP = "http://192.168.1.110";
    String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()
    String station = "1";
    String volume = "50";
    boolean pauseIsPressed = true;
    private ImageButton playButton;
    private TextView statusBar;
    public int mCurrentStation;
    List<Station> stationList;
    private RequestQueue mRequestQueue;
    private int mCurrentVolume = 80;
    private boolean isPlaying = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        stationList = Station.readStationList(this);
        setListAdapter(mListAdapter);
        mRequestQueue = Volley.newRequestQueue(this);
        statusBar = (TextView) findViewById(R.id.statusBar);

        findViewById(R.id.prev_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int prevStation = Station.getPrevStation(mCurrentStation);
                if (!pauseIsPressed) {
                    statusBar.setText(stationList.get(prevStation) + " is playing");
                    sendCommand("play", prevStation, mCurrentVolume);
                } else {
                    statusBar.setText(stationList.get(prevStation) + " is current station");
                }
                mCurrentStation = prevStation;
            }
        });
        findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int nextStation = mCurrentStation+1;
                if (pauseIsPressed) {
                    statusBar.setText(stationList.get(nextStation) + " is current station");
                } else {
                    sendCommand("play", nextStation, mCurrentVolume);
                    statusBar.setText(stationList.get(nextStation) + " is playing");
                }
                mCurrentStation = nextStation;
            }
        });
        playButton = (ImageButton) findViewById(R.id.play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            //@RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(View view) {
                if (pauseIsPressed){
                    //send get reqeust to start
                    sendCommand("play", mCurrentStation, mCurrentVolume);
                    statusBar.setText(stationList.get(mCurrentStation) + " is playing");
                    playButton.setImageResource(R.drawable.profile_pause);
                    pauseIsPressed = false;
                } else {
                    // pausing play
                    sendCommand("stop", 0, mCurrentVolume);
                    statusBar.setText(stationList.get(mCurrentStation) + " has stop");
                    playButton.setImageResource(R.drawable.profile_play);
                    pauseIsPressed = true;
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

    private void sendCommand(String command, int StationToPlay, int volume) {
        String ipaddress = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("IP", DEFAULT_RADIO_IP);
        String url = "http://" + ipaddress + "/" + "?" + command + "=" + StationToPlay + "&" + "volume=" + volume;

        // Request a string response from the provided DEFAULT_RADIO_IP.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //statusBar.setText("Response is: "+ response.substring(0,2));
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
            return stationList.size();
        }

        @Override
        public Object getItem(int position) {
            return stationList.get(position);
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

            Station currentItem = (Station) getItem(position);
            TextView textViewStationName = (TextView) convertView.findViewById(R.id.stationName);
            textViewStationName.setText(currentItem.toString());

            // Because the list item contains multiple touch targets, you should not override
            // onListItemClick. Instead, set a click listener for each target individually.

            convertView.findViewById(R.id.primary_target).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mCurrentStation = (int) getItemId(position);
                            statusBar.setText(stationList.get(mCurrentStation) + " is current station");
                        }
                    });

//            convertView.findViewById(R.id.secondary_action).setOnClickListener(
//                    new View.OnClickListener() {
//                        @Override
//                        public void onClick(View view) {
//                            Toast.makeText(MainActivity.this,
//                                    getText(R.string.touched_secondary_message) + String.valueOf(getItemId(position)),
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    });
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
                    mCurrentVolume += 15;
                    Toast.makeText(this, "Volume Up " + mCurrentVolume, Toast.LENGTH_SHORT).show();
                    sendCommand("", mCurrentStation, mCurrentVolume);
                    return true;
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    mCurrentVolume -= 15;
                    Toast.makeText(this, "Volume Down " + mCurrentVolume, Toast.LENGTH_SHORT).show();
                    sendCommand("", mCurrentStation, mCurrentVolume);
                    return true;
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }
}
