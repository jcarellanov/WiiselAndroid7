package julioarellano.wiiselandroid7.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class StatusInternetConectionBroatcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent paramIntent) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isConnected = false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        try {
            isConnected = activeNetwork.isConnectedOrConnecting();
        } catch (Exception e) {
        }
        if (isConnected) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            context.sendBroadcast(intent);
        }
    }

}
