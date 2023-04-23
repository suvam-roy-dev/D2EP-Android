package com.pwc.d2ep;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
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


public class UpdateOrderActivity extends AppCompatActivity implements View.OnClickListener {

    OrderObject orderObject;
    private MenuItem itemCart;

    private RestClient client1;

    private AutoCompleteTextView etProducts;
    ArrayList<Product> productList;
    ArrayAdapter productAdapter;
    Dialog dialog;
    String token;
    Product selectedProduct;
    ArrayList<CartProduct> cartProducts;
    ArrayList<DocCharge> docCharges, previousCharges;
    ArrayList<ProductCharges> combinedCharges;
    ArrayList<ProductCharges> productCharges;
    double totalCost, subtotal, othercharges = 0.0, otherdisc = 0.0;

    int qtyToAdd = 1;
    double tax = 0.0,discount = 0.0;
    ActivityResultLauncher<Intent> someActivityResultLauncher;


    String account_group = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_order);

        productList = new ArrayList<>();
        docCharges = new ArrayList<>();
        previousCharges = new ArrayList<>();
        combinedCharges = new ArrayList<>();
        productCharges = new ArrayList<>();
        orderObject = (OrderObject) getIntent().getParcelableExtra("order");

        cartProducts = orderObject.products;
        previousCharges = orderObject.docCharges;
        combinedCharges = orderObject.combinedCharges;
        setupSF();
        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.imageButton5).setOnClickListener(this);
        findViewById(R.id.imageButton4).setOnClickListener(this);
        findViewById(R.id.button12).setOnClickListener(this);
        findViewById(R.id.button13).setOnClickListener(this);

        dialog = new Dialog(this);

        dialog.setContentView(R.layout.dialog_product);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);


        setTitle(orderObject.orderName);

        Log.d("2517", "onCreate: "+orderObject.dealerID);

        String[] branch = {orderObject.branch};

        ArrayAdapter<String> branchAdaptr = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,branch);

        ((Spinner)findViewById(R.id.spinner2)).setAdapter(branchAdaptr);

        String[] customer = {orderObject.customer};

        ArrayAdapter<String> customerAdaptr = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,customer);

        ((Spinner)findViewById(R.id.spinner3)).setAdapter(customerAdaptr);

        ((EditText)findViewById(R.id.editTextTextMultiLine)).setText(orderObject.narration);

//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                try {
//
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
//            }
//        });

        etProducts =findViewById(R.id.autoCompleteTextView);

        etProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Toast.makeText(getApplicationContext(),productAdapter.getItem(i).toString(),Toast.LENGTH_SHORT).show();
                showSelectedProduct(productAdapter.getItem(i).toString());
                etProducts.clearFocus();
                etProducts.onEditorAction(EditorInfo.IME_ACTION_DONE);
                findViewById(R.id.cvProductInfo).setVisibility(View.VISIBLE);
            }
        });

//        etProducts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                Toast.makeText(getApplicationContext(),languages[i],Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//
//            }
//        });

        ((TabLayout)findViewById(R.id.tabLayout3)).addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                switch (tab.getText().toString()){
                    case "Customer":
                        findViewById(R.id.cvCustomerTab).setVisibility(View.VISIBLE);
                        findViewById(R.id.cvProductsTab).setVisibility(View.GONE);

                        break;
                    case "Products":

                        findViewById(R.id.cvCustomerTab).setVisibility(View.GONE);
                        findViewById(R.id.cvProductsTab).setVisibility(View.VISIBLE);

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

        findViewById(R.id.button10).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.cvCustomerTab).setVisibility(View.GONE);
                findViewById(R.id.cvProductsTab).setVisibility(View.VISIBLE);
                ((TabLayout)findViewById(R.id.tabLayout3)).getTabAt(1).select();
            }
        });

        findViewById(R.id.button15).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
//                        submitOrder();
//                        submitPatch("");

                        OrderObject orderObject1 =  new OrderObject(orderObject.orderName, orderObject.orderID, orderObject.dealer, orderObject.dealerID, orderObject.branch, orderObject.branchID, orderObject.branch, orderObject.branchID, orderObject.state, ((EditText)findViewById(R.id.editTextTextMultiLine)).getText().toString(), cartProducts, previousCharges,combinedCharges);

                        Intent i = new Intent(getApplicationContext(),CartActivity.class);
                        i.putExtra("order",orderObject1);
                        //startActivity(i);
                        someActivityResultLauncher.launch(i);
                    }
                });

            }
        });

        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();

                            cartProducts = (ArrayList<CartProduct>) data.getSerializableExtra("order");
                            setBadgeCount(getApplicationContext(), itemCart, String.valueOf(cartProducts.size()));
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        if (new TinyDB(this).getBoolean("CloseNewOrder")){
            // TinyDB(this).putBoolean("CloseNewOrder", false);
            finish();
        }
        super.onResume();
    }

    private void showSelectedProduct(String name) {

        selectedProduct = null;
        totalCost = 0;
        othercharges = 0.0;
        otherdisc = 0.0;

        for (int i = 0; i < productList.size(); i++) {
            if (name.equals(productList.get(i).getName())){
                selectedProduct = productList.get(i);
            }
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getCombinedCharges(selectedProduct.cost,selectedProduct.ID, selectedProduct.group.equals("null") ? "null" : "'"+selectedProduct.group+"'", account_group);
                    //getChargeList(selectedProduct.ID);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");
        ((TextView)findViewById(R.id.tvProductNameProductTab)).setText(selectedProduct.name);
        ((TextView)findViewById(R.id.tvUOM)).setText(selectedProduct.uom);
        ((TextView)findViewById(R.id.tvAvailable)).setText((int)selectedProduct.available+"");
        ((TextView)findViewById(R.id.tvAllocated)).setText((int)selectedProduct.allocated+"");
        ((TextView)findViewById(R.id.tvInHand)).setText((int)selectedProduct.inHand+"");
        ((TextView)findViewById(R.id.tvSalesPrice)).setText(selectedProduct.cost+"");

        ((TextView)findViewById(R.id.tvCharges)).setText(round(selectedProduct.cost * tax/100)+"");
        ((TextView)findViewById(R.id.tvDiscount)).setText(round(selectedProduct.cost * discount/100)+"");
        ((TextView)findViewById(R.id.tvTotalCost)).setText("₹ "+round(comupteTotal(qtyToAdd,selectedProduct.cost,tax+othercharges,discount+otherdisc)));
    }

//    void checkUserType() throws UnsupportedEncodingException {
//        String soql = null;
//        try {
//            soql = "SELECT Id,name,account.parentId ,contact.AccountId,account.name FROM User where id='"+client1.getJSONCredentials().getString("userId")+"'";
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getApplicationContext()), soql);
//
//        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
//            @Override
//            public void onSuccess(RestRequest request, final RestResponse result) {
//                result.consumeQuietly(); // consume before going back to main thread
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            //listAdapter.clear();
//                            JSONArray records = result.asJSONObject().getJSONArray("records");
//                            String parentID = records.getJSONObject(0).getJSONObject("Account").getString("ParentId");
//                            Log.d("2513", "parentid: "+parentID);
//                            if(parentID.matches("null")){
//
//
//                                fetchDealerID();
//                            }else{
//
//                                fetchParentID(parentID);
//                            }
//
//                            Log.d("2513", "Check User Type "+result.toString());
////                            fetchBranches();
//                        } catch (Exception ex) {
//                            ex.printStackTrace();
//                        }
//                    }
//                });
//            }
//
//            @Override
//            public void onError(final Exception exception) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
////                        Toast.makeText(getContext(),
////                                "Unable to connect to Salesforce Server!",
////                                Toast.LENGTH_LONG).show();
//                    }
//                });
//            }
//        });
//    }

