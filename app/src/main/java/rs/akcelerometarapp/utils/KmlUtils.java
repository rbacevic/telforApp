package rs.akcelerometarapp.utils;

/**
 * Created by RADEEE on 10-Oct-15.
 */
public class KmlUtils {

    public static String getKMLStartString() {

        return  "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<kml xmlns='http://www.opengis.net/kml/2.2'>\n" +
                "<Document>\n" +
                "<name>TEST DRIVE</name> \n"+
                "<description>DCR</description> \n"+
                "<Style id='highlightPlacemark'> \n"+
                "<IconStyle> \n"+
                "<Icon> \n"+
                "<href>http://www.google.com/mapfiles/ms/micons/earthquake.png</href> \n"+
                "</Icon> \n"+
                "</IconStyle> \n"+
                "</Style> \n"+
                "<Style id='normalPlacemark'> \n"+
                "<IconStyle> \n"+
                "<Icon> \n"+
                "<href>http://maps.google.com/mapfiles/kml/paddle/blu-blank.png</href> \n"+
                "</Icon> \n"+
                "</IconStyle> \n"+
                "</Style> \n";
    }

    public static String createKMLPointString(String pointStyle, int pointIndex, double rmsX,
                                              double rmsY, double rmsZ, double rmsXYZ, double maxRmsXYZ,
                                              String dateFormatted, double speedInKmPerHour, double latitude,
                                              double longitude, double altitude, double maxRmsX, double maxRmsY, double maxRmsZ) {
        return "\t<Placemark>\n" +
                "<styleUrl>#" + pointStyle + "</styleUrl>"+
                "\t<name>" + pointIndex + "</name>\n" +
                "\t<description>"+ "<![CDATA["+
                "Speed [km/h]: " + speedInKmPerHour + "\n"+
                "Altitude: " + altitude + "\n" +
                "RMS X: " + rmsX + "\n" +
                "RMS Y: " + rmsY + "\n" +
                "RMS Z: " + rmsZ + "\n" +
                "Max RMS X: " + maxRmsX + "\n" +
                "Max RMS Y: " + maxRmsY + "\n" +
                "Max RMS Z: " + maxRmsZ + "\n" +
                "Max RMS XYZ: " + maxRmsXYZ + "\n" +
                "RMS XYZ: " + rmsXYZ + "\n" +
                "Time: " + dateFormatted + "\n" +
                "]]>" +
                "</description>\n" +
                "\t<Point>\n" +
                "\t\t<coordinates>"+ longitude +","+ latitude +","+0+ "</coordinates>\n" +
                "\t</Point>\n" +
                "\t</Placemark>\n";
    }

    public static String getKMLEndString() {
        return "</Document>\n</kml>";
    }
}
