package com.pwc.d2ep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.room.Room;

import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
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

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class VisitDetailsActivity extends AppCompatActivity {


    String visitId ;
    private RestClient client1;
    private boolean isDistri;
    VisitDao visitDao;
    TaskDao taskDao;
    boolean tasksDone = false;
    VisitDB selectedVisit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_details_new);

        AppDatabase db = Room.databaseBuilder(this,
                AppDatabase.class, "d2ep_db").build();

        visitDao = db.visitDao();
        taskDao = db.taskDao();



//        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
//        getSupportActionBar().setBackgroundDrawable(d);
//        getSupportActionBar().setHomeButtonEnabled(true);
//        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);

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

        setupSF();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                getDataFromDB();
            }
        });
        if (new TinyDB(this).getBoolean("CloseVisitDetail")){
            new TinyDB(this).putBoolean("CloseVisitDetail", false);
            finish();
        }

    }

    private void getDataFromDB() {
        AppDatabase db = Room.databaseBuilder(this,
                AppDatabase.class, "d2ep_db").build();

        VisitDao visitDao = db.visitDao();
        selectedVisit = visitDao.loadVisitDetails(visitId);
        TaskDao taskDao = db.taskDao();
        int tasksNum = taskDao.loadVisitTasks(visitId).length;

        int comp = 0;
        for (TaskDB task: taskDao.loadVisitTasks(visitId)){
            if (!task.status.equals("Completed")){
                tasksDone = false;
            }else{
                comp += 1;
            }
        }
        if (comp == tasksNum){
            tasksDone = true;
        }
//        if (comp > 0 && selectedVisit.status.equals("New"))
//        {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    startVisit();
//                }
//            });
//        }
        int finalComp = comp;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                populateViews(selectedVisit,tasksNum, finalComp);
            }
        });
    }

    private void populateViews(VisitDB selectedVisit, int num, int comp) {
        isDistri = !Objects.equals(selectedVisit.distributorName, "null");
        ((Button)findViewById(R.id.bTasksVisitDetailsActivity)).setText("View Tasks ("+comp+"/"+num+")");

        if (isDistri) {
            ((TextView) findViewById(R.id.tvDistNameDetailsNew)).setText(": " +selectedVisit.distributorName);
            ((TextView) findViewById(R.id.tvDistAddDetailsNew)).setText(": " +selectedVisit.distributorAddress);
        } else{
            ((TextView) findViewById(R.id.tvDistributorTabTitle)).setText("Retailer Details");
//            ((TextView) findViewById(R.id.textView31)).setText("Retailer Name");
//            ((TextView) findViewById(R.id.textView32)).setText("Retailer Address");
            ((TextView) findViewById(R.id.tvDistNameDetailsNew)).setText(": " +selectedVisit.retailerName);
            ((TextView) findViewById(R.id.tvDistAddDetailsNew)).setText(": " +selectedVisit.retailerAddress);
        }
        ((TextView)findViewById(R.id.tvVisitNameDetailsActivityNew)).setText(": " +selectedVisit.name);

        ((TextView)findViewById(R.id.tvStatusVisitDetailsNew)).setText(selectedVisit.status);

        if(selectedVisit.priority.equals("High")){
            findViewById(R.id.imageView6).setVisibility(View.VISIBLE);
        }

        switch (selectedVisit.status){
            case "New":
                ((CardView)findViewById(R.id.cardView)).setCardBackgroundColor(getColor(R.color.colorPrimary));
                break;
            case "Completed":
                ((CardView)findViewById(R.id.cardView)).setCardBackgroundColor(getColor(R.color.status_completed));
                break;
            case "InProgress":
                ((CardView)findViewById(R.id.cardView)).setCardBackgroundColor(getColor(R.color.status_yellow));
                ((TextView)findViewById(R.id.tvStatusVisitDetailsNew)).setText("In Progress");
                break;
            default:
                ((CardView)findViewById(R.id.cardView)).setCardBackgroundColor(getColor(R.color.status_completed));
                break;
        }

        manageStatus(selectedVisit.status);
        //((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("Owner").getString("Name"));

        //((TextView)findViewById(R.id.tvPriorityDetailsActivity)).setText(selectedVisit.priority);
        ((TextView)findViewById(R.id.tvBeatPlanningDetailsNew)).setText(": " +selectedVisit.beatPlanning);
        ((TextView)findViewById(R.id.tvBeatLocationDetailsNew)).setText(": " +selectedVisit.beatLocation);

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


        //((TextView)findViewById(R.id.tvEndTimeDetailsActivty)).setText(endtTime);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date strDate = sdf.parse(date);
            //((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate) + " / " +startTime.substring(0,startTime.length()-3));

        } catch (ParseException e) {
            e.printStackTrace();
            //((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(date + " / " +startTime.substring(0,startTime.length()-3));

        }
        ((TextView)findViewById(R.id.tvDTDetailsActivityNew)).setText(": " +date + " | " +startTime.substring(0,startTime.length()-3));

