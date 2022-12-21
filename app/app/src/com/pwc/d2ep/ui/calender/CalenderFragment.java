package com.pwc.d2ep.ui.calender;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.pwc.d2ep.R;
import com.pwc.d2ep.Visit;
import com.pwc.d2ep.VisitGalleryAdapter;
import com.pwc.d2ep.databinding.FragmentSlideshowBinding;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalenderFragment extends Fragment implements View.OnClickListener {

    private FragmentSlideshowBinding binding;
    private RestClient client1;
    private RecyclerView rvVisits;
    private ArrayList<Visit> totalVisits;
    private ArrayList<Visit> selectedVisits;
    private CardView cvCurrent, cvN1, cvN2, cvP1, cvP2;
    private TextView tvCurrentPrimary, tvCurrentSecondary, tvN1Primary, tvN1Secondary, tvN2Primary, tvN2Secondary,tvP1Primary, tvP1Secondary,tvP2Primary, tvP2Secondary;
    private boolean isDate = true, isMonth = false;

    private Date currentDate, n1Date, n2Date, p1Date, p2Date;
    private Date currentMonth, n1Month, p1Month;
    private VisitDao visitDao;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        selectedVisits = new ArrayList<>();
        totalVisits = new ArrayList<>();

        AppDatabase db = Room.databaseBuilder(getContext(),
                AppDatabase.class, "d2ep_db").build();

        visitDao = db.visitDao();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                populateView();
            }
        });


