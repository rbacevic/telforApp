package rs.akcelerometarapp.utils;

import android.text.format.DateFormat;

import java.util.ArrayList;

/**
 * Created by RADEEE on 30-Nov-15.
 */
public class TxtFileUtils {

    public static String createTxtPointString(String idM, String idK, String rmsX,
                                              String rmsY, String rmsZ, String rmsXYZ, String maxRmsXYZ,
                                              String dateFormatted, String speedInKmPerHour, double latitude,
                                              double longitude, String maxRmsX, String maxRmsY, String maxRmsZ,
                                              String xValueForApeak, String yValueForApeak, String zValueForApeak) {


        ArrayList<String> txtDataString = new ArrayList<>();
        txtDataString.add(idM);
        txtDataString.add(idK);
        txtDataString.add(rmsX);
        txtDataString.add(rmsY);
        txtDataString.add(rmsZ);
        txtDataString.add(rmsXYZ);
        txtDataString.add(xValueForApeak);
        txtDataString.add(yValueForApeak);
        txtDataString.add(zValueForApeak);
        txtDataString.add(maxRmsXYZ);
        txtDataString.add(String.valueOf(latitude));
        txtDataString.add(String.valueOf(longitude));
        txtDataString.add(dateFormatted);
        txtDataString.add(speedInKmPerHour);
        txtDataString.add(maxRmsX);
        txtDataString.add(maxRmsY);
        txtDataString.add(maxRmsZ);

        String formattedString = createStringFromArraySeparatedWithTilda(txtDataString, true).toString();
        return formattedString;
    }

    public static StringBuilder createStringFromArraySeparatedWithTilda(ArrayList<String> strings,
                                                                  boolean addNewLine) {

        StringBuilder txtFileData = new StringBuilder();

        for (int angle = 0; angle < strings.size(); angle++) {
            txtFileData.append(strings.get(angle));
            if (angle < strings.size() - 1) {
                txtFileData.append("~");
            }
        }
        if (addNewLine) {
            txtFileData.append(";");
        }
        return txtFileData;
    }

    public static String createTxtFileTitle(long currentTime, String measurementName,
                                            String measurementDescription, String idK, String idM) {

        ArrayList<String> titlesData = new ArrayList<>();
        titlesData.add(DateFormat.format("yyyy-MM-dd-kk-mm-ss", currentTime).toString());
        titlesData.add(measurementName);
        titlesData.add(measurementDescription);
        titlesData.add(idK);
        titlesData.add(idM);

        return TxtFileUtils.createStringFromArraySeparatedWithTilda(titlesData, false)
                .toString()
                .concat(".txt");
    }
}