//    void fetchParentID(String parentID) throws UnsupportedEncodingException {
//        String soql = null;
//        soql = "SELECT Id, Name, AccountGroup__c from Account where Id='"+parentID+"'";
//        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getApplicationContext()), soql);
//
//        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
//            @Override
//            public void onSuccess(RestRequest request, final RestResponse result) {
//                result.consumeQuietly(); // consume before going back to main thread
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            //listAdapter.clear();
//                            JSONArray records = result.asJSONObject().getJSONArray("records");
//                            dealerID = records.getJSONObject(0).getString("Id");
//                            //dealerName = records.getJSONObject(0).getString("Name");
//                            Log.d("2513", "Parent Info: "+result.toString());
//                            //fetchBranches();
//                            fetchAccountGroup();
////                            runOnUiThread(new Runnable() {
////                                @Override
////                                public void run() {
////                                    ((TextView) findViewById(R.id.textView40)).setText("Dealer Name : "+dealerName);
////                                }
////                            });
//                        } catch (Exception ex) {
//                            ex.printStackTrace();
//                        }
//                    }
//                });
//            }
//
//            @Override
//            public void onError(final Exception exception) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
////                        Toast.makeText(getContext(),
////                                "Unable to connect to Salesforce Server!",
////                                Toast.LENGTH_LONG).show();
//                    }
//                });
//            }
//        });
//    }

//    void fetchDealerID() throws UnsupportedEncodingException {
//        String soql = null;
//        try {
//            soql = "Select Id, Contact.Id, Contact.Name, Contact.Account.ID, Contact.Account.Name from User where id='"+client1.getJSONCredentials().getString("userId")+"'";
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getApplicationContext()), soql);
//
//        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
//            @Override
//            public void onSuccess(RestRequest request, final RestResponse result) {
//                result.consumeQuietly(); // consume before going back to main thread
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            //listAdapter.clear();
//                            JSONArray records = result.asJSONObject().getJSONArray("records");
//                            dealerID = records.getJSONObject(0).getJSONObject("Contact").getJSONObject("Account").getString("Id");
//                            //dealerName = records.getJSONObject(0).getJSONObject("Contact").getJSONObject("Account").getString("Name");
//                            Log.d("2513", "run: "+dealerID);
//
////                            runOnUiThread(new Runnable() {
////                                @Override
////                                public void run() {
////                                    ((TextView) findViewById(R.id.textView40)).setText("Dealer Name : "+dealerName);
////                                }
////                            });
//                            fetchAccountGroup();
//                            //fetchBranches();
//                        } catch (Exception ex) {
//                            ex.printStackTrace();
//                        }
//                    }
//                });
//            }
//
//            @Override
//            public void onError(final Exception exception) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
////                        Toast.makeText(getContext(),
////                                "Unable to connect to Salesforce Server!",
////                                Toast.LENGTH_LONG).show();
//                    }
//                });
//            }
//        });
//    }

    void fetchAccountGroup() throws UnsupportedEncodingException {
        String soql = "SELECT ID, AccountGroup__c FROM Account WHERE Id='"+orderObject.dealerID+"'";
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getApplicationContext()), soql);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            JSONArray records = result.asJSONObject().getJSONArray("records");

                            account_group = records.getJSONObject(0).getString("AccountGroup__c");
                            Log.d("Account Group", "Group: "+result.asJSONObject().getJSONArray("records"));
