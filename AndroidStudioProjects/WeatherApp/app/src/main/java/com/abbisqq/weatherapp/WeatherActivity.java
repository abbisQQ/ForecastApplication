package com.abbisqq.weatherapp;


import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

    final int PERMISSION_LOCATION = 111;

    private GoogleApiClient mGoogleApiClient;
    private ArrayList<DailyWeatherReport> reports = new ArrayList<>();
    private WeatherAdapter adapter;

    private ImageView weatherIcon;
    private ImageView weatherIconMini;
    private TextView weatherDate;
    private TextView currentTemp;
    private TextView cityCountry;
    private TextView weatherDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        weatherIcon = (ImageView)findViewById(R.id.weatherIcon);
        weatherIconMini = (ImageView)findViewById(R.id.weatherIconMini);
        weatherDate = (TextView)findViewById(R.id.weatherDate);
        currentTemp = (TextView)findViewById(R.id.currentTemp);
        cityCountry = (TextView)findViewById(R.id.cityCountry);
        weatherDescription = (TextView)findViewById(R.id.weatherDescription);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this,this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycler_list);

        adapter = new WeatherAdapter(reports);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerView.setLayoutManager(layoutManager);
    }

    public void downloadWeatherData(@NonNull Location location) {
        String baseURL = "http://api.openweathermap.org/data/2.5/forecast";
        String units = "&units=metric";
        String forecastURL = "/?lat=" + location.getLatitude() + "&lon=" + location.getLongitude();
        String apiKey = "&APPID=";

        baseURL += forecastURL + units + apiKey;

        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, baseURL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                //Log.v("KAT", "Rez: " + response.toString());
                try {

                    JSONObject city = response.getJSONObject("city");
                    String cityName = city.getString("name");
                    String country = city.getString("country");

                    JSONArray list = response.getJSONArray("list");

                    for (int x = 0; x < 35; x = x + 5) {

                        JSONObject obj = list.getJSONObject(x);
                        JSONObject main = obj.getJSONObject("main");
                        Double currentTemp = main.getDouble("temp");
                        Double maxTemp = main.getDouble("temp_max");
                        Double minTemp = main.getDouble("temp_min");

                        JSONArray weatherList = obj.getJSONArray("weather");
                        JSONObject weather = weatherList.getJSONObject(0);
                        String weatherType = weather.getString("main");

                        String rawDate = obj.getString("dt_txt");

                        DailyWeatherReport report = new DailyWeatherReport(cityName,currentTemp.intValue(),maxTemp.intValue(),minTemp.intValue(),weatherType,country,rawDate);

                        reports.add(report);
                    }

                } catch (JSONException e) {
                    Log.v("KEY", "ERR: " + e.getLocalizedMessage());
                }

                updateUI();
                adapter.notifyDataSetChanged();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("KAT", "ERR: " + error.toString());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);
    }

    public void updateUI() {
        if (reports.size() > 0){
            DailyWeatherReport report = reports.get(0);

            switch (report.getWeather()) {
                case DailyWeatherReport.WEATHER_TYPE_CLEAR:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_THUNDERSTORM:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.thunder_lightning));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.thunder_lightning));
                    break;
                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.partially_cloudy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.partially_cloudy));
            } // switch

            weatherDate.setText("Today");
            currentTemp.setText(Integer.toString(report.getCurrentTemp()) + "°");
            cityCountry.setText(report.getCityName() + ", " + report.getCountry());
            weatherDescription.setText(report.getWeather());

        } // if
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
        } else {
            startLocationServices();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        if (location != null)
            downloadWeatherData(location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationServices();
                } else {
                    Toast.makeText(this, "Turn on your location", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void startLocationServices() {
        try {
            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,req,this);
        } catch (SecurityException exception) {

        }
    }

    public class WeatherViewHolder extends RecyclerView.ViewHolder {

        private ImageView weatherImg;
        private TextView weatherDescription;
        private TextView weatherDay;
        private TextView tempHigh;
        private TextView tempLow;


        public WeatherViewHolder(View itemView) {
            super(itemView);

            weatherImg = (ImageView)itemView.findViewById(R.id.weatherImg);
            weatherDescription = (TextView)itemView.findViewById(R.id.weatherDescription);
            tempHigh = (TextView)itemView.findViewById(R.id.tempHigh);
            tempLow = (TextView)itemView.findViewById(R.id.tempLow);
        }

        public void updateUI(DailyWeatherReport report) {

            switch (report.getWeather()) {
                case DailyWeatherReport.WEATHER_TYPE_CLEAR:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.cloudy_mini));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.rainy_mini));;
                    break;
                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.snow_mini));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_THUNDERSTORM:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.thunder_lightning_mini));
                    break;
                default:
                    weatherImg.setImageDrawable(getResources().getDrawable(R.drawable.partially_cloudy_mini));
            }

            weatherDescription.setText(report.getWeather());
            tempHigh.setText(Integer.toString(report.getMaxTemp())+"\u00B0");
            tempLow.setText(Integer.toString(report.getMinTemp())+"\u00B0");
        }
    }

    public class WeatherAdapter extends RecyclerView.Adapter<WeatherViewHolder> {

        private ArrayList<DailyWeatherReport> list;

        public WeatherAdapter(ArrayList<DailyWeatherReport> list) {
            this.list = list;
        }

        @Override
        public void onBindViewHolder(WeatherViewHolder holder, int position) {
            DailyWeatherReport report = list.get(position);
            holder.updateUI(report);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public WeatherViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_weather, parent, false);
            return new WeatherViewHolder(card);
        }
    }
}








