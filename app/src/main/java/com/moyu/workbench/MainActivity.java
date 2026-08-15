package com.moyu.workbench;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://xiaoxuinchina.github.io/moyu-workbench/";
    private static final String HOME_HOST = "xiaoxuinchina.github.io";
    private static final String HOME_PATH_PREFIX = "/moyu-workbench/";
    private static final String MIGRATION_PREFIX = "MOYU_WORKBENCH_IMPORT:";
    private static final int LOCATION_REQUEST_CODE = 1201;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1202;

    private WebView webView;
    private ProgressBar progressBar;
    private String pendingGeoOrigin;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingMigrationJson;
    private boolean pageReady = false;
    private ValueCallback<Uri[]> filePathCallback;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main);
        webView=findViewById(R.id.webView); progressBar=findViewById(R.id.progressBar); configureWebView();
        processIntent(getIntent());
        if(savedInstanceState==null) webView.loadUrl(HOME_URL); else webView.restoreState(savedInstanceState);
    }

    @Override protected void onNewIntent(Intent intent){ super.onNewIntent(intent); setIntent(intent); processIntent(intent); }

    private void processIntent(Intent intent){
        if(intent==null||intent.getData()==null)return; Uri uri=intent.getData();
        if("moyuwb".equalsIgnoreCase(uri.getScheme()) && "import".equalsIgnoreCase(uri.getHost())){
            String source=uri.getQueryParameter("source"); if("clipboard".equals(source)) readMigrationFromClipboard();
        }
    }

    private void readMigrationFromClipboard(){
        try{
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if(cm==null||!cm.hasPrimaryClip()||cm.getPrimaryClip()==null||cm.getPrimaryClip().getItemCount()==0){Toast.makeText(this,"未检测到迁移数据",Toast.LENGTH_LONG).show();return;}
            CharSequence cs=cm.getPrimaryClip().getItemAt(0).coerceToText(this); String text=cs==null?"":cs.toString();
            if(!text.startsWith(MIGRATION_PREFIX)){Toast.makeText(this,"剪贴板中没有有效迁移数据",Toast.LENGTH_LONG).show();return;}
            pendingMigrationJson=text.substring(MIGRATION_PREFIX.length());
            if(pageReady) injectMigration();
        }catch(Exception e){Toast.makeText(this,"迁移读取失败",Toast.LENGTH_LONG).show();}
    }

    private void injectMigration(){
        if(pendingMigrationJson==null||pendingMigrationJson.isEmpty()||!pageReady)return;
        try{
            JSONObject payload=new JSONObject(pendingMigrationJson);
            JSONArray records=payload.optJSONArray("records"); if(records==null)records=new JSONArray();
            JSONObject incomingProfile=payload.optJSONObject("profile");
            if(incomingProfile==null)incomingProfile=new JSONObject().put("nickname","摸鱼人");

            final int incomingCount=records.length();
            String recordsJson=records.toString();
            String profileJson=incomingProfile.toString();

            String js="(function(){try{"+
                    "const incoming=JSON.parse("+JSONObject.quote(recordsJson)+");"+
                    "const existing=JSON.parse(localStorage.getItem('moyu-workbench-v1')||'[]');"+
                    "const merged=[];const seen=new Set();"+
                    "[...existing,...incoming].forEach(function(x){const k=String(x.id||'')+'|'+String(x.type||'')+'|'+String(x.date||'')+'|'+String(x.title||'');if(!seen.has(k)){seen.add(k);merged.push(x);}});"+
                    "localStorage.setItem('moyu-workbench-v1',JSON.stringify(merged));"+
                    "const incProfile=JSON.parse("+JSONObject.quote(profileJson)+");"+
                    "let cur={};try{cur=JSON.parse(localStorage.getItem('moyu-profile-v1')||'{}')}catch(e){}"+
                    "if(!cur.nickname||cur.nickname==='摸鱼人')cur.nickname=incProfile.nickname||'摸鱼人';"+
                    "if(!cur.avatar&&incProfile.avatar)cur.avatar=incProfile.avatar;"+
                    "localStorage.setItem('moyu-profile-v1',JSON.stringify(cur));"+
                    "return String(incoming.length);"+
                    "}catch(e){return 'ERR:'+e.message;}})();";

            pendingMigrationJson=null;
            webView.evaluateJavascript(js,value->{
                if(value!=null&&value.contains("ERR:")){
                    Toast.makeText(this,"迁移数据写入失败",Toast.LENGTH_LONG).show();
                    return;
                }
                clearMigrationClipboard();
                Toast.makeText(this,"已迁移 "+incomingCount+" 条记录",Toast.LENGTH_LONG).show();
                webView.reload();
            });
        }catch(Exception e){Toast.makeText(this,"迁移数据格式错误",Toast.LENGTH_LONG).show();}
    }

    private void clearMigrationClipboard(){
        try{
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            if(cm!=null)cm.setPrimaryClip(ClipData.newPlainText("",""));
        }catch(Exception ignored){}
    }

    @SuppressLint("SetJavaScriptEnabled") private void configureWebView(){
        WebSettings s=webView.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setGeolocationEnabled(true);
        s.setUseWideViewPort(false); s.setLoadWithOverviewMode(false); s.setTextZoom(100); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); s.setCacheMode(WebSettings.LOAD_DEFAULT); s.setJavaScriptCanOpenWindowsAutomatically(false); s.setSupportMultipleWindows(false);
        webView.setBackgroundColor(android.graphics.Color.rgb(238,249,255));
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){ Uri uri=request.getUrl(); String scheme=uri.getScheme(); String host=uri.getHost(); String path=uri.getPath();
                if("https".equalsIgnoreCase(scheme)&&HOME_HOST.equalsIgnoreCase(host)&&path!=null&&path.startsWith(HOME_PATH_PREFIX))return false;
                try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){} return true; }
            @Override public void onPageFinished(WebView view,String url){super.onPageFinished(view,url);pageReady=true;injectMigration();}
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView v,int p){progressBar.setProgress(p);progressBar.setVisibility((p>0&&p<100)?View.VISIBLE:View.GONE);}
            @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){if(hasLocationPermission())cb.invoke(origin,true,false);else{pendingGeoOrigin=origin;pendingGeoCallback=cb;requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_REQUEST_CODE);}}
            @Override public boolean onShowFileChooser(WebView webView,ValueCallback<Uri[]> filePath,FileChooserParams params){
                if(filePathCallback!=null)filePathCallback.onReceiveValue(null);
                filePathCallback=filePath;
                try{
                    Intent intent=params.createIntent();
                    startActivityForResult(intent,FILE_CHOOSER_REQUEST_CODE);
                    return true;
                }catch(Exception e){
                    filePathCallback=null;
                    Toast.makeText(MainActivity.this,"无法打开图片选择器",Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==FILE_CHOOSER_REQUEST_CODE){
            if(filePathCallback!=null){
                Uri[] result=WebChromeClient.FileChooserParams.parseResult(resultCode,data);
                filePathCallback.onReceiveValue(result);
                filePathCallback=null;
            }
        }
    }

    private boolean hasLocationPermission(){return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==LOCATION_REQUEST_CODE){boolean g=false;for(int r:results)if(r==PackageManager.PERMISSION_GRANTED){g=true;break;}if(pendingGeoCallback!=null)pendingGeoCallback.invoke(pendingGeoOrigin,g,false);pendingGeoOrigin=null;pendingGeoCallback=null;}}
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle out){if(webView!=null)webView.saveState(out);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){if(webView!=null){webView.stopLoading();webView.setWebChromeClient(null);webView.setWebViewClient(null);webView.destroy();}super.onDestroy();}
}
