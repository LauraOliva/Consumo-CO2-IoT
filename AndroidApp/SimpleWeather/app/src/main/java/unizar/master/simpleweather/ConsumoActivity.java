package unizar.master.simpleweather;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class ConsumoActivity extends AppCompatActivity {

    private static String [] enchufes = {"Enchufe 1", "Enchufe 2"};
    private static String [] ips = {"http://192.168.43.20", "http://192.168.43.24"};

    private static Spinner spinner = null;
    private static ArrayAdapter<String> adapter = null;
    private static String currentURL = ips[0];
    private static int pos = 0;

    private static TextView voltText, currText, powerText, enInstText, enTodText;
    private static Switch button;
    private static Button voltHelp, currHelp, powerHelp, enHelp;
    private static TextView formulaVolt, formulaCurr, formulaPower, formulaEn;
    private static EditText socketName;

    public static String energyToday, volt = null, curr, power;
    public static double [] energy = {0.0, 0.0};
    public static String [] energyInst = {null, null};
    private static boolean [] on = {false, false};

    private static SharedPreferences.Editor editor;
    private static SharedPreferences sp;

    Handler mHandler = new Handler();

    private static SwipeRefreshLayout pullToRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumo);
        getSupportActionBar().hide();

        sp = getApplicationContext().getSharedPreferences("Sockets", MODE_PRIVATE);
        for(int i = 0; i < enchufes.length; i++) {
            enchufes[i] = sp.getString("socket_"+i, "Enchufe " + (i+1));
        }

        pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new ConsumoActivity.SocketDataTask().execute(0,1);
                new ConsumoActivity.SocketDataTask().execute(1,1);
                adapter.notifyDataSetChanged();
                pullToRefresh.setRefreshing(false);
            }
        });

        voltText = findViewById(R.id.textVoltaje);
        currText = findViewById(R.id.textCorriente);
        powerText = findViewById(R.id.textPotencia);
        enInstText = findViewById(R.id.textEnergia);
        enTodText = findViewById(R.id.textEnergiaT);

        if(volt == null){
            voltText.setText("0 V");
            currText.setText("0.0 A");
            powerText.setText("0 W");
            enInstText.setText("0.0 kWh");
            enTodText.setText("0.0 kWh");
        }
        else{
            voltText.setText(volt);
            currText.setText(curr);
            powerText.setText(power);
            enInstText.setText(energyInst[pos]);
            enTodText.setText(energyToday);
        }

        voltHelp = findViewById(R.id.voltageHelp);
        currHelp = findViewById(R.id.currentHelp);
        powerHelp = findViewById(R.id.powerHelp);
        enHelp = findViewById(R.id.energyHelp);
        formulaVolt = findViewById(R.id.formulaVolt);
        formulaCurr = findViewById(R.id.formulaCurr);
        formulaPower = findViewById(R.id.formulaPower);
        formulaEn = findViewById(R.id.formulaEnergy);

        formulaVolt.setVisibility(View.GONE);
        formulaCurr.setVisibility(View.GONE);
        formulaPower.setVisibility(View.GONE);
        formulaEn.setVisibility(View.GONE);

        voltHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (formulaVolt.getVisibility() == View.VISIBLE) formulaVolt.setVisibility(View.GONE);
                else formulaVolt.setVisibility(View.VISIBLE);
            }
        });

        currHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (formulaCurr.getVisibility() == View.VISIBLE) formulaCurr.setVisibility(View.GONE);
                else formulaCurr.setVisibility(View.VISIBLE);
            }
        });

        powerHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (formulaPower.getVisibility() == View.VISIBLE) formulaPower.setVisibility(View.GONE);
                else formulaPower.setVisibility(View.VISIBLE);
            }
        });

        enHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (formulaEn.getVisibility() == View.VISIBLE) formulaEn.setVisibility(View.GONE);
                else formulaEn.setVisibility(View.VISIBLE);
            }
        });

        socketName = findViewById(R.id.nameText);
        socketName.setText(enchufes[pos]);

        socketName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                enchufes[pos] = socketName.getText().toString();
                editor = sp.edit();
                editor.putString("socket_"+pos, enchufes[pos]);
                editor.apply();
                adapter.notifyDataSetChanged();
            }
        });

        button = findViewById(R.id.buttonOnOff);
        button.setChecked(on[pos]);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SocketChangeStateTask().execute();
                new SocketDataTask().execute(pos,0);

                voltText.setText(volt);
                currText.setText(curr);
                powerText.setText(power);
                enInstText.setText(energyInst[pos]);
                enTodText.setText(energyToday);

            }
        });

        //getWindow().requestFeature(Window.FEATURE_PROGRESS);
        //getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

        spinner = findViewById(R.id.spinner);

        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, enchufes);

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                String url = ips[i];
                pos = i;
                currentURL = url;
                new SocketDataTask().execute(pos,0);

                voltText.setText(volt);
                currText.setText(curr);
                powerText.setText(power);
                enInstText.setText(energyInst[pos]);
                enTodText.setText(energyToday);
                button.setChecked(on[pos]);
                socketName.setText(enchufes[pos]);

                // http://192.168.1.190/?m=1&o=1 -> cambiar de estado y obtener datos
                // http://192.168.1.190/?m=1 -> Obtener datos
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                currentURL = ips[0];
                pos = 0;
            }

        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (true) {
                    try {
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // Write your code here to update the UI.
                                new SocketDataTask().execute(pos,0);

                                voltText.setText(volt);
                                currText.setText(curr);
                                powerText.setText(power);
                                enInstText.setText(energyInst[pos]);
                                enTodText.setText(energyToday);
                                button.setChecked(on[pos]);
                            }
                        });
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        Log.e("ERROR", e.getMessage(), e);
                    }
                }
            }
        }).start();
    }

    public static class SocketDataTask extends AsyncTask<Integer, Void, String> {
        protected void onPreExecute() {
            //t2.setText("Fetching Data from Server.Please Wait...");
        }
        protected String doInBackground(Integer... params) {
            int id = params[0];
            int update = params[1];
            double total = 0.0;
            Locale.setDefault(new Locale("en", "US"));
            //for(int i = 0; i < ips.length; i++) {
            try {
                URL url = new URL(ips[id] + "/?m=1");
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
                    int idx1, idx2;
                    if(stringBuilder.toString() != null) {
                        idx1 = stringBuilder.toString().indexOf("{t}{s}Voltage{m}") + (new String("{t}{s}Voltage{m}")).length();
                        idx2 = stringBuilder.toString().indexOf("{e}{s}Current{m}");
                        volt = stringBuilder.toString().substring(idx1, idx2);

                        idx1 = idx2 + (new String("{e}{s}Current{m}")).length();
                        idx2 = stringBuilder.toString().indexOf("{e}{s}Power{m}");
                        curr = stringBuilder.toString().substring(idx1, idx2);

                        idx1 = idx2 + (new String("{e}{s}Power{m}")).length();
                        idx2 = stringBuilder.toString().indexOf("{e}{s}Apparent Power{m}");
                        power = stringBuilder.toString().substring(idx1, idx2);

                        idx1 = stringBuilder.toString().indexOf("Energy Today");
                        idx2 = stringBuilder.toString().indexOf(" kWh{e}{s}Energy Yesterday");
                        energyToday = stringBuilder.toString().substring(idx1 + 15, idx2) + " kWh";

                        // Energia total actual
                        idx1 = stringBuilder.toString().indexOf("{s}Energy Total{m}") + (new String("{s}Energy Total{m}")).length();
                        idx2 = stringBuilder.toString().indexOf(" kWh{e}</table>");
                        double totalEnergy = Double.parseDouble(stringBuilder.toString().substring(idx1, idx2));

                        // Energia total ultima consulta
                        System.out.println("-----------------> " + totalEnergy);
                        System.out.println("-----------------> " + energy[id]);
                        double e = totalEnergy - energy[id];
                        if( e < 0){
                            if(update == 1) energy[id] = totalEnergy;
                            //e = totalEnergy;
                            e = 0.0;
                        }
                        else{
                            if(update == 1) energy[id] += e;
                        }
                        if(update == 1) {
                            editor = MainActivity.sp.edit();
                            editor.putFloat("energy_" + id, (float) energy[id]);
                            editor.apply();
                        }

                        energyInst[id] = String.format("%.3f", e) + " kWh";

                        if(stringBuilder.toString().contains("ON")) on[id] = true;
                        else on[id] = false;
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                //}
            }
            return String.format("%.3f", total);

        }
        protected void onPostExecute(String response) {
            if(response == null) {
                //Toast.makeText(MainActivity.this, "There was an error", Toast.LENGTH_SHORT).show();
                System.out.println("Respuesta nula");
                return;
            }

            /*energyToday = response;
            System.out.println("-----------> " + energyToday);

            System.out.println("Respuesta " + response);*/
            /*try {
                JSONObject channel = (JSONObject) new JSONTokener(response).nextValue();
                System.out.println(channel.toString());
                JSONObject data = (JSONObject) channel.getJSONArray("feeds").get(0);
                double co2 = data.getDouble(THINGSPEAK_FIELD1);
                double hum = data.getDouble(THINGSPEAK_FIELD2);
                double temp = data.getDouble(THINGSPEAK_FIELD3);
                co2Int = String.format("%.2f", co2);
                tempInt = String.format("%.2f", temp);
                humInt =String.format("%.2f",  hum);
                if(textCO2 != null && textHum != null && textTemp != null) {
                    textCO2.setText(co2Int + " ppm");
                    textHum.setText(humInt + " %");
                    textTemp.setText(tempInt + " ºC");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }*/
        }
    }

    public static class SocketChangeStateTask extends AsyncTask<Void, Void, String> {
        protected void onPreExecute() {}
        protected String doInBackground(Void... urls) {
            try {
                URL url = new URL(currentURL + "/?m=1&o=1");
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
                } finally {
                    urlConnection.disconnect();
                }
            } catch(Exception e) {
                e.printStackTrace();
                //}
            }
            return "OK";

        }
        protected void onPostExecute(String response) {}
    }
}
