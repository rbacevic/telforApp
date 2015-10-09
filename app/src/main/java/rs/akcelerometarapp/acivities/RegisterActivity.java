package rs.akcelerometarapp.acivities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import rs.akcelerometarapp.R;
import rs.akcelerometarapp.utils.ProgressDialogUtils;

/**
 * Created by RADEEE on 09-Oct-15.
 */
public class RegisterActivity extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        configUI();
        setAllListeners();
    }

    protected void configUI() {
        progressDialog = ProgressDialogUtils.initProgressDialog(this);

        usernameEditText = (EditText)findViewById(R.id.register_username);
        passwordEditText = (EditText)findViewById(R.id.register_password);
        repeatPasswordEditText = (EditText)findViewById(R.id.repeat_register_password);
        nameEditText = (EditText)findViewById(R.id.register_name);
        lastnameEditText = (EditText)findViewById(R.id.register_last_name);
        emailEditText = (EditText)findViewById(R.id.register_email);
        registerButton = (Button)findViewById(R.id.register_button);
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
        //odraditi poziv prema serveru
        //zatvoriti progress dialog
        //poruka success/fail
        ProgressDialogUtils.dismissProgressDialog(progressDialog);
        Intent newIntent = new Intent(this, MainActivity.class);
        startActivity(newIntent);
    }

    protected EditText usernameEditText;
    protected EditText passwordEditText;
    protected EditText repeatPasswordEditText;
    protected EditText nameEditText;
    protected EditText lastnameEditText;
    protected EditText emailEditText;
    protected Button registerButton;
    protected ProgressDialog progressDialog;

    protected static final String TAG = "RegisterActivity";
}
