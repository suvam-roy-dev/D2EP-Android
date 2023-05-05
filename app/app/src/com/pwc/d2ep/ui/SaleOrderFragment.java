package com.pwc.d2ep.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.fonts.FontFamily;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.pwc.d2ep.BadgeDrawable;
import com.pwc.d2ep.Branch;
import com.pwc.d2ep.NewOrderActivity;
import com.pwc.d2ep.OrderObject;
import com.pwc.d2ep.R;
import com.pwc.d2ep.SalesOrder;
import com.pwc.d2ep.SalesOrdersAdapter;
import com.pwc.d2ep.TinyDB;
import com.pwc.d2ep.Visit;
import com.pwc.d2ep.VisitGalleryAdapter;
import com.pwc.d2ep.databinding.FragmentSaleOrderBinding;
import com.pwc.d2ep.ui.home.DashboardViewModel;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;
import smartdevelop.ir.eram.showcaseviewlib.config.PointerType;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class SaleOrderFragment extends Fragment {

    private FragmentSaleOrderBinding binding;
    private RestClient client1;
    private String ownerId;
    private String dealerID;
    private RecyclerView rvOrders;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private TextView tvDraft,tvProgress,tvComplete;
    Dialog dialog;
    final int[] selectedSpin = {0};
    Spinner spin;
    int selectedIndex = 1;
    ArrayAdapter aa;
    ArrayList<SalesOrder> ordersList;
    String[] statuses = {"All" ,"Draft", "In Progress", "Completed"};
    SwipeRefreshLayout mRefresh;
    RadioGroup rgSort;
    RadioGroup rgFilter;

    boolean isFirst = true;
//    void startGuide(){
////        new GuideView.Builder(getContext())
////                .setTitle("Sort & Filter orders list")
////                //.setContentText("You can open Sort/Filter menu by tapping this button!")
////                .setGravity(Gravity.center)
////                .setTargetView(binding.imageButton3)
////                .setPointerType(PointerType.circle)
////                .setDismissType(DismissType.outside) //optional - default dismissible by TargetView
////                .build()
////                .show();
////
////        new GuideView.Builder(getContext())
////                .setTitle("Create new order")
////                //.setContentText("You can create")
////                .setGravity(Gravity.center)
////                .setTargetView(binding.button9)
////                .setPointerType(PointerType.circle)
////                .setDismissType(DismissType.outside) //optional - default dismissible by TargetView
////                .build()
////                .show();
//
////        new MaterialShowcaseView.Builder(getActivity())
////                .setTarget(binding.button9)
////                .setDismissText("GOT IT")
////                .setContentText("")
////                .setDelay(500) // optional but starting animations immediately in onCreate can make them choppy
////                .singleUse("2511") // provide a unique ID used to ensure it is only shown once
////                .show();
//
//
//        // sequence example
//        ShowcaseConfig config = new ShowcaseConfig();
//        config.setMaskColor(getActivity().getColor(R.color.text_color_dark_50));
//        config.setDelay(300); // half second between each showcase view
//
//        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(getActivity(), "4");
//
//        sequence.setConfig(config);
//
//        sequence.addSequenceItem(binding.imageButton3,
//                "You can sort/filter the sales orders list from here.", "Next >");
//
//        sequence.addSequenceItem(binding.button9,
//                "You can create new sales order by clicking this button.", "Next >");
//
////        sequence.addSequenceItem(mButtonTwo,
////                "This is button two", "GOT IT");
////
////        sequence.addSequenceItem(mButtonThree,
////                "This is button three", "GOT IT");
//
//        sequence.start();
//
//    }

    public static String round(double value) {
//        if (places < 0) throw new IllegalArgumentException();
//
//        BigDecimal bd = BigDecimal.valueOf(value);
//        bd = bd.setScale(places, RoundingMode.HALF_UP);
//        return bd.doubleValue();
        String val = String.valueOf((double) Math.round(value * 100) / 100);

        return (val.charAt(val.length()-1)) == '0' ? val+"0":val;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        ordersList = new ArrayList<>();
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        aa = new ArrayAdapter(getContext(),android.R.layout.simple_spinner_item,statuses);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding = FragmentSaleOrderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        rvOrders = binding.rvSalesOrders;
        tvDraft = binding.textView35;
        tvProgress = binding.textView37;
        tvComplete = binding.textView38;

        dialog = new Dialog(getContext());

        dialog.setContentView(R.layout.dialog_layout);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        rgSort = dialog.findViewById(R.id.rgSort);
        rgFilter = dialog.findViewById(R.id.rgFilter);

        mRefresh = binding.swiperefresh;

        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                connectSF();
                getView().findViewById(R.id.progressBar7).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.rvSalesOrders).setVisibility(View.INVISIBLE);
                ((ImageButton)getView().findViewById(R.id.imageButton3)).setImageResource(R.drawable.dots);

                rgFilter.clearCheck();
                rgSort.clearCheck();
                dialog.findViewById(R.id.etFilterName).setVisibility(View.GONE);
                dialog.findViewById(R.id.etFilterId).setVisibility(View.GONE);

                ((EditText)dialog.findViewById(R.id.etFilterName)).setText("");
                ((EditText)dialog.findViewById(R.id.etFilterId)).setText("");
                selectedSpin[0] = 0;
                mRefresh.setRefreshing(false);
            }
        });

        rvOrders.setHasFixedSize(true);
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.button9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getContext(), NewOrderActivity.class));
            }
        });

        binding.imageButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
        //startGuide();
        return root;
    }

    void showDialog(){
        final int[] selectedTab = {1};



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
                ((ImageButton)getView().findViewById(R.id.imageButton3)).setImageResource(R.drawable.dots);
                rgFilter.clearCheck();
                rgSort.clearCheck();
                dialog.findViewById(R.id.etFilterName).setVisibility(View.GONE);
                dialog.findViewById(R.id.etFilterId).setVisibility(View.GONE);

                ((EditText)dialog.findViewById(R.id.etFilterName)).setText("");
                ((EditText)dialog.findViewById(R.id.etFilterId)).setText("");
                selectedSpin[0] = 0;
                getView().findViewById(R.id.progressBar7).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.rvSalesOrders).setVisibility(View.INVISIBLE);

                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        connectSF();
                    }
                });
                dialog.dismiss();

            }
        });

        ((RadioButton)dialog.findViewById(R.id.radioButton3)).setText("Status");

        ((RadioButton)dialog.findViewById(R.id.radioButton4)).setText("Customer Name");
        ((RadioButton)dialog.findViewById(R.id.radioButton5)).setText("Order ID");
        ((EditText)dialog.findViewById(R.id.etFilterId)).setHint("Order ID");

        dialog.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ImageButton)getView().findViewById(R.id.imageButton3)).setImageResource(R.drawable.dots_badge);
                if (selectedTab[0] == 1) {
                    switch (rgSort.getCheckedRadioButtonId()) {
                        case R.id.radioButton:
                            ordersList.sort(new Comparator<SalesOrder>() {
                                @Override
                                public int compare(SalesOrder visit, SalesOrder t1) {
                                    return (visit.getCustomerName().compareTo(t1.getCustomerName()));
                                }
                            });
                            Log.d("2512", "Should Sort: ");
                            SalesOrdersAdapter adapter1 = new SalesOrdersAdapter(ordersList, getContext());
                            adapter1.setHasStableIds(true);
                            rvOrders.setAdapter(adapter1);
                            break;
                        case R.id.radioButton2:
                            ordersList.sort(new Comparator<SalesOrder>() {
                                @Override
                                public int compare(SalesOrder visit, SalesOrder t1) {
                                    return (t1.getDate().compareTo(visit.getDate()));
                                }
                            });
                            SalesOrdersAdapter adapter = new SalesOrdersAdapter(ordersList, getContext());
                            adapter.setHasStableIds(true);
                            rvOrders.setAdapter(adapter);
                            break;
                        case R.id.radioButton3:
                            ordersList.sort(new Comparator<SalesOrder>() {
                                @Override
                                public int compare(SalesOrder visit, SalesOrder t1) {
                                    return (visit.getStatus().compareTo(t1.getStatus()));
                                }
                            });
                            SalesOrdersAdapter adapter3 = new SalesOrdersAdapter(ordersList, getContext());
                            adapter3.setHasStableIds(true);
                            rvOrders.setAdapter(adapter3);
                            break;
                    }
                    dialog.dismiss();
                } else {
                    switch (rgFilter.getCheckedRadioButtonId()) {
                        case R.id.radioButton4:
                            ArrayList<SalesOrder> filterVisits = new ArrayList<>();
                            Log.d("2512", "onClick: Should filter list");
                            for (SalesOrder v: ordersList) {
                                if(v.getCustomerName().toLowerCase().contains(((EditText)dialog.findViewById(R.id.etFilterName)).getText().toString().toLowerCase())){
                                    filterVisits.add(v);
                                }
                            }
                            SalesOrdersAdapter adapter = new SalesOrdersAdapter(filterVisits, getContext());
                            adapter.setHasStableIds(true);
                            rvOrders.setAdapter(adapter);
                            break;
                        case R.id.radioButton5:
                            ArrayList<SalesOrder> filterVisitsID = new ArrayList<>();
                            for (SalesOrder v: ordersList) {
                                Log.d("2512", "onClick: Comparing "+v.getOrderName()+" to "+ ((EditText)dialog.findViewById(R.id.etFilterId)).getText().toString());
                                if(v.getOrderName().toLowerCase().contains(((EditText)dialog.findViewById(R.id.etFilterId)).getText().toString().toLowerCase())){
                                    filterVisitsID.add(v);
                                }
                            }
                            SalesOrdersAdapter adapterID = new SalesOrdersAdapter(filterVisitsID, getContext());
                            rvOrders.setAdapter(adapterID);
                            break;

                        case R.id.radioButton6:
                            String st = "New";

                            switch (spin.getSelectedItem().toString()){
                                case "All":
                                    st = "All";
                                    break;
                                case "Draft":
                                    st = "Draft";
                                    break;
                                case "In Progress":
                                    st = "Submitted For Approval";
                                    break;
                                case "Completed":
                                    st = "Closed";
                                    break;
                            }

                            ArrayList<SalesOrder> filterVisitsSt = new ArrayList<>();
                            Log.d("2512", "onClick: Should filter list by" + st);
                            for (SalesOrder v: ordersList) {
                                if (st.equals("All")){
                                    filterVisitsSt.add(v);
                                }
                                if(v.getStatus().equals(st)){
                                    filterVisitsSt.add(v);
                                }
                            }
                            Log.d("2512", "onClick: should show visits: "+filterVisitsSt.size());
                            SalesOrdersAdapter adapterSt = new SalesOrdersAdapter(filterVisitsSt, getContext());
                            adapterSt.setHasStableIds(true);
                            rvOrders.setAdapter(adapterSt);
                            break;
                    }
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    private void populateView() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {

        if(isFirst || new TinyDB(getContext()).getBoolean("RefreshOrders")) {
            new TinyDB(getContext()).putBoolean("RefreshOrders", false);
            new TinyDB(getContext()).putBoolean("CloseNewOrder", false);
            ordersList.clear();
            connectSF();
            getView().findViewById(R.id.progressBar7).setVisibility(View.VISIBLE);
            getView().findViewById(R.id.rvSalesOrders).setVisibility(View.INVISIBLE);
            isFirst = false;
        }
        super.onResume();
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
                        if (client == null) {
                            SalesforceSDKManager.getInstance().
                                    logout(getActivity());
                            return;
                        }
                        // Cache the returned client
                        client1 = client;
                        try {
                            ownerId = client1.getJSONCredentials().getString("userId");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.d("2512", ""+client.getClientInfo());
                        checkUserType();
                       // AsyncTask.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                try {
//                                    connectDB();
//                                } catch (UnsupportedEncodingException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        });
                    }
                });
    }

    void checkUserType()  {
        String soql = null;
        try {
            soql = "SELECT Id,name,account.parentId ,contact.AccountId,account.name FROM User where id='"+client1.getJSONCredentials().getString("userId")+"'";
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            JSONArray records = result.asJSONObject().getJSONArray("records");
                            String parentID = records.getJSONObject(0).getJSONObject("Account").getString("ParentId");
                            Log.d("2513", "parentid: "+parentID);
                            if(parentID.matches("null")){
                                fetchAccountID();
                            }else{
                                fetchParentID(parentID);
                            }

                            Log.d("2513", "Check User Type "+result.toString());
//                            fetchBranches();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(getContext(),
//                                "Unable to connect to Salesforce Server!",
//                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    void fetchParentID(String parentID) throws UnsupportedEncodingException {
        String soql = null;
        soql = "SELECT Id, Name from Account where Id='"+parentID+"'";
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            JSONArray records = result.asJSONObject().getJSONArray("records");
                            dealerID = records.getJSONObject(0).getString("Id");
                            //dealerName = records.getJSONObject(0).getString("Name");
                            Log.d("2513", "Parent Info: "+result.toString());
                            //fetchBranches();
                            fetchSalesOrders();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(getContext(),
//                                "Unable to connect to Salesforce Server!",
//                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void fetchAccountID(){
        try {
            String ownerId = client1.getJSONCredentials().getString("userId");
            //sendHighRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Priority__c='High' ORDER BY VisitStartTime__c ASC");
            //sendMissedRequest("SELECT Name,Priority__c,DistributorToVisit__r.Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' and Status__C='Missed' ORDER BY VisitStartTime__c DESC");
            //sendTotalRequest("SELECT Name,ID,Status__c,Priority__c,DistributorToVisit__c, DistributorToVisit__r.Name,VisitStartTime__c,VisitEndTime__c, DistributorToVisit__r.BillingStreet, RetailerToVisit__c,RetailerToVisit__r.Name, RetailerToVisit__r.MailingStreet, BeatPlanning__c,BeatPlanning__r.Name, BeatPlanning__r.BeatTasks__c, BeatLocation__c, BeatLocation__r.Name, Owner.Name from Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
            String soql = "Select Id, Contact.Id, Contact.Name, Contact.Account.ID from User where id='"+ownerId+"'";
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
                                JSONObject contact = records.getJSONObject(0).getJSONObject("Contact");
                                JSONObject account = contact.getJSONObject("Account");
                                dealerID = account.getString("Id");
                                Log.d("2512", "Dealer ID :" + dealerID);

                                fetchSalesOrders();
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

                            //loadCachedVisits("Total");

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

    private void fetchSalesOrders() {
            ordersList.clear();
        try {
//            String soql = "select Id,Name,State__c,Dealer__r.name,Branch__r.name,Customer__r.name,IssueDate__c,TotalCost__c,Subtotal__c,Tax__c,Discount__c,AdditionalChargeApplied__c,AdditionalDiscountApplied__c from SalesOrder__c where Dealer__c ='"+dealerID+"'";
//
            //String soql = "select Id,Name,State__c,Status__c,Dealer__r.name,Branch__r.name,Customer__r.name,IssueDate__c,TotalCost__c,Subtotal__c,Tax__c,Discount__c,AdditionalChargeApplied__c,AdditionalDiscountApplied__c,(select Product__c from Sales_Order_Details__r )  from SalesOrder__c where Dealer__c ='"+dealerID+"' ORDER BY LastModifiedDate Desc";

            String soql = "select Id,Name,State__c,Status__c,Dealer__r.name,Branch__r.name,Customer__r.name,IssueDate__c,TotalCost__c,Subtotal__c,Tax__c,Discount__c,AdditionalChargeApplied__c,AdditionalDiscountApplied__c,(select Product__c from Sales_Order_Details__r ) from SalesOrder__c where Dealer__c ='"+dealerID+"' AND CreatedById = '"+ownerId+"' ORDER BY LastModifiedDate Desc";
            RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getContext()), soql);
            Log.d("SalesOrdersNew", "Checking for Query :" + soql);

            client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //listAdapter.clear();
                                //Log.d("2515", "Sales Orders :" + result.toString());

                                log(result.toString());
                                JSONArray records = result.asJSONObject().getJSONArray("records");


                                int drafts = 0;
                                int inProgress = 0;
                                int completed = 0;
                                for (int i = 0; i < records.length(); i++) {

                                    String orderID = records.getJSONObject(i).getString("Id");
                                    String orderName = records.getJSONObject(i).getString("Name");
                                    String status = records.getJSONObject(i).getString("Status__c");
                                    String date = records.getJSONObject(i).getString("IssueDate__c");
                                    String totalCost = records.getJSONObject(i).getString("TotalCost__c");

                                    String customerName = records.getJSONObject(i).getJSONObject("Customer__r").getString("Name");
                                    String branchName = records.getJSONObject(i).getJSONObject("Branch__r").getString("Name");
                                    String productCount = "0";

                                    if (records.getJSONObject(i).has("Sales_Order_Details__r") && records.getJSONObject(i).getString("Sales_Order_Details__r") != "null" ) {
                                        productCount = records.getJSONObject(i).getJSONObject("Sales_Order_Details__r").getString("totalSize");
                                    }

                                    if(records.getJSONObject(i).getString("State__c").length()==0){
                                        status = "Draft";
                                    }

                                    SalesOrder newOrder = new SalesOrder(orderID,orderName,status,customerName,branchName,date,productCount,totalCost);

                                    // TODO: 24-02-2023 Uncomment line below once null date issue is fixed
                                    if(!newOrder.getDate().equals("null") && !newOrder.getTotalCost().equals("null") && checkDate(newOrder.getDate()))ordersList.add(newOrder);


                                    //ordersList.add(newOrder);
                                    switch (status){
                                        case "null":
                                            drafts++;
                                            break;
                                        case "Closed":
                                            completed++;
                                            break;
                                        case "Draft":
                                            drafts++;
                                            break;
                                        default:
                                            inProgress++;
                                            break;
                                    }
                                }

                                int finalDrafts = drafts;
                                int finalInProgress = inProgress;
                                int finalCompleted = completed;
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tvDraft.setText("Draft ("+ finalDrafts +")");
                                        tvProgress.setText("In Progress ("+ finalInProgress +")");
                                        tvComplete.setText("Complete ("+ finalCompleted +")");
                                        getView().findViewById(R.id.progressBar7).setVisibility(View.INVISIBLE);
//                                        ordersList.sort(new Comparator<SalesOrder>() {
//                                            @Override
//                                            public int compare(SalesOrder salesOrder, SalesOrder t1) {
//                                                return salesOrder.getDate().compareTo(t1.getDate());
//                                            }
//                                        });
//
//                                        Collections.reverse(ordersList);
                                        SalesOrdersAdapter adapter = new SalesOrdersAdapter(ordersList,getContext());

                                        rvOrders.setAdapter(adapter);
                                        rvOrders.setVisibility(View.VISIBLE);
                                    }
                                });
                                Log.d("2513", "Total Orders: "+ ordersList);
                            } catch (Exception e){
                                onError(e);
                                Log.e("2512", "Login Error: "+e.toString());
//
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

                            //loadCachedVisits("Total");

                        }
                    });
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private boolean checkDate(String date) {
       return true;
    }

    public void log(String message) {
        // Split by line, then ensure each line can fit into Log's maximum length.
        for (int i = 0, length = message.length(); i < length; i++) {
            int newline = message.indexOf('\n', i);
            newline = newline != -1 ? newline : length;
            do {
                int end = Math.min(newline, i + 5000);
                Log.d("SalesOrdersNew", message.substring(i, end));
                i = end;
            } while (i < newline);
        }
    }
}