package com.pwc.d2ep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

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
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VisitTasksActivity extends AppCompatActivity {

    String visitId ;
    private RestClient client1;
    private ArrayList<VisitTask> totalVisits;
    private RecyclerView rvVisits;
    AppDatabase db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_tasks);

        setupSF();
        totalVisits = new ArrayList<>();

        rvVisits = findViewById(R.id.rvTasks);
        rvVisits.setHasFixedSize(true);
        rvVisits.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        visitId = getIntent().getStringExtra("ID");
        setTitle("Tasks");
    }


    private void fetchTasks(TaskDao taskDao) {

        TaskDB[] tasks = taskDao.loadVisitTasks(visitId);

        totalVisits.clear();
        for (TaskDB task: tasks) {
            VisitTask vTemp = new VisitTask(task.taskId,task.visitId, task.subject,task.date,task.status);
            totalVisits.add(vTemp);
        }

        TaskListAdapter adapter = new TaskListAdapter(totalVisits,getApplicationContext());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rvVisits.setAdapter(adapter);
                rvVisits.setVisibility(View.VISIBLE);
                findViewById(R.id.progressBar2).setVisibility(View.GONE);

            }
        });
        checkForCompletion();
    }

    private void checkForCompletion(){
        VisitDao visitDao = db.visitDao();
        VisitDB visitDB = visitDao.loadVisitDetails(visitId);

        boolean isCompleted = true;

        for (TaskDB task: db.taskDao().loadVisitTasks(visitId)) {
            if(!task.status.equals("Completed")){
                isCompleted = false;
            }
        }

        try {
            updateTable(isCompleted);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void updateTable(boolean completed) throws UnsupportedEncodingException {
        String soql= "";
        //soql = "UPDATE Task SET status = 'Completed', Description = '"+comments+"' WHERE Id = '"+taskId+"' and WhatId = '"+visitId+"'";

        Map<String, Object> map = new HashMap<>();
        map.put("Status__c",completed ? "Completed" : "New");
        RestRequest restRequest = RestRequest.getRequestForUpdate(ApiVersionStrings.getVersionNumber(this), "Visit__c",visitId,map);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(() -> {
                    try {
                        //listAdapter.clear();
                        Log.d("2512 Visit", "Result :" + result.toString());
                        //Toast.makeText(VisitTasksActivity.this,"Visit Updated Successfully!",Toast.LENGTH_SHORT).show();

                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                VisitDB visitDB = db.visitDao().loadVisitDetails(visitId);

                                visitDB.status = completed ? "Completed" : "New";
                                visitDB.isSynced = true;
                                int rows = db.visitDao().updateVisits(visitDB);
//                                    if (rows > 0){
//                                        Toast.makeText(getApplicationContext(),"Task Updated", Toast.LENGTH_SHORT).show();
//                                    }
                                db.close();
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
                        VisitDB visitDB = db.visitDao().loadVisitDetails(visitId);

                        visitDB.status = completed ? "Completed" : "New";
                        visitDB.isSynced = false;
                        int rows = db.visitDao().updateVisits(visitDB);
//                        if (rows > 0){
//                            runOnUiThread(() -> Toast.makeText(getApplicationContext(),"Visit Updated", Toast.LENGTH_SHORT).show());
//                        }
                        db.close();
                    }
                });

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //setupSF();
        db = Room.databaseBuilder(this,
                AppDatabase.class, "d2ep_db").build();

        TaskDao taskDao = db.taskDao();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                fetchTasks(taskDao);
            }
        });


            if(new TinyDB(this).getBoolean("Online") == false){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Window window = getWindow();
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(Color.parseColor("#FF675B"));
                }
            }

    }

    @Override
    protected void onPause() {
        super.onPause();
        db.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
                                            logout(VisitTasksActivity.this);
                                    return;
                                }
                                // Cache the returned client
                                client1 = client;
//                                try {
//                                    String ownerId = client.getJSONCredentials().getString("userId");
////                                    sendHighRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Priority__c='Low' ORDER BY VisitStartTime__c ASC");
////                                    sendMissedRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Status__C='Missed' ORDER BY VisitStartTime__c DESC");
////                                    sendTotalRequest("SELECT ID,DistributorToVisit__r.Name,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
//
//
//                                    //sendRequest("SELECT Id, OwnerID, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where WhatId ='"+visitId+"'");
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

    private void sendRequest(String soql) throws UnsupportedEncodingException {
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
//                            for (int i = 0; i < records.length(); i++) {
//                                listAdapter.add(records.getJSONObject(i).getString("Name"));
//                            }
                        totalVisits = new ArrayList<>();

                        for (int i = 0; i < records.length(); i++) {
                            if (true) {
                                String name = (records.getJSONObject(i).getString("Subject"));
                                String time = records.getJSONObject(i).getString("ActivityDate");
                                String prio = (records.getJSONObject(i).getString("Status"));
//                                time = time.replace("T", ", ").substring(0, time.length() - 8);
                               String id = records.getJSONObject(i).getString("Id");

                                VisitTask vTemp = new VisitTask(id,visitId,name,time,prio);

                                totalVisits.add(vTemp);
                            }

                        }
                        TaskListAdapter adapter = new TaskListAdapter(totalVisits,getApplicationContext());

                        rvVisits.setAdapter(adapter);

                        rvVisits.setVisibility(View.VISIBLE);
                        findViewById(R.id.progressBar2).setVisibility(View.GONE);
                    } catch (Exception e) {
                        onError(e);
                        Log.e("2512", "Login Error: "+e.toString());
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
//                runOnUiThread(() -> Toast.makeText(VisitTasksActivity.this,
//                        getString(R.string.sf__generic_error, exception.toString()),
//                        Toast.LENGTH_LONG).show());
            }
        });
    }
}