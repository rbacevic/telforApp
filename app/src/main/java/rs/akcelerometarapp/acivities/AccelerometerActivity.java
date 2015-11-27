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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
import rs.akcelerometarapp.network.CustomHttpClient;
import rs.akcelerometarapp.network.UrlAddresses;
import rs.akcelerometarapp.utils.FileUtils;
import rs.akcelerometarapp.utils.KmlUtils;
import rs.akcelerometarapp.utils.SessionManager;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
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
            measurementId = bundle.getString("measurementId");
            userId = bundle.getString("userID");
            measurementDescription = bundle.getString("opis");
            saveKMLFile = bundle.getBoolean("saveKML");
            saveRAWFile = bundle.getBoolean("saveRaw");
            minDistanceBetweenTwoPoints = bundle.getInt("eliminateNearPoints");
            minTimeBetweenTwoPoints = bundle.getInt("timeBetweenPoints") * 1000;
            deviceOrientation = bundle.getInt("deviceOrientation");
            measureUnit = bundle.getInt("measureUnit");
            measurementName = bundle.getString("imeMerenja");
        }

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

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        String title = getResources().getString(R.string.app_name) + " - " + username + " / " + saveFile + " / "
                + unit + " / " + deviceOrient + " / " + minTimeBetweenTwoPoints/1000 + "s / " + minDistanceBetweenTwoPoints + "m";
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);

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
        super.onBackPressed();
        if (mRecording) {
            stopMeasurement();
            if (saveKMLFile || saveRAWFile) {
                Toast.makeText(this, getString(R.string.save_complated), Toast.LENGTH_LONG).show();
                saveHistory();
            }
            updatableLocationSubscription.unsubscribe();
            finish();
        }
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
                    case KeyEvent.KEYCODE_FOCUS:
                        return true;
                    case KeyEvent.KEYCODE_CAMERA:
                        return true;
                }
                //Kada korisnik pusti neko dugme odnosno okonca akciju
            case KeyEvent.ACTION_UP:
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_VOLUME_UP:
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                    case KeyEvent.KEYCODE_FOCUS:
                    case KeyEvent.KEYCODE_CAMERA:

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
                stopMeasurement();
                if (saveRAWFile || saveKMLFile) {
                    saveHistory();
                    Toast.makeText(this, getString(R.string.save_complated), Toast.LENGTH_LONG).show();
                }
                updatableLocationSubscription.unsubscribe();
                finish();
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
            Toast.makeText(AccelerometerActivity.this, "NIJE PRONADjEN AKCELEROMETAR!", Toast.LENGTH_SHORT).show();
        }
    }

    private final LocationRequest locationRequest() {
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
        passFilterGroup = (RadioGroup) findViewById(R.id.pass_filter);
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
        rmsTextView.setText("0.00");

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

    //*********************************** Private Data *******************************************//

    private void calculateRMS(ReactiveSensorEvent reactiveSensorEvent) {

        SensorEvent event = reactiveSensorEvent.getSensorEvent();

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
            //uncomment for low pass filter data
            /*if (angle == 0) {
                mCurrents[4] = mLowPassFilters[angle];
            } else if (angle == 1) {
                mCurrents[5] = mLowPassFilters[angle];
            } else if (angle == 2) {
                mCurrents[6] = mLowPassFilters[angle];
            }*/
            mCurrents[angle] = value;
            mAccValueViews[angle].setText(roundFourDecimals(value));
        }

        // Izracunavanje vektora akceleracije R - osa
        Double dReal = Math.abs(Math.sqrt(Math.pow(mCurrents[DATA_X], 2)
                + Math.pow(mCurrents[DATA_Y], 2)
                + Math.pow(mCurrents[DATA_Z], 2)));
        fReal = dReal.floatValue();

        mCurrents[DATA_R] = fReal;
        mAccValueViews[DATA_R].setText(roundFourDecimals(fReal));

        //Log.d(TAG, "currents: " + mCurrents[0] + " " + mCurrents[1] + " " + mCurrents[2] + " " + mCurrents[3]);
        synchronized (this) {
            // History register
            mGraphView.addDataToGraphHistory(mCurrents.clone());
        }

        if (locationOfMaxRms != null) {

            if (!measureStarted && samplesCount == 0) {
                Toast.makeText(this, "Postavite uredjaj u ravnotezan polozaj", Toast.LENGTH_LONG).show();
            }

            if (mRecording) {
                if (saveRAWFile && measureStarted) {

                    if (rawFileOutputStream == null) {
                        rawFileOutputStream = fileUtils.createCSVFile(System.currentTimeMillis());
                    }

                    mRawHistory.add(event.values.clone());    // dodavanje raw signala u listu
                    mFilterHistory.add(mCurrents.clone());    // dodavanje filtiranog signala u listu
                }

                if (saveKMLFile && kmlFileOutputStream == null) {
                    kmlFileOutputStream = fileUtils.createKMLFile(System.currentTimeMillis(), getKmlHeaderString());
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
                        calculateRMSPoint(false);
                    }
                }
            }
        } else {
            //uzimanje prvog GPS-a
            if (!getIntialGPS) {
                getIntialGPS = true;
                if (lastRMSDate == null) {
                    Toast.makeText(this, "Traznje GPS-a...", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(AccelerometerActivity.this, "GPS pronadjen", Toast.LENGTH_LONG).show();
                        }

                        if (locationOfMaxRms != null && minDistanceBetweenTwoPoints > 0) {
                            float distanceInMeters = locationOfMaxRms.distanceTo(location);
                            // Log.d(TAG, "distance in meters" + distanceInMeters);
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
            if (maxRmsX < currentApeakX) {
                maxRmsX = currentApeakX;
            }
            rmsX += Math.pow(currentApeakX, 2);

            double currentApeakY = values[DATA_Y];
            if (maxRmsY < currentApeakY) {
                maxRmsY = currentApeakY;
            }
            rmsY += Math.pow(currentApeakY, 2);

            double currentApeakZ = values[DATA_Z];
            if (maxRmsZ < currentApeakZ) {
                maxRmsZ = currentApeakZ;
            }
            rmsZ += Math.pow(currentApeakZ, 2);

            rmsXYZ += (Math.pow(values[DATA_X], 2)
                    + Math.pow(values[DATA_Y], 2)
                    + Math.pow(values[DATA_Z], 2));

            Log.d(TAG, "values " + values[DATA_X] + " " + values[DATA_Y] + " " + values[DATA_Z]);
        }

        rmsX = Math.sqrt(rmsX / samplesCount);
        rmsY = Math.sqrt(rmsY / samplesCount);
        rmsZ = Math.sqrt(rmsZ / samplesCount);
        rmsXYZ = Math.sqrt(rmsXYZ / samplesCount);
        Log.d("samples count ", "" + samplesCount);

        Date date = new Date(locationOfMaxRms.getTime());
        DateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss:SSS");
        String dateFormatted = dateFormatter.format(date);

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
            createPoint(markerType, locationOfMaxRms, rmsX,
                    rmsY, rmsZ, rmsXYZ, valueOfMaxRMS, dateFormatted,
                    maxRmsX, maxRmsY, maxRmsZ, xValueForMaxRMS, yValueForMaxRMS, zValueForMaxRMS);
            Toast.makeText(this, Html.fromHtml(comfortLevelMessage), Toast.LENGTH_SHORT).show();
            if (saveRAWFile) {
                fileUtils.appendResultsToCsvFile(rawFileOutputStream, mFilterHistory, mRawHistory);
            }
        }

        if (rmsXYZ < RMS_TRACEHOLD_FIRST && !measureStarted && !recordPoint) {
            measureStarted = true;
            Log.d(TAG, "start measuring");
            Toast.makeText(this, "Uredjaj kalibirsan, pocinje merenje !!!", Toast.LENGTH_LONG).show();
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

    private void createPoint(String pointStyle, Location location, double rmsX,
                             double rmsY, double rmsZ, double rmsXYZ, double maxRmsXYZ,
                             String dateFormatted,  double maxRmsX, double maxRmsY, double maxRmsZ,
                             double xForApeakXYZ, double yForApeakXYZ, double zForApeakXYZ) {

        double speedInKmPerHour = (location.getSpeed() * 3600) / 1000;

        if (location.hasSpeed()) {
            validLocationsWithSpeed++;
            Log.d(TAG, "speed counter " + validLocationsWithSpeed);
        }

        if (location.hasAltitude()) {
            validLocationsWithAltitude++;
            Log.d(TAG, "altitude counter " + validLocationsWithAltitude);
        }

        //dodaj tacku u kml
        if (saveKMLFile) {
            String kmlelement = KmlUtils.createKMLPointString(pointStyle, kmlPointsCounter + 1, roundFourDecimals(rmsX),
                    roundFourDecimals(rmsY), roundFourDecimals(rmsZ), roundFourDecimals(rmsXYZ), roundFourDecimals(maxRmsXYZ),
                    dateFormatted, roundFourDecimals(speedInKmPerHour), location.getLatitude(), location.getLongitude(),
                    roundFourDecimals(location.getAltitude()), roundFourDecimals(maxRmsX), roundFourDecimals(maxRmsY),
                    roundFourDecimals(maxRmsZ), roundFourDecimals(xForApeakXYZ), roundFourDecimals(yForApeakXYZ), roundFourDecimals(zForApeakXYZ));
            fileUtils.appendResultsToKmlFile(kmlFileOutputStream, kmlelement);
            kmlPointsCounter++;

            if (rmsXYZ < RMS_TRACEHOLD_FIRST) {
                numberOfGreenMarkers++;
            } else if (RMS_TRACEHOLD_FIRST <= rmsXYZ  &&  rmsXYZ <= RMS_TRACEHOLD_SECOND) {
                numberOfYellowMarkers++;
            } else {
                numberOfRedMarkers++;
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
        }

        if (!SessionManager.getInstance(this).isLocalUser()) {
            //posalji tacku na server
            ArrayList<NameValuePair> postParameters = new ArrayList<>();
            postParameters.add(new BasicNameValuePair("idK", userId));
            postParameters.add(new BasicNameValuePair("idM", measurementId));
            postParameters.add(new BasicNameValuePair("rmsX", String.valueOf(rmsX)));
            postParameters.add(new BasicNameValuePair("rmsY", String.valueOf(rmsY)));
            postParameters.add(new BasicNameValuePair("rmsZ", String.valueOf(rmsZ)));
            /*postParameters.add(new BasicNameValuePair("maxRmsX", String.valueOf(maxRmsX)));
            postParameters.add(new BasicNameValuePair("maxRmsY", String.valueOf(maxRmsY)));
            postParameters.add(new BasicNameValuePair("maxRmsZ", String.valueOf(maxRmsZ)));*/
            postParameters.add(new BasicNameValuePair("x", String.valueOf(xForApeakXYZ)));
            postParameters.add(new BasicNameValuePair("y", String.valueOf(yForApeakXYZ)));
            postParameters.add(new BasicNameValuePair("z", String.valueOf(zForApeakXYZ)));
            postParameters.add(new BasicNameValuePair("aRms", String.valueOf(rmsXYZ)));
            postParameters.add(new BasicNameValuePair("aPeak", String.valueOf(maxRmsXYZ)));
            postParameters.add(new BasicNameValuePair("vreme", dateFormatted));
            postParameters.add(new BasicNameValuePair("brzina", String.valueOf(speedInKmPerHour)));
            postParameters.add(new BasicNameValuePair("longitude", String.valueOf(location.getLongitude())));
            postParameters.add(new BasicNameValuePair("latitude", String.valueOf(location.getLatitude())));
         //   postParameters.add(new BasicNameValuePair("opis", measurementDescription));

            String response = null;

            try {

                response = CustomHttpClient.executeHttpPost(UrlAddresses.AddPointURL(), postParameters);
                String res = response.toString();
                res = res.replaceAll("\\s+", "");
                Log.d(TAG, res);
            } catch (Exception e) {
                Toast.makeText(this, "Greska prilikom slanja tacke na server", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }


    }

    private void stopMeasurement() {

        if (!SessionManager.getInstance(this).isLocalUser()) {
            ArrayList<NameValuePair> postParameters = new ArrayList<>();
            postParameters.add(new BasicNameValuePair("idM", measurementId));
            postParameters.add(new BasicNameValuePair("idK", userId));
            postParameters.add(new BasicNameValuePair("akcija", "stop"));

            String response = null;

            try {

                response = CustomHttpClient.executeHttpPost(UrlAddresses.StopMesurementURL(), postParameters);
                String res = response.toString();
                res= res.replaceAll("\\s+", "");

                // Prebacivanje res u integer da bi moglo da se primeni u if-u
                int rezultatPovratna = Integer.parseInt(res);

                if(rezultatPovratna > 0){
                    Toast.makeText(this, "Merenje je uspesno sacuvano", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Server nije trenutno dostupan...", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
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
                String finalKMLpoint = KmlUtils.createFinalKMLPointString(kmlPointsCounter, numberOfGreenMarkers,
                        numberOfYellowMarkers, numberOfRedMarkers, roundFourDecimals(averageRMSX/kmlPointsCounter),
                        roundFourDecimals(averageRMSY/kmlPointsCounter), roundFourDecimals(averageRMSZ/kmlPointsCounter),
                        roundFourDecimals(averageRMSXYZ/kmlPointsCounter), roundFourDecimals(averageMaxRMSXYZ/kmlPointsCounter),
                        roundFourDecimals(averageMaxRMSX/kmlPointsCounter), roundFourDecimals(averageMaxRMSY/kmlPointsCounter),
                        roundFourDecimals(averageMaxRMSZ/kmlPointsCounter), roundFourDecimals(averageX/kmlPointsCounter),
                        roundFourDecimals(averageY / kmlPointsCounter), roundFourDecimals(averageZ / kmlPointsCounter),
                        roundFourDecimals(averageSpeed / validLocationsWithSpeed), roundFourDecimals(averageAltitude / validLocationsWithAltitude),
                        locationOfMaxRms.getLatitude(), locationOfMaxRms.getLongitude());

                fileUtils.appendResultsToKmlFile(kmlFileOutputStream, finalKMLpoint);
            }

            fileUtils.finishEditingKmlFile(kmlFileOutputStream);
        }

        mRawHistory.clear();
        mFilterHistory.clear();
        mGraphView.clearGraphHistory();
        mTempFilterList.clear();
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

    private float[] mCurrents = new float[7];
    private ConcurrentLinkedQueue<float[]> mRawHistory = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<float[]> mFilterHistory = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<float[]> mTempFilterList = new ConcurrentLinkedQueue<>();

    private TextView[] mAccValueViews = new TextView[4];
    private float[] mLowPassFilters = {0.0f, 0.0f, 0.0f, 0.0f};

    private GraphView mGraphView;
    private TextView mFilterRateView;
    private RadioGroup passFilterGroup;
    private ImageView lightBulbImageView;
    private TextView rmsTextView;

    private boolean saveKMLFile = false;
    private boolean saveRAWFile = false;
    private int kmlPointsCounter = 0;
    private FileUtils fileUtils;
    private FileOutputStream rawFileOutputStream;
    private FileOutputStream kmlFileOutputStream;

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

    // Kasnjenje podataka predefinisan za pocetak rada na SENSOR_DELAY_UI
    // Kroz implementiranu logiku moguce ga je prome u meniju kasnije
    private int mSensorDelay = SensorManager.SENSOR_DELAY_UI;
    private int mPassFilter = PASS_FILTER_HIGH;
    private float mFilterRate = 0.1f;
    private boolean mRecording = false;
    private Toolbar toolbar;
    private float fReal;

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