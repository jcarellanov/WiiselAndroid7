package julioarellano.wiiselandroid7.manager;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.application.WiiselApplication;
import julioarellano.wiiselandroid7.constants.AppConstants;
import julioarellano.wiiselandroid7.service.UIService;
import julioarellano.wiiselandroid7.utils.UIUtil;

public class HttpUrlConnectionManager {

    private static final String UPLOADFILE_URL = "/api/v1/data";
    private static final String LOGIN_URL = "/api/v1/tokens";
    private static final String ALARM_URL = "/api/v1/data/alert";
    private static final String DATA_UPDATE = "/api/v1/data/reload";
    private String msg = "";
    private static final int READ_TIMEOUT = 15000;
    private static final int CONNECTION_TIMEOUT = 10000;


    private static String HOST = "";

    private Context ctx;
    private static HttpUrlConnectionManager manager;

    private HttpUrlConnectionManager(Context context) {
        ctx = context;
    }

    public static HttpUrlConnectionManager getInstance(Context context) {
        if (manager == null) {
            manager = new HttpUrlConnectionManager(context);
        }
        return manager;

    }

    /**
     * @return returned host
     */
    private String getHost() {

        SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);
        HOST = "http://" + sharedPreferences.getString(AppConstants.PREFERENCES_IPADDRESSSERVER, AppConstants.DEFAULT_IPADDRESS);
        String token = sharedPreferences.getString(AppConstants.PREFERENCES_TOKEN, "");

