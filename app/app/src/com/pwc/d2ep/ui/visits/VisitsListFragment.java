package com.pwc.d2ep.ui.visits;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.pwc.d2ep.HostActivity;
import com.pwc.d2ep.R;
import com.pwc.d2ep.TinyDB;
import com.pwc.d2ep.Visit;
import com.pwc.d2ep.VisitGalleryAdapter;
import com.pwc.d2ep.databinding.FragmentGalleryBinding;
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

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class VisitsListFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private RestClient client1;
    private RecyclerView rvVisits;
    private ArrayList<Visit> totalVisits;

    private ArrayList<Visit> missedVisits;
    private ArrayList<Visit> highVisits;
    private String filter = "";

    private VisitDao visitDao;
    private TaskDao taskDao;
    TabLayout tabLayout;
    TinyDB tinyDB;
    final int[] selectedSpin = {0};
    Spinner spin;
    int selectedIndex = 1;
    VisitGalleryAdapter adapter;
    private boolean canChange = true;
    ArrayAdapter aa;

    Dialog dialog;
    String[] statuses = {"All" ,"Not Started", "In Progress", "Completed"};


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        AppDatabase db = Room.databaseBuilder(getContext(),
                AppDatabase.class, "d2ep_db").build();

        visitDao = db.visitDao();
        taskDao = db.taskDao();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tinyDB = new TinyDB(getContext());
        missedVisits = new ArrayList<>();
        totalVisits = new ArrayList<>();
        highVisits = new ArrayList<>();

        aa = new ArrayAdapter(getContext(),android.R.layout.simple_spinner_item,statuses);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner



        if (getArguments()!=null) {
            filter = getArguments().getString("filter");
        }

        switch (filter){
            case "Missed":
                tabLayout.selectTab(tabLayout.getTabAt(3));
            case "High":
                tabLayout.selectTab(tabLayout.getTabAt(2));
        }

                AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                populateView();
            }
        });