//        String accountType =
//                SalesforceSDKManager.getInstance().getAccountType();
//
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
//                                try {
//                                    String ownerId = client.getJSONCredentials().getString("userId");
//
//                                    sendRequest("SELECT ID,Status__c,Priority__c,DistributorToVisit__r.Name,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"' ORDER BY VisitStartTime__c DESC");
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
                Visit visit = new Visit(v.distributorName, v.time, v.time, v.visitId, v.status, "Visit");
                totalVisits.add(visit);
                //Log.d("DBROOM", "New Visit: "+visit.getName() +" Synced: "+v.isSynced);
            }


            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getVisitsByDate(new Date());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });
    }


    @Override
    public void onResume() {
        super.onResume();

    }


    private void getVisitsByDate(Date date) throws ParseException {

        getView().findViewById(R.id.textView8).setVisibility(View.INVISIBLE);
        selectedVisits.clear();
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.setTime(date);

        Calendar visitDate = Calendar.getInstance();

        for(Visit v : totalVisits){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date strDate = sdf.parse(v.getDate());
            //Log.d("2512 Date", "Visit Date: "+ strDate.toString()+" is: " +(new Date().after(strDate) ? "After" : "Before")+ new Date().toString());
            visitDate.setTime(strDate);
            if(selectedDate.get(Calendar.DAY_OF_MONTH) == visitDate.get(Calendar.DAY_OF_MONTH) && selectedDate.get(Calendar.MONTH) == visitDate.get(Calendar.MONTH) && selectedDate.get(Calendar.YEAR) == visitDate.get(Calendar.YEAR)){
                Log.d("2512", "Comparing : "+ date +" To "+strDate);
                selectedVisits.add(v);
            }
        }
        Log.d("2512", "Selected Visits: "+ selectedVisits.size());
        VisitGalleryAdapter adapter = new VisitGalleryAdapter(selectedVisits,getContext());
        rvVisits.setHasFixedSize(true);
        rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));
        rvVisits.setAdapter(adapter);

        rvVisits.setVisibility(View.VISIBLE);
        getView().findViewById(R.id.progressBar6).setVisibility(View.GONE);

        if(selectedVisits.size() == 0){
            getView().findViewById(R.id.textView8).setVisibility(View.VISIBLE);
        }
    }

    private void getVisitsByMonth(Date date) throws ParseException {
        getView().findViewById(R.id.textView8).setVisibility(View.INVISIBLE);
        selectedVisits.clear();
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.setTime(date);

        Calendar visitDate = Calendar.getInstance();

        for(Visit v : totalVisits){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date strDate = sdf.parse(v.getDate());
            //Log.d("2512 Date", "Visit Date: "+ strDate.toString()+" is: " +(new Date().after(strDate) ? "After" : "Before")+ new Date().toString());
            visitDate.setTime(strDate);
            if(selectedDate.get(Calendar.MONTH) == visitDate.get(Calendar.MONTH) && selectedDate.get(Calendar.YEAR) == visitDate.get(Calendar.YEAR)){
                Log.d("2512", "Comparing : "+ date +" To "+strDate);
                selectedVisits.add(v);
            }
        }
        Log.d("2512", "Selected Visits: "+ selectedVisits.size());
        VisitGalleryAdapter adapter = new VisitGalleryAdapter(selectedVisits,getContext());
        rvVisits.setHasFixedSize(true);
        rvVisits.setLayoutManager(new LinearLayoutManager(getContext()));
        rvVisits.setAdapter(adapter);

        rvVisits.setVisibility(View.VISIBLE);
        getView().findViewById(R.id.progressBar6).setVisibility(View.GONE);
        if(selectedVisits.size() == 0){
            getView().findViewById(R.id.textView8).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.calender,menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //return super.onOptionsItemSelected(item);

        switch (item.getItemId()){
            case R.id.calenderMenuDate:
                cvN2.setVisibility(View.VISIBLE);
                cvP2.setVisibility(View.VISIBLE);
                setDates();
                if(isMonth){
                    setTextColors(19,getView());
                }
                isDate = true;
                isMonth = false;
                try {
                    getVisitsByDate(currentDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }


//                cvN1.getLayoutParams().width = 64;
//                cvP1.getLayoutParams().width = 64;
//                cvCurrent.getLayoutParams().width = 64;
                break;
            case R.id.calenderMenuMonth:
                cvN2.setVisibility(View.GONE);
                cvP2.setVisibility(View.GONE);
                if(isDate){
                    setTextColors(19,getView());
                }
                isDate = false;
                isMonth = true;
                setMonths();
                try {
                    getVisitsByMonth(new Date());
                } catch (ParseException e) {
                    e.printStackTrace();
                }



//                cvN1.getLayoutParams().width = 100;
//                cvP1.getLayoutParams().width = 100;
//                cvCurrent.getLayoutParams().width = 100;
                break;


        }

        return super.onOptionsItemSelected(item);
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
                            if (records.getJSONObject(i).has("DistributorToVisit__r") && records.getJSONObject(i).getString("DistributorToVisit__r") != "null") {
                                String name = (records.getJSONObject(i).getJSONObject("DistributorToVisit__r").getString("Name"));
                                String time = records.getJSONObject(i).getString("VisitStartTime__c");
                                String prio = records.getJSONObject(i).getString("Status__c");
                                time = time.replace("T", ", ").substring(0, time.length() - 8);
                                String id = records.getJSONObject(i).getString("Id");
                                String date = time.split(",")[0];
                                Visit vTemp = new Visit(name, time, date, id,prio, "Visit");

                                totalVisits.add(vTemp);
                            }

                        }

                        getVisitsByDate(new Date());
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
        SlideshowViewModel slideshowViewModel =
                new ViewModelProvider(this).get(SlideshowViewModel.class);

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ((Button)root.findViewById(R.id.bLastDates)).setText("<");
        ((Button)root.findViewById(R.id.bNextDates)).setText(">");

        rvVisits = root.findViewById(R.id.rvVisitsCalender);

        cvN1 = root.findViewById(R.id.cvCalenderN1);
        cvN1.setOnClickListener(this);
        cvN2 = root.findViewById(R.id.cvCalenderN2);
        cvN2.setOnClickListener(this);
        cvP1 = root.findViewById(R.id.cvCalenderP1);
        cvP1.setOnClickListener(this);
        cvP2 = root.findViewById(R.id.cvCalenderP2);
        cvP2.setOnClickListener(this);
        cvCurrent = root.findViewById(R.id.cvCalenderCurrent);
        cvCurrent.setOnClickListener(this);

        tvCurrentPrimary = root.findViewById(R.id.tvCalenderCurrentPrimary);
        tvCurrentSecondary = root.findViewById(R.id.tvCalenderCurrentSecondary);

        tvN1Primary = root.findViewById(R.id.tvCalenderN1Primary);
        tvN1Secondary = root.findViewById(R.id.tvCalenderN1Secondary);

        tvN2Primary = root.findViewById(R.id.tvCalenderN2Primary);
        tvN2Secondary = root.findViewById(R.id.tvCalenderN2Secondary);

        tvP1Primary = root.findViewById(R.id.tvCalenderP1Primary);
        tvP1Secondary = root.findViewById(R.id.tvCalenderP1Secondary);


        tvP2Primary = root.findViewById(R.id.tvCalenderP2Primary);
        tvP2Secondary = root.findViewById(R.id.tvCalenderP2Secondary);

        root.findViewById(R.id.bNextDates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setNextDates();
            }
        });

        root.findViewById(R.id.bLastDates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setPreviousDates();
            }
        });
        setDates();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.cvCalenderN1:
                setTextColors(1, view);
