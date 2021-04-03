package com.rsargsyan.easyselect.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.rsargsyan.easyselect.EasySelectTextView;
import com.rsargsyan.easyselect.EasySelectTextView.WordSpanningStrategy;

public class MainActivity extends AppCompatActivity {

    EasySelectTextView demoDefaultBehaviour;
    EasySelectTextView demoCustomForegroundAndWordSelection;
    EasySelectTextView demoNonDefaultInitialTextColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.rsargsyan.easyselect.demo.R.layout.activity_main);

        demoDefaultBehaviour = findViewById(R.id.demoDefaultBehaviour);
        demoDefaultBehaviour.setOnSelectionCompletedCallback(selectedString -> {
            Toast.makeText(MainActivity.this, selectedString, Toast.LENGTH_SHORT).show();
        });

        demoCustomForegroundAndWordSelection =
                findViewById(R.id.demoCustomForegroundAndWordSelection);
        demoCustomForegroundAndWordSelection.setSpanningStrategy(
                EasySelectTextView.WordSpanningStrategy.getInstance());
        demoCustomForegroundAndWordSelection.setSelectionTextHighlightColor(0);
        demoCustomForegroundAndWordSelection.setOnSelectionCompletedCallback(selectedString -> {
            Toast.makeText(MainActivity.this, selectedString, Toast.LENGTH_SHORT).show();
        });

        demoNonDefaultInitialTextColor = findViewById(R.id.demoNonDefaultInitialTextColor);
        demoNonDefaultInitialTextColor.setOnSelectionCompletedCallback(selectedString -> {
            Toast.makeText(MainActivity.this, selectedString, Toast.LENGTH_SHORT).show();
        });
    }
}