//        String accountType =
//                SalesforceSDKManager.getInstance().getAccountType();
//        ClientManager.LoginOptions loginOptions =
//                SalesforceSDKManager.getInstance().getLoginOptions();
//// Get a rest client
//        new ClientManager(getActivity(), accountType, loginOptions,
//                false).
//                getRestClient(getActivity(), new ClientManager.RestClientCallback() {
//                            @Override
//                            public void
//                            authenticatedRestClient(RestClient client) {
//                                if (client == null) {
//                                    SalesforceSDKManager.getInstance().
//                                            logout(getActivity());
//                                    return;
//                                }
//                                // Cache the returned client
//                                client1 = client;
//                                if (getArguments()!=null) {
//                                    filter = getArguments().getString("filter");
//                                }
//                                try {
//                                    String ownerId = client.getJSONCredentials().getString("userId");
//                                    if (filter == "High"){
//                                        sendRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where Priority__c = 'High' and OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
//                                    }else {
//                                        sendRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where OwnerId='" + ownerId + "' ORDER BY VisitStartTime__c DESC");
//                                    }
//                                } catch (UnsupportedEncodingException | JSONException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                );
    }

    void showDialog(){

        final int[] selectedTab = {1};



        RadioGroup rgSort = dialog.findViewById(R.id.rgSort);
        RadioGroup rgFilter = dialog.findViewById(R.id.rgFilter);

        spin = (Spinner) dialog.findViewById(R.id.spinner);



        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedSpin[0] = i;
                Log.d("2512", "onItemSelected: "+i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spin.setAdapter(aa);

        spin.setSelection(selectedSpin[0]);
        //Creating the ArrayAdapter instance having the country list


        rgFilter.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (radioGroup.getCheckedRadioButtonId()){
                    case R.id.radioButton4:
                        dialog.findViewById(R.id.etFilterName).setVisibility(View.VISIBLE);
                        dialog.findViewById(R.id.spinner).setVisibility(View.GONE);
                        dialog.findViewById(R.id.etFilterId).setVisibility(View.GONE);
                        break;

                    case R.id.radioButton5:
                        dialog.findViewById(R.id.etFilterName).setVisibility(View.GONE);
                        dialog.findViewById(R.id.spinner).setVisibility(View.GONE);
                        dialog.findViewById(R.id.etFilterId).setVisibility(View.VISIBLE);
                        break;

                    case R.id.radioButton6:
                        dialog.findViewById(R.id.etFilterId).setVisibility(View.GONE);
                        dialog.findViewById(R.id.etFilterName).setVisibility(View.GONE);
                        dialog.findViewById(R.id.spinner).setVisibility(View.VISIBLE);
                        break;
                }
            }
        });

        dialog.findViewById(R.id.imageButton2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        TabLayout tabLayout1 = ((TabLayout)dialog.findViewById(R.id.tabLayout));
        tabLayout1.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                switch (tab.getText().toString()){
                    case "Sort":
                        selectedTab[0] =1 ;
                        rgSort.setVisibility(View.VISIBLE);
                        rgFilter.setVisibility(View.INVISIBLE);
                        break;
                    case "Filter":
                        selectedTab[0] =2;
                        rgSort.setVisibility(View.INVISIBLE);
                        rgFilter.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        selectedTab[0] = tabLayout1.getSelectedTabPosition()+1;
        dialog.findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rgFilter.clearCheck();
                rgSort.clearCheck();
                dialog.findViewById(R.id.etFilterName).setVisibility(View.GONE);
                dialog.findViewById(R.id.etFilterId).setVisibility(View.GONE);

                ((EditText)dialog.findViewById(R.id.etFilterName)).setText("");
                ((EditText)dialog.findViewById(R.id.etFilterId)).setText("");
                selectedSpin[0] = 0;
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        populateView();
                    }
                });
                dialog.dismiss();

            }
        });

        ArrayList<Visit> selectedList = selectedIndex == 1 ? totalVisits : selectedIndex == 2? highVisits : missedVisits;
        dialog.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (selectedTab[0] == 1) {
                    switch (rgSort.getCheckedRadioButtonId()) {
                        case R.id.radioButton:
                            selectedList.sort(new Comparator<Visit>() {
                                @Override
                                public int compare(Visit visit, Visit t1) {
                                    return (visit.getName().compareTo(t1.getName()));
                                }
                            });
                            Log.d("2512", "Should Sort: ");
                            VisitGalleryAdapter adapter1 = new VisitGalleryAdapter(selectedList, getContext());
                            adapter1.setHasStableIds(true);
                            rvVisits.setAdapter(adapter1);
                            break;
                        case R.id.radioButton2:
                            selectedList.sort(new Comparator<Visit>() {
                                @Override
                                public int compare(Visit visit, Visit t1) {
                                    return (t1.getDate().compareTo(visit.getDate()));
                                }
                            });
                            VisitGalleryAdapter adapter = new VisitGalleryAdapter(selectedList, getContext());
                            adapter.setHasStableIds(true);
                            rvVisits.setAdapter(adapter);
                            break;
                        case R.id.radioButton3:
                            selectedList.sort(new Comparator<Visit>() {
                                @Override
                                public int compare(Visit visit, Visit t1) {
                                    return (visit.getStatus().compareTo(t1.getStatus()));
                                }
                            });
                            VisitGalleryAdapter adapter3 = new VisitGalleryAdapter(selectedList, getContext());
                            adapter3.setHasStableIds(true);
                            rvVisits.setAdapter(adapter3);
                            break;
                    }
                    dialog.dismiss();
                } else {
                    switch (rgFilter.getCheckedRadioButtonId()) {
                        case R.id.radioButton4:
                            ArrayList<Visit> filterVisits = new ArrayList<>();
                            Log.d("2512", "onClick: Should filter list");
                            for (Visit v: selectedList) {
                                if(v.getName().toLowerCase().contains(((EditText)dialog.findViewById(R.id.etFilterName)).getText().toString().toLowerCase())){
                                    filterVisits.add(v);
                                }
                            }
                            VisitGalleryAdapter adapter = new VisitGalleryAdapter(filterVisits, getContext());
                            adapter.setHasStableIds(true);
                            rvVisits.setAdapter(adapter);
                            break;
                        case R.id.radioButton5:
                            ArrayList<Visit> filterVisitsID = new ArrayList<>();

                            for (Visit v: selectedList) {
                                Log.d("2512", "onClick: Comparing "+v.getDate()+" to "+ ((EditText)dialog.findViewById(R.id.etFilterId)).getText().toString());
                                if(v.getDate().toLowerCase().contains(((EditText)dialog.findViewById(R.id.etFilterId)).getText().toString().toLowerCase())){
                                    filterVisitsID.add(v);
                                }
                            }
                            VisitGalleryAdapter adapterID = new VisitGalleryAdapter(filterVisitsID, getContext());
                            adapterID.setHasStableIds(true);
                            rvVisits.setAdapter(adapterID);
                            break;

                        case R.id.radioButton6:
                            String st = "New";

                            switch (spin.getSelectedItem().toString()){
                                case "All":
                                    st = "All";
                                    break;
                                case "Not Started":
                                    st = "New";
                                    break;
                                case "In Progress":
                                    st = "InProgress";
                                    break;
                                case "Completed":
                                    st = "Completed";
                                    break;
                            }

                            ArrayList<Visit> filterVisitsSt = new ArrayList<>();
                            Log.d("2512", "onClick: Should filter list by" + st);
                            for (Visit v: selectedList) {
                                if (st.equals("All")){
                                    filterVisitsSt.add(v);
                                }
                                if(v.getPriority().equals(st)){
                                    filterVisitsSt.add(v);
                                }
                            }
                            Log.d("2512", "onClick: should show visits: "+filterVisitsSt.size());
                            VisitGalleryAdapter adapterSt = new VisitGalleryAdapter(filterVisitsSt, getContext());
                            adapterSt.setHasStableIds(true);
                            rvVisits.setAdapter(adapterSt);
                            break;
                    }
                    dialog.dismiss();
                }
            }
        });
                dialog.show();
    }


    private void populateView() {
        totalVisits.clear();
        highVisits.clear();
        VisitDB[] visits = visitDao.loadAllVisits();
        //Log.d("DBROOM", "populateView: "+visits.length);
        //int highVisits = 0, missedVisits = 0;
        int notStarted = 0;
        int inProgress = 0;
        int completed = 0;
        for (VisitDB v : visits) {
            Visit visit;

            if(!Objects.equals(v.distributorName, "null")) {
                visit = new Visit(v.distributorName, v.distributorAddress, v.time, v.name, v.visitId, v.status, v.priority, "Visit",taskDao.loadVisitTasks(v.visitId).length);
            } else {
                visit = new Visit(v.retailerName, v.retailerAddress, v.time, v.name, v.visitId, v.status,v.priority, "Visit",taskDao.loadVisitTasks(v.visitId).length);
            }
            switch (v.status){
                case "New":
                    notStarted += 1;
                    break;
                case "InProgress":
                    inProgress += 1;
                    break;
                case "Completed":
                    completed += 1;
                    break;
            }

            int finalNotStarted = notStarted;
            int finalInProgress = inProgress;
            int finalCompleted = completed;
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView)requireView().findViewById(R.id.textView3)).setText("Not Started("+ finalNotStarted +")");
                    ((TextView)requireView().findViewById(R.id.textView10)).setText("In Progress("+ finalInProgress +")");

                    ((TextView)requireView().findViewById(R.id.textView11)).setText("Completed("+ finalCompleted +")");

                }
            });
            if(v.priority.equals("High")){
                highVisits.add(visit);
            }
            totalVisits.add(visit);
            //Log.d("DBROOM", "New Visit: "+visit.getName() +" Synced: "+v.isSynced);
        }
