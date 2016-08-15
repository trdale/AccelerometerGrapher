package tomdale.accelerometergrapher;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;


public class MainActivity extends AppCompatActivity {

    private SensorManager sensor_manager;
    private Boolean is_running;
    private JSONArray json_array;
    private ReentrantLock lock;
    private Timer timer;

    private SensorEventListener accelerometer = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            //Log.d("x: ", String.valueOf(event.values[0]));
            //Log.d("y: ", String.valueOf(event.values[1]));
            //Log.d("z: ", String.valueOf(event.values[2]));
            JSONObject packager = new JSONObject();
            try {
                packager.put("x", event.values[0]);
                packager.put("y", event.values[1]);
                packager.put("z", event.values[2]);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            lock.lock();
            json_array.put(packager);
            lock.unlock();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensor_manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        final Button button = (Button) findViewById(R.id.button);
        is_running = false;
        json_array = new JSONArray();
        lock = new ReentrantLock(true);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (is_running){
                    button.setText("Start");
                    is_running = false;
                    sensor_manager.unregisterListener(accelerometer);
                    timer.cancel();

                } else {

                    button.setText("Stop");
                    timer = new Timer();
                    is_running = true;
                    HandlerThread handler_thread = new HandlerThread("accelerometer");
                    handler_thread.start();
                    Handler handler = new Handler(handler_thread.getLooper());
                    sensor_manager.registerListener(accelerometer, sensor_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensor_manager.SENSOR_DELAY_NORMAL, handler);

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                URL url = new URL("http://ec2-52-11-175-107.us-west-2.compute.amazonaws.com:3000/accelerometer");
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");
                                connection.setConnectTimeout(15000);
                                //connection.setDoInput(true);
                                connection.setDoOutput(true);
                                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                                lock.lock();
                                connection.setFixedLengthStreamingMode(json_array.toString().getBytes().length);
                                connection.connect();
                                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                                Log.d("array: ", json_array.toString());
                                out.write(json_array.toString().getBytes());
                                json_array = new JSONArray();
                                lock.unlock();
                                out.flush();
                                //connection.getResponseCode();
                                out.close();
                                connection.disconnect();
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (ProtocolException e) {
                                e.printStackTrace();
                            } catch (SocketTimeoutException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }, 0, 1000);
                }
            }
        });
    }

}
