package com.geansea.gszipdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button unzipButton = (Button) findViewById(R.id.unzipButton);
        if (unzipButton != null) {
            unzipButton.setOnClickListener(new JumpClickListener());
        }
        
        Button zipButton = (Button) findViewById(R.id.zipButton);
        if (zipButton != null) {
            zipButton.setOnClickListener(new JumpClickListener());
        }
    }

    private class JumpClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent jumpIntent = null;
            switch (v.getId()) {
                case R.id.unzipButton:
                    jumpIntent = new Intent(MainActivity.this, UnzipActivity.class);
                    break;
                case R.id.zipButton:
                    jumpIntent = new Intent(MainActivity.this, ZipActivity.class);
                    break;
                default:
                    break;
            }
            if (jumpIntent != null) {
                startActivity(jumpIntent);
            }
        }
    }
}
