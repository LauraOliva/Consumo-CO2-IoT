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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.TimeZone;

public class DetailAlarmActivity extends AppCompatActivity {

    private TextView nameTextView = null;
    private TextView condTextView = null;
    private Switch onButton = null;

    //Database
    private myDbAdapter dbAdapter;

    // Add or edit code
    private int code;

    // Alarm
    private long id;

    // Activity result
    private int activityResult = Activity.RESULT_CANCELED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_alarm);

        dbAdapter = new myDbAdapter(this);

        // Fill the labels
        nameTextView = findViewById(R.id.nameDet);
        condTextView = findViewById(R.id.condDet);

        // Manage the buttons
        onButton = findViewById(R.id.switchDet);

        // Get data from DB
        Intent intent = getIntent();
        id = intent.getExtras().getLong("id");
        code = intent.getExtras().getInt("code");
        fillData();

        // Modify toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(nameTextView.getText().toString().toUpperCase());

        // Manage on button to enable or disabled alarms
        onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityResult = Activity.RESULT_OK;

                Toast.makeText(DetailAlarmActivity.this, "Alarma " + nameTextView.getText().toString() + " ha sido "
                        + ((onButton.isChecked()) ? "Activada" : "Desactivada"), Toast.LENGTH_SHORT).show();
                // Change data on the database
                dbAdapter.updateEnabled(id, (onButton.isChecked() ? 1 : 0));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        activityResult = resultCode;
        if(resultCode == Activity.RESULT_OK){
            // If there is any change fill data again
            fillData();
        }
    }

    private void fillData() {
        Cursor c = dbAdapter.getData(id);

        // Traverse the cursor output table
        while (c.moveToNext()) {

            int cid =c.getInt(c.getColumnIndex(myDbAdapter.myDbHelper.UID));
            String name =c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.NAME));
            String cond =c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.MEASURE));
            cond += " " + c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.CONDITION));
            cond += " " + c.getDouble(c.getColumnIndex(myDbAdapter.myDbHelper.NUMBER));
            int enabled = c.getInt(c.getColumnIndex(myDbAdapter.myDbHelper.ENABLED));

            nameTextView.setText(name);
            condTextView.setText(cond);

            if(enabled == 1) onButton.setChecked(true);
            else onButton.setChecked(false);

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go back
                Intent returnIntent = new Intent();
                setResult(activityResult, returnIntent);
                finish();
                return true;
            case R.id.delete_alarm_item:
                // Show alert dialog
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(this);
                }
                builder.setTitle("Eliminar alarma")
                        .setMessage("¿Seguro que quieres borrar la alarma " + nameTextView.getText().toString() + "?")
                        .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dbAdapter.delete(id);
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
            case R.id.edit_alarm_item:
                // Make an intent
                Intent intent = new Intent(DetailAlarmActivity.this, NewAlarmActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("code", code);
                bundle.putSerializable("id", id);
                intent.putExtras(bundle);
                // Start activity for results
                startActivityForResult(intent, code);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
