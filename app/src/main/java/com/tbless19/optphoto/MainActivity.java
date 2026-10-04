package com.tbless19.optphoto;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 1001;
    private WebView web;
    private ValueCallback<Uri[]> pendingChooser;
    private Uri sharedImage;
    private boolean pageReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        // Keep the page clear of the status bar and navigation bar (edge-to-edge on Android 15+)
        web.setOnApplyWindowInsetsListener((View v, WindowInsets insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                deliverSharedImage();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (pendingChooser != null) pendingChooser.onReceiveValue(null);
                pendingChooser = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                Intent chooser = Intent.createChooser(pick, "Choose your photo");
                try {
                    startActivityForResult(chooser, PICK_IMAGE);
                } catch (Exception e) {
                    pendingChooser = null;
                    Toast.makeText(MainActivity.this, "No photo picker found on this phone.", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });

        handleIntent(getIntent());
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        deliverSharedImage();
    }

    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) sharedImage = uri;
        }
    }

    /** Hands a photo shared from Gallery to the page as a data URL. */
    private void deliverSharedImage() {
        if (!pageReady || sharedImage == null) return;
        Uri uri = sharedImage;
        sharedImage = null;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[64 * 1024];
            int n;
            while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
            String type = getContentResolver().getType(uri);
            if (type == null) type = "image/jpeg";
            String dataUrl = "data:" + type + ";base64," + Base64.encodeToString(buf.toByteArray(), Base64.NO_WRAP);
            web.evaluateJavascript("window.loadFromAndroid(" + JSONObject.quote(dataUrl) + ")", null);
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't open that photo.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE || pendingChooser == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            result = new Uri[]{data.getData()};
        }
        pendingChooser.onReceiveValue(result);
        pendingChooser = null;
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    /** Called from the page's "Save as JPG" button. */
    private class Bridge {
        @JavascriptInterface
        public void saveJpeg(String base64, String filename) {
            boolean ok;
            String msg;
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OPT Photo");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                ContentResolver cr = getContentResolver();
                Uri item = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (item == null) throw new IllegalStateException("insert failed");
                try (OutputStream out = cr.openOutputStream(item)) {
                    out.write(bytes);
                }
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                cr.update(item, values, null, null);
                ok = true;
                msg = "Saved to Photos › OPT Photo.";
            } catch (Exception e) {
                ok = false;
                msg = "Couldn't save the photo. Check that your phone has free storage and try again.";
            }
            final String js = "window.onAndroidSaved(" + ok + "," + JSONObject.quote(msg) + ")";
            runOnUiThread(() -> web.evaluateJavascript(js, null));
        }
    }
}
