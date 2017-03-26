package julioarellano.wiiselandroid7.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import julioarellano.wiiselandroid7.constants.AppConstants;

public class UIService extends Service {

    @Override
    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String msg = (String) intent.getExtras().get(AppConstants.EXTRA_TOAST);
        Toast.makeText(UIService.this, msg, Toast.LENGTH_SHORT).show();
        stopSelf(startId);
        return super.onStartCommand(intent, flags, startId);
    }

}
