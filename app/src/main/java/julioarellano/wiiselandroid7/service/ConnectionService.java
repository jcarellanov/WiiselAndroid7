package julioarellano.wiiselandroid7.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.constants.AppConstants;

public class ConnectionService extends Service {

    long time;
    private boolean condition = true;
    private long currentTimeMillis;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {

        currentTimeMillis = System.currentTimeMillis();

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (condition) {
                    Intent intent = new Intent(AppConstants.ACTION_UPDATE_TIMER);
                    intent.putExtra(AppConstants.EXTRA_STATUS_TIMER, String.valueOf(System.currentTimeMillis() - currentTimeMillis));
                    sendBroadcast(intent);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        thread.start();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotificationForeground(1, "WIISEL", getString(R.string.mess_receiving_act));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        condition = false;
        super.onDestroy();
    }

    // create notification for startforeground service
    private void showNotificationForeground(int msgID, String title, String text) {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title).setContentText(text);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();

        mNotificationManager.notify(msgID, notification);
        startForeground(msgID, notification);

    }
}