//        findViewById(R.id.cvInfoVisitDetails).setVisibility(View.VISIBLE);
//        findViewById(R.id.cvDistriVisitDetails).setVisibility(View.VISIBLE);
//        findViewById(R.id.cvBeatVisitDetails).setVisibility(View.VISIBLE);
//        findViewById(R.id.cvBottomVisitDetails).setVisibility(View.VISIBLE);

        findViewById(R.id.progressBar).setVisibility(View.GONE);

        if(comp!=num && comp > 0){
            startVisit();
        }
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
                                monitorNetwork();
//                                try {
//                                    String ownerId = client.getJSONCredentials().getString("userId");
////                                    sendHighRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Priority__c='Low' ORDER BY VisitStartTime__c ASC");
////                                    sendMissedRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Status__C='Missed' ORDER BY VisitStartTime__c DESC");
////                                    sendTotalRequest("SELECT ID,DistributorToVisit__r.Name,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
//                                    //sendVisitDetailsRequest("SELECT ID,OwnerId,Status__c,Priority__c,DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, BeatPlanning__c, BeatPlanning__r.BeatTasks__c FROM Visit__c where ID='"+visitId+"'");
//
//                                    sendVisitDetailsRequest("SELECT Name,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where ID='"+visitId+"'");
//                                    //sendRequest("SELECT Id, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where WhatId ='"+visitId+"'");
//                                    sendRequest("SELECT Id, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where WhatId ='"+visitId+"'");
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

    private void manageStatus(String status){

        findViewById(R.id.bSwitchVisitDetails).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVisit();
                findViewById(R.id.bSwitchVisitDetails).setEnabled(false);
                findViewById(R.id.view).setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.bSwitchComplete).setEnabled(tasksDone);

        //findViewById(R.id.bSwitchComplete).setEnabled(tasksDone);
        findViewById(R.id.bSwitchComplete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tasksDone == false) {
                    ((SwitchCompat) findViewById(R.id.bSwitchComplete)).setChecked(false);
                    Toast.makeText(getApplicationContext(), "Please complete all Open Tasks before completing the Visit.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tasksDone == false) {
                    //((SwitchCompat) findViewById(R.id.bSwitchComplete)).setChecked(false);
                    Toast.makeText(getApplicationContext(), "Please complete all Open Tasks before completing the Visit.", Toast.LENGTH_SHORT).show();
                }
            }
        });