//                            String[] branches = new String[records.length()];
//
//
//                            for (int i = 0; i < records.length(); i++) {
//                                branches[i] = records.getJSONObject(i).getString("Name");
//                                String id =  records.getJSONObject(i).getString("Id");
//                                branchList.add(new Branch(branches[i],id));
//                            }
//                            branchAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_spinner_dropdown_item,branches);
//                            branchSpinner.setAdapter(branchAdapter);
//                            findViewById(R.id.imageView9).setVisibility(View.VISIBLE);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                runOnUiThread(new Runnable() {
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

    private void getChargeList(String Id) throws UnsupportedEncodingException {
        String soql = "select Product__r.name, name,Id,ChargeMaster__c, ChargeMaster__r.Description__c, ChargeMaster__r.ChargeType__c, ChargeMaster__r.Amount__c, ChargeMaster__r.AmountType__c from ProductChargeConfiguration__c where Dealer__c='"+orderObject.dealerID+"' and (Product__r.name= null or Product__c='"+Id+"') and Active__c = true AND IsValid__c = true";
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

        docCharges.clear();
        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            Log.d("2516 Checked for ID: "+Id, "Result :" + result.toString());

                            JSONArray record = result.asJSONObject().getJSONArray("records");
                            for (int i = 0; i < record.length(); i++) {
                                String Id =  record.getJSONObject(i).getString("Id");
                                String chargeID = record.getJSONObject(i).getString("ChargeMaster__c");
                                JSONObject charge = record.getJSONObject(i).getJSONObject("ChargeMaster__r");
                                String chargeType = charge.getString("ChargeType__c");
                                String amountType = charge.getString("AmountType__c");
                                DocCharge d = new DocCharge(charge.getString("Description__c"),Id,chargeID,true,charge.getDouble("Amount__c"), amountType.matches("Floating"),chargeType.matches("Tax"));
                                docCharges.add(d);
                                Log.d("2516 Added doc_charge:", "run: "+charge.toString());

                                if (chargeType.matches("Tax")) {
                                    othercharges += charge.getDouble("Amount__c");
                                }else otherdisc += charge.getDouble("Amount__c");
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView)findViewById(R.id.tvTotalCost)).setText("₹ "+String.valueOf(round(comupteTotal(qtyToAdd,selectedProduct.cost,othercharges,otherdisc))));
                                    ((TextView)findViewById(R.id.tvCharges)).setText(round(othercharges)+"");
                                    ((TextView)findViewById(R.id.tvDiscount)).setText(round(otherdisc)+"");

                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                runOnUiThread(new Runnable() {
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

    private void getCombinedCharges(Double cost,String productId,String productGroup, String accountGroup) throws UnsupportedEncodingException {
        String soql = "SELECT Name, Id, ChargeMaster__c, Product__c, ProductGroup__r.name, ChargeMaster__r.Amount__c, ChargeMaster__r.ChargeType__c, ChargeMaster__r.Description__c, ChargeMaster__r.AmountType__c FROM ProductChargeConfiguration__c WHERE Active__c = True AND IsValid__c = True AND (Level__c = 'DEALER' OR Level__c = 'OEM') AND ( ((DocumentType__c ='SALES ORDER' OR DocumentType__c =NULL) AND ACCOUNTGroup__c = NULL) OR ((DocumentType__c ='SALES ORDER' OR DocumentType__c =NULL) AND ACCOUNTGroup__c ='"+accountGroup+"')) And (Product__c=null OR Product__c in('"+productId+"')) AND (ProductGroup__c=null or ProductGroup__c in("+productGroup+"))";
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

        Log.d("CombinedCharges Query", soql);
        docCharges.clear();
        productCharges.clear();

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            Log.d("CombinedCharges", "Result :" + result.toString());

                            String chargeIDs = "(";
                            JSONArray record = result.asJSONObject().getJSONArray("records");
                            for (int i = 0; i < record.length(); i++) {
                                String Id =  record.getJSONObject(i).getString("Id");
                                String chargeID = record.getJSONObject(i).getString("ChargeMaster__c");

                                JSONObject charge = record.getJSONObject(i).getJSONObject("ChargeMaster__r");
                                String chargeType = charge.getString("ChargeType__c");
                                String amountType = charge.getString("AmountType__c");

                                double amount = amountType.equals("Fixed") ? charge.getDouble("Amount__c") : cost* (charge.getDouble("Amount__c")/100);

//                                DocCharge d = new DocCharge(charge.getString("Description__c"),Id,chargeID,true,amount, amountType.matches("Floating"),chargeType.matches("Tax"));
//                                docCharges.add(d);
//                                Log.d("2516 Added doc_charge:", "run: "+charge.toString());
//
//                                if (chargeType.matches("Tax")) {
//                                    othercharges += amount;
//                                }else otherdisc += amount;
                                chargeIDs += "'"+chargeID+"',";
                            }

                            getFilteredCharges(chargeIDs,cost);
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    ((TextView)findViewById(R.id.tvTotalCost)).setText("₹ "+String.valueOf(round(comupteTotal(qtyToAdd,selectedProduct.cost,othercharges,otherdisc),2)));
//                                    ((TextView)findViewById(R.id.tvCharges)).setText(round(othercharges,2)+"");
//                                    ((TextView)findViewById(R.id.tvDiscount)).setText(round(otherdisc,2)+"");
//                                }
//                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                runOnUiThread(new Runnable() {
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

    private void getFilteredCharges(String chargeList, Double cost) throws UnsupportedEncodingException {

        chargeList = chargeList.substring(0, chargeList.length()-1);
        chargeList += ")";

        String soql = "SELECT ID, ChargeMaster__c,ChargeMaster__r.name,ChargeMaster__r.ChargeType__c,ChargeMaster__r.Amount__c, ChargeMaster__r.Description__c, ChargeMaster__r.AmountType__c, Dealer__c,Dealer__r.name, FinancialAccount__c\n" +
                "                           FROM ChargeFinanceConfiguration__c where Dealer__c ='"+orderObject.dealerID+"' AND ChargeMaster__c IN "+chargeList+"";
        //String soql = "SELECT ID, ChargeMaster__c,ChargeMaster__r.name,ChargeMaster__r.ChargeType__c,ChargeMaster__r.Amount__c, Dealer__c,Dealer__r.name, FinancialAccount__c FROM ChargeFinanceConfiguration__c where Dealer__c ='"+dealerID+"' AND ChargeMaster__c IN "+chargeList+"";
//        String soql = " SELECT ID, ChargeMaster__c,ChargeMaster__r.name,ChargeMaster__r.ChargeType__c,ChargeMaster__r.Amount__c, Dealer__c,Dealer__r.name, FinancialAccount__c\n" +
//                "                           FROM ChargeFinanceConfiguration__c where Dealer__c ='0013h0000092PXNAA2' AND ChargeMaster__c IN ('a033h000016zXuxAAE')";

        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

        Log.d("FilteredCharges Query", soql);
        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            Log.d("FilteredCharges", "Result :" + result.toString());
                            JSONArray record = result.asJSONObject().getJSONArray("records");
                            for (int i = 0; i < record.length(); i++) {
                                String Id =  record.getJSONObject(i).getString("Id");
                                String chargeID = record.getJSONObject(i).getString("ChargeMaster__c");
                                String transactionID = record.getJSONObject(i).getString("FinancialAccount__c");

                                JSONObject charge = record.getJSONObject(i).getJSONObject("ChargeMaster__r");
                                String chargeType = charge.getString("ChargeType__c");
                                String amountType = charge.getString("AmountType__c");

                                double amount = amountType.equals("Fixed") ? charge.getDouble("Amount__c") : cost* (charge.getDouble("Amount__c")/100);

                                DocCharge d = new DocCharge(charge.getString("Description__c"),Id,chargeID,true,amount, amountType.matches("Floating"),chargeType.matches("Tax"));
                                docCharges.add(d);

                                ProductCharges p = new ProductCharges(null, selectedProduct.ID,charge.getString("Description__c"),chargeID,amountType,chargeType,transactionID,charge.getDouble("Amount__c"),charge.getDouble("Amount__c"));
                                productCharges.add(p);

                                Log.d("2516 Added doc_charge:", "run: "+charge.toString());

                                if (chargeType.matches("Tax")) {
                                    othercharges += amount;
                                }else otherdisc += amount;
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView)findViewById(R.id.tvTotalCost)).setText("₹ "+String.valueOf(round(comupteTotal(qtyToAdd,selectedProduct.cost,othercharges,otherdisc))));
                                    ((TextView)findViewById(R.id.tvCharges)).setText(round(othercharges)+"");
                                    ((TextView)findViewById(R.id.tvDiscount)).setText(round(otherdisc)+"");
                                }
                            });

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                runOnUiThread(new Runnable() {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_order, menu);
        itemCart = menu.findItem(R.id.itemEditOrder);
        setBadgeCount(this, itemCart, String.valueOf(orderObject.products.size()));
        return super.onCreateOptionsMenu(menu);
    }

    public static void setBadgeCount(Context context, MenuItem itemicon, String count) {
        BadgeDrawable badge;
        LayerDrawable icon = (LayerDrawable) itemicon.getIcon();
        // Reuse drawable if possible
        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
        if (reuse != null && reuse instanceof BadgeDrawable) {
            badge = (BadgeDrawable) reuse;
        } else {
            badge = new BadgeDrawable(context);
        }
        badge.setCount(count);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_badge, badge);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();  return true;
        }
        if (id == R.id.itemEditOrder) {
            OrderObject orderObject1 =  new OrderObject(orderObject.orderName, orderObject.orderID, orderObject.dealer, orderObject.dealerID,orderObject.branch, orderObject.branchID, orderObject.customer, orderObject.customerID, orderObject.state, ((EditText)findViewById(R.id.editTextTextMultiLine)).getText().toString(), cartProducts ,previousCharges,combinedCharges);
            Intent i = new Intent(getApplicationContext(),CartActivity.class);
            i.putExtra("order",orderObject1);
            //startActivity(i);
            someActivityResultLauncher.launch(i);
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
                                            logout(UpdateOrderActivity.this);
                                    return;
                                }
                                // Cache the returned client
                                client1 = client;
                                try {
                                    //checkUserType();
                                    fetchAccountGroup();
                                    fetchProducts(orderObject.branchID);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                JSONObject cred = client1.getJSONCredentials();
                                try {
                                    token = cred.getString("accessToken");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
    }

    void fetchProducts(String id) throws UnsupportedEncodingException {

        String soql = "Select Product_Name__c,Id,Product__r.ProductGroup__c,TotalAvailableQuantity__c, TotalAllocatedQuantity__c, InHandQuantity__c, UOM__c, Product__r.UnitPrice__c, Product__r.Id from INVProductSummary__c where BranchName__c='"+id+"'";
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(getApplicationContext()), soql);
        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            JSONArray records = result.asJSONObject().getJSONArray("records");
                            Log.d("2512", "run: "+result.toString());
                            String[] products = new String[records.length()];

                            for (int i = 0; i < records.length(); i++) {
                                products[i] = records.getJSONObject(i).getString("Product_Name__c");
                                Log.d("2517", "Added "+products[i]+" to list");
                                String uom = records.getJSONObject(i).getString("UOM__c");

                                String ID = records.getJSONObject(i).getJSONObject("Product__r").getString("Id");
                                Double available = records.getJSONObject(i).getDouble("TotalAvailableQuantity__c");
                                Double allocated = records.getJSONObject(i).getDouble("TotalAllocatedQuantity__c");
                                Double inHand = records.getJSONObject(i).getDouble("InHandQuantity__c");
                                Double cost = records.getJSONObject(i).getJSONObject("Product__r").getDouble("UnitPrice__c");
                                String group = records.getJSONObject(i).getJSONObject("Product__r").getString("ProductGroup__c");

                                Product p = new Product(products[i],ID,uom,available,allocated,inHand,cost,group);
                                productList.add(p);
                            }
                            productAdapter = new
                                    ArrayAdapter(getApplicationContext(),android.R.layout.simple_list_item_1,products);

                            etProducts.setAdapter(productAdapter);
                            etProducts.setThreshold(1);
                            Log.d("2517", "run: Should work with: "+products.length+" Products");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                runOnUiThread(new Runnable() {
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


    @Override
    public void onClick(View view) {

        double fixedCharges = 0.0;
        double floatingCharges = 0.0;
        double fixedDisc = 0.0;
        double floatingDisc = 0.0;

        for (int j = 0; j < productCharges.size(); j++) {
            if(productCharges.get(j).chargeType.equals("Tax")){
                if(productCharges.get(j).chargeValueType.equals("Fixed")){
                    fixedCharges += productCharges.get(j).chargeValue;
                } else floatingCharges += productCharges.get(j).chargeValue;
            } else{
                if(productCharges.get(j).chargeValueType.equals("Fixed")){
                    fixedDisc += productCharges.get(j).chargeValue;
                } else floatingDisc += productCharges.get(j).chargeValue;
            }
        }

        switch (view.getId()){
            case R.id.imageButton5:
                qtyToAdd++;
                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");
                ((TextView)findViewById(R.id.tvTotalCost)).setText(""+round(qtyToAdd*(selectedProduct.cost+ (selectedProduct.cost *floatingCharges/100) - (selectedProduct.cost*floatingDisc/100))+ fixedCharges - fixedDisc));
//
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd*selectedProduct.cost)+"");
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd*selectedProduct.cost+docCharges.get(0).amount)+"");
                break;
            case R.id.imageButton4:
                if (qtyToAdd>0) qtyToAdd--;
                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");
                ((TextView)findViewById(R.id.tvTotalCost)).setText(""+round(qtyToAdd*(selectedProduct.cost+ (selectedProduct.cost *floatingCharges/100) - (selectedProduct.cost*floatingDisc/100))+ fixedCharges - fixedDisc));
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd*selectedProduct.cost)+"");
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd*selectedProduct.cost+docCharges.get(0).amount)+"");
                break;
            case R.id.button12:
                CartProduct cartProduct = new CartProduct(selectedProduct.name,selectedProduct.ID,null,selectedProduct.uom,qtyToAdd,selectedProduct.cost,othercharges,otherdisc,qtyToAdd*(selectedProduct.cost+ (selectedProduct.cost *floatingCharges/100) - (selectedProduct.cost*floatingDisc/100))+ fixedCharges - fixedDisc);

                showEditDialog(cartProduct);
                break;
            case R.id.button13:

                for (int i = 0; i < cartProducts.size(); i++) {
                    if(cartProducts.get(i).name.equals(selectedProduct.name)){
                        Toast.makeText(this,"Error! You already have this product in the cart.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                for (int i = 0; i < productCharges.size(); i++) {
                    ProductCharges p = new ProductCharges(productCharges.get(i).id,productCharges.get(i).productId,productCharges.get(i).chargeName,productCharges.get(i).chargeMaster,productCharges.get(i).chargeValueType,productCharges.get(i).chargeType,productCharges.get(i).transactionAccount,productCharges.get(i).chargeValue,productCharges.get(i).amount);
                    combinedCharges.add(p);
                }

                CartProduct cartProduct1 = new CartProduct(selectedProduct.name,selectedProduct.ID,null,selectedProduct.uom,qtyToAdd,selectedProduct.cost,((selectedProduct.cost *floatingCharges/100) +fixedCharges),((selectedProduct.cost*floatingDisc/100) + fixedDisc),qtyToAdd*(selectedProduct.cost+ (selectedProduct.cost *floatingCharges/100) - (selectedProduct.cost*floatingDisc/100))+ fixedCharges - fixedDisc,productCharges);
                cartProducts.add(cartProduct1);
                Toast.makeText(this,"Product Added to cart", Toast.LENGTH_SHORT).show();
                setBadgeCount(this, itemCart, String.valueOf(cartProducts.size()));
                resetProduct();
                break;
        }
    }

    private void showEditDialog(CartProduct selectedProduct) {

        final double[] changedPrice = {selectedProduct.salesPrice};


        double fixedCharges = 0.0;
        double floatingCharges = 0.0;
        double fixedDisc = 0.0;
        double floatingDisc = 0.0;

        for (int j = 0; j < productCharges.size(); j++) {
            if(productCharges.get(j).chargeType.equals("Tax")){
                if(productCharges.get(j).chargeValueType.equals("Fixed")){
                    fixedCharges += productCharges.get(j).chargeValue;
                } else floatingCharges += productCharges.get(j).chargeValue;
            } else{
                if(productCharges.get(j).chargeValueType.equals("Fixed")){
                    fixedDisc += productCharges.get(j).chargeValue;
                } else floatingDisc += productCharges.get(j).chargeValue;
            }
        }

        double finalFloatingCharges = floatingCharges;
        double finalFloatingDisc = floatingDisc;
        double finalFixedCharges = fixedCharges;
        double finalFixedDisc = fixedDisc;
        dialog.findViewById(R.id.imageButton7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (qtyToAdd>1) qtyToAdd--;
                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*changedPrice[0]+"");
//                ((TextView)dialog.findViewById(R.id.textView71)).setText(qtyToAdd*changedCharge[0]+"");
//                ((TextView)dialog.findViewById(R.id.textView72)).setText(qtyToAdd*changedDisc[0]+"");

                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*(changedPrice[0]+ (changedPrice[0]*finalFloatingCharges/100) - (changedPrice[0]*finalFloatingDisc/100))+ finalFixedCharges - finalFixedDisc+"");

            }
        });
        dialog.findViewById(R.id.imageButton8).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qtyToAdd++;
                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*changedPrice[0]+"");
