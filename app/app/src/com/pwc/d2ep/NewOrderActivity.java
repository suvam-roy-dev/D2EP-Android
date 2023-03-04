package com.pwc.d2ep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
    String dealerID,branchID, customerID;

    MenuItem itemCart;
    String dealerName, branchName,customerName,state,narration="N/A";
    private AutoCompleteTextView etProducts;
    ArrayList<Branch> branchList;
    ArrayList<Product> productList;
    ArrayAdapter productAdapter;
    Dialog dialog;
    String token;
    Product selectedProduct;
    ArrayList<CartProduct> cartProducts;
    ArrayList<DocCharge> docCharges;
    double totalCost, subtotal, othercharges = 0.0, otherdisc;

    int qtyToAdd = 1;
    double tax = 0.0,discount = 0.0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order);

        branchList = new ArrayList<>();
        productList = new ArrayList<>();
        cartProducts = new ArrayList<>();
        docCharges = new ArrayList<>();
        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle("New Order");
        setupSF();
        dialog = new Dialog(this);

        dialog.setContentView(R.layout.dialog_product);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        findViewById(R.id.imageButton5).setOnClickListener(this);
        findViewById(R.id.imageButton4).setOnClickListener(this);
        findViewById(R.id.button12).setOnClickListener(this);
        findViewById(R.id.button13).setOnClickListener(this);

        branchSpinner = findViewById(R.id.spinner2);
         customerSpinner = findViewById(R.id.spinner3);
         branchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
             @Override
             public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {


                 for (int j = 0; j < branchList.size(); j++) {
                     if(adapterView.getSelectedItem().toString().equals(branchList.get(j).name)){
                         branchID = branchList.get(j).id;

                         branchName = branchList.get(j).name;
                     }
                 }
                 Log.d("2512", "onItemSelected: "+branchID);
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

        customerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                customerName = adapterView.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

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

        ((EditText)findViewById(R.id.editTextTextMultiLine)).addTextChangedListener(new TextWatcher() {
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

        findViewById(R.id.button15).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
//                        submitOrder();
//                        submitPatch("");

                        OrderObject orderObject =  new OrderObject(dealerName, dealerID,branchName, branchID,customerName, customerID,state,narration,cartProducts, docCharges);

                        Intent i = new Intent(getApplicationContext(),CartActivity.class);
                        i.putExtra("order",orderObject);
                        startActivity(i);
                    }
                });

            }
        });
    }

    @Override
    protected void onResume() {
        if (new TinyDB(this).getBoolean("CloseNewOrder")){
            new TinyDB(this).putBoolean("CloseNewOrder", false);
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



    private void submitPatch(String orderID) {
        String payload = "{\"SalesOrderDocChargePatch\": [\n" +
                "        {\n" +
                "\"Id\": \"a1M3h00000FtcTsEAJ\",\n" +
                "            \"chargeMasterId\": \"a033h000004vLBUAA2\",\n" +
                "            \"chargeType\": \"Tax\",\n" +
                "            \"amount\": 100.00,\n" +
                "            \"amountType\": \"Floating\",\n" +
                "            \"isApplied\": \"Yes\",\n" +
                "            \"description\": \"Core  Dia (in cm)\",\n" +
                "            \"value\": 500.00\n" +
                "        },\n" +
                "        {\n" +
                "\"Id\": \"a1M3h00000FtcTtEAJ\",\n" +
                "            \"chargeMasterId\": \"a033h000004vLBVAA2\",\n" +
                "            \"chargeType\": \"Discount\",\n" +
                "            \"amount\": 30.00,\n" +
                "            \"amountType\": \"Fixed\",\n" +
                "            \"isApplied\": \"Yes\",\n" +
                "            \"description\": \"Employee Discount\",\n" +
                "            \"value\": 3.00\n" +
                "        },\n" +
                "        {\n" +
                "\"Id\": \"a1M3h00000FtcTuEAJ\",\n" +
                "            \"chargeMasterId\": \"a033h000004vLBQAA2\",\n" +
                "            \"chargeType\": \"Discount\",\n" +
                "            \"amount\": 1.00,\n" +
                "            \"amountType\": \"Floating\",\n" +
                "            \"isApplied\": \"Yes\",\n" +
                "            \"description\": \"Additional Discount\",\n" +
                "            \"value\": 30.00\n" +
                "        },\n" +
                "        {\n" +
                "\"Id\": \"a1M3h00000FtcTvEAJ\",\n" +
                "            \"chargeMasterId\": \"a033h000003j26jAAA\",\n" +
                "            \"chargeType\": \"Tax\",\n" +
                "            \"amount\": 150.00,\n" +
                "            \"amountType\": \"Fixed\",\n" +
                "            \"isApplied\": \"Yes\",\n" +
                "            \"description\": \"Packing Type\",\n" +
                "            \"value\": 150.00\n" +
                "        },\n" +
                "        {\n" +
                "\"Id\": \"a1M3h00000FtcTwEAJ\",\n" +
                "            \"chargeMasterId\": \"a033h000003j188AAA\",\n" +
                "            \"chargeType\": \"Tax\",\n" +
                "            \"amount\": 8.00,\n" +
                "            \"amountType\": \"Floating\",\n" +
                "            \"isApplied\": \"Yes\",\n" +
                "            \"description\": \"Size/Width (in cm)\",\n" +
                "            \"value\": 240.00\n" +
                "        }\n" +
                "    ],\n" +
                "    \"SalesOrderDetailPatch\": [\n" +
                "        {\n" +
                "\"Id\": \"a1L3h00000BRTYcEAP\",\n" +
                "            \"productId\": \"01t3h0000019DJ0AAM\",\n" +
                "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
                "            \"quantity\": 10.00,\n" +
                "            \"status\": \"Open\",\n" +
                "            \"allocatedQuantity\": 0.00,\n" +
                "            \"salesPrice\": 300.00,\n" +
                "            \"discount\": 120.00,\n" +
                "            \"tax\": 1150.00\n" +
                "        }\n" +
                "    ],\n" +
                " \"SalesOrderPatch\":\n" +
                "        {\n" +
                "\"Id\": \"a1K3h00000495aYEAQ\",\n" +
                "            \"state\": \"Draft\",\n" +
                "            \"dealerId\": \"0013h0000092PXNAA2\",\n" +
                "            \"narration\": \"abc narration \",\n" +
                "            \"branchId\": \"0013h0000092PY5AAM\",\n" +
                "            \"customerId\": \"0033h00000OZJG2AAP\",\n" +
                "            \"totalCost\": 4007.00,\n" +
                "            \"subtotal\": 3000.00,\n" +
                "            \"tax\": 1150.00,\n" +
                "            \"discount\": 120.00,\n" +
                "            \"AdditionalChargeApplied\": 510.00,\n" +
                "            \"AdditionalDiscountApplied\": 33.00\n" +
                "        }\n" +
                "}";

        final MediaType JSON
                = MediaType.get("application/json; charset=utf-8");

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(payload, JSON);
        Request request = new Request.Builder()
                .url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h00000495aYEAQ")
                .addHeader("Authorization","Bearer "+token)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Log.d("2515", response.body().string());
            Log.d("2515", response.toString());
        } catch (IOException e) {
            Log.d("2515", "submitPatch: "+e);
            e.printStackTrace();
        }
    }

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
    private void showEditDialog() {

        dialog.findViewById(R.id.imageButton7).setOnClickListener(this);
        dialog.findViewById(R.id.imageButton8).setOnClickListener(this);

        dialog.findViewById(R.id.button14).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.button16).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.uom);
        ((EditText)dialog.findViewById(R.id.editText)).setText(selectedProduct.cost+"");
        ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*selectedProduct.cost+"");

        dialog.findViewById(R.id.textView63).setVisibility(View.GONE);
        dialog.findViewById(R.id.editText3).setVisibility(View.GONE);
        dialog.findViewById(R.id.textView72).setVisibility(View.GONE);

        double  docTotal = 0.0;
        for (int i = 0; i < docCharges.size(); i++) {
            docTotal += docCharges.get(i).amount;
            switch (i){
                case 0:
                    ((TextView)dialog.findViewById(R.id.textView55)).setText(docCharges.get(i).name);
                    ((EditText)dialog.findViewById(R.id.editText2)).setText(docCharges.get(i).amount+"");
                    ((TextView)dialog.findViewById(R.id.textView71)).setText(docCharges.get(i).amount+"");
                    break;
                case 1:
                    ((TextView)dialog.findViewById(R.id.textView63)).setText(docCharges.get(1).name);
                    ((EditText)dialog.findViewById(R.id.editText3)).setText(docCharges.get(1).amount+"");
                    ((TextView)dialog.findViewById(R.id.textView72)).setText(docCharges.get(1).amount+"");

                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
                    break;
            }
        }

        ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*selectedProduct.cost+docTotal+"");


        dialog.show();
    }

    private void showSelectedProduct(String name) {

        selectedProduct = null;
        totalCost = 0;
        othercharges = 0.0;

        for (int i = 0; i < productList.size(); i++) {
            if (name.equals(productList.get(i).getName())){
                selectedProduct = productList.get(i);
            }
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getChargeList(selectedProduct.ID);
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

        ((TextView)findViewById(R.id.tvCharges)).setText(selectedProduct.cost * tax/100+"");
        ((TextView)findViewById(R.id.tvDiscount)).setText("-"+selectedProduct.cost * discount/100+"");
        ((TextView)findViewById(R.id.tvTotalCost)).setText("₹ "+comupteTotal(qtyToAdd,selectedProduct.cost,tax+othercharges,discount));
    }

    private void getChargeList(String Id) throws UnsupportedEncodingException {
        String soql = "select Product__r.name, name, ChargeMaster__r.Description__c, ChargeMaster__r.ChargeType__c, ChargeMaster__r.Amount__c, ChargeMaster__r.AmountType__c from ProductChargeConfiguration__c where Dealer__c='"+dealerID+"' and (Product__r.name= null or Product__c='"+Id+"') and Active__c = true AND IsValid__c = true";
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
                                JSONObject charge = record.getJSONObject(i).getJSONObject("ChargeMaster__r");
                                DocCharge d = new DocCharge(charge.getString("Description__c"),true,charge.getDouble("Amount__c"));
                                docCharges.add(d);
                                Log.d("2516 Added doc_charge:", "run: "+charge.toString());

                                othercharges += charge.getDouble("Amount__c");
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView)findViewById(R.id.tvTotalCost)).setText("₹ "+String.valueOf(comupteTotal(qtyToAdd,selectedProduct.cost,othercharges,discount)));
                                    ((TextView)findViewById(R.id.tvCharges)).setText(othercharges+"");


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
        double appDisc = subtotal*(discount/100);
        totalCost = (subtotal+appTax-appDisc);
        return (subtotal+appTax-appDisc);
    }


    private void resetProduct() {
        selectedProduct = null;
        qtyToAdd = 1;
        totalCost = 0;
        othercharges = 0.0;
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
                                    fetchDealerID();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
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

                            String[] branches = new String[records.length()];


                            for (int i = 0; i < records.length(); i++) {
                                branches[i] = records.getJSONObject(i).getString("Name");
                                String id =  records.getJSONObject(i).getString("Id");
                                branchList.add(new Branch(branches[i],id));
                            }
                            branchAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_spinner_dropdown_item,branches);
                            branchSpinner.setAdapter(branchAdapter);
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
                            }
                            branchAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_spinner_dropdown_item,customers);
                            customerSpinner.setAdapter(branchAdapter);
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

        String soql = "Select Product_Name__c,Id,TotalAvailableQuantity__c, TotalAllocatedQuantity__c, InHandQuantity__c, UOM__c, Product__r.UnitPrice__c, Product__r.Id from INVProductSummary__c where BranchName__c='"+id+"'";

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
                                String uom = records.getJSONObject(i).getString("UOM__c");

                                String ID = records.getJSONObject(i).getJSONObject("Product__r").getString("Id");
                                Double available = records.getJSONObject(i).getDouble("TotalAvailableQuantity__c");
                                Double allocated = records.getJSONObject(i).getDouble("TotalAllocatedQuantity__c");
                                Double inHand = records.getJSONObject(i).getDouble("InHandQuantity__c");
                                Double cost = records.getJSONObject(i).getJSONObject("Product__r").getDouble("UnitPrice__c");

                                Product p = new Product(products[i],ID,uom,available,allocated,inHand,cost);
                                productList.add(p);
                            }
                            productAdapter = new
                                    ArrayAdapter(getApplicationContext(),android.R.layout.simple_list_item_1,products);

                            etProducts.setAdapter(productAdapter);
                            etProducts.setThreshold(1);
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
            OrderObject orderObject =  new OrderObject(dealerName, dealerID,branchName, branchID,customerName, customerID,state,narration,cartProducts,docCharges);

            Intent i = new Intent(getApplicationContext(),CartActivity.class);
            i.putExtra("order",orderObject);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){

            case R.id.imageButton5:
            case R.id.imageButton8:
                qtyToAdd++;
                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");
                ((TextView)findViewById(R.id.tvTotalCost)).setText(""+comupteTotal(qtyToAdd,selectedProduct.cost,othercharges,discount));

                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
                ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*selectedProduct.cost+"");
                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*selectedProduct.cost+docCharges.get(0).amount+"");

                break;
            case R.id.imageButton4:
            case R.id.imageButton7:
                if (qtyToAdd>0) qtyToAdd--;
                ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd+"");
                ((TextView)findViewById(R.id.tvTotalCost)).setText(""+comupteTotal(qtyToAdd,selectedProduct.cost,othercharges,discount));

                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd+"");
                ((TextView)dialog.findViewById(R.id.textView52)).setText(qtyToAdd*selectedProduct.cost+"");
                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd*selectedProduct.cost+docCharges.get(0).amount+"");

                break;
            case R.id.button12:
                showEditDialog();
                break;
            case R.id.button13:

                CartProduct cartProduct = new CartProduct(selectedProduct.name,selectedProduct.ID,selectedProduct.uom,qtyToAdd,selectedProduct.cost,othercharges,discount,comupteTotal(qtyToAdd,selectedProduct.cost,othercharges,discount));
                cartProducts.add(cartProduct);
                Toast.makeText(this,"Product Added to cart", Toast.LENGTH_SHORT).show();
                setBadgeCount(this, itemCart, String.valueOf(cartProducts.size()));
                resetProduct();
                break;
        }

    }

}