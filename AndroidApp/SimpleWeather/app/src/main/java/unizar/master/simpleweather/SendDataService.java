package unizar.master.simpleweather;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.renderscript.RenderScript;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class SendDataService extends Service {

    public static final long NOTIFY_INTERVAL = 5*60*1000; // 5 min
    protected Handler mHandler = new Handler();
    protected Handler handler = new Handler();
    private Timer mTimer = null;

   // private final LocalBinder mBinder = new LocalBinder();
    private Looper mServiceLooper;

    // Base de datos
    private myDbAdapter dbAdapter;

    // Notificaciones
    private NotificationCompat.Builder mBuilder;
    public static NotificationManager mNotifyMgr;

    /*public class LocalBinder extends Binder {
        public SendDataService getService() {
            return SendDataService .this;
        }
    }*/

    @Override
    public IBinder onBind(Intent intent) {
        return null;//mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(mTimer != null){
            mTimer.cancel();
        } else{
            mTimer = new Timer();
        }
        dbAdapter = new myDbAdapter(this);

        mNotifyMgr = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getApplicationContext());

        mTimer.scheduleAtFixedRate(new SendDataTimerTask(), 0, NOTIFY_INTERVAL);
        /*
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                THREAD_PRIORITY_BACKGROUND);
        thread.start();

        dbAdapter = new myDbAdapter(this);

        mNotifyMgr = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getApplicationContext());

        mServiceLooper = thread.getLooper();
        handler = new Handler(mServiceLooper);
        */
    }

    class SendDataTimerTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Write your code here to update the UI.
                    Toast.makeText(getApplicationContext(), "Send data",
                            Toast.LENGTH_LONG).show();
                    WeatherFragment.getWeather(getApplicationContext());
                    new MedidasActivity.FetchIntMeasuresTask().execute();
                    new ConsumoActivity.SocketDataTask().execute(0,1);
                    new ConsumoActivity.SocketDataTask().execute(1,1);
                    new SendDataTask().execute();
                }
            });
        }
    }

    /*@Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (true) {
                    try {
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                // Write your code here to update the UI.
                                WeatherFragment.getWeather(getApplicationContext());
                                new MedidasActivity.FetchIntMeasuresTask().execute();
                                new ConsumoActivity.SocketDataTask().execute(0,1);
                                new ConsumoActivity.SocketDataTask().execute(1,1);
                                new SendDataTask().execute();
                            }
                        });
                        Thread.sleep(300000);
                    } catch (Exception e) {
                        Log.e("ERROR", e.getMessage(), e);
                    }
                }
            }
        }).start();
        return android.app.Service.START_STICKY;
    }*/

    private double checkMeasure(String measure){
        Locale.setDefault(new Locale("en", "US"));
        if (measure.equals(NewAlarmActivity.measures[0])) return Double.parseDouble(MedidasActivity.tempInt);
        else if (measure.equals(NewAlarmActivity.measures[1])) return Double.parseDouble(WeatherFragment.tempExt);
        else if (measure.equals(NewAlarmActivity.measures[2])) return Double.parseDouble(MedidasActivity.humInt);
        else if (measure.equals(NewAlarmActivity.measures[3])) return Double.parseDouble(WeatherFragment.humExt);
        else if (measure.equals(NewAlarmActivity.measures[4])) return Double.parseDouble(MedidasActivity.co2Int);
        else if (measure.equals(NewAlarmActivity.measures[5])) return ConsumoActivity.energy[0];
        else if (measure.equals(NewAlarmActivity.measures[6])) return ConsumoActivity.energy[1];
        return 0.0;
    }

    private boolean checkCondition(double m, String cond, double n){
        if(cond.equals(NewAlarmActivity.conditions[0])) return m < n;
        else if(cond.equals(NewAlarmActivity.conditions[1])) return m <= n;
        else if(cond.equals(NewAlarmActivity.conditions[2])) return m == n;
        else if(cond.equals(NewAlarmActivity.conditions[3])) return m > n;
        else if(cond.equals(NewAlarmActivity.conditions[4])) return m >= n;
        return false;
    }

    private class SendDataTask extends AsyncTask<String, Void, String> {
        protected void onPreExecute(){}
        protected String doInBackground(String... data) {

            OkHttpClient client = new OkHttpClient();
            new MultipartBody.Builder();

            if(WeatherFragment.tempExt == null || MedidasActivity.tempInt == null
                    || WeatherFragment.humExt == null || MedidasActivity.humInt == null
                    || MedidasActivity.co2Int == null ){
                return "ERROR";
            }

            // Check alarms
            Cursor c = dbAdapter.getData();

            while (c.moveToNext()){
                Long cid =c.getLong(c.getColumnIndex(myDbAdapter.myDbHelper.UID));
                String name =c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.NAME));
                String m =c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.MEASURE));
                String cond = c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.CONDITION));
                double n = c.getDouble(c.getColumnIndex(myDbAdapter.myDbHelper.NUMBER));
                int enabled = c.getInt(c.getColumnIndex(myDbAdapter.myDbHelper.ENABLED));
                if(enabled == 1 && MainActivity.not_alarm_id != cid){
                    double measure_data = checkMeasure(m);
                    if(checkCondition(measure_data, cond, n)){
                        // Notificar al usuario
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);;
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("id", cid);
                        if (m.equals(NewAlarmActivity.measures[0]) || m.equals(NewAlarmActivity.measures[2])
                                || m.equals(NewAlarmActivity.measures[4])){
                            bundle.putSerializable("tab", 1);
                        }
                        else if (m.equals(NewAlarmActivity.measures[1]) || m.equals(NewAlarmActivity.measures[3])){
                            bundle.putSerializable("tab", 0);
                        }
                        else if (m.equals(NewAlarmActivity.measures[5]) || m.equals(NewAlarmActivity.measures[6])) {
                            bundle.putSerializable("tab", 2);
                        }
                        intent.putExtras(bundle);


//Yes intent
                        Intent desactivateReceive = new Intent(getApplicationContext(), Receiver.class);
                        Bundle desactivateBundle = new Bundle();
                        desactivateBundle.putInt("userAnswer", 1);//This is the value I want to pass
                        desactivateBundle.putLong("id", cid);//This is the value I want to pass
                        desactivateReceive.putExtras(desactivateBundle);
                        desactivateReceive.setAction("Custom");
                        PendingIntent pendingIntentYes = PendingIntent.getBroadcast(getApplicationContext(), 0, desactivateReceive, PendingIntent.FLAG_CANCEL_CURRENT);

                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        mBuilder =new NotificationCompat.Builder(getApplicationContext())
                                .setContentIntent(pendingIntent)
                                .setSmallIcon(R.drawable.co2)
                                .setContentTitle(name)
                                .setContentText(m + " " + cond + " " + String.valueOf(n))
                                .setVibrate(new long[] {100, 250, 100, 500})
                                .setAutoCancel(true)
                                .addAction(R.drawable.ic_not, "Desactivar", pendingIntentYes);

                        mNotifyMgr.notify(cid.intValue(), mBuilder.build());
                    }
                }
            }
            String sendEn0 = ConsumoActivity.energyInst[0], sendEn1 = ConsumoActivity.energyInst[1];
            if(ConsumoActivity.energyInst[0] == null) sendEn0 = "0 kWh";
            if(ConsumoActivity.energyInst[1] == null) sendEn1 = "0 kWh";

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("tempExt",WeatherFragment.tempExt)
                    .addFormDataPart("humExt",WeatherFragment.humExt)
                    .addFormDataPart("co2", MedidasActivity.co2Int)
                    .addFormDataPart("tempInt", MedidasActivity.tempInt)
                    .addFormDataPart("humInt", MedidasActivity.humInt)
                    .addFormDataPart("consumo1", sendEn0.substring(0, sendEn0.length()-4))
                    .addFormDataPart("consumo2", sendEn1.substring(0, sendEn1.length()-4))
                    .build();


            Request request = new Request.Builder()
                    .url("http://192.168.43.42:5000/postData")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                Headers responseHeaders = response.headers();
                for (int i = 0; i < responseHeaders.size(); i++) {
                    System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                }

                System.out.println(response.body().string());
            }catch (IOException e){
                e.printStackTrace();
            }

            return "OK";
        }

        protected void onPostExecute(String page) {}
    }

}
