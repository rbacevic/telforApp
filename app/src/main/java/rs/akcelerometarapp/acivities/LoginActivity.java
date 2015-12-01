package rs.akcelerometarapp.acivities;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

import rs.akcelerometarapp.R;
import rs.akcelerometarapp.constants.Constants;
import rs.akcelerometarapp.network.CustomHttpClient;
import rs.akcelerometarapp.network.UrlAddresses;
import rs.akcelerometarapp.utils.ProgressDialogUtils;
import rs.akcelerometarapp.utils.SessionManager;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

/**
 * Created by RADEEE on 07-Oct-15.
 */
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        configUI();
        setAllListeners();

        if (SessionManager.getInstance(this).isLoggedIn()) {
            UrlAddresses.setBaseUrl(SessionManager.getInstance(this).getSeverUrl());
            Intent newIntent = new Intent(this, CreateNewMeasurement.class);
            startActivity(newIntent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (loginSubscription != null && !loginSubscription.isUnsubscribed()) {
            loginSubscription.unsubscribe();
        }
    }

    protected void configUI() {
        progressDialog = ProgressDialogUtils.initProgressDialog(this);

        usernameEditText = (AppCompatEditText)findViewById(R.id.login_username);
        passwordEditText = (AppCompatEditText)findViewById(R.id.login_password);
        serverURLEditText = (AppCompatEditText)findViewById(R.id.server_url);
        loginButton = (AppCompatButton)findViewById(R.id.login_button);
        registerButton = (AppCompatButton)findViewById(R.id.register_button);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        serverURLEditText.setText(UrlAddresses.DEFAULT_URL);
    }

    protected void setAllListeners () {
        loginButton.setOnClickListener(loginClickListener);
        registerButton.setOnClickListener(registerClickListener);
    }

    protected boolean validateFields() {

        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String serverURL = serverURLEditText.getText().toString();

        if (username.length() == 0) {
            Toast.makeText(this, getString(R.string.username_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() == 0) {
            Toast.makeText(this, getString(R.string.password_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (serverURL.length() == 0) {
            Toast.makeText(this, getString(R.string.server_url_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    View.OnClickListener loginClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (validateFields()) {
                UrlAddresses.setBaseUrl(serverURLEditText.getText().toString());
                login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString(),
                        serverURLEditText.getText().toString());
            }
        }
    };

    View.OnClickListener registerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent newIntent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(newIntent);
        }
    };

    protected void login(String username, String password, String serverURL) {

        ProgressDialogUtils.showProgressDialog(progressDialog);

        if (username.equalsIgnoreCase("test") && password.equalsIgnoreCase("test")) {
            ProgressDialogUtils.dismissProgressDialog(progressDialog);
            SessionManager.getInstance(this).createLoginSession(username, "0", serverURL, true);
            Toast.makeText(this, "Ulogovani ste kao lokalni korisnik", Toast.LENGTH_SHORT).show();
            Intent newIntent = new Intent(this, CreateNewMeasurement.class);
            startActivity(newIntent);
        } else {
            executeLoginRequest();
        }
    }

    private void executeLoginRequest() {

        loginSubscription = loginObservable()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "Complete");
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG,  s);

                        String res= s.replaceAll("\\s+", "");
                        int rezultatPovratna = Integer.parseInt(res);

                        if(rezultatPovratna > 0){
                            Toast.makeText(LoginActivity.this, "Uspesna verifikacija", Toast.LENGTH_SHORT).show();

                            ProgressDialogUtils.dismissProgressDialog(progressDialog);
                            SessionManager.getInstance(LoginActivity.this)
                                    .createLoginSession(usernameEditText.getText().toString(), res, serverURLEditText.getText().toString(), false);
                            Intent newIntent = new Intent(LoginActivity.this, CreateNewMeasurement.class);
                            startActivity(newIntent);

                        } else {
                            ProgressDialogUtils.dismissProgressDialog(progressDialog);
                            Toast.makeText(LoginActivity.this, "Pogresno korisnicko ime ili sifra", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String setUpLoginRequest() {

        ArrayList<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair(Constants.USERNAME, usernameEditText.getText().toString()));
        postParameters.add(new BasicNameValuePair(Constants.PASSWORD,  passwordEditText.getText().toString()));
        postParameters.add(new BasicNameValuePair(Constants.ACTION, Constants.ACTION_LOGIN));

        Log.d(TAG, "URL " + UrlAddresses.LoginURL());
        try {
            return CustomHttpClient.executeHttpPost(UrlAddresses.LoginURL(), postParameters);
        } catch (Exception e) {
            e.printStackTrace();
            ProgressDialogUtils.dismissProgressDialog(progressDialog);
            Toast.makeText(this, "Server nije trenutno dostupan...", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public Observable<String> loginObservable() {

        return Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.just(setUpLoginRequest());
            }
        });
    }

    private Subscription loginSubscription;
    protected AppCompatEditText usernameEditText;
    protected AppCompatEditText passwordEditText;
    protected AppCompatEditText serverURLEditText;
    protected AppCompatButton loginButton;
    protected AppCompatButton registerButton;
    protected ProgressDialog progressDialog;
    protected Toolbar toolbar;
    protected static final String TAG = "LoginActivity";
}
