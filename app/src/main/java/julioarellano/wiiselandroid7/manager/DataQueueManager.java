package julioarellano.wiiselandroid7.manager;

import android.os.AsyncTask;

import com.android.wiisel.application.WiiselApplication;

public class DataQueueManager {

    private FileDataManager fileDataManager;

    public DataQueueManager() {
        fileDataManager = new FileDataManager();
    }

    // public static DataQueueManager getInstance() {
    // if (dataQueueManager == null) {
    // dataQueueManager = new DataQueueManager();
    // }
    // return dataQueueManager;
    // }

    /**
     * 
     * @param data
     *            - string for writing to file
     * @param nameFile
     *            - name of data file
     */
    public void writeDataToFile(final String data, final String nameFile) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                if (fileDataManager.openDataFile(nameFile)) {
                    if (fileDataManager.writeData(data)) {
                        if (fileDataManager.closeDataReadFife()) {
                            WiiselApplication.showLog("i", "write was successful");
                        }
                    } else {
                        WiiselApplication.showLog("e", "writeData() return null");
                    }
                } else {
                    WiiselApplication.showLog("e", "openDataFile() return null");
                }
            }
        });
        thread.start();
    }

    public void writeDataToFileAsync(String[] params) {
        DataWriter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    }

    AsyncTask<String, Void, Void> DataWriter = new AsyncTask<String, Void, Void>() {

        @Override
        protected Void doInBackground(String... params) {
            if (fileDataManager.openDataFile(params[0])) {
                if (fileDataManager.writeData(params[1])) {
                    if (fileDataManager.closeDataReadFife()) {
                        WiiselApplication.showLog("i", "write was successful");
                    }
                } else {
                    WiiselApplication.showLog("e", "writeData() return null");
                }
            } else {
                WiiselApplication.showLog("e", "openDataFile() return null");
            }

            return null;
        }
    };

}
