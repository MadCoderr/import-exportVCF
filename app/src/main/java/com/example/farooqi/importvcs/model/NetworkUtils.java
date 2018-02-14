package com.example.farooqi.importvcs.model;

import android.content.Context;
import android.util.Log;

import com.example.farooqi.importvcs.MainActivity;
import com.example.farooqi.importvcs.VCFFile;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by SAMSUNG on 2/9/2018.
 */

public class NetworkUtils {

    public static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    static List<String> list;

    public static List<String> getVCFFileFromUrl(Context context) {
        list = new ArrayList<>();
        AsyncHttpClient client = new AsyncHttpClient();
        String url = "http://konnect.aptechmedia.com/uploads/89/e0a652834f854d8419498404dc683a1e.vcf";
        client.get(url, new FileAsyncHttpResponseHandler(context) {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Log.i(LOG_TAG, throwable.getMessage());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                Log.i(LOG_TAG, file.getAbsolutePath());
                File newFile = new File(file.getAbsolutePath());
                try {
                    InputStream inputStream = new FileInputStream(newFile);
                    VCFFile vFile = new VCFFile(inputStream);
                    list = vFile.readData();
                    Log.i(LOG_TAG, list.toString());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        return list;
    }
}
