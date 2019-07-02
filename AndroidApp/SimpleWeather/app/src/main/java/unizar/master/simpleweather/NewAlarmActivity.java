package unizar.master.simpleweather;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Calendar;
import java.util.TimeZone;

public class NewAlarmActivity extends AppCompatActivity {

    private EditText nameText = null;
    private EditText numberAlarm = null;
    private Spinner measureSpinner = null;
    private Spinner conditionSpinner = null;

    public static final String [] measures = {"Temperatura (Interior)", "Temperatura (Exterior)", "Humedad (Interior)",
                                        "Humedad (Exterior)", "CO2", "Consumo (Enchufe 1)", "Consumo (Enchufe 2)"};
    public static final String [] conditions = {"menor que", "menor o igual que", "igual que", "mayor o igual que", "mayor que"};

    private static ArrayAdapter<String> adapterMeasures = null;
    private static ArrayAdapter<String> adapterConditions = null;

    private int posMeasure = 0;
    private int posCondition = 0;

    // Database
    private long id;
    private myDbAdapter dbAdapter;

    private int code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_alarm);

        // Modify toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_new);

        nameText = findViewById(R.id.alarmName);
        numberAlarm = findViewById(R.id.alarmNum);

        dbAdapter = new myDbAdapter(this);

        measureSpinner = findViewById(R.id.alarmMeasure);
        adapterMeasures = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, measures);
        measureSpinner.setAdapter(adapterMeasures);
        measureSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                posMeasure = i;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                posMeasure = 0;
            }
        });

        conditionSpinner = findViewById(R.id.alarmCond);
        adapterConditions = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, conditions);
        conditionSpinner.setAdapter(adapterConditions);
        conditionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                posCondition = i;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                posCondition = 0;
            }
        });

        code = getIntent().getExtras().getInt("code");
        if(code == AlarmsActivity.EDIT_CODE){
            // Edit alarm, fill the fields
            id = getIntent().getExtras().getLong("id");
            fillData();
            getSupportActionBar().setTitle(R.string.title_activity_edit);
        }

    }


    private void fillData() {

        Cursor c = dbAdapter.getData(id);

        // Traverse the cursor output table
        while (c.moveToNext()) {
            String name =c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.NAME));
            String m =c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.MEASURE));
            String cond = c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.CONDITION));
            double n = c.getDouble(c.getColumnIndex(myDbAdapter.myDbHelper.NUMBER));

            nameText.setText(name);
            numberAlarm.setText(String.valueOf(n));

            for(int i = 0; i < measures.length; i++){
                if(measures[i].equals(m)){
                    posMeasure = i;
                    break;
                }
            }
            for(int i = 0; i < conditions.length; i++){
                if(conditions[i].equals(cond)){
                    posCondition = i;
                    break;
                }
            }
            measureSpinner.setSelection(posMeasure);
            conditionSpinner.setSelection(posCondition);

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_alarm_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder;
        switch (item.getItemId()) {
            case android.R.id.home:
                // Show alert dialog
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(this);
                }
                builder.setTitle("Cancelar edición alarma")
                        .setMessage("¿Estás seguro de que quieres salir sin guardar?")
                        .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent returnIntent = new Intent();
                                setResult(Activity.RESULT_CANCELED, returnIntent);
                                finish();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setCancelable(true)
                        .show();
                return true;
            case R.id.save_alarm_item:
                // Save alarm in the database
                saveAlarm();
                return true;
            case R.id.delete_alarm_item:
                // Show alert dialog
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(this);
                }
                String message = "";
                if(code == AlarmsActivity.EDIT_CODE) message = "¿Seguro que quieres borrar la alarma " + nameText.getText().toString() + "?";
                else message = "¿Seguro que quieres borrar esta alarma?";
                builder.setTitle("Eliminar alarma")
                        .setMessage(message)
                        .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if(code == AlarmsActivity.EDIT_CODE) dbAdapter.delete(id);
                                // Return to the alarm activity
                                Intent returnIntent = new Intent();
                                setResult(Activity.RESULT_OK, returnIntent);
                                finish();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setCancelable(true)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveAlarm(){
        // Get the data
        Boolean okay = true;
        double number = 0.0;
        String name = "";
        String measure = "";
        String cond = "";
        // Check empty fields
        if((numberAlarm.getText() != null && !numberAlarm.getText().toString().equals(""))
                && (nameText.getText() != null && !nameText.getText().toString().equals(""))) {
            // Get data from the fields
            number = Double.parseDouble(numberAlarm.getText().toString());
            name = nameText.getText().toString();
            measure = measures[posMeasure];
            cond = conditions[posCondition];
        }
        else{
            Toast.makeText(NewAlarmActivity.this, "Empty field", Toast.LENGTH_LONG).show();
            okay = false;
        }

        if(okay) {

            // Save changes on data base: insert or update
            if(code == AlarmsActivity.EDIT_CODE){
                dbAdapter.updateData(id,name, measure, cond, number);
            }
            else {
                dbAdapter.insertData(name, measure, cond, number, 1);
            }


            // Return to the alarm activity with result
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        }
    }
}
