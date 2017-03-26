package julioarellano.wiiselandroid7.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.activity.AcLogin;
import julioarellano.wiiselandroid7.constants.AppConstants;

/**
 * this receiver automatically opens app when device is turned on
 */

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);
            boolean isAutoStart = sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOSTART, AppConstants.DEFAULT_AUTOSTART);
//            boolean boolean1 = sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOCONNECT, false);
            if (/*boolean1 ||*/isAutoStart) {

                Intent startActIntent = new Intent(context, AcLogin.class);
                startActIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(startActIntent);

            }
        }

    }
}
