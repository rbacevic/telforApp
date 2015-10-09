package rs.akcelerometarapp.acivities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import rs.akcelerometarapp.R;
import rs.akcelerometarapp.utils.ProgressDialogUtils;

/**
 * Created by RADEEE on 07-Oct-15.
 */
public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

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
        //odraditi poziv prema serveru
        //zatvoriti progress dialog
        //poruka success/fail
        ProgressDialogUtils.dismissProgressDialog(progressDialog);
        Intent newIntent = new Intent(this, MainActivity.class);
        startActivity(newIntent);
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
