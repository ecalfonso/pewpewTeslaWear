package com.edalfons.common_code;

public class CarAlertItem {
    private int drawable_id;
    private String helper_text;

    public CarAlertItem(int id, String text) {
        this.drawable_id = id;
        this.helper_text = text;
    }

    int getDrawable_id() {
        return drawable_id;
    }

    String getHelper_text() {
        return helper_text;
    }
}