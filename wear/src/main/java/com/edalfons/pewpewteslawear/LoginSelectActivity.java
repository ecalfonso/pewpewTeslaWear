package com.edalfons.pewpewteslawear;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.Button;

public class LoginSelectActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_select);

        final Button credentials_button = findViewById(R.id.button);
        final Button access_token_button = findViewById(R.id.button2);

        credentials_button.setOnClickListener(v -> {
            Intent credentials_login_activity = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivity(credentials_login_activity);
        });

        access_token_button.setOnClickListener(v -> {
            Intent access_token_login_activity = new Intent(getApplicationContext(),
                    LoginActivity2.class);
            startActivity(access_token_login_activity);
        });
    }
}
