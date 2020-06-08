package com.edalfons.pewpewteslawear;

import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.TextView;

import androidx.wear.widget.CircularProgressLayout;

import java.util.Objects;

public class MyConfirmationActivity extends WearableActivity implements
    CircularProgressLayout.OnTimerFinishedListener, View.OnClickListener {

    private CircularProgressLayout cLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_confirmation);

        // Enables Always-on
        setAmbientEnabled();

        /* Set Cmd text */
        Intent i = getIntent();
        final TextView tv = findViewById(R.id.circular_progress_tv);
        tv.setText(i.getStringExtra(getString(R.string.commands_key_str)));

        cLayout = findViewById(R.id.circular_progress_layout);
        cLayout.setOnClickListener(this);
        cLayout.setOnTimerFinishedListener(this);

        cLayout.setTotalTime(2500);

        cLayout.startTimer();
    }

    @Override
    public void onClick(View v) {
        cLayout.stopTimer();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onTimerFinished(CircularProgressLayout layout) {
        /* Show success animation */
        Intent i = new Intent(this, ConfirmationActivity.class);
        i.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
        startActivity(i);

        /* Success vibration */
        Vibrator v = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        Objects.requireNonNull(v).vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));

        /* Return success to Home Activity */
        setResult(RESULT_OK);
        finish();
    }
}
