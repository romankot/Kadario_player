package com.kremor.karadio.main;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;


public class MainActivity extends ListActivity {
    private static final Uri DOCS_URI = Uri.parse("https://github.com/karawin/Ka-Radio");
    public static final String FIRSTRUN = "firstrun";
    public static final String PREFERENCE = "karadio_preferences";
    public static final String STORED_STATION = "STORED_STATION";
    public static final String CURRENT_STATION = "currentStation";
    private static final String CURRENT_VOLUME = "volume";
    public static final String PLAYING = "playing";
    public static final String STATUS = "status";
    private static final String MUTE = "mMute";
    private SharedPreferences preferences;
    public static final String DEFAULT_RADIO_IP = "http://192.168.1.110";
    Boolean radioPlaying = false;
    private ImageButton playButton;
    private TextView statusBar;
    public int mCurrentStation;
    List<Station> mStationList;
    private RequestQueue mRequestQueue;
    private int mCurrentVolume = 40;
    private Boolean mMute = false;
    private Menu menu;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mRequestQueue = Volley.newRequestQueue(this);
        PreferenceManager.setDefaultValues(this, PREFERENCE, MODE_PRIVATE, R.xml.preferences, false);
        preferences = getSharedPreferences(PREFERENCE, MODE_PRIVATE);

        readCurrentRadioStatus(savedInstanceState);
        readStationList();
        setListAdapter(mListAdapter);

        statusBar = (TextView) findViewById(R.id.statusBar);
        findViewById(R.id.prev_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int prevStation = prevStation(mCurrentStation);
                if (radioPlaying) {
                    statusBar.setText(mStationList.get(prevStation) + " is playing");
                    sendCommand("play", prevStation);
                    mCurrentStation = prevStation;
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
                    mCurrentStation = nextStation;
                }
            }
        });
        playButton = (ImageButton) findViewById(R.id.play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!radioPlaying) {
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

        if (preferences.getBoolean(FIRSTRUN, true)) {
            //... Display instructions for first time
            findViewById(R.id.tipTextView).setVisibility(View.VISIBLE);
            preferences.edit().putBoolean(FIRSTRUN, false).apply();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_STATION, mCurrentStation);
        outState.putInt(CURRENT_VOLUME, mCurrentVolume);
        outState.putString(STATUS, String.valueOf(statusBar.getText()));
        outState.putBoolean(PLAYING, radioPlaying);
        outState.putBoolean(MUTE, mMute);
        outState.putSerializable(STORED_STATION, (Serializable) mStationList);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        statusBar.setText(savedInstanceState.getString(STATUS));
        if (radioPlaying = savedInstanceState.getBoolean(PLAYING)) {
            playButton.setImageResource(R.drawable.profile_pause);
        } else
            playButton.setImageResource(R.drawable.profile_play);
        mCurrentStation = savedInstanceState.getInt(CURRENT_STATION);
        mCurrentVolume = savedInstanceState.getInt(CURRENT_VOLUME);
        mMute = savedInstanceState.getBoolean(MUTE);
        mStationList = (List<Station>) savedInstanceState.getSerializable(STORED_STATION);
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
        String ipaddress = preferences.getString("IP", DEFAULT_RADIO_IP);
        String url = "http://" + ipaddress + "/" + "?" + command + "=" + StationToPlay + "&" + "volume=" + mCurrentVolume;

        // Request a string response from the provided DEFAULT_RADIO_IP.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (command.equals("infos")) {
                            String[] arr = response.split("\n");
                            List<String> list = new ArrayList<>();
                            for (String s : arr) {
                                list.add(s.substring(s.indexOf(":") + 1));
                                list.set(list.size() - 1, list.get(list.size() - 1).trim());
                            }
                            if (radioPlaying = Integer.parseInt(list.get(4)) > 0) {
                                mCurrentVolume = Integer.parseInt(list.get(0));
                                mCurrentStation = Integer.parseInt(list.get(1));
                                statusBar.setText(mStationList.get(mCurrentStation) + " is playing");
                                radioPlaying = true;
                            } else {
                                radioPlaying = false;
                            }
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
            textViewStationName.setText(station.getName());

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
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mute_button:
                if (mMute) {
                    sendCommand("volume", mCurrentVolume);
                    mMute = false;
                    menu.getItem(0).setIcon(R.drawable.sound);
                }
                else {
                    sendCommand("volume", 0);
                    mMute = true;
                    menu.getItem(0).setIcon(R.drawable.no_sound);
                }
                return true;
            case R.id.docs_link:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, DOCS_URI));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            case R.id.preferences_menu:
                try {
                    startActivity(new Intent(this, MyPreferencesActivity.class));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            case R.id.fetchStations:
                DownloadStationTask task = new DownloadStationTask();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRequestQueue != null) {
            mRequestQueue.stop();
        }
    }

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


        SeekBar seekbarVolume = (SeekBar) v.findViewById(R.id.dialog_seekbar);
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
        return builder.create();
    }

    private class DownloadStationTask extends AsyncTask<String, Void, ArrayList<Station>> {

        private ArrayList<Station> stlist = new ArrayList<>();

        @Override
        protected ArrayList<Station> doInBackground(String... urls) {
            String ipaddress = preferences.getString("IP", MainActivity.DEFAULT_RADIO_IP);
            OkHttpClient client = new OkHttpClient();
            for (int i = 0; i < 25; i++) {
                String url = String.format("http://%s/?list=%d", ipaddress, i);

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .build();
                okhttp3.Response response = null;
                try {
                    response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String str = response.body().string().trim();
                        Station st = new Station(str, 0);
                        stlist.add(st);

                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return stlist;
        }

        @Override
        protected void onPostExecute(ArrayList<Station> list) {
            mStationList = list;
            mListAdapter.notifyDataSetChanged();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(STORED_STATION, new Gson().toJson(mStationList));
            editor.commit();
        }

    }
    public void readStationList() {
        if (!preferences.contains(STORED_STATION)) {
            try {
                DownloadStationTask task = new DownloadStationTask();
                task.execute();
            } finally {
                mStationList = Station.createMockStationList();
            }
        } else {
            Type type = new TypeToken<ArrayList<Station>>() {
            }.getType();
            mStationList = new Gson().fromJson(preferences.getString(STORED_STATION, String.valueOf(Station.createMockStationList())), type);
        }
        //mStationList = Station.createMockStationList();
    }

    private void readCurrentRadioStatus(Bundle bundle) {
        if ((bundle == null) || (!bundle.containsKey(PLAYING))) {
            sendCommand("infos", 0);
        }
    }
}
