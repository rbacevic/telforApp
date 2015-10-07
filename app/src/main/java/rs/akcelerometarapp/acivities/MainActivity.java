package rs.akcelerometarapp.acivities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
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

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rs.akcelerometarapp.R;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Akcelerometar_AP";

    private static final int DATA_R = 3;

    private static final int STATUS_START = 1;
    private static final int STATUS_STOP = 2;

    private static final int PASS_FILTER_RAW = 0;
    private static final int PASS_FILTER_LOW = 1;
    private static final int PASS_FILTER_HIGH = 2;

    private static final int MENU_SENSOR_DELAY = (Menu.FIRST + 1);
    private static final int MENU_START_SAVE = (Menu.FIRST + 2);
    private static final int MENU_SAVE = (Menu.FIRST + 3);
    private static final int MENU_END = (Menu.FIRST + 4);

    private static final int DIALOG_SAVE_PROGRESS = 0;

    private static final int SAMPLES_COUNT = 200;
    private static final double RMS_TRACEHOLD = 0.315;

    private float[] mCurrents = new float[4];
    private ConcurrentLinkedQueue<float[]> mHistory = new ConcurrentLinkedQueue<float[]>();
    private ConcurrentLinkedQueue<float[]> mRawHistory = new ConcurrentLinkedQueue<float[]>();
    private ConcurrentLinkedQueue<float[]> mFilterHistory = new ConcurrentLinkedQueue<float[]>();

    private ConcurrentLinkedQueue<float[]> mTempFilterList = new ConcurrentLinkedQueue<float[]>();

    private TextView[] mAccValueViews = new TextView[4];
    private float[] mLowPassFilters = {0.0f, 0.0f, 0.0f, 0.0f};
    private boolean[] mGraphs = {true, true, true, true};
    private int[] mAngleColors = new int[4];

    private int mBGColor;
    private int mZeroLineColor;
    private int mStringColor;

    private GraphView mGraphView;
    private TextView mFilterRateView;

    private String kmlElements = new String();
    private int kmlPointsCounter = 0;

    // Kasnjenje podataka predefinisan za pocetak rada na SENSOR_DELAY_UI
    // Kroz implementiranu logiku moguce ga je prome u meniju kasnije
    private int mSensorDelay = SensorManager.SENSOR_DELAY_UI;
    private int mMaxHistorySize;
    private boolean mDrawRoop = true;
    private int mDrawDelay = 100;
    private int mLineWidth = 2;
    private int mGraphScale = 6;
    private int mZeroLineY = 230;
    private int mZeroLineYOffset = 0;
    private float mTouchOffset;
    private int mStatus = STATUS_START;
    private int mPassFilter = PASS_FILTER_RAW;
    private float mFilterRate = 0.1f;
    private boolean mRecording = false;
    Toolbar toolbar;
    private float fReal;
    private Subscription sensor;
    private ReactiveSensors reactiveSensor;
    private ReactiveLocationProvider locationProvider;
    private Observable<Location> lastKnownLocationObservable;
    private Observable<Location> locationUpdatesObservable;
    private Subscription lastKnownLocationSubscription;
    private Subscription updatableLocationSubscription;
    LocationRequest locationRequest;
    Location locationOut;
    Location locationOfMaxRms;
    private static final int REQUEST_CHECK_SETTINGS = 0;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MainActivity.onCreate()");


        Window window = getWindow();

        // Održavanje aktivnog ekrana da ne ide u sleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Skrivanje Title-a
