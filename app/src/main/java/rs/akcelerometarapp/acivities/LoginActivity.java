package rs.akcelerometarapp.acivities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

import rs.akcelerometarapp.R;
import rs.akcelerometarapp.network.CustomHttpClient;
import rs.akcelerometarapp.network.dtos.URLS;
import rs.akcelerometarapp.utils.ProgressDialogUtils;

/**
 * Created by RADEEE on 07-Oct-15.
 */
public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        configUI();
        setAllListeners();
    }

    protected void configUI() {
        progressDialog = ProgressDialogUtils.initProgressDialog(this);
        usernameTexView = (TextView)findViewById(R.id.login_username_textView);
        passwordTexView = (TextView)findViewById(R.id.login_password_textView);
        usernameEditText = (EditText)findViewById(R.id.login_username);
        passwordEditText = (EditText)findViewById(R.id.login_password);
        loginButton = (Button)findViewById(R.id.login_button);
        registerButton = (Button)findViewById(R.id.register_button);
    }

    protected void setAllListeners () {
        loginButton.setOnClickListener(loginClickListener);
        registerButton.setOnClickListener(registerClickListener);
    }

    protected boolean validateFields() {

        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (username == null || username.length() == 0) {
            Toast.makeText(this, getString(R.string.username_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password == null || password.length() == 0) {
            Toast.makeText(this, getString(R.string.password_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    View.OnClickListener loginClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (validateFields()) {
                login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
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

    protected void login(String username, String password) {

        ProgressDialogUtils.showProgressDialog(progressDialog);

        ArrayList<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair("username", username));
        postParameters.add(new BasicNameValuePair("password", password));
        postParameters.add(new BasicNameValuePair("akcija", "login"));

        String response = null;

        try {

            response = CustomHttpClient.executeHttpPost(URLS.LoginURL(), postParameters);
            String res=response.toString();
            res= res.replaceAll("\\s+", "");

            // Prebacivanje res u integer da bi moglo da se primeni u if-u
            int rezultatPovratna = Integer.parseInt(res);

            if(rezultatPovratna > 0){
                Toast.makeText(this, "Uspesna verifikacija", Toast.LENGTH_SHORT).show();

                ProgressDialogUtils.dismissProgressDialog(progressDialog);
                Intent newIntent = new Intent(this, CreateNewMeasurement.class);
                Bundle bundle = new Bundle();
                bundle.putString("id", res);
                bundle.putString("username", username);
                newIntent.putExtras(bundle);
                startActivity(newIntent);

            } else {
                ProgressDialogUtils.dismissProgressDialog(progressDialog);
                Toast.makeText(this, "Pogresno korisnicko ime ili sifra", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            ProgressDialogUtils.dismissProgressDialog(progressDialog);
            Toast.makeText(this, "Slaba konekcija sa internetom,pokusajte ponovo..", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    protected TextView usernameTexView;
    protected TextView passwordTexView;
    protected EditText usernameEditText;
    protected EditText passwordEditText;
    protected Button loginButton;
    protected Button registerButton;
    protected ProgressDialog progressDialog;

    protected static final String TAG = "LoginActivity";
}
