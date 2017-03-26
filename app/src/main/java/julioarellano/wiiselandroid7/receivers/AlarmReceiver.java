package julioarellano.wiiselandroid7.receivers;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.android.wiisel.R;
import com.android.wiisel.application.WiiselApplication;
import com.android.wiisel.constants.AppConstants;
import com.android.wiisel.manager.ConnectionManager;
import com.android.wiisel.service.BluetoothLeServiceLeft;
import com.android.wiisel.service.BluetoothLeServiceRight;
import com.android.wiisel.service.UIService;

import org.apache.http.HttpResponse;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

//import com.android.wiisel.manager.BleThreadsDataManager;

/**
 * This class sends data-file to the server
 */

public class AlarmReceiver extends BroadcastReceiver {

    Handler handler = new Handler();
    Context ctx;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;

//        if (BleThreadsDataManager.getInstance().getBluetoothGattServiceFirst() == null
//                && BleThreadsDataManager.getInstance().getBluetoothGattServiceSecond() == null) {
        if (BluetoothLeServiceRight.getBtGatt() == null && BluetoothLeServiceLeft.getBtGatt() == null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean isConnected = false;
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            try {
                isConnected = activeNetwork.isConnectedOrConnecting();
            } catch (Exception e) {
            }
            if (isConnected) {
                sendDataFileToServer();
            } else {
                Intent intent2 = new Intent(ctx, UIService.class);
                intent2.putExtra(AppConstants.EXTRA_TOAST, ctx.getString(R.string.mess_nointernet));
                ctx.startService(intent2);

            }

        }
    }

    private void sendDataFileToServer() {
        File sdPath = Environment.getExternalStorageDirectory();
        sdPath = new File(sdPath.getPath() + "/" + AppConstants.DOWNLOAD_DIR);
        if (sdPath != null) {
            try {
                for (final File f : sdPath.listFiles()) {
                    if (f.isFile()) {
                        Thread thread = new Thread(new Runnable() {

                            private JSONObject jsonObject;
                            private HttpResponse response;
                            private int statusCode;

                            @Override
                            public void run() {
                                try {
                                    response = ConnectionManager.getInstance(ctx).uploadFile(f);
                                    InputStream content = response.getEntity().getContent();
                                    byte[] buffer = new byte[1024];
                                    StringBuilder jsonResponse = new StringBuilder();
                                    while (content.read(buffer) > 0) {
                                        jsonResponse.append(new String(buffer));
                                    }
                                    jsonObject = new JSONObject(jsonResponse.toString());
                                    WiiselApplication.showLog("d", jsonObject.toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (response != null) {
                                    statusCode = response.getStatusLine().getStatusCode();
                                }
                                if (statusCode == 200) {
                                    try {

                                        String sendingTime = (String) jsonObject.getString(AppConstants.PARAM_SENDINGTIME);
                                        String received = (String) jsonObject.getString(AppConstants.PARAM_RECEIVED);
                                        final String message = (String) jsonObject.getString(AppConstants.PARAM_MESSAGE);
                                        String ampelStatus = (String) jsonObject.getString(AppConstants.PARAM_AMPEL);

                                        String sendTimeHour = sendingTime.substring(0, 2);
                                        String sendTimeMinutes = sendingTime.substring(3, 5);

                                        Integer intSendTimeHour = Integer.valueOf(sendTimeHour);
                                        Integer intSendTimeMinutes = Integer.valueOf(sendTimeMinutes);

                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH", Locale.US);
                                        String format = simpleDateFormat.format(new Date());
                                        Integer intCurrentTime = Integer.valueOf(format);
                                        if (intSendTimeHour > intCurrentTime) {

                                            Calendar cal = Calendar.getInstance();

                                            cal.set(Calendar.HOUR_OF_DAY, intSendTimeHour);
                                            cal.set(Calendar.MINUTE, intSendTimeMinutes);

                                            AlarmManager alarmService = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                                            Intent intent = new Intent(ctx, AlarmReceiver.class);
                                            PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);
                                            alarmService.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);

                                        } else {
                                            Calendar cal = Calendar.getInstance();

                                            cal.add(Calendar.DAY_OF_YEAR, 1);
                                            cal.set(Calendar.HOUR_OF_DAY, intSendTimeHour);
                                            cal.set(Calendar.MINUTE, intSendTimeMinutes);

                                            AlarmManager alarmService = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                                            Intent intent = new Intent(ctx, AlarmReceiver.class);
                                            PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);
                                            alarmService.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);

                                        }

                                        String numofst = (String) jsonObject.getString(AppConstants.PARAM_NUMOFST);
                                        String walkdist = (String) jsonObject.getString(AppConstants.PARAM_WALKDIST);
                                        String acttime = (String) jsonObject.getString(AppConstants.PARAM_ACTTIME);

                                        SharedPreferences sharedPreferences = ctx.getSharedPreferences(
                                            ctx.getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
                                        Editor editor = sharedPreferences.edit();
                                        editor.putString(AppConstants.PREFERENCES_AMPEL, ampelStatus);
                                        editor.putString(AppConstants.PREFERENCES_NUMOFST, numofst);
                                        editor.putString(AppConstants.PREFERENCES_WALKDIST, walkdist);
                                        editor.putString(AppConstants.PREFERENCES_ACTTIME, acttime);
                                        editor.commit();

                                        if (received.equals("true")) {

                                            moveFile(f, Environment.getExternalStorageDirectory().getPath() + "/"
                                                + AppConstants.DOWNLOAD_DIR + "/" + AppConstants.HISTORY_DIR);

                                            f.delete();
                                        }
                                        handler.post(new Runnable() {

                                            @Override
                                            public void run() {
                                                NotificationCompat.Builder builder = new Builder(ctx).setSmallIcon(R.drawable.ic_launcher)
                                                    .setContentText(message);
                                                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                                builder.setSound(alarmSound);
                                                NotificationManager mNotifyMgr = (NotificationManager) ctx
                                                    .getSystemService(Service.NOTIFICATION_SERVICE);
                                                mNotifyMgr.notify(99911, builder.build());
//                                                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    try {
                                        final String message = (String) jsonObject.get(AppConstants.PARAM_MESSAGE);
                                        handler.post(new Runnable() {

                                            @Override
                                            public void run() {
                                                NotificationCompat.Builder builder = new Builder(ctx).setSmallIcon(R.drawable.ic_launcher)
                                                    .setContentText(message);
                                                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                                builder.setSound(alarmSound);
                                                NotificationManager mNotifyMgr = (NotificationManager) ctx
                                                    .getSystemService(Service.NOTIFICATION_SERVICE);
                                                mNotifyMgr.notify(99911, builder.build());
//                                                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                        });
                        thread.start();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void moveFile(File f, String string) throws Exception {
        File file = new File(string);
        if (!file.exists()) {
            file.mkdir();
        }

        InputStream in = new FileInputStream(f.getAbsolutePath());
        OutputStream out = new FileOutputStream(file.getAbsoluteFile() + "/" + f.getName());

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();

        f.delete();
    }
}
