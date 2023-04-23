package com.pwc.d2ep.ui.home;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.JsonObject;
import com.pwc.d2ep.BuildConfig;
import com.pwc.d2ep.HostActivity;
import com.pwc.d2ep.LoginActivity;
import com.pwc.d2ep.R;
import com.pwc.d2ep.TinyDB;
import com.pwc.d2ep.Visit;
import com.pwc.d2ep.VisitDashboardAdapter;
import com.pwc.d2ep.databinding.FragmentHomeBinding;
import com.pwc.d2ep.db.AppDatabase;
import com.pwc.d2ep.db.TaskDB;
import com.pwc.d2ep.db.TaskDao;
import com.pwc.d2ep.db.VisitDB;
import com.pwc.d2ep.db.VisitDao;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.auth.HttpAccess;
import com.salesforce.androidsdk.config.LoginServerManager;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DashbaordFragment extends Fragment {
    private FragmentHomeBinding binding;
    private RestClient client1;
    private TextView tvHighPrioNumber;
    private TextView tvMissedNumber;
    private Button bVisits;
    private RecyclerView rvVisits;
    ArrayList<Visit> totalVisits;
    ArrayList<Object> cachedVisitsTotal;
    ArrayList<Object> cachedVisitsHigh;
    ArrayList<Visit> highPriorityVisits;
    ArrayList<Visit> missedVisits;
    private boolean isHighSelected = false;
    private boolean isMissedSelected = false;
    boolean isLoaded = false;
    TinyDB tinyDB;
    VisitDao visitDao;
    TaskDao taskDao;
    private String ownerId;
    private String dealerID;
    String visitsIDS = "(";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        totalVisits = new ArrayList<>();
        highPriorityVisits = new ArrayList<>();

        Log.d("2512", "onCreate:");
        setHasOptionsMenu(true);
        //checkForUpdate();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.dashboard,menu);
    }

    private void checkForUpdate() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        double currentVersion = Double.parseDouble(BuildConfig.VERSION_NAME);
        db.collection("Versions")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                double ver = document.getDouble("version");

                                if (currentVersion < ver){
                                    //Toast.makeText(getContext(),"Please update the app!", Toast.LENGTH_SHORT).show();
                                    showUpdateDialog(document.getString("url"));
                                }
                            }
                        } else {
                            Log.w("Firestore", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void showUpdateDialog(String url) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        // Set the message show for the Alert time
        builder.setMessage("A new update is available.");

        // Set Alert Title
        builder.setTitle("Update!");

        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
        builder.setCancelable(false);

        // Set the positive button with yes name Lambda OnClickListener method is use of DialogInterface interface.
        builder.setPositiveButton("Update", (DialogInterface.OnClickListener) (dialog, which) -> {
            // When the user click yes button then app will close
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        });

        // Set the Negative button with No name Lambda OnClickListener method is use of DialogInterface interface.
        builder.setNegativeButton("Dismiss", (DialogInterface.OnClickListener) (dialog, which) -> {
            // If user click no then dialog box is canceled.
            dialog.cancel();
        });

        // Create the Alert dialog
        AlertDialog alertDialog = builder.create();
        // Show the Alert Dialog box
        alertDialog.show();
    }

    private void connectDB() throws UnsupportedEncodingException {
        AppDatabase db = Room.databaseBuilder(getContext(),
                AppDatabase.class, "d2ep_db").build();

        visitDao = db.visitDao();
        taskDao = db.taskDao();
        Log.d("DBROOM", "connectDB: Unsynced tasks :"+ taskDao.loadUnsynced().length);
        //Log.d("DBROOM", "connectDB: Total Records: "+visitDao.loadAllVisits().length);

        if (taskDao.loadUnsynced().length == 0) {
           //fetchVisits();
//            fetchTasks();
            //fetchToken();
        }else {
            syncTasks();
        }
    }

    private void syncTasks() throws UnsupportedEncodingException {
        Log.d("2512", "syncTasks start: ");
        TaskDB[] tasks = taskDao.loadUnsynced();
        for (TaskDB vTemp: tasks) {
            updateTable(vTemp);
        }

        VisitDB[] visits = visitDao.loadUnsynced();

        for(VisitDB visitDB : visits){
            updateVisit(visitDB);
        }
//        fetchVisits();
//        fetchTasks();
    }

    private void updateVisit(VisitDB visitDB) throws UnsupportedEncodingException {

        String soql= "";
        //soql = "UPDATE Task SET status = 'Completed', Description = '"+comments+"' WHERE Id = '"+taskId+"' and WhatId = '"+visitId+"'";
        Map<String, Object> map = new HashMap<>();
        map.put("Status__c",visitDB.status);
        RestRequest restRequest = RestRequest.getRequestForUpdate(ApiVersionStrings.getVersionNumber(getContext()), "Visit__c",visitDB.visitId,map);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                getActivity().runOnUiThread(() -> {
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

            }
        });
    }

    private void updateTable(TaskDB task) throws UnsupportedEncodingException {
        String soql= "";
        //soql = "UPDATE Task SET status = 'Completed', Description = '"+comments+"' WHERE Id = '"+taskId+"' and WhatId = '"+visitId+"'";

        Map<String, Object> map = new HashMap<>();
        map.put("status",task.status);
        map.put("Description", task.description);
        RestRequest restRequest = RestRequest.getRequestForUpdate(ApiVersionStrings.getVersionNumber(getContext()), "Task",task.taskId,map);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                getActivity().runOnUiThread(() -> {
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
                                taskDB.status = task.description;
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

    private void fetchTasks() throws NullPointerException{
        ArrayList<TaskDB> fetchedTasks = new ArrayList<>();
        try {
            ownerId = client1.getJSONCredentials().getString("userId");
            //sendHighRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Priority__c='High' ORDER BY VisitStartTime__c ASC");
            //sendMissedRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Status__C='Missed' ORDER BY VisitStartTime__c DESC");
            //sendTotalRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
            String soql = "SELECT Id, WhatId, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where OwnerID ='"+ownerId+"'";
            RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);

            client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //listAdapter.clear();
                                Log.d("2512", "Result :" + result.toString());
                                JSONArray records = result.asJSONObject().getJSONArray("records");
//                            for (int i = 0; i < records.length(); i++) {
//                                listAdapter.add(records.getJSONObject(i).getString("Name"));
//                            }


                                fetchedTasks.clear();
                                for (int i = 0; i < records.length(); i++) {

                                        TaskDB vTemp = new TaskDB();
                                        vTemp.taskId = records.getJSONObject(i).getString("Id");
                                        vTemp.visitId = records.getJSONObject(i).getString("WhatId");
                                        vTemp.date =  records.getJSONObject(i).getString("ActivityDate");
                                        vTemp.subject = records.getJSONObject(i).getString("Subject");
                                        vTemp.status = records.getJSONObject(i).getString("Status");
                                        vTemp.priority = records.getJSONObject(i).getString("Priority");
                                        vTemp.description = records.getJSONObject(i).getString("Description");;
                                        vTemp.isSynced = true;


                                        fetchedTasks.add(vTemp);
                                    }
                                AsyncTask.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        addTasksToDB(fetchedTasks);
                                    }
                                });
                            } catch (Exception e){
                                onError(e);
                                Log.d("2514", "Login Error: "+e.toString());
                                AsyncTask.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        populateView();
                                    }
                                });

                            }
                        }
                    });
                }

                @Override
                public void onError(final Exception exception) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            Toast.makeText(getContext(),
//                                    "Unable to connect to Salesforce Server!",
//                                    Toast.LENGTH_SHORT).show();
                            tinyDB.putBoolean("Online", false);
                            //loadCachedVisits("Total");
                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    populateView();
                                }
                            });
                        }
                    });
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addRecordsToDB(ArrayList<VisitDB> visits) {
        Log.d("DBROOM", "Adding Records: "+visits.size());

        for(VisitDB visitDB : visits){
            visitDao.insertVisits(visitDB);
        }
//        if(visitDao.loadAllVisits().length ==0) {
//            for(VisitDB visitDB : visits){
//                visitDao.insertVisits(visitDB);
//            }
//            //visitDao.insertVisits(visitDB);
//        }else {
//            for(VisitDB visitDB : visits) {
//                visitDao.updateVisits(visitDB);
//            }
//        }
        Log.d("DBROOM", "Added Records: "+visitDao.loadAllVisits().length);
        //populateView();
    }

    private void addTasksToDB(ArrayList<TaskDB> taskDBS){
        Log.d("DBROOM", "Adding Tasks: "+taskDBS.size());

        for(TaskDB taskDB : taskDBS){
            taskDao.insertTask(taskDB);
        }

//        if(taskDao.loadAllTasks().length ==0) {
//            for(TaskDB taskDB : taskDBS){
//                taskDao.insertTask(taskDB);
//            }
//            //visitDao.insertVisits(visitDB);
//        }else {
//            for(TaskDB taskDB : taskDBS) {
//                taskDao.updateTasks(taskDB);
//            }
//        }
        Log.d("DBROOM", "Added Tasks: "+taskDao.loadAllTasks().length);
        populateView();
    }

    private void populateView() {
        highPriorityVisits.clear();
        totalVisits.clear();

        VisitDB[] visits = visitDao.loadAllVisits();
        Log.d("DBROOM", "populateView: "+visits.length);
        int highVisits = 0, missedVisit = 0;
        for (VisitDB v : visits) {
           int num = taskDao.loadVisitTasks(v.visitId).length;
            Visit visit = new Visit(v.distributorName,v.distributorAddress,v.time,v.time,v.visitId,v.status,v.priority      ,"Visit",num);
            if(v.ownerID.equals(ownerId)) {
                totalVisits.add(visit);
                if (v.priority.equals("High")) {
                    highVisits += 1;
                    highPriorityVisits.add(visit);
                }
                if (!v.status.equals("Completed")) {
                    missedVisit += 1;
                }
            }
            //Log.d("DBROOM", "New Visit: "+visit.getName() +" Synced: "+v.isSynced);
        }
        try {
            calculateMissed();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Log.d("DBROOM", "Total Views: "+totalVisits.size());
        Log.d("DBROOM", "Total Views on UI: "+totalVisits.size());

        VisitDashboardAdapter adapter = new VisitDashboardAdapter(totalVisits,getContext());

        if (isMissedSelected) {
            adapter = new VisitDashboardAdapter(missedVisits, getContext());
        }
        if (isHighSelected){
            adapter = new VisitDashboardAdapter(highPriorityVisits, getContext());
        }
        int finalHighVisits = highVisits;
        int finalMissedVisits = missedVisit;
        VisitDashboardAdapter finalAdapter = adapter;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rvVisits.setHasFixedSize(true);
                rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));
                rvVisits.setAdapter(finalAdapter);
                if (getView()==null){
                    return;
                }
                requireView().findViewById(R.id.constraintLayout).setVisibility(View.VISIBLE);
                requireView().findViewById(R.id.llDashbaord).setVisibility(View.VISIBLE);
                rvVisits.setVisibility(View.VISIBLE);
                requireView().findViewById(R.id.progressBar3).setVisibility(View.GONE);
                isLoaded = true;
                bVisits.setText("View All "+(totalVisits.size())+" Visits");
                if (isMissedSelected) {
                    bVisits.setText("View All "+(missedVisits.size())+" Visits");
                }
                if (isHighSelected){
                    bVisits.setText("View All "+(highPriorityVisits.size())+" Visits");
                }
                tvHighPrioNumber.setText(finalHighVisits +"");
                //tvMissedNumber.setText(finalMissedVisits +"");
            }
        });
//        try {
//            checkquery("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name,(SELECT Status, COUNT(id) cnt from Tasks GROUP BY ROLLUP(status)) from Visit__c where OwnerId='"+ownerId+"'","Composite Query");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
    }

//    @Override
//    public void onPause() {
//        super.onPause();
//    }

    private void connectSF(){
        String accountType =
                SalesforceSDKManager.getInstance().getAccountType();
        ClientManager.LoginOptions loginOptions =
                SalesforceSDKManager.getInstance().getLoginOptions();
//        RestClient.AuthTokenProvider authTokenProvider = new ClientManager.AccMgrAuthTokenProvider();
//        RestClient.ClientInfo clientInfo = new RestClient.ClientInfo()
//
//        RestClient client = new RestClient(accountType,"", HttpAccess.DEFAULT,authTokenProvider );
        //loginOptions.setLoginUrl("http://google.com");
// Get a rest client
        try{
        new ClientManager(getActivity(), accountType, loginOptions,
                false).
                getRestClient(getActivity(), new ClientManager.RestClientCallback() {
                    @Override
                    public void
                    authenticatedRestClient(RestClient client) {
                        if (client == null) {
                            SalesforceSDKManager.getInstance().
                                    logout(getActivity());
                            return;
                        }
                        // Cache the returned client
                        client1 = client;

                        String token = "";
                        String host = "";
                        try {
                            ownerId = client1.getJSONCredentials().getString("userId");
                            tinyDB.putString("ownerID", ownerId);
                            fetchDealerID();
                            Log.d("ClientInfo", "Profile ID: "+client.getJSONCredentials().toString());
                            JSONObject cred = client1.getJSONCredentials();
                            token = cred.getString("accessToken");
                            host = cred.getString("instanceUrl");

                            String finalToken1 = token;
                            String finalHost1 = host;
                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    //checkSalesOrderQuery(finalToken1,finalHost1);
                                    //checkQueryWithToken(finalToken1, finalHost1);
                                    fetchToken();
                                }
                            });

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String finalToken = token;
                        String finalHost = host;
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                //checkSalesOrderQuery(finalToken, finalHost);
//                                checkSalesOrderQuery(client.getAuthToken());
//                                checkSalesOrderQuery(client.getRefreshToken());
                            }
                        });
                        Log.d("2512", "Token: "+client.getRefreshToken());

                        ((TextView)((NavigationView)getView().getRootView().findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.tvUserName)).setText(client.getClientInfo().displayName);
                        ((TextView)((NavigationView)getView().getRootView().findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.tvUserEMail)).setText(client.getClientInfo().email);
                        Picasso.get().load(client.getClientInfo().thumbnailUrl).placeholder(R.drawable.user).into(((ImageView)((NavigationView)getView().getRootView().findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.ivUserDP)));

                    }
                });
    } catch (Exception e){
        Log.d("2512", "onCreate: ");
        getActivity().finish();
        startActivity(new Intent(getContext(), LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
    }

    void fetchDealerID() throws UnsupportedEncodingException {
        String soql = null;
        try {
            soql = "Select Id, Contact.Id, Contact.Name, Contact.Account.ID, Contact.Account.Name from User where id='"+client1.getJSONCredentials().getString("userId")+"'";
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
               getActivity(). runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            JSONArray records = result.asJSONObject().getJSONArray("records");
                            dealerID = records.getJSONObject(0).getJSONObject("Contact").getJSONObject("Account").getString("Id");
                            checkquery("select Product__r.name, name, ChargeMaster__r.Description__c, ChargeMaster__r.ChargeType__c, ChargeMaster__r.Amount__c, ChargeMaster__r.AmountType__c from ProductChargeConfiguration__c where Dealer__c='"+dealerID+"' and (Product__r.name= null or Product__c='01t3h0000019DJ0AAM') and Active__c = true AND IsValid__c = true","2515 Product Charges");
                            checkquery("SELECT ID, ChargeMaster__c, AccountGroup__c, ChargeMaster__r.Description__c, ChargeMaster__r.AmountType__c, ChargeMaster__r.Amount__c, ChargeMaster__r.ChargeType__c, Active__c, IsValid__c,Level__c, Dealer__c , DocumentType__c FROM DocumentChargeConfig__c where Active__c = true AND IsValid__c = true AND Level__c ='Dealer' AND AccountGroup__c =NULL AND Dealer__c ='"+dealerID+"' AND (DocumentType__c ='SALES ORDER' OR DocumentType__c =NULL)","2515 Dealer Charges");
                            checkquery("SELECT ID, ChargeMaster__c, AccountGroup__c, ChargeMaster__r.Description__c, ChargeMaster__r.AmountType__c, ChargeMaster__r.Amount__c, ChargeMaster__r.ChargeType__c, Dealer__c, DocumentType__c, Active__c, IsValid__c, Level__c FROM DocumentChargeConfig__c where  Active__c = true AND IsValid__c = true\n" +
                                    "and (DocumentType__c=null or DocumentType__c='SALES ORDER')","2515 Other Charges");

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
            }
        });
    }

    private void fetchVisits(){
        ArrayList<VisitDB> fetchedVisits = new ArrayList<>();
        try {
            //sendHighRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Priority__c='High' ORDER BY VisitStartTime__c ASC");
            //sendMissedRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Status__C='Missed' ORDER BY VisitStartTime__c DESC");
            //sendTotalRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
        String soql = "select Name,Status__c,DistributorToVisit__c, DistributorToVisit__r.Name, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, VisitStartTime__c, BeatPlanning__c, BeatPlanning__r.BeatTasks__c, OwnerId from Visit__c where BeatPlanning__r.FieldSalesRep__c ='"+ownerId+"'";
//        String soql = "select Id,Name,Status__c,DistributorToVisit__c, DistributorToVisit__r.Name, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, VisitStartTime__c, BeatPlanning__c, BeatPlanning__r.BeatTasks__c, OwnerId from Visit__c where ID = 'a1g3h000001wzcxAAA'";
        //String soql = "select Id,Name,BeatTasks__c from BeatPlanning__c where ID = 'a1f3h00000Fv2gyAAB'";

        //String soql = "select Id,Name,Status__c,DistributorToVisit__c, DistributorToVisit__r.Name, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, VisitStartTime__c, BeatPlanning__c, BeatPlanning__r.BeatTasks__c, OwnerId from Visit__c LIMIT 1";

        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);
        Log.d("VisitsNew", "Checking for query : " +soql);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            Log.d("VisitsNew", "Result :" + result.toString());
                            JSONArray records = result.asJSONObject().getJSONArray("records");

//                            for (int i = 0; i < records.length(); i++) {
//                                listAdapter.add(records.getJSONObject(i).getString("Name"));
//                            }

                            fetchedVisits.clear();
                            for (int i = 0; i < records.length(); i++) {
                                if ((records.getJSONObject(i).has("DistributorToVisit__r") && records.getJSONObject(i).getString("DistributorToVisit__r") != "null") || (records.getJSONObject(i).has("RetailerToVisit__r") && records.getJSONObject(i).getString("RetailerToVisit__r") != "null")) {
                                    String distName = "null";
                                    String retName = "null";
                                    String distAddress = "null";
                                    String retAddress = "null";
                                    if (records.getJSONObject(i).getString("DistributorToVisit__r") != "null"){
                                        distName = records.getJSONObject(i).getJSONObject("DistributorToVisit__r").getString("Name");
                                        distAddress = records.getJSONObject(i).getJSONObject("DistributorToVisit__r").getString("BillingStreet");
                                    }

                                    if (records.getJSONObject(i).getString("RetailerToVisit__r") != "null"){
                                        retName = records.getJSONObject(i).getJSONObject("RetailerToVisit__r").getString("Name");
                                        retAddress = records.getJSONObject(i).getJSONObject("RetailerToVisit__r").getString("MailingStreet");
                                    }

                                    String time = records.getJSONObject(i).getString("VisitStartTime__c");
                                    String prio = records.getJSONObject(i).getString("Status__c");
                                    time = time.replace("T", ", ").substring(0, time.length() - 8);
                                    String id = records.getJSONObject(i).getString("Id");
                                    String date = time.split(",")[0];

                                    VisitDB vTemp = new VisitDB();
                                    vTemp.name = records.getJSONObject(i).getString("Name");
                                    vTemp.visitId = id;
                                    vTemp.distributorName = distName;
                                    vTemp.time = time;
                                    vTemp.beatPlanning = records.getJSONObject(i).getJSONObject("BeatPlanning__r").getString("Name");
                                    vTemp.beatLocation = records.getJSONObject(i).getJSONObject("BeatLocation__r").getString("Name");
                                    vTemp.distributorAddress = distAddress;
                                    vTemp.retailerName = retName;
                                    vTemp.retailerAddress = retAddress;
                                    vTemp.isSynced = true;
                                    vTemp.ownerID = ownerId;
                                    vTemp.status = prio;
                                    vTemp.priority = records.getJSONObject(i).getString("Priority__c");

                                    fetchedVisits.add(vTemp);
                                }
                            }
                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    addRecordsToDB(fetchedVisits);
                                }
                            });
                            tinyDB.putBoolean("Online", true);
                            ((HostActivity)getActivity()).updateStatusBarColor("#1da1f2");
                        } catch (Exception e){
                            onError(e);
                            Log.e("2512", "Login Error: "+e.toString());
//                            AsyncTask.execute(new Runnable() {
//                                @Override
//                                public void run() {
//                                    populateView();
//                                }
//                            });
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(getContext(),
//                                "Unable to connect to Salesforce Server!",
//                                Toast.LENGTH_LONG).show();
                        tinyDB.putBoolean("Online", false);

                        ((HostActivity)getActivity()).updateStatusBarColor("#FF675B");
                        //loadCachedVisits("Total");
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                populateView();
                            }
                        });
                    }
                });
            }
        });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isLoaded = false;
        Log.d("2512", "onViewCreated: ");
        highPriorityVisits = new ArrayList<>();
        cachedVisitsHigh = new ArrayList<>();
        cachedVisitsTotal = new ArrayList<>();
        totalVisits = new ArrayList<>();
        missedVisits = new ArrayList<>();
        tinyDB = new TinyDB(getContext());
        String accountType =
                SalesforceSDKManager.getInstance().getAccountType();

        ClientManager.LoginOptions loginOptions =
                SalesforceSDKManager.getInstance().getLoginOptions();
