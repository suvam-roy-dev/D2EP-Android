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
import com.pwc.d2ep.BuildConfig;
import com.pwc.d2ep.HostActivity;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
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
            fetchVisits();
            fetchTasks();
        }else {
            syncTasks();
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

        fetchVisits();
        fetchTasks();

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
                                Log.e("2512", "Login Error: "+e.toString());
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

        if(visitDao.loadAllVisits().length ==0) {
            for(VisitDB visitDB : visits){
                visitDao.insertVisits(visitDB);
            }
            //visitDao.insertVisits(visitDB);
        }else {
            for(VisitDB visitDB : visits) {
                visitDao.updateVisits(visitDB);
            }
        }
        //Log.d("DBROOM", "Added Records: "+visits.length);
        //populateView();
    }

    private void addTasksToDB(ArrayList<TaskDB> taskDBS){
        Log.d("DBROOM", "Adding Tasks: "+taskDBS.size());

        if(taskDao.loadAllTasks().length ==0) {
            for(TaskDB taskDB : taskDBS){
                taskDao.insertTask(taskDB);
            }
            //visitDao.insertVisits(visitDB);
        }else {
            for(TaskDB taskDB : taskDBS) {
                taskDao.updateTasks(taskDB);
            }
        }
        //Log.d("DBROOM", "Added Records: "+visits.length);
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
            totalVisits.add(visit);
            if(v.priority.equals("High")){
                highVisits += 1;
                highPriorityVisits.add(visit);
            }
            if(!v.status.equals("Completed")){
                missedVisit += 1;
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
                tvMissedNumber.setText(finalMissedVisits +"");
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
        //loginOptions.setLoginUrl("http://google.com");
// Get a rest client
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
                        try {
                            ownerId = client1.getJSONCredentials().getString("userId");
                            fetchDealerID();
                            Log.d("2511", "Profile ID: "+client.getJSONCredentials().toString());
                            JSONObject cred = client1.getJSONCredentials();
                            token = cred.getString("accessToken");


                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String finalToken = token;
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                checkSalesOrderQuery(finalToken);
//                                checkSalesOrderQuery(client.getAuthToken());
//                                checkSalesOrderQuery(client.getRefreshToken());
                            }
                        });
                        Log.d("2512", "Token: "+client.getRefreshToken());

                        ((TextView)((NavigationView)getView().getRootView().findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.tvUserName)).setText(client.getClientInfo().displayName);
                        ((TextView)((NavigationView)getView().getRootView().findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.tvUserEMail)).setText(client.getClientInfo().email);
                        Picasso.get().load(client.getClientInfo().thumbnailUrl).placeholder(R.drawable.user).into(((ImageView)((NavigationView)getView().getRootView().findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.ivUserDP)));
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    connectDB();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
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
        String soql = "SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC";
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

    private void checkSalesOrderQuery(String token) {
        OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000494csEAA")
                    .addHeader("Authorization","Bearer "+token)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                //Log.d("2514", "Request URL: https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h0000048l32EAA"+ "\n" +" Token: "+token +"\n"+ " Response: "+response.body().string());
                log(response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("2514", "checkSalesOrderQuery: "+e.toString());
            }
    }

    public void log(String message) {
        // Split by line, then ensure each line can fit into Log's maximum length.
        for (int i = 0, length = message.length(); i < length; i++) {
            int newline = message.indexOf('\n', i);
            newline = newline != -1 ? newline : length;
            do {
                int end = Math.min(newline, i + 1000);
                Log.d("2514", message.substring(i, end));
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