//                ((TextView)dialog.findViewById(R.id.textView71)).setText(qtyToAdd*changedCharge[0]+"");
//                ((TextView)dialog.findViewById(R.id.textView72)).setText(qtyToAdd*changedDisc[0]+"");
                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*(changedPrice[0]+ (changedPrice[0]*finalFloatingCharges/100) - (changedPrice[0]*finalFloatingDisc/100))+ finalFixedCharges - finalFixedDisc+"");

            }
        });


        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.UOM);
        ((EditText)dialog.findViewById(R.id.editText)).setText(round(selectedProduct.salesPrice)+"");
        ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd*selectedProduct.salesPrice)+"");

        dialog.findViewById(R.id.textView63).setVisibility(View.GONE);
        dialog.findViewById(R.id.editText3).setVisibility(View.GONE);
        dialog.findViewById(R.id.textView72).setVisibility(View.GONE);

        dialog.findViewById(R.id.textView64).setVisibility(View.GONE);
        dialog.findViewById(R.id.editText4).setVisibility(View.GONE);
        dialog.findViewById(R.id.textView73).setVisibility(View.GONE);

        dialog.findViewById(R.id.textView65).setVisibility(View.GONE);
        dialog.findViewById(R.id.editText5).setVisibility(View.GONE);
        dialog.findViewById(R.id.textView74).setVisibility(View.GONE);

        dialog.findViewById(R.id.textView69).setVisibility(View.GONE);
        dialog.findViewById(R.id.editText8).setVisibility(View.GONE);
        dialog.findViewById(R.id.textView75).setVisibility(View.GONE);

        double  docTotal = 0.0;
