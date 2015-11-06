package rs.akcelerometarapp.utils;

import android.content.Context;
import android.os.Environment;
import android.text.format.DateFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import rs.akcelerometarapp.R;

/**
 * Created by RADEEE on 24-Oct-15.
 */
public class FileUtils {

    private Context mContext;

    public FileUtils(Context mContext) {
        this.mContext = mContext;
    }

    public String getDirectoryPath() {

        // Kreiranje direktorijuma na SD kartici ako ne postoji
        String appName = mContext.getResources().getString(R.string.app_name);
        String dirPath = Environment.getExternalStorageDirectory()
                .toString() + "/" + appName;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dirPath;
    }

    public FileOutputStream createCSVFile(long cuurentTime) {

        try {
            String fileName = DateFormat
                    .format("yyyy-MM-dd-kk-mm-ss",
                            cuurentTime).toString()
                    .concat(".csv");
            File file = new File(getDirectoryPath(), fileName);
            if (file.createNewFile()) {
                return new FileOutputStream(
                        file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void appendResultsToCsvFile(FileOutputStream fileOutputStream,
                                       ConcurrentLinkedQueue<float[]> filterHistory,
                                       ConcurrentLinkedQueue<float[]> rawHistory) {

        // Kreiranje datoteke u CSV formatu
        StringBuilder csvData = new StringBuilder();

        Iterator<float[]> iterator = filterHistory.iterator();
        Iterator<float[]> iteratorRaw = rawHistory.iterator();

        //Iteracija kroz strukturu i formatiranje izvestaja sa zarezom i novim redom
        while (iterator.hasNext() && iteratorRaw.hasNext()) {
            float[] values = iterator.next();
            float[] valuesRaw = iteratorRaw.next();
            for (int angle = 0; angle < 4; angle++) {
                csvData.append(String.valueOf(values[angle]));
                if (angle < 4) {
                    csvData.append(",");
                }
            }
            for (int angle = 0; angle < 3; angle++) {
                csvData.append(String.valueOf(valuesRaw[angle]));
                if (angle < 3) {
                    csvData.append(",");
                }
            }
            csvData.append("\n");
        }

        try {
            // Unos podataka
            fileOutputStream.write(csvData.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finishEditingCsvFile(FileOutputStream fileOutputStream) {
        try {
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileOutputStream createKMLFile(long cuurentTime, String kmlHeader) {

        try {
            String fileName = DateFormat
                    .format("yyyy-MM-dd-kk-mm-ss",
                            cuurentTime).toString()
                    .concat(".kml");
            File file = new File(getDirectoryPath(), fileName);
            if (file.createNewFile()) {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(kmlHeader.getBytes());
                return fileOutputStream;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void appendResultsToKmlFile(FileOutputStream fileOutputStream, String kmlPointContent) {

        try {
            // Unos podataka
            fileOutputStream.write(kmlPointContent.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finishEditingKmlFile(FileOutputStream fileOutputStream) {
        try {
            fileOutputStream.write(KmlUtils.getKMLEndString().getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
