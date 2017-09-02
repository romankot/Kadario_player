package com.kremor.karadio.main;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
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

public class MainActivity extends ListActivity  {
    private static final Uri DOCS_URI = Uri.parse("https://github.com/karawin/Ka-Radio");
    public static final String FIRSTRUN = "firstrun";
    public static final String STORED_STATION = "STORED_STATION";
    public static final String CURRENT_STATION = "currentStation";
    private static final String CURRENT_VOLUME = "volume";
    public static final String PLAYING = "playing";
    public static final String STATUS = "status";
    private static final String MUTE = "mMute";
    private static final String STORED_VOLUME = "storedVolume";
    private SharedPreferences mPreferences;
    public static final String DEFAULT_RADIO_IP = "192.168.1.110";
    Boolean radioPlaying = false;
    private ImageButton playButton;
    private TextView statusBar;
    public int mCurrentStation;
    List<Station> mStationList;
    private RequestQueue mRequestQueue;
    private int mCurrentVolume = 70;
    private Boolean mMute = false;
    private Menu menu;
    private DownloadStationTask task;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mRequestQueue = Volley.newRequestQueue(this);
        PreferenceManager.setDefaultValues(this,R.xml.preferences, false);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        statusBar = (TextView) findViewById(R.id.statusBar);

        readCurrentRadioStatus(savedInstanceState);
        readStationList();

        setListAdapter(mListAdapter);
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

        if (mPreferences.getBoolean(FIRSTRUN, true)) {
            //... Display instructions for first time
            findViewById(R.id.tipTextView).setVisibility(View.VISIBLE);
            mPreferences.edit().putBoolean(FIRSTRUN, false).apply();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putInt(CURRENT_STATION, mCurrentStation);
        bundle.putInt(CURRENT_VOLUME, mCurrentVolume);
        bundle.putString(STATUS, String.valueOf(statusBar.getText()));
        bundle.putBoolean(PLAYING, radioPlaying);
        bundle.putBoolean(MUTE, mMute);
        bundle.putSerializable(STORED_STATION, (Serializable) mStationList);
        bundle.putString(getString(R.string.ip), mPreferences.getString(getString(R.string.ip), ""));

        super.onSaveInstanceState(bundle);
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
        if (!mPreferences.getString(getString(R.string.ip), "").equals(savedInstanceState.getString(getString(R.string.ip)))) {
            DownloadStationTask downloadStationTask = new DownloadStationTask();
            downloadStationTask.execute();
        }
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

    private void sendCommand(final String command, int command_value) {
        String ipaddress = mPreferences.getString(getString(R.string.ip), DEFAULT_RADIO_IP);
        String url = "http://" + ipaddress + "/" + "?" + command + "=" + command_value + "&" + "volume=" + mCurrentVolume;
        if (url.contains("infos")) {
            url = url.substring(0, url.indexOf('&'));
        }

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
                                mPreferences.edit()
                                        .putInt(STORED_VOLUME, mCurrentVolume)
                                        .commit();
                                mCurrentStation = Integer.parseInt(list.get(1));
                                statusBar.setText(list.get(2) + " is playing");
                                radioPlaying = true;
                            } else {
                                radioPlaying = false;
                                statusBar.setText("Radio is silent");
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                statusBar.setText("Radio isn't reachable");
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
                    startActivity(new Intent(this, SettingActivity.class));

                } catch (ActivityNotFoundException ignored) {
                }
                break;
            case R.id.fetchStations:
                task = new DownloadStationTask();
                task.execute();
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
        if (task!= null) {
            task.cancel(true);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mCurrentVolume >= 244)
                        mCurrentVolume = 244;

                    mCurrentVolume += 10;
                    Toast.makeText(this, "Volume Up " + mCurrentVolume, Toast.LENGTH_SHORT).show();
                    sendCommand("", mCurrentStation);
                    return true;
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mCurrentVolume <= 10)
                        mCurrentVolume = 10;
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

    private class DownloadStationTask extends AsyncTask<String, Void, ArrayList<Station>> {

        private ArrayList<Station> stlist = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            statusBar.setText("Fetching station list. Shouldn't take long");
        }

        @Override
        protected ArrayList<Station> doInBackground(String... urls) {
            String ipaddress = mPreferences.getString(getString(R.string.ip), DEFAULT_RADIO_IP);
            OkHttpClient client = new OkHttpClient();
            for (int i = 0; i < 250; i++) {
                String url = String.format("http://%s/?list=%d", ipaddress, i);

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .build();
                okhttp3.Response response = null;
                if (isCancelled()) {
                    client.connectionPool().evictAll();
                    break; }
                try {
                    response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String str = response.body().string().trim();
                        if (!str.isEmpty()) {
                            Station st = new Station(str, 0);
                            stlist.add(st);
                        }
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
            mPreferences.edit()
                .putString(STORED_STATION, new Gson().toJson(mStationList))
                .commit();
            statusBar.setText("Station read success");
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            statusBar.setText("Very bad response");
        }
    }
    public void readStationList() {
        if (!mPreferences.contains(STORED_STATION)) {
            try {
                DownloadStationTask task = new DownloadStationTask();
                task.execute();
            } finally {
                mStationList = Station.createMockStationList();
            }
        } else {
            Type type = new TypeToken<ArrayList<Station>>() {
            }.getType();
            mStationList = new Gson().fromJson(mPreferences.getString(STORED_STATION, String.valueOf(Station.createMockStationList())), type);
        }
        //mStationList = Station.createMockStationList();
    }

    private void readCurrentRadioStatus(Bundle bundle) {
        if ((bundle == null) || (!bundle.containsKey(PLAYING))) {
            sendCommand("infos", 0);
        }
    }
}
