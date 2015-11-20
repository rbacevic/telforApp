package rs.akcelerometarapp.network.dtos;

/**
 * Created by RADEEE on 10-Oct-15.
 */
public class URLS {

    public static String BASE_URL;
    public static final String      DEFAULT_URL                      = "http://csl.ftn.kg.ac.rs:8088/VibroMap";
                                                                    //AndromapWebApp/ServletAndroid"
    protected static final String   URL_REGISTER                     = "/ServletAndroid";
    protected static final String   URL_LOGIN                        = "/ServletAndroid";
    protected static final String   URL_CREATE_MEASUREMENT           = "/ServletMerenjeAndroid";
    protected static final String   URL_ADD_POINT                    = "/ServletUpisTacke";
    protected static final String   URL_STOP_MEASUREMENT             = "/ServletMerenjeAndroid";

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static void setBaseUrl(String baseUrl) {
        BASE_URL = baseUrl;
    }

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
