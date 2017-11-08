package com.test.maptest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.test.maptest.Popupwindows.CommonPopupWindow;
import com.test.maptest.Popupwindows.CommonUtil;
import com.test.maptest.VolleyRequest.VolleyRequest;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.util.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        NavigationView.OnNavigationItemSelectedListener,
        MqttCallbackExtended,
        GoogleMap.OnInfoWindowClickListener{

    private GoogleMap mMap;
    public MqttAndroidClient mqttServer = null;
    ArrayList<Double> get_Long = new ArrayList<>();
    ArrayList<Double> get_Lat = new ArrayList<>();
    ArrayList<String> get_LoRa_ID = new ArrayList<>();
    double[] user_to_marker_distance;
    ArrayList<Boolean> trash_state = new ArrayList<>();
    double Long = 0;
    double Lat = 0;
    LatLng myLoca;
    ArrayList<Marker> mMarkers = new ArrayList<>();
    Marker singleMarker;
    ListView lvHistoryData;
    ArrayAdapter<String> adHistoryData;
    ListView lvTrashFill;
    ArrayAdapter<String> adTrashFill;
    VolleyRequest volleyRequest;
    Handler handler = new Handler();
    AlertDialog directionDialog;
    AlertDialog directionDialog2;
    LocationManager locationManager;
    LocationListener locationListener;
    Polyline polyline;
    Boolean locaReady = false;
    CommonPopupWindow popupWindow;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mqttServer.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        checkGPS();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 20, locationListener);
        volleyRequest = new VolleyRequest(this);
        String url = "http://rabbit-test.ddns.net:1880/gps";
        volleyRequest.getGPS(url, volleyCallback);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        try {
            mapFragment.getMapAsync(this);
        } catch (Exception ignored) {
        }
        mqttServer = new MqttAndroidClient(this, getString(R.string.mqttServer), Build.ID + "-" + Build.MODEL + "-" + Build.VERSION.RELEASE);
        mqttServer.setCallback(this);
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(getString(R.string.mqttUserName));
            options.setPassword(getString(R.string.mqttPassword).toCharArray());
            mqttServer.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true); // 右上角的定位功能；這行會出現紅色底線，不過仍可正常編譯執行
        mMap.getUiSettings().setZoomControlsEnabled(true);  // 右下角的放大縮小功能
        mMap.getUiSettings().setCompassEnabled(true);       // 左上角的指南針，要兩指旋轉才會出現
        mMap.getUiSettings().setMapToolbarEnabled(true);    // 右下角的導覽及開啟 Google Map功能
        mMap.setOnInfoWindowClickListener(this);

        Log.d("測試", "最高放大層級：" + mMap.getMaxZoomLevel());
        Log.d("測試", "最低放大層級：" + mMap.getMinZoomLevel());
    }

    void Mqtt_sub() throws MqttException {
        mqttServer.subscribe("rabbit-test", 0, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                //Toast.makeText(MapsActivity.this, "訂閱成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Toast.makeText(MapsActivity.this, "訂閱失敗", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_route_plan: {
                LayoutInflater inflater = LayoutInflater.from(MapsActivity.this); //LayoutInflater的目的是將自己設計xml的Layout轉成View
                View view = inflater.inflate(R.layout.trash_fill_route_dialog, null); //指定要給View表述的Layout
                lvTrashFill = (ListView) view.findViewById(R.id.lvFill_trash);
                adTrashFill = new ArrayAdapter<>(MapsActivity.this, android.R.layout.simple_list_item_1);
                lvTrashFill.setAdapter(adTrashFill);
                lvTrashFill.setOnItemClickListener(trash_select);
                adTrashFill.clear();
                List<String> fill = new ArrayList<>();
                List<Boolean> fill_check = new ArrayList<>();
                for (int i = 0; i < trash_state.size(); i++) {
                    if (trash_state.get(i)) {
                        adTrashFill.add("LoRa_ID:" + get_LoRa_ID.get(i));
                        fill.add("LoRa_ID:" + get_LoRa_ID.get(i));
                        fill_check.add(false);
                    }
                }
                final boolean[] temp_fillcheck = new boolean[fill_check.size()];
                final String[] temp_fill = fill.toArray(new String[fill.size()]);
                for (int i = 0; i < fill_check.size(); i++) {
                    temp_fillcheck[i] = fill_check.get(i);
                }

                if (adTrashFill.getCount() <= 0) {
                    adTrashFill.add("沒有垃圾桶滿喔");
                }
                adTrashFill.notifyDataSetChanged();
                directionDialog = new AlertDialog.Builder(MapsActivity.this) //宣告對話框物件，並顯示
                        .setTitle("目前已滿的垃圾桶")
                        //.setView(view)
                        .setMultiChoiceItems(temp_fill, temp_fillcheck, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                temp_fillcheck[which] = isChecked;
                            }
                        })
                        .setPositiveButton("關閉", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNegativeButton("規劃", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String res = "選了：";
                                List<String> temp = new ArrayList<>();
                                for (int i = 0; i < temp_fill.length; i++) {
                                    if (temp_fillcheck[i]) {
                                        res += temp_fill[i];
                                        temp.add(temp_fill[i]);
                                    }
                                }
                                if (res.equals("選了："))
                                    Toast.makeText(MapsActivity.this, "沒有選擇任何垃圾桶唷!", Toast.LENGTH_SHORT).show();
                                else {
                                    Toast.makeText(MapsActivity.this, res, Toast.LENGTH_SHORT).show();

                                    requestNavigation(temp);
                                }
                            }
                        }).show();
                break;
            }
            case R.id.nav_history_data: {
                LayoutInflater inflater = LayoutInflater.from(MapsActivity.this); //LayoutInflater的目的是將自己設計xml的Layout轉成View

                View view = inflater.inflate(R.layout.history_data_dialog, null); //指定要給View表述的Layout
                lvHistoryData = (ListView) view.findViewById(R.id.lvHistory_data);
                adHistoryData = new ArrayAdapter<>(MapsActivity.this, android.R.layout.simple_list_item_1);
                lvHistoryData.setAdapter(adHistoryData);
                lvHistoryData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String getID = parent.getItemAtPosition(position).toString();
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("ID",getID.split(":")[1]);
                        intent.putExtras(bundle);
                        intent.setClass(MapsActivity.this, HistoryActivity.class);
                        startActivity(intent);
                        directionDialog2.dismiss();
                    }
                });
                for (int i = 1; i < 11; i++) {
                    adHistoryData.add("LoRa_ID:" + i);
                }
                adHistoryData.add("LoRa_ID:99");
                adHistoryData.notifyDataSetChanged();
                directionDialog2 = new AlertDialog.Builder(MapsActivity.this) //宣告對話框物件，並顯示
                        .setTitle("歷史資料")
                        .setView(view)
                        .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                break;
            }
            case R.id.nav_about_us: {
                LayoutInflater inflater = LayoutInflater.from(MapsActivity.this); //LayoutInflater的目的是將自己設計xml的Layout轉成View

                View view = inflater.inflate(R.layout.about_us_dialog, null); //指定要給View表述的Layout

                new AlertDialog.Builder(MapsActivity.this) //宣告對話框物件，並顯示
                        .setTitle("關於我們")
                        .setView(view)
                        .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
                break;
            }
            case R.id.nav_contact_dev:
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:qaz1010203044@gmail.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "[Gomigo 回報]");
                intent.putExtra(Intent.EXTRA_TEXT, "\n\n\n\n--以下內容發送時請保留--\nDevice：" + Build.MODEL + "\nAndroid Version：" + Build.VERSION.RELEASE);
                startActivity(Intent.createChooser(intent, "Send Email"));
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void requestNavigation(List<String> temp) {
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(myLoca != null) {
            int[] temp_int = new int[temp.size()];
            double[] temp_dis = new double[temp.size()];
            for (int i = 0; i < temp.size(); i++) {
                temp_int[i] = Integer.parseInt(temp.get(i).split(":")[1]);
                temp_dis[i] = user_to_marker_distance[Integer.parseInt(temp.get(i).split(":")[1]) - 1];
            }

            List<Integer> sort_temp = new ArrayList<>();
            for(int i =0;i<temp.size();i++)
            {
                int index = 9999;
                double min  = 9999;
                for (int j = 0; j < temp.size(); j++) {
                    if(temp_dis[j] < min){
                        index = j;
                        min = temp_dis[j];
                    }
                }
                sort_temp.add(temp_int[index]);
                temp_dis[index] = 9999;
            }

            String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=" + myLoca.latitude + "," + myLoca.longitude + "&" +
                    "language=zh-TW&sensor=true&mode=walking&";
            String waypoints = "";
            for (int i = 0; i < temp.size(); i++) {
                int index = sort_temp.get(i) - 1;
                switch (i){
                    case 0:
                        url += "destination=" + get_Lat.get(index) + "," + get_Long.get(index) + "&";
                        break;
                    default:
                        waypoints += get_Lat.get(index) + "," + get_Long.get(index) + "|";
                        break;
                }
            }
            url+= "waypoints=" + waypoints;
            url = url.substring(0, url.length() - 1);
            volleyRequest.getDirection(url, volleyCallback);
            directionDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    void checkGPS() {
        if (!isGPSEnabled(this)) {
            Toast.makeText(MapsActivity.this, "請開始定位功能避免定位失效", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    public static boolean isGPSEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    void change_icon(String s) {
        String arraydata = "[" + s + "]";
        String ID = "", weight = "";
        JSONArray jArray = null;
        JSONObject jObject = null;
        try {
            jArray = new JSONArray(arraydata);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < jArray.length(); i++) {
            try {
                jObject = jArray.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                ID = jObject.getString("LoRa_ID");
                weight = jObject.getString("weight");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Long = get_Long.get(Integer.valueOf(ID) - 1);
        Lat = get_Lat.get(Integer.valueOf(ID) - 1);
        LatLng sydney = new LatLng(Lat, Long);

        String search = "LoRa ID : " + ID;
        int index = 0;
        for (int t = 0; t < mMarkers.size(); t++) {
            if (mMarkers.get(t).getTitle().equals(search)) {
                index = t;
                break;
            }
        }
        singleMarker = mMarkers.get(index);
        singleMarker.remove();
        mMarkers.remove(index);

        MarkerOptions addmarker = null;
        trash_state.set(index, false);
        int test = (Integer.valueOf(weight)) / 50;
        if (test >= 0 && test < 10) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_0)).snippet("Weight ：" + weight);
        } else if (test >= 10 && test <= 19) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_10)).snippet("Weight ：" + weight);
        } else if (test >= 20 && test <= 29) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_20)).snippet("Weight ：" + weight);
        } else if (test >= 30 && test <= 39) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_30)).snippet("Weight ：" + weight);
        } else if (test >= 40 && test <= 49) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_40)).snippet("Weight ：" + weight);
        } else if (test >= 50 && test <= 59) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_50)).snippet("Weight ：" + weight);
        } else if (test >= 60 && test <= 69) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_60)).snippet("Weight ：" + weight);
        } else if (test >= 70 && test <= 79) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_70)).snippet("Weight ：" + weight);
        } else if (test >= 80 && test <= 89) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_80)).snippet("Weight ：" + weight);
        } else if (test >= 90 && test < 100) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_90)).snippet("Weight ：" + weight);
        } else if (test == 100) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_100)).snippet("Weight ：" + weight);
            trash_state.set(index, true);
        }
        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
        singleMarker = mMap.addMarker(addmarker);
        mMarkers.add(index, singleMarker);
    }

    @Override
    public void connectComplete(boolean b, String s) {
        //Toast.makeText(MapsActivity.this, "連線成功", Toast.LENGTH_SHORT).show();
        try {
            Mqtt_sub();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        String data = new String(mqttMessage.getPayload());
        Log.d("MQTT Data", data);
        change_icon(data);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        //showAllPop();
    }

    public class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            // 依指定layout檔，建立地標訊息視窗View物件
            LayoutInflater inflater = LayoutInflater.from(MapsActivity.this);
            View infoWindow = inflater.inflate(R.layout.my_infowindow, null);
            // 顯示地標title
            TextView title = ((TextView) infoWindow.findViewById(R.id.txtTitle));
            title.setText(marker.getTitle());
            // 顯示地標snippet
            TextView snippet = ((TextView) infoWindow.findViewById(R.id.txtSnippet));
            snippet.setText(marker.getSnippet());
            return infoWindow;
        }
    }

    private ListView.OnItemClickListener trash_select = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!(parent.getItemAtPosition(position).toString().equals("沒有垃圾桶滿喔"))) {
                String name = parent.getItemAtPosition(position).toString().split(":")[1];
                int index = Integer.parseInt(name) - 1;
                Log.d("select matker",index + "");
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(myLoca != null) {
                    String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=" + myLoca.latitude + "," + myLoca.longitude + "&" +
                            "destination=" + get_Lat.get(index) + "," + get_Long.get(index) + "&" +
                            "language=zh-TW&" +
                            "sensor=true&" +
                            "mode=walking";
                    volleyRequest.getDirection(url, volleyCallback);
                    directionDialog.dismiss();
                }
                /* use google origin direction
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lng + ", " + lat + "&mode=w");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
                */
            }
        }
    };

    private ArrayList<LatLng> decodePoly(String encoded) {
        ArrayList<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length(), lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    private void drawPath(final ArrayList<LatLng> points) {
        Runnable r1 =  new Runnable() {
            @Override
            public void run() {
                if(polyline != null)
                    polyline.remove();
                polyline = mMap.addPolyline(new PolylineOptions().addAll(points).width(5).color(Color.BLUE));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 15.5f), 2000, null);
            }
        };
        handler.post(r1);
    }

    public VolleyRequest.VolleyCallback volleyCallback = new VolleyRequest.VolleyCallback() {
        @Override
        public void onSuccess(String label, String result) {
            switch (label) {
                case "history":
                    HistoryHandle(result);
                    break;
                case "GPS":
                    GPSHandle(result);
                    break;
                case "direction":
                    Log.d("direction",result);
                    DirectionHandle(result);
            }
        }

        private void DirectionHandle(String result) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(result);
                JSONArray routeObject = jsonObject.getJSONArray("routes");
                String polyline = routeObject.getJSONObject(0).getJSONObject("overview_polyline").getString("points");
                if (polyline.length() > 0) {
                    drawPath(decodePoly(polyline));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void GPSHandle(String result) {
            Log.d("GetData", result);
            JSONArray jArray;
            JSONObject jObject;
            try {
                jArray = new JSONArray(result);
                for (int i = 0; i < jArray.length(); i++) {
                    jObject = jArray.getJSONObject(i);
                    get_LoRa_ID.add(jObject.getString("LoRa_ID"));
                    get_Long.add(jObject.getDouble("Long"));
                    get_Lat.add(jObject.getDouble("Lat"));
                    trash_state.add(false);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                LatLng sydney = null;
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_wait);
                for (int i = 0; i < get_LoRa_ID.size(); i++) {
                    sydney = new LatLng(get_Lat.get(i), get_Long.get(i));
                    MarkerOptions addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + get_LoRa_ID.get(i)).icon(icon).snippet("Weight ：No data");
                    mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
                    singleMarker = mMap.addMarker(addmarker);
                    mMarkers.add(singleMarker);
                    Log.d("LoRa_ID", get_LoRa_ID.get(i) + "");
                }
                Log.d("test", mMarkers.get(0).getTitle());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(16));     // 放大地圖到 16 倍大
            } catch (Exception e) {
                Log.e("Get Err", e.getMessage());
            }
        }

        private void HistoryHandle(String result) {
            Log.d("GetData", result);
            ArrayList<String> hisData = new ArrayList<>();
            JSONArray jArray;
            JSONObject jObject;
            try {
                jArray = new JSONArray(result);
                for (int i = 0; i < jArray.length(); i++) {
                    jObject = jArray.getJSONObject(i);
                    String data = "LoRa_ID: " + jObject.getString("LoRa_ID") + "\nWeight： " + jObject.getString("weight") + "\nTimeStamp： " + jObject.getString("timestamp");
                    hisData.add(data);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(String error) {
            Toast.makeText(MapsActivity.this, "伺服器出錯啦!", Toast.LENGTH_SHORT).show();
            Log.e("GetError", error);
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            myLoca = new LatLng(location.getLatitude(), location.getLongitude());
            if (!locaReady) {
                Toast.makeText(MapsActivity.this, "定位資料已就緒", Toast.LENGTH_SHORT).show();
                locaReady = true;
            }

            //calc distance to each marker
            new Thread(new Runnable() {
                @Override
                public void run() {
                    user_to_marker_distance = new double[get_LoRa_ID.size()];
                    for (int i = 0; i < get_LoRa_ID.size(); i++) {
                        LatLng temp = new LatLng(get_Lat.get(i),get_Long.get(i));
                        user_to_marker_distance[i] = calcDistance(myLoca,temp);
                    }
                }
            }).start();
        }

        public double calcDistance(LatLng L1,LatLng L2){
            return Math.sqrt(Math.pow(L1.latitude - L2.latitude, 2) + Math.pow(L1.longitude - L2.longitude,2));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    public void showAllPop() {
        if (popupWindow != null && popupWindow.isShowing()) return;
        View upView = LayoutInflater.from(this).inflate(R.layout.popup_up, null);
        CommonUtil.measureWidthAndHeight(upView);
        popupWindow = new CommonPopupWindow.Builder(this)
                .setView(R.layout.popup_up)
                .setWidthAndHeight(ViewGroup.LayoutParams.MATCH_PARENT, upView.getMeasuredHeight())
                .setAnimationStyle(R.style.AnimRight)
                .setBackGroundLevel(1.0f)//取值范围0.0f-1.0f 值越小越暗
                .setViewOnclickListener(new CommonPopupWindow.ViewInterface() {
                    @Override
                    public void getChildView(View view, int layoutResId) {

                    }
                })
                .create();
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0);
    }
}