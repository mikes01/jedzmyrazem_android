package mikolajgrygiel.jedzmyrazem;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import mikolajgrygiel.jedzmyrazem.enums.RestApiUrl;


public class SearchActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {
    private static final String LOG_TAG = "SearchActivity";
    private static final int GOOGLE_API_CLIENT_ID = 0;
    public AutoCompleteTextView mAutocompleteTextViewSrc;
    private AutoCompleteTextView mAutocompleteTextViewDst;
    private TextView mTextViewDate;
    private TextView mTextViewTime;
    private TextView mAttTextView;
    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;
    public LatLng start_location, finish_location;
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(51.043317, 16.803714), new LatLng(51.219122, 17.199909));
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    private Calendar calendar;
    private int year, month, day;
    private int hour, minute;

    private ArrayList<Parent> parents;
    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    protected LocationManager locationManager;
    private AppCompatActivity activity;
    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mGoogleApiClient = new GoogleApiClient.Builder(SearchActivity.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();
        mAutocompleteTextViewSrc = (AutoCompleteTextView) findViewById(R.id
                .autoCompleteTextViewSrc);
        mAutocompleteTextViewSrc.setThreshold(3);

        mAttTextView = (TextView) findViewById(R.id.att);
        mAutocompleteTextViewSrc.setOnItemClickListener(mAutocompleteClickListenerStart);

        mAutocompleteTextViewDst = (AutoCompleteTextView) findViewById(R.id
                .autoCompleteTextViewDst);
        mAutocompleteTextViewDst.setThreshold(3);

        mAttTextView = (TextView) findViewById(R.id.att);
        mAutocompleteTextViewDst.setOnItemClickListener(mAutocompleteClickListenerFinish);

        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
                BOUNDS_MOUNTAIN_VIEW, null);

        mAutocompleteTextViewSrc.setAdapter(mPlaceArrayAdapter);
        mAutocompleteTextViewDst.setAdapter(mPlaceArrayAdapter);

        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        minute = calendar.get(Calendar.MINUTE);
        hour = calendar.get(Calendar.HOUR_OF_DAY);

        mTextViewDate = (TextView) findViewById(R.id
                .textViewDate);
        showDate(year, month+1, day);

        mTextViewTime = (TextView) findViewById(R.id
                .textViewTime);
        showTime(hour, minute);

        expListView = (ExpandableListView) findViewById(R.id.expandableListView);

        parents = new ArrayList<Parent>();
        listAdapter = new MyExpandableListAdapter(this, parents);
        expListView.setAdapter(listAdapter);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        activity = this;
        start_location = null;
        gps = new GPSTracker(activity);
    }


    private AdapterView.OnItemClickListener mAutocompleteClickListenerStart
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallbackStart);
        }

        private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallbackStart
                = new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(PlaceBuffer places) {
                if (!places.getStatus().isSuccess()) {
                    Log.e(LOG_TAG, "Place query did not complete. Error: " +
                            places.getStatus().toString());
                    return;
                }
                // Selecting the first object buffer.
                final Place place = places.get(0);
                start_location = place.getLatLng();
            }
        };
    };

    private AdapterView.OnItemClickListener mAutocompleteClickListenerFinish
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallbackFinish);
        }

        private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallbackFinish
                = new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(PlaceBuffer places) {
                if (!places.getStatus().isSuccess()) {
                    Log.e(LOG_TAG, "Place query did not complete. Error: " +
                            places.getStatus().toString());
                    return;
                }
                // Selecting the first object buffer.
                final Place place = places.get(0);
                finish_location = place.getLatLng();
            }
        };
    };

    @Override
    public void onConnected(Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(LOG_TAG, "Google Places API connected.");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());

        Toast.makeText(this,
                "Google Places API connection failed with error code:" +
                        connectionResult.getErrorCode(),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mPlaceArrayAdapter.setGoogleApiClient(null);
        Log.e(LOG_TAG, "Google Places API connection suspended.");
    }

    @SuppressWarnings("deprecation")
    public void setDate(View view) {
        showDialog(999);
    }

    @SuppressWarnings("deprecation")
    public void setTime(View view) {
        showDialog(1000);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // TODO Auto-generated method stub
        if (id == 999) {
            return new DatePickerDialog(this, myDateListener, year, month, day);
        }
        else if (id == 1000) {
            return new TimePickerDialog(this, mTimeSetListener, hour, minute,
                    DateFormat.is24HourFormat(this));
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker arg0, int arg1, int arg2, int arg3) {
            // TODO Auto-generated method stub
            // arg1 = year
            // arg2 = month
            // arg3 = day
            showDate(arg1, arg2+1, arg3);
        }
    };

    private TimePickerDialog.OnTimeSetListener mTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    showTime(hourOfDay, minute);

                }
            };

    private void showDate(int year, int month, int day) {
        mTextViewDate.setText(String.format("%04d-%02d-%02d", year, month, day));
    }

    private void showTime(int hour, int minute) {
        mTextViewTime.setText(String.format("%02d:%02d", hour, minute));
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }

    public void search(View view)
    {
        String start_lat, start_lng;
        if(start_location == null || mAutocompleteTextViewSrc.getText().toString().equals("")) {
            Location loc = gps.getLocation();
            if (gps.isGPSEnabled()) {
                start_lat = Double.toString(loc.getLatitude());
                start_lng = Double.toString(loc.getLongitude());
            } else {
                gps.showSettingsAlert();
                return;
            }
        }
        else
        {
            start_lat = Double.toString(start_location.latitude);
            start_lng = Double.toString(start_location.longitude);
        }
        SearchTask st = new SearchTask();
        st.execute(mTextViewDate.getText().toString(), mTextViewTime.getText().toString(), start_lat, start_lng);
    }

    private class SearchTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String parameters = prepareStringParameters(params);
            if (parameters == null) return null;
            ServiceHandler sh = new ServiceHandler();
            String jsonStr = sh.makeServiceCall(RestApiUrl.SEARCH.getUrl(), ServiceHandler.GET, null, parameters);

            return jsonStr;
        }

        @Nullable
        private String prepareStringParameters(String[] params) {
            String parameters = "";
            parameters += "date" + "=" + params[0] + "&";
            parameters += "start_time" + "=" + params[1] + "&";
            parameters += "start_lat" + "=" + params[2] + "&";
            parameters += "start_lng" + "=" + params[3] + "&";

            if(finish_location == null)
            {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Uzupełnij cel podróży",
                                Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }
            parameters += "finish_lat" + "=" + finish_location.latitude + "&";
            parameters += "finish_lng" + "=" + finish_location.longitude;
            return parameters;
        }

        @Override
        protected void onPostExecute(String result)
        {
            parents.clear();
            ((MyExpandableListAdapter)expListView.getExpandableListAdapter()).notifyDataSetChanged();
            if (result == null)
                return;
            try {
                if (parseResponse(result)) return;
                ((MyExpandableListAdapter)expListView.getExpandableListAdapter()).notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private boolean parseResponse(String result) throws JSONException {
            JSONObject jsonObject = new JSONObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("journey");
            expListView.requestFocus();
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            if(jsonArray.length() < 1)
            {
                Toast.makeText(getApplicationContext(),
                        "Nie znaleziono żadnego przejazdu",
                        Toast.LENGTH_LONG).show();
                return true;
            }

            String startTime="", finishTime="";
            Date date;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            for(int i=0; i<jsonArray.length(); i++){
                JSONArray journey = jsonArray.getJSONArray(i);
                JSONObject start = journey.getJSONObject(0).getJSONArray("waypoints").getJSONObject(0);
                JSONObject finish = journey.getJSONObject(journey.length()-1).getJSONArray("waypoints").getJSONObject(journey.getJSONObject(journey.length()-1).getJSONArray("waypoints").length()-1);
                try {

                    date = sdf.parse(start.getString("time"));
                    startTime = new SimpleDateFormat("HH:mm", Locale.US).format(date);

                    date = sdf.parse(finish.getString("time"));
                    finishTime = new SimpleDateFormat("HH:mm", Locale.US).format(date);

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            final Parent parent = new Parent();

                parent.setStartPlace(start.getString("name"));
                parent.setStartTime(startTime);
                parent.setFinishPlace(finish.getString("name"));
                parent.setFinishTime(finishTime);
                parent.setPasses(Integer.toString(journey.length() - 1));
                parent.setChildren(new ArrayList<Child>());
                for(int j = 0; j < journey.length(); j++) {
                    final Child child = new Child();
                    start = journey.getJSONObject(j).getJSONArray("waypoints").getJSONObject(0);
                    finish =journey.getJSONObject(j).getJSONArray("waypoints").getJSONObject(journey.getJSONObject(journey.length()-1).getJSONArray("waypoints").length()-1);

                    try {

                        date = sdf.parse(start.getString("time"));
                        startTime = new SimpleDateFormat("HH:mm", Locale.US).format(date);

                        date = sdf.parse(finish.getString("time"));
                        finishTime = new SimpleDateFormat("HH:mm", Locale.US).format(date);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }


                    child.setStartPlace(start.getString("name"));
                    child.setFinishPlace(finish.getString("name"));

                    child.setStartTime(startTime);

                    child.setFinishTime(finishTime);
                    JSONObject driver = journey.getJSONObject(j).getJSONObject("user");
                    child.setDriver(driver.getString("username"));
                    child.setPhone(driver.getString("phone"));
                    child.setSpaces(journey.getJSONObject(j).getString("spaces"));
                    parent.getChildren().add(child);
                }
                parents.add(parent);
            }
            return false;
        }
    }
}