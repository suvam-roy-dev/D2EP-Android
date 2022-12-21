package com.pwc.d2ep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pwc.d2ep.db.AppDatabase;
import com.pwc.d2ep.db.TaskDB;
import com.pwc.d2ep.db.TaskDao;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TaskDetailsActivity extends AppCompatActivity {
    String visitId,taskId;
    private RestClient client1;
    TaskDao taskDao;
    AppDatabase db;
    TaskDB task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        visitId = getIntent().getStringExtra("VISITID");
        taskId = getIntent().getStringExtra("TASKID");
        setTitle("Task Details");

        setupSF();

        db = Room.databaseBuilder(this,
                AppDatabase.class, "d2ep_db").build();

        taskDao = db.taskDao();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                fetchTaskDetails(taskDao);
            }
        });
    }

    private void fetchTaskDetails(TaskDao taskDao) {
        task = taskDao.loadTaskDetails(taskId);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {


        ((TextView)findViewById(R.id.tvSubjectTaskDetails)).setText(task.subject);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date strDate = sdf.parse(task.date);
            //holder.tvDate.setText("Date: "+new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate));
            ((TextView)findViewById(R.id.tvDateTaskDetails)).setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate));

        } catch (ParseException e) {
            e.printStackTrace();
            ((TextView)findViewById(R.id.tvDateTaskDetails)).setText(task.date);
        }
        ((TextView)findViewById(R.id.tvPrioTaskDetails)).setText(task.priority);
        ((TextView)findViewById(R.id.tvStatusTaskDetails)).setText(task.status);

        findViewById(R.id.textView5).setVisibility(View.VISIBLE);
        findViewById(R.id.textView9).setVisibility(View.VISIBLE);
        findViewById(R.id.textView12).setVisibility(View.VISIBLE);
        if(task.status.equals("Completed")){
            ((CheckBox)findViewById(R.id.checkTaskDetails)).setChecked(true);
        }else {
            ((CheckBox)findViewById(R.id.checkTaskDetails)).setChecked(false);

        }


        ((EditText)findViewById(R.id.etDiscriptionTaskDetails)).setText(task.description);
        if (task.description.equals("null")){
            ((EditText)findViewById(R.id.etDiscriptionTaskDetails)).setText("");
        }

        ((EditText)findViewById(R.id.etDiscriptionTaskDetails)).setImeOptions(EditorInfo.IME_ACTION_DONE);

        findViewById(R.id.cvSubjectTaskDetails).setVisibility(View.VISIBLE);
        findViewById(R.id.cvInfoTaskDetails).setVisibility(View.VISIBLE);
        findViewById(R.id.cvDescTaskDetails).setVisibility(View.VISIBLE);
        findViewById(R.id.cvBottomTaskDetails).setVisibility(View.VISIBLE);


        findViewById(R.id.progressBar4).setVisibility(View.GONE);

                findViewById(R.id.bSaveTaskDetails).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            updateTable(((CheckBox)findViewById(R.id.checkTaskDetails)).isChecked(),((EditText)findViewById(R.id.etDiscriptionTaskDetails)).getText().toString());
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

        if(new TinyDB(this).getBoolean("Online") == false){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.parseColor("#FF675B"));
            }
        }
    }

    private void updateTable(boolean completed,String comments) throws UnsupportedEncodingException {
        String soql= "";
        //soql = "UPDATE Task SET status = 'Completed', Description = '"+comments+"' WHERE Id = '"+taskId+"' and WhatId = '"+visitId+"'";

            Map<String, Object> map = new HashMap<>();
            map.put("status",completed ? "Completed" : "Not Started");
            map.put("Description", comments);
            RestRequest restRequest = RestRequest.getRequestForUpdate(ApiVersionStrings.getVersionNumber(this), "Task",taskId,map);

            client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread
                    runOnUiThread(() -> {
                        try {
                            //listAdapter.clear();
                            Log.d("2512 Visit", "Result :" + result.toString());
                            //Toast.makeText(TaskDetailsActivity.this,"Task Updated Successfully!",Toast.LENGTH_SHORT).show();
                            if(completed){
                                ((TextView)findViewById(R.id.tvStatusTaskDetails)).setText("Completed");
                            }
                            if(!completed){
                                ((TextView)findViewById(R.id.tvStatusTaskDetails)).setText("Not Started");
                            }

                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    TaskDB taskDB = new TaskDB();
                                    taskDB.taskId = taskId;
                                    taskDB.visitId = visitId;
                                    taskDB.description = comments;
                                    taskDB.status = completed ? "Completed" : "Not Started";
                                    taskDB.isSynced = true;
                                    taskDB.subject = task.subject;
                                    taskDB.date = task.date;
                                    taskDB.priority = task.priority;
                                    int rows = taskDao.updateTasks(taskDB);
//                                    if (rows > 0){
//                                        Toast.makeText(getApplicationContext(),"Task Updated", Toast.LENGTH_SHORT).show();
//                                    }
                                    db.close();
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
                            taskDB.status = completed ? "Completed" : "Not Started";
                            taskDB.isSynced = false;
                            taskDB.subject = task.subject;
                            taskDB.date = task.date;
                            taskDB.priority = task.priority;
                            int rows = taskDao.updateTasks(taskDB);
//                                    if (rows > 0){
//                                        runOnUiThread(() -> Toast.makeText(getApplicationContext(),"Task Updated", Toast.LENGTH_SHORT).show());
//                                    }
                            db.close();
                            finish();
                        }
                    });

                }
            });
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
                            ((CheckBox)findViewById(R.id.checkTaskDetails)).setChecked(true);
                        }else {
                            ((CheckBox)findViewById(R.id.checkTaskDetails)).setChecked(false);

                        }


                        ((EditText)findViewById(R.id.etDiscriptionTaskDetails)).setText(records.getJSONObject(0).getString("Description"));
                        if (records.getJSONObject(0).getString("Description").equals("null")){
                            ((EditText)findViewById(R.id.etDiscriptionTaskDetails)).setText("");
                        }

                        findViewById(R.id.bSaveTaskDetails).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    updateTable(((CheckBox)findViewById(R.id.checkTaskDetails)).isChecked(),((EditText)findViewById(R.id.etDiscriptionTaskDetails)).getText().toString());


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
}