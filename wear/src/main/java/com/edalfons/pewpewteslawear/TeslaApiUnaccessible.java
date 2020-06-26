package com.edalfons.pewpewteslawear;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;

public class TeslaApiUnaccessible extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tesla_api_unaccessible);
    }
}
