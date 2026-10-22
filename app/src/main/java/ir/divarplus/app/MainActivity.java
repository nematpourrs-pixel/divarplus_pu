package ir.divarplus.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.WindowInsets;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String PREF = "divarplus_settings";
    private static final String URL_KEY = "server_url";
    private static final String RECOMMENDED_PATH = "/divar_plus/";
    private FrameLayout root;
    private WebView web;
    private String homeUrl = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(23,36,59));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
                return insets;
            });
        }
        homeUrl = getSharedPreferences(PREF,MODE_PRIVATE).getString(URL_KEY,"");
        if(homeUrl.isEmpty()) showSetup(); else showWebsite();
    }

    private TextView text(String content,int size,int color) {
        TextView v = new TextView(this);
        v.setText(content);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.RIGHT);
        return v;
    }

    private void showSetup() {
        root.removeAllViews();
        web = null;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        layout.setPadding(36,44,36,44);
        layout.setBackgroundColor(Color.rgb(248,250,253));
        TextView title = text("دیوارپلاس",30,Color.rgb(25,38,59));
        title.setTypeface(null,Typeface.BOLD);
        layout.addView(title);
        TextView desc = text("آدرس HTTPS سرور دیوارپلاس را وارد کنید. مسیر نصب شما /divar_plus/ است. این برنامه هیچ آگهی آزمایشی ندارد.",15,Color.rgb(80,95,115));
        desc.setPadding(0,24,0,25);
        layout.addView(desc);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://your-domain.ir/divar_plus/");
        if(!homeUrl.isEmpty()) input.setText(homeUrl);
        layout.addView(input,new LinearLayout.LayoutParams(-1,155));
        Button connect = new Button(this);
        connect.setText("اتصال به سرور دیوارپلاس");
        connect.setAllCaps(false);
        layout.addView(connect,new LinearLayout.LayoutParams(-1,140));
        TextView help = text("می‌توانید فقط آدرس دامنه را وارد کنید؛ مسیر /divar_plus/ خودکار تکمیل می‌شود.",12,Color.GRAY);
        help.setPadding(0,26,0,0);
        layout.addView(help);
        connect.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            Uri uri = Uri.parse(value);
            if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getHost().isEmpty() || uri.getUserInfo()!=null) {
                input.setError("لطفاً آدرس HTTPS معتبر وارد کنید.");
                return;
            }
            String path = uri.getPath();
            if(path==null || path.isEmpty() || "/".equals(path)) {
                value = "https://" + uri.getAuthority() + RECOMMENDED_PATH;
            } else if(!value.endsWith("/")) {
                value += "/";
            }
            homeUrl = value;
            getSharedPreferences(PREF,MODE_PRIVATE).edit().putString(URL_KEY,homeUrl).apply();
            showWebsite();
        });
        root.addView(layout,new FrameLayout.LayoutParams(-1,-1));
    }

    private void showWebsite() {
        root.removeAllViews();
        if(web!=null) { web.destroy(); web=null; }
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setGeolocationEnabled(false);
        if (Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(true);
        WebView.setWebContentsDebuggingEnabled(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                if (!req.isForMainFrame()) return false;
                Uri uri=req.getUrl();
                String host=uri.getHost();
                if("https".equalsIgnoreCase(uri.getScheme()) &&
                    ("divar.ir".equalsIgnoreCase(host) || "www.divar.ir".equalsIgnoreCase(host))) {
                    openExternal(uri);
                    return true;
                }
                Uri base=Uri.parse(homeUrl);
                if(!"https".equalsIgnoreCase(uri.getScheme()) ||
                   host==null || !host.equalsIgnoreCase(base.getHost()) ||
                   uri.getPort()!=base.getPort() ||
                   uri.getPath()==null || !uri.getPath().startsWith(base.getPath())) {
                    openExternal(uri);
                    return true;
                }
                return false;
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if(request.isForMainFrame()) showConnectionError();
            }
        });
        root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        web.loadUrl(homeUrl);
    }

    private void openExternal(Uri uri) {
        if(!"https".equalsIgnoreCase(uri.getScheme())) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE));
        } catch(ActivityNotFoundException e) {
            new AlertDialog.Builder(this).setMessage("برنامه‌ای برای باز کردن آگهی پیدا نشد.")
                .setPositiveButton("باشه",null).show();
        }
    }

    private void showConnectionError() {
        if(web!=null) {
            web.loadDataWithBaseURL(null,
                "<meta charset='utf-8'><body dir='rtl' style='font-family:sans-serif;padding:28px'>" +
                "<h2>خطا در اتصال به سرور</h2><p>آدرس یا وضعیت سرور دیوارپلاس را بررسی کنید.</p>" +
                "<p>با دکمه بازگشت می‌توانید آدرس سرور را تغییر دهید.</p></body>",
                "text/html","UTF-8",null);
        }
    }

    @Override public void onBackPressed() {
        if(web!=null && web.canGoBack()) { web.goBack(); return; }
        new AlertDialog.Builder(this).setTitle("دیوارپلاس")
            .setItems(new String[]{"تغییر آدرس سرور","بستن برنامه"},(dialog,choice) -> {
                if(choice==0) {
                    if(web!=null) { web.destroy(); web=null; }
                    showSetup();
                } else finish();
            }).show();
    }
    @Override protected void onDestroy() {
        if(web!=null) { web.destroy(); web=null; }
        super.onDestroy();
    }
}