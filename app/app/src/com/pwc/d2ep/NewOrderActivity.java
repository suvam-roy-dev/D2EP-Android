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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewOrderActivity extends AppCompatActivity implements View.OnClickListener {

    private RestClient client1;
    ArrayAdapter branchAdapter;
    Spinner branchSpinner, customerSpinner;
    String dealerID,branchID, customerID = null;
    String productPayload = "";
    MenuItem itemCart;
    String dealerName, branchName,customerName,state,narration="N/A";
    private AutoCompleteTextView etProducts, etCustomers;
    ArrayList<Branch> branchList;
    ArrayList<Product> productList;
    ArrayList<Customer> customerList;
    ArrayAdapter productAdapter, customerAdapter;
    Dialog dialog;
    String token;
    Product selectedProduct;
    ArrayList<CartProduct> cartProducts;
    ArrayList<DocCharge> docCharges;
    ArrayList<ProductCharges> productCharges;
    ArrayList<ProductCharges> combinedCharges;
    double totalCost, subtotal, othercharges = 0.0, otherdisc = 0.0;
    boolean isBranch;
    int qtyToAdd = 1;
    double tax = 0.0,discount = 0.0;
    private double disc;
    ActivityResultLauncher<Intent> someActivityResultLauncher;

    String account_group = "";
    private String productChargesPayload = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order);

        branchList = new ArrayList<>();
        customerList = new ArrayList<>();
        productList = new ArrayList<>();
        cartProducts = new ArrayList<>();
        docCharges = new ArrayList<>();
        productCharges = new ArrayList<>();
        combinedCharges = new ArrayList<>();
        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg, null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle("Add New Order");
        setupSF();
        dialog = new Dialog(this);

        dialog.setContentView(R.layout.dialog_product);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        findViewById(R.id.imageButton5).setOnClickListener(this);
        findViewById(R.id.imageButton4).setOnClickListener(this);
        findViewById(R.id.button12).setOnClickListener(this);
        findViewById(R.id.button13).setOnClickListener(this);

        branchSpinner = findViewById(R.id.spinner2);
        customerSpinner = findViewById(R.id.spinner3);
        branchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                etCustomers.setText("");
                customerID = null;
                for (int j = 0; j < branchList.size(); j++) {
                    if (adapterView.getSelectedItem().toString().equals(branchList.get(j).name)) {
                        branchID = branchList.get(j).id;
                        branchName = branchList.get(j).name;
                    }
                }
                Log.d("2512", "onItemSelected: " + branchID);
                try {
                    fetchCustomers(branchID);
                    fetchProducts(branchID);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

//        customerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//
//                customerName = adapterView.getSelectedItem().toString();
//                for (int j = 0; j < customerList.size(); j++) {
//                    if (adapterView.getSelectedItem().toString().equals(customerList.get(j).name)) {
//                        customerID = customerList.get(j).id;
//                        customerName = customerList.get(j).name;
//                    }
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//
//            }
//        });

        etProducts = findViewById(R.id.autoCompleteTextView);


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


        etCustomers = findViewById(R.id.autoCompleteTextView2);
        etCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Toast.makeText(getApplicationContext(),productAdapter.getItem(i).toString(),Toast.LENGTH_SHORT).show();
                customerName = customerAdapter.getItem(i).toString();
                for (int j = 0; j < customerList.size(); j++) {
                    if (customerAdapter.getItem(i).toString().equals(customerList.get(j).name)) {
                        customerID = customerList.get(j).id;
                        customerName = customerList.get(j).name;
                    }
                }
                etCustomers.clearFocus();
                etCustomers.onEditorAction(EditorInfo.IME_ACTION_DONE);
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

        ((TabLayout) findViewById(R.id.tabLayout3)).addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                switch (tab.getText().toString()) {
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
                ((TabLayout) findViewById(R.id.tabLayout3)).getTabAt(1).select();
            }
        });

        ((EditText) findViewById(R.id.editTextTextMultiLine)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                narration = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        findViewById(R.id.button11).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (customerID == null){
                    Toast.makeText(getApplicationContext(),"Please select a customer!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(cartProducts.size() == 0){
                    Toast.makeText(getApplicationContext(),"Please add at least 1 product!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getApplicationContext(), "Saving as Draft...", Toast.LENGTH_SHORT).show();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        prepareChargesPayload();
                        prepareProductPayload();
                        submitPatch("Draft");
                    }
                });
            }
        });

        findViewById(R.id.button15).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                OrderObject orderObject = new OrderObject("", "", dealerName, dealerID, branchName, branchID, customerName, customerID, state, narration, cartProducts, null,combinedCharges);

                Intent i = new Intent(getApplicationContext(), CartActivity.class);
                i.putExtra("order", orderObject);

                if (customerID == null){
                    Toast.makeText(getApplicationContext(),"Please select a customer!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (cartProducts.size() > 0) {
                    //startActivity(i);
                    someActivityResultLauncher.launch(i);
                } else
                    Toast.makeText(getApplicationContext(), "Please add at least 1 product.", Toast.LENGTH_SHORT).show();

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
            new TinyDB(this).putBoolean("CloseNewOrder", false);
            new TinyDB(this).putBoolean("RefreshOrders", true);
            finish();
        }
        super.onResume();
    }

    private void submitOrder() {

        String payload = "{\n" +
                "   \n" +
                "    \"salesOrderDocChargeLst\": [\n" +
                "        {\n" +
                "            \"ChargeMaster\": \"a033h000004vLBUAA2\",\n" +
                "            \"ChargeType\": \"Tax\",\n" +
                "            \"Amount\": 4.00,\n" +
                "            \"AmountType\": \"Floating\",\n" +
                "            \"IsApplied\": \"Yes\",\n" +
                "            \"Description\": \"Core  Dia (in cm)\",\n" +
                "            \"Value\": 500.00\n" +
                "        },\n" +
                "        {\n" +
                "            \"ChargeMaster\": \"a033h000004vLBVAA2\",\n" +
                "            \"ChargeType\": \"Discount\",\n" +
                "            \"Amount\": 3.00,\n" +
                "            \"AmountType\": \"Fixed\",\n" +
                "            \"IsApplied\": \"Yes\",\n" +
                "            \"Description\": \"Employee Discount\",\n" +
                "            \"Value\": 3.00\n" +
                "        },\n" +
                "        {\n" +
                "            \"ChargeMaster\": \"a033h000004vLBQAA2\",\n" +
                "            \"ChargeType\": \"Discount\",\n" +
                "            \"Amount\": 1.00,\n" +
                "            \"AmountType\": \"Floating\",\n" +
                "            \"IsApplied\": \"Yes\",\n" +
                "            \"Description\": \"Additional Discount\",\n" +
                "            \"Value\": 30.00\n" +
                "        },\n" +
                "        {\n" +
                "            \"ChargeMaster\": \"a033h000003j26jAAA\",\n" +
                "            \"ChargeType\": \"Tax\",\n" +
                "            \"Amount\": 150.00,\n" +
                "            \"AmountType\": \"Fixed\",\n" +
                "            \"IsApplied\": \"Yes\",\n" +
                "            \"Description\": \"Packing Type\",\n" +
                "            \"Value\": 150.00\n" +
                "        },\n" +
                "        {\n" +
                "            \"ChargeMaster\": \"a033h000003j188AAA\",\n" +
                "            \"ChargeType\": \"Tax\",\n" +
                "            \"Amount\": 8.00,\n" +
                "            \"AmountType\": \"Floating\",\n" +
                "            \"IsApplied\": \"Yes\",\n" +
                "            \"Description\": \"Size/Width (in cm)\",\n" +
                "            \"Value\": 240.00\n" +
                "        }\n" +
                "    ],\n" +
                "    \"salesOrderDetailLst\": [\n" +
                "        {\n" +
                "            \"productId\": \"01t3h0000019DJ0AAM\",\n" +
                "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
                "            \"quantity\": 10.00,\n" +
                "            \"status\": \"Open\",\n" +
                "            \"allocatedQuantity\": 0.00,\n" +
                "            \"salesPrice\": 300.00,\n" +
                "            \"discount\": 120.00,\n" +
                "            \"tax\": 1150.00\n" +
                "        },\n" +
                "\t{\n" +
                "            \"productId\": \"01t3h0000019DJ0AAM\",\n" +
                "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
                "            \"quantity\": 20.00,\n" +
                "            \"status\": \"Open\",\n" +
                "            \"allocatedQuantity\": 0.00,\n" +
                "            \"salesPrice\": 300.00,\n" +
                "            \"discount\": 120.00,\n" +
                "            \"tax\": 1150.00\n" +
                "        }\n" +
                "    ],\n" +
                " \"salesOrder\": \n" +
                "        {\n" +
                "            \"State\": \"Draft\",\n" +
                "            \"DealerId\": \""+dealerID+"\",\n" +
                "            \"Narration\": \""+narration+"\",\n" +
                "            \"BranchId\": \""+branchID+"\",\n" +
                "            \"CustomerId\": \""+customerID+"\",\n" +
                "            \"TotalCost\": "+totalCost+",\n" +
                "            \"Subtotal\": "+subtotal+",\n" +
                "            \"Tax\": "+tax+",\n" +
                "            \"Discount\": "+discount+",\n" +
                "            \"AdditionalChargeApplied\": 510.00,\n" +
                "            \"AdditionalDiscountApplied\": 33.00\n" +
                "        }\n" +
                "}";

        final MediaType JSON
                = MediaType.get("application/json; charset=utf-8");

        OkHttpClient client = new OkHttpClient();

            RequestBody body = RequestBody.create(payload, JSON);
            Request request = new Request.Builder()
                    .url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/")
                    .addHeader("Authorization","Bearer "+token)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                Log.d("2515", response.body().string());
                Log.d("2515", response.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
    }



//    private void submitPatch(String orderID) {
//        String payload = "{\"SalesOrderDocChargePatch\": [\n" +
//                "        {\n" +
//                "\"Id\": \"a1M3h00000FtcTsEAJ\",\n" +
//                "            \"chargeMasterId\": \"a033h000004vLBUAA2\",\n" +
//                "            \"chargeType\": \"Tax\",\n" +
//                "            \"amount\": 100.00,\n" +
//                "            \"amountType\": \"Floating\",\n" +
//                "            \"isApplied\": \"Yes\",\n" +
//                "            \"description\": \"Core  Dia (in cm)\",\n" +
//                "            \"value\": 500.00\n" +
//                "        },\n" +
//                "        {\n" +
//                "\"Id\": \"a1M3h00000FtcTtEAJ\",\n" +
//                "            \"chargeMasterId\": \"a033h000004vLBVAA2\",\n" +
//                "            \"chargeType\": \"Discount\",\n" +
//                "            \"amount\": 30.00,\n" +
//                "            \"amountType\": \"Fixed\",\n" +
//                "            \"isApplied\": \"Yes\",\n" +
//                "            \"description\": \"Employee Discount\",\n" +
//                "            \"value\": 3.00\n" +
//                "        },\n" +
//                "        {\n" +
//                "\"Id\": \"a1M3h00000FtcTuEAJ\",\n" +
//                "            \"chargeMasterId\": \"a033h000004vLBQAA2\",\n" +
//                "            \"chargeType\": \"Discount\",\n" +
//                "            \"amount\": 1.00,\n" +
//                "            \"amountType\": \"Floating\",\n" +
//                "            \"isApplied\": \"Yes\",\n" +
//                "            \"description\": \"Additional Discount\",\n" +
//                "            \"value\": 30.00\n" +
//                "        },\n" +
//                "        {\n" +
//                "\"Id\": \"a1M3h00000FtcTvEAJ\",\n" +
//                "            \"chargeMasterId\": \"a033h000003j26jAAA\",\n" +
//                "            \"chargeType\": \"Tax\",\n" +
//                "            \"amount\": 150.00,\n" +
//                "            \"amountType\": \"Fixed\",\n" +
//                "            \"isApplied\": \"Yes\",\n" +
//                "            \"description\": \"Packing Type\",\n" +
//                "            \"value\": 150.00\n" +
//                "        },\n" +
//                "        {\n" +
//                "\"Id\": \"a1M3h00000FtcTwEAJ\",\n" +
//                "            \"chargeMasterId\": \"a033h000003j188AAA\",\n" +
//                "            \"chargeType\": \"Tax\",\n" +
//                "            \"amount\": 8.00,\n" +
//                "            \"amountType\": \"Floating\",\n" +
//                "            \"isApplied\": \"Yes\",\n" +
//                "            \"description\": \"Size/Width (in cm)\",\n" +
//                "            \"value\": 240.00\n" +
//                "        }\n" +
//                "    ],\n" +
//                "    \"SalesOrderDetailPatch\": [\n" +
//                "        {\n" +
//                "\"Id\": \"a1L3h00000BRTYcEAP\",\n" +
//                "            \"productId\": \"01t3h0000019DJ0AAM\",\n" +
//                "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
//                "            \"quantity\": 10.00,\n" +
//                "            \"status\": \"Open\",\n" +
//                "            \"allocatedQuantity\": 0.00,\n" +
//                "            \"salesPrice\": 300.00,\n" +
//                "            \"discount\": 120.00,\n" +
//                "            \"tax\": 1150.00\n" +
//                "        }\n" +
//                "    ],\n" +
//                " \"SalesOrderPatch\":\n" +
//                "        {\n" +
//                "\"Id\": \"a1K3h00000495aYEAQ\",\n" +
//                "            \"state\": \"Draft\",\n" +
//                "            \"dealerId\": \"0013h0000092PXNAA2\",\n" +
//                "            \"narration\": \"abc narration \",\n" +
//                "            \"branchId\": \"0013h0000092PY5AAM\",\n" +
//                "            \"customerId\": \"0033h00000OZJG2AAP\",\n" +
//                "            \"totalCost\": 4007.00,\n" +
//                "            \"subtotal\": 3000.00,\n" +
//                "            \"tax\": 1150.00,\n" +
//                "            \"discount\": 120.00,\n" +
//                "            \"AdditionalChargeApplied\": 510.00,\n" +
//                "            \"AdditionalDiscountApplied\": 33.00\n" +
//                "        }\n" +
//                "}";
//
//        final MediaType JSON
//                = MediaType.get("application/json; charset=utf-8");
//
//        OkHttpClient client = new OkHttpClient();
//
//        RequestBody body = RequestBody.create(payload, JSON);
//        Request request = new Request.Builder()
//                .url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000495aYEAQ")
//                .addHeader("Authorization","Bearer "+token)
//                .post(body)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            Log.d("2515", response.body().string());
//            Log.d("2515", response.toString());
//        } catch (IOException e) {
//            Log.d("2515", "submitPatch: "+e);
//            e.printStackTrace();
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_order, menu);

        itemCart = menu.findItem(R.id.itemEditOrder);

        setBadgeCount(this, itemCart, "0");

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
        ((EditText)dialog.findViewById(R.id.editText)).setText(round(selectedProduct.salesPrice,2)+"");
        ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd*selectedProduct.salesPrice,2)+"");

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
                    ((EditText)dialog.findViewById(R.id.editText2)).setText(round(productCharges.get(i).chargeValue,2)+"");
                    double total = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                    ((TextView)dialog.findViewById(R.id.textView71)).setText(round(total,2)+"");
                    break;
                case 1:
                    ((TextView)dialog.findViewById(R.id.textView63)).setText(productCharges.get(i).chargeName+ (productCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText)dialog.findViewById(R.id.editText3)).setText(round(productCharges.get(i).chargeValue,2)+"");
                    double total1 = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                    ((TextView)dialog.findViewById(R.id.textView72)).setText(round(total1,2)+"");

                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
                    break;

                case 2:
                    ((TextView) dialog.findViewById(R.id.textView64)).setText(productCharges.get(i).chargeName+ (productCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText4)).setText(round(productCharges.get(i).chargeValue,2)+"");
                    double total2 = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
                    ((TextView) dialog.findViewById(R.id.textView73)).setText(round(total2,2) + "");

                    dialog.findViewById(R.id.textView64).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText4).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView73).setVisibility(View.VISIBLE);
                    break;
                case 3:
                    ((TextView) dialog.findViewById(R.id.textView65)).setText(productCharges.get(i).chargeName+ (productCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText5)).setText(round(productCharges.get(i).chargeValue,2)+"");
                    double total3 = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;

                    ((TextView) dialog.findViewById(R.id.textView74)).setText(round(total3,2) + "");
                    dialog.findViewById(R.id.textView65).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText5).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView74).setVisibility(View.VISIBLE);
                    break;
                case 4:
                    ((TextView) dialog.findViewById(R.id.textView69)).setText(productCharges.get(i).chargeName+ (productCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText8)).setText(round(productCharges.get(i).chargeValue,2)+"");
                    double total4 = productCharges.get(i).chargeValueType.equals("Fixed") ? productCharges.get(i).chargeValue : qtyToAdd * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;

                    ((TextView) dialog.findViewById(R.id.textView75)).setText(round(total4,2) + "");

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

                NewOrderActivity.this.selectedProduct.cost = selectedProduct.salesPrice;
                othercharges = selectedProduct.tax;
                otherdisc = selectedProduct.discount;


                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");

                ((TextView)findViewById(R.id.tvSalesPrice)).setText(round(selectedProduct.salesPrice,2)+"");
