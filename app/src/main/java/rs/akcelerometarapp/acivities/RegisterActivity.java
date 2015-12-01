package rs.akcelerometarapp.acivities;


import android.app.ProgressDialog;
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
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

/**
 * Created by RADEEE on 09-Oct-15.
 */
public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        configUI();
        setAllListeners();
    }

    protected void configUI() {
        progressDialog = ProgressDialogUtils.initProgressDialog(this);

        usernameEditText = (AppCompatEditText)findViewById(R.id.register_username);
        passwordEditText = (AppCompatEditText)findViewById(R.id.register_password);
        repeatPasswordEditText = (AppCompatEditText)findViewById(R.id.repeat_register_password);
        nameEditText = (AppCompatEditText)findViewById(R.id.register_name);
        lastnameEditText = (AppCompatEditText)findViewById(R.id.register_last_name);
        emailEditText = (AppCompatEditText)findViewById(R.id.register_email);
        serverURLEditText = (AppCompatEditText)findViewById(R.id.server_url);
        registerButton = (AppCompatButton)findViewById(R.id.register_button);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        serverURLEditText.setText(UrlAddresses.DEFAULT_URL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registerSubscription != null && !registerSubscription.isUnsubscribed()) {
            registerSubscription.unsubscribe();
        }
    }

    protected void setAllListeners () {
        registerButton.setOnClickListener(registerClickListener);
    }

    protected boolean validateFields() {

        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirmPassword = repeatPasswordEditText.getText().toString();
        String name = nameEditText.getText().toString();
        String lastname = lastnameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String serverURL = serverURLEditText.getText().toString();

        if (username.length() == 0) {
            Toast.makeText(this, getString(R.string.username_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() == 0) {
            Toast.makeText(this, getString(R.string.password_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (confirmPassword.length() == 0) {
            Toast.makeText(this, getString(R.string.confirm_password), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.passwords_not_equal), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (name.length() == 0) {
            Toast.makeText(this, getString(R.string.name_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (lastname.length() == 0) {
            Toast.makeText(this, getString(R.string.lastname_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (email.length() == 0) {
            Toast.makeText(this, getString(R.string.email_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (serverURL.length() == 0) {
            Toast.makeText(this, getString(R.string.server_url_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    View.OnClickListener registerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (validateFields()) {
                UrlAddresses.setBaseUrl(serverURLEditText.getText().toString());
                register();
            }
        }
    };

    protected void register() {

        ProgressDialogUtils.showProgressDialog(progressDialog);
        executeRegisterRequest();
    }

    private void executeRegisterRequest() {

        registerSubscription = registerObservable()
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

                        // response 0 - uspesna registracija
                        // response 1 - zauzet username
                        // response 2 - nisu sva polja popunjena

                        if(rezultatPovratna == 0) {
                            ProgressDialogUtils.dismissProgressDialog(progressDialog);
                            Toast.makeText(RegisterActivity.this, "Uspesna registracija", Toast.LENGTH_SHORT).show();
                            finish();

                        } else if (rezultatPovratna == 1) {
                            ProgressDialogUtils.dismissProgressDialog(progressDialog);
                            Toast.makeText(RegisterActivity.this, "Korsisnik sa unetim korisnickim imenom vec postoji.", Toast.LENGTH_SHORT).show();
                        } else  if (rezultatPovratna == 2) {
                            ProgressDialogUtils.dismissProgressDialog(progressDialog);
                            Toast.makeText(RegisterActivity.this, "Niste popunili sva polja", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String setUpRegisterRequest() {

        ArrayList<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair(Constants.USERNAME, usernameEditText.getText().toString()));
        postParameters.add(new BasicNameValuePair(Constants.PASSWORD, passwordEditText.getText().toString()));
        postParameters.add(new BasicNameValuePair(Constants.NAME, nameEditText.getText().toString()));
        postParameters.add(new BasicNameValuePair(Constants.LAST_NAME, lastnameEditText.getText().toString()));
        postParameters.add(new BasicNameValuePair(Constants.EMAIL, emailEditText.getText().toString()));
        postParameters.add(new BasicNameValuePair(Constants.ACTION, Constants.ACTION_REGISTER));

        Log.d(TAG, "URL " + UrlAddresses.RegisterURL());
        try {
            return CustomHttpClient.executeHttpPost(UrlAddresses.RegisterURL(), postParameters);
        } catch (Exception e) {
            e.printStackTrace();
            ProgressDialogUtils.dismissProgressDialog(progressDialog);
            Toast.makeText(this, "Server nije trenutno dostupan...", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public Observable<String> registerObservable() {

        return Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.just(setUpRegisterRequest());
            }
        });
    }

    private Subscription registerSubscription;
    protected AppCompatEditText usernameEditText;
    protected AppCompatEditText passwordEditText;
    protected AppCompatEditText repeatPasswordEditText;
    protected AppCompatEditText nameEditText;
    protected AppCompatEditText lastnameEditText;
    protected AppCompatEditText emailEditText;
    protected AppCompatEditText serverURLEditText;
    protected AppCompatButton registerButton;
    protected ProgressDialog progressDialog;
    protected Toolbar toolbar;
    protected static final String TAG = "RegisterActivity";
}
