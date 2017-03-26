package julioarellano.wiiselandroid7.application;

import android.app.Application;
import android.util.Log;

import julioarellano.wiiselandroid7.R;

public class WiiselApplication extends Application {

    private static boolean DisplayLog = true;

    private static String name = "";
    private static int mode = 0;
    private static boolean isStartService = false;
    public static boolean isCalibrationMode = false;

    public static void showLog(String level, String str) {
        if (DisplayLog) {
            if (level.equals("d"))
                Log.d(name, str);
            if (level.equals("e"))
                Log.e(name, str);
            if (level.equals("i"))
                Log.i(name, str);
            if (level.equals("w"))
                Log.w(name, str);
            if (level.equals("v"))
                Log.v(name, str);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        name = getResources().getString(R.string.app_name);
    }

    public static boolean isStartService() {
        return isStartService;
    }

    public static void setStartService(boolean isStartService) {
        WiiselApplication.isStartService = isStartService;
    }

    public static int getMode() {
        return mode;
    }

    public static void setMode(int mode) {
        WiiselApplication.mode = mode;
    }

}