//                cvN1.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
//                cvN1.setCardElevation(8);
//                setTextColors(1, view);
//                cvP1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvP2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvCurrent.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvN2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
                try {
                    if (isDate) {
                        getVisitsByDate(n1Date);
                    }else {
                        getVisitsByMonth(n1Month);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.cvCalenderN2:
                setTextColors(2,view);
//                cvN1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvP1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvP2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvCurrent.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvN2.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
//                cvN2.setCardElevation(8);
                try {
                    getVisitsByDate(n2Date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.cvCalenderP1:
                setTextColors(-1,view);
//                cvN1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvP1.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
//                cvP1.setCardElevation(8);
//                cvP2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvCurrent.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvN2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
                try {
                    if(isDate) {
                        getVisitsByDate(p1Date);
                    }else getVisitsByMonth(p1Month);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.cvCalenderP2:
                setTextColors(-2,view);
//                cvN1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvP1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvP2.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
//                cvP2.setCardElevation(8);
//                cvCurrent.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvN2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
                try {
                    getVisitsByDate(p2Date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.cvCalenderCurrent:
                setTextColors(0,view);
//                cvN1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvP1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvP2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
//                cvCurrent.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
//                cvCurrent.setCardElevation(8);
//                cvN2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
                try {
                    if(isDate) {
                        getVisitsByDate(currentDate);
                    }else getVisitsByMonth(currentMonth);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void setTextColors(int i,View view) {

        ((TextView) view.getRootView().findViewById(R.id.tvCalenderN2Primary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));
        ((TextView) view.getRootView().findViewById(R.id.tvCalenderN2Secondary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));
        ((TextView) view.getRootView().findViewById(R.id.tvCalenderN1Primary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));
        ((TextView) view.getRootView().findViewById(R.id.tvCalenderN1Secondary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));
        ((TextView) view.getRootView().findViewById(R.id.tvCalenderP1Primary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));
        ((TextView) view.getRootView().findViewById(R.id.tvCalenderP1Secondary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));
        ((TextView) view.getRootView().findViewById(R.id.tvCalenderP2Primary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));
        ((TextView) view.getRootView().findViewById(R.id.tvCalenderP2Secondary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));
        ((TextView) view.getRootView().findViewById(R.id.tvCalenderCurrentPrimary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));
        ((TextView) view.getRootView().findViewById(R.id.tvCalenderCurrentSecondary)).setTextColor(getResources().getColor(R.color.disbaled_color,null));

        cvN1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
        cvP1.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
        cvP2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
        cvCurrent.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));
        cvN2.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background, null));

        switch (i){
            case -2:
                ((TextView) view.getRootView().findViewById(R.id.tvCalenderP2Primary)).setTextColor(getResources().getColor(android.R.color.white,null));
                ((TextView) view.getRootView().findViewById(R.id.tvCalenderP2Secondary)).setTextColor(getResources().getColor(android.R.color.white,null));
                cvP2.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
                break;
            case -1:
                ((TextView) view.getRootView().findViewById(R.id.tvCalenderP1Primary)).setTextColor(getResources().getColor(android.R.color.white,null));
                ((TextView) view.getRootView().findViewById(R.id.tvCalenderP1Secondary)).setTextColor(getResources().getColor(android.R.color.white,null));
                cvP1.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
                break;
            case 0:

                ((TextView) view.getRootView().findViewById(R.id.tvCalenderCurrentPrimary)).setTextColor(getResources().getColor(android.R.color.white,null));
                ((TextView) view.getRootView().findViewById(R.id.tvCalenderCurrentSecondary)).setTextColor(getResources().getColor(android.R.color.white,null));
                cvCurrent.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
                break;
            case 1:
                ((TextView) view.getRootView().findViewById(R.id.tvCalenderN1Primary)).setTextColor(getResources().getColor(android.R.color.white,null));
                ((TextView) view.getRootView().findViewById(R.id.tvCalenderN1Secondary)).setTextColor(getResources().getColor(android.R.color.white,null));
                cvN1.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
                break;
            case 2:
                ((TextView) view.getRootView().findViewById(R.id.tvCalenderN2Primary)).setTextColor(getResources().getColor(android.R.color.white,null));
                ((TextView) view.getRootView().findViewById(R.id.tvCalenderN2Secondary)).setTextColor(getResources().getColor(android.R.color.white,null));
                cvN2.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
                break;
        }
    }

    void setDates(){

        Calendar calendar = Calendar.getInstance();
        currentDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, 1);
        n1Date = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, 1);
        n2Date = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, -3);
        p1Date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        p2Date = calendar.getTime();

        tvCurrentPrimary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(currentDate));
        tvCurrentSecondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(currentDate));

        tvN1Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(n1Date));
        tvN1Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(n1Date));

        tvN2Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(n2Date));
        tvN2Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(n2Date));

        tvP1Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(p1Date));
        tvP1Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(p1Date));

        tvP2Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(p2Date));
        tvP2Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(p2Date));
    }

    private void setNextDates(){
        setTextColors(19,getView());
        if(isDate){
            Calendar c = Calendar.getInstance();
            c.setTime(currentDate);
            c.add(Calendar.DATE,5);
            currentDate = c.getTime();

            tvCurrentPrimary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(currentDate));
            tvCurrentSecondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(currentDate));

            c.setTime(n1Date);
            c.add(Calendar.DATE,5);
            n1Date = c.getTime();

            tvN1Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(n1Date));
            tvN1Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(n1Date));

            c.setTime(n2Date);
            c.add(Calendar.DATE,5);
            n2Date = c.getTime();

            tvN2Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(n2Date));
            tvN2Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(n2Date));

            c.setTime(p1Date);
            c.add(Calendar.DATE,5);
            p1Date = c.getTime();

            tvP1Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(p1Date));
            tvP1Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(p1Date));

            c.setTime(p2Date);
            c.add(Calendar.DATE,5);
            p2Date = c.getTime();

            tvP2Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(p2Date));
            tvP2Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(p2Date));
        }


        if(isMonth){
            Calendar c = Calendar.getInstance();
            c.setTime(currentMonth);
            c.add(Calendar.MONTH,3);
            currentMonth = c.getTime();

            tvCurrentPrimary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(currentMonth));
            tvCurrentSecondary.setText(new SimpleDateFormat("yyyy", Locale.getDefault()).format(currentMonth));

            c.setTime(n1Month);
            c.add(Calendar.MONTH,3);
            n1Month = c.getTime();

            tvN1Primary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(n1Month));
            tvN1Secondary.setText(new SimpleDateFormat("yyyy", Locale.getDefault()).format(n1Month));


            c.setTime(p1Month);
            c.add(Calendar.MONTH,3);
            p1Month = c.getTime();

            tvP1Primary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(p1Month));
            tvP1Secondary.setText(new SimpleDateFormat("yyyy", Locale.getDefault()).format(p1Month));

        }

    }

    void setMonths(){

        Calendar calendar = Calendar.getInstance();
        currentMonth = calendar.getTime();

        calendar.add(Calendar.MONTH, 1);
        n1Month = calendar.getTime();


        calendar.add(Calendar.MONTH, -2);
        p1Month = calendar.getTime();

        tvCurrentPrimary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(currentMonth));
        tvCurrentSecondary.setText(new SimpleDateFormat("YYYY", Locale.getDefault()).format(currentMonth));

        tvN1Primary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(n1Month));
        tvN1Secondary.setText(new SimpleDateFormat("YYYY", Locale.getDefault()).format(n1Month));

        tvP1Primary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(p1Month));
        tvP1Secondary.setText(new SimpleDateFormat("YYYY", Locale.getDefault()).format(p1Month));
    }

    private void setPreviousDates(){
        setTextColors(19,getView());
        if(isDate){
            Calendar c = Calendar.getInstance();
            c.setTime(currentDate);
            c.add(Calendar.DATE,-5);
            currentDate = c.getTime();

            tvCurrentPrimary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(currentDate));
            tvCurrentSecondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(currentDate));

            c.setTime(n1Date);
            c.add(Calendar.DATE,-5);
            n1Date = c.getTime();

            tvN1Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(n1Date));
            tvN1Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(n1Date));

            c.setTime(n2Date);
            c.add(Calendar.DATE,-5);
            n2Date = c.getTime();

            tvN2Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(n2Date));
            tvN2Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(n2Date));

            c.setTime(p1Date);
            c.add(Calendar.DATE,-5);
            p1Date = c.getTime();

            tvP1Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(p1Date));
            tvP1Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(p1Date));

            c.setTime(p2Date);
            c.add(Calendar.DATE,-5);
            p2Date = c.getTime();

            tvP2Primary.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(p2Date));
            tvP2Secondary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(p2Date));
        }


        if(isMonth){
            Calendar c = Calendar.getInstance();
            c.setTime(currentMonth);
            c.add(Calendar.MONTH,-3);
            currentMonth = c.getTime();

            tvCurrentPrimary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(currentMonth));
            tvCurrentSecondary.setText(new SimpleDateFormat("yyyy", Locale.getDefault()).format(currentMonth));

            c.setTime(n1Month);
            c.add(Calendar.MONTH,-3);
            n1Month = c.getTime();

            tvN1Primary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(n1Month));
            tvN1Secondary.setText(new SimpleDateFormat("yyyy", Locale.getDefault()).format(n1Month));


            c.setTime(p1Month);
            c.add(Calendar.MONTH,-3);
            p1Month = c.getTime();

            tvP1Primary.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(p1Month));
            tvP1Secondary.setText(new SimpleDateFormat("yyyy", Locale.getDefault()).format(p1Month));

        }

    }
}