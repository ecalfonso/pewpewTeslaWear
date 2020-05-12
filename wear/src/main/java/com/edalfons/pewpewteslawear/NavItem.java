package com.edalfons.pewpewteslawear;

import android.graphics.drawable.Drawable;

import androidx.wear.widget.drawer.WearableNavigationDrawerView;

class NavItem {
    private final String title;
    private final Drawable drawable;

    NavItem(String s, Drawable d) {
        title = s;
        drawable = d;
    }

    String getTitle() {
        return title;
    }

    Drawable getDrawable() {
        return drawable;
    }
}

class NavItemAdapter extends
        WearableNavigationDrawerView.WearableNavigationDrawerAdapter {
    private final NavItem[] items;

    NavItemAdapter(NavItem[] item) {
        items = item;
    }

    @Override
    public CharSequence getItemText(int pos) {
        return items[pos].getTitle();
    }

    @Override
    public Drawable getItemDrawable(int pos) {
        return items[pos].getDrawable();
    }

    @Override
    public int getCount() {
        return items.length;
    }
}
