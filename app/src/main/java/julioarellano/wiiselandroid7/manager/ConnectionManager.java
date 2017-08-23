package julioarellano.wiiselandroid7.manager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.SmsManager;
import android.text.TextUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.application.WiiselApplication;
import julioarellano.wiiselandroid7.constants.AppConstants;
import julioarellano.wiiselandroid7.service.UIService;
import julioarellano.wiiselandroid7.utils.UIUtil;

//import static java.net.Proxy.Type.HTTP;

/**
 * This manager has methods for work with server. Singleton
 */

//Http methods deprecated in favor of HttpUrlConnectionManager

public class ConnectionManager {

    private static final String UPLOADFILE_URL = "/api/v1/data";
    private static final String LOGIN_URL = "/api/v1/tokens";
    private static final String ALARM_URL = "/api/v1/data/alert";
    private static final String DATA_UPDATE = "/api/v1/data/reload";
    private String msg = "";


    private static String HOST = "";

    private Context ctx;
    private static ConnectionManager manager;

    private ConnectionManager(Context context) {
        ctx = context;
    }

    public static ConnectionManager getInstance(Context context) {
        if (manager == null) {
            manager = new ConnectionManager(context);
        }
        return manager;
    }



    /**
     * 
     * @param user
     *            - user login
     * @param password
     *            - user password
     * 
     * @return - http response with json request
     * @throws Exception
     */
    public HttpResponse serverLogin(final String user, final String password) throws Exception {

        JSONObject request = new JSONObject();
        request.put(AppConstants.PARAM_EMAIL, user);
        request.put(AppConstants.PARAM_PASSWORD, password);

        getHost();

        WiiselApplication.showLog("d", request.toString() + "  " + HOST + LOGIN_URL);
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(HOST + LOGIN_URL);

        StringEntity stringEntity = new StringEntity(request.toString(), HTTP.UTF_8);
        stringEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(stringEntity);

        HttpResponse response = client.execute(post);

        JSONObject jsonObject = fromHttpToJson(response);
        WiiselApplication.showLog("d", "Json response" + jsonObject.toString());

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

            String token = (String) jsonObject.get(AppConstants.PARAM_AUTHTOKEN);
            String first_name = (String) jsonObject.get(AppConstants.PARAM_FIRSTNAME);
            String last_name = (String) jsonObject.get(AppConstants.PARAM_LASTNAME);
            String cl_phone = (String) jsonObject.get(AppConstants.PARAM_CLPHONE);

            SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);
            Editor editor = sharedPreferences.edit();

            editor.putString(AppConstants.PREFERENCES_TOKEN, token);
            editor.putString(AppConstants.PREFERENCES_FIRSTNAME, first_name);
            editor.putString(AppConstants.PREFERENCES_LASTNAME, last_name);
            editor.putString(AppConstants.PREFERENCES_CLPHONE, cl_phone);