//        for (int i = 0; i < docCharges.size(); i++) {
//            docTotal += docCharges.get(i).amount;
//            switch (i){
//                case 0:
//                    ((TextView)dialog.findViewById(R.id.textView55)).setText(docCharges.get(i).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText2)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText2)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView)dialog.findViewById(R.id.textView71)).setText(qtyToAdd*docCharges.get(i).amount+"");
//                    break;
//                case 1:
//                    ((TextView)dialog.findViewById(R.id.textView63)).setText(docCharges.get(1).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText3)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText3)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView)dialog.findViewById(R.id.textView72)).setText(qtyToAdd*docCharges.get(1).amount+"");
//
//                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
//                    break;
//
//                case 2:
//                    ((TextView) dialog.findViewById(R.id.textView64)).setText(docCharges.get(2).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText4)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText4)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView) dialog.findViewById(R.id.textView73)).setText(qtyToAdd*docCharges.get(2).amount + "");
//
//                    dialog.findViewById(R.id.textView64).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText4).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView73).setVisibility(View.VISIBLE);
//                    break;
//                case 3:
//                    ((TextView) dialog.findViewById(R.id.textView65)).setText(docCharges.get(3).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText5)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText5)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView) dialog.findViewById(R.id.textView74)).setText(qtyToAdd*docCharges.get(3).amount + "");
//
//                    dialog.findViewById(R.id.textView65).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText5).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView74).setVisibility(View.VISIBLE);
//                    break;
//                case 4:
//                    ((TextView) dialog.findViewById(R.id.textView69)).setText(docCharges.get(4).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText8)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText8)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView) dialog.findViewById(R.id.textView75)).setText(qtyToAdd*docCharges.get(4).amount + "");
//
//                    dialog.findViewById(R.id.textView69).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText8).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView75).setVisibility(View.VISIBLE);
//                    break;
//            }
//        }
        for (int i = 0; i < productCharges.size(); i++) {
            //docTotal += selectedProduct.charges.get(i).amount;
            switch (i){
                case 0:
                    ((TextView)dialog.findViewById(R.id.textView55)).setText(productCharges.get(i).chargeName+ (productCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText)dialog.findViewById(R.id.editText2)).setText(round(productCharges.get(i).chargeValue)+"");
                    double total = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                    ((TextView)dialog.findViewById(R.id.textView71)).setText(round(total)+"");
                    break;
                case 1:
                    ((TextView)dialog.findViewById(R.id.textView63)).setText(productCharges.get(i).chargeName+ (productCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText)dialog.findViewById(R.id.editText3)).setText(round(productCharges.get(i).chargeValue)+"");
                    double total1 = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                    ((TextView)dialog.findViewById(R.id.textView72)).setText(round(total1)+"");

                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
                    break;

                case 2:
                    ((TextView) dialog.findViewById(R.id.textView64)).setText(productCharges.get(i).chargeName+ (productCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText4)).setText(round(productCharges.get(i).chargeValue)+"");
                    double total2 = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                    ((TextView) dialog.findViewById(R.id.textView73)).setText(round(total2) + "");

                    dialog.findViewById(R.id.textView64).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText4).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView73).setVisibility(View.VISIBLE);
                    break;
                case 3:
                    ((TextView) dialog.findViewById(R.id.textView65)).setText(productCharges.get(i).chargeName+ (productCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText5)).setText(round(productCharges.get(i).chargeValue)+"");
                    double total3 = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;

                    ((TextView) dialog.findViewById(R.id.textView74)).setText(round(total3) + "");
                    dialog.findViewById(R.id.textView65).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText5).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView74).setVisibility(View.VISIBLE);
                    break;
                case 4:
                    ((TextView) dialog.findViewById(R.id.textView69)).setText(productCharges.get(i).chargeName+ (productCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText8)).setText(round(productCharges.get(i).chargeValue)+"");
                    double total4 = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;

                    ((TextView) dialog.findViewById(R.id.textView75)).setText(round(total4) + "");

                    dialog.findViewById(R.id.textView69).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText8).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView75).setVisibility(View.VISIBLE);
                    break;
            }
        }
        double finalDocTotal = docTotal;

        dialog.findViewById(R.id.button16).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.button14).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



//                double charges = 0.0;
//                double disc = 0.0;
////                NewOrderActivity.this.selectedProduct.cost = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText)).getText().toString());
//
//                if(docCharges.get(0).isFloating){
//                    docCharges.get(0).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString()) * changedPrice[0]/100;
//                }else {
//                    docCharges.get(0).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());
//                }
//                    if (docCharges.get(0).isTax) {
//                        charges += docCharges.get(0).amount;
//                    } else disc += docCharges.get(0).amount;
//
//
//
//                if(docCharges.size()>1) {
//                    if(docCharges.get(1).isFloating){
//                        docCharges.get(1).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString()) * changedPrice[0]/100;
//                    }else {
//                        docCharges.get(1).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());
//                    }
//                   // docCharges.get(i).
//                    if(docCharges.get(1).isTax) {
//                        charges += docCharges.get(1).amount;
//                    }else disc += docCharges.get(1).amount;
//                }
//
//                if(docCharges.size()>2) {
//                    if(docCharges.get(2).isFloating){
//                        docCharges.get(2).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText4)).getText().toString()) * changedPrice[0]/100;
//                    }else {
//                        docCharges.get(2).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText4)).getText().toString());
//                    }
//                    // docCharges.get(i).
//                    if(docCharges.get(2).isTax) {
//                        charges += docCharges.get(2).amount;
//                    }else disc += docCharges.get(2).amount;
//                }
//
//                if(docCharges.size()>3) {
//                    if(docCharges.get(3).isFloating){
//                        docCharges.get(3).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText5)).getText().toString()) * changedPrice[0]/100;
//                    }else {
//                        docCharges.get(3).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText5)).getText().toString());
//                    }
//                    // docCharges.get(i).
//                    if(docCharges.get(3).isTax) {
//                        charges += docCharges.get(3).amount;
//                    }else disc += docCharges.get(3).amount;
//                }
//
//                selectedProduct.salesPrice = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText)).getText().toString());
//                selectedProduct.tax = charges;
//                selectedProduct.discount = disc;
//                for (int i = 0; i < cartProducts.size(); i++) {
//                    if (selectedProduct.name.matches(cartProducts.get(i).name)) {
//                        //cartProducts.remove(i);
//                        cartProducts.set(i, selectedProduct);
//                    }
//                }

                double cost = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText)).getText().toString());
//                double charge = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());
//                double disc = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());


                double fixedCharge = 0;
                double floatingCharge = 0;
                double fixedDiscount = 0;
                double floatingDiscount = 0;


                for (int i = 0; i < productCharges.size(); i++) {
                    switch (i){
                        case 0:
                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());

                            if (productCharges.get(i).chargeType.equals("Tax")){
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            } else{
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedDiscount;
                                } else {
                                    floatingDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            }

                            break;
                        case 1:
                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());
                            if (productCharges.get(i).chargeType.equals("Tax")){
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            } else{
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedDiscount;
                                } else {
                                    floatingDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            }
                            break;
                        case 2:
                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText4)).getText().toString());
                            if (productCharges.get(i).chargeType.equals("Tax")){
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            } else{
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedDiscount;
                                } else {
                                    floatingDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            }
                            break;
                        case 3:
                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText5)).getText().toString());
                            if (productCharges.get(i).chargeType.equals("Tax")){
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            } else{
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedDiscount;
                                } else {
                                    floatingDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            }
                            break;
                        case 4:
                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText8)).getText().toString());
                            if (productCharges.get(i).chargeType.equals("Tax")){
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            } else{
                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
                                    fixedDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = fixedDiscount;
                                } else {
                                    floatingDiscount += productCharges.get(i).chargeValue;
                                    productCharges.get(i).amount = qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                                }
                            }
                            break;
                    }
                }

                selectedProduct.charges = productCharges;
                selectedProduct.salesPrice = cost;
                selectedProduct.qty = qtyToAdd;
                selectedProduct.tax = fixedCharge + floatingCharge;
                selectedProduct.discount = fixedDiscount + floatingDiscount;
                selectedProduct.total = qtyToAdd*(cost+ (cost*floatingCharge/100) - (cost*floatingDiscount/100))+ fixedCharge - fixedDiscount;

                for (int i = 0; i < cartProducts.size(); i++) {
                    if (selectedProduct.name.matches(cartProducts.get(i).name)) {
                        //cartProducts.remove(i);
                        cartProducts.set(i, selectedProduct);
                        break;
                    }
                }

                UpdateOrderActivity.this.selectedProduct.cost = selectedProduct.salesPrice;
                othercharges = selectedProduct.tax;
                otherdisc = selectedProduct.discount;


                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");

                ((TextView)findViewById(R.id.tvSalesPrice)).setText(round(qtyToAdd*selectedProduct.salesPrice)+"");