//        Log.d("DBROOM", "Total Views: "+totalVisits.size());
//        Log.d("DBROOM", "Total Views on UI: "+totalVisits.size());

        if(filter.equals("Missed")){
            missedVisits.clear();
            for(Visit v : totalVisits){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date strDate = null;
                String date = v.getStartTime();

                date = date.substring(0,date.length()-3);

                String[] split = date.split(",");
                try {
                    strDate = sdf.parse(split[0]);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //Log.d("2512 Date", "Visit Date: "+ strDate.toString()+" is: " +(new Date().after(strDate) ? "After" : "Before")+ new Date().toString());
                if(new Date().after(strDate) && !v.getPriority().equals("Completed")){
                    missedVisits.add(v);
                }
            }

             notStarted = 0;
             inProgress = 0;
             completed = 0;
//            totalVisits.clear();
//            totalVisits = missedVisits;
            for (Visit v: missedVisits) {
                switch (v.getPriority()){
                    case "New":
                        notStarted += 1;
                        break;
                    case "InProgress":
                        inProgress += 1;
                        break;
                    case "Completed":
                        completed += 1;
                        break;
                }

                int finalNotStarted = notStarted;
                int finalInProgress = inProgress;
                int finalCompleted = completed;
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)requireView().findViewById(R.id.textView3)).setText("Not Started("+ finalNotStarted +")");
                        ((TextView)requireView().findViewById(R.id.textView10)).setText("In Progress("+ finalInProgress +")");

                        ((TextView)requireView().findViewById(R.id.textView11)).setText("Completed("+ finalCompleted +")");

                    }
                });
            }
        }

        if(filter.equals("High")){


            notStarted = 0;
            inProgress = 0;
            completed = 0;
//            totalVisits.clear();
//            totalVisits = missedVisits;
            for (Visit v: highVisits) {
                switch (v.getPriority()){
                    case "New":
                        notStarted += 1;
                        break;
                    case "InProgress":
                        inProgress += 1;
                        break;
                    case "Completed":
                        completed += 1;
                        break;
                }

                int finalNotStarted = notStarted;
                int finalInProgress = inProgress;
                int finalCompleted = completed;
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)requireView().findViewById(R.id.textView3)).setText("Not Started("+ finalNotStarted +")");
                        ((TextView)requireView().findViewById(R.id.textView10)).setText("In Progress("+ finalInProgress +")");

                        ((TextView)requireView().findViewById(R.id.textView11)).setText("Completed("+ finalCompleted +")");

                    }
                });
            }
        }

