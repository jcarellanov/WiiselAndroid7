package julioarellano.wiiselandroid7.constants;

import java.util.UUID;

/**
 * Class which contains uuid
 */

public class AppConstants {
    // Parameters. JSON
    public static final String PARAM_NUMOFST = "num_of_st";
    public static final String PARAM_ACTTIME = "act_time";
    public static final String PARAM_WALKDIST = "walk_dist";
    public static final String PARAM_AMPEL = "ampel";
    public static final String PARAM_MESSAGE = "message";
    public static final String PARAM_FIRSTNAME = "first_name";
    public static final String PARAM_LASTNAME = "last_name";
    public static final String PARAM_CLPHONE = "cl_phone";
    public static final String PARAM_DATAFILE = "data_file";
    public static final String PARAM_AUTHTOKEN = "auth_token";
    public static final String PARAM_IDS = "ids";
    public static final String PARAM_HSUM = "h_sum";
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_ALERT = "alert";
    public static final String PARAM_SHOWAMPELSTATUS = "show_ampel_status";
    public static final String PARAM_SENDINGTIME = "sending_time";
    public static final String PARAM_RECEIVED = "received";

    // SMS
    public static final String CALLBACK_SMSSENT = "SMS_SENT";
    public static final String CALLBSCK_SMSDELIVERED = "SMS_DELIVERED";

    // Shared Preferences
    public static final String PREFERENCES_AMPEL = "ampel";
    public static final String PREFERENCES_NUMOFST = "num_of_st";
    public static final String PREFERENCES_ACTTIME = "act_time";
    public static final String PREFERENCES_WALKDIST = "walk_dist";
    public static final String PREFERENCES_IPADDRESSSERVER = "server_ip";
    public static final String PREFERENCES_AUTOSTART = "auto_start";
    public static final String PREFERENCES_SHOW_LOGGER = "show_logger";
    public static final String PREFERENCES_AUTOCONNECT = "auto_connect";
    public static final String PREFERENCES_AUTOAUTH = "auto_authorization";
    public static final String PREFERENCES_FALLDETECT = "fall_detection";
    public static final String PREFERENCES_ACCELEROMETERSENSITIVITY = "accelerometer_sens";
    public static final String PREFERENCES_NOMOVEMENTSENSITIVITY = "no_movement_sensitivity";
    public static final String PREFERENCES_PERMITED_ANGLE = "permited_angle";
    public static final String PREFERENCES_FIRSTNAME = "first_name";
    public static final String PREFERENCES_LASTNAME = "last_name";
    public static final String PREFERENCES_TOKEN = "auth_token";
    public static final String PREFERENCES_CLPHONE = "cl_phone";
    public static final String PREFERENCES_SHOWAMPELSTATUS = "show_ampel_status";
    public static final String PREFERENCES_CLIMGENERALMODE = "data_mode";
    public static final String PREFERENCES_USEDEFAULT = "use_default";
    public static final String PREFERENCES_DELAY_TIME = "delay_time";
    public static final String PREFERENCES_PRESS_SENSITIVITY = "pressure_sensitivity";
    public static final String PREFERENCES_AUTHENTICATED = "authenticated";
    public static final String PREFERENCES_STRING1 ="string1";
    public static final String PREFERENCES_STRING2 ="string2";


    // DEFAULT VALUE
    public static final int DEFAULT_TIMERALARM = 35000;
    public static final String DEFAULT_IPADDRESS = "81.169.151.83";
    public static final int DEFAULT_ACCELEROMETERSENSITIVITY = 25;
    public static final int DEFAULT_NOMOVEMENTSENSITIVITY = 20;
    public static final int DEFAULT_PERMITED_ANGLE = 45;
    public static final boolean DEFAULT_AUTOAUTHORIZATION = true;
    public static final boolean DEFAULT_AUTOSTART = true;
    public static final boolean DEFAULT_AUTOCONNECT = true;
    public static final boolean DEFAULT_FALLETECTION = true;
    public static final boolean DEFAULT_USAGE = true;
    public static final boolean DEFAULT_SHOW_LOGGER = false;
    public static final int DEFAULT_DELAY_TIME = 5;
    public static final int DEFAULT_PRESS_SENSITIVITY = 200;
    public static String lastMessage = "";

    // download directory
    public static final String DOWNLOAD_DIR = "WiiselDataDir";
    public static final String HISTORY_DIR = "WiiselHistoryDataDir";