//                updateProducts();
//                setCharges();
                ((TextView)findViewById(R.id.tvCharges)).setText(round(fixedCharge+qtyToAdd*(selectedProduct.salesPrice*floatingCharge/100))+"");
                ((TextView)findViewById(R.id.tvDiscount)).setText(round(fixedDiscount+qtyToAdd*(selectedProduct.salesPrice*floatingDiscount/100))+"");

                ((TextView)findViewById(R.id.tvTotalCost)).setText(round(selectedProduct.total)+"");

                dialog.dismiss();
            }
        });

//        ((EditText) dialog.findViewById(R.id.editText)).addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
//                    ((TextView) dialog.findViewById(R.id.textView52)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
//                    selectedProduct.salesPrice = Double.parseDouble(charSequence.toString());
//                    selectedProduct.total = (selectedProduct.qty * Double.parseDouble(charSequence.toString())) + selectedProduct.tax - selectedProduct.discount+finalDocTotal;
//                    changedPrice[0] = Double.parseDouble(charSequence.toString());
//                }
//
//                ((TextView) dialog.findViewById(R.id.textView76)).setText((changedCharge[0]+changedPrice[0])*qtyToAdd+"");
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//
//            }
//        });
//
//        ((EditText) dialog.findViewById(R.id.editText2)).addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
//                    ((TextView) dialog.findViewById(R.id.textView71)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
//                    boolean isTax = docCharges.get(0).isApplied;
//                    if(isTax) {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
//                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//
//                        //docCharges.get(0).amount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                        changedCharge[0] = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//                    else {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
//                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                        docCharges.get(0).amount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//
//
//
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText((changedCharge[0]+changedPrice[0])*qtyToAdd+"");
//
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//
//            }
//        });
//
//        ((EditText) dialog.findViewById(R.id.editText3)).addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
//                    ((TextView) dialog.findViewById(R.id.textView72)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
//                    boolean isTax = docCharges.get(1).isApplied;
//                    if(isTax) {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
//                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//                    else {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
//                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText((changedCharge[0]+changedPrice[0])*qtyToAdd+"");
//
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//
//            }
//        });
//
//        ((EditText) dialog.findViewById(R.id.editText4)).addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
//                    ((TextView) dialog.findViewById(R.id.textView73)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
//                    boolean isTax = docCharges.get(2).isApplied;
//                    if(isTax) {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
//                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//                    else {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
//                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
//
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//
//            }
//        });
//
//        ((EditText) dialog.findViewById(R.id.editText5)).addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
//                    ((TextView) dialog.findViewById(R.id.textView74)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
//                    boolean isTax = docCharges.get(3).isApplied;
//                    if(isTax) {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
//                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//                    else {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
//                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
//
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//
//            }
//        });
//
//        ((EditText) dialog.findViewById(R.id.editText8)).addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
//                    ((TextView) dialog.findViewById(R.id.textView75)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
//                    boolean isTax = docCharges.get(4).isApplied;
//                    if(isTax) {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
//                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//                    else {
//                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
//                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
//                    }
//
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
//
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//
//            }
//        });

        ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd*(changedPrice[0]+ (changedPrice[0]*finalFloatingCharges/100) - (changedPrice[0]*finalFloatingDisc/100))+ finalFixedCharges - finalFixedDisc)+"");


        dialog.show();
    }

