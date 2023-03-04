package com.pwc.d2ep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.room.Room;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.pwc.d2ep.db.AppDatabase;
import com.pwc.d2ep.db.TaskDB;
import com.pwc.d2ep.db.TaskDao;
import com.pwc.d2ep.db.VisitDB;
import com.pwc.d2ep.db.VisitDao;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TaskDetailsActivity extends AppCompatActivity {
    String visitId,taskId;
    private RestClient client1;
    TaskDao taskDao;
    VisitDao visitDao;
    AppDatabase db;
    TaskDB task;
    String logID, visitName;
    FirebaseFirestore fdb;
    boolean isOnline;

    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details_new);

        fdb = FirebaseFirestore.getInstance();
        final View activityRootView = findViewById(R.id.linearLayout4);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > dpToPx(getApplicationContext(), 200)) { // if more than 200 dp, it's probably a keyboard...
                    // ... do something here
                    (findViewById(R.id.cardView)).setVisibility(View.GONE);
                    (findViewById(R.id.tvSubjectTaskDetailsNew)).setVisibility(View.GONE);
                    (findViewById(R.id.tvDTTaskDetailsNew)).setVisibility(View.GONE);
                    (findViewById(R.id.cardView6)).setVisibility(View.GONE);
                    (findViewById(R.id.textView17)).setVisibility(View.GONE);
                    (findViewById(R.id.textView20)).setVisibility(View.GONE);
                }else {

                    (findViewById(R.id.cardView)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.tvSubjectTaskDetailsNew)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.tvDTTaskDetailsNew)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.cardView6)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.textView17)).setVisibility(View.VISIBLE);
                    (findViewById(R.id.textView20)).setVisibility(View.VISIBLE);
                }
            }
        });


        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AppDatabase db = Room.databaseBuilder(this,
                AppDatabase.class, "d2ep_db").build();

        visitDao = db.visitDao();
        taskDao = db.taskDao();

        visitId = getIntent().getStringExtra("VISITID");
        taskId = getIntent().getStringExtra("TASKID");
        setTitle("Task Details");

        setupSF();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                fetchTaskDetails(taskDao);
            }
        });
    }

    private void fetchTaskDetails(TaskDao taskDao) {
        task = taskDao.loadTaskDetails(taskId);

        visitName = visitDao.loadVisitDetails(visitId).name;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {


        ((TextView)findViewById(R.id.tvSubjectTaskDetailsNew)).setText(": "+task.subject);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date strDate = sdf.parse(task.date);
            //holder.tvDate.setText("Date: "+new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate));
            ((TextView)findViewById(R.id.tvDTTaskDetailsNew)).setText(": "+new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate));

        } catch (ParseException e) {
            e.printStackTrace();
            ((TextView)findViewById(R.id.tvDTTaskDetailsNew)).setText(": "+task.date);
        }
//        ((TextView)findViewById(R.id.tvPrioTaskDetails)).setText(task.priority);
//        ((TextView)findViewById(R.id.tvStatusTaskDetails)).setText(task.status);

//        findViewById(R.id.textView5).setVisibility(View.VISIBLE);
//        findViewById(R.id.textView9).setVisibility(View.VISIBLE);
//        findViewById(R.id.textView12).setVisibility(View.VISIBLE);
        if(task.status.equals("Completed")){
            ((CheckBox)findViewById(R.id.checkMarkComplete)).setChecked(true);
            ((CheckBox)findViewById(R.id.checkMarkComplete)).setEnabled(false);
            ((CheckBox)findViewById(R.id.checkWaiting)).setEnabled(false);

            ((TextView)findViewById(R.id.tvStatusTaskDetails)).setText("Completed");
            ((CardView)findViewById(R.id.cardView)).setCardBackgroundColor(getColor(R.color.status_completed));
        }else {
            ((CheckBox)findViewById(R.id.checkMarkComplete)).setChecked(false);
        }



                if(task.status.equals("WaitingOnSomeoneElse")){
                    ((CheckBox)findViewById(R.id.checkWaiting)).setChecked(true);
                    ((CheckBox)findViewById(R.id.checkMarkComplete)).setEnabled(false);
                    ((TextView)findViewById(R.id.tvStatusTaskDetails)).setText("In Progress");
                    ((CardView)findViewById(R.id.cardView)).setCardBackgroundColor(getColor(R.color.status_yellow));
                }else {
                    ((CheckBox)findViewById(R.id.checkWaiting)).setChecked(false);
                }

                ((CheckBox)findViewById(R.id.checkWaiting)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b){
                            ((CheckBox)findViewById(R.id.checkMarkComplete)).setEnabled(false);
                        }else ((CheckBox)findViewById(R.id.checkMarkComplete)).setEnabled(true);
                    }
                });

                ((CheckBox)findViewById(R.id.checkMarkComplete)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b){
                            ((CheckBox)findViewById(R.id.checkWaiting)).setEnabled(false);
                        }else ((CheckBox)findViewById(R.id.checkWaiting)).setEnabled(true);
                    }
                });
        ((EditText)findViewById(R.id.etDiscriptionTaskDetails2)).setText(task.description);
        if (task.description.equals("null")){
            ((EditText)findViewById(R.id.etDiscriptionTaskDetails2)).setText("");
        }

        ((EditText)findViewById(R.id.etDiscriptionTaskDetails2)).setImeOptions(EditorInfo.IME_ACTION_DONE);

