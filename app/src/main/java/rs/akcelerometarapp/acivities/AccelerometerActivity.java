package rs.akcelerometarapp.acivities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pwittchen.reactivesensors.library.ReactiveSensorEvent;
import com.github.pwittchen.reactivesensors.library.ReactiveSensors;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rs.akcelerometarapp.R;
import rs.akcelerometarapp.components.GraphView;
import rs.akcelerometarapp.constants.Constants;
import rs.akcelerometarapp.network.CustomHttpClient;
import rs.akcelerometarapp.network.UrlAddresses;
import rs.akcelerometarapp.utils.FileUtils;
import rs.akcelerometarapp.utils.KmlUtils;
import rs.akcelerometarapp.utils.SessionManager;
import rs.akcelerometarapp.utils.TxtFileUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by RADEEE on 10-Oct-15.
 */
public class AccelerometerActivity extends AppCompatActivity {

    //*********************************** Life Cycle *********************************************//

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "MainActivity.onCreate()");
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        // Održavanje aktivnog ekrana da ne ide u sleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Setovanje layouta
        setContentView(R.layout.activity_main);
        // Setovanje UI
        configureUI();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            measurementId = bundle.getString(Constants.MEASUREMENT_ID);
            userId = bundle.getString(Constants.USER_ID);
            measurementDescription = bundle.getString(Constants.MEASUREMENT_DESCRIPTION);
            saveKMLFile = bundle.getBoolean(Constants.SAVE_KML_FILE);
            saveRAWFile = bundle.getBoolean(Constants.SAVE_RAW_FILE);
            minDistanceBetweenTwoPoints = bundle.getInt(Constants.ELIMINATE_NEAR_POINTS);
            minTimeBetweenTwoPoints = bundle.getInt(Constants.TIME_BETWEEN_POINTS) * 1000;
            deviceOrientation = bundle.getInt(Constants.DEVICE_ORIENTATION);
            measureUnit = bundle.getInt(Constants.MEASURE_UNIT);
            measurementName = bundle.getString(Constants.MEASUREMENT_NAME);
        }

        configureToolbar();

        locationProvider = new ReactiveLocationProvider(getApplicationContext());
        locationRequest = locationRequest();
        locationUpdatesObservable = getLocationUpdatesObservable();

        //zapocni snimanje
        mRecording = true;
        locationUpdatesObservable.subscribe();
        mTempFilterList.clear();
        valueOfMaxRMS = 0;
        xValueForMaxRMS = 0;
        yValueForMaxRMS = 0;
        zValueForMaxRMS = 0;
        samplesCount = 0;
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "MainActivity.onStart()");
        //Inicijalizacija
        initialize();
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "MainActivity.onResume()");
        startGraph();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "MainActivity.onPause()");
        stopGraph();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "MainActivity.onStop()");
        sensor.unsubscribe();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    final public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
       /* super.onBackPressed();
        if (mRecording) {
            //stopMeasurement();
            Toast.makeText(this, getString(R.string.save_complated), Toast.LENGTH_LONG).show();
            saveHistory();
            updatableLocationSubscription.unsubscribe();
            finish();
        }*/
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_VOLUME_UP:
                        mGraphView.increaseGraphScale();
                        return true;
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        mGraphView.decreaseGraphScale();
                        return true;
                }
                //Kada korisnik pusti neko dugme odnosno okonca akciju
            case KeyEvent.ACTION_UP:
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_VOLUME_UP:
                    case KeyEvent.KEYCODE_VOLUME_DOWN:

                        return true;
                }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Mogucnost pomeranja grafika po Y osi istog
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mGraphView.setmTouchOffset(event.getY());
                break;
            case MotionEvent.ACTION_UP:
                mGraphView.setmZeroLineY(mGraphView.getmZeroLineY() + mGraphView.getmZeroLineYOffset());
                mGraphView.setmZeroLineYOffset(0);
                break;
            case MotionEvent.ACTION_MOVE:
                mGraphView.setmZeroLineYOffset((int) (event.getY() - mGraphView.getmTouchOffset()));
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_SENSOR_DELAY, Menu.NONE,
                R.string.sensor_delay_label).setIcon(
                android.R.drawable.ic_menu_rotate);
        menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, R.string.save_complate).setIcon(
                android.R.drawable.ic_menu_save);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mRecording) {
            menu.findItem(MENU_SAVE).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SENSOR_DELAY:
                selectSensorDelay();
                break;
            case MENU_SAVE:
                //stopMeasurement();
                saveHistory();
                Toast.makeText(this, getString(R.string.save_complated), Toast.LENGTH_LONG).show();
                updatableLocationSubscription.unsubscribe();
                if (SessionManager.getInstance(this).isLocalUser()) {
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);//intent);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                //Refrence: https://developers.google.com/android/reference/com/google/android/gms/location/SettingsApi
                switch (resultCode) {
                    case RESULT_OK:
                        // All required changes were successfully made
                        break;
                    case RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Log.d(TAG,"User Cancelled enabling location");
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    //************************************* Init *************************************************//

    private void initialize() {
        reactiveSensor = new ReactiveSensors(AccelerometerActivity.this);
        if (reactiveSensor.hasSensor(Sensor.TYPE_ACCELEROMETER)) {
            sensor = defineSensorListener();
        } else {
            Toast.makeText(AccelerometerActivity.this, getResources().getString(R.string.acc_sensor_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    private LocationRequest locationRequest() {
        return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100);
    }

    //********************************* Private API UI *******************************************//

    private void configureUI() {

        // Uzimanje frame layout-a
        FrameLayout frame = (FrameLayout) findViewById(R.id.frame);

        //Dodat frame layout za pregled grafa sa parametrom mGraphView i spec parametrom 0
        mGraphView = new GraphView(this);
        frame.addView(mGraphView, 0);

        //Setovan listener za check box
        CheckBox[] checkboxes = new CheckBox[4];
        checkboxes[DATA_X] = (CheckBox) findViewById(R.id.accele_x);
        checkboxes[DATA_Y] = (CheckBox) findViewById(R.id.accele_y);
        checkboxes[DATA_Z] = (CheckBox) findViewById(R.id.accele_z);
        checkboxes[DATA_R] = (CheckBox) findViewById(R.id.accele_r);
        for (int i = 0; i < 4; i++) {
            if (mGraphView.getmGraphs()[i]) {
                checkboxes[i].setChecked(true);
            }
            checkboxes[i]
                    .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView,
                                                     boolean isChecked) {
                            switch (buttonView.getId()) {
                                case R.id.accele_x:
                                    mGraphView.setGraphLineVisibility(DATA_X, isChecked);
                                    break;
                                case R.id.accele_y:
                                    mGraphView.setGraphLineVisibility(DATA_Y, isChecked);
                                    break;
                                case R.id.accele_z:
                                    mGraphView.setGraphLineVisibility(DATA_Z, isChecked);
                                    break;
                                case R.id.accele_r:
                                    mGraphView.setGraphLineVisibility(DATA_R, isChecked);
                                    break;
                            }
                        }
                    });
        }

        // Uzete vrednosti osa za TextView prikaz
        mAccValueViews[DATA_X] = (TextView) findViewById(R.id.accele_x_value);
        mAccValueViews[DATA_Y] = (TextView) findViewById(R.id.accele_y_value);
        mAccValueViews[DATA_Z] = (TextView) findViewById(R.id.accele_z_value);
        mAccValueViews[DATA_R] = (TextView) findViewById(R.id.accele_r_value);

        // Pass filter - registracija listener-a za radio button-e
        RadioGroup passFilterGroup = (RadioGroup) findViewById(R.id.pass_filter);
        passFilterGroup.check(R.id.pass_filter_high);
        passFilterGroup
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (checkedId) {
                            case R.id.pass_filter_raw:
                                mPassFilter = PASS_FILTER_RAW;
                                break;
                            case R.id.pass_filter_low:
                                mPassFilter = PASS_FILTER_LOW;
                                break;
                            case R.id.pass_filter_high:
                                mPassFilter = PASS_FILTER_HIGH;
                                break;
                        }
                    }
                });

        lightBulbImageView = (ImageView)findViewById(R.id.light_bulb_image);
        rmsTextView = (TextView)findViewById(R.id.rms_value);
        rmsTextView.setText(getString(R.string.rms_default_value));

        //Rate filtera - Uzimanje vrednosti za TextView prikaz
        mFilterRateView = (TextView) findViewById(R.id.filter_rate_value);
        mFilterRateView.setText(String.valueOf(mFilterRate));

        //Rate filtera - registracija listener-a za  promenu seek bar-a
        SeekBar filterRateBar = (SeekBar) findViewById(R.id.filter_rate);
        filterRateBar.setMax(100);
        filterRateBar.setProgress((int) (mFilterRate * 100));
        seekBarChangeListener(filterRateBar).subscribe(new Subscriber<Float>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Float aFloat) {
                mFilterRateView.setText(String.valueOf(aFloat));
            }
        });
    }

    private void selectSensorDelay() {
        // Izbor kasnjenja senzora iz pod menija
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final CharSequence[] delays = {"FASTEST", "GAME", "UI", "NORMAL"};
        dialogBuilder.setItems(delays, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        mSensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
                        break;
                    case 1:
                        mSensorDelay = SensorManager.SENSOR_DELAY_GAME;
                        break;
                    case 2:
                        mSensorDelay = SensorManager.SENSOR_DELAY_UI;
                        break;
                    case 3:
                        mSensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
                        break;
                }
                // Re-registracija listener-a
                stopGraph();
                startGraph();
            }
        });
        dialogBuilder.show();
    }

    void configureToolbar() {

        String username = SessionManager.getInstance(this).getKeyUsername();
        String unit = measureUnit == 0 ? "G" : "m/s^2";
        String deviceOrient = deviceOrientation == 0 ? "H" : "V";
        String saveFile = "";

        if (saveKMLFile) {
            saveFile = "KML";
        }

        if (saveRAWFile) {
            if (saveFile.length() > 0) {
                saveFile = saveFile + " i CSV";
            } else {
                saveFile = "CSV";
            }
        }

        fileUtils = new FileUtils(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        String title = getResources().getString(R.string.app_name) + " - " + username + " / " + saveFile + " / "
                + unit + " / " + deviceOrient + " / " + minTimeBetweenTwoPoints/1000 + "s / " + minDistanceBetweenTwoPoints + "m";
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
    }

    private String roundFourDecimals(double d)
    {
        DecimalFormat twoDForm = new DecimalFormat("#.####");
        return twoDForm.format(d);
    }

    private String roundTwoDecimals(double d)
    {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return twoDForm.format(d);
    }

    private String getFormattedDate(long currentTime) {

        Date date = new Date(currentTime);
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");
        return dateFormatter.format(date);
    }

    //*********************************** Private Data *******************************************//

    private void collectDataFromSensor(SensorEvent event) {

        for (int angle = 0; angle < 3; angle++) {

            int index = 0;
            if (angle == DATA_X) {
                index = deviceOrientation == HORIZONTAL_DEVICE_ORIENTATION ? DATA_X : DATA_Z;
            }

            if (angle == DATA_Y) {
                index = DATA_Y;
            }

            if (angle == DATA_Z) {
                index = deviceOrientation == HORIZONTAL_DEVICE_ORIENTATION ? DATA_Z : DATA_X;
            }

            float value = measureUnit == UNIT_IN_G ? event.values[index]/SensorManager.GRAVITY_EARTH : event.values[index];

            // Kreiranje low-pass filtera po formuli  aX = (x*0.1)+(ax*(1-0.1))
            mLowPassFilters[angle] = (mLowPassFilters[angle] * (1 - mFilterRate))
                    + (value * mFilterRate);
            // Filter
            switch (mPassFilter) {
                case PASS_FILTER_LOW:
                    value = mLowPassFilters[angle];
                    break;
                //High-pass filter po formuli aX = aX-((x*0.1)+(ax*(1-0.1)))
                case PASS_FILTER_HIGH:
                    value -= mLowPassFilters[angle];
                    break;
            }
            mCurrents[angle] = value;
            mAccValueViews[angle].setText(roundFourDecimals(value));
        }

        // Izracunavanje vektora akceleracije R - osa
        Double dReal = Math.abs(Math.sqrt(Math.pow(mCurrents[DATA_X], 2)
                + Math.pow(mCurrents[DATA_Y], 2)
                + Math.pow(mCurrents[DATA_Z], 2)));
        float fReal = dReal.floatValue();

        mCurrents[DATA_R] = fReal;
        mAccValueViews[DATA_R].setText(roundFourDecimals(fReal));

        synchronized (this) {
            // History register
            mGraphView.addDataToGraphHistory(mCurrents.clone());
        }
    }

    private void calculateRMS(ReactiveSensorEvent reactiveSensorEvent) {

        SensorEvent event = reactiveSensorEvent.getSensorEvent();
        collectDataFromSensor(event);

        if (locationOfMaxRms != null) {

            if (!measureStarted && samplesCount == 0 && !deviceOrientationChanged) {
                Toast.makeText(this, getString(R.string.find_equilibrium_position), Toast.LENGTH_LONG).show();
            }

            if (mRecording) {
                if (saveRAWFile && measureStarted) {

                    if (rawFileOutputStream == null) {
                        rawFileOutputStream = fileUtils.createCSVFile(System.currentTimeMillis());
                    }

                    mRawHistory.add(event.values.clone());    // dodavanje raw signala u listu
                    for (int i = 4; i < 15; i++) {
                        mCurrents[i] = Constants.INVALID_VALUE;
                    }
                    mFilterHistory.add(mCurrents.clone());    // dodavanje filtiranog signala u listu
                }

                if (saveKMLFile && kmlFileOutputStream == null) {
                    kmlFileOutputStream = fileUtils.createKMLFile(System.currentTimeMillis(), getKmlHeaderString());
                }

                if (txtFileOutputStream == null) {
                    txtFileOutputStream = fileUtils.createTxtFile(System.currentTimeMillis(), measurementName, measurementDescription, userId, measurementId);
                }

                mTempFilterList.add(mCurrents.clone());

                double currentRmsXYZ = (Math.abs(Math.sqrt(Math.pow(mCurrents[DATA_X], 2)
                        + Math.pow(mCurrents[DATA_Y], 2)
                        + Math.pow(mCurrents[DATA_Z], 2))));
                samplesCount++;

                if (currentRmsXYZ >= valueOfMaxRMS) {
                    valueOfMaxRMS = currentRmsXYZ;
                    xValueForMaxRMS = mCurrents[DATA_X];
                    yValueForMaxRMS = mCurrents[DATA_Y];
                    zValueForMaxRMS = mCurrents[DATA_Z];
                    getDetectionLocation();
                }

                //check last update time
                long diff = -1;
                if(lastRMSDate != null) {
                    diff = new Date().getTime() - lastRMSDate.getTime();
                }

                if(diff > minTimeBetweenTwoPoints) {
                    if (measureStarted) {
                        if (minDistanceBetweenTwoPoints > 0) {
                            if (locationUpdated) {
                                calculateRMSPoint(true);
                            } else {
                                lastRMSDate = new Date();
                                mTempFilterList.clear();
                                valueOfMaxRMS = 0;
                                xValueForMaxRMS = 0;
                                yValueForMaxRMS = 0;
                                zValueForMaxRMS = 0;
                                samplesCount = 0;
                                Log.d(TAG,"Prostorna neosetljivost");
                            }
                        } else  {
                            calculateRMSPoint(true);
                        }
                    } else {
                        //kalibracija
                        if ((Math.abs(event.values[0]) > Math.abs(event.values[2]) && deviceOrientation == HORIZONTAL_DEVICE_ORIENTATION) ||
                                (Math.abs(event.values[0]) < Math.abs(event.values[2]) && deviceOrientation == VERICAL_DEVICE_ORIENTATION)){
                            deviceOrientationChanged = true;
                        } else  {
                            deviceOrientationChanged = false;
                        }
                        calculateRMSPoint(false);
                    }
                }
            }
        } else {
            //uzimanje prvog GPS-a
            if (!getIntialGPS) {
                getIntialGPS = true;
                if (lastRMSDate == null) {
                    Toast.makeText(this, getString(R.string.finding_GPS), Toast.LENGTH_SHORT).show();
                }
                getDetectionLocation();
                lastRMSDate = new Date();
            }
        }
    }

    private void getDetectionLocation() {
        updatableLocationSubscription = locationUpdatesObservable
                .subscribe(new Subscriber<Location>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Location location) {

                        if (locationOfMaxRms == null) {
                            Toast.makeText(AccelerometerActivity.this, getString(R.string.GPS_found), Toast.LENGTH_LONG).show();
                        }

                        if (locationOfMaxRms != null && minDistanceBetweenTwoPoints > 0) {
                            float distanceInMeters = locationOfMaxRms.distanceTo(location);
                            Log.d(TAG, "distance in meters" + distanceInMeters);
                            if (distanceInMeters > minDistanceBetweenTwoPoints) {
                                locationOfMaxRms = location;
                                locationUpdated = true;
                            }
                        } else {
                            locationOfMaxRms=location;
                            locationUpdated = true;
                        }
                    }
                });
    }

    private void calculateRMSPoint(boolean recordPoint) {

        Iterator<float[]> iterator = mTempFilterList.iterator();

        double rmsX = 0;
        double rmsY = 0;
        double rmsZ = 0;
        double rmsXYZ = 0;
        double maxRmsX = 0;
        double maxRmsY = 0;
        double maxRmsZ = 0;

        while (iterator.hasNext()) {

            float[] values = iterator.next();

            double currentApeakX = values[DATA_X];
            if (Math.abs(maxRmsX) < Math.abs(currentApeakX)) {
                maxRmsX = currentApeakX;
            }
            rmsX += Math.pow(currentApeakX, 2);

            double currentApeakY = values[DATA_Y];
            if (Math.abs(maxRmsY) < Math.abs(currentApeakY)) {
                maxRmsY = currentApeakY;
            }
            rmsY += Math.pow(currentApeakY, 2);

            double currentApeakZ = values[DATA_Z];
            if (Math.abs(maxRmsZ) < Math.abs(currentApeakZ)) {
                maxRmsZ = currentApeakZ;
            }
            rmsZ += Math.pow(currentApeakZ, 2);

            rmsXYZ += (Math.pow(values[DATA_X], 2)
                    + Math.pow(values[DATA_Y], 2)
                    + Math.pow(values[DATA_Z], 2));
        }

        rmsX = Math.sqrt(rmsX / samplesCount);
        rmsY = Math.sqrt(rmsY / samplesCount);
        rmsZ = Math.sqrt(rmsZ / samplesCount);
        rmsXYZ = Math.sqrt(rmsXYZ / samplesCount);
        Log.d("samples count ", "" + samplesCount);

        String dateFormatted = getFormattedDate(locationOfMaxRms.getTime());

        Log.d(TAG, "RMS X: " + rmsX);
        Log.d(TAG, "RMS Y: " + rmsY);
        Log.d(TAG, "RMS Z: " + rmsZ);
        Log.d(TAG, "Apeak X: " + maxRmsX);
        Log.d(TAG, "Apeak Y: " + maxRmsY);
        Log.d(TAG, "Apeak Z: " + maxRmsZ);
        Log.d(TAG, "Apeak XYZ: " + valueOfMaxRMS);
        Log.d(TAG, "value of X for Apeak XYZ: " + xValueForMaxRMS);
        Log.d(TAG, "value of Y for Apeak XYZ: " + yValueForMaxRMS);
        Log.d(TAG, "value of Z for Apeak XYZ: " + zValueForMaxRMS);
        Log.d(TAG, "RMS XYZ: " + rmsXYZ);
        Log.d("latitude",locationOfMaxRms.getLatitude()+"");
        Log.d("longitude", locationOfMaxRms.getLongitude() + "");
        Log.d("speed",locationOfMaxRms.getSpeed()+"");
        Log.d("altitude", locationOfMaxRms.getAltitude() + "");
        Log.d("time", dateFormatted + "");

        rmsTextView.setText(roundTwoDecimals(rmsXYZ));
        Log.d(TAG, "RMS XYZ rounded: " + roundTwoDecimals(rmsXYZ));
        Log.d(TAG, "RMS XYZ: " + rmsXYZ);

        String markerType;
        String comfortLevelMessage;
        if (rmsXYZ < RMS_TRACEHOLD_FIRST) {
            markerType = "greenPlacemark";
            comfortLevelMessage = "Snimljena <font color='green'>UDOBNA</font> tacka !!!";
            lightBulbImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.light_bulb_green));
        } else if (RMS_TRACEHOLD_FIRST <= rmsXYZ  &&  rmsXYZ <= RMS_TRACEHOLD_SECOND) {
            markerType = "orangePlacemark";
            comfortLevelMessage = "Snimljena <font color='yellow'>SREDNJE UDOBNA</font> tacka !!!";
            lightBulbImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.light_bulb_yellow));
        } else {
            markerType = "highlightPlacemark";
            comfortLevelMessage = "Snimljena <font color='red'>NEUDOBNA</font> tacka !!!";
            lightBulbImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.light_bulb_red));
        }

        if (recordPoint) {
            saveMeasurePoint(markerType, locationOfMaxRms, rmsX,
                    rmsY, rmsZ, rmsXYZ, valueOfMaxRMS, dateFormatted,
                    maxRmsX, maxRmsY, maxRmsZ, xValueForMaxRMS, yValueForMaxRMS, zValueForMaxRMS);
            Toast.makeText(this, Html.fromHtml(comfortLevelMessage), Toast.LENGTH_SHORT).show();
            if (saveRAWFile) {
                Log.d(TAG, "filter history count " + mFilterHistory.size());
                Iterator<float[]> filterHistoryIterator = mFilterHistory.iterator();

                while (filterHistoryIterator.hasNext()) {
                    Log.d(TAG, " last point");
                    float[] filterValues = filterHistoryIterator.next();
                    if (!filterHistoryIterator.hasNext()) {
                        mFilterHistory.remove(filterValues);
                        mCurrents[DATA_X] = filterValues[DATA_X];
                        mCurrents[DATA_Y] = filterValues[DATA_Y];
                        mCurrents[DATA_Z] = filterValues[DATA_Z];
                        mCurrents[DATA_R] = filterValues[DATA_R];
                        mCurrents[4] = (float)rmsX;
                        mCurrents[5] = (float)rmsY;
                        mCurrents[6] = (float)rmsZ;
                        mCurrents[7] = (float)rmsXYZ;
                        mCurrents[8] = (float)xValueForMaxRMS;
                        mCurrents[9] = (float)yValueForMaxRMS;
                        mCurrents[10] = (float)zValueForMaxRMS;
                        mCurrents[11] = (float)valueOfMaxRMS;
                        mCurrents[12] = (float)maxRmsX;
                        mCurrents[13] = (float)maxRmsY;
                        mCurrents[14] = (float)maxRmsZ;
                        mFilterHistory.add(mCurrents.clone());
                        Log.d(TAG, "Added last point");
                        for (int i = 4; i < 15; i++) {
                            mCurrents[i] = Constants.INVALID_VALUE;
                        }
                    }
                }

                fileUtils.appendResultsToCsvFile(rawFileOutputStream, mFilterHistory, mRawHistory);
            }
        }

        if (rmsXYZ < RMS_TRACEHOLD_FIRST && !measureStarted && !recordPoint) {
            if (deviceOrientationChanged) {
                String deviceOrientationChangedMessage = "Orijentacija uredjaja je promenjena u ";
                if (deviceOrientation == HORIZONTAL_DEVICE_ORIENTATION) {
                    deviceOrientationChangedMessage = deviceOrientationChangedMessage + "VERTIKALAN polozaj. ";
                    deviceOrientation = VERICAL_DEVICE_ORIENTATION;
                } else if (deviceOrientation == VERICAL_DEVICE_ORIENTATION) {
                    deviceOrientationChangedMessage = deviceOrientationChangedMessage + "HORIZONTALAN polozaj. ";
                    deviceOrientation = HORIZONTAL_DEVICE_ORIENTATION;
                }
                Toast.makeText(this,  deviceOrientationChangedMessage + getString(R.string.wait_for_sensor_calibration), Toast.LENGTH_LONG).show();

            } else {
                measureStarted = true;
                Log.d(TAG, "start measuring");
                String deviceOrientationMessage = " i postavljen u " + (deviceOrientation == HORIZONTAL_DEVICE_ORIENTATION ? "HORIZONTALAN polozaj. " : "VERTIKALAN polozaj. ");
                Toast.makeText(this, getString(R.string.sensor_calibrated) + deviceOrientationMessage + getString(R.string.measurement_started), Toast.LENGTH_LONG).show();
            }
        }

        mFilterHistory.clear();
        mRawHistory.clear();
        lastRMSDate = new Date();
        mTempFilterList.clear();
        valueOfMaxRMS = 0;
        xValueForMaxRMS = 0;
        yValueForMaxRMS = 0;
        zValueForMaxRMS = 0;
        samplesCount = 0;
        locationUpdated = false;
    }

    private void saveMeasurePoint(String pointStyle, Location location, double rmsX,
                                  double rmsY, double rmsZ, double rmsXYZ, double maxRmsXYZ,
                                  String dateFormatted,  double maxRmsX, double maxRmsY, double maxRmsZ,
                                  double xForApeakXYZ, double yForApeakXYZ, double zForApeakXYZ) {

        double speedInKmPerHour = (location.getSpeed() * 3600) / 1000;

        //dodaj tacku u kml
        if (saveKMLFile) {
            String kmlelement = KmlUtils.createKMLPointString(pointStyle, savedPointsCounter + 1, roundFourDecimals(rmsX),
                    roundFourDecimals(rmsY), roundFourDecimals(rmsZ), roundFourDecimals(rmsXYZ), roundFourDecimals(maxRmsXYZ),
                    dateFormatted, roundFourDecimals(speedInKmPerHour), location.getLatitude(), location.getLongitude(),
                    roundFourDecimals(location.getAltitude()), roundFourDecimals(maxRmsX), roundFourDecimals(maxRmsY),
                    roundFourDecimals(maxRmsZ), roundFourDecimals(xForApeakXYZ), roundFourDecimals(yForApeakXYZ), roundFourDecimals(zForApeakXYZ));
            fileUtils.appendResultsToKmlFile(kmlFileOutputStream, kmlelement);
        }

        // calculate average values
        if (rmsXYZ < RMS_TRACEHOLD_FIRST) {
            numberOfGreenMarkers++;
        } else if (RMS_TRACEHOLD_FIRST <= rmsXYZ  &&  rmsXYZ <= RMS_TRACEHOLD_SECOND) {
            numberOfYellowMarkers++;
        } else {
            numberOfRedMarkers++;
        }

        if (location.hasSpeed()) {
            validLocationsWithSpeed++;
            Log.d(TAG, "speed counter " + validLocationsWithSpeed);
        }

        if (location.hasAltitude()) {
            validLocationsWithAltitude++;
            Log.d(TAG, "altitude counter " + validLocationsWithAltitude);
        }

        averageRMSX += rmsX;
        averageRMSY += rmsY;
        averageRMSZ += rmsZ;
        averageRMSXYZ += rmsXYZ;
        averageMaxRMSXYZ += maxRmsXYZ;
        averageMaxRMSX += maxRmsX;
        averageMaxRMSY += maxRmsY;
        averageMaxRMSZ += maxRmsZ;
        averageX += xForApeakXYZ;
        averageY += yForApeakXYZ;
        averageZ += zForApeakXYZ;
        averageSpeed += speedInKmPerHour;
        averageAltitude += location.getAltitude();
        savedPointsCounter++;

        //save results into backup txt file
        String txtPointString = TxtFileUtils.createTxtPointString(measurementId, userId, roundFourDecimals(rmsX),
                roundFourDecimals(rmsY), roundFourDecimals(rmsZ), roundFourDecimals(rmsXYZ), roundFourDecimals(maxRmsXYZ),
                dateFormatted, roundFourDecimals(speedInKmPerHour), location.getLatitude(), location.getLongitude(),
                roundFourDecimals(maxRmsX), roundFourDecimals(maxRmsY), roundFourDecimals(maxRmsZ),
                roundFourDecimals(xForApeakXYZ), roundFourDecimals(yForApeakXYZ), roundFourDecimals(zForApeakXYZ));
        fileUtils.appendResultsToTxtFile(txtFileOutputStream, txtPointString);

        // send measure results to server
       sendPointOnServer(location, rmsX, rmsY, rmsZ, rmsXYZ, maxRmsXYZ, dateFormatted,
               maxRmsX, maxRmsY, maxRmsZ, xForApeakXYZ, yForApeakXYZ, zForApeakXYZ, speedInKmPerHour, measurementDescription, false);
    }

    private void sendPointOnServer(Location location, double rmsX,
                                   double rmsY, double rmsZ, double rmsXYZ, double maxRmsXYZ,
                                   String dateFormatted,  double maxRmsX, double maxRmsY, double maxRmsZ,
                                   double xForApeakXYZ, double yForApeakXYZ, double zForApeakXYZ, double speedInKmPerHour,
                                   String pointDescription, boolean isLastPoint) {

        if (!SessionManager.getInstance(this).isLocalUser()) {
            //posalji tacku na server
            executeLoginRequest(location, rmsX, rmsY, rmsZ, rmsXYZ,
                    maxRmsXYZ, dateFormatted, maxRmsX, maxRmsY, maxRmsZ, xForApeakXYZ,
                    yForApeakXYZ, zForApeakXYZ, speedInKmPerHour, pointDescription, isLastPoint);
        }
    }

    private void stopMeasurement() {

        if (!SessionManager.getInstance(this).isLocalUser()) {
           executeStopMeasurementRequest();
        }
    }

    //*********************************** Graph View *********************************************//

    private void startGraph() {
        if (sensor.isUnsubscribed()) {
            defineSensorListener();
        }

        if (!mGraphView.ismDrawRoop()) {
            // Vraćanje iscrtavanja grafa
            mGraphView.setmDrawRoop(true);
            mGraphView.surfaceCreated(mGraphView.getHolder());
        }
    }

    private void stopGraph() {
        // Uklanjanje sensor listener
        //	mSensorManager.unregisterListener(mSensorEventListener);
        sensor.unsubscribe();
        // Zaustavljanje iscrtavanja grafa
        mGraphView.setmDrawRoop(false);

        if (sendPointToServerSubscription != null && !sendPointToServerSubscription.isUnsubscribed()) {
            sendPointToServerSubscription.unsubscribe();
        }

        if (stopMeasurementSubscription != null && !stopMeasurementSubscription.isUnsubscribed()) {
            stopMeasurementSubscription.unsubscribe();
        }
    }

    //*********************************** Listeners **********************************************//

    private Observable<Float> seekBarChangeListener(final SeekBar seekBar) {
        return Observable.create(new Observable.OnSubscribe<Float>() {
            @Override
            public void call(final Subscriber<? super Float> subscriber) {
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        mFilterRate = (float) progress / 100;
                        subscriber.onNext(mFilterRate);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }
        });
    }

    private Subscription defineSensorListener() {
        sensor = reactiveSensor.observeSensor(Sensor.TYPE_ACCELEROMETER, mSensorDelay)
                .onBackpressureDrop()
                .retry()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .filter(ReactiveSensorEvent.filterSensorChanged())
                .subscribe(new Subscriber<ReactiveSensorEvent>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("observable",e.toString());
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(ReactiveSensorEvent reactiveSensorEvent) {
                        calculateRMS(reactiveSensorEvent);
                    }
                });

        return sensor;
    }

    private Observable<Location> getLocationUpdatesObservable() {
        return locationProvider
                .checkLocationSettings(
                        new LocationSettingsRequest.Builder()
                                .addLocationRequest(locationRequest)
                                .setAlwaysShow(true)  //Refrence: http://stackoverflow.com/questions/29824408/google-play-services-locationservices-api-new-option-never
                                .build()
                )
                .doOnNext(new Action1<LocationSettingsResult>() {
                    @Override
                    public void call(LocationSettingsResult locationSettingsResult) {
                        Status status = locationSettingsResult.getStatus();
                        if (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                            try {
                                status.startResolutionForResult(AccelerometerActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException th) {
                                Log.e("MainActivity", "Error opening settings activity.", th);
                            }
                        }
                    }
                })
                .flatMap(new Func1<LocationSettingsResult, Observable<Location>>() {
                    @Override
                    public Observable<Location> call(LocationSettingsResult locationSettingsResult) {
                        return locationProvider.getUpdatedLocation(locationRequest);
                    }
                });
    }

    //*********************************** Save History *******************************************//

    private String getKmlHeaderString() {

        String username = SessionManager.getInstance(AccelerometerActivity.this).getKeyUsername();
        String unit = measureUnit == 0 ? "G" : "m/s^2";
        String deviceOrient = deviceOrientation == 0 ? "H" : "V";
        String saveFile = "";

        if (saveKMLFile) {
            saveFile = "KML";
        }

        if (saveRAWFile) {
            if (saveFile.length() > 0) {
                saveFile = saveFile + " i CSV";
            } else {
                saveFile = "CSV";
            }
        }

        String desc = "User: " + username + "\nopis merenja: " + measurementDescription +
                "\njedinica: " + unit + "\npolozaj uredjaja: " + deviceOrient +
                "\nformati: " + saveFile + "\nminimalna distanca izmedju dve tacke: " + minDistanceBetweenTwoPoints + "m" +
                "\nminimalno vreme izmedju dve tacke: " + minTimeBetweenTwoPoints/1000 + "s";

        return  KmlUtils.getKMLStartString(measurementName, desc);
    }

    private void saveHistory() {

        // Zaustavi snimanje
        mRecording = false;
        // Zaustavi graf
        stopGraph();

        if (saveRAWFile) {
            fileUtils.finishEditingCsvFile(rawFileOutputStream);
        }

        if (saveKMLFile) {

            if (locationOfMaxRms != null) {
                Log.d(TAG, "tacke " + numberOfGreenMarkers);
                String finalKMLpoint = KmlUtils.createFinalKMLPointString(savedPointsCounter, numberOfGreenMarkers,
                        numberOfYellowMarkers, numberOfRedMarkers, roundFourDecimals(averageRMSX/savedPointsCounter),
                        roundFourDecimals(averageRMSY/savedPointsCounter), roundFourDecimals(averageRMSZ/savedPointsCounter),
                        roundFourDecimals(averageRMSXYZ/savedPointsCounter), roundFourDecimals(averageMaxRMSXYZ/savedPointsCounter),
                        roundFourDecimals(averageMaxRMSX/savedPointsCounter), roundFourDecimals(averageMaxRMSY/savedPointsCounter),
                        roundFourDecimals(averageMaxRMSZ/savedPointsCounter), roundFourDecimals(averageX/savedPointsCounter),
                        roundFourDecimals(averageY / savedPointsCounter), roundFourDecimals(averageZ / savedPointsCounter),
                        roundFourDecimals(averageSpeed / validLocationsWithSpeed), roundFourDecimals(averageAltitude / validLocationsWithAltitude),
                        locationOfMaxRms.getLatitude(), locationOfMaxRms.getLongitude());

                fileUtils.appendResultsToKmlFile(kmlFileOutputStream, finalKMLpoint);
            }

            fileUtils.finishEditingKmlFile(kmlFileOutputStream);
        }

        //send average results to server
        String averagePointDescription = "Ukupan broj snimljenih tacaka razlicitog nivoa udobnosti: " + savedPointsCounter +
                ". Ukupan broj udobnih tacaka: " + numberOfGreenMarkers + ". Ukupan broj srednje udobnih tacaka: " + numberOfYellowMarkers + ". Ukupan broj neudobnih tacaka: " + numberOfRedMarkers + ".";
        sendPointOnServer(locationOfMaxRms, averageRMSX/savedPointsCounter, averageRMSY/savedPointsCounter, averageRMSZ/savedPointsCounter,
                averageRMSXYZ/savedPointsCounter, averageMaxRMSXYZ/savedPointsCounter, getFormattedDate(locationOfMaxRms.getTime()),
                averageMaxRMSX/savedPointsCounter, averageMaxRMSY / savedPointsCounter, averageMaxRMSZ/savedPointsCounter, averageX/savedPointsCounter,
                averageY/savedPointsCounter, averageZ/savedPointsCounter, averageSpeed/validLocationsWithSpeed, averagePointDescription, true);

        if (txtFileOutputStream != null) {
            fileUtils.finishEditingTxtFile(txtFileOutputStream);
        }

        mRawHistory.clear();
        mFilterHistory.clear();
        mGraphView.clearGraphHistory();
        mTempFilterList.clear();
    }

    //********************************* Communication with sever *********************************//

    private void executeLoginRequest(Location location, double rmsX,
                                     double rmsY, double rmsZ, double rmsXYZ, double maxRmsXYZ,
                                     String dateFormatted,  double maxRmsX, double maxRmsY, double maxRmsZ,
                                     double xForApeakXYZ, double yForApeakXYZ, double zForApeakXYZ, double speedInKmPerHour,
                                     String pointDescription, final boolean isLastPoint) {

        sendPointToServerSubscription = sendPointToServerObservable(location, rmsX, rmsY, rmsZ, rmsXYZ,
                                        maxRmsXYZ, dateFormatted, maxRmsX, maxRmsY, maxRmsZ, xForApeakXYZ,
                                        yForApeakXYZ, zForApeakXYZ, speedInKmPerHour, pointDescription)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "Complete");
                        if (isLastPoint) {
                            stopMeasurement();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String s) {
                        String res = s.replaceAll("\\s+", "");
                        Log.d(TAG, res);

                    }
                });
    }

    private String setUpSendPointToServerRequest(Location location, double rmsX,
                                                 double rmsY, double rmsZ, double rmsXYZ, double maxRmsXYZ,
                                                 String dateFormatted,  double maxRmsX, double maxRmsY, double maxRmsZ,
                                                 double xForApeakXYZ, double yForApeakXYZ, double zForApeakXYZ, double speedInKmPerHour,
                                                 String pointDescription) {

        ArrayList<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair(Constants.ID_K, userId));
        postParameters.add(new BasicNameValuePair(Constants.ID_M, measurementId));
        postParameters.add(new BasicNameValuePair(Constants.RMS_X, String.valueOf(roundFourDecimals(rmsX))));
        postParameters.add(new BasicNameValuePair(Constants.RMS_Y, String.valueOf(roundFourDecimals(rmsY))));
        postParameters.add(new BasicNameValuePair(Constants.RMS_Z, String.valueOf(roundFourDecimals(rmsZ))));
           /*postParameters.add(new BasicNameValuePair(Constants.MAX_RMS_X, String.valueOf(maxRmsX)));
            postParameters.add(new BasicNameValuePair(Constants.MAX_RMS_Y, String.valueOf(maxRmsY)));
            postParameters.add(new BasicNameValuePair(Constants.MAX_RMS_Z, String.valueOf(maxRmsZ)));*/
        postParameters.add(new BasicNameValuePair(Constants.X, String.valueOf(roundFourDecimals(xForApeakXYZ))));
        postParameters.add(new BasicNameValuePair(Constants.Y, String.valueOf(roundFourDecimals(yForApeakXYZ))));
        postParameters.add(new BasicNameValuePair(Constants.Z, String.valueOf(roundFourDecimals(zForApeakXYZ))));
        postParameters.add(new BasicNameValuePair(Constants.A_RMS, String.valueOf(roundFourDecimals(rmsXYZ))));
        postParameters.add(new BasicNameValuePair(Constants.A_PEAK, String.valueOf(roundFourDecimals(maxRmsXYZ))));
        postParameters.add(new BasicNameValuePair(Constants.TIME, dateFormatted));
        postParameters.add(new BasicNameValuePair(Constants.SPEED, String.valueOf(roundFourDecimals(speedInKmPerHour))));
        postParameters.add(new BasicNameValuePair(Constants.LONGITUDE, String.valueOf(location.getLongitude())));
        postParameters.add(new BasicNameValuePair(Constants.LATITUDE, String.valueOf(location.getLatitude())));
        postParameters.add(new BasicNameValuePair(Constants.DESCRIPTION, pointDescription));

        Log.d(TAG, "URL " + UrlAddresses.AddPointURL());
        try {
            return CustomHttpClient.executeHttpPost(UrlAddresses.AddPointURL(), postParameters);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Greska prilikom slanja tacke na server", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public Observable<String> sendPointToServerObservable(final Location location, final double rmsX,
                                                          final double rmsY, final double rmsZ, final double rmsXYZ,
                                                          final double maxRmsXYZ, final String dateFormatted, final double maxRmsX,
                                                          final double maxRmsY, final double maxRmsZ, final double xForApeakXYZ,
                                                          final double yForApeakXYZ, final double zForApeakXYZ,
                                                          final double speedInKmPerHour, final String pointDescription) {

        return Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.just(setUpSendPointToServerRequest(location, rmsX, rmsY, rmsZ, rmsXYZ,
                        maxRmsXYZ, dateFormatted, maxRmsX, maxRmsY, maxRmsZ, xForApeakXYZ, yForApeakXYZ,
                        zForApeakXYZ, speedInKmPerHour, pointDescription));
            }
        });
    }

    private void executeStopMeasurementRequest() {

        stopMeasurementSubscription = stopMeasurementObservable()
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
                            Toast.makeText(AccelerometerActivity.this, "Merenje je uspesno sacuvano", Toast.LENGTH_SHORT).show();
                        }

                        finish();
                    }
                });
    }

    private String setUpStopMeasurementRequest() {

        ArrayList<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair(Constants.ID_M, measurementId));
        postParameters.add(new BasicNameValuePair(Constants.ID_K, userId));
        postParameters.add(new BasicNameValuePair(Constants.ACTION, Constants.ACTION_STOP));

        Log.d(TAG, "URL " + UrlAddresses.StopMesurementURL());
        try {
            return CustomHttpClient.executeHttpPost(UrlAddresses.StopMesurementURL(), postParameters);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Server nije trenutno dostupan...", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public Observable<String> stopMeasurementObservable() {

        return Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.just(setUpStopMeasurementRequest());
            }
        });
    }

    private static final String TAG = "AkcelerometerActivity";

    private static final int DATA_X = 0;
    private static final int DATA_Y = 1;
    private static final int DATA_Z = 2;
    private static final int DATA_R = 3;

    private static final int PASS_FILTER_RAW = 0;
    private static final int PASS_FILTER_LOW = 1;
    private static final int PASS_FILTER_HIGH = 2;

    private static final int MENU_SENSOR_DELAY = (Menu.FIRST + 1);
    private static final int MENU_SAVE = (Menu.FIRST + 2);

    private static final double RMS_TRACEHOLD_FIRST = 0.315;
    private static final double RMS_TRACEHOLD_SECOND = 0.63;
    private static final int UNIT_IN_G = 0;
    private static final int UNIT_IN_METRE_PER_SECOND_SQUARE = 1;
    private static final int HORIZONTAL_DEVICE_ORIENTATION = 0;
    private static final int VERICAL_DEVICE_ORIENTATION = 1;

    private float[] mCurrents = new float[15];
    private ConcurrentLinkedQueue<float[]> mRawHistory = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<float[]> mFilterHistory = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<float[]> mTempFilterList = new ConcurrentLinkedQueue<>();

    private TextView[] mAccValueViews = new TextView[4];
    private float[] mLowPassFilters = {0.0f, 0.0f, 0.0f, 0.0f};

    private GraphView mGraphView;
    private TextView mFilterRateView;
    private ImageView lightBulbImageView;
    private TextView rmsTextView;

    private boolean saveKMLFile = false;
    private boolean saveRAWFile = false;
    private int savedPointsCounter = 0;
    private FileUtils fileUtils;
    private FileOutputStream rawFileOutputStream;
    private FileOutputStream kmlFileOutputStream;
    private FileOutputStream txtFileOutputStream;

    private String measurementId;
    private String userId;
    private String measurementDescription;
    private String measurementName;

    // The minimum time between updates in milliseconds
   // private static final long TIME_INTERVAL_FOR_GETTING_RMS = 10 * 1000; // 10 seconds
    private Date lastRMSDate;
    private int minDistanceBetweenTwoPoints;
    private long minTimeBetweenTwoPoints;
    private boolean locationUpdated;
    private boolean getIntialGPS = false;

    private boolean measureStarted = false;
    private int samplesCount = 0;
    private int measureUnit = UNIT_IN_G;
    private int deviceOrientation = HORIZONTAL_DEVICE_ORIENTATION;
    private boolean deviceOrientationChanged = false;

    // Kasnjenje podataka predefinisan za pocetak rada na SENSOR_DELAY_UI
    // Kroz implementiranu logiku moguce ga je prome u meniju kasnije
    private int mSensorDelay = SensorManager.SENSOR_DELAY_UI;
    private int mPassFilter = PASS_FILTER_HIGH;
    private float mFilterRate = 0.1f;
    private boolean mRecording = false;

    private Subscription sendPointToServerSubscription;
    private Subscription stopMeasurementSubscription;
    private Subscription sensor;
    private ReactiveSensors reactiveSensor;
    private ReactiveLocationProvider locationProvider;
    private Observable<Location> locationUpdatesObservable;
    private Subscription updatableLocationSubscription;
    private LocationRequest locationRequest;
    private Location locationOfMaxRms;
    private double valueOfMaxRMS;
    private double xValueForMaxRMS;
    private double yValueForMaxRMS;
    private double zValueForMaxRMS;
    private static final int REQUEST_CHECK_SETTINGS = 0;

    private double averageRMSX = 0;
    private double averageRMSY = 0;
    private double averageRMSZ = 0;
    private double averageRMSXYZ = 0;
    private double averageMaxRMSXYZ = 0;
    private double averageMaxRMSX = 0;
    private double averageMaxRMSY = 0;
    private double averageMaxRMSZ = 0;
    private double averageX = 0;
    private double averageY = 0;
    private double averageZ = 0;
    private double averageSpeed = 0;
    private double averageAltitude = 0;
    private int numberOfGreenMarkers = 0;
    private int numberOfYellowMarkers = 0;
    private int numberOfRedMarkers = 0;
    private int validLocationsWithSpeed = 0;
    private int validLocationsWithAltitude = 0;
}