//    private void showEditDialog(CartProduct selectedProduct) {
//
//        final double[] changedPrice = {selectedProduct.salesPrice};
//        final double[] changedCharge = {selectedProduct.tax};
//        final double[] changedDisc = {selectedProduct.discount};
//
//
//        dialog.findViewById(R.id.imageButton7).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (qtyToAdd>1) qtyToAdd--;
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
////                ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*changedPrice[0]+"");
////                ((TextView)dialog.findViewById(R.id.textView71)).setText(qtyToAdd*changedCharge[0]+"");
////                ((TextView)dialog.findViewById(R.id.textView72)).setText(qtyToAdd*changedDisc[0]+"");
//
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*(changedPrice[0]+changedCharge[0]-changedDisc[0])+"");
//
//            }
//        });
//        dialog.findViewById(R.id.imageButton8).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                qtyToAdd++;
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
////                ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*changedPrice[0]+"");
////                ((TextView)dialog.findViewById(R.id.textView71)).setText(qtyToAdd*changedCharge[0]+"");
////                ((TextView)dialog.findViewById(R.id.textView72)).setText(qtyToAdd*changedDisc[0]+"");
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*(changedPrice[0]+changedCharge[0]-changedDisc[0])+"");
//
//            }
//        });
//
//
//        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
//        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
//        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.UOM);
//        ((EditText)dialog.findViewById(R.id.editText)).setText(selectedProduct.salesPrice+"");
//        ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*selectedProduct.salesPrice+"");
//
//        dialog.findViewById(R.id.textView63).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText3).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView72).setVisibility(View.GONE);
//
//        dialog.findViewById(R.id.textView64).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText4).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView73).setVisibility(View.GONE);
//
//        dialog.findViewById(R.id.textView65).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText5).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView74).setVisibility(View.GONE);
//
//        dialog.findViewById(R.id.textView69).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText8).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView75).setVisibility(View.GONE);
//
//        double  docTotal = 0.0;
//        for (int i = 0; i < docCharges.size(); i++) {
//            docTotal += docCharges.get(i).amount;
//            switch (i){
//                case 0:
//                    ((TextView)dialog.findViewById(R.id.textView55)).setText(docCharges.get(i).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText2)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText2)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView)dialog.findViewById(R.id.textView71)).setText(qtyToAdd*docCharges.get(i).amount+"");
//                    break;
//                case 1:
//                    ((TextView)dialog.findViewById(R.id.textView63)).setText(docCharges.get(1).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText3)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText3)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView)dialog.findViewById(R.id.textView72)).setText(qtyToAdd*docCharges.get(1).amount+"");
//
//                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
//                    break;
//
//                case 2:
//                    ((TextView) dialog.findViewById(R.id.textView64)).setText(docCharges.get(2).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText4)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText4)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView) dialog.findViewById(R.id.textView73)).setText(qtyToAdd*docCharges.get(2).amount + "");
//
//                    dialog.findViewById(R.id.textView64).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText4).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView73).setVisibility(View.VISIBLE);
//                    break;
//                case 3:
//                    ((TextView) dialog.findViewById(R.id.textView65)).setText(docCharges.get(3).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText5)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText5)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView) dialog.findViewById(R.id.textView74)).setText(qtyToAdd*docCharges.get(3).amount + "");
//
//                    dialog.findViewById(R.id.textView65).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText5).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView74).setVisibility(View.VISIBLE);
//                    break;
//                case 4:
//                    ((TextView) dialog.findViewById(R.id.textView69)).setText(docCharges.get(4).name);
//                    if(docCharges.get(i).isFloating){
//                        ((EditText) dialog.findViewById(R.id.editText8)).setText((docCharges.get(i).amount*100)/changedPrice[0] + "");
//                    }else {
//                        ((EditText) dialog.findViewById(R.id.editText8)).setText(docCharges.get(i).amount + "");
//                    }
//                    ((TextView) dialog.findViewById(R.id.textView75)).setText(qtyToAdd*docCharges.get(4).amount + "");
//
//                    dialog.findViewById(R.id.textView69).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText8).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView75).setVisibility(View.VISIBLE);
//                    break;
//            }
//        }
//
//        double finalDocTotal = docTotal;
//
//        dialog.findViewById(R.id.button16).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                dialog.dismiss();
//            }
//        });
//
//        dialog.findViewById(R.id.button14).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//
//
//                double charges = 0.0;
//                double disc = 0.0;
////                NewOrderActivity.this.selectedProduct.cost = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText)).getText().toString());
//
//                if(docCharges.get(0).isFloating){
//                    docCharges.get(0).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString()) * changedPrice[0]/100;
//                }else {
//                    docCharges.get(0).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());
//                }
//                if (docCharges.get(0).isTax) {
//                    charges += docCharges.get(0).amount;
//                } else disc += docCharges.get(0).amount;
//
//
//
//                if(docCharges.size()>1) {
//                    if(docCharges.get(1).isFloating){
//                        docCharges.get(1).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString()) * changedPrice[0]/100;
//                    }else {
//                        docCharges.get(1).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());
//                    }
//                    // docCharges.get(i).
//                    if(docCharges.get(1).isTax) {
//                        charges += docCharges.get(1).amount;
//                    }else disc += docCharges.get(1).amount;
//                }
//
//                if(docCharges.size()>2) {
//                    if(docCharges.get(2).isFloating){
//                        docCharges.get(2).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText4)).getText().toString()) * changedPrice[0]/100;
//                    }else {
//                        docCharges.get(2).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText4)).getText().toString());
//                    }
//                    // docCharges.get(i).
//                    if(docCharges.get(2).isTax) {
//                        charges += docCharges.get(2).amount;
//                    }else disc += docCharges.get(2).amount;
//                }
//
//                if(docCharges.size()>3) {
//                    if(docCharges.get(3).isFloating){
//                        docCharges.get(3).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText5)).getText().toString()) * changedPrice[0]/100;
//                    }else {
//                        docCharges.get(3).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText5)).getText().toString());
//                    }
//                    // docCharges.get(i).
//                    if(docCharges.get(3).isTax) {
//                        charges += docCharges.get(3).amount;
//                    }else disc += docCharges.get(3).amount;
//                }
//
//                selectedProduct.salesPrice = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText)).getText().toString());
//                selectedProduct.tax = charges;
//                selectedProduct.discount = disc;
//                for (int i = 0; i < cartProducts.size(); i++) {
//                    if (selectedProduct.name.matches(cartProducts.get(i).name)) {
//                        //cartProducts.remove(i);
//                        cartProducts.set(i, selectedProduct);
//                    }
//                }
//
//                UpdateOrderActivity.this.selectedProduct.cost = selectedProduct.salesPrice;
//                othercharges = selectedProduct.tax;
//                otherdisc = selectedProduct.discount;
//
//
//                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");
//
//                ((TextView)findViewById(R.id.tvSalesPrice)).setText(selectedProduct.salesPrice+"");
////                updateProducts();
////                setCharges();
//                ((TextView)findViewById(R.id.tvCharges)).setText(charges+"");
//                ((TextView)findViewById(R.id.tvDiscount)).setText(disc+"");
//
//                ((TextView)findViewById(R.id.tvTotalCost)).setText((selectedProduct.salesPrice+charges-disc)*qtyToAdd+"");
//
//                dialog.dismiss();
//            }
//        });
//
////        ((EditText) dialog.findViewById(R.id.editText)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView52)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    selectedProduct.salesPrice = Double.parseDouble(charSequence.toString());
////                    selectedProduct.total = (selectedProduct.qty * Double.parseDouble(charSequence.toString())) + selectedProduct.tax - selectedProduct.discount+finalDocTotal;
////                    changedPrice[0] = Double.parseDouble(charSequence.toString());
////                }
////
////                ((TextView) dialog.findViewById(R.id.textView76)).setText((changedCharge[0]+changedPrice[0])*qtyToAdd+"");
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText2)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView71)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(0).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////
////                        //docCharges.get(0).amount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                        changedCharge[0] = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                        docCharges.get(0).amount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText((changedCharge[0]+changedPrice[0])*qtyToAdd+"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText3)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView72)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(1).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText((changedCharge[0]+changedPrice[0])*qtyToAdd+"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText4)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView73)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(2).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText5)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView74)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(3).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText8)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView75)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(4).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
//
//        ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*(selectedProduct.salesPrice+othercharges-otherdisc)+"");
//
//
//        dialog.show();
//    }

