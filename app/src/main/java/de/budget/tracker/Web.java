package de.budget.tracker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.webkit.WebSettings;
import android.webkit.WebView;

final class Web {
    private Web() {}

    @SuppressLint("SetJavaScriptEnabled")
    static WebView create(Activity a, Bridge bridge, String page, boolean transparent) {
        WebView w = new WebView(a);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        w.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        w.setVerticalScrollBarEnabled(false);
        w.setHorizontalScrollBarEnabled(false);
        if (transparent) w.setBackgroundColor(Color.TRANSPARENT);
        w.addJavascriptInterface(bridge, "Android");
        w.loadUrl("file:///android_asset/" + page);
        return w;
    }
}