//        if(filter.equals("High")) {
//            highVisits.clear();
//            for (Visit v : totalVisits) {
//                if(v.getPriority().equals("High")){
//                    highVisits.add(v);
//                }
//            }
//            totalVisits.clear();
//            totalVisits = highVisits;
//            Log.d("2512", "High Visits: "+ highVisits.size());
//        }

//        int finalHighVisits = highVisits;
//        int finalMissedVisits = missedVisits;
        adapter = new VisitGalleryAdapter(filter.equals("High") ? highVisits : filter.equals("Missed") ? missedVisits :totalVisits,getContext());


        adapter.setHasStableIds(true);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rvVisits.setHasFixedSize(true);
                rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));
                rvVisits.setAdapter(adapter);
                Log.d("2512", "Size:" + adapter.getItemCount());
                rvVisits.setVisibility(View.VISIBLE);
                requireView().findViewById(R.id.progressBar5).setVisibility(View.GONE);
//                isLoaded = true;
//                bVisits.setText("View All "+(totalVisits.size())+" Visits");
//                tvHighPrioNumber.setText(finalHighVisits +" Visits");
//                tvMissedNumber.setText(finalMissedVisits +" Visits");
            }
        });
    }

    void filterTable(String filter){
        switch (filter){
            case "Name":
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //connectSF();


//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                populateView();
//            }
//        });
    }

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

                        if (client.getJSONCredentials() == null) {
                            SalesforceSDKManager.getInstance().
                                    logout(getActivity());
                            return;
                        }

                        if (!SalesforceSDKManager.getInstance().hasNetwork()){
                            ((HostActivity)getActivity()).updateStatusBarColor("#ff675b");
                            tinyDB.putBoolean("Online", false);

                        }else {
                            ((HostActivity)getActivity()).updateStatusBarColor("#1da1f2");
                            tinyDB.putBoolean("Online", true);
                        }
                        // Cache the returned client\
                        client1 = client;
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
                });
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.visits,menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //return super.onOptionsItemSelected(item);

        switch (item.getItemId()){
            case R.id.visitMenuSortPriority:
//                try {
//                    String ownerId = client1.getJSONCredentials().getString("userId");
//                    totalVisits.clear();
//                    if (filter == "High"){
//                        sendRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where Priority__c = 'High' and OwnerId='"+ownerId+"' ORDER BY Priority__c DESC");
//                    }else {
//                        sendRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where OwnerId='" + ownerId + "' ORDER BY Priority__c DESC");
//                    }
//                    } catch (UnsupportedEncodingException | JSONException e) {
//                    e.printStackTrace();
//                }

                totalVisits.sort(new Comparator<Visit>() {
                    @Override
                    public int compare(Visit visit, Visit t1) {
                        return (visit.getPriority().compareTo(t1.getPriority()));
                    }
                });
                VisitGalleryAdapter adapter3 = new VisitGalleryAdapter(totalVisits,getContext());
                rvVisits.setAdapter(adapter3);
                break;

            case R.id.visitMenuSortDate:
//                try {
//                    String ownerId = client1.getJSONCredentials().getString("userId");
//                    totalVisits.clear();
//                    if (filter == "High"){
//                        sendRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where Priority__c = 'High' and OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
//                    }else {
//                        sendRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where OwnerId='" + ownerId + "' ORDER BY VisitStartTime__c DESC");
//                    }
//                    } catch (UnsupportedEncodingException | JSONException e) {
//                    e.printStackTrace();
//                }

                totalVisits.sort(new Comparator<Visit>() {
                    @Override
                    public int compare(Visit visit, Visit t1) {
                        return (t1.getDate().compareTo(visit.getDate()));
                    }
                });
                VisitGalleryAdapter adapter = new VisitGalleryAdapter(totalVisits,getContext());
                rvVisits.setAdapter(adapter);
                break;

            case R.id.visitMenuSortName:

                totalVisits.sort(new Comparator<Visit>() {
                    @Override
                    public int compare(Visit visit, Visit t1) {
                       return (visit.getName().compareTo(t1.getName()));
                    }
                });
                VisitGalleryAdapter adapter1 = new VisitGalleryAdapter(totalVisits,getContext());
                rvVisits.setAdapter(adapter1);

//                try {
//                    String ownerId = client1.getJSONCredentials().getString("userId");
//                    totalVisits.clear();
//                    if (filter == "High"){
//                        sendRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where Priority__c = 'High' and OwnerId='"+ownerId+"' ORDER BY DistributorToVisit__r.Name ASC");
//                    }else {
//                        sendRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where OwnerId='" + ownerId + "' ORDER BY DistributorToVisit__r.Name ASC");
//                    }
//                    } catch (UnsupportedEncodingException | JSONException e) {
//                    e.printStackTrace();
//                }
                break;
        }

        return super.onOptionsItemSelected(item);
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
        VisitGalleryAdapter adapter = new VisitGalleryAdapter(missedVisits,getContext());
        rvVisits.setAdapter(adapter);
        //tvMissedNumber.setText(missedVisits.size()+" Visits");
    }

