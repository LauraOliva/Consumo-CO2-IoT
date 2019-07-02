package unizar.master.simpleweather;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class WeatherFragment extends Fragment {
    private static Typeface weatherFont;
    private static TextView cityField;
    private static TextView updatedField;
    private static TextView detailsField;
    private static TextView currentTemperatureField;
    private static TextView weatherIcon;

    static public String tempExt = null, humExt = null;

    private static Handler handler;

    private static AppLocationService appLocationService;

    private static SharedPreferences sp;
    private static SharedPreferences.Editor editor;

    private static Activity activity;

    private static SwipeRefreshLayout pullToRefresh;

    public WeatherFragment(){
        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_weather, container, false);
        cityField = (TextView)rootView.findViewById(R.id.city_field);
        updatedField = (TextView)rootView.findViewById(R.id.updated_field);
        detailsField = (TextView)rootView.findViewById(R.id.details_field);
        currentTemperatureField = (TextView)rootView.findViewById(R.id.current_temperature_field);
        weatherIcon = (TextView)rootView.findViewById(R.id.weather_icon);
        weatherIcon.setTypeface(weatherFont);

        pullToRefresh = rootView.findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                WeatherFragment.getWeather(getContext());
                pullToRefresh.setRefreshing(false);
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherFont = Typeface.createFromAsset(getActivity().getAssets(), "fonts/weather.ttf");
        activity = getActivity();
        getWeather(getContext());

    }

    public static boolean getWeather(Context context){
        appLocationService = new AppLocationService( context);
        Location location = appLocationService
                .getLocation(LocationManager.GPS_PROVIDER);

        //you can hard-code the lat & long if you have issues with getting it
        //remove the below if-condition and use the following couple of lines
        //double latitude = 37.422005;
        //double longitude = -122.084095

        // Shared preferences
        sp = context.getSharedPreferences("Location", MODE_PRIVATE);
        boolean response = true;
        double latitude, longitude;
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            editor = sp.edit();
            editor.putFloat("latitude", (float) latitude);
            editor.putFloat("longitude", (float) longitude);
            editor.apply();
        }
        else{
            latitude = sp.getFloat("latitude", 41.6560593f);
            longitude = sp.getFloat("longitude", -0.8891f);
            response = false;
        }
        LocationAddress locationAddress = new LocationAddress();
        System.out.println("-----------------" + latitude + ", " + longitude);
        locationAddress.getAddressFromLocation(latitude, longitude, context, new GeocoderHandler());
        return response;
    }

    private static void updateWeatherData(final String city, final String lat, final String lon){
        new Thread(){
            public void run(){
                while(true) {
                    try {
                        final JSONObject json = RemoteFetch.getJSON(activity, lat, lon);
                        if (json == null) {
                            handler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(activity,
                                            activity.getString(R.string.place_not_found),
                                            Toast.LENGTH_LONG).show();
                                    System.out.println();
                                }
                            });
                        } else {
                            handler.post(new Runnable() {
                                public void run() {
                                    renderWeather(json);
                                }
                            });
                        }
                        sleep(300000);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private static void renderWeather(JSONObject json){
        Locale.setDefault(new Locale("en", "US"));
        try {
            cityField.setText(json.getString("name").toUpperCase(Locale.US) +
                    ", " +
                    json.getJSONObject("sys").getString("country"));

            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");
            detailsField.setText(
                    details.getString("description").toUpperCase(Locale.US) +
                            "\n" + "Humidity: " + main.getString("humidity") + "%" +
                            "\n" + "Pressure: " + main.getString("pressure") + " hPa");
            humExt =  main.getString("humidity");

            currentTemperatureField.setText(
                    String.format("%.2f", main.getDouble("temp"))+ " ℃");
            tempExt = String.format("%.2f", main.getDouble("temp"));

            DateFormat df = DateFormat.getDateTimeInstance();
            String updatedOn = df.format(new Date(json.getLong("dt")*1000));
            updatedField.setText("Last update: " + updatedOn);

            setWeatherIcon(details.getInt("id"),
                    json.getJSONObject("sys").getLong("sunrise") * 1000,
                    json.getJSONObject("sys").getLong("sunset") * 1000);

        }catch(Exception e){
            Log.e("SimpleWeather", "One or more fields not found in the JSON data");
        }
    }

    private static void setWeatherIcon(int actualId, long sunrise, long sunset){
        int id = actualId / 100;
        String icon = "";
        if(actualId == 800){
            long currentTime = new Date().getTime();
            if(currentTime>=sunrise && currentTime<sunset) {
                icon = activity.getString(R.string.weather_sunny);
            } else {
                icon = activity.getString(R.string.weather_clear_night);
            }
        } else {
            switch(id) {
                case 2 : icon = activity.getString(R.string.weather_thunder);
                    break;
                case 3 : icon = activity.getString(R.string.weather_drizzle);
                    break;
                case 7 : icon = activity.getString(R.string.weather_foggy);
                    break;
                case 8 : icon = activity.getString(R.string.weather_cloudy);
                    break;
                case 6 : icon = activity.getString(R.string.weather_snowy);
                    break;
                case 5 : icon = activity.getString(R.string.weather_rainy);
                    break;
            }
        }
        weatherIcon.setText(icon);
    }


    private static class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress, lat, lon;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    lat = bundle.getString("latitude");
                    lon = bundle.getString("longitude");
                    break;
                default:
                    locationAddress = null;
                    lat = null;
                    lon = null;
            }
            Toast.makeText(activity,
                    locationAddress,
                    Toast.LENGTH_LONG).show();
            updateWeatherData(locationAddress, lat, lon);
        }
    }
}
