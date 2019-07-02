package unizar.master.simpleweather;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class AlarmsActivity extends AppCompatActivity {

    // Edit and add codes
    public static int ADD_CODE = 1;
    public static int EDIT_CODE = 2;

    // Database
    private myDbAdapter dbAdapter;

    // List
    private AlarmListAdapter adapter;
    private ListView list = null;
    private ArrayList<Long> ids;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms);

        dbAdapter = new myDbAdapter(this);

        // Modify toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_list);

        // Fiil list
        fillData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK){
            // If there is any change fill data again
            fillData();
        }
    }

    private void fillData(){
        Cursor c = dbAdapter.getData();

        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> conds = new ArrayList<>();
        ArrayList<Boolean> on = new ArrayList<>();
        ids = new ArrayList<>();
        while (c.moveToNext()){
            Long cid =c.getLong(c.getColumnIndex(myDbAdapter.myDbHelper.UID));
            String name =c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.NAME));
            String cond =c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.MEASURE));
            cond += " " + c.getString(c.getColumnIndex(myDbAdapter.myDbHelper.CONDITION));
            cond += " " + c.getDouble(c.getColumnIndex(myDbAdapter.myDbHelper.NUMBER));
            int enabled = c.getInt(c.getColumnIndex(myDbAdapter.myDbHelper.ENABLED));

            names.add(name);
            conds.add(cond);
            on.add((enabled == 1) ? true : false);
            ids.add(cid);
        }

        // List
        adapter = new AlarmListAdapter(this, R.layout.row_layout, names.toArray(new String[0]),
                on.toArray(new Boolean[0]), conds.toArray(new String[0]), ids.toArray(new Long[0]),
                (AlarmManager)getSystemService(Context.ALARM_SERVICE));

        list = findViewById(R.id.alarmList);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Make an intent
                Intent intent = new Intent(AlarmsActivity.this, NewAlarmActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("code", EDIT_CODE);
                bundle.putSerializable("id", ids.get(i));
                intent.putExtras(bundle);
                // Start activity for results
                startActivityForResult(intent, EDIT_CODE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.alarm_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.refresh_alarm_item:
                fillData();
                return true;
            case R.id.add_alarm_item:
                // Make an intent
                Intent intent = new Intent(AlarmsActivity.this, NewAlarmActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("code", ADD_CODE);
                intent.putExtras(bundle);
                // Start activity for results
                startActivityForResult(intent, ADD_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillData();
    }
}