//		requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Setovanje layouta
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        locationProvider = new ReactiveLocationProvider(getApplicationContext());
        lastKnownLocationObservable = locationProvider.getLastKnownLocation();
        locationRequest = locationRequest();
        locationUpdatesObservable = getLocationUpdatesObservable();
        // Uzimanje frame layout-a
        FrameLayout frame = (FrameLayout) findViewById(R.id.frame);


        // Uzimanje resources-a
        Resources resources = getResources();

        // Uzimanje boja iz  resource-a
        mStringColor = resources.getColor(R.color.string);
        mBGColor = resources.getColor(R.color.background);
        mZeroLineColor = resources.getColor(R.color.zero_line);
        mAngleColors[SensorManager.DATA_X] = resources
                .getColor(R.color.accele_x);
        mAngleColors[SensorManager.DATA_Y] = resources
                .getColor(R.color.accele_y);
        mAngleColors[SensorManager.DATA_Z] = resources
                .getColor(R.color.accele_z);
        mAngleColors[DATA_R] = resources.getColor(R.color.accele_r);

        //Dodat frame layout za pregled grafa sa parametrom mGraphView i spec parametrom 0
        mGraphView = new GraphView(this);
        frame.addView(mGraphView, 0);
        //Setovan listener za check box
        CheckBox[] checkboxes = new CheckBox[4];
        checkboxes[SensorManager.DATA_X] = (CheckBox) findViewById(R.id.accele_x);
        checkboxes[SensorManager.DATA_Y] = (CheckBox) findViewById(R.id.accele_y);
        checkboxes[SensorManager.DATA_Z] = (CheckBox) findViewById(R.id.accele_z);
        checkboxes[DATA_R] = (CheckBox) findViewById(R.id.accele_r);
        for (int i = 0; i < 4; i++) {
            if (mGraphs[i]) {
                checkboxes[i].setChecked(true);
            }
            checkboxes[i]
                    .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView,
                                                     boolean isChecked) {
                            switch (buttonView.getId()) {
                                case R.id.accele_x:
                                    mGraphs[SensorManager.DATA_X] = isChecked;
                                    break;
                                case R.id.accele_y:
                                    mGraphs[SensorManager.DATA_Y] = isChecked;
                                    break;
                                case R.id.accele_z:
                                    mGraphs[SensorManager.DATA_Z] = isChecked;
                                    break;
                                case R.id.accele_r:
                                    mGraphs[DATA_R] = isChecked;
                                    break;
                            }
                        }
                    });
        }

        // Uzete vrednosti osa za TextView prikaz
        mAccValueViews[SensorManager.DATA_X] = (TextView) findViewById(R.id.accele_x_value);
        mAccValueViews[SensorManager.DATA_Y] = (TextView) findViewById(R.id.accele_y_value);
        mAccValueViews[SensorManager.DATA_Z] = (TextView) findViewById(R.id.accele_z_value);
        mAccValueViews[DATA_R] = (TextView) findViewById(R.id.accele_r_value);
        // Pass filter - registracija listener-a za radio button-e
        RadioGroup passFilterGroup = (RadioGroup) findViewById(R.id.pass_filter);
        passFilterGroup.check(R.id.pass_filter_raw);
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

    @Override
    protected void onStart() {
        Log.i(TAG, "MainActivity.onStart()");

        //Inicijalizacija
        initialize();
        lastKnownLocationSubscription = lastKnownLocationObservable
                .subscribe();




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
    protected void onDestroy() {
        Log.i(TAG, "MainActivity.onDestroy()");
        super.onDestroy();
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
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_VOLUME_UP:
                        //Inkrement prikaza dijagrama
                        mGraphScale++;
                        return true;
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        if (mGraphScale > 1) {
                            // Dekrement prikaza dijagrama
                            mGraphScale--;
                        }
                        return true;
                    case KeyEvent.KEYCODE_FOCUS:
                        if (mStatus == STATUS_START) {
                            // Stopiranje sensor listener-a
                            mStatus = STATUS_STOP;
                        } else {
                            // Restartivanje sensor listener-a
                            mStatus = STATUS_START;
                        }
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
                mTouchOffset = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                mZeroLineY += mZeroLineYOffset;
                mZeroLineYOffset = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                mZeroLineYOffset = (int) (event.getY() - mTouchOffset);
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_SENSOR_DELAY, Menu.NONE,
                R.string.sensor_delay_label).setIcon(
                android.R.drawable.ic_menu_rotate);
        menu.add(Menu.NONE, MENU_START_SAVE, Menu.NONE,
                R.string.start_save_label).setIcon(
                android.R.drawable.ic_menu_recent_history);
        menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, R.string.save_label).setIcon(
                android.R.drawable.ic_menu_save);
        menu.add(Menu.NONE, MENU_END, Menu.NONE, R.string.end_label).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mRecording) {
            menu.findItem(MENU_START_SAVE).setVisible(false);
            menu.findItem(MENU_SAVE).setVisible(true);
        } else {
            menu.findItem(MENU_START_SAVE).setVisible(true);
            menu.findItem(MENU_SAVE).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SENSOR_DELAY:
                selectSensorDelay();
                break;
            case MENU_START_SAVE:
                mRecording = true;
                locationUpdatesObservable.subscribe();
                Toast.makeText(this, R.string.start_save_msg, Toast.LENGTH_SHORT)
                        .show();
                break;
            case MENU_SAVE:
                saveHistory();
                updatableLocationSubscription.unsubscribe();
                break;
            case MENU_END:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public void initialize() {
        reactiveSensor = new ReactiveSensors(MainActivity.this);
        if (reactiveSensor.hasSensor(Sensor.TYPE_ACCELEROMETER)) {
            sensor = defineSensorListener();
        } else {
            Toast.makeText(MainActivity.this, "NIJE PRONADjEN AKCELEROMETAR!", Toast.LENGTH_SHORT).show();
        }
    }

    public Subscription defineSensorListener() {
        Subscription sensor = reactiveSensor.observeSensor(Sensor.TYPE_ACCELEROMETER, mSensorDelay)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .filter(ReactiveSensorEvent.filterSensorChanged())
                .subscribe(new Subscriber<ReactiveSensorEvent>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(ReactiveSensorEvent reactiveSensorEvent) {


                        SensorEvent event = reactiveSensorEvent.getSensorEvent();

                        for (int angle = 0; angle < 3; angle++) {
                            float value = event.values[angle];

                            // Kreiranje low-pass filtera po formuli  aX = (x*0.1)+(ax*(1-0.1))
                            mLowPassFilters[angle] = (mLowPassFilters[angle] * (1 - mFilterRate))
                                    + (value * mFilterRate);
                            // Filter
                            switch (mPassFilter) {
                                case PASS_FILTER_LOW:
/*
                                    value = mLowPassFilters[angle];

                                    break;*/
                                    //High-pass filter po formuli aX = aX-((x*0.1)+(ax*(1-0.1)))
                                case PASS_FILTER_HIGH:
                                    value -= mLowPassFilters[angle];

                                    break;
                            }
                            mCurrents[angle] = value;
                            mAccValueViews[angle].setText(String.valueOf(value));

                        }

                        if (mRecording) {
                            mRawHistory.add(event.values.clone());    // dodavanje raw signala u listu
                            mFilterHistory.add(mCurrents.clone());    // dodavanje filtiranog signala u listu
                            mTempFilterList.add(mCurrents.clone());
                        } else {
                            mTempFilterList.clear();
                        }

                        // Izracunavanje vektora akceleracije R - osa
                        @SuppressWarnings("deprecation")
                        Double dReal = new Double(Math.abs(Math.sqrt(Math.pow(
                                event.values[SensorManager.DATA_X], 2)
                                + Math.pow(event.values[SensorManager.DATA_Y], 2)
                                + Math.pow(event.values[SensorManager.DATA_Z], 2))));
                        fReal = dReal.floatValue();
                        // Kreiranje low-pass filtera
                        mLowPassFilters[DATA_R] = (mLowPassFilters[DATA_R] * (1 - mFilterRate))
                                + (fReal * mFilterRate);
                        // filter
                        switch (mPassFilter) {
                          /*  case PASS_FILTER_LOW:
                                fReal = mLowPassFilters[DATA_R];
                                break;*/
                            case PASS_FILTER_HIGH:
                                fReal -= mLowPassFilters[DATA_R];
                                break;
                        }
                        mCurrents[DATA_R] = fReal;
                        mAccValueViews[DATA_R].setText(String.valueOf(fReal));

                        //Log.d(TAG, "currents: " + mCurrents[0] + " " + mCurrents[1] + " " + mCurrents[2] + " " + mCurrents[3]);
                        synchronized (this) {
                            // History register
                            if (mHistory.size() >= mMaxHistorySize) {
                                mHistory.poll();
                            }
                            mHistory.add(mCurrents.clone());
                        }

                        // Log.d(TAG, " " + mTempFilterList.size());
                        if (mTempFilterList.size() == SAMPLES_COUNT) {

                            Iterator<float[]> iterator = mTempFilterList.iterator();

                            double rmsX = 0;
                            double rmsY = 0;
                            double rmsZ = 0;
                            double rmsXYZ = 0;
                            double maxXYZ = 0;

                            int i = 0;
                            while (iterator.hasNext()) {
                                float[] values = iterator.next();

                                rmsX += Math.pow(values[0], 2);
                                rmsY += Math.pow(values[1], 2);
                                rmsZ += Math.pow(values[2], 2);
                                double currentRmsXYZ = (Math.abs(Math.sqrt(Math.pow(values[0], 2)
                                        + Math.pow(values[1], 2)
                                        + Math.pow(values[2], 2))));
                                rmsXYZ += Math.pow(currentRmsXYZ, 2);
                                ;
                                if (currentRmsXYZ > maxXYZ) {
                                    maxXYZ = currentRmsXYZ;
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
                                                   locationOfMaxRms=location;
                                                }
                                            });


                                }

                                if (i < 10) {
                                    mTempFilterList.remove(values);
                                    Log.d(TAG, "remove first");
                                }
                                i++;
                            }

                            rmsX = Math.sqrt(rmsX / SAMPLES_COUNT);
                            rmsY = Math.sqrt(rmsY / SAMPLES_COUNT);
                            rmsZ = Math.sqrt(rmsZ / SAMPLES_COUNT);
                            rmsXYZ = Math.sqrt(rmsXYZ / SAMPLES_COUNT);

                            Date date = new Date(locationOfMaxRms.getTime());
                            java.text.DateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss:SSS");
                            String dateFormatted = dateFormatter.format(date);

                            if (rmsXYZ > RMS_TRACEHOLD) {

                                //Get GPS
                                //Create Point in KML File
                                Log.d(TAG, "RMS X: " + rmsX);
                                Log.d(TAG, "RMS Y: " + rmsY);
                                Log.d(TAG, "RMS Z: " + rmsZ);
                                Log.d(TAG, "Max RMS XYZ: " + maxXYZ);
                                Log.d(TAG, "RMS XYZ: " + rmsXYZ);
                                Log.d("latitude",locationOfMaxRms.getLatitude()+"");
                                Log.d("longitude",locationOfMaxRms.getLongitude()+"");
                                Log.d("time",dateFormatted+"");

                                createPointInKMLFile("highlightPlacemark", locationOfMaxRms, rmsX,
                                        rmsY, rmsZ, rmsXYZ, maxXYZ, dateFormatted);
                            } else {
                                createPointInKMLFile("normalPlacemark", locationOfMaxRms, rmsX,
                                        rmsY, rmsZ, rmsXYZ, maxXYZ, dateFormatted);
                            }
                        }
                    }

                });

        return sensor;

    }

    private void createPointInKMLFile(String pointStyle, Location location, Double rmsX,
                                      Double rmsY, Double rmsZ, Double rmsXYZ, Double maxRmsXYZ,
                                      String dateFormatted) {

        double speedInKmPerHour = (location.getSpeed() * 3600) / 1000;
        String kmlelement ="\t<Placemark>\n" +
                "<styleUrl>#" + pointStyle + "</styleUrl>"+
                "\t<name>" + kmlPointsCounter + "</name>\n" +
                "\t<description>"+ "<![CDATA["+
                "Speed [km/h]: " + speedInKmPerHour + "\n"+
                "RMS X: " + rmsX + "\n" +
                "RMS Y: " + rmsY + "\n" +
                "RMS Z: " + rmsZ + "\n" +
                "Max RMS XYZ: " + maxRmsXYZ + "\n" +
                "RMS XYZ: " + rmsXYZ + "\n" +
                "Time: " + dateFormatted + "\n" +
                "]]>" +
                "</description>\n" +
                "\t<Point>\n" +
                "\t\t<coordinates>"+ location.getLongitude() +","+location.getLatitude()+","+0+ "</coordinates>\n" +
                "\t</Point>\n" +
                "\t</Placemark>\n";
        kmlElements = kmlElements + kmlelement;

        kmlPointsCounter++;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            dismissDialog(DIALOG_SAVE_PROGRESS);

            Bundle data = msg.getData();
            if (data.getBoolean("success")) {
                // Inicijalizacija RAW history-a
                mRawHistory = new ConcurrentLinkedQueue<float[]>();

            }

            Toast.makeText(MainActivity.this, data.getString("msg"),
                    Toast.LENGTH_SHORT).show();

            startGraph();
        }
    };

    private void startGraph() {
        if (sensor.isUnsubscribed()) {
            defineSensorListener();
        }

        if (!mDrawRoop) {
            // Vraćanje iscrtavanja grafa
            mDrawRoop = true;
            mGraphView.surfaceCreated(mGraphView.getHolder());
        }
    }

    private void stopGraph() {
        // Uklanjanje sensor listener
        //	mSensorManager.unregisterListener(mSensorEventListener);
        sensor.unsubscribe();
        // Zaustavljanje iscrtavanja grafa
        mDrawRoop = false;
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

    private void saveHistory() {
        // Zaustavi snimanje
        mRecording = false;

        // Zaustavi graf
        stopGraph();

        //Prikaz prgresivnog dialoga iz klase Dialog
        showDialog(DIALOG_SAVE_PROGRESS);

        // Startovanje SaveThread niti
        SaveThread thread = new SaveThread();
        thread.start();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_SAVE_PROGRESS:
                ProgressDialog saveProgress = new ProgressDialog(this);
                saveProgress.setTitle("During storage");
                saveProgress.setMessage("I have to save the history");
                saveProgress.setIndeterminate(false);
                saveProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                saveProgress.setMax(100);
                saveProgress.setCancelable(false);
                saveProgress.show();
                return saveProgress;
        }
        return super.onCreateDialog(id);
    }

    private class GraphView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

        private Thread mThread;
        private SurfaceHolder mHolder;

        public GraphView(Context context) {
            super(context);

            Log.i(TAG, "GraphView.GraphView()");

            mHolder = getHolder();
            mHolder.addCallback(this);

            setFocusable(true);
            requestFocus();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            Log.i(TAG, "GraphView.surfaceChanged()");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "GraphView.surfaceCreated()");

            mDrawRoop = true;
            mThread = new Thread(this);
            mThread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "GraphView.surfaceDestroyed()");

            mDrawRoop = false;
            boolean roop = true;
            while (roop) {
                try {
                    mThread.join();
                    roop = false;
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            mThread = null;
        }

        @Override
        public void run() {
            Log.i(TAG, "GraphView.run()");

            int width = getWidth();
            mMaxHistorySize = (int) ((width - 20) / mLineWidth);

            Paint textPaint = new Paint();
            textPaint.setColor(mStringColor);
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(14);

            Paint zeroLinePaint = new Paint();
            zeroLinePaint.setColor(mZeroLineColor);
            zeroLinePaint.setAntiAlias(true);

            // Iscrtavanje X,Y,Z i R ose i kanvasa za pozadinu
            Paint[] linePaints = new Paint[4];
            for (int i = 0; i < 4; i++) {
                linePaints[i] = new Paint();
                linePaints[i].setColor(mAngleColors[i]);
                linePaints[i].setAntiAlias(true);
                linePaints[i].setStrokeWidth(2);
            }

            while (mDrawRoop) {
                Canvas canvas = mHolder.lockCanvas();

                if (canvas == null) {
                    break;
                }

                canvas.drawColor(mBGColor);

                float zeroLineY = mZeroLineY + mZeroLineYOffset;

                synchronized (mHolder) {
                    float twoLineY = zeroLineY - (20 * mGraphScale);
                    float oneLineY = zeroLineY - (10 * mGraphScale);
                    float minasOneLineY = zeroLineY + (10 * mGraphScale);
                    float minasTwoLineY = zeroLineY + (20 * mGraphScale);

                    canvas.drawText("2", 5, twoLineY + 5, zeroLinePaint);
                    canvas.drawLine(20, twoLineY, width, twoLineY,
                            zeroLinePaint);

                    canvas.drawText("1", 5, oneLineY + 5, zeroLinePaint);
                    canvas.drawLine(20, oneLineY, width, oneLineY,
                            zeroLinePaint);

                    canvas.drawText("0", 5, zeroLineY + 5, zeroLinePaint);
                    canvas.drawLine(20, zeroLineY, width, zeroLineY,
                            zeroLinePaint);

                    canvas.drawText("-1", 5, minasOneLineY + 5, zeroLinePaint);
                    canvas.drawLine(20, minasOneLineY, width, minasOneLineY,
                            zeroLinePaint);

                    canvas.drawText("-2", 5, minasTwoLineY + 5, zeroLinePaint);
                    canvas.drawLine(20, minasTwoLineY, width, minasTwoLineY,
                            zeroLinePaint);

                    if (mHistory.size() > 1) {
                        Iterator<float[]> iterator = mHistory.iterator();
                        float[] before = new float[4];
                        int x = width - mHistory.size() * mLineWidth;
                        int beforeX = x;
                        x += mLineWidth;

                        if (iterator.hasNext()) {
                            float[] history = iterator.next();
                            for (int angle = 0; angle < 4; angle++) {
                                before[angle] = zeroLineY
                                        - (history[angle] * mGraphScale);
                            }
                            while (iterator.hasNext()) {
                                history = iterator.next();
                                for (int angle = 0; angle < 4; angle++) {
                                    float startY = zeroLineY
                                            - (history[angle] * mGraphScale);
                                    float stopY = before[angle];
                                    if (mGraphs[angle]) {
                                        canvas.drawLine(x, startY, beforeX,
                                                stopY, linePaints[angle]);
                                    }
                                    before[angle] = startY;
                                }
                                beforeX = x;
                                x += mLineWidth;
                            }
                        }
                    }
                }

                mHolder.unlockCanvasAndPost(canvas);

                try {
                    Thread.sleep(mDrawDelay);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    private class SaveThread extends Thread {
        @Override
        public void run() {

            String kmlStart = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<kml xmlns='http://www.opengis.net/kml/2.2'>\n" +
                    "<Document>\n" +
                    "<name>TEST DRIVE</name> \n"+
                    "<description>DCR</description> \n"+
                    "<Style id='highlightPlacemark'> \n"+
                    "<IconStyle> \n"+
                    "<Icon> \n"+
                    "<href>http://www.google.com/mapfiles/ms/micons/earthquake.png</href> \n"+
                    "</Icon> \n"+
                    "</IconStyle> \n"+
                    "</Style> \n"+
                    "<Style id='normalPlacemark'> \n"+
                    "<IconStyle> \n"+
                    "<Icon> \n"+
                    "<href>http://maps.google.com/mapfiles/kml/paddle/blu-blank.png</href> \n"+
                    "</Icon> \n"+
                    "</IconStyle> \n"+
                    "</Style> \n";

            String kmlend = "</Document>\n</kml>";

            String kmlContent = kmlStart;
            kmlContent = kmlContent + kmlElements;
            kmlContent = kmlContent + kmlend;

            // Kreiranje datoteke u CSV formatu
            StringBuilder csvData = new StringBuilder();
            // Iterator za ConcurrentLinkedQueue<float[]> mRawHistory
            Iterator<float[]> iteratorpom = mHistory.iterator();            // uvek ima 230 odmeraka,LOSE, pomocna lista, NE KORISTI SE
            Iterator<float[]> iterator = mFilterHistory.iterator();
            Iterator<float[]> iteratorRaw = mRawHistory.iterator();

            // sinhonizacija listi da se poklapaju podaci na grafiku, sad mozda i ne treba
            int n = mHistory.size();
            int nRaw = mRawHistory.size();
            // pomera se filtrirana lista
            for (int i = 0; i < n - nRaw; i++)
                iterator.next();
            // kraj sinhonizacije

            //Iteracija kroz strukturu i formatiranje izvestaja sa zarezom i novim redom
            while (iterator.hasNext() && iteratorRaw.hasNext()) {
                float[] values = iterator.next();
                float[] valuesRaw = iteratorRaw.next();
                for (int angle = 0; angle < 3; angle++) {
                    csvData.append(String.valueOf(values[angle]));
                    if (angle < 3) {
                        csvData.append(",");
                    }
                }
                for (int angle = 0; angle < 3; angle++) {
                    csvData.append(String.valueOf(valuesRaw[angle]));
                    if (angle < 3) {
                        csvData.append(",");
                    }
                }
                csvData.append("\n");
            }
//			csvData.append(String.valueOf(mFilterHistory.size()));	// velicina filtirane liste
//			csvData.append("\n");
//			csvData.append(String.valueOf(mRawHistory.size()));		// velicina raw liste
//			csvData.append("\n");

            // Priprema za Poruku
            Message msg = new Message();
            Bundle bundle = new Bundle();

            try {
                // Kreiranje direktorijuma na SD kartici ako ne postoji
                String appName = getResources().getString(R.string.app_name);
                String dirPath = Environment.getExternalStorageDirectory()
                        .toString() + "/" + appName;
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // Snimanje u CSV datoteku
                String fileName = DateFormat
                        .format("yyyy-MM-dd-kk-mm-ss",
                                System.currentTimeMillis()).toString()
                        .concat(".csv");
                File file = new File(dirPath, fileName);
                if (file.createNewFile()) {
                    FileOutputStream fileOutputStream = new FileOutputStream(
                            file);
                    // Unos podataka
                    fileOutputStream.write(csvData.toString().getBytes());
                    fileOutputStream.close();
                }

                //Snimanje u kml datoteku
                String kmlFileName = DateFormat
                        .format("yyyy-MM-dd-kk-mm-ss",
                                System.currentTimeMillis()).toString()
                        .concat(".kml");
                File kmlFile = new File(dirPath, kmlFileName);
                if (kmlFile.createNewFile()) {
                    FileOutputStream kmlFileOutputStream = new FileOutputStream(
                            kmlFile);
                    // Unos podataka
                    kmlFileOutputStream.write(kmlContent.toString().getBytes());
                    kmlFileOutputStream.close();
                }

                // Kompletirenje unosa podataka u datoteku
                bundle.putString("msg", MainActivity.this.getResources()
                        .getString(R.string.save_complate));
                bundle.putBoolean("success", true);
            } catch (Exception e) {
//				Log.e(TAG, e.getMessage());

                // Upozorenje o neuspelom snimanju
                bundle.putString("msg", e.getMessage());
                bundle.putBoolean("success", false);
            }

            // Messaging koristi handler
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    public Observable<Float> seekBarChangeListener(final SeekBar seekBar) {
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

    public final LocationRequest locationRequest() {
        return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100);
    }

    Observable<Location> getLocationUpdatesObservable() {
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
                                status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
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
}
