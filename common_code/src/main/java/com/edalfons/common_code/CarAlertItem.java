package com.edalfons.common_code;

public class CarAlertItem {
    private int drawable_id;
    private String helper_text;

    public CarAlertItem(int id, String text) {
        this.drawable_id = id;
        this.helper_text = text;
    }

    public int getDrawable_id() {
        return drawable_id;
    }

    public String getHelper_text() {
        return helper_text;
    }
}