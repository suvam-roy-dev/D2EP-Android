package com.pwc.d2ep.ui.visits;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.pwc.d2ep.R;
import com.pwc.d2ep.Visit;
import com.pwc.d2ep.VisitGalleryAdapter;
import com.pwc.d2ep.databinding.FragmentGalleryBinding;
import com.pwc.d2ep.db.AppDatabase;
import com.pwc.d2ep.db.VisitDB;
import com.pwc.d2ep.db.VisitDao;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
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
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        missedVisits = new ArrayList<>();
        totalVisits = new ArrayList<>();
        highVisits = new ArrayList<>();

        AppDatabase db = Room.databaseBuilder(getContext(),
                AppDatabase.class, "d2ep_db").build();

        visitDao = db.visitDao();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                populateView();
            }
        });

        if (getArguments()!=null) {
            filter = getArguments().getString("filter");
        }

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

    private void populateView() {
        totalVisits.clear();
        VisitDB[] visits = visitDao.loadAllVisits();
        //Log.d("DBROOM", "populateView: "+visits.length);
        //int highVisits = 0, missedVisits = 0;
        for (VisitDB v : visits) {
            Visit visit;
            if(!Objects.equals(v.distributorName, "null")) {
                visit = new Visit(v.distributorName, v.time, v.time, v.visitId, v.status, "Visit");
            } else {
                visit = new Visit(v.retailerName, v.time, v.time, v.visitId, v.status, "Visit");
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
                try {
                    strDate = sdf.parse(v.getDate());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //Log.d("2512 Date", "Visit Date: "+ strDate.toString()+" is: " +(new Date().after(strDate) ? "After" : "Before")+ new Date().toString());
                if(new Date().after(strDate) && !v.getPriority().equals("Completed")){
                    missedVisits.add(v);
                }
            }
            totalVisits.clear();
            totalVisits = missedVisits;
        }

        if(filter.equals("High")) {
            highVisits.clear();
            for (Visit v : totalVisits) {
                if(v.getPriority().equals("High")){
                    highVisits.add(v);
                }
            }
            totalVisits.clear();
            totalVisits = highVisits;
        }

//        int finalHighVisits = highVisits;
//        int finalMissedVisits = missedVisits;
        VisitGalleryAdapter adapter = new VisitGalleryAdapter(totalVisits,getContext());
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rvVisits.setHasFixedSize(true);
                rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));
                rvVisits.setAdapter(adapter);

                rvVisits.setVisibility(View.VISIBLE);
                getView().findViewById(R.id.progressBar5).setVisibility(View.GONE);
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

    private void sendRequest(String soql) throws UnsupportedEncodingException {
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                requireActivity().runOnUiThread(() -> {
                    try {
                        //listAdapter.clear();
                        Log.d("2512", "Result :" + result.toString());
                        JSONArray records = result.asJSONObject().getJSONArray("records");
//                            for (int i = 0; i < records.length(); i++) {
//                                listAdapter.add(records.getJSONObject(i).getString("Name"));
//                            }
                        totalVisits = new ArrayList<>();

                        for (int i = 0; i < records.length(); i++) {
                            if ((records.getJSONObject(i).has("DistributorToVisit__r") && records.getJSONObject(i).getString("DistributorToVisit__r") != "null") || (records.getJSONObject(i).has("RetailerToVisit__r") && records.getJSONObject(i).getString("RetailerToVisit__r") != "null")) {
                                String name = "";

                                if (records.getJSONObject(i).getString("DistributorToVisit__r") != "null"){
                                    name = records.getJSONObject(i).getJSONObject("DistributorToVisit__r").getString("Name");
                                }

                                if (records.getJSONObject(i).getString("RetailerToVisit__r") != "null"){
                                    name = records.getJSONObject(i).getJSONObject("RetailerToVisit__r").getString("Name");
                                }
                                String time = records.getJSONObject(i).getString("VisitStartTime__c");
                                String prio = records.getJSONObject(i).getString("Status__c");
                                time = time.replace("T", ", ").substring(0, time.length() - 8);
                                String id = records.getJSONObject(i).getString("Id");
                                String date = time.split(",")[0];
                                Visit vTemp = new Visit(name, time, date, id,prio, "Visit");

                                totalVisits.add(vTemp);
                            }

                        }


                        rvVisits.setHasFixedSize(true);
                        rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));

                        if(filter !=  "Missed") {
                            VisitGalleryAdapter adapter = new VisitGalleryAdapter(totalVisits,getContext());
                            rvVisits.setAdapter(adapter);
                        } else {
                            calculateMissed();
                        }

                        rvVisits.setVisibility(View.VISIBLE);
                        getView().findViewById(R.id.progressBar5).setVisibility(View.GONE);
                    } catch (Exception e) {
                        onError(e);
                        Log.e("2512", "Login Error: "+e.toString());
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(),
                        getActivity().getString(R.string.sf__generic_error, exception.toString()),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        rvVisits = root.findViewById(R.id.rvVisits);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}