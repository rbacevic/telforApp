package rs.akcelerometarapp.acivities;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

import rs.akcelerometarapp.R;
import rs.akcelerometarapp.network.CustomHttpClient;
import rs.akcelerometarapp.network.dtos.URLS;
import rs.akcelerometarapp.utils.ProgressDialogUtils;

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
        registerButton = (AppCompatButton)findViewById(R.id.register_button);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
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


        if (username == null || username.length() == 0) {
            Toast.makeText(this, getString(R.string.username_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password == null || password.length() == 0) {
            Toast.makeText(this, getString(R.string.password_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (confirmPassword == null || confirmPassword.length() == 0) {
            Toast.makeText(this, getString(R.string.confirm_password), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.passwords_not_equal), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (name == null || name.length() == 0) {
            Toast.makeText(this, getString(R.string.name_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (lastname == null || lastname.length() == 0) {
            Toast.makeText(this, getString(R.string.lastname_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (email == null || email.length() == 0) {
            Toast.makeText(this, getString(R.string.email_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    View.OnClickListener registerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (validateFields()) {
                register(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString(),
                        nameEditText.getText().toString(),
                        lastnameEditText.getText().toString(),
                        emailEditText.getText().toString());
            }
        }
    };

    protected void register(String username, String password, String name, String lastname, String email) {

        ProgressDialogUtils.showProgressDialog(progressDialog);

        ArrayList<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair("username", username));
        postParameters.add(new BasicNameValuePair("password", password));
        postParameters.add(new BasicNameValuePair("username", username));
        postParameters.add(new BasicNameValuePair("ime", name));
        postParameters.add(new BasicNameValuePair("prezime", lastname));
        postParameters.add(new BasicNameValuePair("email", email));
        postParameters.add(new BasicNameValuePair("akcija", "registracija"));

        String response = null;

        try {

            response = CustomHttpClient.executeHttpPost(URLS.RegisterURL(), postParameters);
            String res = response.toString();
            res= res.replaceAll("\\s+", "");

            // Prebacivanje res u integer da bi moglo da se primeni u if-u
            int rezultatPovratna = Integer.parseInt(res);

            // response 0 - uspesna registracija
            // response 1 - zauzet username
            // response 2 - nisu sva polja popunjena

            if(rezultatPovratna == 0) {
                Toast.makeText(this, "Uspesna registracija", Toast.LENGTH_SHORT).show();

                /*ProgressDialogUtils.dismissProgressDialog(progressDialog);
                Intent newIntent = new Intent(this, LoginActivity.class);
                startActivity(newIntent);*/
                finish();

            } else if (rezultatPovratna == 1) {
                ProgressDialogUtils.dismissProgressDialog(progressDialog);
                Toast.makeText(this, "Korsisnik sa unetim korisnickim imenom vec postoji.", Toast.LENGTH_SHORT).show();
            } else  if (rezultatPovratna == 2) {
                ProgressDialogUtils.dismissProgressDialog(progressDialog);
                Toast.makeText(this, "Niste popunili sva polja", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            ProgressDialogUtils.dismissProgressDialog(progressDialog);
            Toast.makeText(this, "Server nije trenutno dostupan...", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    protected AppCompatEditText usernameEditText;
    protected AppCompatEditText passwordEditText;
    protected AppCompatEditText repeatPasswordEditText;
    protected AppCompatEditText nameEditText;
    protected AppCompatEditText lastnameEditText;
    protected AppCompatEditText emailEditText;
    protected AppCompatButton registerButton;
    protected ProgressDialog progressDialog;
    protected Toolbar toolbar;
    protected static final String TAG = "RegisterActivity";
}