//        findViewById(R.id.cvSubjectTaskDetails).setVisibility(View.VISIBLE);
//        findViewById(R.id.cvInfoTaskDetails).setVisibility(View.VISIBLE);
//        findViewById(R.id.cvDescTaskDetails).setVisibility(View.VISIBLE);
//        findViewById(R.id.cvBottomTaskDetails).setVisibility(View.VISIBLE);


        findViewById(R.id.progressBar4).setVisibility(View.GONE);

                findViewById(R.id.bSaveTaskDetails).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            updateTable(((CheckBox)findViewById(R.id.checkMarkComplete)).isChecked(),((CheckBox)findViewById(R.id.checkWaiting)).isChecked(),((EditText)findViewById(R.id.etDiscriptionTaskDetails2)).getText().toString());

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            Log.e("2512", "Update Error: "+e.toString());
                        }
                    }
                });

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();  return true;
        }

        return super.onOptionsItemSelected(item);
    }


    void setupSF(){
        String accountType =
                SalesforceSDKManager.getInstance().getAccountType();

        ClientManager.LoginOptions loginOptions =
                SalesforceSDKManager.getInstance().getLoginOptions();
// Get a rest client
        new ClientManager(this, accountType, loginOptions,
                false).
                getRestClient(this, new ClientManager.RestClientCallback() {
                            @Override
                            public void
                            authenticatedRestClient(RestClient client) {
                                if (client == null) {
                                    SalesforceSDKManager.getInstance().
                                            logout(TaskDetailsActivity.this);
                                    return;
                                }
                                // Cache the returned client
                                client1 = client;
                                monitorNetwork();
//                                try {
//                                    String ownerId = client.getJSONCredentials().getString("userId");
////                                    sendHighRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Priority__c='Low' ORDER BY VisitStartTime__c ASC");
////                                    sendMissedRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Status__C='Missed' ORDER BY VisitStartTime__c DESC");
////                                    sendTotalRequest("SELECT ID,DistributorToVisit__r.Name,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
//                                    //sendVisitDetailsRequest("SELECT ID,OwnerId,Status__c,Priority__c,DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, BeatPlanning__c, BeatPlanning__r.BeatTasks__c FROM Visit__c where ID='"+visitId+"'");
//
//                                    sendVisitDetailsRequest("SELECT Id, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where WhatId ='"+visitId+"' and Id = '"+taskId+"'");
//                                    //sendRequest("SELECT Id, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where WhatId ='"+visitId+"'");
//
//
//                                } catch (UnsupportedEncodingException e) {
//                                    e.printStackTrace();
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
                            }
                        }
                );
    }

    @Override
    protected void onResume() {
        super.onResume();
        logID = String.valueOf(Calendar.getInstance().getTimeInMillis());
    }

    private void updateTable(boolean completed, boolean waiting,String comments) throws UnsupportedEncodingException {
        String soql= "";
        //soql = "UPDATE Task SET status = 'Completed', Description = '"+comments+"' WHERE Id = '"+taskId+"' and WhatId = '"+visitId+"'";
        if (isOnline) logFirebase("request",Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND));
            Map<String, Object> map = new HashMap<>();
            map.put("status",completed ? "Completed" : waiting ? "WaitingOnSomeoneElse" : "Not Started");
            map.put("Description", comments);
            RestRequest restRequest = RestRequest.getRequestForUpdate(ApiVersionStrings.getVersionNumber(this), "Task",taskId,map);

            if (isOnline) {
                client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
                    @Override
                    public void onSuccess(RestRequest request, final RestResponse result) {
                        result.consumeQuietly(); // consume before going back to main thread
                        runOnUiThread(() -> {
                            try {
                                //listAdapter.clear();
                                Log.d("2512 Visit", "Result :" + result.toString());
                                //Toast.makeText(TaskDetailsActivity.this,"Task Updated Successfully!",Toast.LENGTH_SHORT).show();
                                if (completed) {
                                    ((TextView) findViewById(R.id.tvStatusTaskDetails)).setText("Completed");
                                }
                                if (waiting) {
                                    ((TextView) findViewById(R.id.tvStatusTaskDetails)).setText("In Progress");
                                }
                                if (!completed && !waiting) {
                                    ((TextView) findViewById(R.id.tvStatusTaskDetails)).setText("Not Started");
                                }

                                AsyncTask.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        TaskDB taskDB = new TaskDB();
                                        taskDB.taskId = taskId;
                                        taskDB.visitId = visitId;
                                        taskDB.description = comments;
                                        taskDB.status = completed ? "Completed" : waiting ? "WaitingOnSomeoneElse" : "Not Started";
                                        taskDB.isSynced = true;
                                        taskDB.subject = task.subject;
                                        taskDB.date = task.date;
                                        taskDB.priority = task.priority;
                                        int rows = taskDao.updateTasks(taskDB);
                                        if (rows > 0) {
                                            logFirebase("response", Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":" + Calendar.getInstance().get(Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND));
                                        }
                                        finish();
                                    }
                                });

                            } catch (Exception e) {
                                onError(e);
                                Log.e("2512", "Error: " + e.toString());

                            }
                        });
                    }

                    @Override
                    public void onError(final Exception exception) {
//                    runOnUiThread(() -> Toast.makeText(TaskDetailsActivity.this,
//                            getString(R.string.sf__generic_error, exception.toString()),
//                            Toast.LENGTH_LONG).show());

                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                TaskDB taskDB = new TaskDB();
                                taskDB.taskId = taskId;
                                taskDB.visitId = visitId;
                                taskDB.description = comments;
                                taskDB.status = completed ? "Completed" : waiting ? "WaitingOnSomeoneElse" : "Not Started";
                                taskDB.isSynced = false;
                                taskDB.subject = task.subject;
                                taskDB.date = task.date;
                                taskDB.priority = task.priority;
                                int rows = taskDao.updateTasks(taskDB);
//                                    if (rows > 0){
//                                        runOnUiThread(() -> Toast.makeText(getApplicationContext(),"Task Updated", Toast.LENGTH_SHORT).show());
//                                    }
                                finish();
                            }
                        });

                    }
                });
            }else {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        TaskDB taskDB = new TaskDB();
                        taskDB.taskId = taskId;
                        taskDB.visitId = visitId;
                        taskDB.description = comments;
                        taskDB.status = completed ? "Completed" : waiting ? "WaitingOnSomeoneElse" : "Not Started";
                        taskDB.isSynced = false;
                        taskDB.subject = task.subject;
                        taskDB.date = task.date;
                        taskDB.priority = task.priority;
                        int rows = taskDao.updateTasks(taskDB);
//                                    if (rows > 0){
//                                        runOnUiThread(() -> Toast.makeText(getApplicationContext(),"Task Updated", Toast.LENGTH_SHORT).show());
//                                    }
                        finish();
                    }
                });
            }
    }

    private void logFirebase(String send, String s) {

        Map<String, Object> log = new HashMap<>();
        log.put("visitid",visitName);
        log.put("mode","online");
        log.put(send, s);

        fdb.collection("task request logs").document(visitName).set(log, SetOptions.merge());
    }

    private void sendVisitDetailsRequest(String soql) throws UnsupportedEncodingException {
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(() -> {
                    try {
                        //listAdapter.clear();
                        Log.d("2512 Visit", "Result :" + result.toString());
                        JSONArray records = result.asJSONObject().getJSONArray("records");

                        ((TextView)findViewById(R.id.tvSubjectTaskDetails)).setText(records.getJSONObject(0).getString("Subject"));

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        try {
                            Date strDate = sdf.parse(records.getJSONObject(0).getString("ActivityDate"));
                            //holder.tvDate.setText("Date: "+new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate));
                            ((TextView)findViewById(R.id.tvDateTaskDetails)).setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate));

                        } catch (ParseException e) {
                            e.printStackTrace();
                            ((TextView)findViewById(R.id.tvDateTaskDetails)).setText(records.getJSONObject(0).getString("ActivityDate"));


                        }
                        ((TextView)findViewById(R.id.tvPrioTaskDetails)).setText(records.getJSONObject(0).getString("Priority"));
                        ((TextView)findViewById(R.id.tvStatusTaskDetails)).setText(records.getJSONObject(0).getString("Status"));

                        findViewById(R.id.textView5).setVisibility(View.VISIBLE);
                        findViewById(R.id.textView9).setVisibility(View.VISIBLE);
                        findViewById(R.id.textView12).setVisibility(View.VISIBLE);
                        if(records.getJSONObject(0).getString("Status").equals("Completed")){
                            ((CheckBox)findViewById(R.id.checkMarkComplete)).setChecked(true);
                        }else {
                            ((CheckBox)findViewById(R.id.checkMarkComplete)).setChecked(false);

                        }


                        ((EditText)findViewById(R.id.etDiscriptionTaskDetails)).setText(records.getJSONObject(0).getString("Description"));
                        if (records.getJSONObject(0).getString("Description").equals("null")){
                            ((EditText)findViewById(R.id.etDiscriptionTaskDetails)).setText("");
                        }

                        findViewById(R.id.bSaveTaskDetails).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    updateTable(((CheckBox)findViewById(R.id.checkMarkComplete)).isChecked(),((CheckBox)findViewById(R.id.checkWaiting)).isChecked(),((EditText)findViewById(R.id.etDiscriptionTaskDetails)).getText().toString());

                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                    Log.e("2512", "Update Error: "+e.toString());
                                }
                            }
                        });


                        findViewById(R.id.cvSubjectTaskDetails).setVisibility(View.VISIBLE);
                        findViewById(R.id.cvInfoTaskDetails).setVisibility(View.VISIBLE);
                        findViewById(R.id.cvDescTaskDetails).setVisibility(View.VISIBLE);
                        findViewById(R.id.cvBottomTaskDetails).setVisibility(View.VISIBLE);


                        findViewById(R.id.progressBar4).setVisibility(View.GONE);

                    } catch (Exception e) {
                        onError(e);
                        Log.e("2512", "Login Error: "+e.toString());
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                runOnUiThread(() -> Toast.makeText(TaskDetailsActivity.this,
                        getString(R.string.sf__generic_error, exception.toString()),
                        Toast.LENGTH_LONG).show());
            }
        });

    }

    NetworkRequest networkRequest = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build();

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            updateStatusBarColor("#1da1f2");
            isOnline = true;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    if (taskDao.loadUnsynced().length != 0) {
                        try {
                            syncTasks();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            updateStatusBarColor("#FF675B");
            isOnline = false;
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            final boolean unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }
    };

    private void monitorNetwork() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(ConnectivityManager.class);
        connectivityManager.requestNetwork(networkRequest, networkCallback);
        if (connectivityManager.getActiveNetworkInfo()==null){
            updateStatusBarColor("#FF675B");
        }
    }

    public void updateStatusBarColor(String color){// Color must be in hexadecimal fromat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Window window = getWindow();
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(Color.parseColor(color));
                }
            });
        }
    }

    private void syncTasks() throws UnsupportedEncodingException {
        TaskDB[] tasks = taskDao.loadUnsynced();
        for (TaskDB vTemp: tasks) {
            updateTable(vTemp);
        }
        VisitDB[] visits = visitDao.loadUnsynced();

        for(VisitDB visitDB : visits){
            updateVisit(visitDB);
        }
    }

    private void updateVisit(VisitDB visitDB) throws UnsupportedEncodingException {
        String soql= "";
        //soql = "UPDATE Task SET status = 'Completed', Description = '"+comments+"' WHERE Id = '"+taskId+"' and WhatId = '"+visitId+"'";

        Map<String, Object> map = new HashMap<>();
                map.put("Status__c",visitDB.status);
        RestRequest restRequest = RestRequest.getRequestForUpdate(ApiVersionStrings.getVersionNumber(this), "Visit__c",visitDB.visitId,map);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(() -> {
                    try {
                        //listAdapter.clear();
                        Log.d("2512 Visit", "Result :" + result.toString());
                        //Toast.makeText(getActivity(),"Task Updated Successfully!",Toast.LENGTH_SHORT).show();

                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {

                                visitDB.isSynced = true;
                                int rows = visitDao.updateVisits(visitDB);
//                                    if (rows > 0){
//                                        Toast.makeText(getApplicationContext(),"Task Updated", Toast.LENGTH_SHORT).show();
//                                    }

//                                fetchVisits();
//                                fetchTasks();
                            }
                        });

                    } catch (Exception e) {
                        onError(e);
                        Log.e("2512", "Error: " + e.toString());

                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
//                runOnUiThread(() -> Toast.makeText(TaskDetailsActivity.this,
//                        getString(R.string.sf__generic_error, exception.toString()),
//                        Toast.LENGTH_LONG).show());
//
//                AsyncTask.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        TaskDB taskDB = new TaskDB();
//                        taskDB.taskId = taskId;
//                        taskDB.visitId = visitId;
//                        taskDB.description = comments;
//                        taskDB.status = completed ? "Completed" : "Not Started";
//                        taskDB.isSynced = false;
//                        taskDB.subject = task.subject;
//                        taskDB.date = task.date;
//                        taskDB.priority = task.priority;
//                        int rows = taskDao.updateTasks(taskDB);
////                                    if (rows > 0){
////                                        Toast.makeText(getApplicationContext(),"Task Updated", Toast.LENGTH_SHORT).show();
////                                    }
//                        db.close();
//                        finish();
//                    }
//                });
//                fetchVisits();
//                fetchTasks();

            }
        });
    }

    private void updateTable(TaskDB task) throws UnsupportedEncodingException {
        String soql= "";
        //soql = "UPDATE Task SET status = 'Completed', Description = '"+comments+"' WHERE Id = '"+taskId+"' and WhatId = '"+visitId+"'";

        Map<String, Object> map = new HashMap<>();
        map.put("status",task.status);
        map.put("Description", task.description);
        RestRequest restRequest = RestRequest.getRequestForUpdate(ApiVersionStrings.getVersionNumber(this), "Task",task.taskId,map);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(() -> {
                    try {
                        //listAdapter.clear();
                        Log.d("2512 Visit", "Result :" + result.toString());
                        //Toast.makeText(getActivity(),"Task Updated Successfully!",Toast.LENGTH_SHORT).show();

                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                TaskDB taskDB = new TaskDB();
                                taskDB.taskId = task.taskId;
                                taskDB.visitId = task.visitId;
                                taskDB.description = task.description;
                                taskDB.status = task.status;
                                taskDB.isSynced = true;
                                taskDB.subject = task.subject;
                                taskDB.date = task.date;
                                taskDB.priority = task.priority;
                                int rows = taskDao.updateTasks(taskDB);
//                                    if (rows > 0){
//                                        Toast.makeText(getApplicationContext(),"Task Updated", Toast.LENGTH_SHORT).show();
//                                    }


                            }
                        });

                    } catch (Exception e) {
                        onError(e);
                        Log.e("2512", "Error: " + e.toString());
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
//                runOnUiThread(() -> Toast.makeText(TaskDetailsActivity.this,
//                        getString(R.string.sf__generic_error, exception.toString()),
//                        Toast.LENGTH_LONG).show());
//
//                AsyncTask.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        TaskDB taskDB = new TaskDB();
//                        taskDB.taskId = taskId;
//                        taskDB.visitId = visitId;
//                        taskDB.description = comments;
//                        taskDB.status = completed ? "Completed" : "Not Started";
//                        taskDB.isSynced = false;
//                        taskDB.subject = task.subject;
//                        taskDB.date = task.date;
//                        taskDB.priority = task.priority;
//                        int rows = taskDao.updateTasks(taskDB);
////                                    if (rows > 0){
////                                        Toast.makeText(getApplicationContext(),"Task Updated", Toast.LENGTH_SHORT).show();
////                                    }
//                        db.close();
//                        finish();
//                    }
//                });
//                fetchVisits();
//                fetchTasks();
            }
        });
    }


}