//    private void showEditDialog(CartProduct selectedProduct) {
//
//        final double[] changedPrice = {selectedProduct.salesPrice};
//        final double[] changedCharge = {selectedProduct.tax};
//        final double[] changedDisc = {selectedProduct.discount};
//
//        dialog.findViewById(R.id.imageButton7).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (qtyToAdd>1) qtyToAdd--;
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
////                ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*changedPrice[0]+"");
////                ((TextView)dialog.findViewById(R.id.textView71)).setText(qtyToAdd*changedCharge[0]+"");
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*(changedPrice[0]+changedCharge[0]-changedDisc[0])+"");
//
//            }
//        });
//        dialog.findViewById(R.id.imageButton8).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                qtyToAdd++;
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
////                ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*changedPrice[0]+"");
////                ((TextView)dialog.findViewById(R.id.textView71)).setText(qtyToAdd*changedCharge[0]+"");
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*(changedPrice[0]+changedCharge[0]-changedDisc[0])+"");
//
//
//            }
//        });
//
//
//        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
//        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
//        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.UOM);
//        ((EditText)dialog.findViewById(R.id.editText)).setText(selectedProduct.salesPrice+"");
//        ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*selectedProduct.salesPrice+"");
//
//        dialog.findViewById(R.id.textView63).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText3).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView72).setVisibility(View.GONE);
//
//        dialog.findViewById(R.id.textView64).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText4).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView73).setVisibility(View.GONE);
//
//        dialog.findViewById(R.id.textView65).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText5).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView74).setVisibility(View.GONE);
//
//        dialog.findViewById(R.id.textView69).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText8).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView75).setVisibility(View.GONE);
//
//        double  docTotal = 0.0;
//        for (int i = 0; i < docCharges.size(); i++) {
//            docTotal += docCharges.get(i).amount;
//            switch (i){
//                case 0:
//                    ((TextView)dialog.findViewById(R.id.textView55)).setText(docCharges.get(i).name);
//                    ((EditText)dialog.findViewById(R.id.editText2)).setText(docCharges.get(i).amount+"");
//                    ((TextView)dialog.findViewById(R.id.textView71)).setText(qtyToAdd*docCharges.get(i).amount+"");
//                    break;
//                case 1:
//                    ((TextView)dialog.findViewById(R.id.textView63)).setText(docCharges.get(1).name);
//                    ((EditText)dialog.findViewById(R.id.editText3)).setText(docCharges.get(1).amount+"");
//                    ((TextView)dialog.findViewById(R.id.textView72)).setText(qtyToAdd*docCharges.get(1).amount+"");
//
//                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
//                    break;
//
//                case 2:
//                    ((TextView) dialog.findViewById(R.id.textView64)).setText(docCharges.get(2).name);
//                    ((EditText) dialog.findViewById(R.id.editText4)).setText(docCharges.get(2).amount + "");
//                    ((TextView) dialog.findViewById(R.id.textView73)).setText(qtyToAdd*docCharges.get(2).amount + "");
//
//                    dialog.findViewById(R.id.textView64).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText4).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView73).setVisibility(View.VISIBLE);
//                    break;
//                case 3:
//                    ((TextView) dialog.findViewById(R.id.textView65)).setText(docCharges.get(3).name);
//                    ((EditText) dialog.findViewById(R.id.editText5)).setText(docCharges.get(3).amount + "");
//                    ((TextView) dialog.findViewById(R.id.textView74)).setText(qtyToAdd*docCharges.get(3).amount + "");
//
//                    dialog.findViewById(R.id.textView65).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText5).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView74).setVisibility(View.VISIBLE);
//                    break;
//                case 4:
//                    ((TextView) dialog.findViewById(R.id.textView69)).setText(docCharges.get(4).name);
//                    ((EditText) dialog.findViewById(R.id.editText8)).setText(docCharges.get(4).amount + "");
//                    ((TextView) dialog.findViewById(R.id.textView75)).setText(qtyToAdd*docCharges.get(4).amount + "");
//
//                    dialog.findViewById(R.id.textView69).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText8).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView75).setVisibility(View.VISIBLE);
//                    break;
//            }
//        }
//
//        double finalDocTotal = docTotal;
//
//        dialog.findViewById(R.id.button16).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                dialog.dismiss();
//            }
//        });
//
//        dialog.findViewById(R.id.button14).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                double charges = 0.0;
//                double disc = 0.0;
////                NewOrderActivity.this.selectedProduct.cost = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText)).getText().toString());
//
//                docCharges.get(0).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());
//
//                if(docCharges.get(0).isTax) {
//                    charges += docCharges.get(0).amount;
//                }else disc += docCharges.get(0).amount;
//
//                if(docCharges.size()>1) {
//                    docCharges.get(1).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());
//                    if(docCharges.get(1).isTax) {
//                        charges += docCharges.get(1).amount;
//                    }else disc += docCharges.get(1).amount;
//                }
//
//                if(docCharges.size()>2) {
//                    docCharges.get(2).amount = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText4)).getText().toString());
//
//                    // docCharges.get(i).
//                    if(docCharges.get(2).isTax) {
//                        charges += docCharges.get(2).amount;
//                    }else disc += docCharges.get(2).amount;
//                }
//
//                selectedProduct.salesPrice = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText)).getText().toString());
//                selectedProduct.tax = charges;
//                selectedProduct.discount = disc;
//                for (int i = 0; i < cartProducts.size(); i++) {
//                    if (selectedProduct.name.matches(cartProducts.get(i).name)) {
//                        //cartProducts.remove(i);
//                        cartProducts.set(i, selectedProduct);
//                    }
//                }
//
//                UpdateOrderActivity.this.selectedProduct.cost = selectedProduct.salesPrice;
//                othercharges = selectedProduct.tax;
//                otherdisc = selectedProduct.discount;
//
//                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");
//
//                ((TextView)findViewById(R.id.tvSalesPrice)).setText(selectedProduct.salesPrice+"");
////                updateProducts();
////                setCharges();
//                ((TextView)findViewById(R.id.tvCharges)).setText(charges+"");
//                ((TextView)findViewById(R.id.tvDiscount)).setText(disc+"");
//
//
//                ((TextView)findViewById(R.id.tvTotalCost)).setText((selectedProduct.salesPrice+charges-disc)*qtyToAdd+"");
//
//                dialog.dismiss();
//            }
//        });
//
////        ((EditText) dialog.findViewById(R.id.editText)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView52)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    selectedProduct.salesPrice = Double.parseDouble(charSequence.toString());
////                    selectedProduct.total = (selectedProduct.qty * Double.parseDouble(charSequence.toString())) + selectedProduct.tax - selectedProduct.discount+finalDocTotal;
////                    changedPrice[0] = Double.parseDouble(charSequence.toString());
////                }
////
////                ((TextView) dialog.findViewById(R.id.textView76)).setText((changedCharge[0]+changedPrice[0])*qtyToAdd+"");
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText2)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView71)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(0).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////
////                        //docCharges.get(0).amount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                        changedCharge[0] = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                        docCharges.get(0).amount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText((changedCharge[0]+changedPrice[0])*qtyToAdd+"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText3)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView72)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(1).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText((changedCharge[0]+changedPrice[0])*qtyToAdd+"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText4)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView73)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(2).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText5)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView74)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(3).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
////
////        ((EditText) dialog.findViewById(R.id.editText8)).addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView75)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
////                    boolean isTax = docCharges.get(4).isApplied;
////                    if(isTax) {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.qty * Double.parseDouble(charSequence.toString()) - selectedProduct.discount+finalDocTotal;
////                        selectedProduct.tax = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////                    else {
////                        selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) - selectedProduct.qty * Double.parseDouble(charSequence.toString()) + selectedProduct.tax+finalDocTotal;
////                        selectedProduct.discount = selectedProduct.qty * Double.parseDouble(charSequence.toString());
////                    }
////
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////
////            }
////        });
//
//        ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*(selectedProduct.salesPrice+othercharges-otherdisc)+"");
//
//
//        dialog.show();
//    }

//    private void showEditDialog() {
//        dialog.findViewById(R.id.imageButton7).setOnClickListener(this);
//        dialog.findViewById(R.id.imageButton8).setOnClickListener(this);
//
//        dialog.findViewById(R.id.button14).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dialog.dismiss();
//            }
//        });
//
//        dialog.findViewById(R.id.button16).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dialog.dismiss();
//            }
//        });
//
//        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
//        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
//        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.uom);
//        ((EditText)dialog.findViewById(R.id.editText)).setText(round(selectedProduct.cost)+"");
//        ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd*selectedProduct.cost)+"");
//
//        dialog.findViewById(R.id.textView63).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText3).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView72).setVisibility(View.GONE);
//
//        double  docTotal = 0.0;
//        for (int i = 0; i < docCharges.size(); i++) {
//            docTotal += docCharges.get(i).amount;
//            switch (i){
//                case 0:
//                    ((TextView)dialog.findViewById(R.id.textView55)).setText(docCharges.get(i).name);
//                    ((EditText)dialog.findViewById(R.id.editText2)).setText(round(docCharges.get(i).amount)+"");
//                    ((TextView)dialog.findViewById(R.id.textView71)).setText(round(docCharges.get(i).amount)+"");
//                    break;
//                case 1:
//                    ((TextView)dialog.findViewById(R.id.textView63)).setText(docCharges.get(1).name);
//                    ((EditText)dialog.findViewById(R.id.editText3)).setText(round(docCharges.get(1).amount)+"");
//                    ((TextView)dialog.findViewById(R.id.textView72)).setText(round(docCharges.get(1).amount)+"");
//
//                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
//                    break;
//            }
//        }
//        ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd*selectedProduct.cost+docTotal)+"");
//        dialog.show();
//    }

    private void resetProduct() {
        selectedProduct = null;
        qtyToAdd = 1;
        totalCost = 0;
        othercharges = 0.0;
        findViewById(R.id.cvProductInfo).setVisibility(View.INVISIBLE);
        etProducts.setText("");
    }

    private double comupteTotal(int qtyToAdd, double cost, double tax, double discount) {
        double subtotal = qtyToAdd * cost;
        double appTax = tax;
        double appDisc = discount;
        totalCost = (subtotal+appTax-appDisc);
        return (subtotal+appTax-appDisc);
    }

    public static String round(double value) {
//        if (places < 0) throw new IllegalArgumentException();
//
//        BigDecimal bd = BigDecimal.valueOf(value);
//        bd = bd.setScale(places, RoundingMode.HALF_UP);
//        return bd.doubleValue();
        String val = String.valueOf((double) Math.round(value * 100) / 100);

        return ((val.charAt(val.length()-1)) == '0' || (val.charAt(val.length()-2)) == '.') ? val+"0":val;
    }
}