            editor.commit();
        }
        /*else{
            msg = jsonObject.getString(AppConstants.PARAM_MESSAGE);
            SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
                    Context.MODE_PRIVATE);
            Editor editor = sharedPreferences.edit();
            editor.putString(msg,AppConstants.lastMessage);

            return response;
        }*/
        return response;
    }



    /**
     * 
     * @param file
     *            - path to file
     * @return json responce
     * @throws Exception
     */
    public HttpResponse uploadFile(final File file) throws Exception {

        String token = getHost();

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(HOST + UPLOADFILE_URL);
        FileBody bin = new FileBody(file);
        String hashSum = hashSum(file);

        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart(AppConstants.PARAM_DATAFILE, bin);
        reqEntity.addPart(AppConstants.PARAM_AUTHTOKEN, new StringBody(token));
        reqEntity.addPart(AppConstants.PARAM_IDS, new StringBody(file.getName()));
        reqEntity.addPart(AppConstants.PARAM_HSUM, new StringBody(hashSum));
        httppost.setEntity(reqEntity);
        WiiselApplication.showLog("d", token + " " + file.getName() + " " + hashSum + " " + HOST + UPLOADFILE_URL);

        HttpResponse response = httpclient.execute(httppost);

        return response;

    }

    /**
     * Updates user data
     * 
     * @return data in json
     * @throws Exception
     */
    public HttpResponse updateUserData() {
        try {
            String token = getHost();

            if (TextUtils.isEmpty(token)) {
                Intent intent = new Intent(ctx, UIService.class);
                intent.putExtra(AppConstants.EXTRA_TOAST, ctx.getString(R.string.mess_pleasereconnect));
                ctx.startService(intent);
                return null;
            }

            JSONObject request = new JSONObject();
            request.put(AppConstants.PARAM_AUTHTOKEN, token);

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(HOST + DATA_UPDATE);

            WiiselApplication.showLog("d", request.toString() + " : " + HOST + DATA_UPDATE);

            StringEntity stringEntity = new StringEntity(request.toString(), HTTP.UTF_8);
            stringEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

            post.setEntity(stringEntity);
            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

                JSONObject jsonObject = fromHttpToJson(response);
                WiiselApplication.showLog("d", jsonObject.toString());

                String walkDist = jsonObject.getString(AppConstants.PARAM_WALKDIST);
                boolean showAmpel;
                try {
                    showAmpel = jsonObject.getBoolean(AppConstants.PARAM_SHOWAMPELSTATUS);
                } catch (Exception e) {
                    showAmpel = true;
                }
                String ampel = jsonObject.getString(AppConstants.PARAM_AMPEL);
                String numOfSt = jsonObject.getString(AppConstants.PARAM_NUMOFST);
                String activeTime = jsonObject.getString(AppConstants.PARAM_ACTTIME);

                SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
                    Context.MODE_PRIVATE);
                Editor editor = sharedPreferences.edit();

                editor.putString(AppConstants.PREFERENCES_AMPEL, ampel);
                editor.putString(AppConstants.PREFERENCES_NUMOFST, numOfSt);
                editor.putString(AppConstants.PREFERENCES_WALKDIST, walkDist);
                editor.putString(AppConstants.PREFERENCES_ACTTIME, activeTime);
                editor.putBoolean(AppConstants.PREFERENCES_SHOWAMPELSTATUS, showAmpel);

                editor.commit();
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();

            SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);
            Editor editor = sharedPreferences.edit();

            String emptymark = "-";

            editor.putString(AppConstants.PREFERENCES_NUMOFST, emptymark);
            editor.putString(AppConstants.PREFERENCES_WALKDIST, emptymark);
            editor.putString(AppConstants.PREFERENCES_ACTTIME, emptymark);

            editor.commit();

            return null;
        }
    }

    /**
     * 
     * When fall is detected then we should call doctor view email
     * 
     * @return
     * @throws Exception
     */
    public HttpResponse fallDetectionAlarmNotifViaEmail() throws Exception {

        String token = getHost();

        JSONObject request = new JSONObject();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String time = sdf.format(new Date());

        request.put(AppConstants.PARAM_ALERT, time);
        request.put(AppConstants.PARAM_AUTHTOKEN, token);

        WiiselApplication.showLog("d", request.toString());
        UIUtil.appendLoggerMessage(ctx, "Send Email: " + request.toString());

        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(HOST + ALARM_URL);

        StringEntity stringEntity = new StringEntity(request.toString(), HTTP.UTF_8);
        stringEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        post.setEntity(stringEntity);
        HttpResponse response = client.execute(post);

        return response;
    }

    /**
     * When fall is detected then we should call doctor view email
     * 
     * @throws Exception
     */
    public void fallDetectionAlarmNotifViaSMS() throws Exception {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
            Context.MODE_PRIVATE);

        String firstName = sharedPreferences.getString(AppConstants.PREFERENCES_FIRSTNAME, "");
        String lastName = sharedPreferences.getString(AppConstants.PREFERENCES_LASTNAME, "");
        String clPhone = sharedPreferences.getString(AppConstants.PREFERENCES_CLPHONE, "");

        WiiselApplication.showLog("d", "Name: " + firstName + " " + lastName + " Numbers: " + clPhone);
        UIUtil.appendLoggerMessage(ctx, "Send sms: " + "Name: " + firstName + " " + lastName + " Numbers: " + clPhone);

        SmsManager smsManager = SmsManager.getDefault();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String time = sdf.format(new Date());
        String message = String.format(ctx.getString(R.string.mess_patientfall), firstName + " " + lastName, time);

        String[] arrayPhoneNumber = clPhone.split(",");
        for (int i = 0; i < arrayPhoneNumber.length; i++) {

            if (!TextUtils.isEmpty(arrayPhoneNumber[i])) {
                WiiselApplication.showLog("d", "Message: " + message + " , Number: " + arrayPhoneNumber[i]);
                UIUtil.appendLoggerMessage(ctx, "SMS body: " + "Message: " + message + " , Number: " + arrayPhoneNumber[i]);

                PendingIntent sentPendingIntent = PendingIntent.getBroadcast(ctx, 0, new Intent(AppConstants.CALLBACK_SMSSENT), 0);
                PendingIntent deliveredPendingIntent = PendingIntent
                    .getBroadcast(ctx, 0, new Intent(AppConstants.CALLBSCK_SMSDELIVERED), 0);

                registerCallBackReceiver();
                try {
                    smsManager.sendTextMessage(arrayPhoneNumber[i], null, message, sentPendingIntent, deliveredPendingIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Listener for sms update
     */
    private void registerCallBackReceiver() {
        ctx.registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                case Activity.RESULT_OK:
                    WiiselApplication.showLog("d", "SMS sent");
                    UIUtil.appendLoggerMessage(ctx, "SMS sent");
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    WiiselApplication.showLog("d", "Generic failure");
                    UIUtil.appendLoggerMessage(ctx, "Generic failure");
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    WiiselApplication.showLog("d", "Generic failure");
                    UIUtil.appendLoggerMessage(ctx, "Generic failure");
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    WiiselApplication.showLog("d", "Null PDU");
                    UIUtil.appendLoggerMessage(ctx, "Null PDU");
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    WiiselApplication.showLog("d", "Radio off");
                    UIUtil.appendLoggerMessage(ctx, "Radio off");
                    break;
                }
            }
        }, new IntentFilter(AppConstants.CALLBACK_SMSSENT));

        ctx.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                case Activity.RESULT_OK:
                    WiiselApplication.showLog("d", "SMS delivered");
                    UIUtil.appendLoggerMessage(ctx, "SMS delivered");
                    break;
                case Activity.RESULT_CANCELED:
                    WiiselApplication.showLog("d", "SMS not delivered");
                    UIUtil.appendLoggerMessage(ctx, "SMS not delivered");
                    break;
                }
            }
        }, new IntentFilter(AppConstants.CALLBSCK_SMSDELIVERED));

    }

    /**
     * 
     * @param file
     *            - new File(path);
     * @return - hash sum of file
     */
    private String hashSum(File file) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] dataBytes = new byte[1024];
            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            fis.close(); // acreo added
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (md != null) {
            byte[] mdbytes = md.digest();
            StringBuffer sb = new StringBuffer("");
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * 
     * @return returned host
     */
    private String getHost() {

        SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
            Context.MODE_PRIVATE);
        HOST = "http://" + sharedPreferences.getString(AppConstants.PREFERENCES_IPADDRESSSERVER, AppConstants.DEFAULT_IPADDRESS);
        String token = sharedPreferences.getString(AppConstants.PREFERENCES_TOKEN, "");

        return token;

    }

    /**
     * Converter
     * 
     * @param response
     *            - responce from server
     * @return - Json object
     * @throws Exception
     */
    public JSONObject fromHttpToJson(HttpResponse response) throws Exception {
        HttpEntity entity = response.getEntity();
        InputStream content = entity.getContent();
        byte[] buffer = new byte[1024];
        StringBuilder jsonResponse = new StringBuilder();
        while (content.read(buffer) > 0) {
            jsonResponse.append(new String(buffer));
        }
        JSONObject jsonObject = new JSONObject(jsonResponse.toString());
        return jsonObject;
    }

}
