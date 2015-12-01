package rs.akcelerometarapp.acivities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
 * Created by RADEEE on 10-Oct-15.
 */
public class CreateNewMeasurement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_measurement);

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

    @Override
    protected void onStop() {
        super.onStop();
        if (createMeasurementSubscription != null && !createMeasurementSubscription.isUnsubscribed()) {
            createMeasurementSubscription.unsubscribe();
        }
    }

    protected void setAllListeners () {
        startMeasurement.setOnClickListener(startMeasurementClickListener);
        unitsRadioGroup.setOnCheckedChangeListener(unitChanged);
        deviceOrientationRadioGroup.setOnCheckedChangeListener(phoneOrientationChanged);
    }

    protected boolean validateFields() {

        String name = measurementName.getText().toString();

        if (name.length() == 0) {
            Toast.makeText(this, getString(R.string.measurement_name_missing), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (userId.length() == 0) {
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

        timeConstant = 10;
        distanceConstant = 0;

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
            bundle.putString(Constants.USER_ID, userId);
            bundle.putString(Constants.MEASUREMENT_ID, "0");
            bundle.putString(Constants.DESCRIPTION, measurementDescription.getText().toString());
            bundle.putString(Constants.MEASUREMENT_NAME, measurementName);
            bundle.putBoolean(Constants.SAVE_KML_FILE, saveKMLFile.isChecked());
            bundle.putBoolean(Constants.SAVE_RAW_FILE, saveRawFile.isChecked());
            bundle.putInt(Constants.ELIMINATE_NEAR_POINTS, distanceConstant);
            bundle.putInt(Constants.TIME_BETWEEN_POINTS, timeConstant);
            bundle.putInt(Constants.DEVICE_ORIENTATION, deviceOrientation);
            bundle.putInt(Constants.MEASURE_UNIT, measureUnit);
            newIntent.putExtras(bundle);
            startActivity(newIntent);
        } else {
            ProgressDialogUtils.showProgressDialog(progressDialog);
            executeCreateNewMeasurementRequest();
        }
    }

    private void executeCreateNewMeasurementRequest() {

        createMeasurementSubscription = createNewMeasurementObservable()
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
                            Toast.makeText(CreateNewMeasurement.this, getString(R.string.measurement_creation_success), Toast.LENGTH_SHORT).show();

                            ProgressDialogUtils.dismissProgressDialog(progressDialog);

                            Intent newIntent = new Intent(CreateNewMeasurement.this, AccelerometerActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString(Constants.USER_ID, userId);
                            bundle.putString(Constants.MEASUREMENT_ID, res);
                            bundle.putString(Constants.DESCRIPTION, measurementDescription.getText().toString());
                            bundle.putString(Constants.MEASUREMENT_NAME, measurementName.getText().toString());
                            bundle.putBoolean(Constants.SAVE_KML_FILE, saveKMLFile.isChecked());
                            bundle.putBoolean(Constants.SAVE_RAW_FILE, saveRawFile.isChecked());
                            bundle.putInt(Constants.ELIMINATE_NEAR_POINTS, distanceConstant);
                            bundle.putInt(Constants.TIME_BETWEEN_POINTS, timeConstant);
                            bundle.putInt(Constants.DEVICE_ORIENTATION, deviceOrientation);
                            bundle.putInt(Constants.MEASURE_UNIT, measureUnit);
                            newIntent.putExtras(bundle);
                            startActivity(newIntent);

                        } else {
                            ProgressDialogUtils.dismissProgressDialog(progressDialog);
                            Toast.makeText(CreateNewMeasurement.this, getString(R.string.measurement_creation_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String setUpCreateNewMeasurementRequest() {

        ArrayList<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair(Constants.MEASUREMENT_N, measurementName.getText().toString()));
        postParameters.add(new BasicNameValuePair(Constants.ID_K, userId));
        postParameters.add(new BasicNameValuePair(Constants.DESCRIPTION, measurementDescription.getText().toString()));
        postParameters.add(new BasicNameValuePair(Constants.ACTION, Constants.ACTION_START));

        Log.d(TAG, "URL " + UrlAddresses.CreateMesurementURL());
        try {
            return CustomHttpClient.executeHttpPost(UrlAddresses.CreateMesurementURL(), postParameters);
        } catch (Exception e) {
            e.printStackTrace();
            ProgressDialogUtils.dismissProgressDialog(progressDialog);
            Toast.makeText(this, "Server nije trenutno dostupan...", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public Observable<String> createNewMeasurementObservable() {

        return Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.just(setUpCreateNewMeasurementRequest());
            }
        });
    }

    private Subscription createMeasurementSubscription;
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
    private int timeConstant;
    private int distanceConstant;
    private static final int UNIT_IN_G = 0;
    private static final int UNIT_IN_METRE_PER_SECOND_SQUARE = 1;
    private static final int HORIZONTAL_DEVICE_ORIENTATION = 0;
    private static final int VERICAL_DEVICE_ORIENTATION = 1;
    protected static final String TAG = "CreateNewMeasurement";
}
