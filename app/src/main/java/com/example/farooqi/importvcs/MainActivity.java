package com.example.farooqi.importvcs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.renderscript.ScriptGroup;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.ContactsContract;
import android.provider.Contacts.People;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import com.example.farooqi.importvcs.model.NetworkUtils;
import com.example.farooqi.importvcs.model.User;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_READ_CONTACT = 1;
    private static final int PERMISSION_REQUEST_WRITE_CONTACT = 0;

    ArrayList<String> vCard;
    String vfile;
    String storage_path;
    Cursor cursor;

    List<String> list;
    List<User> userList;
    RecyclerView recycler;
    CustomAdapter adapter;

    ProgressBar proBar;

    File newFile;
    String uploadedFileName;
    String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
// ********************* For reading data from local vcf file ************************
//        InputStream inputStream = getResources().openRawResource(R.raw.contacts);
//        VCFFile file = new VCFFile(inputStream);
//        List<String> result = file.readData();
//
//        Log.i("show_data", result.toString());
//
//        userList = User.extracDataFromFile(result);
//        Log.i("show_data", userList.toString());
// *************************************************************************************

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]
                            {Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_READ_CONTACT);
        }


        list = new ArrayList<>();
        userList = new ArrayList<>();
        recycler = findViewById(R.id.rec_view);
        adapter = new CustomAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }


    public void importVCF(View view) {
        if (!uploadedFileName.isEmpty() && uploadedFileName != null) {
            final AsyncHttpClient client = new AsyncHttpClient();
            String url = "http://konnect.aptechmedia.com/uploads/89/" + uploadedFileName;
            client.get(url, new FileAsyncHttpResponseHandler(this) {
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                    Log.i(LOG_TAG, throwable.getMessage());
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, File file) {
                    newFile = new File(file.getAbsolutePath());
                    try {
                        InputStream inputStream = new FileInputStream(newFile);
                        VCFFile vFile = new VCFFile(inputStream);
                        list.addAll(vFile.readData());
                        userList.addAll(User.extracDataFromFile(list));
                        adapter.notifyDataSetChanged();
                        Log.i(LOG_TAG, list.toString());
                        Log.i(LOG_TAG, userList.toString());
                        loadContacts();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });

        } else {
            Toast.makeText(MainActivity.this, "first export file", Toast.LENGTH_SHORT).show();
        }

    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == PERMISSION_REQUEST_WRITE_CONTACT) {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                loadContacts();
//            } else {
//                Log.i(LOG_TAG, "permission not granted");
//            }
//        }
//        if (requestCode == PERMISSION_REQUEST_READ_CONTACT) {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                getVcardString();
//            } else {
//                Log.i(LOG_TAG, "permission not granted");
//            }
//        }
//    }

    @SuppressLint("StaticFieldLeak")
    private void loadContacts() {
        final AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

            @Override
            protected void onPreExecute() {
                proBar = findViewById(R.id.pro_bar);
                proBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected String doInBackground(Void... voids) {
                return showResult();
            }

            @Override
            protected void onPostExecute(String str) {
                proBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        };
        task.execute();
    }

    private String showResult() {
        for (User user : userList) {
            newContacts(user.getName(), user.getTel());
        }
        return "contact imported";
    }

    private void newContacts(String name, String tel) {
        //  Initializing a ContentProviderOperation object
        ArrayList<ContentProviderOperation> op = new ArrayList<>();
        int index = op.size();
        Log.i(LOG_TAG, String.valueOf(index));
        op.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        op.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, index)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        );

        op.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, index)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, tel)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());


        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, op);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    public void exportVCF(View view) {
        exportContacts();

    }

    @SuppressLint("StaticFieldLeak")
    private void exportContacts() {
        AsyncTask<Void, Void, File> task = new AsyncTask<Void, Void, File>() {

            @Override
            protected void onPreExecute() {
                proBar = findViewById(R.id.pro_bar);
                proBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected File doInBackground(Void... voids) {
                return getVcardString();

            }

            @Override
            protected void onPostExecute(File file) {
                proBar.setVisibility(View.GONE);
                makeRequest(file);
            }
        };
        task.execute();
    }

    private void makeRequest(File myFile) {
        final RequestParams params = new RequestParams();
        params.put("userid", "89");
        try {
            params.put("userfile", myFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        RestClient.post("user/uploadvcf", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.i("response_upload", response.toString());
                    String status = response.getString("status");
                    if (status.equals("1")) {
                        Log.i("vcf", "uploaded");
                        result = "uploaded";
                        uploadedFileName = response.getString("filename");
                    } else {
                        result = "could not be uploaded";
                        Log.i("vcf", "could not be uploaded");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);

                System.out.println(responseString);
            }
        });

    }

    private File getVcardString() {
        File myFile = null;
        vfile = "contacts_test.vcf";
        vCard = new ArrayList<String>();
        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            Log.i("cursor_length", cursor.getCount() + "");

            for (int i = 0; i < cursor.getCount(); i++) {
                get(cursor);
                cursor.moveToNext();
            }
            // upload file test
            myFile = new File(storage_path);
            Log.i("storage_path", myFile.getPath());
            return myFile;
        } else {
            Log.d("TAG", "No Contacts in Your Phone");
            return null;
        }
    }

    public void get(Cursor cursor) {

        //cursor.moveToFirst();
        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        AssetFileDescriptor fd;
        try {
            fd = this.getContentResolver().openAssetFileDescriptor(uri, "r");

            FileInputStream fis = fd.createInputStream();
//            byte[] buf = new byte[(int) fd.getDeclaredLength()];
            byte[] buf = readBytes(fis);
            fis.read(buf);
            String vcardstring = new String(buf);
            vCard.add(vcardstring);

            storage_path = Environment.getExternalStorageDirectory().toString() + File.separator + vfile;
            FileOutputStream mFileOutputStream = new FileOutputStream(storage_path, true);
            mFileOutputStream.write(vcardstring.toString().getBytes());

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        @Override
        public CustomAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.user_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CustomAdapter.ViewHolder holder, int position) {
            User current = userList.get(position);
            holder.userName.setText("Name: " + current.getName());
            holder.userFName.setText("FName: " + current.getFname());
            holder.userTel.setText("TEL: " + current.getTel());
        }

        @Override
        public int getItemCount() {
            Log.i("MainActivity_size", "size: " + userList.size());
            return userList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView userName;
            TextView userFName;
            TextView userTel;

            public ViewHolder(View itemView) {
                super(itemView);

                userName = itemView.findViewById(R.id.lbl_user_name);
                userFName = itemView.findViewById(R.id.lbl_user_fname);
                userTel = itemView.findViewById(R.id.lbl_user_tel);
            }
        }
    }

}
