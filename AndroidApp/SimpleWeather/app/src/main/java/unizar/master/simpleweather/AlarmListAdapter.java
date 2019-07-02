package unizar.master.simpleweather;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.TimeZone;

public class AlarmListAdapter extends ArrayAdapter {

    private Context context;
    private int resource;
    private  String[] times;
    private Boolean[] on;
    private String[] names;
    private Long[] ids;
    private AlarmManager manager;
    private TimeZone timeZone;

    private myDbAdapter dbAdapter;

    public AlarmListAdapter(@NonNull Context context, int resource, String [] times, Boolean [] on,
                            String [] names, Long[] ids, AlarmManager manager) {
        super(context, resource, times);

        this.context = context;
        this.resource = resource;
        this.times = times;
        this.on = on;
        this.names = names;
        this.ids = ids;
        this.manager = manager;

        dbAdapter = new myDbAdapter(context);
    }

    public View getView(final int position, View view, final ViewGroup group){
        View row;

        // get an inflater and inflate row
        LayoutInflater inflater = ((Activity) this.context).getLayoutInflater();

        // wire objects with row's widgets
        row = inflater.inflate(resource, null);

        // populate row's objects with data
        final TextView timeView = row.findViewById(R.id.timeText);
        final TextView nameView = row.findViewById(R.id.labelText);
        final Switch onButton = row.findViewById(R.id.switch2);

        timeView.setText(this.times[position]);
        nameView.setText(this.names[position]);

        onButton.setChecked(this.on[position]);

        onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(getContext(), "Alarma " + times[position] + " ha sido "
                        + ((onButton.isChecked()) ? "Activada" : "Desactivada"), Toast.LENGTH_SHORT).show();
                // Change data on the database
                dbAdapter.updateEnabled(ids[position], (onButton.isChecked() ? 1 : 0));;
                //editAlarm(ids[position], on[position]);
            }
        });

        return row;
    }

}
