package rs.akcelerometarapp.acivities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

import rs.akcelerometarapp.R;
import rs.akcelerometarapp.network.CustomHttpClient;
import rs.akcelerometarapp.network.UrlAddresses;
import rs.akcelerometarapp.utils.ProgressDialogUtils;
import rs.akcelerometarapp.utils.SessionManager;

/**
 * Created by RADEEE on 10-Oct-15.
 */
public class CreateNewMeasurement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_measurement);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        configUI();
        collectData();
        setAllListeners();
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_LOGOUT, Menu.NONE, R.string.logout_label).setIcon(
                android.R.drawable.ic_menu_save);
        menu.add(Menu.NONE, MENU_EXIT, Menu.NONE, R.string.exit_label).setIcon(
                android.R.drawable.ic_menu_save);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(MENU_LOGOUT).setVisible(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_LOGOUT:
                SessionManager.getInstance(getApplicationContext()).logoutUser();
                finish();
                break;
            case MENU_EXIT:
                showExitDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void configUI() {
        progressDialog = ProgressDialogUtils.initProgressDialog(this);
        usernameTextView = (TextView)findViewById(R.id.username_text);
        measurementName = (AppCompatEditText)findViewById(R.id.measurement_name);
        measurementDescription = (AppCompatEditText)findViewById(R.id.measurement_description);
        startMeasurement = (AppCompatButton)findViewById(R.id.start_measurement);
        saveKMLFile = (CheckBox)findViewById(R.id.save_kml_file);
        saveKMLFile.setChecked(true);
        saveRawFile = (CheckBox)findViewById(R.id.save_raw_file);
        saveRawFile.setChecked(false);
        eliminateNearPoints = (AppCompatEditText)findViewById(R.id.eliminate_points_in_radius);
        timeBetweenPoints = (AppCompatEditText)findViewById(R.id.time_between_points);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        unitsRadioGroup = (RadioGroup) findViewById(R.id.measure_unit);
        unitsRadioGroup.check(R.id.unit_in_metre_per_seconds_square);
        deviceOrientationRadioGroup = (RadioGroup) findViewById(R.id.phone_orientation);
        deviceOrientationRadioGroup.check(R.id.vertical_phone_orientation);
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Da li zelite da napustite aplikaciju?").setPositiveButton("Da", dialogClickListener)
                .setNegativeButton("Ne", dialogClickListener).show();
    }

    protected void collectData() {

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager != null) {
            userId = sessionManager.getKeyUserId();
            username = sessionManager.getKeyUsername();
        }
        usernameTextView.setText(username);
    }

    @Override
    protected void onResume() {
        super.onResume();
        measurementName.setText("");
        measurementDescription.setText("");
    }

    protected void setAllListeners () {
        startMeasurement.setOnClickListener(startMeasurementClickListener);
        unitsRadioGroup.setOnCheckedChangeListener(unitChanged);
        deviceOrientationRadioGroup.setOnCheckedChangeListener(phoneOrientationChanged);
    }

    protected boolean validateFields() {

        String name = measurementName.getText().toString();

        if (name == null || name.length() == 0) {
            Toast.makeText(this, getString(R.string.measurement_name_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (userId == null || userId.length() == 0) {
            Toast.makeText(this, getString(R.string.user_id_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    View.OnClickListener startMeasurementClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (validateFields()) {
                startMeasure(measurementName.getText().toString());
            }
        }
    };

    RadioGroup.OnCheckedChangeListener unitChanged = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.unit_in_g:
                    measureUnit = UNIT_IN_G;
                    break;
                case R.id.unit_in_metre_per_seconds_square:
                    measureUnit = UNIT_IN_METRE_PER_SECOND_SQUARE;
                    break;
            }
        }
    };

    RadioGroup.OnCheckedChangeListener phoneOrientationChanged = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.horizontal_phone_orientation:
                    deviceOrientation = HORIZONTAL_DEVICE_ORIENTATION;
                    break;
                case R.id.vertical_phone_orientation:
                    deviceOrientation = VERICAL_DEVICE_ORIENTATION;
                    break;
            }
        }
    };

    protected void startMeasure(String measurementName) {

        SessionManager sessionManager = SessionManager.getInstance(this);

        eliminateNearPoints.setText(eliminateNearPoints.getText().toString().length() == 0 ? "0" : eliminateNearPoints.getText().toString());
        timeBetweenPoints.setText(timeBetweenPoints.getText().toString().length() == 0 ? "10" : timeBetweenPoints.getText().toString());

        int timeConstant = 10;
        int distanceConstant = 0;

        try {
            distanceConstant = Integer.parseInt(eliminateNearPoints.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        try {
            timeConstant = Integer.parseInt(timeBetweenPoints.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (sessionManager.isLocalUser()) {
            Intent newIntent = new Intent(this, AccelerometerActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("userID", userId);
            bundle.putString("measurementId", "test");
            bundle.putString("opis", measurementDescription.getText().toString());
            bundle.putString("imeMerenja", measurementName);
            bundle.putBoolean("saveKML", saveKMLFile.isChecked());
            bundle.putBoolean("saveRaw", saveRawFile.isChecked());
            bundle.putInt("eliminateNearPoints", distanceConstant);
            bundle.putInt("timeBetweenPoints", timeConstant);
            bundle.putInt("deviceOrientation", deviceOrientation);
            bundle.putInt("measureUnit", measureUnit);
            newIntent.putExtras(bundle);
            startActivity(newIntent);
        } else {
            ProgressDialogUtils.showProgressDialog(progressDialog);

            ArrayList<NameValuePair> postParameters = new ArrayList<>();
            postParameters.add(new BasicNameValuePair("naziv", measurementName));
            postParameters.add(new BasicNameValuePair("idK", userId));
            postParameters.add(new BasicNameValuePair("opis", measurementDescription.getText().toString()));
            postParameters.add(new BasicNameValuePair("akcija", "start"));

            String response = null;

            try {

                response = CustomHttpClient.executeHttpPost(UrlAddresses.CreateMesurementURL(), postParameters);
                String res = response.toString();
                res= res.replaceAll("\\s+", "");

                // Prebacivanje res u integer da bi moglo da se primeni u if-u
                int rezultatPovratna = Integer.parseInt(res);

                if(rezultatPovratna > 0){
                    Toast.makeText(this, "Merenje je uspesno kreirano", Toast.LENGTH_SHORT).show();

                    ProgressDialogUtils.dismissProgressDialog(progressDialog);

                    Intent newIntent = new Intent(this, AccelerometerActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("userID", userId);
                    bundle.putString("measurementId", res);
                    bundle.putString("opis", measurementDescription.getText().toString());
                    bundle.putString("imeMerenja", measurementName);
                    bundle.putBoolean("saveKML", saveKMLFile.isChecked());
                    bundle.putBoolean("saveRaw", saveRawFile.isChecked());
                    bundle.putInt("eliminateNearPoints", distanceConstant);
                    bundle.putInt("timeBetweenPoints", timeConstant);
                    bundle.putInt("deviceOrientation", deviceOrientation);
                    bundle.putInt("measureUnit", measureUnit);
                    newIntent.putExtras(bundle);
                    startActivity(newIntent);

                } else {
                    ProgressDialogUtils.dismissProgressDialog(progressDialog);
                    Toast.makeText(this, "Doslo je do greske prilikom kreiranja merenja, molimo vas pokusajte ponovo", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                ProgressDialogUtils.dismissProgressDialog(progressDialog);
                Toast.makeText(this, "Server nije trenutno dostupan...", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    protected AppCompatEditText measurementName;
    protected AppCompatEditText measurementDescription;
    protected TextView usernameTextView;
    protected CheckBox saveKMLFile;
    protected CheckBox saveRawFile;
    protected AppCompatEditText eliminateNearPoints;
    protected AppCompatEditText timeBetweenPoints;
    protected RadioGroup unitsRadioGroup;
    protected RadioGroup deviceOrientationRadioGroup;
    private int measureUnit = UNIT_IN_METRE_PER_SECOND_SQUARE;
    private int deviceOrientation = VERICAL_DEVICE_ORIENTATION;
    protected AppCompatButton startMeasurement;
    protected ProgressDialog progressDialog;
    protected String userId;
    protected String username;
    protected Toolbar toolbar;
    private static final int MENU_LOGOUT = (Menu.FIRST + 1);
    private static final int MENU_EXIT = (Menu.FIRST + 2);

    private static final int UNIT_IN_G = 0;
    private static final int UNIT_IN_METRE_PER_SECOND_SQUARE = 1;
    private static final int HORIZONTAL_DEVICE_ORIENTATION = 0;
    private static final int VERICAL_DEVICE_ORIENTATION = 1;
    protected static final String TAG = "CreateNewMeasurement";
}
