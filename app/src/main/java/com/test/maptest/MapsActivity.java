package com.test.maptest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    public MqttAndroidClient mqttServer = null;
    RequestQueue mQueue;
    String gps_data  = "";
    ArrayList<String> get_Long = new ArrayList<>();
    ArrayList<String> get_Lat = new ArrayList<>();
    ArrayList<String> get_LoRa_ID = new ArrayList<>();
    ArrayList<Boolean> trash_state = new ArrayList<>();
    double Long = 0;
    double Lat = 0;
    ArrayList<Marker> mMarkers = new ArrayList<>();
    Marker singleMarker;
    ListView lvHistoryData;
    ArrayAdapter<String> adHistoryData;
    ListView lvTrashFill;
    ArrayAdapter<String> adTrashFill;


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
        mQueue = Volley.newRequestQueue(MapsActivity.this);
        String url = "http://rabbit-test.ddns.net:1880/gps";
        init_get(url);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        try {
            mapFragment.getMapAsync(this);
        }
        catch (Exception ignored){}
        mqttServer = new MqttAndroidClient(this, "tcp://wise-msghub.eastasia.cloudapp.azure.com:1883", Build.ID + "-" + Build.MODEL + "-" + Build.VERSION.RELEASE);
        mqttServer.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Toast.makeText(MapsActivity.this,"連線成功", Toast.LENGTH_SHORT).show();
                try {
                    Mqtt_sub();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Toast.makeText(MapsActivity.this,"已斷開連線", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                String data = new String(mqttMessage.getPayload());
                Log.d("MQTT Data" ,data);
                change_icon(data);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Toast.makeText(MapsActivity.this, "發送成功", Toast.LENGTH_SHORT).show();
            }
        });
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName("16b49341-9a95-4998-873c-3f6db31f1d99:d0153d9c-6be0-49a3-8ce9-a1b6d7dd1c82");
            options.setPassword("d4vs50nverbdu7ki73314i8sse".toCharArray());
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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true); // 右上角的定位功能；這行會出現紅色底線，不過仍可正常編譯執行
        mMap.getUiSettings().setZoomControlsEnabled(true);  // 右下角的放大縮小功能
        mMap.getUiSettings().setCompassEnabled(true);       // 左上角的指南針，要兩指旋轉才會出現
        mMap.getUiSettings().setMapToolbarEnabled(true);    // 右下角的導覽及開啟 Google Map功能

        Log.d("測試", "最高放大層級："+mMap.getMaxZoomLevel());
        Log.d("測試", "最低放大層級："+mMap.getMinZoomLevel());
    }

    void Mqtt_sub() throws MqttException {
        mqttServer.subscribe("rabbit-test", 0, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Toast.makeText(MapsActivity.this,"訂閱成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Toast.makeText(MapsActivity.this,"訂閱失敗", Toast.LENGTH_SHORT).show();
            }
        });
    }

    void init_get(String url)
    {
        StringRequest getRequest = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        gps_data = s;
                        Log.d("GetData",s);

                        JSONArray jArray = null;
                        JSONObject jObject = null;
                        try {
                            jArray = new JSONArray(gps_data);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        for (int i = 0; i < jArray.length(); i++)
                        {
                            try {
                                jObject = jArray.getJSONObject(i);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                get_LoRa_ID.add(jObject.getString("LoRa_ID"));
                                get_Long.add(jObject.getString("Long"));
                                get_Lat.add(jObject.getString("Lat"));
                                trash_state.add(false);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        try {
                            LatLng sydney = null;
                            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_wait);
                            for (int i = 0; i < get_LoRa_ID.size(); i++) {
                                Long = Double.parseDouble(get_Long.get(i));
                                Lat = Double.parseDouble(get_Lat.get(i));
                                sydney = new LatLng(Long, Lat);
                                MarkerOptions addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + get_LoRa_ID.get(i)).icon(icon).snippet("Ping ：No data\nWeight ：No data");
                                mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
                                singleMarker = mMap.addMarker(addmarker);
                                mMarkers.add(singleMarker);
                                Log.d("LoRa_ID", get_LoRa_ID.get(i) + "");
                            }
                            Log.d("test",mMarkers.get(0).getTitle());
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(16));     // 放大地圖到 16 倍大
                        }
                        catch (Exception e)
                        {
                            Log.e("Get Err" , e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(MapsActivity.this,"伺服器出錯啦!", Toast.LENGTH_SHORT).show();
                        Log.e("GetError",volleyError.toString());
                    }
                });
        mQueue.add(getRequest);
    }

    void get_all(String url)
    {
        StringRequest getRequest = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        String all_data = s;
                        Log.d("GetData",s);
                        adHistoryData.clear();
                        JSONArray jArray = null;
                        JSONObject jObject = null;
                        try {
                            jArray = new JSONArray(all_data);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        for (int i = 0; i < jArray.length(); i++)
                        {
                            try {
                                jObject = jArray.getJSONObject(i);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                String data = "LoRa_ID: " + jObject.getString("LoRa_ID") + "\nPing： " + jObject.getString("ping") + "\nWeight： " + jObject.getString("weight") + "\nTimeStamp： " + jObject.getString("timestamp");
                                adHistoryData.add(data);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if( jArray.length()<= 0 )
                        {
                            adHistoryData.add("無歷史資料");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(MapsActivity.this,"伺服器出錯啦!", Toast.LENGTH_SHORT).show();
                        Log.e("GetError",volleyError.toString());
                    }
                });
        mQueue.add(getRequest);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_route_plan) {
            LayoutInflater inflater = LayoutInflater.from(MapsActivity.this); //LayoutInflater的目的是將自己設計xml的Layout轉成View
            View view = inflater.inflate(R.layout.trash_fill_route_dialog, null); //指定要給View表述的Layout
            lvTrashFill = (ListView) view.findViewById(R.id.lvFill_trash);
            adTrashFill = new ArrayAdapter(MapsActivity.this, android.R.layout.simple_list_item_1);
            lvTrashFill.setAdapter(adTrashFill);
            lvTrashFill.setOnItemClickListener(trash_select);
            adTrashFill.clear();
            for (int i = 0; i < trash_state.size() ; i++) {
                if(trash_state.get(i))
                {
                    adTrashFill.add("LoRa_ID： " + get_LoRa_ID.get(i));
                }
            }
            if(adTrashFill.getCount() <= 0 )
            {
                adTrashFill.add("沒有垃圾桶滿喔");
            }
            adTrashFill.notifyDataSetChanged();
            new AlertDialog.Builder(MapsActivity.this) //宣告對話框物件，並顯示
                    .setTitle("目前已滿的垃圾桶")
                    .setView(view)
                    .setPositiveButton("關閉", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();

        } else if (id == R.id.nav_history_data) {
            LayoutInflater inflater = LayoutInflater.from(MapsActivity.this); //LayoutInflater的目的是將自己設計xml的Layout轉成View
            View view = inflater.inflate(R.layout.history_data_dialog, null); //指定要給View表述的Layout
            lvHistoryData = (ListView) view.findViewById(R.id.lvHistory_data);
            adHistoryData = new ArrayAdapter(MapsActivity.this, android.R.layout.simple_list_item_1);
            lvHistoryData.setAdapter(adHistoryData);
            String url = "http://rabbit-mqtt.ddns.net:1880/mongodata";
            get_all(url);
            adHistoryData.notifyDataSetChanged();
            new AlertDialog.Builder(MapsActivity.this) //宣告對話框物件，並顯示
                    .setTitle("歷史資料")
                    .setView(view)
                    .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
        } else if (id == R.id.nav_about_us) {
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
        }
        else if (id == R.id.nav_contact_dev)
        {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:qaz1010203044@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "[Gomigo 回報]");
            intent.putExtra(Intent.EXTRA_TEXT,"\n\n\n\n--以下內容發送時請保留--\nDevice：" + Build.MODEL +"\nAndroid Version：" + Build.VERSION.RELEASE);
            startActivity(Intent.createChooser(intent, "Send Email"));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

    void checkGPS()
    {
        if(!isGPSEnabled(this)) {
            Toast.makeText(MapsActivity.this, "請開始定位功能避免定位失效", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    public static boolean isGPSEnabled(Context context){
        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    void change_icon(String s) {
        String arraydata = "[" + s + "]";
        String ID = "",ping = "",weight = "";
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
                ping = jObject.getString("ping");
                weight = jObject.getString("weight");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Long = Double.parseDouble(get_Long.get(Integer.valueOf(ID) - 1));
        Lat = Double.parseDouble(get_Lat.get(Integer.valueOf(ID) - 1));
        LatLng sydney = new LatLng(Long, Lat);

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
        trash_state.set(index,false);
        int test = (Integer.valueOf(weight)) / 50;
        if (test >= 0 && test < 10) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_0)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test >= 10 && test <= 19) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_10)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test >= 20 && test <= 29) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_20)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test >= 30 && test <= 39) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_30)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test >= 40 && test <= 49) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_40)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test >= 50 && test <= 59) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_50)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test >= 60 && test <= 69) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_60)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test >= 70 && test <= 79) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_70)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test >= 80 && test <= 89) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_80)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test >= 90 && test < 100) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_90)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
        } else if (test == 100) {
            addmarker = new MarkerOptions().position(sydney).title("LoRa ID : " + ID).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_trash_100)).snippet("Ping ：" + ping + "\nWeight ：" + weight);
            trash_state.set(index,true);
        }
        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
        singleMarker = mMap.addMarker(addmarker);
        mMarkers.add(index,singleMarker);
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
            if(!(parent.getItemAtPosition(position).toString().equals("沒有垃圾桶滿喔")))
            {
                int index = parent.getPositionForView(view);
                //Toast.makeText(MapsActivity.this,index + "" , Toast.LENGTH_SHORT).show();
                double lat = Double.parseDouble(get_Lat.get(index));
                double lng = Double.parseDouble(get_Long.get(index));
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lng + ", " + lat + "&mode=w");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }
}