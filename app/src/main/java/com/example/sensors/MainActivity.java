package com.example.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

//import com.github.niqdev.mjpeg.DisplayMode;
//import com.github.niqdev.mjpeg.Mjpeg;
//import com.github.niqdev.mjpeg.MjpegView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //PostToDB postToDB;
    //public int c = 0;
    //@BindView(R.id.VIEW_NAME)
//    MjpegView mjpegView;

    public VideoView video;
    public EditText address;
    public int port;
    public String ip;

    private float lastX, lastY, lastZ;

    public boolean connected = false;

    private SensorManager sensorManager;
    private SensorManager getSensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    public EditText porte;
    public EditText ipade;

//    private float deltaXMax = 0;
//    private float deltaYMax = 0;
//    private float deltaZMax = 0;


    public float deltaX = 0;
    public float deltaY = 0;
    public float deltaZ = 0;

    //Thread newThread = new Thread(new ClientThre());


    public float delGX = 0;
    public float delGY = 0;
    public float delGZ = 0;

    int TIMEOUT = 5; //seconds

    public Button btn;

    private float vibrateThreshold = 0;

    private TextView currentX, currentY, currentZ, maxX, maxY, maxZ;

    private TextView gyroX, gyroY, gyroZ;

    public Vibrator v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipade = (EditText) findViewById(R.id.ipad);
        porte = (EditText) findViewById(R.id.port);
        Log.v("tag","tag");
        btn = (Button) findViewById(R.id.Start);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            vibrateThreshold = accelerometer.getMaximumRange() / 2;
        } else {
            // fail! we do not have a sensor
        }
        // initialise vibration
        getSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (getSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscope = getSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            getSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        btn.setOnClickListener(connectListener);



        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void loadipcam(String adds){
        /*Tried this but this aint working as it sends a single request for the page*/
        Uri uri =Uri.parse(adds);
        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();

// Begin customizing
// set toolbar colors
        intentBuilder.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        intentBuilder.setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

// set start and exit animations
//        intentBuilder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
//        intentBuilder.setExitAnimations(this, android.R.anim.slide_in_left,
//                android.R.anim.slide_out_right);

// build custom tabs intent
        CustomTabsIntent customTabsIntent = intentBuilder.build();

// launch the url
        customTabsIntent.launchUrl(this, uri);

    }




    public class ClientThre implements Runnable {
        PrintWriter out;
        Socket socket;

        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(ip);
                socket = new Socket(serverAddr, port);
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                Log.v("INSIDE", "sending sensor data to remote host");
                JSONObject json;
                while (true) {
                    json=new JSONObject();
                    try {
                        json.put("AX", deltaX);
                        json.put("AY", deltaY);
                        json.put("AZ", deltaZ);
                        json.put("GX", delGX);
                        json.put("GY", delGY);
                        json.put("GZ", delGZ);
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                    }

                    out.printf(json.toString());
                    out.flush();
                    Thread.sleep(2);
                    if (!connected) {
                        break;
                    }
                }
            } catch (Exception e) {

                e.printStackTrace();
            } finally {
                try {
                    Log.v("socket", "closed");
                    socket.close();
                } catch (Exception e) {
                    Log.v("end of sensor thread", "reached");
                    e.printStackTrace();
                }
            }

        }
    }


    private Button.OnClickListener connectListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!connected) {
                ip = ipade.getText().toString();
                port = Integer.parseInt(porte.getText().toString());
                if (!ip.equals("")) {
                    //connectPhones.setText("Stop Streaming");
                    Thread cThread = new Thread(new ClientThre());
                    cThread.start();
                    connected = true;
                }
                address = (EditText) findViewById(R.id.StreamAddress);
                String adds = address.getText().toString();
                String address = "http://" + adds;
                Log.v("address",address);
                //loadipcam now opens a custom chrome tab
                loadipcam(address);

            } else {
                //connectPhones.setText("Start Streaming");
                connected = false;
                Log.v("button", "closed " + connected);
                // acc_disp=false;
            }
        }
    };


    //register the sensor onResume
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        getSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // unregister in onPause

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        getSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //clean current values
        //displayCleanValues();
        // display the current values
        //displayCurrentValues();
        // display the max values
        //displayMaxValues();
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // get the change in x,y,z values of the accelerometer
            deltaX =sensorEvent.values[0];
            deltaY =sensorEvent.values[1];
            deltaZ =sensorEvent.values[2];

        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            delGX = sensorEvent.values[0];
            delGY = sensorEvent.values[1];
            delGZ = sensorEvent.values[2];
        }
        // if the change is below 2, it is just plain noise

//        if (deltaX < 2)
//            deltaX = 0;
//        if (deltaY < 2)
//            deltaY = 0;
        //if ((deltaX > vibrateThreshold) || (deltaY > vibrateThreshold) || (deltaZ > vibrateThreshold))
        //v.vibrate(600);
    }

}






