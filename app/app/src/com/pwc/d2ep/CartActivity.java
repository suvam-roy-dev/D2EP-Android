package com.pwc.d2ep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CartActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    double otherCost = 3000.0;
    RecyclerView rvProducts;
    String token = "";
    OrderObject orderObject;
    String productPayload = "";

    ArrayList<DocCharge> docCharges;

    double totalCost, subtotal, tax,disc;
    private RestClient client1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        docCharges = new ArrayList<>();
        setupSF();


        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        orderObject = (OrderObject) getIntent().getSerializableExtra("order");
        //Toast.makeText(this,orderObject.products.get(0).name,Toast.LENGTH_SHORT).show();

        ((TextView)findViewById(R.id.textView80)).setText(": " +orderObject.dealer);
        ((TextView)findViewById(R.id.textView81)).setText(": " +orderObject.branch);
        ((TextView)findViewById(R.id.textView82)).setText(": " +orderObject.customer);
        //((TextView)findViewById(R.id.textView83)).setText(": " +orderObject.state);
        ((TextView)findViewById(R.id.textView84)).setText(": " +orderObject.narration);

        ((TextView)findViewById(R.id.textView95)).setText(getSubtotal(orderObject.products)+"");
        ((TextView)findViewById(R.id.textView96)).setText(getTax(orderObject.products)+"");
        ((TextView)findViewById(R.id.textView97)).setText("-"+getDiscount(orderObject.products)+"");
        ((TextView)findViewById(R.id.textView98)).setText("0.0");
        ((TextView)findViewById(R.id.textView99)).setText("3000");

        ((TextView)findViewById(R.id.textView102)).setText(String.valueOf(getTotal(getSubtotal(orderObject.products),getTax(orderObject.products),getDiscount(orderObject.products),otherCost)));

        rvProducts = findViewById(R.id.rcCartProducts);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        ((CheckBox)findViewById(R.id.checkBox)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox3)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox4)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox5)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox6)).setOnCheckedChangeListener(this);
        CartProductAdapter cartProductAdapter = new CartProductAdapter(orderObject.products,this, true);
        rvProducts.setAdapter(cartProductAdapter);

        final ArrayList<CartProduct>[] cartProducts = new ArrayList[]{orderObject.products};

        findViewById(R.id.bRemoveProduct1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cartProducts[0].remove(0);
                findViewById(R.id.cardView12).setVisibility(View.GONE);
                if (cartProducts[0].size()>3){
                    findViewById(R.id.button23).setVisibility(View.VISIBLE);
                    ((Button)findViewById(R.id.button23)).setText("View All "+ cartProducts[0].size()+" Products");
                }else  findViewById(R.id.button23).setVisibility(View.INVISIBLE);
            }
        });

        findViewById(R.id.bRemoveProduct2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cartProducts[0].remove(1);
                findViewById(R.id.cardView13).setVisibility(View.GONE);
                if (cartProducts[0].size()>3){
                    findViewById(R.id.button23).setVisibility(View.VISIBLE);
                    ((Button)findViewById(R.id.button23)).setText("View All "+ cartProducts[0].size()+" Products");
                }else  findViewById(R.id.button23).setVisibility(View.INVISIBLE);
            }
        });

        findViewById(R.id.bRemoveProduct3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cartProducts[0].remove(2);
                findViewById(R.id.cardView14).setVisibility(View.GONE);
                if (cartProducts[0].size()>3){
                    findViewById(R.id.button23).setVisibility(View.VISIBLE);
                    ((Button)findViewById(R.id.button23)).setText("View All "+ cartProducts[0].size()+" Products");
                }else  findViewById(R.id.button23).setVisibility(View.INVISIBLE);
            }
        });


        findViewById(R.id.button23).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(view.getContext());

                dialog.setContentView(R.layout.dialog_product_list);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);

                RecyclerView rvProduct = dialog.findViewById(R.id.rvProductListDialog);
                rvProduct.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

                //ViewCompat.setNestedScrollingEnabled(rvProduct,true);
                final CartProductAdapter[] adapter = {new CartProductAdapter(cartProducts[0], getApplicationContext(), true)};
                rvProduct.setAdapter(adapter[0]);

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        cartProducts[0] = adapter[0].getUpdatedList();
                        adapter[0] = new CartProductAdapter(cartProducts[0],getApplicationContext(),true);

                        rvProduct.setAdapter(adapter[0]);

                        if (cartProducts[0].size()>3){
                            findViewById(R.id.button23).setVisibility(View.VISIBLE);
                            ((Button)findViewById(R.id.button23)).setText("View All "+ cartProducts[0].size()+" Products");
                        }else  findViewById(R.id.button23).setVisibility(View.INVISIBLE);
                    }
                });
                dialog.findViewById(R.id.button22).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        if (cartProducts[0].size()>3){
            findViewById(R.id.button23).setVisibility(View.VISIBLE);
            ((Button)findViewById(R.id.button23)).setText("View All "+ cartProducts[0].size()+" Products");
        }
        for (int i = 0; i < cartProducts[0].size(); i++) {

            String name = cartProducts[0].get(i).name;
            String uom = cartProducts[0].get(i).UOM;
            int qty  = cartProducts[0].get(i).qty;
            double salesPrice = cartProducts[0].get(i).salesPrice;
            double tax1 = cartProducts[0].get(i).tax;
            double disc1 = cartProducts[0].get(i).discount;
            double total = cartProducts[0].get(i).total;

            ConstraintLayout constraintLayout = findViewById(R.id.cvParentCart);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            switch (i){
                case 0:

                    findViewById(R.id.cardView12).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.tvProduct1Name)).setText(name);
                    ((TextView)findViewById(R.id.tvUOMProduct1)).setText(uom);
                    ((TextView)findViewById(R.id.tvQty)).setText(qty+"");
                    ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+salesPrice);
                    ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+tax1+"");
                    ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+disc1+"");
                    ((TextView)findViewById(R.id.tvTotalCostProduct1)).setText("₹ " +total+"");

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                    break;

                case 1:

                    findViewById(R.id.cardView13).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.tvProduct2Name)).setText(name);
                    ((TextView)findViewById(R.id.tvUOMProduct2)).setText(uom);
                    ((TextView)findViewById(R.id.tvQty2)).setText(qty+"");
                    ((TextView)findViewById(R.id.tvSalesPriceProduct2)).setText(""+salesPrice+"");
                    ((TextView)findViewById(R.id.tvChargesProduct2)).setText(""+tax1+"");
                    ((TextView)findViewById(R.id.tvDiscountProduct2)).setText(""+disc1+"");
                    ((TextView)findViewById(R.id.tvTotalCostProduct2)).setText("₹ "+total+"");


