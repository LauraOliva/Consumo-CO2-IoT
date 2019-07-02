package unizar.master.simpleweather;

import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.Locale;

public class MedidasActivity extends AppCompatActivity {


    private static SwipeRefreshLayout pullToRefresh;

    private static TextView textCO2, textHum, textTemp;
    static public String co2Int = null, tempInt = null, humInt = null;
    //private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medidas);
        getSupportActionBar().hide();
        textCO2=(TextView)findViewById(R.id.textCO2);
        textHum=(TextView)findViewById(R.id.textHumedad);
        textTemp=(TextView)findViewById(R.id.textTemp);

        if(co2Int == null || humInt == null || tempInt == null) {
            textCO2.setText("");
            textHum.setText("");
            textTemp.setText("");
        }
        else {
            textCO2.setText(co2Int + " ppm");
            textHum.setText(humInt + " %");
            textTemp.setText(tempInt + " ºC");
        }

        pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new MedidasActivity.FetchIntMeasuresTask().execute();
                pullToRefresh.setRefreshing(false);
            }
        });

    }


    public static class FetchIntMeasuresTask extends AsyncTask<Void, Void, String> {
        protected void onPreExecute() {
            //t2.setText("Fetching Data from Server.Please Wait...");
        }
        protected String doInBackground(Void... urls) {
            try {
                URL url = new URL("http://192.168.43.42:5000/getData");
                System.out.println(url.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }
        protected void onPostExecute(String response) {
            if(response == null) {
                //Toast.makeText(MainActivity.this, "There was an error", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                System.out.println(response);
                JSONArray response_json = new JSONArray(response);
                response_json = (JSONArray) response_json.get(0);

                /*
                JSONObject channel = (JSONObject) new JSONTokener(response).nextValue();
                System.out.println(channel.toString());
                JSONObject data = new JSONObject(response);
                double co2 = data.getDouble(THINGSPEAK_FIELD1);
                double hum = data.getDouble(THINGSPEAK_FIELD2);
                double temp = data.getDouble(THINGSPEAK_FIELD3);*/
                if(response_json.get(0) != null) {
                    Locale.setDefault(new Locale("en", "US"));
                    tempInt = String.format("%.2f", response_json.get(0));
                    humInt = String.format("%.2f", response_json.get(1));
                    co2Int = String.format("%.2f", response_json.get(2));
                    if (textCO2 != null && textHum != null && textTemp != null) {
                        textCO2.setText(co2Int + " ppm");
                        textHum.setText(humInt + " %");
                        textTemp.setText(tempInt + " ºC");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IllegalFormatConversionException e){
                e.printStackTrace();
            }
        }
    }
}
