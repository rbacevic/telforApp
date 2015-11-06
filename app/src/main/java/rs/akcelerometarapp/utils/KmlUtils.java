package rs.akcelerometarapp.utils;

/**
 * Created by RADEEE on 10-Oct-15.
 */
public class KmlUtils {

    public static String getKMLStartString(String name, String description) {

        return  "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<kml xmlns='http://www.opengis.net/kml/2.2'>\n" +
                "<Document>\n" +
                "<name>"+ name +"</name> \n"+
                "<description>" + description + "</description> \n"+
                "<Style id='greenPlacemark'> \n"+
                "<IconStyle> \n"+
                "<Icon> \n"+
                "<href>http://maps.google.com/mapfiles/kml/paddle/grn-blank.png</href> \n"+
                "</Icon> \n"+
                "</IconStyle> \n"+
                "</Style> \n"+
                "<Style id='orangePlacemark'> \n"+
                "<IconStyle> \n"+
                "<Icon> \n"+
                "<href>http://maps.google.com/mapfiles/kml/paddle/orange-blank.png</href> \n"+
                "</Icon> \n"+
                "</IconStyle> \n"+
                "</Style> \n" +
                "<Style id='highlightPlacemark'> \n"+
                "<IconStyle> \n"+
                "<Icon> \n"+
                "<href>http://www.google.com/mapfiles/ms/micons/earthquake.png</href> \n"+
                "</Icon> \n"+
                "</IconStyle> \n"+
                "</Style> \n" +
                "<Style id='averagePlacemark'> \n"+
                "<IconStyle> \n"+
                "<Icon> \n"+
                "<href>http://maps.google.com/mapfiles/kml/pal4/icon39.png</href> \n"+
                "</Icon> \n"+
                "</IconStyle> \n"+
                "</Style> \n";
    }

    public static String createKMLPointString(String pointStyle, int pointIndex, String rmsX,
                                              String rmsY, String rmsZ, String rmsXYZ, String maxRmsXYZ,
                                              String dateFormatted, String speedInKmPerHour, double latitude,
                                              double longitude, String altitude, String maxRmsX, String maxRmsY, String maxRmsZ,
                                              String xValueForApeak, String yValueForApeak, String zValueForApeak) {
        return "\t<Placemark>\n" +
                "<styleUrl>#" + pointStyle + "</styleUrl>"+
                "\t<name>" + "Tacka " + pointIndex + "</name>\n" +
                "\t<description>"+ "<![CDATA["+
                "Brzina [km/h]: " + speedInKmPerHour + "\n"+
                "Altitude: " + altitude + "\n" +
                "RMS X: " + rmsX + "\n" +
                "RMS Y: " + rmsY + "\n" +
                "RMS Z: " + rmsZ + "\n" +
                "RMS XYZ: " + rmsXYZ + "\n" +
                "Apeak X: " + maxRmsX + "\n" +
                "Apeak Y: " + maxRmsY + "\n" +
                "Apeak Z: " + maxRmsZ + "\n" +
                "Apeak XYZ: " + maxRmsXYZ + "\n" +
                "X :"  + xValueForApeak + "\n" +
                "Y :"  + yValueForApeak + "\n" +
                "Z :"  + zValueForApeak + "\n" +
                "Vreme: " + dateFormatted + "\n" +
                "]]>" +
                "</description>\n" +
                "\t<Point>\n" +
                "\t\t<coordinates>"+ longitude +","+ latitude +","+0+ "</coordinates>\n" +
                "\t</Point>\n" +
                "\t</Placemark>\n";
    }

    public static String createFinalKMLPointString(int numberOfMarkers, int numberOfGreenMarkers, int numberOfYellowMarkers,
                                                   int numberOfRedMarkers, double averageRMSX, double averageRMSY, double averageRMSZ,
                                                   double averageRMSXYZ, double averageMaxRMSXYZ, double averageMaxRMSX, double averageMaxRMSY,
                                                   double averageMaxRMSZ, double averageX, double averageY, double averageZ,
                                                   double averageSpeed, double averageAltitude, double latitude, double longitude) {
        return "\t<Placemark>\n" +
                "<styleUrl>#" + "averagePlacemark" + "</styleUrl>"+
                "\t<name>" + "Rezime merenja" + "</name>\n" +
                "\t<description>"+ "<![CDATA["+
                "Prosecna brzina [km/h]: " + averageSpeed + "\n"+
                "Prosecna nadmorska visina: " + averageAltitude + "\n" +
                "Ukupan broj tacaka: " + numberOfMarkers + "\n" +
                "Ukupan broj udobnih tacaka: " + numberOfGreenMarkers + "\n" +
                "Ukupan broj srednje udobnih tacaka: " + numberOfYellowMarkers + "\n" +
                "Ukupan broj neudobnih tacaka: " + numberOfRedMarkers + "\n" +
                "Prosecne vrednosti RMS-a: \n" +
                "RMS X: " + averageRMSX + "\n" +
                "RMS Y: " + averageRMSY + "\n" +
                "RMS Z: " + averageRMSZ + "\n" +
                "RMS XYZ: " + averageRMSXYZ + "\n" +
                "Apeak X: " + averageMaxRMSX + "\n" +
                "Apeak Y: " + averageMaxRMSY + "\n" +
                "Apeak Z: " + averageMaxRMSZ + "\n" +
                "Apeak XYZ: " + averageMaxRMSXYZ + "\n" +
                "X :"  + averageX + "\n" +
                "Y :"  + averageY + "\n" +
                "Z :"  + averageZ + "\n" +
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