//    private void sendRequest(String soql) throws UnsupportedEncodingException {
//        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);
//
//        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
//            @Override
//            public void onSuccess(RestRequest request, final RestResponse result) {
//                result.consumeQuietly(); // consume before going back to main thread
//                requireActivity().runOnUiThread(() -> {
//                    try {
//                        //listAdapter.clear();
//                        Log.d("2512", "Result :" + result.toString());
//                        JSONArray records = result.asJSONObject().getJSONArray("records");
////                            for (int i = 0; i < records.length(); i++) {
////                                listAdapter.add(records.getJSONObject(i).getString("Name"));
////                            }
//                        totalVisits = new ArrayList<>();
//
//                        for (int i = 0; i < records.length(); i++) {
//                            if ((records.getJSONObject(i).has("DistributorToVisit__r") && records.getJSONObject(i).getString("DistributorToVisit__r") != "null") || (records.getJSONObject(i).has("RetailerToVisit__r") && records.getJSONObject(i).getString("RetailerToVisit__r") != "null")) {
//                                String name = "";
//
//                                if (records.getJSONObject(i).getString("DistributorToVisit__r") != "null"){
//                                    name = records.getJSONObject(i).getJSONObject("DistributorToVisit__r").getString("Name");
//                                }
//
//                                if (records.getJSONObject(i).getString("RetailerToVisit__r") != "null"){
//                                    name = records.getJSONObject(i).getJSONObject("RetailerToVisit__r").getString("Name");
//                                }
//                                String time = records.getJSONObject(i).getString("VisitStartTime__c");
//                                String prio = records.getJSONObject(i).getString("Status__c");
//                                time = time.replace("T", ", ").substring(0, time.length() - 8);
//                                String id = records.getJSONObject(i).getString("Id");
//                                String date = time.split(",")[0];
//                                Visit vTemp = new Visit(name, time, date, id,prio, "Visit");
//
//                                totalVisits.add(vTemp);
//                            }
//
//                        }
//
//
//                        rvVisits.setHasFixedSize(true);
//                        rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));
//
//                        if(filter !=  "Missed") {
//                            VisitGalleryAdapter adapter = new VisitGalleryAdapter(totalVisits,getContext());
//                            rvVisits.setAdapter(adapter);
//                        } else {
//                            calculateMissed();
//                        }
//
//                        rvVisits.setVisibility(View.VISIBLE);
//                        getView().findViewById(R.id.progressBar5).setVisibility(View.GONE);
//                    } catch (Exception e) {
//                        onError(e);
//                        Log.e("2512", "Login Error: "+e.toString());
//                    }
//                });
//            }
//
//            @Override
//            public void onError(final Exception exception) {
//                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(),
//                        getActivity().getString(R.string.sf__generic_error, exception.toString()),
//                        Toast.LENGTH_LONG).show());
//            }
//        });
//    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        rvVisits = root.findViewById(R.id.rvVisits);
        rvVisits.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                switch (newState){
                    case SCROLL_STATE_IDLE:
                        canChange = true;
                        tabLayout.setEnabled(true);
                        break;
                    case SCROLL_STATE_SETTLING:
                        canChange = false;
                        tabLayout.setEnabled(false);
                        break;
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
        tabLayout = root.findViewById(R.id.tabLayout);

        dialog = new Dialog(getContext());

        dialog.setContentView(R.layout.dialog_layout);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        //dialog.getWindow().getAttributes().windowAnimations = R.style.animation;

        root.findViewById(R.id.imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

        LinearLayout tabLayoutL = (LinearLayout)((ViewGroup) tabLayout.getChildAt(0)).getChildAt(tabLayout.getSelectedTabPosition());
        TextView tabTextView = (TextView) tabLayoutL.getChildAt(1);
        tabTextView.setTypeface(null, Typeface.BOLD);
        //tabTextView.setTextSize(24);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                LinearLayout tabLayoutL = (LinearLayout)((ViewGroup) tabLayout.getChildAt(0)).getChildAt(tab.getPosition());
                TextView tabTextView = (TextView) tabLayoutL.getChildAt(1);
                tabTextView.setTypeface(null, Typeface.BOLD);
                //tabTextView.setTextSize(36);
                if (!canChange){
                    return;
                }

                switch (tab.getText().toString()){
                    case "All":
                        filter = "";
                        selectedIndex = 1;
                        break;
                    case "High Priority":
                        filter = "High";
                        selectedIndex = 2;
                        break;
                    case "Missed":
                        filter = "Missed";
                        selectedIndex = 3;
                        break;
                }

                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        populateView();
                    }
                });

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                LinearLayout tabLayoutL = (LinearLayout)((ViewGroup) tabLayout.getChildAt(0)).getChildAt(tab.getPosition());
                TextView tabTextView = (TextView) tabLayoutL.getChildAt(1);
                tabTextView.setTypeface(null, Typeface.NORMAL);
                //tabTextView.setTextSize(24);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}