package com.edalfons.common_code;

public class CarSelectItem {
    private final String display_name;
    private final String id_s;

    public CarSelectItem(String n, String i) {
        this.display_name = n;
        this.id_s = i;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public String getId_s() {
        return id_s;
    }
}