//        ((SwitchCompat)findViewById(R.id.bSwitchComplete)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                if (tasksDone == false) {
//                    ((SwitchCompat) findViewById(R.id.bSwitchComplete)).setChecked(false);
//                }
//            }
//        });




        switch (status){
            case "New":
                    findViewById(R.id.bSwitchVisitDetails).setVisibility(View.VISIBLE);
                findViewById(R.id.view).setVisibility(View.GONE);
                    break;
            case "InProgress":
                findViewById(R.id.bSwitchComplete).setVisibility(View.VISIBLE);
                findViewById(R.id.bSwitchVisitDetails).setVisibility(View.INVISIBLE);
                break;
            case "Completed":
                findViewById(R.id.bSwitchVisitDetails).setVisibility(View.INVISIBLE);
                findViewById(R.id.bSwitchComplete).setVisibility(View.VISIBLE);
                ((SwitchCompat)findViewById(R.id.bSwitchComplete)).setChecked(true);
//                ((Switch)findViewById(R.id.bSwitchComplete)).setThumbTintList(ColorStateList.valueOf(getColor(R.color.text_color_dark)));
//                ((Switch)findViewById(R.id.bSwitchComplete)).setTrackTintList(ColorStateList.valueOf(getColor(R.color.text_color_dark)));
                ((SwitchCompat)findViewById(R.id.bSwitchComplete)).setClickable(false);
                //((Switch)findViewById(R.id.bSwitchComplete)).setAlpha(0.5f);
                break;
        }

    }

    private void startVisit() {
        ((TextView)findViewById(R.id.tvStatusVisitDetailsNew)).setText("In Progress");
        ((CardView)findViewById(R.id.cardView)).setCardBackgroundColor(getColor(R.color.status_yellow));
        findViewById(R.id.bSwitchVisitDetails).setVisibility(View.INVISIBLE);
        findViewById(R.id.bSwitchComplete).setVisibility(View.VISIBLE);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    updateVisitStatus();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
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
                            ((TextView) findViewById(R.id.tvDistNameDetailsNew)).setText(": " +records.getJSONObject(0).getJSONObject("DistributorToVisit__r").getString("Name"));
                            ((TextView) findViewById(R.id.tvDistAddDetailsNew)).setText(": " +records.getJSONObject(0).getJSONObject("DistributorToVisit__r").getString("BillingStreet"));
                        } else{

                            ((TextView) findViewById(R.id.tvDistributorTabTitle)).setText("Retailer Details");
                            ((TextView) findViewById(R.id.textView31)).setText("Retailer Name");
                            ((TextView) findViewById(R.id.textView32)).setText("Retailer Address");
                            ((TextView) findViewById(R.id.tvDistNameDetailsNew)).setText(": " +records.getJSONObject(0).getJSONObject("RetailerToVisit__r").getString("Name"));
                            ((TextView) findViewById(R.id.tvDistAddDetailsNew)).setText(": " +records.getJSONObject(0).getJSONObject("RetailerToVisit__r").getString("MailingStreet"));

                        }
                        ((TextView)findViewById(R.id.tvVisitNameDetailsActivityNew)).setText(": " +(records.getJSONObject(0).getString("Name")));
                        ((TextView)findViewById(R.id.tvStatusVisitDetailsNew)).setText(records.getJSONObject(0).getString("Status__c"));
                        //((TextView)findViewById(R.id.tvOwnerIDDetailsActivity)).setText(records.getJSONObject(0).getJSONObject("Owner").getString("Name"));

                        //((TextView)findViewById(R.id.tvPriorityDetailsActivity)).setText(records.getJSONObject(0).getString("Priority__c"));
                        ((TextView)findViewById(R.id.tvBeatPlanningDetailsNew)).setText(": " +records.getJSONObject(0).getJSONObject("BeatPlanning__r").getString("Name"));
                        ((TextView)findViewById(R.id.tvBeatLocationDetailsNew)).setText(": " +records.getJSONObject(0).getJSONObject("BeatLocation__r").getString("Name"));

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

                        ((TextView)findViewById(R.id.tvDTDetailsActivityNew)).setText(": " +date);
                        //((TextView)findViewById(R.id.tvEndTimeDetailsActivty)).setText(endtTime);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        try {
                            Date strDate = sdf.parse(date);
                            ((TextView)findViewById(R.id.tvDTDetailsActivityNew)).setText(": " +new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate) + " / " +startTime.substring(0,startTime.length()-3));

                        } catch (ParseException e) {
                            e.printStackTrace();
                            ((TextView)findViewById(R.id.tvDTDetailsActivityNew)).setText(": " +date + " / " +startTime.substring(0,startTime.length()-3));

                        }



//                        findViewById(R.id.cvInfoVisitDetails).setVisibility(View.VISIBLE);
//                        findViewById(R.id.cvDistriVisitDetails).setVisibility(View.VISIBLE);
//                        findViewById(R.id.cvBeatVisitDetails).setVisibility(View.VISIBLE);
//                        findViewById(R.id.cvBottomVisitDetails).setVisibility(View.VISIBLE);

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

    private void updateVisitStatus() throws UnsupportedEncodingException {
        String soql= "";
        //soql = "UPDATE Task SET status = 'Completed', Description = '"+comments+"' WHERE Id = '"+taskId+"' and WhatId = '"+visitId+"'";

        Map<String, Object> map = new HashMap<>();
        map.put("Status__c", "InProgress");
        RestRequest restRequest = RestRequest.getRequestForUpdate(ApiVersionStrings.getVersionNumber(this), "Visit__c",visitId,map);

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
                                selectedVisit.status = "InProgress";
                                int rows = visitDao.updateVisits(selectedVisit);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Toast.makeText(getApplicationContext(),rows+" rows updated",Toast.LENGTH_SHORT).show();
                                        Log.d("2512", rows+" rows updated");

                                    }
                                });

                            }
                        });

                    } catch (Exception e) {
                        onError(e);
                        Log.d("2512", "Error: " + e.toString());
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                //Toast.makeText(getApplicationContext(),"Unable to start visit. "+exception.toString(),Toast.LENGTH_SHORT).show();
                Log.d("2512", "Unable to start visit" + exception.toString());
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