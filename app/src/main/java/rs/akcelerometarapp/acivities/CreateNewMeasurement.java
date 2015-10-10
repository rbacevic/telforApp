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
import rs.akcelerometarapp.utils.ProgressDialogUtils;

/**
 * Created by RADEEE on 10-Oct-15.
 */
public class CreateNewMeasurement extends Activity {

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

    protected void configUI() {
        progressDialog = ProgressDialogUtils.initProgressDialog(this);
        usernameTextView = (TextView)findViewById(R.id.username_text);
        measurementName = (EditText)findViewById(R.id.measurement_name);
        measurementDescription = (EditText)findViewById(R.id.measurement_description);
        startMeasurement = (Button)findViewById(R.id.start_measurement);
    }

    protected void collectData() {
        Bundle bundle = getIntent().getExtras();
        userId = bundle.getCharSequence("id").toString();
        username = bundle.getCharSequence("username").toString();
        usernameTextView.setText(username);
    }

    protected void setAllListeners () {
        startMeasurement.setOnClickListener(startMeasurementClickListener);
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

    protected void startMeasure(String measurementName) {

        /*ProgressDialogUtils.showProgressDialog(progressDialog);

        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("naziv ", measurementName));
        postParameters.add(new BasicNameValuePair("idK", userId));
        postParameters.add(new BasicNameValuePair("akcija", "start"));

        String response = null;

        try {

            response = CustomHttpClient.executeHttpPost(URLS.CreateMesurementURL(), postParameters);
            String res = response.toString();
            res= res.replaceAll("\\s+", "");

            // Prebacivanje res u integer da bi moglo da se primeni u if-u
            int rezultatPovratna = Integer.parseInt(res);

            if(rezultatPovratna > 0){
                Toast.makeText(this, "Merenje je uspesno kreirano", Toast.LENGTH_SHORT).show();

                ProgressDialogUtils.dismissProgressDialog(progressDialog);

                Intent newIntent = new Intent(this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("userID", userId);
                bundle.putString("measurementId", res);
                newIntent.putExtras(bundle);
                startActivity(newIntent);

            } else {
                ProgressDialogUtils.dismissProgressDialog(progressDialog);
                Toast.makeText(this, "Doslo je do greske prilikom kreiranja merenja, molimo vas pokusajte ponovo", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            ProgressDialogUtils.dismissProgressDialog(progressDialog);
            Toast.makeText(this, "Slaba konekcija sa internetom,pokusajte ponovo..", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }*/

        Intent newIntent = new Intent(this, MainActivity.class);
        startActivity(newIntent);
    }

    protected EditText measurementName;
    protected EditText measurementDescription;
    protected TextView usernameTextView;
    protected Button startMeasurement;
    protected ProgressDialog progressDialog;
    protected String userId;
    protected String username;

    protected static final String TAG = "CreateNewMeasurement";
}
