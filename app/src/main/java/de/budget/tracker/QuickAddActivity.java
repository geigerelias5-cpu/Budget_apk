package de.budget.tracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

/** Halbtransparentes Fenster mit Ziffernblock zum schnellen Eintragen einer Ausgabe. */
public class QuickAddActivity extends Activity {
    static final String EXTRA_CAT = "cat";

    private WebView web;
    private Bridge bridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bridge = new Bridge(this, getIntent().getStringExtra(EXTRA_CAT));
        web = Web.create(this, bridge, "quick.html", true);
        setContentView(web);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String cat = intent.getStringExtra(EXTRA_CAT);
        bridge.setStartCategory(cat);
        web.evaluateJavascript("window.reopen && reopen()", null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("window.dismiss ? dismiss() : false", value -> {
            if (!"true".equals(value)) {
                super.onBackPressed();
                overridePendingTransition(0, 0);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