//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                    break;

                case 2:


                    findViewById(R.id.cardView14).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.tvProduct3Name)).setText(name);
                    ((TextView)findViewById(R.id.tvUOMProduct3)).setText(uom);
                    ((TextView)findViewById(R.id.tvQty3)).setText(qty+"");
                    ((TextView)findViewById(R.id.tvSalesPriceProduct3)).setText(""+salesPrice+"");
                    ((TextView)findViewById(R.id.tvChargesProduct3)).setText(""+tax1+"");
                    ((TextView)findViewById(R.id.tvDiscountProduct3)).setText(""+disc1+"");
                    ((TextView)findViewById(R.id.tvTotalCostProduct3)).setText("₹ "+total+"");

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                    break;
            }
        }

        findViewById(R.id.button17).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareProductPayload();
                Toast.makeText(getApplicationContext(),"Submitting Order...", Toast.LENGTH_SHORT).show();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        submitOrder("Submitted For Approval");
                    }
                });
            }
        });

        findViewById(R.id.button18).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareProductPayload();
                Toast.makeText(getApplicationContext(),"Saving as Draft...", Toast.LENGTH_SHORT).show();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        submitOrder("Draft");
                    }
                });
            }
        });

//        ((CheckBox)findViewById(R.id.checkBox2)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                if (!b){
//                    otherCost = 0.0;
//                    ((TextView)findViewById(R.id.textView102)).setText(""+getTotal(getSubtotal(orderObject.products),getTax(orderObject.products),getDiscount(orderObject.products),0));
//                    ((TextView)findViewById(R.id.textView99)).setText("0.0");
//                } else {
//                    otherCost = 3000.00;
//                        ((TextView)findViewById(R.id.textView102)).setText(""+getTotal(getSubtotal(orderObject.products),getTax(orderObject.products),getDiscount(orderObject.products),otherCost));
//                        ((TextView)findViewById(R.id.textView99)).setText("3000.0");
//                }
//            }
//        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                super.onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private double getTotal(double subtotal, double tax, double discount, double otherCost) {

        totalCost = subtotal + tax - discount + otherCost;
        return (subtotal + tax - discount + otherCost);
    }

    private double getSubtotal(ArrayList<CartProduct> products) {
        double cost = 0;
        for (int i = 0; i < products.size(); i++) {
            cost += products.get(i).salesPrice*products.get(i).qty;
        }
        subtotal = cost;
        return cost;
    }

    private double getTax(ArrayList<CartProduct> products) {
        double cost = 0;
        for (int i = 0; i < products.size(); i++) {
            cost += products.get(i).tax;
        }
        tax = cost;
        return cost;
    }

    private double getDiscount(ArrayList<CartProduct> products) {
        double cost = 0;
        for (int i = 0; i < products.size(); i++) {
            cost += products.get(i).discount;
        }
        disc = cost;
        return cost;
    }

    private double getAddDiscount() {
        double cost = 0;
        for (int i = 0; i < docCharges.size(); i++) {

            View[] views = {findViewById(R.id.checkBox),findViewById(R.id.checkBox3),findViewById(R.id.checkBox4),findViewById(R.id.checkBox5),findViewById(R.id.checkBox6)};

            if(!docCharges.get(i).isApplied && ((CheckBox)views[i]).isChecked()) {
                cost += docCharges.get(i).amount;
            }
        }
        return cost;
    }

    private double getAddCost() {
        double cost = 0;
        View[] views = {findViewById(R.id.checkBox),findViewById(R.id.checkBox3),findViewById(R.id.checkBox4),findViewById(R.id.checkBox5),findViewById(R.id.checkBox6)};

        for (int i = 0; i < docCharges.size(); i++) {
            if(docCharges.get(i).isApplied && ((CheckBox)views[i]).isChecked()) {
                cost += docCharges.get(i).amount;
            }
        }
        return cost;
    }

    void setCharges(){

        ((TextView)findViewById(R.id.textView95)).setText(getSubtotal(orderObject.products)+"");
        ((TextView)findViewById(R.id.textView96)).setText(getTax(orderObject.products)+"");

        ((TextView)findViewById(R.id.textView98)).setText("-"+getAddDiscount()+"");
        ((TextView)findViewById(R.id.textView99)).setText(getAddCost()+"");
//        ((TextView)findViewById(R.id.textView96)).setText(getTax(orderObject.products)+"");
//        ((TextView)findViewById(R.id.textView96)).setText(getTax(orderObject.products)+"");
        double total = getSubtotal(orderObject.products) + getTax(orderObject.products) - getAddDiscount() + getAddCost();

        ((TextView)findViewById(R.id.textView102)).setText("₹ "+total);


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
                                            logout(CartActivity.this);
                                    return;
                                }
                                // Cache the returned client
                                client1 = client;
                                JSONObject cred = client.getJSONCredentials();
                                try {
                                    token = cred.getString("accessToken");
                                    getChargeList();
                                } catch (JSONException | UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
    }

    void prepareProductPayload(){
        for (int i = 0; i < orderObject.products.size(); i++) {
            productPayload += "{\n" +
            "            \"productId\": \""+orderObject.products.get(i).ID+"\",\n" +
                    "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
                    "            \"quantity\": "+(double)orderObject.products.get(i).qty+",\n" +
                    "            \"status\": \"Open\",\n" +
                    "            \"allocatedQuantity\": 0.00,\n" +
                    "            \"salesPrice\": "+orderObject.products.get(i).salesPrice+",\n" +
                    "            \"discount\": "+orderObject.products.get(i).discount+",\n" +
                    "            \"tax\": "+orderObject.products.get(i).tax+"\n" +
                    "        }";
            productPayload += ",";
        }
        productPayload = productPayload.substring(0,productPayload.length()-1);

        Log.d("2515", "prepareProductPayload: "+productPayload);
    }

    private void submitOrder(String status) {


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
                "    \"salesOrderDetailLst\": [" +
                productPayload +
                " ],\n" +
                " \"salesOrder\": \n" +
                "        {\n" +
                "            \"State\": \"draft\",\n" +
                "            \"Status\": \""+status+"\",\n" +
                "            \"DealerId\": \""+orderObject.dealerID+"\",\n" +
                "            \"Narration\": \""+orderObject.narration+"\",\n" +
                "            \"BranchId\": \""+orderObject.branchID+"\",\n" +
                "            \"CustomerId\": \""+orderObject.customerID+"\",\n" +
                "            \"TotalCost\": "+getTotal(subtotal,tax,disc,getAddCost())+",\n" +
                "            \"Subtotal\": "+subtotal+",\n" +
                "            \"Tax\": "+getTax(orderObject.products)+",\n" +
                "            \"Discount\": "+getDiscount(orderObject.products)+",\n" +
                "            \"AdditionalChargeApplied\": "+getAddCost()+",\n" +
                "            \"AdditionalDiscountApplied\": 10.00\n" +
                "        }\n" +
                "}";

        Log.d("2515", "submitOrder: "+payload);

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
                    switch (status){
                        case "Draft":
                            Toast.makeText(getApplicationContext(),"Order saved as Draft!", Toast.LENGTH_SHORT).show();

                            break;
                        case "Submitted For Approval":
                            Toast.makeText(getApplicationContext(),"Order submitted for approval!", Toast.LENGTH_SHORT).show();

                            break;
                    }

                }
            });

            finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void submitPatch(String orderID) {
        String payload = "{\n" +
                "   \n" +
                "    \"salesOrderDocChargeLst\": [\n" +
                "        {\n" +
                "            \"Id\": \"a1M3h00000FtdlmEAB\",\n" +
                "\t\t\t\"ChargeMaster\": \"a033h000004vLBUAA2\",\n" +
                "            \"ChargeType\": \"Tax\",\n" +
                "            \"Amount\": 4.00,\n" +
                "            \"AmountType\": \"Floating\",\n" +
                "            \"IsApplied\": \"Yes\",\n" +
                "            \"Description\": \"Core  Dia (in cm)\",\n" +
                "            \"Value\": 800.00\n" +
                "        },\n" +
                "        {\n" +
                "            \"Id\": \"a1M3h00000FtdlnEAB\",\n" +
                "\t\t\t\"ChargeMaster\": \"a033h000004vLBVAA2\",\n" +
                "            \"ChargeType\": \"Discount\",\n" +
                "            \"Amount\": 3.00,\n" +
                "            \"AmountType\": \"Fixed\",\n" +
                "            \"IsApplied\": \"Yes\",\n" +
                "            \"Description\": \"Employee Discount\",\n" +
                "            \"Value\": 3.00\n" +
                "        },\n" +
                "        {\n" +
                "            \"Id\": \"a1M3h00000FtdloEAB\",\n" +
                "\t\t\t\"ChargeMaster\": \"a033h000004vLBQAA2\",\n" +
                "            \"ChargeType\": \"Discount\",\n" +
                "            \"Amount\": 1.00,\n" +
                "            \"AmountType\": \"Floating\",\n" +
                "            \"IsApplied\": \"Yes\",\n" +
                "            \"Description\": \"Additional Discount\",\n" +
                "            \"Value\": 450.00\n" +
                "        },\n" +
                "        {\n" +
                "            \"Id\": \"a1M3h00000FtdlpEAB\",\n" +
                "\t\t\t\"ChargeMaster\": \"a033h000003j26jAAA\",\n" +
                "            \"ChargeType\": \"Tax\",\n" +
                "            \"Amount\": 150.00,\n" +
                "            \"AmountType\": \"Fixed\",\n" +
                "            \"IsApplied\": \"Yes\",\n" +
                "            \"Description\": \"Packing Type\",\n" +
                "            \"Value\": 150.00\n" +
                "        },\n" +
                "        {\n" +
                "            \"Id\": \"a1M3h00000FtdlqEAB\",\n" +
                "\t\t\t\"ChargeMaster\": \"a033h000003j188AAA\",\n" +
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
                "            \"Id\": "+null+",\n" +
                "\t\t\t\"productId\": \"01t3h0000019DJ0AAM\",\n" +
                "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
                "            \"quantity\": 250.00,\n" +
                "            \"status\": \"Open\",\n" +
                "            \"allocatedQuantity\": 0.00,\n" +
                "            \"salesPrice\": 300.00,\n" +
                "            \"discount\": 120.00,\n" +
                "            \"tax\": 1150.00\n" +
                "        }\n" +
                "    ],\n" +
                " \"salesOrder\": \n" +
                "        {\n" +
                "\t\t\t\"Id\": \"a1K3h00000499xKEAQ\",\n" +
                "            \"State\": \"Draft\",\n" +
                "            \"DealerId\": \"0013h0000092PXNAA2\",\n" +
                "            \"status\": \"Submitted For Approval\",\n" +
                "            \"Narration\": \"Test order Update\",\n" +
                "            \"BranchId\": \"0013h0000092PY5AAM\",\n" +
                "            \"CustomerId\": \"0033h00000OZJG2AAP\",\n" +
                "            \"TotalCost\": 251219.00,\n" +
                "            \"Subtotal\": 3000.00,\n" +
                "            \"Tax\": 1150.00,\n" +
                "            \"Discount\": 120.00,\n" +
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
            Log.d("2515", "submitPatch: "+e);
            e.printStackTrace();
        }
    }

    private void getChargeList() throws UnsupportedEncodingException {
        String soql = "SELECT ID, ChargeMaster__c, AccountGroup__c, ChargeMaster__r.Description__c, ChargeMaster__r.AmountType__c, ChargeMaster__r.Amount__c, ChargeMaster__r.ChargeType__c, Dealer__c, DocumentType__c, Active__c, IsValid__c, Level__c FROM DocumentChargeConfig__c where  Active__c = true AND IsValid__c = true\n" +
                "and (DocumentType__c=null or DocumentType__c='SALES ORDER')";
        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

        client1.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //listAdapter.clear();
                            Log.d("2516", "Result :" + result.toString());

                            JSONArray record = result.asJSONObject().getJSONArray("records");
                            for (int i = 0; i < record.length(); i++) {
                                JSONObject charge = record.getJSONObject(i).getJSONObject("ChargeMaster__r");

                                String desc = charge.getString("Description__c");
                                String type =  charge.getString("AmountType__c");
                                String disc =  charge.getString("ChargeType__c");
                                double amount = type.equals("Fixed") ? charge.getDouble("Amount__c") : totalCost* (charge.getDouble("Amount__c")/100);
                                DocCharge d = new DocCharge(desc, disc.equals("Tax"),amount);
                                docCharges.add(d);
                            }

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {

                                    setCharges();
                                    for (int i = 0; i < docCharges.size(); i++) {

                                        String name = docCharges.get(i).name;
                                        double amount = docCharges.get(i).amount;
                                        boolean isApplied = docCharges.get(i).isApplied;

//                                        ConstraintLayout constraintLayout = findViewById(R.id.cvParent);
//                                        ConstraintSet constraintSet = new ConstraintSet();
//                                        constraintSet.clone(constraintLayout);

                                        switch (i){
                                            case 0:
                                                findViewById(R.id.checkBox).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView78).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView103).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox)).setText(name);
                                                ((TextView)findViewById(R.id.textView103)).setText(amount+"");

                                                //((CheckBox)findViewById(R.id.checkBox)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                break;

                                            case 1:

                                                findViewById(R.id.checkBox3).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView104).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView105).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox3)).setText(name);
                                                ((TextView)findViewById(R.id.textView105)).setText(amount+"");

                                                //((CheckBox)findViewById(R.id.checkBox3)).setChecked(isApplied);



//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                break;

                                            case 2:

                                                findViewById(R.id.checkBox4).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView106).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView107).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox4)).setText(name);
                                                ((TextView)findViewById(R.id.textView107)).setText(amount+"");

                                                //((CheckBox)findViewById(R.id.checkBox4)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                                break;

                                            case 3:

                                                findViewById(R.id.checkBox5).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView108).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView109).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox5)).setText(name);
                                                ((TextView)findViewById(R.id.textView109)).setText(amount+"");

                                                //((CheckBox)findViewById(R.id.checkBox5)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                break;

                                            case 4:
                                                findViewById(R.id.checkBox6).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView110).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView111).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox6)).setText(name);
                                                ((TextView)findViewById(R.id.textView111)).setText(amount+"");

                                                //((CheckBox)findViewById(R.id.checkBox6)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                                break;
                                        }

                                    }
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
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()){

            case R.id.checkBox:
            case R.id.checkBox3:
            case R.id.checkBox4:
            case R.id.checkBox5:
            case R.id.checkBox6:
                setCharges();
                break;

        }
    }
}