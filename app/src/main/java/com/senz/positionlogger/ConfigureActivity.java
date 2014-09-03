package com.senz.positionlogger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import static android.view.View.*;


public class ConfigureActivity extends Activity {

    SharedPreferences mSP;
    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        mSP = getSharedPreferences(LoggerService.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

        findViewById(R.id.savePreferences).setOnClickListener(SavePreferenceOnClickListener);
        findViewById(R.id.serviceButton).setOnClickListener(ServiceButtonOnClickListener);

        mHandler = new Handler(Looper.myLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
        initConfigFields();
        mHandler.post(checkServiceStatus);
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(checkServiceStatus);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.configure, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    private void initConfigFields() {
        boolean autoStart = mSP.getBoolean("autoStart", true);
        int interval = mSP.getInt("interval", 15);

        ((CheckBox) findViewById(R.id.autoStart)).setChecked(autoStart);
        ((TextView) findViewById(R.id.interval)).setText(Integer.toString(interval));
        ((TextView) findViewById(R.id.userId)).setText(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
    }

    Runnable checkServiceStatus = new Runnable () {
        @Override
        public void run() {
            initServiceStatus();
            mHandler.postDelayed(checkServiceStatus, 100);
        }
    };

    private void initServiceStatus() {
        boolean started = LoggerService.started;
        String status, button;

        if (started) {
            status = "Started";
            button = "Stop Service";
        }
        else {
            status = "Stopped";
            button = "Start Service";
        }

        ((TextView) findViewById(R.id.serviceStatus)).setText(status);
        ((Button) findViewById(R.id.serviceButton)).setText(button);
    }

    private OnClickListener SavePreferenceOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            Boolean autoStart = ((CheckBox) ConfigureActivity.this.findViewById(R.id.autoStart)).isChecked();
            String intervalStr = ((TextView) ConfigureActivity.this.findViewById(R.id.interval)).getText().toString();
            String userId = ((TextView) ConfigureActivity.this.findViewById(R.id.userId)).getText().toString();
            int interval;

            try {
                interval = Integer.valueOf(intervalStr);
            }
            catch (NumberFormatException e) {
                Toast.makeText(ConfigureActivity.this, "Invalid interval value", Toast.LENGTH_SHORT).show();
                initConfigFields();
                return;
            }

            if (userId.equals("")) {
                Toast.makeText(ConfigureActivity.this, "Invalid user ID", Toast.LENGTH_SHORT).show();
                initConfigFields();
                return;
            }

            SharedPreferences.Editor editor = mSP.edit();
            editor.putBoolean("autoStart", autoStart);
            editor.putInt("interval", interval);
            editor.putString("userId", userId);
            editor.apply();
        }
    };

    private OnClickListener ServiceButtonOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            L.d("service button clicked");
            if (!LoggerService.started) {
                startService(new Intent(ConfigureActivity.this, LoggerService.class));
            }
            else {
                stopService(new Intent(ConfigureActivity.this, LoggerService.class));
            }
        }
    };

}
