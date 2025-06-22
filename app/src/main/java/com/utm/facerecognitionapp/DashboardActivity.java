package com.utm.facerecognitionapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class DashboardActivity extends Activity {

    TextView txtNombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

    }
}
