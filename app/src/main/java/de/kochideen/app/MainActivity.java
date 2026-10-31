package de.kochideen.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Die Oberfläche der App liegt in assets/index.html (HTML/JS).
 * Diese Activity stellt bereit:
 *  - Kamera und Galerie für Fotos (über den Dateiauswahl-Dialog der WebView)
 *  - eine Brücke "Android.post(...)", die die KI-Dienste (Gemini / Claude) nativ aufruft
 */
public class MainActivity extends Activity {

    private static final int REQ_CAMERA = 101;
    private static final int REQ_GALLERY = 102;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clearOldPhotos();

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, u));
                    } catch (Exception ignored) { }
                    return true;
                }
                return false;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                if (params.isCaptureEnabled()) {
                    openCamera();
                } else {
                    openGallery();
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    private void clearOldPhotos() {
        File dir = new File(getCacheDir(), "images");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    private void openCamera() {
        try {
            File dir = new File(getCacheDir(), "images");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File f = new File(dir, "foto_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            i.setClipData(ClipData.newRawUri("", cameraUri));
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i, REQ_CAMERA);
        } catch (Exception e) {
            Toast.makeText(this, "Kamera konnte nicht geöffnet werden", Toast.LENGTH_LONG).show();
            finishFileChooser(null);
        }
    }

    private void openGallery() {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(i, "Fotos auswählen"), REQ_GALLERY);
        } catch (Exception e) {
            Toast.makeText(this, "Galerie konnte nicht geöffnet werden", Toast.LENGTH_LONG).show();
            finishFileChooser(null);
        }
    }

    private void finishFileChooser(Uri[] result) {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_CAMERA && requestCode != REQ_GALLERY) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_CAMERA) {
                if (cameraUri != null) result = new Uri[]{cameraUri};
            } else if (data != null) {
                ClipData cd = data.getClipData();
                if (cd != null && cd.getItemCount() > 0) {
                    result = new Uri[cd.getItemCount()];
                    for (int k = 0; k < cd.getItemCount(); k++) {
                        result[k] = cd.getItemAt(k).getUri();
                    }
                } else if (data.getData() != null) {
                    result = new Uri[]{data.getData()};
                }
            }
        }
        finishFileChooser(result);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("(window.handleBack && window.handleBack()) ? true : false", value -> {
            if (!"true".equals(value)) finish();
        });
    }

    private static String readAll(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toString(StandardCharsets.UTF_8.name());
    }

    /** Wird aus JavaScript als window.Android aufgerufen. */
    private class Bridge {

        /** Allgemeiner HTTPS-POST (für Gemini und Claude). headersJson = {"name":"wert",...} */
        @JavascriptInterface
        public void post(final String id, final String url, final String headersJson, final String body) {
            new Thread(() -> {
                int code;
                String text;
                HttpURLConnection c = null;
                try {
                    if (!url.startsWith("https://generativelanguage.googleapis.com/")
                            && !url.startsWith("https://api.anthropic.com/")) {
                        throw new Exception("Adresse nicht erlaubt");
                    }
                    c = (HttpURLConnection) new URL(url).openConnection();
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    c.setConnectTimeout(30000);
                    c.setReadTimeout(300000);
                    JSONObject h = new JSONObject(headersJson);
                    Iterator<String> keys = h.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        c.setRequestProperty(k, h.getString(k));
                    }
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    c.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream os = c.getOutputStream()) {
                        os.write(bytes);
                    }
                    code = c.getResponseCode();
                    InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    text = is == null ? "" : readAll(is);
                } catch (Exception e) {
                    code = 0;
                    text = "{\"error\":{\"message\":"
                            + JSONObject.quote("Keine Verbindung zum Server (" + e.getMessage() + ")")
                            + "}}";
                } finally {
                    if (c != null) c.disconnect();
                }
                final String b64 = Base64.encodeToString(text.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                final String js = "window.__nativeResp(" + JSONObject.quote(id) + "," + code + ",\"" + b64 + "\")";
                runOnUiThread(() -> web.evaluateJavascript(js, null));
            }).start();
        }

        @JavascriptInterface
        public void toast(final String msg) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }
    }
}
