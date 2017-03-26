package julioarellano.wiiselandroid7.manager;

import android.os.Environment;

import com.android.wiisel.application.WiiselApplication;
import com.android.wiisel.constants.AppConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileDataManager {

    private FileWriter fileWriter;

    /**
     * 
     * @param FILENAME_SD
     *            - path to data file
     * 
     * @return true if file opened correctly
     */
    public boolean openDataFile(String FILENAME_SD) {

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        File sdPath = Environment.getExternalStorageDirectory();
        sdPath = new File(sdPath.getPath() + "/" + AppConstants.DOWNLOAD_DIR);
        sdPath.mkdirs();
        File sdFile = new File(sdPath, FILENAME_SD);

        try {
            fileWriter = new FileWriter(sdFile, true);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

//    public boolean deleteDataFail() {
//        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            return false;
//        }
//        File sdPath = Environment.getExternalStorageDirectory();
//        sdPath = new File(sdPath.getPath() + "/" + AppConstants.DOWNLOAD_DIR);
//
//        if (sdPath != null) {
//            try {
//                for (File f : sdPath.listFiles()) {
//                    if (f.isFile()) {
//
//                        moveFile(f, sdPath.getPath() + "/" + AppConstants.HISTORY_DIR);
//
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return false;
//    }
//
//    private void moveFile(File f, String string) throws Exception {
//        File file = new File(string);
//        if (!file.exists()) {
//            file.mkdir();
//        }
//
//        InputStream in = new FileInputStream(f.getAbsolutePath());
//        OutputStream out = new FileOutputStream(file.getAbsoluteFile() + "/" + f.getName());
//
//        byte[] buf = new byte[1024];
//        int len;
//        while ((len = in.read(buf)) > 0) {
//            out.write(buf, 0, len);
//        }
//        in.close();
//        out.close();
//
//
//        f.delete();
//    }

    public boolean closeDataReadFife() {

        try {
            if (fileWriter != null) {
                fileWriter.close();
                fileWriter = null;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean isOpenFileDataStream() {
        if (fileWriter == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean writeData(String str) {

        try {
            if (fileWriter != null) {
                fileWriter.append(str);
                fileWriter.flush();
                return true;
            } else {
                WiiselApplication.showLog("e", "BufferedWriter == null, call openDataFile");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
