package julioarellano.wiiselandroid7.manager;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.constants.AppConstants;

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


        return message;

    }


}
