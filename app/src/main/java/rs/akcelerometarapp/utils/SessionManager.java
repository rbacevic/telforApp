package rs.akcelerometarapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by RADEEE on 12-Oct-15.
 */
public class SessionManager {

    protected static SessionManager instance;

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    private SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void createLoginSession(String username, String userId, boolean isLocalUser){

        editor.putBoolean(IS_LOGIN, true);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_ID, userId);
        editor.putBoolean(KEY_IS_LOCAL_USER, isLocalUser);
        editor.commit();
    }

    public void logoutUser(){

        editor.remove(IS_LOGIN);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_IS_LOCAL_USER);
        editor.commit();
    }

    public boolean isLoggedIn(){
        return pref.getBoolean(IS_LOGIN, false);
    }

    public boolean isLocalUser(){
        return pref.getBoolean(KEY_IS_LOCAL_USER, false);
    }

    public String getKeyUsername() {
        return pref.getString(KEY_USERNAME, null);
    }

    public String getKeyUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;

    protected static final String PREF_NAME = "VibroAppPref";
    protected static final String IS_LOGIN = "IsLoggedIn";
    protected static final String KEY_USERNAME = "username";
    protected static final String KEY_USER_ID = "userID";
    protected static final String KEY_IS_LOCAL_USER = "isLocalUser";
    int PRIVATE_MODE = 0;
}