// Get a rest client
        try {
            new ClientManager(getActivity(), accountType, loginOptions,
                    false).
                    getRestClient(getActivity(), new ClientManager.RestClientCallback() {
                                @Override
                                public void
                                authenticatedRestClient(RestClient client) {
                                    if (client == null) {
                                        SalesforceSDKManager.getInstance().
                                                logout(getActivity());
                                        return;
                                    }
                                    // Cache the returned client
                                    client1 = client;

                                }
                            }
                    );
        } catch (Exception e){
            Log.d("2512", "onCreate: ");

            requireActivity().finish();
            requireContext().startActivity(new Intent(getContext(), LoginActivity.class));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(isLoaded){
            Log.d("2512", "onResume: Is Loaded = true");
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    populateView();
                }
            });
        }
        if (!isLoaded){
            Log.d("2512", "onResume: Is Loaded = false");
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    //fetchToken();
                }
            });

            connectSF();


//            String accountType =
//                    SalesforceSDKManager.getInstance().getAccountType();
//
//            ClientManager.LoginOptions loginOptions =
//                    SalesforceSDKManager.getInstance().getLoginOptions();
//// Get a rest client
//            new ClientManager(getActivity(), accountType, loginOptions,
//                    SalesforceSDKManager.getInstance().
//                            shouldLogoutWhenTokenRevoked()).
//                    getRestClient(getActivity(), new ClientManager.RestClientCallback() {
//                                @Override
//                                public void
//                                authenticatedRestClient(RestClient client) {
//                                    if (client == null) {
//                                        SalesforceSDKManager.getInstance().
//                                                logout(getActivity());
//                                        return;
//                                    }
//                                    // Cache the returned client
//                                    client1 = client;
//                                    try {
//                                        String ownerId = client.getJSONCredentials().getString("userId");
//                                        sendHighRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Priority__c='High' ORDER BY VisitStartTime__c ASC");
//                                        //sendMissedRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Status__C='Missed' ORDER BY VisitStartTime__c DESC");
//                                        sendTotalRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
//
//                                    } catch (UnsupportedEncodingException e) {
//                                        e.printStackTrace();
//                                    } catch (JSONException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//                    );
        }
    }

    private void checkSalesOrderQuery(String token, String host) {

        String json = "{\n" +
                " \"serviceRequest\": \n" +
                "        {\n" +
                "\t\t\t\"id\": \"null\",\n" +
                "            \"title\": \"Error Message 2\",\n" +
                "            \"description\": \"Error in the  sales order edit \",\n" +
                "            \"deviceDetails\": \"Samsung s22\",\n" +
                "            \"moduleName\": \"Sales Order\",\n" +
                "            \"status\": \"Draft\",\n" +
                "\t\t\t\"binaryCode\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAIAAABMXPacAABsGklEQVR4nJT9WYxl2ZYYhq219t5nuGPMERk5TzXXq3pzvx7Yc7O72ZZkiYIkQAYM2LAByTZIGxAMw4RAwD+WYUCG9SFAMERzEGGTFk2JZKubZM/9hu43Vr1XWZVzRmbGHHHnM+y911r+OPdGRlZlselb72Xce+455+6z5mmvhf+Pv/sPEJFICBUAAKF5qc7/vjgEeu57AEA9d/xTL33p2CtOeOlO587Rz7nfX3i7c1e+fGd8xYI+c+3nfa2f+2Pnv1msXV/6ePa2ORkXy1EABRaMzImxaAhFSRUVEF9ccQbdl+D80qM153x6GfjpNXze6+wZXjoXX/1bi2teIpKX7/bq9y8v8NOgefEJP7PiV5DXy2TwMmbn1PlqAvrMUQUAIvTRIiqqKOh5MngJLp95tJc44hWL/Cwd/EWvOepeSVavPvcz3/7r/NqnEfOqa14Jbv1X/Jl/+NRB/SzYXr5WFFRVwRIoIgAA4gvA4jmWAXiJqvHlw3OYvUSTeO7bf70nPLsCX3nVqw58mgtexRWfvhbPDuj81z5zc32ViAWc/2ZDyC+er7lE9QVY5rdUfGlBeIahxVHEZgFkqRFJ2Mimlxd3duBTwv/coj919FPMga9EwasF8qvv+dkzEF6J27+Qc+A8WX7OT3y+JniFynohJj4rEl5+cDzD0DkaBwAEVHvu6DkkvqSUXv7pfz1kLKThK8QVvBKAr3y9TL4NTfyFF75Yxufpgs//oVcbBoCfRQ0CflZ1n6Pal2UCfubgHB9oEQHxBfHqS5f8xVDSz6fXl42kT2PiM5e9+P1X0+FfQLwvY+dVdzgTb59vEH3OIl+iuZdI8xWLeeWD4ae+QEUFUCSyn5XfCgvxp58H3E/93Cs1/4vn/axV+LlK/qWV/v+tzBdS/V8h4BDOg+1zUKGff5+XgP4q8LxKN52/YqHnCABAFCw2ynehAxaqZs4hf4EWnR/4NH7hbPl4/i6fElYLTODniZXzhvOrX58D6VdIyFeTEp4Z0p+hAng1fb/65z+jcj+tqBEQEBtpg4iAIIKiUdUaJCRVkDOINOT5stI+v8rPER7nl3TeLoCF9aCfBxdcyNjPA/TnAuKz2ukzUvyz9v1L539aPnzaMPv0wZcOn/v4MlucAz8i0hzoiASgquhEokpVVKeFWkOEqnomLwBUP6VdVF++72ef+jzC9BU2IqieR4jqp4Wb4iuh8DlK93NMr5fueYbhVyuzV9g15+5xDrLnxMGrpMtnADMHOgBQA/G5ClcBA4BBjieT3f3n40k5HEx86a0hak6ABRWqwmc9os9afeef8MwmW+iPl1hbARHPeXn6KuY+/4PnUPXZxz5/lp6Hwks3+9SRf12z67w39Km1LLTHp4TVyyzUiBkiAITmmQWJtS6nj4ZFcXIwmlTD4bQoK+9rFFVGa8kCqqKAzkn/BfWfyYWXGOIVwNOX4f3pKxexkAWbvPjynG5cePavePxXmYBnf/7VQZ5zF70EvM8aBmd3/pQt+YI3z5TW2V/EOczngl3nxxRYQbGqynvPh7PBwcnpeDwt6sAqIqyoCgqIRIjWOgsKCgIgOnePz5kpqufhu1gk4ssCBbS5Tl+Ir0+jYPHmvMg6Hw3CV0ob/awZ9ZKwgldy019oKn76dQZffMFQnwoQzHF9JqvnOEBEIMJGyYKKglCoi4en0+Hes5PBbDQpKh8kRpA5hAgAUIkQkYjQOgQgK4iAi/CEnD0cKKiqvGwyLnQ5QIO3BXWLaiO9XkLYC0gvmGtB9wtMvQz5Tzk3nzKgXoLmp6zJBV/oK1E5v/iVlvWn/aS54DtnnimgKgioiKqqgAoogBICgTGKyOjr6vFoOjnaHQymo0lR+SgsTZiNFOdWCC7UMQIioaI93ZssbSyRjWisigoAgOiCyFVRgOaWKSqoiAhHCBVwFHTOpeASQgYFK6Qv+Ld5K6ovCEbOMcoLTMzh9Sk2O6dXXgWx+b9zyL2IxjT8jy+w8gqr5pyBt+AmPHcjVVVRVhWRBtyN0FQAUWWAyBwZIcRQh1nt62kV4nA28+NxWdTBhyjc0Dtig29qVmrOgkuIQACARIr2D7777bSV9lqrVzdX06zlORazWVlWFrHTytvdtkkTNMiqMbCv6vFkdjo8rqrC+2gIW7lbbW9uXFjPllo2NxYAVQFAVAJLYEUia8gBIhghQFSdG4dn8J8jZQGAl7HwKRycSXI8Q5OCoqqKCKg0UpiQiGjBpS9bdIoLWQswN1XOqE0bGR1YIrOIsCwsQ1AGEAZk8HU8HVehOJxMi8m0qn2MzDGwijaiYEHvczJeiLIzK77RH4qIqGhPB1MZTgdusL/7xBoSURZREQAwiNZZ56y1BAAxsg8xskYWjqwsAGoMHbrhw93HnW6aJEtrvXZqLQMWPo6nhchEgQ11t1ZX1rfWk7ZxSKigSoAANLeazsFf9RyBNofxJamEcxIVEeYYObAElhC58jFGBoXE2lbm8tQ5a4iIiLCJOIqIKLPwgrIJkQgJqUGrqApLFIkN7YOqigKiAIp6748HdSgPpzM/ndV1iDFEbe4lc+WAoA29N2S/0NBzDnthOaoiKiKhgHUELMCRa+aApLiwYgEYgFnrOhI1dqTKXCuDQVSiBlwhRGauipLM+GAPEbBxjYnmokB1fHy013qa5fny9tpaq9OGNDEJogNjwQJYQAMGiRpI6DlM0MLI0wZAIiHEKoRZHevahxgjcxQRVhVVFlAgwoEha02WuHaWZIlLrFkIUBUVFpkr9zmBNs+mqsDa/I4KALGC4KSsBieDshpPi7qqQ4jCUXQOdAVVBEVqbGt6oc1f8Osc/OfsDAVEBCUkJLDOIBKaBVOcZ0mAuT7CxUobda8KzQrOk6YCsDSico51EUKcwxI4xjCdTcrB6aF1xlhrLZHJjUmoZfvttNNpJVlmDCmoD8HXkSOTqjXkrDHWIGIU9T7WPsbIUYSZJfKZoEYFAkACQFBh7yV6LitvjUkTmzhnLc1DD2dKu4nKg6o01K4s2tD7tKjHo6KqTydTX9Y+Rm4oXWROIXMzqTE/4ZyfhudJHRd27dyK0rmn2tA5EoK1BlEREaiBM9ELuaWLuyxiGEjUyAUVaBAwt4tf4ZviOYNxvmIBVBH2SlGMsdYV1qmZhNNpeXo4WjCsATSAgihICgRISIREBolQUXlOfYhgEAwCEr2ka3Ghh0FBmEUq5ujZuoYnVVQa8xWJyND82QRAoCxlNJjV9dFkVteeQ2wEFpyZD9T8BuFnXOAXTos2IuAFYs7kzwtXhACQELXhAGigPsfAHKnnkdhAZuFbq+LcOl3c7IWMe8mhPbOgz5s4C2sFURWEKxBgmJscCIBExjpjEkICQrSkCMwgkRGY5r4PzP+lswV/KgXyAiBzjpQY6zmBNLaeYqMzCRWD17riEE9npa/q6INII62kIfQXVuGL237KdAVYWGOwSBScUT8AAuriP1AEbcQJIVhrLQDR/CnO/n+eAxaGGi3sPkVQBQMA88eHMzNicdWZZX7eJTgHkxdibS4ICBdqC0BZVZqVWEBLRKZRD+ef6QV1zPU4vpQJnGPoRSLqDCxIiqwqAr6GUPkYZ1VV1oFDFGZl1jPbea7GXn6Mz77OvkRceP24wMXZas+ED87dx4acrXWWGhomwLmlOldNC0g1PL0wpl78UTyPgMVazhSDIgKahT+jCxIlPdOsZ04Jonnh8SGiafSxdZQ4stYgGSKaa+mFzb6guoV4+MzrBe8CNMtgaTQ5FKXEcubDuPYcosbIHFVkHndE1DMb4iXGPi/pzjO1vnDwcB5pPCeFFlc2bACqgDo3QwEskUEy58X+3K05Aw/hQuKdWwAC6hk2EDBBNHNnvJFNC/n9wl9SgbnSw4WMXnBcg5GFx42O0KEzaAkskSGDjTlpDBHhgiDnFKWNE76gizOAN4ABAG3cV1DFOuhs6EMc+MZhEmAWYRBFRUB6mb/Oc+65+MqCvBQWxNVQri64oJEF5532OU6aagdEBFmkBdS61AkQLm42N23OeGSuAKlhkAVzGkCDgKACQIACSBaddZYURDGwCCAQgSOwCMaAggFVQ2SNJRNZGBDAAGhCaBrNKkqgBhGtaYiHVF0jfuYScq6FCJUQTBNV0QbCIIs4SfPEjffUuAuBdTAOXJQ+TmOMkYVZubHlFIEa1wlfgrC++HQW3oXz7K2LxNXCpjofTjxPBmcXzpUJKAI1agABrHUpKCmwAoPC3LZXPaPNRvhbIkCMDVjAAhGQ1YiQtSBJ0IAikLFGwaG0kFupaWcuMEQOWZ5mZIJgFE7TNCHLrByZkiSqoiUJUaJW0bvUOATPFKqgqKySOQsiQURZAA0iWUKL6ixao2RNY6GwiBcGVRIV0dj4UzH6Og5HdayKyJ5FIkdhndM7ABo8g/MiQAULj3CBTdUze2dRr6baWOcKDUEv+O8VocO5LwkLrlhg7cyUsERm7sGDU2hkSMSGZxCRLKAFREVCSwmQWodZbpIkkg2mlXb7nTyRJG0jdjAkKmwpn5XlNBQF1xQZktkoDRPWBIOaOgLMarCgRsFFDEFDVHBoME1AHYYQeVKR4c5y3srSSQ3RywyEya20bDsVMOiIFISMMSJERpUjIoBRFIUILMHrNMC0CHEyZalZYuOrIiZohJTxpWjUiwq/hbWJnxb6Z3SvDRwbkgYEXJC2zvXrOc0BMNfHesaaMOeWMz62hEQoqsTNV+gQcoCFyEZnE5v3EpckRkyv0+4vd9I0BTJR7ckoPi7tifTjbvXs+Z7EfQBf++Pog6/nTCeIvpYYlEGFkIOwZ+bAqCJsAIFIySIHa8A46yMEHyxwO6MkTaKPUQTIGOd67aTTcYRiKEnIGJO6rN1aXW5Zl7vg0mTsZTSdhZmCAgoCJpC0wBvESCigjKjUuMOLqNO5/xrf8syYwzP0vCSSdJG+miu2xkqFhYV5Hsj6wuybWz4LNQFACERoEKxFbcJ0BkjRMmaKttfrrXTy5bWldrdVq3CMo3qqmKw642v48b2j09PTop6V0ZXBV37oY6hCqKpCvY9RrckBbGKQUQFJWVFBVAAVVSQwSxRU54zXxqMFUGUJCIhoraGS/WTsFRCAiChJE+vc0VFZlSUQOucSgwZUvRcfXOpWN5Yvbq+7bElhNXJQDblpOZu1Mlc6q8oSVb0XjSJMTVToPBpUAWnxce7sNvT+wrNZKAScexDnAKwLHfDCCWhs0oU1ri+8sIVdo8agRbXOCCoREVPCmri8t7S2ZDr9NHGF59PnRbrcVqjuntD43idGB5HdycnxpBQWq4BgjHPkEptZNg58jAaZAzPj2JeT6TQEjiwIyjE0WQMEFZGgEuf8PI9tgAoiGmMBgGMgjWgsEJ25HwBNsoIQgKM3hISQGkTCZ7vuo48ypCTrdFdWVhLnUktrS/nGymp/5XJJSVDSvFZfcxlEIuA8sNAYsQvJonNMNFIJVZVfhA8aKm4szgawCzuooewX9K1n0ucFehAUkeYK1aAhIFQCtdYSoWXXTiBZ7vQ3r17aP338yXe/e3w0nIwKRWgv9ba2vqK5nk52h6fT1eXlqGRUQHyIMUqsuZ5MxoPJsK6K4Gv2gbmRgRxh7k8RAioTKAEaIiDiuTM25woQFVZrnc1dq5Wzr4vxpK5mzpESKCgziyKSJTJESqQqUkWuJAoLKIJNkJziabJzkLkkcwa53t7qf+Hd26sX31jtb1Rsa+e8LaH0EKKoKC74QOcVynN5rdrY8/MYpCriIsCvC8I+C6ydhWsXavzMZ4aFCENEQ0iEhtA02RhQAEEVS2Q0yVPN8rW1Oou/+y/+6c6jZ9PpLComedsYW55O1X24tfa1Xu/d/ee/P4QhxfL5s92j/eO6rHyoo4SASs44Q5aAEMEAIgGgQzs3fFVBCEQMIhKKiiEE0zixCqSiagymmVvdXL5y+WIMvP9893B/X4InUgSwZIBsBBKiuVtH5IwVsY0Fh2pU0aBi9J4j1yASykfD2XR069bxtWuXL155K8/SU7BAJQaPZRRhbUJLKnMGmMuXM9bUhfNEjaadWz4N37xI7uG593Oax4VaRgRj0BI2LEs419mNdrEGICHrE/v47vcePd45nlWmu5YsbWVkrLHgK2R/ejLt9Z5nratpyz598vh07+lkMiJVUkzIWDIJAZBpkNw4R4QGiRbStMm0iCFI08S6pPa+9rUKGGpSfcKAqjgtffHsYDSY5klCKMZZjpFVgNCZNEkzjTwrqiRBArBkiMjarNXpObLTce3rCCwoYlEABQmssaWvdx4+Gh0dj4bjtWtf7a53au6Voca0wjJIFVVZlZt87plSOCeUYKFfGzdqIXDwzLRveEFABV+oXW3iU03KoTF1CIEA6ExFq4KCbamZTvn7979zNBxt3rp24/pPVxX4EUcVQoM8tH7fHzwf+8PlSxd7mysHOzun40lOkCUJkANFZmBVQLJIzhmXOpskNklcmiChS2yW5y5JiEyv111e6VrXqevp8PhkNBj62hdVCApZpwNgQ+0n01E1GU2nZRPjcGliE7t99eqVq9e7eXJwePyn3/w+i7cOF3ESWllfeeOt14xdL4vDyXA6OBqcHJzMxmPxhU0pSxMyNJlOdh4+UbQ8u7Zx8waRmdkEqAQTtPYQoyrP0wEqcwE0B/5CSZ9pgzOdsHiDqKoG0agqAOM82jwPtxkEagJpZ+kN1SYvASp2Z3Dy+HQ2SHsb7/776etjX0JR1tWSD1VAiQlmbVp13eOCpYeStVbzlCxqYt3mxlpvbSNv5a08b7WyrJW5NEVnwHQACYAUDaI2P6pIAIBIkGaUUzKV5c3rpJ5nUofRpC5LD8en09l00jGIGuqyEBZjM5OkeTtfWVvpdOzy0oW1m18ZDU9/+MOPiQAtgGL08fDg9NqNeuO9dTu6mi9NVy6H7emDg6eH+0+e+NmYFSKRCIxnxbPHT6eTCerpyrX3vUlDyyDVQgZ8rTEqoyKfg3njEyz+WbxZBP70hYG5sHgWoTtEVFrAvSF8XFimqk26mRu7w+7sD8LGW0vbX6H2JEyMF/AUffCBI0U2UBBJu5POqkrqgbM+zdJuK3nt+rWvff19b5ZS175x+UJh9LCq46hkQiEQbTY9ASA25QNzymFBjOoFUxRrwbUwde50xcTHLLMosShnEko0RNYCgk3ypJVHwI9/8uDO9z5c6ra+/MW3v/rez06GwydP9lISRTRA9Wj0w+9+9HVn7eqXYgdjBAMXtl6/vnxx/endB/XxIYRoiBipDuHg+V5dFTdC7F/8SrrSnlECaNQQeg+MKggi89qHRQzoPBpevBaWEC4qHmguiwiB55Z+I/EX9UuiACJyFvNrDP/85v/UbEzBTokBEjCDWVqwtZm21BpOlNoZ8O5K5Z+rKJEBojy1V69sLRn75PjZ8WQ8Ouz2L79Fmx1aSlQCitBcs4ECqGlyMaqii0o9RELwrHXiR49mk+JkNCki+6KsZqcOpcngKgALxxhVRUKhsTg8GP/xH89+66+0fuZnv3Z88Dt1qG2ioErKs8HpRx89fuv9vq5ekgjaAvXs2ptX37aDZ73h810/HbayrNXJo/fFePj00QMEvtj+GchbMwYwVqkAj8CoGmHurs1dAoXzhmqTLF6kxRbWKMxFEqASgBJIY3GaJo6gYRE0FABFgibCRSAWN711bNEQsVRsDXYupqKGxZADEuOkrDFH4zDv4cQbIge8sdp/c2PdS3hSHH/y44+uAlzb+nKNjBhAwsK0m/vgCqiCALiIdzbq38G0mI6Gn3zyaFh5QqqqejIe5WkTfVWjqD6y1AiaSDAOyZD3k0c7u7/2W7/56N6j73znR0CixgKRVT7a3d1d7V9x69o2jIlAjQaTbG3zajdr5U8/ujOeVCtLPZMmgjoaDh58PKtms97rX+2ubfkiFwJGAEZhgia8Os/6AYAKq7KI8sLjkqaWcC5v5rEHWYggS6gIjQ4gVRZVBUFUMgCA80oBFBK1ZIIlY1EJIfooUTBYRuCiCoUHX8NaGjWoorFtwGGeuk5i3stbl66tLPfsn1M82j042t1/PVQdm84kERVhiY35s4iZzMWQNAakQURMbWepH+KlK6/BRoC6rIang1AXwXtjcW2164sqhuBIDCIaYEYkyhKz//Tp4bNP3nv/i08fPtk/PU0sC5CCaKgefPJgba3bWX4viEHLyhWRNZRtXny713I7H31ycjpZX++7VivUEGK5u/Og9PXVr/0ctS9gmTtA8YRJIiGKkHhBxAigymBUMGIMiBG0AT29IPu5/4xzw2AecFaHYAi8NJG/uUNAiG1Ei2qMQGTrFIwPNkOtamGvEnkYvI8+1lErVfYHiqzCiiUjWERyqMsEnZZqbL19Mf+g1zosiwDUStJuJYFIgQUERNRQiciiYM6MBwOMOSXCevLw0Qc//MnRdBYVHWLwtYIYC9bQUr87iqLRW1RDIMworKDMWowHjx4c/ZVf/fWdL987+J0/5KBoUdCiQignH//k3tdbSbL5hk9aLNIELsmGHtx47X33/M4nxbTs9nLjEiAJvtx/eLeu6u1r17be+UYCXRts0BhQZBaSysCkCrmbhrrmGFxK1irUwBGEkHme12iqtmSRIEE1NmYUEhSLah0KYhTVgAagnUKeYYIiyJ7j9LC2mQqiEkEoa9AIChyUYwCIhIlABFAEtgAwrVEQgCSyib7bSep6Qla7uTsaTbjEtQ1X1QUlBgDB0YTRFOWlbtdbHTMCh4gQQmjlrlcMn0T74OGjT548IYMBVIQJkIRJVQGFBZtcu6iCqIgKs6gCpInsPt8lrt5897WPP7q783wvQRYgQkCuj3afPb7ff2/5os/7IWvFqmIfQYQ62Iatm2/p/sNHVVGnmSOXAkEEHuw9qcsZgt74yi+1e92ne6PB4wGz333+YWJxeaXn0i10tpOlmrmqJsy8QsAIygzKTXkmAVuC1GBi1JJPlTX4UAZAdLnJckJEVDAI6lSAESNOQx1Lu2HGkdwEDIRAiALGRHBqDTpBZiDSmsAYZ1EMKZBN6gjTENNYWwx9gHfWlh/tjU6f3//uUfJsZ38wHBMAB/airXa2ubmyfXvl1tV3ErMkBKez4sM//pOjw8PSy4Od5yJRCJHQGEVhAFEAFQGixhKfV7E1OS1RUQAghzhSeOPKrfe+8dW9f/zbHDw5FDEAyj7cuXP3xuWtlbe+GgJ4ioFU1AKIWzGt3qVOYvYeP6lnU5MYtIlJc+HJ6e5OHSJH/tJP/WraSsriwWhc/vHvfWtalnmWOuvaney11y+//9b1a9duh8pGVIkaQxQWBXUGU+tSC5YCS+UxMPl4XJZ+RhXTmBxSVBSRpgjAEDiCmmNZebtk66yd7hbxJE2aGKmzVmISa8/GcChARPyMAGwgUXQmqcR4k/Q2rthsNmi339zxf2f68W//D99Ea0v2LLGxxgAJQOmTB28c3bi+fn11u1/OwkkYfvjRncfHp4lxoY6IioIGX/j1SGDItLNswkNlFWlCD3PfXVVDBJe32VKdtDa/+N7l7//owYPHrRQiaRMwYAk/ufv4Z1cuuQtbqIoIEgTUWmNcBym/cZlod2ennkzIok1zaw0ZWwyP7nzwAQH89Fd/8aj7GuLdlfXl6dMixHJWzcYTPT46CL7+n1y6bTZSrqSuJYQEwKYOU6NgBGTiQzU9qvx0Jjw7rCZhNgaO2HjDAMAyVxvGoHNIVtXYhKLx9bLHoSYQgj8ZFPWur6uV5S9AL5NJphFKf+Cjd60g6JIkCWgGaKDfdTZr5Vo566OflmINqSFLCIQChohUJUS+e//5nTv3OrJ2HMvnj3ZGVQUiUQOoKIIBUIYIYmnuN2apyxLHkZkVQIBAVRvrPAJohHZ/KXPGqvu5y9snX/3yzrM9NcnScidrtZwxKCCo95/cy4+fkzPOmcS0kvUV203IGJOnJn/nIvUOnn0yHo6UIE1aKRkfw/7u07Jm7wVt7pJrK5sPdvcP8wSryUxDgAg/+t6d125d/ZWvf11tnREVMK2kEAeRVLzIqa+GZeXHh8Wgmg7Hk3FV1gYpS22aWGMIEY1NTJJ1k3Y3aXXSTo7GujTkUve6+Pjp7vMnD48HJYd4cHDw2hdm1/u/jKsJHxoPymVhdEp5j5xlYx9VFYAj9JnNl95ev/CdtZN7z1WtGlWyRMZZZ6xT1cQ5Zvzo3v7h8b9Ag8PhIAoa4zTKPFTOTSTOXru0HX2oyzJPsunEl15iQItgqCkFVUVgIELrnIuKTsK4ElS6evt2f32r220FZYhRQvAhHk/GcTwWERBNnFs+7K33O+3WhXS1t7beW3nniu1vdp5+cHp8xOpt2uotr0TRweDg8PnO8uZlkz5J0iRv2V4riwbrWZUoik0/+Mm9X33nout2WJ7Gx0XFpWq0oEFj5BoklKHiahYjG5f3816WJC5xmXOb1i0nrTTppldbnaXMJSahJFSVndy58yTo/un4+fPjwVSCuLooJ6PxzuPnN9+Il1Y7T0NV79dcVT3S7Ko5OXRk7ej0GEYHmLSJ8PrGzb/x7371P/+nre9+8KBmTBNjrEUyZBDRkLUKeHh8vHd4YC1KDAkRpqkajjGKqAqQoduv33znrZulZ6l9rOKTh8+iOCX1QkZBVBEEiSzZNO1ORsW9HzyEzeXpgztF6d988/WZmLIoq8jKESKHwCFGVeEYVLQqimJa7D23iXu+vNTdWO1ufO0rr9/qHnV/Bu792dHBvrBPkrzV7R6PijufPLxYUre3PD4ZqWpidWOlU7YyEq2C7O48+8MfP/j5r3Qnd6aD2XHlZ6icGUPGtI3NXUbZ0vWeM6tJayVJUkNEYNBlkBAmCBYRJJWYKVgDQlDbP/rhdwYTX8y48FRyWgYrPnLg0clAimknW05aMhoMyde1FGuyDgDqsr1B+fzpycVbuVGbdPXK9a/8Rz8P/5eA958fpKllNGgskG0yGCzAUQAh+oAqJE32DdGaptSenG23ckNLWarkwkxGorS5vmmd5RhsYgnJGgBQYAZWPyvuf/Lj8odVVVdJtwcuGVUhgkZlH2rxwQABoQgriyU0aELEyDxTnoyr/YPB9unkpN8yiieDwWRSVsUUuQYOjpIoOJlWhJVFpxEPjkZeuShCiraV2vX1jnCox6XwNFNvbJLbrJu0kqVWaztvt2xqE0fGGCIlQKMKEhkZgHVezQOICKxeYuFrsWydOsBMCdFJbtMUInBdhhDL+kGpt1AlxmBYJ/7I6w1SWe33waT/9//uW//2L//GrYtcFnR0ePyobveWl9OTiQ+MxhqbETlsnFoWVkGVxBpEQTTgY1MEb8gQKAKNT4ph74BFQ82TWSWmlVpCRKAE8Kzcq4n9SKhDVZSAyIqT4TRSQta6xOZpkvW7iYXNXjvLl0QDxwoARcFHUEARB6ICsaqmu88PIcQYgnJANXl39Qtv3djYvrK/PxwNT5mpt77S7jpfT1qdfGlpaW1paTVPrnbd2qaf1JVF1057icuXb3SW17PUrSaupQLEUSWqiCpLk4qcF34CgIIISIQowKw+Ui3WoLWOMjJpKwOb1zX4IojDqa+KyveTdrtbf5Db2XhYlv7p/Y9Weld+6Re+yLF4fveP/tY/+d22U0RR0EkVgrj1CxenFRMaRcMKiMa6DAwYIxxro4KEAGTAWDIiaq1FFYOINilqG1iYDdikv9zl2iNAO3GsglwZkHmKwxhjrSFAQC86K+u0SfIYdC5ppWm3m17/6Z+63LHT2tfjauKxiMqeYxXrkfGRQ31Sjse+kowosSYoqXEXL1698YVfU+HrW9X+IPWDGhS3btTIRWstbeW0lHa3lyCHGo7vuunhaKXb6VF/2dng0G+RsSRoMAJGIFYUYJaFG3MWXgVmYAZBAFUCl5K1lCwvpWhTcu3xsN5/ujMZzYxKUH5095PvpH9f1cwmg9F0EGO4evW9W9c2pJN9c5+yydNZLUfjgQ9eYkRC69I07yW9TNCyaowRFAAdOEoyIAQNcZ60EySlpuDXqJJCFKoYVEVQBQUMQkqW0DkLkclAYpFURBSIhEgQwDhAkyZtBEZQz772vqrLyif5d/5kL9lWQFTfVGAKEqvUPCqrSV1EtklnbaOTuk6et/Mr/a2Vq5fWwMg0GPLZslG62DKJDUq1xMrrcTHdvb/3PX9foZPZ9uqla908rFjiqVlPTU/rJNVamAWsiGmysQCswKKRWdg3exiAAQUQkYyZ7+L5xa/99LBWMradt4tZdXIypBhUmYjRQIwqmHb6rXbmbt26eeutb1x7/yuy2rn3+3vM0a5vqko4Gfr6uCo+rEo/KysvFMSqkiIaIjBOXZLmOZEHKbHJazOgtJxNk3y5ZT1yXfthFXwUELEisd3ZzhylqAZbIiNDIAoqhWhEMoLWGkemr2gVDKEQJQClAiIaTcGs2GaLBQJAXETDrKpE5S7VBMYhALrc9qzrqbGdNME61HFaiw9Yqe12buU5UmvGesqzWooYZ/WMir1iMH04ODpWNkDUSjv91upmL/3GG/qlDeimvvQFqppYAwA35WDsJUoMwsyiQMYYZ9Qag8Ig9uu//n8cFT86OT5k70Hi5mQ8Pjouy6KsCkVCtSeDUgtvnD2Zzq6Bf7y7B/tazH5A9nb7Yp/q0Lue5dk1B68nEI/H46On1WT/dFIczmbHZR1rFoZApkIiiREb8hcCKcRKlvc23r56sUP7I/HTYno4mxYn0+KIcLi29v7tN+HSysYMqS6qIKEIOqpiUUWVqIYosaIoQYUVQRRaiqARNCFmEAGwipG1qfAX1QAKwBWColQBAAFmOAF9ElSfKyBgEkFZWOLUME3TrXe/tu3Q9KE9BVOa3G7a7LLgn/fvPfofjgfOmkuJGSDupFb+7KPq2kZyZWPrN79sL1BUyFAKBo3RQ7MEZW5qRJtkjjCy1sL4d//swUBFmRGoDYYnJZyOysHEY+3r6e7Dj37/D35YlOOVldbVG5ff+elf2Fze7vaTO989qvxO77UvrS3ZbtJeabWXljJjDQZfxnowmZ0Oi91RWY8n1ZjKMKvirKzHvhpwlOBFInCEUHuXwjvXtt95++sFybHWKhyLWO3OqjDKDXYuXvjpm5e8QBHiVEIdhUVq4amw1MJFCEyh2WHuGFDFRy6Yg7JAVFZQ0Kgqoqzz1DuICgKdi+YjqwhXClaBAUmEhcUXYwjhUq/z5td+eaUfiqpVQQTjEJU2zA//0Sc/+PBH7DsGpxJDjLWKJwgtV9682v/Vr93+tQvUcr7wlRgWDaAikRfBbQJFQQDPHhj/9refuGCZFIMAcGRIyVgTL1yykzD7R//V9IMP/2ubmkvbvfXVN977Yn9t6eKlzY3Kx4+Ph4P905i0jTPdzK73Or000Rg5MgeufD0Lftf7qS/qsqpriVXkkjSwKKlIiBj8DKi12TLd1zIoE5FaCNCgcuCKTI4G7HKeLtk8iE44zDgqIDCLqoiGGMtBXVYhsud5njCKBpHGgW72evE8kCTNxybPgqigKk1ioqnthcVOO1USBvbB115jvdW3777+G1uv66xuRYSlLFnt6Y8n5g/+zt99+sSnRlWDclBlUhYJguHyhdZPvbv9C2/dug2nWd8xRZGJSoy+yU2BsEZV8DEK4//7Tx9YVkzSKCyCMbJRJUPd1eKf/f29b935w7qGrQvX++3cUtExk0668pWfefvypRtVVYdiclTMlJSDxsDWuNTYjjOO0IcwLcKoqo6jr0JdlKGOwUdhBilVBCIrR2bVlk1S4wAY1SO6BGOCFlExdRCB2FiDdgkxBS49ewBLlFg0xDFUZSgnsyoUdQiBZVFY2xQ6zBOwoqCioiCLROM8zThPGEFTT6+L4h4F0EayBRZmFl7vmy/e+JX0NYCQt7Ls2mpr9bL5h//PH//zf/nbGgENEwqRogoIC8eoYh1e3uq99/qF/9F7t7eXpyaWEAvSGAGaCqiIilWomO1w99FmfyMjUsuVBEZbe5wUsw9cemf/8e7kcr81slm30ilG8bFV6ni38r2SfZTATjAjiFnKmJoYsSxDXXpLKAos4NLMOetZEKKwxqg+YlQQFRZmjZHDLAaVqOJRa5AAoqQREBEtoTGQWMTOMG1nLWfTpqWCATbGYIbKbBAy4zSyD9Paz2sAVRnmYqexA0HnlfdNbhfO0PCipOqlYlxRgaZ6EVWPhv6Dx//yVvxGe9trAsF0OCxtvXej920zGHpHiqQAzMoCAsCo6gPd3ZkcjvRo5L/x2ps/f4m7UoS8pcqEkZQNiRJJjPZ3/t7fc630yuW1i+98bWt5PemstxJvXOfg2FfVrgSPpp3YtKrrjEggiVKnWZblhAFUCSMUNYiCISQAtegDaBSNHJnJOWMNudwl6qKEGJuHbLZgiTAqKUeOQUVUUNUIs6gRRWavsUZFAnUGEmcTl6VpkrjUGGfJJSVYC0TgjMW8q2gjH5UVcxQAaX5o3lHgRZfIOZEvqhMW5vmikurF13MGUVRB0IPTsop/uj1durDy3mSrtVrm7/Xt7/Xz0cwrqIggzKUbEDXlikZhOKj+8DtPHzw8+vbV/vWL3X/vRjvvY6UusV4oqBEAsiXYvb3R48f76Xc+6S73V9dWNtdXb3/p5754PX+yecGXdzsdSklOy0dslqztk42d1KEgqEFr8hY452d1mFbBByYFg2jJttuOAMazOlZMAs6lea4QSokgwqKK2mwKiwhiSATmG90BEZhAFdUoZSCoIjFCjLGoCy2KRoEZhMxCO03zrJUlXeNSm2Y5rDGeFrMi+rNy8zlU5cXHuVekqvPtqedLmmUO/cX/mkpQJMTRJEyKo9Ppn/jf/8be10YwsEygoFGarRaoRKgoTa5YARFJRQV29ovHB7NLG63DYfjKO+++lx/aLDEUTDq20djl5S6g874bYxjOwulkb+fZ0d0Hzy5tL213r65/8TfKalSpTRPLsQb2EEfFLJg+a2QURACDxhlNHQJwCFwr1KKA2HW00mt3RY4rX8y8bXXS9TY9HsEQAIxqaIrJjAIBCIIgMiIIgNGmMlORgAGUQFXVCpDMt2sHVK49TavaTGprBnmWt1p9cibvrKMtysmxL1X4vHQ5Q4aCKp6JoSbtA4siSRU82ynQnENIlgARRZn14MSzfPtq/ILGZ+PxFJuOCISL2tymtlHmPwKgQUCBoz7er3cHT36yM/z62xd+6QuXtmYnLUIAtNuXftlm3x6Nq7ryMTJIlBhHp5PxeNZqH69v3F9avpW2V2/Sl4IEYt9v37iYQghVjMCKIhKFlYVUHSJaE1Qj68jL2GtuKU/c1eUsLoedg3FdQHe9S12mp1OUUKsXARBCMU2FtAASkIKZ61OITaZ1vm+2KSMFNMaiGlUVQYnMAWpfTkrvEs2yvjMmy7ukE1+rMC6KmefbExel5HMLqLFEQRd7h+aoOav5BCQgQiSjBMQKANOpPtn9cHo6jl6dI1io/jNW0kXdlhKiswiArCwoUe7cOx6Oq8Pj0S9e6H/5ZjpFtl/5le7e0W8cn8wmjw+PB3cm49IXlZJRkKKsDw5H0+KjOjC4ZHntysbr737hzdXE50XtoxhFVJAQpYocgiAoiTY6CZFEcBbBA1uLN9eXt9rp86PBw6EqAWx2YT9wKHyolB2JEpAq4VxFSiM0GlIkVINWERSkKe5AbOJCgIooVplEkUW1jioTddaRzbKOpVj7at4yRuf7KaCpV0VdFMnMKz5FoamHYVDhqLDYlSSgTRANSAQkSl3UkxOOtReFZtflfH+hatPSQxe9bEAJAYkwtUSEBsjXPDidPHhqrqy126etJKtsVbR7JGs31sytlcHsjd3D0eT+w6PDu6Px1KhNWxkTTYqZgu+0DszwdX9STRwRUhnBppYSwyq1cBkjs6oAARokA8Y6g0QWgQNOirCSta4so+OjT4aeC265Vsx6quCrifBUmRC5qd8zqIacpVStRVUDTcOMSCAJIZoUCJumUyqqzMooyqoAagyARAmgCEbEKJKg8KJZEkgTj5SmBA7nto6SRULkEIFABSXyZOaByDq0lpqKJlVUAWBARRGWxvWY7yBF0KazCi5sKYSmHE1FFThEREWDGutOu3X56gVz8zefV/fMSbQPTvxmnobCtPK826Evb7Tp9srzwbu7O/v+ZITLy1IpJcZ0e+5C982MwTtQjsrBC5K0Wt1ZYCYOABIZVIQsoM2sazvDIgSSotZVNUjMaHT48ORktvN4MH5eMaXdd1ud7fb1bmeljeMqHIyreMqKCsiolWClErgUrgETBRbFKJEVRZnIgLKiikYBRCERD8KgRpSiRJFSVISAMRWoteErQRSiecQvgoCwkIFWnqUdu9Tp9bIbIoMYswePv7t/XDOrMfP9mtQUlCMAICMBqQDBvMGGAmDkBRsZACRmEFAVIG3y2rUCL622bty+trn+Cxc3gfm27/ftW0v9qOUpS6hLV5nxyLjEgbWXbm/TG5t1pOOxb/fa7GkZzXQCgSsLkGdZ5oxWMV0yHTVlxECmJlYwSNTNzOaSiTGelFQclofDIowOyupHw8FgMp75UKdZRjZF/D4Y83Rn2s/ay/3NJL/currpVrtLZqmqR8OD4+lxUUWINokCUSqOJQFEQC8R1BAlAIgYEJutegaQQUnnu08TIlKKZ/1GBEXVN9YXzQvFFdEAinUudWZ1+ed+6T9ofbQbn/72wwub254Pi1ndbNFAUNN0aRBqZIxFElBAZWGOgmciswl1GmuMUYsgiKgFz7KUbt+4dGX7C91rV2gpX+l0vv9H97j8Lv7P/hf/+7XVpax1sb+6mi2vc6tbhyko5wTIWk5YE+e6/SBRlG0IwGxUV3vWZt3Bzs7stOwsm81br4sNo4KnnhNDG0t50m1/8x/uldV9i12EUZL2+q+lF1aXszH85NH9r729OpqJr06fjurHd37w/OlBNZ2BKiZpu7u0vLLS7W+1+tfdOz3QdhiPq+MQJ4HFg6KAm/pJUU8AjAKpBBajYkAYYqXStDxCUVS0EYOXCiSKAnOtkTGAVTRIpBEAosSoYann1te381Z68dK1en/yePfPVJPhrBhPSwYNzMSSABIYZ51BC0DGkSgbC0GiIhnEoFBXVSiLKGJtmudtMi7UzBxaXX33nRvdd79EtKH7EA5/34IZTx5F73Hr6i9YZ9sOe73Oxvb61oXV1eV+vnFtq9Nd7S5ZgjJLBpWtSUlEZ1POeheptMvpd37nHz97vD86HYLE22/e+o2/+r/cutgOghxlMB384T+7s/P8B7NR3W21UgOd7nKWRbLy5TfeXvrC7a0WDWvfbtk0DgCW7Gl6eDh9eDJ78HhvPPvhaDILwQeJ5NKlpW7Wuo2ZQ9tONjaTnsEE49DXEygPuPDTECc+VpEFRVQkhsL7KvqpNGRrUYmUCAmcaZOGWEusKw4Ll4OjSgDgrY12Z+WtYvCRzqCp/hiMDwpf5928lWetxBJzHSVPE1FShrr2g9NTUi5CFSNb6zxQCKGbJzGKD9zp9LNWV4L0O9nGlZ+lbdx//Hg4eSqedXj4zrWt5UtfP3n8B3jzjV8azuoE2BkwBoOIc67Tbm2uLa+sr/SWeu1up5NebG9eaGXJcTkcPvp+P98UOfnx9z/cPz4VlhRxY2vzC1/9ra//+ldHZXV4ODIO93bqg4f3LG/44qOymGUtC+J3dh5nHXf19lWXdiZFHc2SdV2Lnb7tXdzO37jQyjLsu1VvR8cE3362d/T9b0mMjgBYhJEoSfPtNL1NrmPe3sROKaOKay4fVj4UoKxhj2QKuB5ZrV3NXOHyTvR7UcsILmoNEoIPxMFQTzBFcgllSGtl/QyRrGufHD2oRtEl4HJTlEerKy2X4nRScOWHhyf7hyflrJKoyoooxiloMA4ntY9oyNiiDGu97upyry5rQ+bmjeu95RUvWqPuFf6ETYjR+NgrR7/wpZ+6cS375re/bb/85rXd0zHEGGrvQzidFmUIk+Pj/cODNMk6vX4Qo6rdTrvfbZXTMWq4evlCTjA8Or57/4H3vpvlkWN76Y9rrlrvvLmUdtPMfPld+ni5P7l3XGVv1OEHs8pLrL3Lbt24vvTWzz/70Z/ffVZTOqakpfGJH53Qt05bpm5nePnqxctf+LXZ07siJ8vtG7btb/TTZejf3Y1HFZ9M9sa7v+99NA/cysqNtU43v5J0L7b6md5cyZfabwOk/+R3f/twb7B3Op2OBtbCZHiKzlCSzMpaAUUAoqTOCaI61+p0WaCaTpZWOltb6920Y/tXbQJZ225sXQrV0w++9ceHw6FERaClpc6US2usErLElbWlcTle6WaJT06Gk1aKBhi44GBOT48h+H7Ky+3b1teQtFbTxClEMDIpv/La5f/kP/43//O//7fSLLVXrl26eGMTkX19MpvWR8NBVZez2XgyGNU1O7KTWgFMNZgeHg4M4Ds3t43AZHB8/8GT0WicpGYyjU+ePBObROdeu3VpkvNgaMcueWPT7ZmN3U9m3S7tHRbPnx0GC8vdW70EB4fDUBj00WZHIOIjJUna7eWtRDfe+7ndH/3ux3cf52mCyYPZpPhzl25vrbHQrIgeEmOMiPjxbG/y4b3T/Yz8a69dvP6ly93VL00H09/943/y/W9+jzABkw5Oj1bWeu1OZp3LWh3npmVZtRJbFAVqLYhVXYzqUVFUdVVNh+7x3U+2Ni5dvRVM+4ZQOwIMhqfHp6etLGGSugrEkVAMMhENq9nhkZ+FcjqhVp76qnCaO0Zi5qKqRkODcnx0vHVxu23NL772pQ8nx9/64Y+Go/riZv8v/zv/weHRYRbX1i8tWyCowz4CIti8vXSpk5jEWIyz4Xg2mQXGk4lfWl6dTGbPnjy3SFvrKxwnD5483RseAyiBS7PUJGkV9NGj3e7qt5cvf41WpBiko7X+25u95WT72SNeWnmscXpwPCE5eHv7yp8amRWRVWRaUSBynHXdtYvrl6689ys/tfpf/HmY+FgrhNIfD31gGYbJcj9/9viwitLNk9wColiMs9GJJnT3o7sff/9HmfmdoDgcVy5vcSz6Pbu82mf1bWdn1UxAEmsK4KoOTbwZEVS9SGhlmDgkCb4uHz6441lWLk0hSVDi/r1PYjWd1GiAfJATrlm4rGMrSyTWs2llHEYfPMLacv/1G9e7rU41LYvx7OTwgIguXru6vr5SleVs8uBXv/Qzo+F+Sslf+o1//2uXe8Oj2a2LVysS+4tv3RjM/KPDSRmmgYsgUSESUrff7vdaPkrcPXGoy+1s1m05wjw1nnV//xAAjUlXlte/9MUvLC2vGGtnVVVOat09SE4TIWOP+MOvpF++uHW1l2VPMbGrRwf3VLvXl/NbV1+b1E/GM2ThaJQMby0nX/25r65cWjdDu75+rft8d29QW0qMS4Ds+srSmzd/o9v67dNBcXJyWkVJCFo5GYsS/d7peDIdWyRnU2ZYT8zpeDoqJt1OdjwaGGdZGAGyJInMKmqt9RwRWFFZIiAKC7BYANBwsPewjJXJsujr8emuhkoVDBAAaYwWQNULK3FwFh2YIEKqoaoP9w/HbmwJy1kJgEmntz8z42eBJbtzeHrl4A+vbd36n//8l3ZY7//42f/3j38wnuwKq03W85W+XdtsRyEmYwgqz5PJdFRU6HmA6JzUlU5Gfqlre4lb7tDhoJzMpgC6tbr6S7/485sXNn3Nwr6bpzFxw3LmldVRRJ1+c2T/3S/9ytJ1Z+zx8z9t97bIbR+f1q+9c+losPtJVQALIay35f2337hxY/zf/jd//PUv/+Kv/uY7o+L58XfvWWOzToqKlzfy1153g0GSW7fcIkOw3GuJyocfjEeDUlgtmGYfeh3C6WhU1ZV6ZIxRhVQMKscQm73ZQNZlRfCJBVQWjUi2CYMKIItCPa2mRz3br6oiSmWIm77aqKIRRQSYgQ0JW3TKTXUTTsvy8cG+L31CCNFcuXX9/a995enYTv2qnx2NQ/qDJ8VHj37wJ9/7IYI6Ag9oTJIm1vraa2jcbFSRqGScWdroL0HPsF4BmW64i+3NH+xMHz/8UQL4+lL3/keTsdSJad+6/Ua3vVLPhOsZa+2sMeR8tVfHNZNlbLgwPfyHP/zBv/f19y9tf+GXf/m7v/vNALN7ZXK1Vft33lvrfhR9FF+tr176D3/u6g/ufVKMpz9+vPPrV76w+ebPXnt2tHdUt4BvX13/S9tX7PST9a7FFvBSdxZiMSvLWdlf7q+trgqHB3fvQz1LEwuECgqRk9Q5BEZMbLPxIFhDIUYyAAgxRkOECjLvK4yEasiq1pEDqV/u2OgVSJoYUjNhYb7rGubbjBJrgg+22ZMunBiiVDnq0vrmz/7iN8pwBejUx5OiDrH0wCqYxRKc0XZqI4vUGAVtVQVlnBe6Nx2SlQGEAHNrE5vY4MZVlfHxytrrr6Umaw33d/cEdHN948r21boKjmqtJ6K+AoIUAfPT06M0TylbwlSOdfWH/6/vnPzld3+h0//p3/rL37t396ionh3OtBy1L16E0pukvrWV/vMPfnKwF5eX+uXx49/9g+y3vnpl+Zf/x48f7LfT9r/59e2sa0pf8HB2rzh9+xu/kly+Mtk/FLG58MPCDp6GfPPv3/nmdwEql9gQ2BCkBp3FyKiA1hhQQGPICCE5awjVWENkuGayRoMCgiGDqkpqDOVZlqdls8GNLKIgolLT8ISMtVbEgzT7T5GiZISZpeOCDeY/97PfsBs/8+g7//ywoNJLKGv2AbUpT0JWrTFGUVXDZbCErmnbzcI+cmQRUELMHCHZJMtypu/++MfP7j+4ePt659Y3Jvd2RtMiTdpXLl9st9vBl+SwDBURVjFOpsN8veWLoq4l7ajlJeLDI+nLb3/3937+nV9bvvBzb75+PB0ctFaOD/amO3crjVC0Dk6PjLkUfS2xU/hCn9559OwjpwCGpoH+q9/7mNxSK13pb2y++2/8hxcvrFTD8OdoZo+HChM/9mDN0qXfWH/bH35yx8WJseRSi01XBoKmMV5Tlg/ABJha5xAtEhEFJEPEyE01PYooap4k1iR5q5OQi1wbRKJ52Aek2XmqCCgAQESK0ccIUpZewN64fqO7+tVHP/y93ZM6qmEWiJ642d3FgKjAtWcWYCZLwTqw4oxxrD6oelQiBOdMnpnUOoPcTwyC7D19WhL9yjd+8buTYlT5Vru7ubWVZGY0mjx9dlAVk2tXrrhWp54MEu+7mXl+PA4sLojLDHI44Yz/4Mf/7N3jt7cuY8ivLbc3ljsfX9iqi6mfVSEkMhgYTNBWedq37h1hx+UjkRpdl0GM6YOka+9+YSNf8RXsTsvy2XRw4EMGUTmcjDGO+1e+HliG9z9OYGZswpFFucmzAItBmpfFKoKAM4Zk0flO0CAZAg6RVUANYlJW0aCxSiGqcXOQKyA0WygjB1FlQDDICopeMEvztY3lpa1rHz/60d7+VNFQ078CjRIZQ5ZINaqG6CtmiUFCLG2u1pCldlbHUIe2ijQtHiyBUXXWio9biesstU+Ojo0OpqNhzXFzbbXXbxXF6PR0//hkv67rLM2uXb9lEzeZji9fvjg8HRXTmXDkkCqfhmjLulf+eQhL2Hpt9dLW0nrSuZnHB8iIjAXG5b6motJ1jkyHKDCG17jylEdhVO5YTTaxzTVkEbqSuPXlbKRABmbKWAYF5+rbb//Gaccc3L1rsa5jzRwxKLJw5UWhyYSKQl17UJQoIKCMJiEvypEFwLh0ZWWp01uqSu8IWllWVQWxCs63t8QoqIqIQYyytdr0/kzzhDYub9eQFVWiXrI0jypRGEWVEBKyFmNd1cU0+FJiDaAgShDt3/79b2+tt969cmNpudfKWjabN1YzICBijDEZLq+019aXp7snEuvJdKoo25sr0ZeVjxprR0IJnR4fXdi4sLq6tPP8INGt5baZnpZMosCqMQQH1g+Hx893P2l9AqnFXsv2ujZLE0NbDJYhE3UKSpjqMUawQVQgTSZOlAEsZRjymu3MGdcDfb+TfXDBVUONWU8upPVB30+L9lpYu/TvXLrwhw8/+qQ+3DWKWUIaNbB4L0hRBBSprEIdQIWNhTqoS6j0MQYxxixvrN+6eW04nITaU+JWl1eKooihqeqkNHNRGZXyxHW7iTOZBg2iad7GPBGzbFxukwyMFa0pCjEDAljwvhqeTGJVogQiNiigElXFgP3Js/Dx473vfbC7vpRtby/dvPHa1Y2lJWfZIkcfIckSWt682Ft51D6ZBEBWsRaXui1lDyDOkTUkHMnI6enJxbUVDXWoZpvLrcHktJYgQbwE9FYBfO1DOYXoUQRBwChHn+e5cZlgIujQOOOsSywYB0BoKUuRDFmF1KVHf48/zNcT8gbbBleJcnf5QraVYyBvuBiqcCJU9l//mS923t99/gfHhyfVpIAIHBmGA+doMqtYlchZawA4yVw5pdIT2pYzYK3tLPUn01iUMTM2RiWgTmdZkYxJiqpq5W6Z0AC10jSqCTUg2zTPtq5enlR1CWpSBxrqYlJXtSo5myStrKqqWTn15YSU5yltaErlyBprs1ZHKio53tutPnry5E9+8PzahaWrl3rvXH1rdcmtt61tQwuX03a/085rK4jqDDhnrUMQcc4kzqqyM7aqCpEICNPJePPC1uWN+HzkCx85ekAQVvYefUCOFjRx5PLW6ZiLSQUYBRxRCmQAVQAFrSgpCJIQ0TyPKEHlJwiQWJummcvzpU86Ny5c3uhdUkwAAie2HjDlmnSz7Su/tLpFRfGkmp74SpbLcSxHz3cPa1/3e20UYGFKXX+16ZWcpXm+tNJLHUVfX00SR1jMZtNptXGr3VlesjYbjU5TR7lFo1CXRQxCalPTanVXbfLG0fEfPjw8OR2URoRAjEEyaZ6YKLUK5+2Wr2ZcsypEIES0lLSydHlt2WaJE+pADEgJuPbMxw8eFncejb/53Wdry/mFje6b6xvvvHkz1n55qV9Pp3Vdp846Z5p28M6aTjtXZRH0XHNdpKk7PDravrjdbuetCjwrhMgxaAgQIooSESAJGY7YTlsGERVFmm6YBqipb8I4r88RYOEIyk1q3qoCiAkM7ONpKKvi4ZPsSZ6avOOyziaRDscnYeZ9VSWpzZIEDGpiLLW7vXz70rax6Cxq8IkzlKcIQNgRrQVtYEQp2tl1Wr0QDx/qilPThgurk+cnIGFr+720c7qcq055CZ9ubty893DQDa6A3mnx3ZVO8mR3ZrXOLRI0RRQaZlIELSrpryxPkixUkOdZ0mrnebvTStudvJ0ba0TAkKIjMo6VrGoIHMKsqoc7s7sPju9uPH82OQ2l7/aWW5KBaDvPnXWKJIBkXafb9z6Oi0pjNR0O1lb6O492Ygx55lJbtTJbawgsIE3Vu7HWWWubDvZWiGA+G2ReNDzvgysIqMBN6Y4sytYWlUOmachGAlzr2PvK4Wha2nTGHKIP7EOsZsABWCwYVWMsfund6yv9K4r47tLG0gbUzIKlGFpd6acts3vydH9ncOHCawnI7mgkW6tf+uLtqZHf/+HQyV528avl3tPJ3r2bmyvTcrIzHe+Oal/GCSYXkwtjo8LqDCakiTVNPy1SFg0tR+PReDLEtY3VgU7ypNPtd13qLAlyRF9birVXUmrsT4sATQt7ZVQGJS0qUMFOf+V6atN8HclkSU7GMhJZp4wIapNMpxVLPDkZXlvdUuHJZLS2uZkexsqoOsTYzFAh67JOp2utQQKOPvjIgTWwSrNzAAVMU/sQREVZkZvWsdhsNRdAJTCETighcGjICHPT69uSE89SRa7KlKI1EZBBWMVExhDlgx//WPOsupHdyLpfsPDsZPfS2zeX1zIB3fn+4PnuQau7vt5uP3v20a1rt9OExgXUj//ccH27t/fR88eRC983RREQ4/qSFWsKpkqjqJRlmWepD/U8JNIUy6kgQObw6Pjw+vXtbpqfHJQogBz7uel3bEJql9pwNClrDxGckkOyYBCB0FjVTFGXOy5LNwD328sXXS5AJkkyQFPVddOEQoSNsSrCHCfTKXO0Wfp8b//y5UuZ5UkVHAlaQDHOmqV+z6ZZUcyK0SSURZSoggaMIQtk0SZ53l7ptNJuKxhwme10e4iFBEkNdbJNS0aFY5SiHofoEbvT6fBwd8YMpDaEoCKkQCAWlJr25wiKaK0NYoehi8nK7uztwSf//WYHf+xHG3IBQ1dLwQkPj8Y/Kr7fypLR5PTdd6+Eok5mHrme7u48xnrrRruUjqgkXdjAXr5kS5UVvfBwsBtDSFpZMtREuakdEubI4FmFKbOmKsazwenmxlYYl50El/tJb8kisVSVvXl1+wrYk8Hus8NyWkaWREVVGUGbSop+pyWSel+lHYN1iAJp1q48j6eFI7KkwNy0f4wsgavJZNRqZcPxGDFurPemxX7ho0EBg0mWJFZHJwdVXXrvEThJc8REAyU2tVlus7zd66YdS3nbtrdssp4uLyEZEk67sXUpsZagDlp5LiX66I+An/0k8rPUGGBmFZB5Rd2ijZ6SQTRonEGkJDeQXaxCNTie/uGwRleB+rKIdVWfSp222uPRsJqYItYb/TxY1Bxeu/3OnfFh9H5ZW7dWzYd36hD50mpqTHtaTtprrfpZLUBpKy1HA6lLRhIwREmapAnirAwSPXOoi9lqN12+vYaU1eXw8PmzJw8fOGS73LeTWXFxbfXC2spw/PDR88npxFc1m6aeWCqjWaie+ajtTjYup0VR2bw9LuqyjlOJyKHtjDIjEZFRNLPpxBlbF4UP4fLK1dFwVB8MJKozJnX07PHTGvDi1YsHu3uhKFZ7y+3+hnirQYBMVBiNC5iI2oG4XUyscQYMGWdbKbXvYGrSxPRTu2mprZiUcVrHYRRvo5JJVMSQVURRG1TF14mJLnWGNEnIWN3YumTbl9Q/p4zypR6YbHdgy3pW16VFWF5ud1pkkczYPj7QSy0zq/TJsweU9Wyr9eBxdeejqaoq448rfWNmDCDYyKhknSXgGAkozZK800uyDiDNqjJqrMaBMKwvd1qW9w4Pd58fHB08D7HwdZHlLXv10sbhwXAwmnkd315769oFenZ69/6j/dOjGXPgWE5ntqxdq5vVZA+OTuoQIlqAKIK1j3U5k9Q4JAVMkrTdXVIBk7g68mg0fu/dm2O5iET7e6MkSfLc3ZlM++ub61tbTx89mU2mSTYim6dZz1mnCiTNGCMWz7EWhigoxpAoHgeWKEQ2b7XT7JEhY60xzqSOiKgsp4kzNknyLEk6bUdLGuPp8V45G4oGytAgdDJsJ618OYPpdkyXfnr5zWc8dESTchYn5QW7YhO/dmltq9v91v2dLG33wAJSOZpe6C5/6fZPDeHp9+49/bXuJvnUh2ecrQ+hsgEv9LIRmNlkWlalS9PRdHZ0MvB1nE3LcVUKCCtG0SzHDz746PGTp6Ix1kXqMDEGAezuzils9Ta2lx9/dLBXnzq0N7avvr9y5e7ocHy6Nx6NZqMiqqx1LwTlqZe83RpO/VKrPfQBBJglMIhIWYf+cv/67Vujw4FJs7r2JwdHdHu8bpblEliwHJSMxLrut1qdvBtDTC2kUI+PnkWxBiwBpZlDS2iRHDpLLQeJw9QlRC6wC4F8IBWrFURUcqJMPdvZ2tx4vjPtZbi00en12oldNZixmN5q8vxR9MUUxHtfkHrSe2mBF/utp9NOtTw73Hty9EQNpSmgl7r2xf1RiKEuiurh6OSDTnc6KU9PTgPGxz/6vcO9w7KsH7jHofLT8awoqqKq0ZrIMbIUo2ER6gsX1p7vHR6NhsBsUBU1scDo1lbXEfXJ8+csTBCdAUfkAa219nj6fHZnR0xO5l0EFB+PhidHXN/sXLi4ufF8MHswfWpbly/f7MxCnGB7aWVlPN3ttbJqNjWUG4ir/dzPypPhyFi3urHFNdQhRtbB8akflVbJgLbbTQedmiOn1llqcahbhjILCJ7irK5DVQdmCTECKRoSwMaKSJ3rtDsua6Wtrst7xrUJkayzBgJXtYde3npSxx/+4KNWy6CArwMhGuuS1KERX5YkERT3d/aQrMIfSoxlMWOOMt+3jIgkCpHrINGggHBUEBEW8QJkkzRvEWjtvUbJbIJkLIJzRNYQaV169jUSKVFRBxaxICCKTWtKa65srESOngM1haSIgSWKRhH7pdtf1C7++Q93R+UPIi+RbXfSXlmeTIvnh/XWW9vrj58zETw7HEa0cVL2u+3MYJqmrTzvr652W8Zpvf9072BwWnomUONsrLyxdjKZHYaCjOG6EglZkrBvdjA4kcgcktwiEkJEYEvB2WCMWssKShYDS4wskatQ1eW0qqOgS7NOf2mjvbTikixzhlWCTdb63ZXl3t07H0oYNkWyDgHJtZbWNrfWZ7M6w+gSdzo+VcDADJETZ6yzHKNBKusIxhqXKTrnUmeANM77PBMF1qryrLy+ulRVVTWrCQnREEhiEUmcITBQSGy1O1mWRl8bVUuogqjKgO00vX714mQ8jjE4bcaFEAE4a1TYogsXOhv/9s+vf3+/eHjn+dgfzurhpJjFWoblvQn3b21dAzvZG43qYgbqM5ek1lqbdLv9brvlEuJZ6X1tDRLZ4aScFiGKpqkran9czra7neir6GsmwwomcZSkdRiziDGuaelIBCKiANgMN1NVEWAxqqZpAAmCVkEDxqmWNu2l6oOwcc5akOjd5bXVh/3u/t4xmdjMpkiMUR+Wl5bKcUWxyNLUECGAgJomso9NM2dCMmkrD0wisd9vIwHO49iCaIIAyKz0NTMnLuGEm2bRBM3QFmwmmSFqt98pZlXtQ7P1punUioTdbsfY5Ohgl4UtQhMebfbmoKr9B//o/7O+ufnW+ubG2+/f+Or2sVy7fzw+frB7cvLk8aOD+3c++Xjj7mu3L7z/3hvl0XS6f7JrTOIcCAcfjvYOg/DJ4bNQF2TS61evptaqamIxs7YsJ7Ni2u53VTiy1IEZnUsTm7lyVrPMx2ARAqIyMy4qK5t5rtTsMwJSAAWkZq+KiC9mXBfogMFai6jEsUI1r79xa3i8F3mqIIBWlaL31pi8243j2jmnAETYbMdv9mkYsAbIoeSt3I/KxNkkS5kFhFAFmp5wqkSaJhZAyRiXuKZUtGmK2OzIACS0Lsvz3d1DATWE8z0gBh2Z125d86qjaYEiDCwq2OwFZQ4c7Hf+5M82ty8OX7uxOjhd7bVf27r81vaVaffmJx9lSz2z92xvMBg+3sHXllxn7UJ2YeWIq06vhyKxrgeTWZB4PJo6gu3NtdXlDnuf2KaRL7DKrJjmOSCgCISoothqZ0mC09MTZG7GqiFCqIMP0VnTzC9qNlI0A5MEpOk0ON+CohpjKIuys5QGz6JMBiKbOoYLFy5c2Np4+rQw1OwNFmU/PD3tL60Oy1GSOpbY9MxQMk13aESwYDSocwlgbQ01yUuVpkk+qipwdASEmFqjSGQIFBCVdF5/bg0CqEsSQBpPZgiLzqgoal23t3z56tX7D3cmRUEqosyRAcQgehZRsi5zsZo9ffjw8f0HzrrvryxdvHltZbn39sXrK8tf9W+Ov/f0oObp03Fch3Ipp1W3PF0ZVTOPiL7yMdZI1O9kvXZejMc2yR2pcEwMEOjhwYncvGWdAy1CZCXKWykgzyaDlLTppazM3oemkh7grMflYqfKuY1zjQCJKuPJpNPrMoAvo7NIhipvbZq+8f77B3v7dZwZEpBIhgaHJ93+hnUWjSFLuBh4oTDf4WEQhZsxZSgCqGqQlAiaOQcqDbErQ5blZeWbHlM4b2wPMbBNHIBmSVbM6klR63x4FqFN2q3267dvzyp98GRvNi1zB8A839gAKEporBUfD+rx7tFpjIwsRrH1w482t9cf3Ly8dWm72+58eevilLdmx9NqUpSSZIDtTtsXdSfPCJRj7LXyViutyrqVRUJNHEREQiXEo6PTpyG08tyaWREVEdJ27r2fFYVrJrey1HUIzKoaIxuz6PsOwFHmjSFFcLFriFkEeFZMj04G65tro7KYjCOiAqBL0q2tSzfffP3DD39gVESDYZyORtV0nOU5EFmDICoqgNi0Y1TVKMIAwJpnSVEUVVmRtcDN1gKMgSeTmY+x2+0imXlhC54RiliDWWJjlmQunZZ1YAQ1SCRkW3l+6fKl9c2LR6czH42wKdg71GaMNpCzlGZ5y6qPjEYAFaAZJ1IPw/Hg9KM797qd9oWtzQevXV3d2szb3QvGmmmLwF4wdqjS67W6nVyR251EomdUIlLlZu4jx2gAZrNiVE7f6K7sGaNB1LhLV68NB+PJaGpAECWEUJZViKyivKjkb/QTzwe6LNo2g4LOe1eiM2VZZgarhMppiRJjZADsdbvvvP+VJw8eT4thAiLCs7o+Phm99caVqpyUUzbKKoyESChRmFElBobxtOy024PR+NnBKZKZdxQRAFUfYr+T97vdYlaFwAggKtj0c1EFkPFkVoWYpOnqSu9KkKOjUwVwWXb1yqVbN69GgFav9bO/8NOnx8dHu3tcV6rQWVrqr20mabeqC9vtdLXyjOIJ1CA3Xb9EBXgwHQ3ujh8+fLy8tHxxe+vK1e3NKxfe7V1NI6ClpN3p9vtqyRodz2b95RxBfV2lWaYK3nsD4EM4OTqm9ctpOsK69qK9Tp5at2tcEST6UNXVtCxEhBCVRQwCIosKNO12mpbNggDCzUwwQLJkXJKlZJN2jhpYOJbTSe0juXT7wvbbX/7an/3pN4UrzMi4vNNrpWk6PDmKPjqDCmAJg4hEkWZ3u8BkOrPGLC0vD6dTH8SRSY0BsgZp1UIrt5NJNZ4VgOgMCjAQRJHKx8qzD8oCo2LSb/FSq51vZ7PSX7926eqVCyfHJ3fu3vPGXb968drt6++8/6Ywjk8nVRlETe21lap998vvfvzJw8FgBApiwBpkMVFYRRqWiBL2Tg53D49/8OEn/V7vW5c3L1/YtFnSXV3tdttVVRbl1DibJIn3YTSeLq2tcgjMUVUVdf/gZHgjZO12VtSVL6eDsNTufvG9N+78qNg9Oq5iCLXPDDlHi4HU4LnZPDrfoQtqFCACKNmkm2ed3sqFC9sXt5zKZDgyzvmqRuGirA73dp2hy1eu1FX90Qc/cTldu319bWXlyZOd0eC0k5iI2LSPCCw+CoBRAWDlGA5PB+1e+8KFFQGKlSdBJasKXNdHw2I0mQmLMUQIzRZnBVA1CC5NLathxVkwXLhue+XWdnu5nz5/9OTBg3sFCAH9+PvHDx4+uXr90huv3+p02sb4GKJzAgz4z//2f/n08PjPPrz/0QcfnZ6Oau8VNCrHpk24iMamAQILKxAqR2uS/vLav/Vv/IZN0od3H8xi3W25zLnoeTAarG6szyazB/fvIwIr97q9X//NXxehg4ODwsfgAyr1un3Xbu2ORsd7+6P9Qz+bTcs6iCpaFRAwhhIQVvWsgQgBybTylc0L126/vrKx3e3eTLotF6IR1moKcaeuJz/83vf3Do9N2rpy+fLSysru7t7x4JQRD/YPitEgdTZ3zfhRbAYDAYABskSgyCoViwe0iQ2RuYxGkIEiECKlLk2IRLSxxgQVDBpjHFlETJMsS/M0zzvtdpa3kizjYnCy9/DZ4/uswTijKj5GVhTBpeX1N1+71ul3puXs9GRQzwrLmd26ef2vbl0d//T733+w+/EHP77/8OlsVlDTZ4RISIkNojBGVRYDrOFkcPLk8eNfeP+94Wp/9HzPmpyMEWXjbJra/WdDRCACVSyLWVHNeq2tJHEhBrRYV/XJ6ZErO1sX19/+xi+vG3NwDKPh9wfDoqgkBrFJZqwrJkUUX5TTxNLKynJvrZvlV5VWZbUtIZfRaXJz2ZguzCZcJCc/+aPj3Wfsq1hOd+qi3Nha3li9evPi/mDCwtJtz4ooYJyxBHMdb4ik6eAjAMKtnPLUMWgqYlMwlIDLkIxRtQBILookaZrlaZKnYCRNbZOnThOnkVU01mF/99Hp3mEshlHKqhoTCjmLaFijQbSgg8Odbx0+Y1WFqKSpa9lpGd3MI7LrrfzS11e//vY7Hz+4//G9+/c+eTAcDL0PSupRABgRWQxIE3yGnZ1nx194p9fv6qMnRVHZbgtIszRJEzebTZru+CwaNBwd7r357nYZekRQVCUaiEGm09H0YVHPStl48wtffdO3/8pHD6bTpzMvolAD9npcGWPTHKxFZYkYcK0ttcFTID9pryxd77cffv9bezs/vPvhjx89eogYnbOAVPjq2WwyHB5fuLy9tLn5c3/pa9HDwaAoZmwYE4PWoucmzEEWbWJTQlAjFVeAkhhDjCzEiFnijCoHPysKY5AIY/Qg3lfFdBxEGSA+PjqZjIa+9lVZI8bMUWIwhFo4gMXUGVESRmOIlBW4DkFE1BJhsrm+aWdVabC2hFRFSq0ivP7WtTevXS3/0tc+eLz38JO79+89GQ7HBBSx2SUKjKAKRyeD6Xi82u+uLLdnRVXNQIBXVnocfOk9ESEpAYrCw0c725eutJOMu10lwKpG8K1Mp8Xo0SdHh3vPY3h8+Qu/Toy2TVwgawLgyQChks0wQ9BogWjmTRWS7d7She3O6PGdP/qnf/x7f/DgwX2OVVOC6L1BYwgJqSir4mBv12atTn9paWWlvdy3Nk2SNEsSY9AyVlWECIiipILAdVlMB7WvQAAjVDVXMWoUE0WjjyCGkJRBfJJSp5Os9DsrqytIMDjcGY6PSSIhphYcEUeJ0QuINal11ns2CI6AGQHJIJAxIeCbt2/+5m/9Cv7X/8V/pjyfLuxSm0Di0FoD6lxqJRT8aPf5B588evDJg8PDk9l0xhyDRAZlSn/pZ77+pXfeOpwMH9x/7MuY5snt1688uHPv8bNn1ljVGDWIMIjptZdbrTYSkG02/5MCBO9jCKNxlbS677//Tn99VexWVI4aGYxCBioIwZIj8AbFOXTdtk5np/u7H/3Z9+5+/PGsnjWtWeabpmkxQBcAlMgQkhFFH1XJCBiNSs0gDwRo3DJRYG3mllACNjEIRIqAxjgLihbRErrEEqFzLstcq5Mv9Vv9bitNUzJm58nunbsPJqNRiAEXLaBYGAnTNEuS1Ieoyo6ImZlVkAwl1y5e/bf+6l/+5fe+hv/l//VvWswJVRQQyTiklBySk8QZAlTMrfFS1/H+0/2ffPzJj39053gwLr1HZy9tX/jVX/25NEt2d/eHJ9PtS9upo2/9yZ96XwMBc4gcWKIK+jrUQVmUNSJht91aX+pZ53wIVVGfjsrpqCSTg02aXblkjLUWCUWinnVaY1EfmENkL6EiA4DCzbCRZkZQ0/Hk3LRSPGs+sxiQ3DRLORtYTfPJCkCIsBgvj80ce2qmIDW5PoOGBBEQm7loqEKEaZKoalkU4+msrstmPswiFWqccUSGledNV1gA0CTphc2tL7/3hYuXtsVY/L/9p38jTRK0plmkaQIfiMaiSYxRk2DqCIVs3jFSmqeHOx/cff7BDz48PDy0Jvnyl9/9xTdvrFzdnMXWnZ0nn/zgh0+e7ZAqg0QJzJEjR5a6CgwYWRoySaxJnTPWKRLHWJUheM/N/mpoZug2DTSajhnNlDudd1ISBRVgBgTBs/Hci9e8J8Ti0NlQu8bHWzQSgMWh8xfPT0GAswHisJgRPQ9MNXUbygrU5MxVAMkgMnOMYTEUvNlxMO/eDHjWHwQMGSVrbZpgBiEsLS3h//lv/I2gsERgM4yA1hCSISQyBgnJNsOtDQWb2cQQxCzNyziZjO8eHZ6OJseHJxLjlZtXo9qnDx49fXw/xqiNGJHIzBw5RA5e0NgQOcRgCVQlREFjrXXKGiIrs8q8VcyCOhdzt2He12deGqSqCiJN+80zWocXsNVz3Rp0Dn1YDJyad2d6gZ7FDJhzR85fcnbm4lTUs5s1k8mbyotFfKKZVzgPH+J8BPt8xqeiCrAigJmHWQTx7/ytf3D88P6Yp0TaQnRI6tAYtMYRGTKEhub3dZSgs+ISwmhcnmm3NIPx9L/9/X/x6MkeJhnGMkQPIF58HQJzFJbIHAKLonVJWdUsTIjCLArWWUTjax9ZdB6dfgGeOSgWk3+b43iOfGExFhzP4LSI4p0D5BwpLz6eNcg6T+TnMdCMez/jkBfX4iJgjmezxs9NjcGz8TENPTTv51TTDG4VUGmqrGlu/gLiw598VBXF6Xj4yZ2PdvYPADRHygnBIRm0ptnHYBCbv0QOLWGGlsABoiv5O48f/P4//ZeTMrYTcA6jxDLUVQwswlF8iFHEoAGgOgZCVNVmMKezliOHEOaN3M6I9Gwy0Rl0zh4JzmCweGz4NATh7NgCjudQ0dDiC5pe/AS+4Jaz44tfbICI+tLh+Zz1Bf/NF0Z4xjfNV/PGgSLz3i5NZzCdj4gnIpunLst7yyvdC6tL+4eHDx8+fP78YMiSKiaI0QQisM4QWmNME6AVJLaSpGyBKkyvtftLm2un93cnXshCzd7HmjWqCouEyCyKIKAYRUzjTIoqQtRFRdFi6NPimRbKEAEXVNZ0wMYzAjwD/zlSbUDzokfWApE6p/z5lYuAa8MJuoCfLpQOnrEgLpRR0xDobGLYGfZ1oSNeUM88xbRA+jy02OS3EQnRGGedMYTG9FaWcPjooZ8rv2hUqqI6GU4fP9+5+5O7E4mI0EW0jtCgs65BOxEZQ8ZhmjhjESXde7z7ye7Bzv2dvb2DwXgUQs2RY2RcVGMjUdMYjBYz/xpIcUMVTWJyztcLuYn4Agc0J7qFyJmPJJ3LnjN5jfNHb2CJc5E972h1Jj3mCFg0bz0vK140UVwIHwRcdLuczxumF+hveo021pM2jbWctdY5lyZpliZpkiQuy5I8T1vtVpplLm2lLm+TaRM6NLiU4NHH98gZBtV5HkJEOJb16WDydO/Z0092DqsxILQQUyJIgIw1ZKwha8k4R2jTlKoAXUq5duVg795sWBWzwelg9+neycFhOZsJx8iqgNjMTlVshmcAYVM7gKS26WQ9nx34QrDOgb0gKjyD9iImfyZgmrPmQmYuEKipj9NmNPCcgmnRyxwaeDfQbPJgTV+5F81F5/OGoQkgGTLWmDRNrbNJ4vI8a3db7U47SVyapmmWmyRftkmb0jzrmMS5zJiMrCExgshCxGJrj4BqCJEgiuIf/eN/cuvaxbTbFkMA2JS5qkQC0MCTabl/dPhg58nTnd2onCG2iDAng9ZZhwbJoDE2AZOiEUhsarIkRiKHlmstp6Pp8eBpVQ2Gg9msiCEU02I6LauqLoqy9j7EqMrczEdSSJxBRGlmJdHCvkAkJEBt+rzR3JZvjEWks0ZtgIaadyiiiGAIEZHnc/2aabIEAKxNz6Dm1dyeXOLSNDPWWueMNc6aLEvSLE2zVprljYPdscmyaWdZ2ybOWTIOTGYoJXIoKswa66BgAalpkQ4giAAgAhpVY1SJTeX0PN3HIeD/4X/3n9g0/fkvffnKzYtLSz0GO2dXg01HMPVxNpsOhtOnBwfP7twfiTeEHUSbkxpLaAyhIXLOGjQJ2dQgoUFVQOtySwlFAARSUamjVlEq5hA91z4UtY+jGE7rYjadDU6H0/E41AERmcV7L8xkCACYWVWx0QSNwAZyzjlnG41mrDHGIKgxRNYaY6lBhoJLXZI4RHTWWGNF1CSJS1MyzhmbIbXIZMa2Wl2iFK3BlGxqTELGghIQACtEVWNTFiIgYCYVFDEgzZzGKBxYQ1QRRZCmgZxgk2aVOTMBqKpw08oUG76SGPFv/LW/HgXB2l47e++99y5eubK61LEugWYbGSowN1kR8TwaTfdPDx7cf3x4ciIAbcTMETVjxpxDQ9Ro9gStMaRk0ZGxAKAqKIYW1rGzBgyaxJIBMewhGpPEAFBJ9AziNDZb1ZiEJMYoEZRRscnSEgAaa5xBMmSo6bYnBM3EcCJAi9S0cUNVZwilMWFV1RjTCDpV1aDKQUQADDAsmKwReISkAsqiLE3466yhoqAKgRBIc4k0w4ibzq0sZ3Js3jGuATnM22A2OxzmXV4j4//pr/1vagZWAWOa2or333rr5muvry23IxpQOGvGJcIiDJFnRXlwfPLk2c7ezl4EyREdoM0QDRpKDKGxhGSbpoZAiAYtEjVqVsE2cwUauW2NsTTPzguqGgDXDFtQRMJmd6kAza0hmSfI0JABRBEWEYPaiK0ztwsNogpqM2scBHhhCM1Nw8gi2jTEbIiraVMsCoCLuc1Ni29pGiwiNk6uNr0tRc28w2Vj40CjLIQXXcIX3V/PVH3jiEHTDRLn7ojEGv/mX/uPWBAQpelhDYbJJu32W9dvXbt568J6z7XyphJGhBfpWUDWuqpOR7MHjx89fvCwEgbCHmJiEQgdATkC4xYmAxkiWngoZuG2CIBawGbidzPpXowo8iK80KjgZvW4cLGa56CmeyU0eSmdG4vN+prip7legEWSuWld3+hjYIVG09gm9PApJ7zprdgMH2msrflw8zPVrbSYc96oIgWQ5vYKMp9Z0oSiFpodmjTfYo8PAKAy1/if/fX/eCYADaEJqCqLChog5/LWG5dvvvn+WxdWO9EkTek9qDT9PUUEEbgKg8Fo7/jw8Z1HR9UICFoACTaxECSDaAEJDRlE00xxNTQ3LhdP/MLDVAABFFngqmlHuIDu3FfABXsDzgv9GjTNAzioTT65qWQ5CwjMO8PqWZTirJU6Lu7QMATOSRcVoCkM0DM7duFTLIz8c1arNpUWc1S/MGmbNwqNT99gaO67Y1Bm/E//+v8apNnRMH+sptu+AqqSkHWt1q2L12+8cfviRt/lmTYIn1tnwKIIipEns/Lg5PjhzuPnz/aayoMWkkXU+VB1REJjkQyicWexkgXs6ZzlDQujERvWXrhGTTBrPphp3hd9ziZzOBAiUDO+WBbMcP6UxQVzEJ/VHSksOA3PzoI5NZyHNyy+fdldeCHc5yB/YZg1CmAO9QV0YTHnMUpga4jmIzJRFZVVSUGbxJ0wSozj+s50cm/n3rW1zTe/8v7l9eVos7mNrmJARYGJ8l7revvS5try6Obtw5PTnY8+HrBXhAQgJRBAI2BigwdBg0gAhERuYYWf+VQvoguIuHiY+ZPJmZO5AAGeo9BFJGNxIzyjQWjahc9D1GeiZH6The8N89udhUbmngaeQ8CLGN9ZoAcAGnkF5xar87rGRjvMmQIWElxAhVAB1MJCnjZah+Z9xl/EYRBEIWgRH+7Wj37n8NLS0s13v3Bl+0Leyua9YlVQRRQFybXaq3m+srZyefvC8enxzsOd3ZMjH8UQZIgMgChGEeLCJXWKZh5SmftmhGcgbATHCz+1IelG9iwkyRlCXshnxObEuYerKhqbWF8jEJvzReNZxBvnJIlzXlRVZVg4uQoKGudQQXOW81FdLHGuG1VBFmF0EXmhtETmUuecQTqf8GFBNepcgJqzUBYAIggAEeiiXFljFX35aDZ5dnK83Wnf/sqXr164mGSOG3k1l3pNwIHyXn61fXFjbeV0OH6+u7v79Hkh0QAYEEUSRNPkP7zMfVREbKqWEWBeGAqLZz6TzHBGZAuRjC9CafKCIPV8RAyg0ZCgoBjOi405QSuAyvlo3/yPKgArzLXtPG6hci7aSYCkzez5M7nTGDlnS1UFVpFz7AWLNTX1qX/zf/u/EiYgJFJaOO46L/39/1V1JbtxHDH0kdUzo5E1mdiwEENOlFjOAsQ55P9POSWHXHJwkAVBFsBALFn7Np7uJl8OZFUrus5BjeLjI/lYRcbaj5xMzdirHF+vM8zm+6vVq2+/OXj+2Wze5XRsAeJiHwnS3BQc+u3dzf27s7M3v/953vcUzkTmtX2nTfWqSo4AWpKV+VBsqGfKhnhOzlOTvQfHy0osCnfCEus1lZtopRoi/5U2AnjAYmne8HqpSZPWz2JTQDLFDKoHEdtKJyLLC39SSMC7OfiePnM1RO6WH+Os8lbUENmmY7YGfbBN/3Zzf/Ld5f769eevvj44OFwuCrTEFJdc5xcP3LXbXe8d7i6ePl5fXt38++bk/PpipIPsiBixjHxSmsbIOBzNpGYBQGu0ZsjrzWU1bdXCYAa+hASmjNEqPDnZcrLChM4m8U1s146v0bQYKrIzjKOdflB9pUvUGOVZGYiQCutGgGTvJojdOc3byNTGMMW1iItSYjULSG43x6fDyfc/7Kxef/Hp4dGLLx/tFocEh2aBA0LEyW5n9rRbr/YWm/v907OL47envY1CVyfBkZU0RGPue0NNE57pE3ClhTUAWTsIH7BQHHRA2TMYQGpsIajM7QIpTMNbu6v9VUAA08aB9Ibat0k38ZoFJItI3rAHct1866uyYQTeORiRR4gYltVKCDZ3E7B5Y2jgypISIklav7k93fx0dfnH3/98dXR0+Pz58tFi8NHN3J1u1ayxIbvMl8tnz8rj9er88ubd8el2HNzHrmID4nQV9WBZiFgFe5GwQvp3KKCx+yQGh1fMtkoUgGvcnyZBuNTVMVHCVlk7riVM8E3DVOE/QibY6uT8RUO+eJAnMYiKWfLlJUYpzELn/5q3dA4DEIuA0mEkrB2ni6IKFWMLLJmjx9SV4MoiCnfbbq6PNz+en/38268vD/Y+Pny5WO6500dzs5pW0c1DYIHOVuv1Yrlze3Jxdns9DFvGQpH4AK9ltKoRY00C4w2HxXFgGiBmDtCjn+CpiAEUj+Q16AysovRUSAQeO9HAXm5by0wqcgpoLQZHUgE6jBCROVUkNc8CAWiUAISQQZgu2BqUMpdQbzMDUkLgHRRUQ6TlAlWhiNOS8CCjohTN1kONbl2nyuKE2UhCRYsUsvPR+2G8v7r4ZXv318ndi0/2P/zgo7loOAFrusJ2zcHZYbb75Inp9uqaw/ieTgUojMupWyBWr85itwI4Wj6oUGJ0D3m6iPZA7ygoO6oCF9DATkoBDDT3AgAsohR0WjpAREaQxGDe0wrRqaog1Mp4BkNwdJ+rFICqGdsVTpibkYWE6kDGKHxCBrJAOriCBu9EtwIzbCHzopEPF6KAgP0HzE+cDfrWoDMAAAAASUVORK5CYII=";
        MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(json, JSON);

        OkHttpClient client = new OkHttpClient();



        // Visits query
//            Request request = new Request.Builder()
//                    //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
//                    .url(host+"/services/data/v55.0/query/?q=SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c  where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC")
//                    .addHeader("Authorization","Bearer "+"00D3h000002nORA!AR8AQEnBwlGSFZWJobLkmtT5KwJfVDIpB9QRjkD7Z3qrOlyrEBVzVRB.fyWI29HKqEIbM0cZ4qXVQeFrvYlRpjFp0fTforrC")
//                    .build();

        // Get Token

        String payload = "grant_type:password\n" +
                "client_id:3MVG9Kip4IKAZQEXXX26xZ160wct9LKeLv_IFZFbazJJbE_y2SpFY8hzffqKF731Kkps5nDBtNDE23I5FXhRc\n" +
                "client_secret:63278E9FCBA8770F0A5154A5005C0C8932BAA78DF89189DFF467C10C6B1A643A\n" +
                "username:stephanfleming@dmsdev.com\n" +
                "password:dms@1234";
//
        MediaType FORM = MediaType.parse("multipart/form-data");
//
        RequestBody formBody = new FormBody.Builder().add("grant_type", "password")
                .add("client_id","3MVG9Kip4IKAZQEXXX26xZ160wct9LKeLv_IFZFbazJJbE_y2SpFY8hzffqKF731Kkps5nDBtNDE23I5FXhRc")
                .add("client_secret","63278E9FCBA8770F0A5154A5005C0C8932BAA78DF89189DFF467C10C6B1A643A")
                .add("username","stephanfleming@dmsdev.com")
                .add("password","dms@1234")
                .addEncoded("MediaType","multipart/form-data")
                .build();

        RequestBody body1 = RequestBody.create(payload, JSON);
        // Service Request
        Request request = new Request.Builder()
                //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
                .url("https://login.salesforce.com/services/oauth2/token")
                .post(formBody)
                .build();


//        Request request = new Request.Builder()
//                //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
//                .url("https://login.salesforce.com/services/oauth2/token")
//                //.addHeader("Content-Type","application/x-www-form-urlencoded")
//                .addHeader("Accept","application/json")
//                .addHeader("Authorization","Bearer "+token)
//                .post(formBody)
//                .build();

            try (Response response = client.newCall(request).execute()) {
                //Log.d("2514", "Request URL: https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h0000048l32EAA"+ "\n" +" Token: "+token +"\n"+ " Response: "+response.body().string());
                String res = response.body().string();
                log(res);

                String newtoken = new JSONObject(res).getString("access_token");
                String newhost = new JSONObject(res).getString("instance_url");

                checkQueryWithToken(newtoken,newhost);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("2514", "checkSalesOrderQuery: "+e.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }


    private void fetchToken(){
        String json = "{\n" +
                " \"serviceRequest\": \n" +
                "        {\n" +
                "\t\t\t\"id\": \"null\",\n" +
                "            \"title\": \"Error Message 2\",\n" +
                "            \"description\": \"Error in the  sales order edit \",\n" +
                "            \"deviceDetails\": \"Samsung s22\",\n" +
                "            \"moduleName\": \"Sales Order\",\n" +
                "            \"status\": \"Draft\",\n" +
                "\t\t\t\"binaryCode\": \"data:image/png;base64,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";
        MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(json, JSON);

        OkHttpClient client = new OkHttpClient();



        // Visits query
//            Request request = new Request.Builder()
//                    //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
//                    .url(host+"/services/data/v55.0/query/?q=SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c  where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC")
//                    .addHeader("Authorization","Bearer "+"00D3h000002nORA!AR8AQEnBwlGSFZWJobLkmtT5KwJfVDIpB9QRjkD7Z3qrOlyrEBVzVRB.fyWI29HKqEIbM0cZ4qXVQeFrvYlRpjFp0fTforrC")
//                    .build();

        // Get Token

        String payload = "grant_type:password\n" +
                "client_id:3MVG9Kip4IKAZQEXXX26xZ160wct9LKeLv_IFZFbazJJbE_y2SpFY8hzffqKF731Kkps5nDBtNDE23I5FXhRc\n" +
                "client_secret:63278E9FCBA8770F0A5154A5005C0C8932BAA78DF89189DFF467C10C6B1A643A\n" +
                "username:stephanfleming@dmsdev.com\n" +
                "password:dms@1234";
//
        MediaType FORM = MediaType.parse("multipart/form-data");
//
        RequestBody formBody = new FormBody.Builder().add("grant_type", "password")
                .add("client_id","3MVG9Kip4IKAZQEXXX26xZ160wct9LKeLv_IFZFbazJJbE_y2SpFY8hzffqKF731Kkps5nDBtNDE23I5FXhRc")
                .add("client_secret","63278E9FCBA8770F0A5154A5005C0C8932BAA78DF89189DFF467C10C6B1A643A")
                .add("username","stephanfleming@dmsdev.com")
                .add("password","dms@1234")
                .addEncoded("MediaType","multipart/form-data")
                .build();

        RequestBody body1 = RequestBody.create(payload, JSON);
        // Service Request
        Request request = new Request.Builder()
                //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
                .url("https://login.salesforce.com/services/oauth2/token")
                .post(formBody)
                .build();


//        Request request = new Request.Builder()
//                //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
//                .url("https://login.salesforce.com/services/oauth2/token")
//                //.addHeader("Content-Type","application/x-www-form-urlencoded")
//                .addHeader("Accept","application/json")
//                .addHeader("Authorization","Bearer "+token)
//                .post(formBody)
//                .build();

        try (Response response = client.newCall(request).execute()) {
            //Log.d("2514", "Request URL: https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h0000048l32EAA"+ "\n" +" Token: "+token +"\n"+ " Response: "+response.body().string());
            String res = response.body().string();
            log(res);

            String newtoken = new JSONObject(res).getString("access_token");
            String newhost = new JSONObject(res).getString("instance_url");

            tinyDB.putString("token", newtoken);
            tinyDB.putString("host", newhost);
            fetchVisitsWithToken(newtoken,newhost);
            //fetchTasksWithToken(newtoken,newhost,null);
            //checkQueryWithToken(newtoken,newhost);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("2514", "checkSalesOrderQuery: "+e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fetchVisitsWithToken(String token, String host) {
        ArrayList<VisitDB> fetchedVisits = new ArrayList<>();
        OkHttpClient client = new OkHttpClient();
        // Visits query
        Request request = new Request.Builder()
                //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
                //.url(host+"/services/data/v55.0/query/?q=select Id,Name,Status__c,DistributorToVisit__c, DistributorToVisit__r.Name, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, VisitStartTime__c, BeatPlanning__c, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, OwnerId, Owner.Name from Visit__c where BeatPlanning__r.FieldSalesRep__c = '"+ownerId+"'")
                .url(host+"/services/data/v55.0/query/?q=SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where BeatPlanning__r.FieldSalesRep__c = '"+ownerId+"' ")
                .addHeader("Authorization","Bearer "+token)
                .build();
        try (Response response = client.newCall(request).execute()) {
            //Log.d("2514", "Request URL: https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h0000048l32EAA"+ "\n" +" Token: "+token +"\n"+ " Response: "+response.body().string());

            String result = response.body().string();
            log(result);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //listAdapter.clear();
                        Log.d("VisitsNew", "Result :" + result.toString());
                        JSONArray records = new JSONObject(result).getJSONArray("records");

//                            for (int i = 0; i < records.length(); i++) {
//                                listAdapter.add(records.getJSONObject(i).getString("Name"));
//                            }

                        fetchedVisits.clear();
                        for (int i = 0; i < records.length(); i++) {
                            if ((records.getJSONObject(i).has("DistributorToVisit__r") && records.getJSONObject(i).getString("DistributorToVisit__r") != "null") || (records.getJSONObject(i).has("RetailerToVisit__r") && records.getJSONObject(i).getString("RetailerToVisit__r") != "null")) {
                                String distName = "null";
                                String retName = "null";
                                String distAddress = "null";
                                String retAddress = "null";
                                if (records.getJSONObject(i).getString("DistributorToVisit__r") != "null"){
                                    distName = records.getJSONObject(i).getJSONObject("DistributorToVisit__r").getString("Name");
                                    distAddress = records.getJSONObject(i).getJSONObject("DistributorToVisit__r").getString("BillingStreet");
                                }

                                if (records.getJSONObject(i).getString("RetailerToVisit__r") != "null"){
                                    retName = records.getJSONObject(i).getJSONObject("RetailerToVisit__r").getString("Name");
                                    retAddress = records.getJSONObject(i).getJSONObject("RetailerToVisit__r").getString("MailingStreet");
                                }

                                String time = records.getJSONObject(i).getString("VisitStartTime__c");
                                String prio = records.getJSONObject(i).getString("Status__c");
                                time = time.replace("T", ", ").substring(0, time.length() - 8);
                                String id = records.getJSONObject(i).getString("Id");
                                String date = time.split(",")[0];

                                VisitDB vTemp = new VisitDB();
                                if (records.getJSONObject(i).getString("Name") != "null" && records.getJSONObject(i).has("Name")) {
                                    vTemp.name = records.getJSONObject(i).getString("Name");
                                }
                                vTemp.visitId = id;

                                visitsIDS += "'"+id+"',";

                                vTemp.distributorName = distName;
                                vTemp.time = time;
                                vTemp.beatPlanning = records.getJSONObject(i).getJSONObject("BeatPlanning__r").getString("Name");
                                vTemp.beatLocation = records.getJSONObject(i).getJSONObject("BeatLocation__r").getString("Name");
                                vTemp.distributorAddress = distAddress;
                                vTemp.retailerName = retName;
                                vTemp.retailerAddress = retAddress;
                                vTemp.isSynced = true;
                                vTemp.ownerID = ownerId;
                                vTemp.status = prio;
                                vTemp.priority = records.getJSONObject(i).getString("Priority__c");

                                fetchedVisits.add(vTemp);
                            }
                        }
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    connectDB();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                addRecordsToDB(fetchedVisits);
                                fetchTasksWithToken(token,host,null);
                            }
                        });
                        tinyDB.putBoolean("Online", true);
                        ((HostActivity)getActivity()).updateStatusBarColor("#1da1f2");
                    } catch (Exception e){
                        Log.e("2514", "Visits By Token: "+e.toString());
//                            AsyncTask.execute(new Runnable() {
//                                @Override
//                                public void run() {
//                                    populateView();
//                                }
//                            });
                    }
                }
            });

            //checkBatchQuery(token,host, visitsIDs);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("VisitsNew", "checkSalesOrderQuery: "+e.toString());
        }
    }

    private void fetchTasksWithToken(String token, String host, String[] ids) {
        ArrayList<TaskDB> fetchedTasks = new ArrayList<>();
        OkHttpClient client = new OkHttpClient();
        // Visits query

        visitsIDS = visitsIDS.substring(0, visitsIDS.length()-1);
        visitsIDS += ")";

        Log.d("2518",visitsIDS);
        Request request = new Request.Builder()
                //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
                //.url(host+"/services/data/v55.0/query/?q=SELECT Id, WhatId, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where OwnerID ='"+ownerId+"'")
                .url(host+"/services/data/v55.0/query/?q=SELECT Id, WhatId, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task WHERE WhatId IN "+ visitsIDS +"")
                .addHeader("Authorization","Bearer "+token)
                .build();
        try (Response response = client.newCall(request).execute()) {
            Log.d("2514", "Checking for query: "+ host+"/services/data/v55.0/query/?q=SELECT Id, WhatId, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task WHERE WhatId IN "+ visitsIDS +"");

            String result = response.body().string();
            log(result);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //listAdapter.clear();
                        Log.d("2512", "Result :" + result.toString());
                        JSONArray records = new JSONObject(result).getJSONArray("records");
//                            for (int i = 0; i < records.length(); i++) {
//                                listAdapter.add(records.getJSONObject(i).getString("Name"));
//                            }


                        fetchedTasks.clear();
                        for (int i = 0; i < records.length(); i++) {

                            TaskDB vTemp = new TaskDB();
                            vTemp.taskId = records.getJSONObject(i).getString("Id");
                            vTemp.visitId = records.getJSONObject(i).getString("WhatId");
                            vTemp.date =  records.getJSONObject(i).getString("ActivityDate");
                            vTemp.subject = records.getJSONObject(i).getString("Subject");
                            vTemp.status = records.getJSONObject(i).getString("Status");
                            vTemp.priority = records.getJSONObject(i).getString("Priority");
                            vTemp.description = records.getJSONObject(i).getString("Description");;
                            vTemp.isSynced = true;


                            fetchedTasks.add(vTemp);
                        }
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                addTasksToDB(fetchedTasks);
                            }
                        });
                    } catch (Exception e){

                        Log.e("2514", "Tasks By Token: "+e.toString());
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                populateView();
                            }
                        });

                    }
                }
            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void checkQueryWithToken(String token, String host) {
        OkHttpClient client = new OkHttpClient();
        // Visits query
            Request request = new Request.Builder()
                    //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
                    .url(host+"/services/data/v55.0/query/?q=select Id,Name,Status__c,DistributorToVisit__c, DistributorToVisit__r.Name, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, VisitStartTime__c, BeatPlanning__c, BeatPlanning__r.BeatTasks__c, OwnerId from Visit__c where BeatPlanning__r.FieldSalesRep__c = '"+ownerId+"'")
                    .addHeader("Authorization","Bearer "+token)
                    .build();
        try (Response response = client.newCall(request).execute()) {
            //Log.d("2514", "Request URL: https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h0000048l32EAA"+ "\n" +" Token: "+token +"\n"+ " Response: "+response.body().string());

            String result = response.body().string();
            log(result);

            JSONArray records = new JSONObject(result).getJSONArray("records");

            String[] visitsIDs = new String[records.length()];

            for (int i = 0; i < records.length(); i++) {
                String id = records.getJSONObject(i).getString("Id");
               visitsIDs[i] = "'"+id+"'";
               // visitsIDs[i] = id;
                log("Adding visit ID: "+visitsIDs[i]);
            }

            checkBatchQuery(token,host, visitsIDs);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("VisitsNew", "checkSalesOrderQuery: "+e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkBatchQuery(String token, String host, String[] ids){
        OkHttpClient client = new OkHttpClient();
        // Visits query
        Request request = new Request.Builder()
                //.url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
                .url(host+"/services/data/v55.0/query/?q=SELECT Id, WhatId, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task where OwnerID ='"+ownerId+"'")
                .addHeader("Authorization","Bearer "+token)
                .build();
        try (Response response = client.newCall(request).execute()) {
            Log.d("2514", "Checking for query: "+ host+"/services/data/v55.0/query/?q=SELECT Id, WhatId, Subject, ActivityDate, Status, Priority, Description, CompletedDateTime FROM Task WHERE WhatId = "+ Arrays.toString(ids) +"");
            log(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("TasksNew", "checkSalesOrderQuery: "+e.toString());
        }
    }

    public void log(String message) {
        // Split by line, then ensure each line can fit into Log's maximum length.
        for (int i = 0, length = message.length(); i < length; i++) {
            int newline = message.indexOf('\n', i);
            newline = newline != -1 ? newline : length;
            do {
                int end = Math.min(newline, i + 1000);
                Log.d("2514 Without login", message.substring(i, end));
                i = end;
            } while (i < newline);
        }
    }

    void calculateMissed() throws ParseException {
        Log.d("2512 Date", "Calculating"+ totalVisits.size()+ " Dates");
        missedVisits.clear();
        for(Visit v : totalVisits){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date strDate = sdf.parse(v.getDate());
            //Log.d("2512 Date", "Visit Date: "+ strDate.toString()+" is: " +(new Date().after(strDate) ? "After" : "Before")+ new Date().toString());
            if(new Date().after(strDate) && !v.getPriority().equals("Completed")){
                missedVisits.add(v);
            }
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMissedNumber.setText(missedVisits.size()+"");
            }
        });
    }

//    private void sendTotalRequest(String soql) throws UnsupportedEncodingException {
//        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);
//
//        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
//            @Override
//            public void onSuccess(RestRequest request, final RestResponse result) {
//                result.consumeQuietly(); // consume before going back to main thread
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            //listAdapter.clear();
//                            Log.d("2512", "Result :" + result.toString());
//                            JSONArray records = result.asJSONObject().getJSONArray("records");
////                            for (int i = 0; i < records.length(); i++) {
////                                listAdapter.add(records.getJSONObject(i).getString("Name"));
////                            }
//
//
//                            totalVisits.clear();
//                            for (int i = 0; i < records.length(); i++) {
//                                if ((records.getJSONObject(i).has("DistributorToVisit__r") && records.getJSONObject(i).getString("DistributorToVisit__r") != "null") || (records.getJSONObject(i).has("RetailerToVisit__r") && records.getJSONObject(i).getString("RetailerToVisit__r") != "null")) {
//                                    String name = "";
//                                    String address = "";
//
//                                    if (records.getJSONObject(i).getString("DistributorToVisit__r") != "null"){
//                                        name = records.getJSONObject(i).getJSONObject("DistributorToVisit__r").getString("Name");
//                                        //address = records.getJSONObject(i).getJSONObject("")
//                                    }
//
//                                    if (records.getJSONObject(i).getString("RetailerToVisit__r") != "null"){
//                                        name = records.getJSONObject(i).getJSONObject("RetailerToVisit__r").getString("Name");
//                                    }
//                                    String time = records.getJSONObject(i).getString("VisitStartTime__c");
//                                    String prio = records.getJSONObject(i).getString("Status__c");
//                                    time = time.replace("T", ", ").substring(0, time.length() - 8);
//                                    String id = records.getJSONObject(i).getString("Id");
//                                    String date = time.split(",")[0];
//
//                                    Visit vTemp = new Visit(name, time, date, id, prio,"Visit");
//
//                                    totalVisits.add(vTemp);
//                                }
//
//                            }
//                            tinyDB.putBoolean("SFIN", true);
//                            isLoaded = true;
//
//                            VisitDashboardAdapter adapter = new VisitDashboardAdapter(totalVisits,getContext());
//                            rvVisits.setHasFixedSize(true);
//                            rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));
//                            rvVisits.setAdapter(adapter);
//                            bVisits.setText("View All "+(totalVisits.size())+" Visits");
//
//                            for (Visit a: totalVisits) {
//                                cachedVisitsTotal.add((Object)a);
//                            }
//
//                            tinyDB.putListObject("VisitsList",cachedVisitsTotal);
//
//                            getView().findViewById(R.id.constraintLayout).setVisibility(View.VISIBLE);
//                            getView().findViewById(R.id.llDashbaord).setVisibility(View.VISIBLE);
//
//                            getView().findViewById(R.id.progressBar3).setVisibility(View.GONE);
//
//                            calculateMissed();
//
//                        } catch (Exception e){
//                            onError(e);
//                            Log.e("2512", "Login Error: "+e.toString());
//                        }
//                    }
//                });
//            }
//
//            @Override
//            public void onError(final Exception exception) {
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
////                        Toast.makeText(getContext(),
////                                "Unable to connect to Salesforce Server!",
////                                Toast.LENGTH_LONG).show();
//                        loadCachedVisits("Total");
//                    }
//                });
//            }
//        });
//    }

//    private void sendHighRequest(String soql) throws UnsupportedEncodingException {
//        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);
//
//        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
//            @Override
//            public void onSuccess(RestRequest request, final RestResponse result) {
//                result.consumeQuietly(); // consume before going back to main thread
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            //listAdapter.clear();
//                            Log.d("2512", "Result :" + result.toString());
//                            JSONArray records = result.asJSONObject().getJSONArray("records");
////                            for (int i = 0; i < records.length(); i++) {
////                                listAdapter.add(records.getJSONObject(i).getString("Name"));
////                            }
//
//                            highPriorityVisits.clear();
//                            for (int i = 0; i < records.length(); i++) {
//                                if (records.getJSONObject(i).has("DistributorToVisit__r") && records.getJSONObject(i).getString("DistributorToVisit__r") != "null") {
//                                    String name = (records.getJSONObject(i).getJSONObject("DistributorToVisit__r").getString("Name"));
//                                    String time = records.getJSONObject(i).getString("VisitStartTime__c");
//                                    String prio = records.getJSONObject(i).getString("Status__c");
//                                    time = time.replace("T", ", ").substring(0, time.length() - 8);
//                                    String id = records.getJSONObject(i).getString("Id");
//                                    String date = time.split(",")[0];
//
//                                    Visit vTemp = new Visit(name, time,date, id, prio,"Visit");
//
//                                    highPriorityVisits.add(vTemp);
//                                }
//
//                            }
//
//
//                            for (Visit a: highPriorityVisits) {
//                                cachedVisitsHigh.add((Object)a);
//                            }
//
//                            tinyDB.putListObject("VisitsListHigh",cachedVisitsHigh);
//                            tvHighPrioNumber.setText(records.length()+"");
//                        } catch (Exception e) {
//                            onError(e);
//                            Log.e("2512", "Login Error: "+e.toString());
//                        }
//                    }
//                });
//            }
//
//            @Override
//            public void onError(final Exception exception) {
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        loadCachedVisits("High");
////                        Toast.makeText(getContext(),
////                                getActivity().getString(R.string.sf__generic_error, exception.toString()),
////                                Toast.LENGTH_LONG).show();
//                    }
//                });
//            }
//        });
//    }

//    private void loadCachedVisits(String filter){
//        if (filter.equals("Total")) {
//            if (tinyDB.getListObject("VisitsList", Visit.class) != null) {
//                ArrayList<Object> visitObjects = tinyDB.getListObject("VisitsList", Visit.class);
//                ArrayList<Visit> visits = new ArrayList<>();
//
//                cachedVisitsTotal.clear();
//                for (Object objs : visitObjects) {
//                    visits.add((Visit) objs);
//                    cachedVisitsTotal.add((Visit)objs);
//                }
//
//                VisitDashboardAdapter adapter = new VisitDashboardAdapter(visits, getContext());
//                rvVisits.setHasFixedSize(true);
//                rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));
//                rvVisits.setAdapter(adapter);
//                bVisits.setText("View All " + (cachedVisitsTotal.size()) + " Visits");
//            }
//        }
//
//        if (filter.equals("High")) {
//            if (tinyDB.getListObject("VisitsListHigh", Visit.class) != null) {
//                ArrayList<Object> visitObjects = tinyDB.getListObject("VisitsListHigh", Visit.class);
//                cachedVisitsHigh.clear();
//                for (Object objs : visitObjects) {
//                    cachedVisitsHigh.add((Visit) objs);
//                }
//                tvHighPrioNumber.setText(cachedVisitsHigh.size()+"");
////                VisitDashboardAdapter adapter = new VisitDashboardAdapter(visits, getContext());
////                rvVisits.setHasFixedSize(true);
////                rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));
////                rvVisits.setAdapter(adapter);
////                bVisits.setText("View All " + (visits.size()) + " Visits");
//            }
//        }
//    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        final TextView textView = binding.textHome;
//        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        tvHighPrioNumber = root.findViewById(R.id.tvHighPriorityNumDashboard);
        tvHighPrioNumber.setText("No Visits");

        tvMissedNumber = root.findViewById(R.id.tvMissedNumDashboard);
        tvMissedNumber.setText("No Visits");

        bVisits = root.findViewById(R.id.bVisitsDashboard);
        rvVisits = root.findViewById(R.id.rvVisitDashboard);

        bVisits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString("filter", "None");
                if(isMissedSelected){
                    bundle.putString("filter", "Missed");
                }
                if(isHighSelected) {
                    bundle.putString("filter", "High");
                }
                NavHostFragment.findNavController(DashbaordFragment.this).navigate(R.id.action_a_to_b, bundle);
            }
        });

        root.findViewById(R.id.cvPriority).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isHighSelected){
                    isHighSelected = false;
                    ((CardView) root.findViewById(R.id.cvPriority)).setCardBackgroundColor(getResources().getColor(android.R.color.white, null));
                    //((ImageView) root.findViewById(R.id.ivHighDash)).setColorFilter(getResources().getColor(R.color.disbaled_color, null));
                    ((TextView) root.findViewById(R.id.tvHighDash)).setTextColor(getResources().getColor(R.color.disbaled_color, null));
                    ((TextView) root.findViewById(R.id.tvHighPriorityNumDashboard)).setTextColor(getResources().getColor(R.color.disbaled_color, null));

                    if(totalVisits.size() > 0) {
                        VisitDashboardAdapter adapter = new VisitDashboardAdapter(totalVisits, getContext());

                        rvVisits.setAdapter(adapter);
                        //rvVisits.notify();
                        bVisits.setText("View All " + (totalVisits.size()) + " Visits");
                        if(totalVisits.size() > 3){
                            bVisits.setVisibility(View.VISIBLE);
                        }
                    } else if (cachedVisitsTotal.size() > 0){
                        ArrayList<Visit> visits = new ArrayList<Visit>();

                        for (Object objs : cachedVisitsTotal) {
                            visits.add((Visit) objs);
                        }
                        VisitDashboardAdapter adapter = new VisitDashboardAdapter(visits, getContext());

                        rvVisits.setAdapter(adapter);
                        //rvVisits.notify();
                        bVisits.setText("View All " + (visits.size()) + " Visits");
                        if(totalVisits.size()>3){
                            bVisits.setVisibility(View.VISIBLE);

                        }
                    }

                }else {
                    isHighSelected = true;
                    //bVisits.setText("View All "+highPriorityVisits.size()+" Visits");
                    ((CardView) root.findViewById(R.id.cvPriority)).setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
                    //((ImageView) root.findViewById(R.id.ivHighDash)).setColorFilter(getResources().getColor(android.R.color.white, null));
                    ((TextView) root.findViewById(R.id.tvHighDash)).setTextColor(getResources().getColor(android.R.color.white, null));
                    ((TextView) root.findViewById(R.id.tvHighPriorityNumDashboard)).setTextColor(getResources().getColor(android.R.color.white, null));

                    if(isMissedSelected){
                        isMissedSelected = false;
                        ((CardView) root.findViewById(R.id.cvMissed)).setCardBackgroundColor(getResources().getColor(android.R.color.white, null));
                        //((ImageView) root.findViewById(R.id.ivMissedDash)).setColorFilter(getResources().getColor(R.color.disbaled_color, null));
                        ((TextView) root.findViewById(R.id.tvMissedDash)).setTextColor(getResources().getColor(R.color.disbaled_color, null));
                        ((TextView) root.findViewById(R.id.tvMissedNumDashboard)).setTextColor(getResources().getColor(R.color.disbaled_color, null));

                    }
                    if(highPriorityVisits.size() > 0) {
                        VisitDashboardAdapter adapter = new VisitDashboardAdapter(highPriorityVisits, getContext());
                        rvVisits.setAdapter(adapter);
                        //rvVisits.notify();
                        bVisits.setText("View All " + (highPriorityVisits.size()) + " Visits");
                        if (highPriorityVisits.size() < 4){
                            bVisits.setVisibility(View.INVISIBLE);
                        }
                    } else if (cachedVisitsHigh.size() > 0){
                        ArrayList<Visit> visits = new ArrayList<Visit>();

                        for (Object objs : cachedVisitsHigh) {
                            visits.add((Visit) objs);
                        }
                        VisitDashboardAdapter adapter = new VisitDashboardAdapter(visits, getContext());
                        rvVisits.setAdapter(adapter);

                        bVisits.setText("View All " + (visits.size()) + " Visits");
                    } else {
                        ArrayList<Visit> visits = new ArrayList<Visit>();

                        VisitDashboardAdapter adapter = new VisitDashboardAdapter(visits, getContext());
                        rvVisits.setAdapter(adapter);
                        bVisits.setVisibility(View.INVISIBLE);
                    }

                }
            }
        });

        root.findViewById(R.id.cvMissed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isMissedSelected) {
                    isMissedSelected = false;
                    ((CardView) root.findViewById(R.id.cvMissed)).setCardBackgroundColor(getResources().getColor(android.R.color.white, null));
                    //((ImageView) root.findViewById(R.id.ivMissedDash)).setColorFilter(getResources().getColor(R.color.disbaled_color, null));
                    ((TextView) root.findViewById(R.id.tvMissedDash)).setTextColor(getResources().getColor(R.color.disbaled_color, null));
                    ((TextView) root.findViewById(R.id.tvMissedNumDashboard)).setTextColor(getResources().getColor(R.color.disbaled_color, null));

                    VisitDashboardAdapter adapter = new VisitDashboardAdapter(totalVisits,getContext());
                    rvVisits.setAdapter(adapter);
                    bVisits.setVisibility(View.VISIBLE);
                    bVisits.setText("View All "+(totalVisits.size())+" Visits");
                } else {
                    isMissedSelected = true;
                    bVisits.setText("View All "+(missedVisits.size())+" Visits");
                    ((CardView) root.findViewById(R.id.cvMissed)).setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
                    //((ImageView) root.findViewById(R.id.ivMissedDash)).setColorFilter(getResources().getColor(android.R.color.white, null));
                    ((TextView) root.findViewById(R.id.tvMissedDash)).setTextColor(getResources().getColor(android.R.color.white, null));
                    ((TextView) root.findViewById(R.id.tvMissedNumDashboard)).setTextColor(getResources().getColor(android.R.color.white, null));

                    if(isHighSelected){
                        isHighSelected = false;
                        ((CardView) root.findViewById(R.id.cvPriority)).setCardBackgroundColor(getResources().getColor(android.R.color.white, null));
                        //((ImageView) root.findViewById(R.id.ivHighDash)).setColorFilter(getResources().getColor(R.color.disbaled_color, null));
                        ((TextView) root.findViewById(R.id.tvHighDash)).setTextColor(getResources().getColor(R.color.disbaled_color, null));
                        ((TextView) root.findViewById(R.id.tvHighPriorityNumDashboard)).setTextColor(getResources().getColor(R.color.disbaled_color, null));
                    }

                    if (missedVisits.size() < 4){
                        bVisits.setVisibility(View.INVISIBLE);
                    }else {
                        bVisits.setVisibility(View.VISIBLE);
                    }

                    VisitDashboardAdapter adapter = new VisitDashboardAdapter(missedVisits,getContext());
                    rvVisits.setAdapter(adapter);
                }
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    void checkquery(String query, String tag) throws UnsupportedEncodingException {
        String soql = query;
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            Log.d(tag, "Result :" + result.toString());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(getContext(),
//                                "Unable to connect to Salesforce Server!",
//                                Toast.LENGTH_LONG).show();
                        tinyDB.putBoolean("Online", false);

                        ((HostActivity)getActivity()).updateStatusBarColor("#FF675B");
                        //loadCachedVisits("Total");
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                populateView();
                            }
                        });
                    }
                });
            }
        });
    }
}