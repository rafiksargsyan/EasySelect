package com.rsargsyan.easyselect.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.rsargsyan.easyselect.EasySelectTextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.rsargsyan.easyselect.demo.R.layout.activity_main);

        EasySelectTextView easySelectTextView = findViewById(R.id.easySelecDemoTextView);
        easySelectTextView.setOnSelectionCompletedCallback(selectedString ->
                Toast.makeText(MainActivity.this,
                        selectedString, Toast.LENGTH_SHORT).show());
    }
}