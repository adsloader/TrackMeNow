package com.example.socketio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Map;

public class MainActivity extends Activity {

    Firebase myFirebaseRef = null;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // 롤리팝의 퍼미션용
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("AAAA", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        setGPSPemission();
        initFireBase();
		setUI();
    }

    // 롤리팝을 위한 메소드
    private void setGPSPemission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);

            }
        }
    }

    private void initFireBase(){
        // Firebase가 이렇게 하라고 한다.
        Firebase.setAndroidContext(this);

        // 박모씨 아들의 파이어베이스 계정입니다.
        //myFirebaseRef = new Firebase("https://glowing-torch-2311.firebaseio.com/");
        myFirebaseRef = new Firebase("https://testandroid-d79e4.firebaseio.com/");

    }

    // firebase 개체를 설정한다.
    private void makeFireBase() {

        myFirebaseRef.child("message").setValue("파이어베이스 메시지처리입니다..");
        myFirebaseRef.child("location").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Object> newPost = (Map<String, Object>)snapshot.getValue();
                doMark( (String)newPost.get("latitude"), (String)newPost.get("longitude") );
            }
            @Override public void onCancelled(FirebaseError error) { }
        });
    }


    // 화면처리를 하는 함수
    WebView mWebView    = null;
	Button  btnTrackMe  = null;
    Button  btnObserver = null;

    @SuppressLint("JavascriptInterface")
	private void setUI(){
		mWebView = (WebView) findViewById(R.id.wbMain);
		
		mWebView.getSettings().setJavaScriptEnabled(true);
        
		// 사실 쓸 이유가 없음. 결과값을 안받으니까..
		mWebView.addJavascriptInterface(new AndroidBridge(), "android");
        
        mWebView.loadUrl("file:///android_asset/main.html");
        mWebView.setWebViewClient(new WebViewClientClass());
        mWebView.loadUrl("file:///android_asset/main.html");

		btnTrackMe = (Button)findViewById(R.id.btnTrackMe);
        btnTrackMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableButtons();
                TrackMyPoint();
            }
        });

        btnObserver = (Button)findViewById(R.id.btnObserver);
        btnObserver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableButtons();
                makeFireBase();
            }
        });

    }

    private void disableButtons(){
        btnTrackMe.setEnabled(false);
        btnObserver.setEnabled(false);
    }

    // OpenLayers 2.0을 사용한 HTML 페이지를 관리할 WebView
	private class WebViewClientClass extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
		@Override
        public void onPageFinished(WebView view, String url) {
			// 그냥 테스트용으로 찍었음
			mWebView.loadUrl("javascript:addMarker(37.556698, 126.923628)");    
        }
	}
	
	// 마커를 찍어준다. UI쓰레드 처리해야 함.
	public void doMark(String l, String l2){
		
		final String v  = l;
		final String v2 = l2;
		
		runOnUiThread(new Runnable() {
            public void run() {
                String sURL = "javascript:addMarker(" + String.format("%s,%s ", v, v2) + ")";
                mWebView.loadUrl(sURL);
            }
        });

    }
	
	// 구현한 것이 하나없음.
	class AndroidBridge {
	}

    // 내 위치를 webview에 알려주는 메소드
    public void TrackMyPoint(){
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Listener 추가하기
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                // Firebase에 위치를 저장한다.
                myFirebaseRef.child("location").child("latitude").setValue( Double.toString(lat) );
                myFirebaseRef.child("location").child("longitude").setValue(Double.toString(lng) );

                // 화면에 위치를 찍는다
                doMark(Double.toString(lat), Double.toString(lng));
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                printMessage(provider + " state is .." + status);
            }

            public void onProviderEnabled(String provider) {
                printMessage(provider + " enabled ..");
            }

            public void onProviderDisabled(String provider) {
                printMessage(provider + " disabled ..");
			}
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, locationListener);
    }

    // Toast 메시지출력
    private void printMessage(String str){
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }
}
