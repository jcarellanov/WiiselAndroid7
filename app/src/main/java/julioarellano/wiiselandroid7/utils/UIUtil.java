package julioarellano.wiiselandroid7.utils;

import android.content.Context;
import android.content.Intent;

import com.android.wiisel.constants.AppConstants;

public class UIUtil {
    public static void appendLoggerMessage(Context context, final String text) {
        Intent intent = new Intent();
        intent.setAction(AppConstants.ACTION_APPEND_MESSAGE);
        intent.putExtra(AppConstants.ACTION_APPEND_MESSAGE, text);
        context.sendBroadcast(intent);
    }
}
