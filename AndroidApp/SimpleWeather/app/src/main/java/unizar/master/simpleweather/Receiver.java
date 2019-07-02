package unizar.master.simpleweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class Receiver extends BroadcastReceiver {

    myDbAdapter dbAdapter;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle answerBundle = intent.getExtras();
        dbAdapter = new myDbAdapter(context);

        int userAnswer = answerBundle.getInt("userAnswer");
        Long id = answerBundle.getLong("id");
        if(userAnswer == 1){
            dbAdapter.updateEnabled(id, 0);
            Toast.makeText(context, "Alarma desactivada", Toast.LENGTH_SHORT).show();
            SendDataService.mNotifyMgr.cancel(id.intValue());
        }
    }
}