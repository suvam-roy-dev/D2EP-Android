package com.pwc.d2ep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import android.content.Intent;
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
import android.widget.Button;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class VisitDetailsActivity extends AppCompatActivity {


    String visitId ;
    private RestClient client1;
    private boolean isDistri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_details_alt);

        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);

        visitId = getIntent().getStringExtra("ID");
        setTitle("Details");
        //setupSF();


        findViewById(R.id.bTasksVisitDetailsActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(VisitDetailsActivity.this,VisitTasksActivity.class);
                i.putExtra("ID", visitId);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                getDataFromDB();
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

    private void getDataFromDB() {
        AppDatabase db = Room.databaseBuilder(this,
                AppDatabase.class, "d2ep_db").build();

        VisitDao visitDao = db.visitDao();

        VisitDB selectedVisit = visitDao.loadVisitDetails(visitId);

        TaskDao taskDao = db.taskDao();
        int tasksNum = taskDao.loadVisitTasks(visitId).length;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                populateViews(selectedVisit,tasksNum);
            }
        });
    }

    private void populateViews(VisitDB selectedVisit, int num) {
        isDistri = !Objects.equals(selectedVisit.distributorName, "null");
        ((Button)findViewById(R.id.bTasksVisitDetailsActivity)).setText("View Tasks ("+num+")");


        if (isDistri) {
            ((TextView) findViewById(R.id.tvDistrNameDetailsActivity)).setText(selectedVisit.distributorName);
            ((TextView) findViewById(R.id.tvStartTimeDetailsActivity)).setText(selectedVisit.distributorAddress);
        } else{

            ((TextView) findViewById(R.id.textView21)).setText("Retailer Name");
            ((TextView) findViewById(R.id.textView22)).setText("Retailer Address");
            ((TextView) findViewById(R.id.tvDistrNameDetailsActivity)).setText(selectedVisit.retailerName);
            ((TextView) findViewById(R.id.tvStartTimeDetailsActivity)).setText(selectedVisit.retailerAddress);

        }
        ((TextView)findViewById(R.id.tvVisitNameDetailsActivity)).setText(selectedVisit.name);
        ((TextView)findViewById(R.id.tvStatusDetailsActivity)).setText(selectedVisit.status);
        //((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("Owner").getString("Name"));

        ((TextView)findViewById(R.id.tvPriorityDetailsActivity)).setText(selectedVisit.priority);
        ((TextView)findViewById(R.id.tvBeatPlanningDetailsActivity)).setText(selectedVisit.beatPlanning);
        ((TextView)findViewById(R.id.tvBeatLocationDetailsActivity)).setText(selectedVisit.beatLocation);

//                        if (records.getJSONObject(0).getString("Priority__c").equals("Low")){
//                            ((TextView)findViewById(R.id.tvPriorityDetailsActivity)).setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
//                        }

        String timeString = selectedVisit.time;
        String[] split = timeString.split(",");
        String date = split[0];
        String startTime = split[1];//.substring(0, split[1].length()-1);

//        String endTimeString = records.getJSONObject(0).getString("VisitEndTime__c");
//        String[] split2 = endTimeString.split("T");
//        String endtTime = split2[1].substring(0, split2[1].length() - 9);

        ((TextView)findViewById(R.id.tvDateDetailsActivityNew)).setText(date);
        //((TextView)findViewById(R.id.tvEndTimeDetailsActivty)).setText(endtTime);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date strDate = sdf.parse(date);
            ((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate) + " / " +startTime.substring(0,startTime.length()-3));

        } catch (ParseException e) {
            e.printStackTrace();
            ((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(date + " / " +startTime.substring(0,startTime.length()-3));

        }



        findViewById(R.id.cvInfoVisitDetails).setVisibility(View.VISIBLE);
        findViewById(R.id.cvDistriVisitDetails).setVisibility(View.VISIBLE);
        findViewById(R.id.cvBeatVisitDetails).setVisibility(View.VISIBLE);
        findViewById(R.id.cvBottomVisitDetails).setVisibility(View.VISIBLE);

        findViewById(R.id.progressBar).setVisibility(View.GONE);
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
                                            logout(VisitDetailsActivity.this);
                                    return;
                                }
                                // Cache the returned client
                                client1 = client;
                                try {
                                    String ownerId = client.getJSONCredentials().getString("userId");
//                                    sendHighRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Priority__c='Low' ORDER BY VisitStartTime__c ASC");
//                                    sendMissedRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Status__C='Missed' ORDER BY VisitStartTime__c DESC");
//                                    sendTotalRequest("SELECT ID,DistributorToVisit__r.Name,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
                                    //sendVisitDetailsRequest("SELECT ID,OwnerId,Status__c,Priority__c,DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, BeatPlanning__c, BeatPlanning__r.BeatTasks__c FROM Visit__c where ID='"+visitId+"'");

                                    sendVisitDetailsRequest("SELECT Name,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where ID='"+visitId+"'");
                                    //sendRequest("SELECT Id, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where WhatId ='"+visitId+"'");
                                    sendRequest("SELECT Id, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where WhatId ='"+visitId+"'");


                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
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
                        ((Button)findViewById(R.id.bTasksVisitDetailsActivity)).setText("View Tasks ("+records.length()+")");

                        if (records.length() == 0){
                            ((Button)findViewById(R.id.bTasksVisitDetailsActivity)).setText("No Tasks");
                            ((Button)findViewById(R.id.bTasksVisitDetailsActivity)).setEnabled(false);
                        }
                    } catch (Exception e) {
                        onError(e);
                        Log.e("2512", "Login Error: "+e.toString());
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
//                runOnUiThread(() -> Toast.makeText(VisitDetailsActivity.this,
//                        getString(R.string.sf__generic_error, exception.toString()),
//                        Toast.LENGTH_LONG).show());
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
//                            for (int i = 0; i < records.length(); i++) {
//                                listAdapter.add(records.getJSONObject(i).getString("Name"));
//                            }

                        isDistri = records.getJSONObject(0).getString("DistributorToVisit__r") != "null";

                        if (isDistri) {
                            ((TextView) findViewById(R.id.tvDistrNameDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("DistributorToVisit__r").getString("Name"));
                            ((TextView) findViewById(R.id.tvStartTimeDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("DistributorToVisit__r").getString("BillingStreet"));
                        } else{

                            ((TextView) findViewById(R.id.textView21)).setText("Retailer Name");
                            ((TextView) findViewById(R.id.textView22)).setText("Retailer Address");
                            ((TextView) findViewById(R.id.tvDistrNameDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("RetailerToVisit__r").getString("Name"));
                            ((TextView) findViewById(R.id.tvStartTimeDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("RetailerToVisit__r").getString("MailingStreet"));

                        }
                        ((TextView)findViewById(R.id.tvVisitNameDetailsActivity)).setText((records.getJSONObject(0).getString("Name")));
                        ((TextView)findViewById(R.id.tvStatusDetailsActivity)).setText(records.getJSONObject(0).getString("Status__c"));
                        //((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("Owner").getString("Name"));

                        ((TextView)findViewById(R.id.tvPriorityDetailsActivity)).setText(records.getJSONObject(0).getString("Priority__c"));
                        ((TextView)findViewById(R.id.tvBeatPlanningDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("BeatPlanning__r").getString("Name"));
                        ((TextView)findViewById(R.id.tvBeatLocationDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("BeatLocation__r").getString("Name"));

//                        if (records.getJSONObject(0).getString("Priority__c").equals("Low")){
//                            ((TextView)findViewById(R.id.tvPriorityDetailsActivity)).setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
//                        }

                        String timeString = records.getJSONObject(0).getString("VisitStartTime__c");
                        String[] split = timeString.split("T");
                        String date = split[0];
                        String startTime = split[1].substring(0, split[1].length() - 9);

                        String endTimeString = records.getJSONObject(0).getString("VisitEndTime__c");
                        String[] split2 = endTimeString.split("T");
                        String endtTime = split2[1].substring(0, split2[1].length() - 9);

                        ((TextView)findViewById(R.id.tvDateDetailsActivityNew)).setText(date);
                        ((TextView)findViewById(R.id.tvEndTimeDetailsActivty)).setText(endtTime);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        try {
                            Date strDate = sdf.parse(date);
                            ((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate) + " / " +startTime.substring(0,startTime.length()-3));

                        } catch (ParseException e) {
                            e.printStackTrace();
                            ((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(date + " / " +startTime.substring(0,startTime.length()-3));

                        }



                        findViewById(R.id.cvInfoVisitDetails).setVisibility(View.VISIBLE);
                        findViewById(R.id.cvDistriVisitDetails).setVisibility(View.VISIBLE);
                        findViewById(R.id.cvBeatVisitDetails).setVisibility(View.VISIBLE);
                        findViewById(R.id.cvBottomVisitDetails).setVisibility(View.VISIBLE);

                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                    } catch (Exception e) {
                        onError(e);
                        Log.e("2512", "Login Error: "+e.toString());
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
//                runOnUiThread(() -> Toast.makeText(VisitDetailsActivity.this,
//                        getString(R.string.sf__generic_error, exception.toString()),
//                        Toast.LENGTH_LONG).show());
            }
        });
    }
}