    // REQUEST
    public static final int REQUEST_SETTINGSONACTIVITYRESULT = 3;
    public static final int REQUEST_ENABLE_BT = 2;
    public static final int REQUEST_SELECT_DEVICE = 123;

    // UUID
    public static final UUID UUID_LOCAL_TIME = UUID.fromString("0000ffc7-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_BATTERY_SENSOR_LOCATION = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
//    public static final ParcelUuid UUID_ACCELEROMETER_SERVICE = ParcelUuid.fromString("0000ffa0-0000-1000-8000-00805f9b34fb");
//    public static final ParcelUuid UUID_ACCELEROMETER_SENSOR_LOCATION = ParcelUuid.fromString("0000ffa3-0000-1000-8000-00805f9b34fb");
//    public static final ParcelUuid UUID_DATA_SAMPLING_SERVICE = ParcelUuid.fromString("0000ffc0-0000-1000-8000-00805f9b34fb");
//    public static final ParcelUuid UUID_STREAM_DATA_PACKET_LOCATION = ParcelUuid.fromString("0000ffc6-0000-1000-8000-00805f9b34fb");

    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_DATA_SERVICE = UUID.fromString("0000ffc0-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SET_CONNINT = UUID.fromString("0000ffc1-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_READ_CONNINT = UUID.fromString("0000ffc2-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CONTROL = UUID.fromString("0000ffc5-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DATA_STREAM = UUID.fromString("0000ffc6-0000-1000-8000-00805f9b34fb");
//    public static final UUID UUID_BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");

    // first --------------
    // Extra
//    public static final String EXTRA_STATUS_PACKETS_FIRST = "item_status_packets_first";
//    public static final String EXTRA_DATA_ARRAY_STRING_FIRST = "data_array_of_string_first";
//    public static final String EXTRA_BLUETOOTH_DEVICE_NAME_FIRST = "bluetooth_device_name_first";
//    public static final String EXTRA_DATA_INSOLE_FALL_DETECTION_FIRST = "data_insole_fall_detection_first";

    // Action
//    public static final String ACTION_ACCELEROMETER_PACKETS_FIRST = "wiisel.bluetooth.accelerometer.packets.first";
//    public static final String ACTION_STATUS_PACKETS_FIRST = "wiisel.bluetooth.status.packets.first";
//    public static final String ACTION_RECEIVE_DATA_FIRST = "wiisel.bluetooth.receivedata.first";
//    public static final String ACTION_CLOSE_UI_FIRST = "wiisel.close.ui.first";

    // second -------------
    // Extra
//    public static final String EXTRA_STATUS_PACKETS_SECOND = "item_status_packets_second";
//    public static final String EXTRA_DATA_ARRAY_STRING_SECOND = "data_array_of_string_second";
//    public static final String EXTRA_BLUETOOTH_DEVICE_NAME_SECOND = "bluetooth_device_name_second";
//    public static final String EXTRA_DATA_INSOLE_FALL_DETECTION_SECOND = "data_insole_fall_detection_second";

    // Action
//    public static final String ACTION_ACCELEROMETER_PACKETS_SECOND = "wiisel.bluetooth.accelerometer.packets.second";
//    public static final String ACTION_STATUS_PACKETS_SECOND = "wiisel.bluetooth.status.packets.second";
//    public static final String ACTION_RECEIVE_DATA_SECOND = "wiisel.bluetooth.receivedata.second";
//    public static final String ACTION_CLOSE_UI_SECOND = "wiisel.close.ui.second";

    // other ---------------
    // Extra
    public static final String EXTRA_ITEM = "item_position";
    public static final String EXTRA_TOAST = "show_toast";
    public static final String EXTRA_STATUS_TIMER = "timer_data_receive";

    // Action
//    public static final String ACTION_START_CONNECTION = "startConnection";
//    public static final String ACTION_DEVICE_SELECTED = "android.bluetooth.devicepicker.action.DEVICE_SELECTED";
//    public static final String ACTION_UPDATE_UI = "wiisel.bluetooth.update.ui";
    public static final String ACTION_UPDATE_TIMER = "wiisel.bluetooth.update.timer";
//    public static final String ACTION_CLOSE_DIALOG_ACTIVITY = "wiisel.close.dialog.activity";

    // LOGGER
    public static final String ACTION_APPEND_MESSAGE = "action_append_logger_message";

}
