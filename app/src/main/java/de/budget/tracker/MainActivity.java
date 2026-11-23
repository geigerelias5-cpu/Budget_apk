package de.budget.tracker;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.WebView;

public class MainActivity extends Activity {
    static final int REQ_NOTIFY = 7;
    static final int REQ_IMPORT = 8;
    static final int REQ_CSV = 9;
    private WebView web;
    private boolean firstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = Web.create(this, new Bridge(this, null), "index.html", false);
        web.setBackgroundColor(getColor(R.color.screen));
        setContentView(web);
    }

    /** Ergebnis der Dateiauswahl beim Import: Text an die Webansicht geben. */
    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode != REQ_IMPORT && requestCode != REQ_CSV) || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        String text = "";
        try (java.io.InputStream in = getContentResolver().openInputStream(data.getData())) {
            if (in != null) {
                java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0 && buf.size() < 20_000_000) buf.write(b, 0, n);
                text = decode(buf.toByteArray());
            }
        } catch (Exception ignored) {
        }
        String fn = requestCode == REQ_CSV ? "csvResult" : "importResult";
        web.evaluateJavascript("window." + fn + " && " + fn + "(" + org.json.JSONObject.quote(text) + ")", null);
    }

    /** Text als UTF-8 lesen; Bank-CSVs sind oft Windows-1252 (Umlaute), dann darauf ausweichen. */
    private static String decode(byte[] bytes) {
        try {
            return java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (Exception e) {
            try {
                return new String(bytes, "windows-1252");
            } catch (Exception e2) {
                return new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_NOTIFY) return;
        boolean ok = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        Store.setLockEnabled(this, ok);
        web.evaluateJavascript("window.lockResult && lockResult(" + ok + ")", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Store.refreshAll(this);
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
