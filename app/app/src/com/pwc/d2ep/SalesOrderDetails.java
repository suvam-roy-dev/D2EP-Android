package com.pwc.d2ep;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SalesOrderDetails extends AppCompatActivity {

    private RestClient client1;
    String orderID, orderName;
    ArrayList<CartProduct> cartProducts;
    ArrayList<DocCharge> docCharges;
    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_order_details);

        cartProducts = new ArrayList<>();
        docCharges = new ArrayList<>();

        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        orderID = getIntent().getStringExtra("orderID");
        orderName = getIntent().getStringExtra("orderName");
        setTitle(orderName);
        connectSF();


        findViewById(R.id.button21).setOnClickListener(new View.OnClickListener() {
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
                CartProductAdapter adapter = new CartProductAdapter(cartProducts,getApplicationContext(), false);
                rvProduct.setAdapter(adapter);



                dialog.findViewById(R.id.button22).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.order_details, menu);
        this.menu = menu;
        menu.getItem(0).setVisible(false);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();  return true;
        }

        if (id == R.id.itemEditOrder) {
            //OrderObject orderObject =  new OrderObject(orderName, dealerID,branchName, branchID,customerName, customerID,state,narration,cartProducts);

            Toast.makeText(this,"In Progress...", Toast.LENGTH_SHORT).show();
//            Intent i = new Intent(getApplicationContext(),NewOrderActivity.class);
//            //i.putExtra("order",orderObject);
//            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    private void connectSF(){
        String accountType =
                SalesforceSDKManager.getInstance().getAccountType();
        ClientManager.LoginOptions loginOptions =
                SalesforceSDKManager.getInstance().getLoginOptions();
        //loginOptions.setLoginUrl("http://google.com");
// Get a rest client
        new ClientManager(this, accountType, loginOptions,
                false).
                getRestClient(this, new ClientManager.RestClientCallback() {
                    @Override
                    public void
                    authenticatedRestClient(RestClient client) {
                        if (client == null) {
                            SalesforceSDKManager.getInstance().
                                    logout(SalesOrderDetails.this);
                            return;
                        }
                        // Cache the returned client
                        client1 = client;
                        String token = "";

                        JSONObject cred = client1.getJSONCredentials();
                        try {
                            token = cred.getString("accessToken");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String finalToken = token;
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                fetchOrderDetails(finalToken,orderID);
//                                checkSalesOrderQuery(client.getAuthToken());
//                                checkSalesOrderQuery(client.getRefreshToken());
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
                Log.d("2514", message.substring(i, end));
                i = end;
            } while (i < newline);
        }
    }

    private void fetchOrderDetails(String token, String orderID) {

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/"+orderID)
                .addHeader("Authorization","Bearer "+token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            //Log.d("2514", "Request URL: https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h0000048l32EAA"+ "\n" +" Token: "+token +"\n"+ " Response: "+response.body().string());

            //String result =
            JSONObject records =  new JSONObject(response.body().string());

            //Log.d("2514", "Request URL: https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h0000048l32EAA"+ "\n" +" Token: "+token +"\n"+ " Response: "+records.toString());
            log("Response: "+records.toString());
            JSONArray infoArray = records.getJSONArray("salesOrderList");

            String orderName = infoArray.getJSONObject(0).getString("Name");
            String state = "Draft";
            if (infoArray.getJSONObject(0).has("Status__c")) {
                state = infoArray.getJSONObject(0).getString("Status__c");
            }

            String narration = "N/A";
            if (infoArray.getJSONObject(0).has("Narration__c")) {
                narration = infoArray.getJSONObject(0).getString("Narration__c");
            }

            String dealer = infoArray.getJSONObject(0).getJSONObject("Dealer__r").getString("Name");
            String customer = infoArray.getJSONObject(0).getJSONObject("Customer__r").getString("Name");
            String branch = infoArray.getJSONObject(0).getJSONObject("Branch__r").getString("Name");

            JSONArray chargesList =  records.getJSONArray("salesOrderDocChargesList");
            String chargeListText = "";

            for (int i = 0; i < chargesList.length(); i++) {

                String desc = chargesList.getJSONObject(i).getString("Description__c");
                double val = 0.0;

                if (chargesList.getJSONObject(i).has("Value__c")){
                    val = chargesList.getJSONObject(i).getDouble("Value__c");
                }

                String isApplied = chargesList.getJSONObject(i).getString("IsApplied__c");

                DocCharge docCharge = new DocCharge(desc,isApplied.equals("Yes") ? true: false, val);
                docCharges.add(docCharge);

                if (chargesList.getJSONObject(i).getString("IsApplied__c").equals("Yes"))
                    chargeListText += desc + " : " + val + "\n";
            }

            if (chargeListText.length() > 5)
                chargeListText = chargeListText.substring(0,chargeListText.length()-3);

            if (chargeListText.length() == 0)
                chargeListText = "N/A";

            JSONArray products =  records.getJSONArray("salesOrderDetailsList");

            double totalCost = infoArray.getJSONObject(0).getDouble("TotalCost__c");
            double subTotal = infoArray.getJSONObject(0).getDouble("Subtotal__c");
            double tax = infoArray.getJSONObject(0).getDouble("Tax__c");
            double discount = infoArray.getJSONObject(0).getDouble("Discount__c");
            double addCharge = infoArray.getJSONObject(0).getDouble("AdditionalChargeApplied__c");
            double addDisc = infoArray.getJSONObject(0).getDouble("AdditionalDiscountApplied__c");

            String productName = "";

            for (int i = 0; i < products.length(); i++) {
                productName = products.getJSONObject(i).getJSONObject("Product__r").getString("Name");
                String uom = products.getJSONObject(i).getJSONObject("UOM__r").getString("Name");
                int Quantity__c = products.getJSONObject(i).getInt("Quantity__c");
                double SalesPrice__c =  products.getJSONObject(i).getDouble("SalesPrice__c");
                double taxA =  products.getJSONObject(i).getDouble("Tax__c");
                double disc =  products.getJSONObject(i).getDouble("Discount__c");
                double total =  products.getJSONObject(i).getDouble("Total__c");

                CartProduct product = new CartProduct(productName,"",uom,Quantity__c,SalesPrice__c,taxA,disc, total);
                cartProducts.add(product);
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(cartProducts.size()<4){
                        findViewById(R.id.button21).setVisibility(View.GONE);
                    }

            for (int i = 0; i < cartProducts.size(); i++) {

                String name = cartProducts.get(i).name;
                String uom = cartProducts.get(i).UOM;
                int qty  = cartProducts.get(i).qty;
                double salesPrice = cartProducts.get(i).salesPrice;
                double tax1 = cartProducts.get(i).tax;
                double disc1 = cartProducts.get(i).discount;
                double total = cartProducts.get(i).total;

                ConstraintLayout constraintLayout = findViewById(R.id.cvParent);
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
                        ((TextView)findViewById(R.id.tvTotalCostProduct1)).setText(""+total+"");

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
                        ((TextView)findViewById(R.id.tvTotalCostProduct2)).setText(""+total+"");


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
                        ((TextView)findViewById(R.id.tvTotalCostProduct3)).setText(""+total+"");

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                        break;
                }

            }

                    for (int i = 0; i < docCharges.size(); i++) {

                        String name = docCharges.get(i).name;
                        double amount = docCharges.get(i).amount;
                        boolean isApplied = docCharges.get(i).isApplied;

                        ConstraintLayout constraintLayout = findViewById(R.id.cvParent);
                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.clone(constraintLayout);

                        switch (i){
                            case 0:
                                findViewById(R.id.checkBox).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView78).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView103).setVisibility(View.VISIBLE);
                                ((CheckBox)findViewById(R.id.checkBox)).setText(name);
                                ((TextView)findViewById(R.id.textView103)).setText(amount+"");

                                ((CheckBox)findViewById(R.id.checkBox)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                break;

                            case 1:

                                findViewById(R.id.checkBox3).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView104).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView105).setVisibility(View.VISIBLE);
                                ((CheckBox)findViewById(R.id.checkBox3)).setText(name);
                                ((TextView)findViewById(R.id.textView105)).setText(amount+"");

                                ((CheckBox)findViewById(R.id.checkBox3)).setChecked(isApplied);



//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                break;

                            case 2:

                                findViewById(R.id.checkBox4).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView106).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView107).setVisibility(View.VISIBLE);
                                ((CheckBox)findViewById(R.id.checkBox4)).setText(name);
                                ((TextView)findViewById(R.id.textView107)).setText(amount+"");

                                ((CheckBox)findViewById(R.id.checkBox4)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                break;

                            case 3:

                                findViewById(R.id.checkBox5).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView108).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView109).setVisibility(View.VISIBLE);
                                ((CheckBox)findViewById(R.id.checkBox5)).setText(name);
                                ((TextView)findViewById(R.id.textView109)).setText(amount+"");


                                ((CheckBox)findViewById(R.id.checkBox5)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                break;

                            case 4:

                                findViewById(R.id.checkBox6).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView110).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView111).setVisibility(View.VISIBLE);
                                ((CheckBox)findViewById(R.id.checkBox6)).setText(name);
                                ((TextView)findViewById(R.id.textView111)).setText(amount+"");


                                ((CheckBox)findViewById(R.id.checkBox6)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                break;
                        }

                    }

                }
            });



            String finalState = state;
            String finalProductName = productName;
            String finalNarration = narration;
            String finalChargeListText = chargeListText;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    ((TextView)findViewById(R.id.textView50)).setText(finalState);

                    if(!finalState.equals("Draft")){
                        ((TextView)findViewById(R.id.textView50)).setBackgroundResource(R.drawable.rounded_status_bg);
                        ((TextView)findViewById(R.id.textView50)).setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.status_yellow)));

                    }else  menu.getItem(0).setVisible(true);

                    ((TextView)findViewById(R.id.textView80)).setText(": "+dealer);
                    ((TextView)findViewById(R.id.textView81)).setText(": "+branch);
                    ((TextView)findViewById(R.id.textView82)).setText(": "+customer);
                    ((TextView)findViewById(R.id.textView83)).setText(": "+ finalState);
                    ((TextView)findViewById(R.id.textView84)).setText(": "+ finalNarration);

                    ((TextView)findViewById(R.id.textView95)).setText(""+ subTotal);
                    ((TextView)findViewById(R.id.textView96)).setText(""+ tax);
                    ((TextView)findViewById(R.id.textView97)).setText(""+ discount);
                    ((TextView)findViewById(R.id.textView98)).setText(""+ addDisc);
                    ((TextView)findViewById(R.id.textView99)).setText(""+ addCharge);

                    ((TextView)findViewById(R.id.textView102)).setText(""+ totalCost);

                    //((TextView)findViewById(R.id.tvAdditionalCost)).setText(""+ finalChargeListText);

                    //((TextView)findViewById(R.id.tvProductNameProductTab)).setText(finalProductName);
                    RecyclerView rvProduct = findViewById(R.id.rcCartProducts);
                    rvProduct.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

                    //ViewCompat.setNestedScrollingEnabled(rvProduct,true);
                    CartProductAdapter adapter = new CartProductAdapter(cartProducts,getApplicationContext(), false);
                    rvProduct.setAdapter(adapter);

                    ((Button)findViewById(R.id.button21)).setText("View All "+cartProducts.size()+" Products");
                    findViewById(R.id.progressBar8).setVisibility(View.INVISIBLE);
                    findViewById(R.id.nv).setVisibility(View.VISIBLE);
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
            Log.d("2514", "checkSalesOrderQuery: "+e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}