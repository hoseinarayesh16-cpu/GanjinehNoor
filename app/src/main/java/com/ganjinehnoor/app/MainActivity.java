
package com.ganjinehnoor.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class MainActivity extends Activity {
  private TextToSpeech tts;
  private WebView web;
  private ValueCallback<Uri[]> uploadCallback;
  private static final int FILE_REQUEST = 600;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);

    web = new WebView(this);
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setAllowFileAccess(true);
    web.setWebViewClient(new WebViewClient());

    web.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onShowFileChooser(
          WebView view,
          ValueCallback<Uri[]> callback,
          FileChooserParams params) {

        if (uploadCallback != null)
          uploadCallback.onReceiveValue(null);

        uploadCallback = callback;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");

        try {
          startActivityForResult(
              Intent.createChooser(intent, "انتخاب فایل صوتی"),
              FILE_REQUEST);
        } catch (Exception e) {
          uploadCallback = null;
          callback.onReceiveValue(null);
          return false;
        }
        return true;
      }
    });

    web.addJavascriptInterface(new Object() {
      @JavascriptInterface
      public void speak(String text) {
        runOnUiThread(() -> {
          if (tts != null) {
            tts.setLanguage(new Locale("ar"));
            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "dua");
          }
        });
      }

      @JavascriptInterface
      public void fetchSurah(int n) {
        if (n < 1 || n > 114) return;

        new Thread(() -> {
          try {
            java.net.URL url = new java.net.URL(
                "https://api.alquran.cloud/v1/surah/"
                + n + "/quran-uthmani");

            java.net.HttpURLConnection c =
                (java.net.HttpURLConnection) url.openConnection();

            c.setConnectTimeout(12000);
            c.setReadTimeout(15000);

            try (java.io.InputStream in = c.getInputStream()) {
              byte[] bytes = in.readAllBytes();
              String json = new String(
                  bytes,
                  java.nio.charset.StandardCharsets.UTF_8);

              String quoted = org.json.JSONObject.quote(json);

              runOnUiThread(() ->
                  web.evaluateJavascript(
                      "onSurahData(" + n + "," + quoted + ")",
                      null));
            } finally {
              c.disconnect();
            }
          } catch (Exception e) {
            runOnUiThread(() ->
                web.evaluateJavascript(
                    "onSurahData(" + n + ",'{}')",
                    null));
          }
        }).start();
      }

      @JavascriptInterface
      public void fetchTranslation(int n) {
        if (n < 1 || n > 114) return;

        new Thread(() -> {
          try {
            java.net.URL url = new java.net.URL(
                "https://api.alquran.cloud/v1/surah/"
                + n + "/fa.makarem");

            java.net.HttpURLConnection c =
                (java.net.HttpURLConnection) url.openConnection();

            c.setConnectTimeout(12000);
            c.setReadTimeout(15000);

            try (java.io.InputStream in = c.getInputStream()) {
              byte[] bytes = in.readAllBytes();
              String json = new String(
                  bytes,
                  java.nio.charset.StandardCharsets.UTF_8);

              String quoted = org.json.JSONObject.quote(json);

              runOnUiThread(() ->
                  web.evaluateJavascript(
                      "onTranslationData(" + n + "," + quoted + ")",
                      null));
            } finally {
              c.disconnect();
            }
          } catch (Exception e) {
            runOnUiThread(() ->
                web.evaluateJavascript(
                    "onTranslationData(" + n + ",'{}')",
                    null));
          }
        }).start();
      }

      @JavascriptInterface
      public void stop() {
        runOnUiThread(() -> {
          if (tts != null) tts.stop();
        });
      }
    }, "Native");

    tts = new TextToSpeech(this, status -> {
      if (status == TextToSpeech.SUCCESS)
        tts.setLanguage(new Locale("ar"));
    });

    setContentView(web);
    web.loadUrl("file:///android_asset/index.html");
  }

  @Override
  protected void onActivityResult(
      int requestCode,
      int resultCode,
      Intent data) {

    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == FILE_REQUEST && uploadCallback != null) {
      Uri[] result =
          (resultCode == RESULT_OK
              && data != null
              && data.getData() != null)
          ? new Uri[]{data.getData()}
          : null;

      uploadCallback.onReceiveValue(result);
      uploadCallback = null;
    }
  }

  @Override
  protected void onDestroy() {
    if (tts != null) {
      tts.stop();
      tts.shutdown();
    }
    super.onDestroy();
  }
}
