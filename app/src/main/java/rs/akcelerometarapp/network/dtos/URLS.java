package rs.akcelerometarapp.network.dtos;

/**
 * Created by RADEEE on 10-Oct-15.
 */
public class URLS {

    public static final String      BASE_URL                         = "http://192.168.1.3:8080/";
                                                                    //AndromapWebApp/ServletAndroid"
    protected static final String   URL_REGISTER                     = "Andromap/ServletAndroid";
    protected static final String   URL_LOGIN                        = "Andromap/ServletAndroid";
    protected static final String   URL_CREATE_MEASUREMENT           = "Andromap/ServletMerenjeAndroid";
    protected static final String   URL_ADD_POINT                    = "Andromap/ServletUpisTacke";
    protected static final String   URL_STOP_MEASUREMENT             = "Andromap/ServletMerenjeAndroid";

    public static String RegisterURL() {
        return BASE_URL + URL_REGISTER;
    }

    public static String LoginURL() {
        return BASE_URL + URL_LOGIN;
    }

    public static String CreateMesurementURL() {
        return BASE_URL + URL_CREATE_MEASUREMENT;
    }

    public static String AddPointURL() {
        return BASE_URL + URL_ADD_POINT;
    }

    public static String StopMesurementURL() {
        return BASE_URL + URL_STOP_MEASUREMENT;
    }
}
