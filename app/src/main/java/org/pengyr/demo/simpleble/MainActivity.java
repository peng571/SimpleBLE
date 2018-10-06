package org.pengyr.demo.simpleble;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends BaseBleActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
    }

    @Override protected void onConnectOn() {
        Log.d("Main", "onConnectOn");
        textView.setText(R.string.ble_state_connected);
    }

    @Override protected void onConnectOff() {
        Log.d("Main", "onConnectOff");
        textView.setText(R.string.ble_state_connecting);
    }
}