//                updateProducts();
//                setCharges();
                ((TextView)findViewById(R.id.tvCharges)).setText(round(fixedCharge+qtyToAdd*(selectedProduct.salesPrice*floatingCharge/100),2)+"");
                ((TextView)findViewById(R.id.tvDiscount)).setText(round(fixedDiscount+qtyToAdd*(selectedProduct.salesPrice*floatingDiscount/100),2)+"");

                ((TextView)findViewById(R.id.tvTotalCost)).setText(round(selectedProduct.total,2)+"");

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

        ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd*(changedPrice[0]+ (changedPrice[0]*finalFloatingCharges/100) - (changedPrice[0]*finalFloatingDisc/100))+ finalFixedCharges - finalFixedDisc,2)+"");

        dialog.show();
    }

    private void showSelectedProduct(String name) {

        selectedProduct = null;
        totalCost = 0;
        othercharges = 0.0;
        otherdisc = 0.0;
        productCharges.clear();

        for (int i = 0; i < productList.size(); i++) {
            if (name.equals(productList.get(i).getName())){
                selectedProduct = productList.get(i);
            }
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //getChargeList(selectedProduct.ID);
                    //fetchProductGroup(selectedProduct.ID);
                    getCombinedCharges(selectedProduct.cost,selectedProduct.ID, selectedProduct.group.equals("null") ? "null" : "'"+selectedProduct.group+"'", account_group);
                    getDealerCharges(dealerID);
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

        ((TextView)findViewById(R.id.tvCharges)).setText(round(selectedProduct.cost * tax/100,2)+"");
        ((TextView)findViewById(R.id.tvDiscount)).setText(round(selectedProduct.cost * discount/100,2)+"");
        ((TextView)findViewById(R.id.tvTotalCost)).setText("₹ "+round(comupteTotal(qtyToAdd,selectedProduct.cost,tax+othercharges,discount),2));
    }

    private void getChargeList(String Id) throws UnsupportedEncodingException {
        String soql = "select Product__r.name, name, Id,ChargeMaster__c, ChargeMaster__r.Description__c, ChargeMaster__r.ChargeType__c, ChargeMaster__r.Amount__c, ChargeMaster__r.AmountType__c from ProductChargeConfiguration__c where Dealer__c='"+dealerID+"' and (Product__r.name= null or Product__c='"+Id+"') and Active__c = true AND IsValid__c = true";
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
                            Log.d("ProductCharges"+Id, "Result :" + result.toString());

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
                                    ((TextView)findViewById(R.id.tvTotalCost)).setText("₹ "+String.valueOf(round(comupteTotal(qtyToAdd,selectedProduct.cost,othercharges,otherdisc),2)));
                                    ((TextView)findViewById(R.id.tvCharges)).setText(round(othercharges,2)+"");
                                    ((TextView)findViewById(R.id.tvDiscount)).setText(round(otherdisc,2)+"");
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

    private void getDealerCharges(String Id) throws UnsupportedEncodingException {
        String soql = "SELECT ID, ChargeMaster__c, AccountGroup__c, ChargeMaster__r.Description__c, ChargeMaster__r.AmountType__c, ChargeMaster__r.Amount__c, ChargeMaster__r.ChargeType__c, Active__c, IsValid__c,Level__c, Dealer__c , DocumentType__c FROM DocumentChargeConfig__c where Active__c = true AND IsValid__c = true AND Level__c ='Dealer' AND AccountGroup__c =NULL AND Dealer__c ='"+dealerID+"' AND (DocumentType__c ='SALES ORDER' OR DocumentType__c =NULL)";
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
                            Log.d("DealerCharges", "Result :" + result.toString());

//                            JSONArray record = result.asJSONObject().getJSONArray("records");
//                            for (int i = 0; i < record.length(); i++) {
//                                String Id =  record.getJSONObject(i).getString("Id");
//                                String chargeID = record.getJSONObject(i).getString("ChargeMaster__c");
//
//                                JSONObject charge = record.getJSONObject(i).getJSONObject("ChargeMaster__r");
//                                String chargeType = charge.getString("ChargeType__c");
//                                String amountType = charge.getString("AmountType__c");
//                                DocCharge d = new DocCharge(charge.getString("Description__c"),Id,chargeID,true,charge.getDouble("Amount__c"), amountType.matches("Floating"),chargeType.matches("Tax"));
//                                docCharges.add(d);
//                                Log.d("2516 Added doc_charge:", "run: "+charge.toString());
//
//                                if (chargeType.matches("Tax")) {
//                                    othercharges += charge.getDouble("Amount__c");
//                                }else otherdisc += charge.getDouble("Amount__c");
//                            }
//
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

    private void getCombinedCharges(Double cost,String productId,String productGroup, String accountGroup) throws UnsupportedEncodingException {
        String soql = "SELECT Name, Id, ChargeMaster__c, Level__c, Product__c, ProductGroup__r.name, ChargeMaster__r.Amount__c, ChargeMaster__r.ChargeType__c, ChargeMaster__r.Description__c, ChargeMaster__r.AmountType__c FROM ProductChargeConfiguration__c WHERE Active__c = True AND IsValid__c = True AND (Level__c = 'DEALER' OR Level__c = 'OEM') AND ( ((DocumentType__c ='SALES ORDER' OR DocumentType__c =NULL) AND ACCOUNTGroup__c = NULL) OR ((DocumentType__c ='SALES ORDER' OR DocumentType__c =NULL) AND ACCOUNTGroup__c ='"+accountGroup+"')) And (Product__c=null OR Product__c in('"+productId+"')) AND (ProductGroup__c=null or ProductGroup__c in("+productGroup+"))";
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

        Log.d("CombinedCharges Query", soql);
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

        String soql = "SELECT ID, ChargeMaster__c, ChargeMaster__r.name,ChargeMaster__r.ChargeType__c,ChargeMaster__r.Amount__c, ChargeMaster__r.Description__c, ChargeMaster__r.AmountType__c, Dealer__c,Dealer__r.name, FinancialAccount__c\n" +
                "                           FROM ChargeFinanceConfiguration__c where Dealer__c ='"+dealerID+"' AND ChargeMaster__c IN "+chargeList+"";
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
                                    ((TextView)findViewById(R.id.tvTotalCost)).setText("₹ "+String.valueOf(round(comupteTotal(qtyToAdd,selectedProduct.cost,othercharges,otherdisc),2)));
                                    ((TextView)findViewById(R.id.tvCharges)).setText(round(othercharges,2)+"");
                                    ((TextView)findViewById(R.id.tvDiscount)).setText(round(otherdisc,2)+"");
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

    private double comupteTotal(int qtyToAdd, double cost, double tax, double discount) {

        double subtotal = qtyToAdd * cost;
        double appTax = tax;
        double appDisc = discount;
        totalCost = (subtotal+appTax-appDisc);
        return (subtotal+appTax-appDisc);
    }

    private void resetProduct() {
        selectedProduct = null;
        qtyToAdd = 1;
        totalCost = 0;
        othercharges = 0.0;
        docCharges.clear();
        productCharges.clear();
        findViewById(R.id.cvProductInfo).setVisibility(View.INVISIBLE);
        etProducts.setText("");
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
                                            logout(NewOrderActivity.this);
                                    return;
                                }
                                // Cache the returned client
                                client1 = client;
                                monitorNetwork();
                                JSONObject cred = client1.getJSONCredentials();
                                try {
                                    token = cred.getString("accessToken");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    checkUserType();
                                    //fetchDealerID();

                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
    }

    void checkUserType() throws UnsupportedEncodingException {
        String soql = null;
        try {
            soql = "SELECT Id,name,account.parentId ,contact.AccountId,account.name FROM User where id='"+client1.getJSONCredentials().getString("userId")+"'";
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
                            String parentID = records.getJSONObject(0).getJSONObject("Account").getString("ParentId");
                            Log.d("2513", "parentid: "+parentID);
                            if(parentID.matches("null")){

                                isBranch = false;
                                fetchDealerID();
                            }else{
                                isBranch = true;
                                fetchParentID(parentID);

                                String[] branches = new String[1];

                                branches[0] = records.getJSONObject(0).getJSONObject("Account").getString("Name");
                                String id =  records.getJSONObject(0).getJSONObject("Contact").getString("AccountId");
                                branchList.add(new Branch(branches[0],id));
                                branchAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_spinner_dropdown_item,branches);
                                branchSpinner.setAdapter(branchAdapter);
                                findViewById(R.id.imageView9).setVisibility(View.VISIBLE);
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

    void fetchDealerID() throws UnsupportedEncodingException {
        String soql = null;
        try {
            soql = "Select Id, Contact.Id, Contact.Name, Contact.Account.ID, Contact.Account.Name from User where id='"+client1.getJSONCredentials().getString("userId")+"'";
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
                            dealerID = records.getJSONObject(0).getJSONObject("Contact").getJSONObject("Account").getString("Id");
                            dealerName = records.getJSONObject(0).getJSONObject("Contact").getJSONObject("Account").getString("Name");
                            Log.d("2513", "run: "+dealerID);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.textView40)).setText("Dealer Name : "+dealerName);
                                }
                            });
                            fetchAccountGroup();
                            fetchBranches();
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

    void fetchParentID(String parentID) throws UnsupportedEncodingException {
        String soql = null;
        soql = "SELECT Id, Name, AccountGroup__c from Account where Id='"+parentID+"'";
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
                            dealerID = records.getJSONObject(0).getString("Id");
                            dealerName = records.getJSONObject(0).getString("Name");
                            Log.d("2513", "Parent Info: "+result.toString());
                            //fetchBranches();
                            fetchAccountGroup();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.textView40)).setText("Dealer Name : "+dealerName);
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

    void fetchBranches() throws UnsupportedEncodingException {
        String soql = "select id,Name from Account where parentid='"+dealerID+"'";
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
                            Log.d("2513", "Branches: "+result.asJSONObject().getJSONArray("records"));
                            String[] branches = new String[records.length()];


                            for (int i = 0; i < records.length(); i++) {
                                branches[i] = records.getJSONObject(i).getString("Name");
                                String id =  records.getJSONObject(i).getString("Id");
                                branchList.add(new Branch(branches[i],id));
                            }
                            branchAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_spinner_dropdown_item,branches);
                            branchSpinner.setAdapter(branchAdapter);
                            findViewById(R.id.imageView9).setVisibility(View.VISIBLE);
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

//    void fetchProductGroup(String id) throws UnsupportedEncodingException {
//        String soql = "select id, name , ProductGroup__c from product2 where id in('"+id+"')";
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
//
//                            String product_group = records.getJSONObject(0).getString("ProductGroup__c");
//                            Log.d("ProductGroup", "Group: "+result.asJSONObject().getJSONArray("records"));
//
//                            AsyncTask.execute(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        //getCombinedCharges(id,product_group,account_group);
//                                    } catch (UnsupportedEncodingException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            });
////                            String[] branches = new String[records.length()];
////
////
////                            for (int i = 0; i < records.length(); i++) {
////                                branches[i] = records.getJSONObject(i).getString("Name");
////                                String id =  records.getJSONObject(i).getString("Id");
////                                branchList.add(new Branch(branches[i],id));
////                            }
////                            branchAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_spinner_dropdown_item,branches);
////                            branchSpinner.setAdapter(branchAdapter);
////                            findViewById(R.id.imageView9).setVisibility(View.VISIBLE);
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
        String soql = "SELECT ID, AccountGroup__c FROM Account WHERE Id='"+dealerID+"'";
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

    void fetchCustomers(String id) throws UnsupportedEncodingException {

        String soql = "select account.name, recordtype.developername,id, name   from contact where recordtype.developername='Customer' and account.id='"+id+"'";

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
                            String[] customers = new String[records.length()];

                            for (int i = 0; i < records.length(); i++) {
                                customers[i] = records.getJSONObject(i).getString("Name");
                                customerID = records.getJSONObject(i).getString("Id");
                                customerList.add(new Customer(customers[i],customerID));
                            }
                            customerID = null;
                            branchAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_spinner_dropdown_item,customers);
                            customerSpinner.setAdapter(branchAdapter);

                            customerAdapter = new
                                    ArrayAdapter(getApplicationContext(),android.R.layout.simple_list_item_1,customers);

                            etCustomers.setAdapter(customerAdapter);
                            etCustomers.setThreshold(1);
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

    void fetchProducts(String id) throws UnsupportedEncodingException {

        String soql = "Select Product_Name__c,Id,Product__r.ProductGroup__c,Product__r.ProductGroup__r.name, TotalAvailableQuantity__c, TotalAllocatedQuantity__c, InHandQuantity__c, UOM__c, Product__r.UnitPrice__c, Product__r.Id from INVProductSummary__c where BranchName__c='"+id+"'";

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

                            log(result.toString());
                            //Log.d("FetchProducts", "run: "+result.toString());
                            String[] products = new String[records.length()];

                            for (int i = 0; i < records.length(); i++) {
                                products[i] = records.getJSONObject(i).getString("Product_Name__c");
                                String uom = records.getJSONObject(i).getString("UOM__c");
                                String group = records.getJSONObject(i).getJSONObject("Product__r").getString("ProductGroup__c");
                                String ID = records.getJSONObject(i).getJSONObject("Product__r").getString("Id");
                                Double available = records.getJSONObject(i).getDouble("TotalAvailableQuantity__c");
                                Double allocated = records.getJSONObject(i).getDouble("TotalAllocatedQuantity__c");
                                Double inHand = records.getJSONObject(i).getDouble("InHandQuantity__c");
                                Double cost = records.getJSONObject(i).getJSONObject("Product__r").getDouble("UnitPrice__c");

                                Product p = new Product(products[i],ID,uom,available,allocated,inHand,cost,group);
                                productList.add(p);
                            }
                            productAdapter = new
                                    ArrayAdapter(getApplicationContext(),android.R.layout.simple_list_item_1,products);

                            etProducts.setAdapter(productAdapter);
                            etProducts.setThreshold(1);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Log.d("FetchProducts", "run: "+ex.toString());
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

    public void log(String message) {
        // Split by line, then ensure each line can fit into Log's maximum length.
        for (int i = 0, length = message.length(); i < length; i++) {
            int newline = message.indexOf('\n', i);
            newline = newline != -1 ? newline : length;
            do {
                int end = Math.min(newline, i + 3000);
                Log.d("Fetch", message.substring(i, end));
                i = end;
            } while (i < newline);
        }
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();  return true;
        }

        if (id == R.id.itemEditOrder) {
            OrderObject orderObject =  new OrderObject("","",dealerName, dealerID,branchName, branchID,customerName, customerID,state,narration,cartProducts,null, combinedCharges);

            Intent i = new Intent(getApplicationContext(),CartActivity.class);
            i.putExtra("order",orderObject);

            if (customerID == null){
                Toast.makeText(this,"Please select a customer!", Toast.LENGTH_SHORT).show();
                return true;
            }

            if(cartProducts.size() > 0) {
                someActivityResultLauncher.launch(i);
            }else Toast.makeText(this,"Please add at least 1 product.", Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
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

        Log.d("ChargeType", "Fixed Charge: "+fixedCharges);
        Log.d("ChargeType", "Floating Charge: "+floatingCharges);
        Log.d("ChargeType", "Fixed Disc: "+fixedDisc);
        Log.d("ChargeType", "Floating Disc: "+floatingDisc);

        switch (view.getId()){

            case R.id.imageButton5:

                qtyToAdd++;
                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");

                //((TextView)findViewById(R.id.tvSalesPrice)).setText(round(selectedProduct.cost,2)+"");
//                updateProducts();
//                setCharges();
                //((TextView)findViewById(R.id.tvCharges)).setText(round(fixedCharges+(selectedProduct.cost*floatingCharges/100),2)+"");
                //((TextView)findViewById(R.id.tvDiscount)).setText(round(fixedDisc+(selectedProduct.cost*floatingDisc/100),2)+"");

                ((TextView)findViewById(R.id.tvTotalCost)).setText(""+round(qtyToAdd*(selectedProduct.cost+ (selectedProduct.cost *floatingCharges/100) - (selectedProduct.cost*floatingDisc/100))+ fixedCharges - fixedDisc,2));
                break;
            case R.id.imageButton4:
                if (qtyToAdd>1) qtyToAdd--;
                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");
                //((TextView)findViewById(R.id.tvSalesPrice)).setText(round(selectedProduct.cost,2)+"");
//                updateProducts();
//                setCharges();
                //((TextView)findViewById(R.id.tvCharges)).setText(round(fixedCharges+(selectedProduct.cost*floatingCharges/100),2)+"");
                //((TextView)findViewById(R.id.tvDiscount)).setText(round(fixedDisc+(selectedProduct.cost*floatingDisc/100),2)+"");

                ((TextView)findViewById(R.id.tvTotalCost)).setText(""+round(qtyToAdd*(selectedProduct.cost+ (selectedProduct.cost *floatingCharges/100) - (selectedProduct.cost*floatingDisc/100))+ fixedCharges - fixedDisc,2));
                break;
            case R.id.button12:
                CartProduct cartProduct = new CartProduct(selectedProduct.name,selectedProduct.ID,null,selectedProduct.uom,qtyToAdd,selectedProduct.cost,(selectedProduct.cost *floatingCharges/100)+fixedCharges,(selectedProduct.cost*floatingDisc/100)+floatingDisc,qtyToAdd*(selectedProduct.cost+ (selectedProduct.cost *floatingCharges/100) - (selectedProduct.cost*floatingDisc/100))+ fixedCharges - fixedDisc);
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

                Log.d("Charges", "Adding product with number of charges: "+cartProducts.get(cartProducts.size()-1).charges.size());
                Toast.makeText(this,"Product Added to cart", Toast.LENGTH_SHORT).show();
                setBadgeCount(this, itemCart, String.valueOf(cartProducts.size()));
                resetProduct();
                break;
        }
    }

    void prepareChargesPayload(){
        for (int j = 0; j < combinedCharges.size(); j++) {
            productChargesPayload += "{\n" +
                    "            \"Id\":"+ (combinedCharges.get(j).id == null ? null : "\""+combinedCharges.get(j).id+"\"") +",\n" +
                    "            \"productId\": \""+combinedCharges.get(j).productId+"\",\n" +
                    "            \"ChargeName\": \""+combinedCharges.get(j).chargeName+"\",\n" +
                    "            \"ChargeMaster\": \""+combinedCharges.get(j).chargeMaster+"\",\n" +
                    "            \"ChargeValue\": "+combinedCharges.get(j).chargeValue+",\n" +
                    "            \"ChargeValueType\": \""+combinedCharges.get(j).chargeValueType+"\",\n" +
                    "            \"ChargeType\": \""+combinedCharges.get(j).chargeType+"\",\n" +
                    "            \"TransactionAccount\": \""+combinedCharges.get(j).transactionAccount+"\",\n" +
                    "            \"Amount\": "+combinedCharges.get(j).amount+"\n" +
                    "        }";
            productChargesPayload += ",";
            productChargesPayload = productChargesPayload.substring(0,productChargesPayload.length()-1);

        }

        //Log.d("2515", "prepareProductPayload: "+productPayload);
    }

    void prepareProductPayload(){
        for (int i = 0; i < cartProducts.size(); i++) {
            productPayload += "{\n" +
                    "            \"Id\":"+ (cartProducts.get(i).pID == null ? null : "\""+cartProducts.get(i).pID+"\"") +",\n" +
                    "            \"productId\": \""+cartProducts.get(i).ID+"\",\n" +
                    "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
                    "            \"isDelete\": false,\n" +
                    "            \"quantity\": "+(double)cartProducts.get(i).qty+",\n" +
                    "            \"status\": \"Open\",\n" +
                    "            \"allocatedQuantity\": 0.00,\n" +
                    "            \"salesPrice\": "+cartProducts.get(i).salesPrice+",\n" +
                    "            \"discount\": "+cartProducts.get(i).discount+",\n" +
                    "            \"tax\": "+cartProducts.get(i).tax+"\n" +
                    "        }";
            productPayload += ",";
        }
        productPayload = productPayload.substring(0,productPayload.length()-1);

        Log.d("2515", "prepareProductPayload: "+productPayload);
    }

    private void submitPatch(String orderID) {
        String payload = "{\n" +
                "   \n" +
                "    \"salesOrderTransactionChargeLst\": [\n" +
                productChargesPayload +
                "    ],\n" +
                "    \"salesOrderDocChargeLst\": [\n" +

                "    ],\n" +
                "    \"salesOrderDetailLst\": [\n" +
                productPayload+
                "    ],\n" +
                " \"salesOrder\": \n" +
                "        {\n" +
                "\t\t\t\"Id\": \"\",\n" +
                "            \"State\": \"draft\",\n" +
                "            \"Status\": \""+orderID+"\",\n" +
                "            \"DealerId\": \""+dealerID+"\",\n" +
                "            \"Narration\": \""+narration+"\",\n" +
                "            \"BranchId\": \""+branchID+"\",\n" +
                "            \"CustomerId\": \""+customerID+"\",\n" +
                "            \"TotalCost\": "+getTotal(subtotal,tax,disc,0.0)+",\n" +
                "            \"Subtotal\": "+subtotal+",\n" +
                "            \"Tax\": "+getTax(cartProducts)+",\n" +
                "            \"Discount\": "+getDiscount(cartProducts)+",\n" +
                "            \"AdditionalChargeApplied\": 1.0,\n" +
                "            \"AdditionalDiscountApplied\": 10.00\n" +
                "        }\n" +
                "}";

//        Log.d("2515", "submitOrder: "+payload);
//        Log.d("2515", "submitPatch: Trying to update order Id:"+ orderObject.orderID);
        final MediaType JSON
                = MediaType.get("application/json; charset=utf-8");

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(payload, JSON);
        Request request = new Request.Builder()
                .url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/")
                .addHeader("Authorization","Bearer "+token)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Log.d("2515", response.body().string());
            Log.d("2515", response.toString());
            new TinyDB(getApplicationContext()).putBoolean("CloseNewOrder", true);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (orderID){
                        case "Draft":
                            Toast.makeText(getApplicationContext(),"Order saved as Draft!", Toast.LENGTH_SHORT).show();
                            break;
                        case "Submitted For Approval":
                            Toast.makeText(getApplicationContext(),"Order submitted for approval!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            });
            new TinyDB(this).putBoolean("RefreshOrders", true);
            finish();
        } catch (IOException e) {
            Log.d("2515", "submitPatch: "+e);
            e.printStackTrace();
        }
    }

    private double getTotal(double subtotal, double tax, double discount, double otherCost) {

        totalCost = subtotal + tax - discount + otherCost;
        return Double.parseDouble(round(subtotal + tax - discount + otherCost,2));
    }

    private double getSubtotal(ArrayList<CartProduct> products) {
        double cost = 0;
        for (int i = 0; i < products.size(); i++) {
            cost += products.get(i).salesPrice*products.get(i).qty;
        }
        subtotal = cost;
        return Double.parseDouble(round(cost,2));
    }

    private double getTax(ArrayList<CartProduct> products) {
        double cost = 0;
        for (int i = 0; i < products.size(); i++) {
            cost += products.get(i).tax;
        }
        tax = cost;
        return Double.parseDouble(round(cost,2));
    }

    private double getDiscount(ArrayList<CartProduct> products) {
        double cost = 0;
        for (int i = 0; i < products.size(); i++) {
            cost += products.get(i).discount;
        }
        disc = cost;
        return Double.parseDouble(round(cost,2));
    }

    private double getAddDiscount() {
        double cost = 0;
        for (int i = 0; i < docCharges.size(); i++) {
            View[] views = {findViewById(R.id.checkBox),findViewById(R.id.checkBox3),findViewById(R.id.checkBox4),findViewById(R.id.checkBox5),findViewById(R.id.checkBox6)};
            if(!docCharges.get(i).isApplied && ((CheckBox)views[i]).isChecked()) {
                cost += docCharges.get(i).amount;
            }
        }
        return Double.parseDouble(round(cost,2));
    }



    public static String round(double value, int places) {
//        if (places < 0) throw new IllegalArgumentException();
//
//        BigDecimal bd = BigDecimal.valueOf(value);
//        bd = bd.setScale(places, RoundingMode.HALF_UP);
//        return bd.doubleValue();
        String val = String.valueOf((double) Math.round(value * 100) / 100);

        return (val.charAt(val.length()-1)) == '0' ? val+"0":val;
    }

}