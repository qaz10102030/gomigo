package com.test.maptest;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.test.maptest.VolleyRequest.VolleyRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    ArrayList<Integer> hisData = new ArrayList<>();
    VolleyRequest volleyRequest;
    TextView tvHis;
    String getID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity);
        tvHis = (TextView)findViewById(R.id.tvHis);
        volleyRequest = new VolleyRequest(this);
        getID = getIntent().getExtras().getString("ID");
        String url = "http://rabbit-test.ddns.net:1880/LoRaData?LoRa_ID=";
        volleyRequest.getHistory(url + getID, volleyCallback);

    }

    private void setView(ArrayList<Integer> hisData) {
        LineChart chart = (LineChart) findViewById(R.id.chart);
        LineData data = new LineData(getDataSet(hisData));
        chart.setData(data);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.animateXY(1500, 1500);
        final String[] quarters = new String[hisData.size()];
        for (int i = 0; i < hisData.size(); i++) {
            quarters[i] = "Data:" + i;
        }
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return quarters[(int) value];
            }
        });
        chart.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        chart.invalidate();
    }

    private ArrayList<ILineDataSet> getDataSet(ArrayList<Integer> hisData) {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        ArrayList<Entry> valueSet1 = new ArrayList<>();
        for (int i = 0; i < hisData.size(); i++) {
            Entry temp = new Entry(i, hisData.get(i));
            valueSet1.add(temp);
        }
        LineDataSet DataSet1 = new LineDataSet(valueSet1, "LoRa_ID:" + getID);
        DataSet1.setColor(Color.BLUE);

        dataSets.add(DataSet1);
        return dataSets;
    }

    private VolleyRequest.VolleyCallback volleyCallback = new VolleyRequest.VolleyCallback() {
        @Override
        public void onSuccess(String label, String result) {
            switch (label) {
                case "history":
                    HistoryHandle(result);
                    break;
            }
        }

        @Override
        public void onError(String error) {
            Toast.makeText(HistoryActivity.this, "伺服器出錯啦!", Toast.LENGTH_SHORT).show();
            Log.e("GetError", error);
        }

        private void HistoryHandle(String result) {
            Log.d("GetData", result);
            JSONArray jArray;
            JSONObject jObject;
            try {
                jArray = new JSONArray(result);
                for (int i = 0; i < jArray.length(); i++) {
                    jObject = jArray.getJSONObject(i);
                    String data = "LoRa_ID: " + jObject.getString("LoRa_ID") + "\nWeight： " + jObject.getString("weight") + "\nTimeStamp： " + jObject.getString("timestamp");
                    int weight = jObject.optInt("weight");
                    hisData.add(weight);
                }
                if(hisData.size() != 0)
                    setView(hisData);
                else{
                    tvHis.setText("無歷史資料!!!");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
}
