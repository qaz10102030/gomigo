package com.test.maptest.VolleyRequest;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class VolleyRequest {
    private RequestQueue mQueue;
    private StringRequest getRequest;
    private VolleyCallback volleycallback;
    private RetryPolicy policy;

    public Response.ErrorListener errorlistener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            volleycallback.onError(volleyError.toString());
        }
    };

    public VolleyRequest(Context context) {
        HTTPSTrustManager.allowAllSSL();
        policy =new DefaultRetryPolicy(10000,DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        this.mQueue  = Volley.newRequestQueue(context);
    }

    public void getHistory(final String url,final VolleyCallback callback) {
        volleycallback = callback;
        getRequest = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        volleycallback.onSuccess("history", response);
                    }
                }, errorlistener);
        getRequest.setRetryPolicy(policy);
        mQueue.add(getRequest);
    }

    public void getGPS(final String url,final VolleyCallback callback) {
        volleycallback = callback;
        getRequest = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        volleycallback.onSuccess("GPS", response);
                    }
                }, errorlistener);
        mQueue.add(getRequest);
    }

    public void getDirection(final String url,final VolleyCallback callback) {
        volleycallback = callback;
        getRequest = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        volleycallback.onSuccess("direction", response);
                    }
                }, errorlistener);
        getRequest.setRetryPolicy(policy);
        mQueue.add(getRequest);
    }

    public interface VolleyCallback {
        void onSuccess(String label, String result);
        void onError(String error);
    }
}
