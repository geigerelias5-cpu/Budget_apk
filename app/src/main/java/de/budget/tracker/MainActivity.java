package de.budget.tracker;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class MainActivity extends Activity {
    private WebView web;
    private boolean firstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = Web.create(this, new Bridge(this, null), "index.html", false);
        web.setBackgroundColor(getColor(R.color.screen));
        setContentView(web);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Daten neu laden, falls über das Widget etwas eingetragen wurde
        if (!firstResume) web.evaluateJavascript("window.nativeResume && nativeResume()", null);
        firstResume = false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("window.goBack ? goBack() : false", value -> {
            if (!"true".equals(value)) super.onBackPressed();
        });
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