        return token;

    }

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


    public String serverLogin(final String email, final String password) throws Exception {

        getHost();
        String message = "";
        HttpURLConnection conn;
        URL url = new URL(HOST + LOGIN_URL);
        conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        JSONObject request = new JSONObject(); // build the object with login credentials
        request.put("email", email);
        request.put("password", password);

        conn.connect();
        DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
        writer.writeBytes(request.toString()); // send login data
        writer.flush();
        writer.close();

        int responseCode = conn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {

            InputStream inputStream = conn.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            JSONObject jsonResponse = new JSONObject(result.toString());
            String token = (String) jsonResponse.get(AppConstants.PARAM_AUTHTOKEN);
            String first_name = (String) jsonResponse.get(AppConstants.PARAM_FIRSTNAME);
            String last_name = (String) jsonResponse.get(AppConstants.PARAM_LASTNAME);
            String cl_phone = (String) jsonResponse.get(AppConstants.PARAM_CLPHONE);

            SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
                    Context.MODE_PRIVATE);
            Editor editor = sharedPreferences.edit();

            editor.putString(AppConstants.PREFERENCES_TOKEN, token);
            editor.putString(AppConstants.PREFERENCES_FIRSTNAME, first_name);
            editor.putString(AppConstants.PREFERENCES_LASTNAME, last_name);
            editor.putString(AppConstants.PREFERENCES_CLPHONE, cl_phone);

            editor.commit();
            message = "Sign in successful";

        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            message = "Invalid email or password";

        } else {
            message = "Check internet connection";
        }

        conn.disconnect();
        return message;

    }


    public HttpURLConnection uploadFile(final File file) throws Exception {

        String token = getHost();
        //FileBody bin = new FileBody(file);
        FileInputStream fileInputStream = new FileInputStream(file);
        String hashSum = hashSum(file);
        long length = file.length();

        String message = "";
        HttpURLConnection conn;
        URL url = new URL(HOST + UPLOADFILE_URL);
        conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        //multipartEntityBuilder.addPart(AppConstants.PARAM_DATAFILE,bin);
        //multipartEntityBuilder.addPart(AppConstants.PARAM_AUTHTOKEN, new ContentBody(token));
/*
        reqEntity.addPart(AppConstants.PARAM_DATAFILE, bin);
        reqEntity.addPart(AppConstants.PARAM_AUTHTOKEN, new StringBody(token));
        reqEntity.addPart(AppConstants.PARAM_IDS, new StringBody(file.getName()));
        reqEntity.addPart(AppConstants.PARAM_HSUM, new StringBody(hashSum));*/

        conn.connect();
        DataOutputStream writer = new DataOutputStream(conn.getOutputStream());

        writer.writeBytes(AppConstants.PARAM_DATAFILE);
        // writer.
        writer.flush();

        //send token
        writer.writeBytes(AppConstants.PARAM_AUTHTOKEN);
        writer.writeBytes(token);
        writer.flush();
        //send filename
        writer.writeBytes(AppConstants.PARAM_IDS);
        writer.writeBytes(file.getName());
        writer.flush();
        //send hash
        writer.writeBytes(AppConstants.PARAM_HSUM);
        writer.writeBytes(hashSum);
        writer.flush();


        return conn;
    }

    public String updateUserData() throws Exception {
        String message = "";

        String token = getHost();

        if (TextUtils.isEmpty(token)) {
            Intent intent = new Intent(ctx, UIService.class);
            intent.putExtra(AppConstants.EXTRA_TOAST, ctx.getString(R.string.mess_pleasereconnect));
            ctx.startService(intent);
            return null;
        }

        JSONObject request = new JSONObject();
        request.put(AppConstants.PARAM_AUTHTOKEN, token);
        URL url = new URL(HOST + DATA_UPDATE);
        HttpURLConnection conn;
        conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        conn.connect();
        DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
        writer.writeBytes(request.toString());
        writer.flush();
        writer.close();

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = conn.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            JSONObject jsonResponse = new JSONObject(result.toString());

            WiiselApplication.showLog("d", jsonResponse.toString());

            String walkDist = jsonResponse.getString(AppConstants.PARAM_WALKDIST);
            boolean showAmpel;
            try {
                showAmpel = jsonResponse.getBoolean(AppConstants.PARAM_SHOWAMPELSTATUS);
            } catch (Exception e) {
                showAmpel = true;
            }
            String ampel = jsonResponse.getString(AppConstants.PARAM_AMPEL);
            String numOfSt = jsonResponse.getString(AppConstants.PARAM_NUMOFST);
            String activeTime = jsonResponse.getString(AppConstants.PARAM_ACTTIME);

            SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
                    Context.MODE_PRIVATE);
            Editor editor = sharedPreferences.edit();

            editor.putString(AppConstants.PREFERENCES_AMPEL, ampel);
            editor.putString(AppConstants.PREFERENCES_NUMOFST, numOfSt);
            editor.putString(AppConstants.PREFERENCES_WALKDIST, walkDist);
            editor.putString(AppConstants.PREFERENCES_ACTTIME, activeTime);
            editor.putBoolean(AppConstants.PREFERENCES_SHOWAMPELSTATUS, showAmpel);

            editor.apply();
            message = "Update successful";
        } else {


            SharedPreferences sharedPreferences = ctx.getSharedPreferences(ctx.getResources().getString(R.string.app_name),
                    Context.MODE_PRIVATE);
            Editor editor = sharedPreferences.edit();

            String emptyMark = "-";

            editor.putString(AppConstants.PREFERENCES_NUMOFST, emptyMark);
            editor.putString(AppConstants.PREFERENCES_WALKDIST, emptyMark);
            editor.putString(AppConstants.PREFERENCES_ACTTIME, emptyMark);

            editor.apply();

            InputStream inputStream = conn.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            JSONObject jsonResponse = new JSONObject(result.toString());
            message = jsonResponse.getString(AppConstants.PARAM_MESSAGE);

        }

        conn.disconnect();
        return message;
    }


    public String fallDetectionAlarmNotifViaEmail() throws Exception {
        String message = "";

        String token = getHost();

        JSONObject request = new JSONObject();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String time = sdf.format(new Date());

        request.put(AppConstants.PARAM_ALERT, time);
        request.put(AppConstants.PARAM_AUTHTOKEN, token);

        WiiselApplication.showLog("d", request.toString());
        UIUtil.appendLoggerMessage(ctx, "Send Email: " + request.toString());

        URL url = new URL(HOST + ALARM_URL);
        HttpURLConnection conn;
        conn = (HttpURLConnection) url.openConnection();

        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        conn.connect();
        DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
        writer.writeBytes(request.toString());
        writer.flush();
        writer.close();

        InputStream inputStream = conn.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            result.append(line);
        }
        JSONObject jsonResponse = new JSONObject(result.toString());
        message = jsonResponse.getString(AppConstants.PARAM_MESSAGE);

        conn.disconnect();
        return message;
    }


}
