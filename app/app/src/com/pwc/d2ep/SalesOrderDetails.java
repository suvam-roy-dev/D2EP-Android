package com.pwc.d2ep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.Manifest;

public class SalesOrderDetails extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private RestClient client1;
    String orderID, orderName;
    ArrayList<CartProduct> cartProducts;
    ArrayList<DocCharge> docCharges, docChargesNew;
    ArrayList<ProductCharges> productCharges;
    ArrayList<ProductCharges> combinedCharges;

    String productPayload = "", productChargesPayload = "";

    ArrayList<CartProduct> removedProducts;

    OrderObject orderObject;
    Menu menu;
    boolean canEdit = true;
    private String token;
    private String docPayload = "";
    private Dialog dialog;
    private int adapterPos;

    CartProductAdapter adapter;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] permissionstorage = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private Dialog dialog_load;


    @Override
    protected void onResume() {
        if (new TinyDB(this).getBoolean("CloseNewOrder")) {
            new TinyDB(this).putBoolean("CloseNewOrder", false);
            new TinyDB(this).putBoolean("RefreshOrders", true);
            finish();
        }
        super.onResume();
    }

    // check weather storage permission is given or not
    public static void checkpermissions(Activity activity) {
        int permissions = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // If storage permission is not given then request for External Storage Permission
        if (permissions != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, permissionstorage, REQUEST_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_order_details);

        cartProducts = new ArrayList<>();
        docCharges = new ArrayList<>();
        docChargesNew = new ArrayList<>();
        productCharges = new ArrayList<>();
        combinedCharges = new ArrayList<>();
        removedProducts = new ArrayList<>();
        dialog = new Dialog(this);

        dialog.setContentView(R.layout.dialog_product);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        dialog_load = new Dialog(this);


        dialog_load.setContentView(R.layout.dialog_progress);
        //dialog_load.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog_load.setCancelable(false);
        dialog_load.setCanceledOnTouchOutside(false);

        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg, null);
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
                adapter = new CartProductAdapter(cartProducts, removedProducts, SalesOrderDetails.this, canEdit);
                rvProduct.setAdapter(adapter);

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        cartProducts = adapter.getUpdatedList();
                        adapter = new CartProductAdapter(cartProducts, removedProducts,getApplicationContext(),true);

                        rvProduct.setAdapter(adapter);

                        if (cartProducts.size()>3){
                            findViewById(R.id.button21).setVisibility(View.VISIBLE);
                            ((Button)findViewById(R.id.button21)).setText("View All "+ cartProducts.size()+" Products");
                        }else  findViewById(R.id.button21).setVisibility(View.INVISIBLE);
                    }
                });

                dialog.findViewById(R.id.button22).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        removedProducts = adapter.getUpdatedRemovedList();
                        cartProducts = adapter.getUpdatedList();
                        updateProducts();
                        //setCharges();
                        try {
                            getChargeList();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                });

                dialog.show();
            }
        });

        findViewById(R.id.button19).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareProductPayload();
                prepareChargesPayload();
                if(cartProducts.size()==0){
                    Toast.makeText(getApplicationContext(), "Please add at least one product!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //Toast.makeText(getApplicationContext(), "Submitting Order...", Toast.LENGTH_SHORT).show();
                dialog_load.show();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {

                        submitPatch("Submitted For Approval");
                    }
                });
            }
        });

        findViewById(R.id.button20).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareProductPayload();
                prepareChargesPayload();
                if(cartProducts.size()==0){
                    Toast.makeText(getApplicationContext(), "Please add at least one product!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //Toast.makeText(getApplicationContext(), "Saving as Draft...", Toast.LENGTH_SHORT).show();
                dialog_load.show();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        submitPatch("Draft");
                    }
                });
            }
        });

        ((CheckBox)findViewById(R.id.checkBox)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox3)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox4)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox5)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox6)).setOnCheckedChangeListener(this);
    }

    private void showEditDialog(CartProduct selectedProduct, int pos) {

        final double[] changedPrice = {selectedProduct.salesPrice};
        final double[] changedCharge = {selectedProduct.tax};
        final double[] changedDisc = {selectedProduct.discount};

        ArrayList<ProductCharges> thisCharges = selectedProduct.charges;

        double fixedCharges = 0.0;
        double floatingCharges = 0.0;
        double fixedDisc = 0.0;
        double floatingDisc = 0.0;

        for (int j = 0; j < thisCharges.size(); j++) {
            if(thisCharges.get(j).chargeType.equals("Tax")){
                if(thisCharges.get(j).chargeValueType.equals("Fixed")){
                    fixedCharges += thisCharges.get(j).chargeValue;
                } else floatingCharges += productCharges.get(j).chargeValue;
            } else{
                if(thisCharges.get(j).chargeValueType.equals("Fixed")){
                    fixedDisc += thisCharges.get(j).chargeValue;
                } else floatingDisc += thisCharges.get(j).chargeValue;
            }
        }

        for (int i = 0; i < thisCharges.size(); i++) {
            Log.d("Showing Charges", changedPrice[0] + " : "+thisCharges.get(i).chargeName + " : "+ thisCharges.get(i).chargeValue);
        }

        final int[] qtyToAdd = {selectedProduct.qty};
        double finalFloatingCharges = floatingCharges;
        double finalFloatingDisc = floatingDisc;
        double finalFixedCharges = fixedCharges;
        double finalFixedDisc = fixedDisc;
        dialog.findViewById(R.id.imageButton7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (qtyToAdd[0] >1) qtyToAdd[0]--;
                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *changedPrice[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView71)).setText(round(qtyToAdd[0] *changedCharge[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView72)).setText(round(qtyToAdd[0] *changedDisc[0])+"");

                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd[0]*(changedPrice[0]+ (changedPrice[0]*finalFloatingCharges/100) - (changedPrice[0]*finalFloatingDisc/100))+ finalFixedCharges - finalFixedDisc+"");
            }
        });
        dialog.findViewById(R.id.imageButton8).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qtyToAdd[0]++;
                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
                //((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *changedPrice[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView71)).setText(round(qtyToAdd[0] *changedCharge[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView72)).setText(round(qtyToAdd[0] *changedDisc[0])+"");
                ((TextView)dialog.findViewById(R.id.textView76)).setText(qtyToAdd[0]*(changedPrice[0]+ (changedPrice[0]*finalFloatingCharges/100) - (changedPrice[0]*finalFloatingDisc/100))+ finalFixedCharges - finalFixedDisc+"");
            }
        });

        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.UOM);
        ((EditText)dialog.findViewById(R.id.editText)).setText(round(selectedProduct.salesPrice,2)+"");
        ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *selectedProduct.salesPrice,2)+"");

        dialog.findViewById(R.id.textView55).setVisibility(View.GONE);
        dialog.findViewById(R.id.editText2).setVisibility(View.GONE);
        dialog.findViewById(R.id.textView71).setVisibility(View.GONE);

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
        for (int i = 0; i < thisCharges.size(); i++) {
            //docTotal += selectedProduct.charges.get(i).amount;
            switch (i){
                case 0:
                    ((TextView)dialog.findViewById(R.id.textView55)).setText(thisCharges.get(i).chargeName+ (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText)dialog.findViewById(R.id.editText2)).setText(round(thisCharges.get(i).chargeValue,2)+"");
                    double total = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).chargeValue : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
                    ((TextView)dialog.findViewById(R.id.textView71)).setText(round(total,2)+"");

                    dialog.findViewById(R.id.textView55).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText2).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView71).setVisibility(View.VISIBLE);
                    break;
                case 1:
                    ((TextView)dialog.findViewById(R.id.textView63)).setText(thisCharges.get(i).chargeName+ (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText)dialog.findViewById(R.id.editText3)).setText(round(thisCharges.get(i).chargeValue,2)+"");
                    double total1 = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).chargeValue : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
                    ((TextView)dialog.findViewById(R.id.textView72)).setText(round(total1,2)+"");

                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
                    break;

                case 2:
                    ((TextView) dialog.findViewById(R.id.textView64)).setText(thisCharges.get(i).chargeName+ (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText4)).setText(round(thisCharges.get(i).chargeValue,2)+"");
                    double total2 = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).chargeValue : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
                    ((TextView) dialog.findViewById(R.id.textView73)).setText(round(total2,2) + "");

                    dialog.findViewById(R.id.textView64).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText4).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView73).setVisibility(View.VISIBLE);
                    break;
                case 3:
                    ((TextView) dialog.findViewById(R.id.textView65)).setText(thisCharges.get(i).chargeName+ (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText5)).setText(round(thisCharges.get(i).chargeValue,2)+"");
                    double total3 = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).chargeValue : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;

                    ((TextView) dialog.findViewById(R.id.textView74)).setText(round(total3,2) + "");
                    dialog.findViewById(R.id.textView65).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText5).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView74).setVisibility(View.VISIBLE);
                    break;
                case 4:
                    ((TextView) dialog.findViewById(R.id.textView69)).setText(thisCharges.get(i).chargeName+ (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText8)).setText(round(thisCharges.get(i).chargeValue,2)+"");
                    double total4 = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).chargeValue : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;

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

                double cost = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText)).getText().toString());
//                double charge = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());
//                double disc = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());


                double fixedCharge = 0;
                double floatingCharge = 0;
                double fixedDiscount = 0;
                double floatingDiscount = 0;


//                for (int i = 0; i < thisCharges.size(); i++) {
//                    switch (i){
//                        case 0:
//                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());
//
//                            if (thisCharges.get(i).chargeType.equals("Tax")){
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingCharge;
//                                }
//                            } else{
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingDiscount;
//                                }
//                            }
//
//                            break;
//                        case 1:
//                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());
//                            if (thisCharges.get(i).chargeType.equals("Tax")){
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingCharge;
//                                }
//                            } else{
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingDiscount;
//                                }
//                            }
//                            break;
//                        case 2:
//                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText4)).getText().toString());
//                            if (thisCharges.get(i).chargeType.equals("Tax")){
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingCharge;
//                                }
//                            } else{
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingDiscount;
//                                }
//                            }
//                            break;
//                        case 3:
//                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText5)).getText().toString());
//                            if (thisCharges.get(i).chargeType.equals("Tax")){
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingCharge;
//                                }
//                            } else{
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingDiscount;
//                                }
//                            }
//                            break;
//                        case 4:
//                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText8)).getText().toString());
//                            if (thisCharges.get(i).chargeType.equals("Tax")){
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingCharge;
//                                }
//                            } else{
//                                if(thisCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += thisCharges.get(i).chargeValue;
//                                    thisCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
//                                    thisCharges.get(i).amount = floatingDiscount;
//                                }
//                            }
//                            break;
//                    }
//                }

                for (int i = 0; i < thisCharges.size(); i++) {
                    switch (i) {
                        case 0:
                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());

                            if (thisCharges.get(i).chargeType.equals("Tax")) {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            }

                            break;
                        case 1:
                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());
                            if (thisCharges.get(i).chargeType.equals("Tax")) {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            }
                            break;
                        case 2:
                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText4)).getText().toString());
                            if (thisCharges.get(i).chargeType.equals("Tax")) {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            }
                            break;
                        case 3:
                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText5)).getText().toString());
                            if (thisCharges.get(i).chargeType.equals("Tax")) {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            }
                            break;
                        case 4:
                            thisCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText8)).getText().toString());
                            if (thisCharges.get(i).chargeType.equals("Tax")) {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = thisCharges.get(i).chargeValue;
                                } else {
                                    floatingDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            }
                            break;
                    }
                }

//                }for (int i = 0; i < productCharges.size(); i++) {
//                    switch (i){
//                        case 0:
//                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());
//
//                            if (productCharges.get(i).chargeType.equals("Tax")){
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            } else{
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            }
//
//                            break;
//                        case 1:
//                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());
//                            if (productCharges.get(i).chargeType.equals("Tax")){
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            } else{
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            }
//                            break;
//                        case 2:
//                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText4)).getText().toString());
//                            if (productCharges.get(i).chargeType.equals("Tax")){
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            } else{
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            }
//                            break;
//                        case 3:
//                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText5)).getText().toString());
//                            if (productCharges.get(i).chargeType.equals("Tax")){
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            } else{
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            }
//                            break;
//                        case 4:
//                            productCharges.get(i).chargeValue = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText8)).getText().toString());
//                            if (productCharges.get(i).chargeType.equals("Tax")){
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedCharge;
//                                } else {
//                                    floatingCharge += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            } else{
//                                if(productCharges.get(i).chargeValueType.equals("Fixed")){
//                                    fixedDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = fixedDiscount;
//                                } else {
//                                    floatingDiscount += productCharges.get(i).chargeValue;
//                                    productCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * productCharges.get(i).chargeValue/100;
//                                }
//                            }
//                            break;
//                    }
//                };

                for (int i = 0; i < thisCharges.size(); i++) {
                    Log.d("Updated Charges", thisCharges.get(i).chargeName + " : "+ thisCharges.get(i).chargeValue);
                }

                selectedProduct.charges = thisCharges;
                selectedProduct.salesPrice = cost;
                selectedProduct.qty = qtyToAdd[0];
                selectedProduct.tax = fixedCharge + floatingCharge;
                selectedProduct.discount = fixedDiscount + floatingDiscount;
                selectedProduct.total = qtyToAdd[0]*(cost+ (cost*floatingCharge/100) - (cost*floatingDiscount/100))+ fixedCharge - fixedDiscount;

                for (int i = 0; i < cartProducts.size(); i++) {
                    if (selectedProduct.name.matches(cartProducts.get(i).name)) {
                        //cartProducts.remove(i);
                        cartProducts.set(i, selectedProduct);
                        break;
                    }
                }

                //setUpProducts();
                switch (pos) {
                    case 0:
                        ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd[0] +"");

                        ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(round(cost,2)+"");
                        updateProducts();
                        setCharges();
                        try {
                            getChargeList();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

//                        ((TextView)findViewById(R.id.tvChargesProduct1)).setText(round(fixedCharge + (qtyToAdd[0]*cost*floatingCharge/100),2)+"");
//                        ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(round(fixedDiscount + (qtyToAdd[0]*cost*floatingDiscount/100),2)+"");
//
//
//                        ((TextView)findViewById(R.id.tvTotalCostProduct1)).setText(round(selectedProduct.total,2) +"");

                        break;
                    case 1:
                        ((TextView)findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
                        ((TextView)findViewById(R.id.tvSalesPriceProduct2)).setText(round(cost,2)+"");
                        updateProducts();
                        setCharges();
                        try {
                            getChargeList();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
//                        ((TextView)findViewById(R.id.tvChargesProduct2)).setText(round(fixedCharge + (qtyToAdd[0]*cost*floatingCharge/100),2)+"");
//                        ((TextView)findViewById(R.id.tvDiscountProduct2)).setText(round(fixedDiscount + (qtyToAdd[0]*cost*floatingDiscount/100),2)+"");
//
//                        ((TextView)findViewById(R.id.tvTotalCostProduct2)).setText(round(selectedProduct.total,2) +"");

                        break;
                    case 2:
                        ((TextView)findViewById(R.id.tvQty3)).setText(qtyToAdd[0] +"");
                        ((TextView)findViewById(R.id.tvSalesPriceProduct3)).setText(round(cost,2)+"");
                        updateProducts();
                        setCharges();
                        try {
                            getChargeList();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
//                        ((TextView)findViewById(R.id.tvChargesProduct3)).setText(round(fixedCharge + (qtyToAdd[0]*cost*floatingCharge/100),2)+"");
//                        ((TextView)findViewById(R.id.tvDiscountProduct3)).setText(round(fixedDiscount + (qtyToAdd[0]*cost*floatingDiscount/100),2)+"");
//                        ((TextView)findViewById(R.id.tvTotalCostProduct3)).setText(round(selectedProduct.total,2) +"");

                        break;
                }

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
//                    ((TextView) dialog.findViewById(R.id.textView52)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString())) + "");
//                    selectedProduct.salesPrice = Double.parseDouble(charSequence.toString());
//                    selectedProduct.total = (selectedProduct.qty * Double.parseDouble(charSequence.toString())) + selectedProduct.tax - selectedProduct.discount+finalDocTotal;
//                    changedPrice[0] = Double.parseDouble(charSequence.toString());
//                }
//
//                ((TextView) dialog.findViewById(R.id.textView76)).setText(round((changedPrice[0]+changedCharge[0])* qtyToAdd[0]) +"");
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
//                    ((TextView) dialog.findViewById(R.id.textView71)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString())) + "");
//                    boolean isTax = true;
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
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round((changedCharge[0]+changedPrice[0])* qtyToAdd[0]) +"");
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
//                    ((TextView) dialog.findViewById(R.id.textView72)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString())) + "");
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
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round((changedCharge[0]+changedPrice[0])* qtyToAdd[0]) +"");
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
//                    ((TextView) dialog.findViewById(R.id.textView73)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString())) + "");
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
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round(selectedProduct.total) +"");
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
//                    ((TextView) dialog.findViewById(R.id.textView74)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString())) + "");
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
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round(selectedProduct.total) +"");
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
//                    ((TextView) dialog.findViewById(R.id.textView75)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString())) + "");
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
//                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round(selectedProduct.total) +"");
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

        ((TextView)dialog.findViewById(R.id.textView76)).setText(round(selectedProduct.total,2)+"");


        dialog.show();
    }
//    private void showEditDialog(CartProduct selectedProduct, int pos) {
//
//        double[] changedPrice = {selectedProduct.salesPrice};
//        double[] changedCharge = {selectedProduct.tax};
//        double[] changedDisc = {selectedProduct.discount};
//
//        int[] qtyToAdd = {selectedProduct.qty};
//
//        Log.d("Duplicate", "Opened Edit Box for: "+pos);
//
//        Log.d("2512", "Initial Charges: "+changedPrice[0] +" "+ changedCharge[0] + " " + qtyToAdd[0]);
//
//        Log.d("2512", "showEditDialog: Current Product List Attempting to edit "+pos);
//        for (int i = 0; i < cartProducts.size(); i++) {
//            Log.d("2512", i + " "+cartProducts.get(i).name + " " + cartProducts.get(i).salesPrice + " " + cartProducts.get(i).tax + " " +cartProducts.get(i).total + " ");
//        }
//
//        dialog.findViewById(R.id.imageButton7).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (qtyToAdd[0] >1) qtyToAdd[0]--;
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *changedPrice[0],2)+"");
//                ((TextView)dialog.findViewById(R.id.textView71)).setText(round(qtyToAdd[0] *changedCharge[0],2)+"");
//                ((TextView)dialog.findViewById(R.id.textView72)).setText(round(qtyToAdd[0] *changedDisc[0],2)+"");
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(round((qtyToAdd[0] *(changedPrice[0]+changedCharge[0]-changedDisc[0])),2)+"");
//
//            }
//        });
//        dialog.findViewById(R.id.imageButton8).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                qtyToAdd[0]++;
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *changedPrice[0],2)+"");
//                ((TextView)dialog.findViewById(R.id.textView71)).setText(round(qtyToAdd[0] *changedCharge[0],2)+"");
//                ((TextView)dialog.findViewById(R.id.textView72)).setText(round(qtyToAdd[0] *changedDisc[0],2)+"");
//
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(round((qtyToAdd[0] *(changedPrice[0]+changedCharge[0]-changedDisc[0])),2)+"");
//            }
//        });
//
//
//        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
//        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
//        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.UOM);
//        ((EditText)dialog.findViewById(R.id.editText)).setText(round(selectedProduct.salesPrice,2)+"");
//        ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *selectedProduct.salesPrice,2)+"");
//
//        dialog.findViewById(R.id.textView63).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText3).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView72).setVisibility(View.GONE);
//
//        double  docTotal = 0.0;
//        for (int i = 0; i < 2; i++) {
//            docTotal += docCharges.get(i).amount;
//            switch (i){
//                case 0:
//                    ((TextView)dialog.findViewById(R.id.textView55)).setText("Charges");
//                    ((EditText)dialog.findViewById(R.id.editText2)).setText(round(selectedProduct.tax,2)+"");
//                    ((TextView)dialog.findViewById(R.id.textView71)).setText(round(qtyToAdd[0] *selectedProduct.tax,2)+"");
//                    break;
//                case 1:
//                    ((TextView)dialog.findViewById(R.id.textView63)).setText("Discounts");
//                    ((EditText)dialog.findViewById(R.id.editText3)).setText(round(selectedProduct.discount,2)+"");
//                    ((TextView)dialog.findViewById(R.id.textView72)).setText(round(qtyToAdd[0] *selectedProduct.discount,2)+"");
//
//                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
//                    break;
//
//                case 2:
//                    ((TextView) dialog.findViewById(R.id.textView64)).setText(docCharges.get(2).name);
//                    ((EditText) dialog.findViewById(R.id.editText4)).setText(round(docCharges.get(2).amount,2) + "");
//                    ((TextView) dialog.findViewById(R.id.textView73)).setText(round(docCharges.get(2).amount,2) + "");
//
//                    dialog.findViewById(R.id.textView64).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText4).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView73).setVisibility(View.VISIBLE);
//                    break;
//                case 3:
//                    ((TextView) dialog.findViewById(R.id.textView65)).setText(docCharges.get(3).name);
//                    ((EditText) dialog.findViewById(R.id.editText5)).setText(round(docCharges.get(3).amount,2) + "");
//                    ((TextView) dialog.findViewById(R.id.textView74)).setText(round(docCharges.get(3).amount,2) + "");
//
//                    dialog.findViewById(R.id.textView65).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText5).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView74).setVisibility(View.VISIBLE);
//                    break;
//                case 4:
//                    ((TextView) dialog.findViewById(R.id.textView69)).setText(docCharges.get(4).name);
//                    ((EditText) dialog.findViewById(R.id.editText8)).setText(round(docCharges.get(4).amount,2) + "");
//                    ((TextView) dialog.findViewById(R.id.textView75)).setText(round(docCharges.get(4).amount,2) + "");
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
////                dialog.findViewById(R.id.imageButton7).setOnClickListener(null);
////                dialog.findViewById(R.id.imageButton8).setOnClickListener(null);
////
////                dialog.findViewById(R.id.button16).setOnClickListener(null);
////                dialog.findViewById(R.id.button14).setOnClickListener(null);
////                ((EditText) dialog.findViewById(R.id.editText)).addTextChangedListener(null);
////                ((EditText) dialog.findViewById(R.id.editText2)).addTextChangedListener(null);
////                ((EditText) dialog.findViewById(R.id.editText3)).addTextChangedListener(null);
////                ((EditText) dialog.findViewById(R.id.editText4)).addTextChangedListener(null);
////                ((EditText) dialog.findViewById(R.id.editText5)).addTextChangedListener(null);
////                ((EditText) dialog.findViewById(R.id.editText8)).addTextChangedListener(null);
//                dialog.dismiss();
//            }
//        });
//
//        dialog.findViewById(R.id.button14).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                double cost = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText)).getText().toString());
//                double charge = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText2)).getText().toString());
//                double disc = Double.parseDouble(((EditText) dialog.findViewById(R.id.editText3)).getText().toString());
//
//                selectedProduct.salesPrice = cost;
//                selectedProduct.qty = qtyToAdd[0];
//                selectedProduct.tax = charge;
//                selectedProduct.discount = disc;
//                selectedProduct.total = qtyToAdd[0] * (cost+charge-disc);
//
//               cartProducts.set(pos,selectedProduct);
////                for (int i = 0; i < cartProducts.size(); i++) {
////                    if (selectedProduct.name.matches(cartProducts.get(i).name)) {
////                        //cartProducts.remove(i);
////                        cartProducts.set(i, selectedProduct);
////                        break;
////                    }
////                }
//
//                Log.d("2512", "showEditDialog: Updated Product List Edited "+pos);
//                for (int i = 0; i < cartProducts.size(); i++) {
//                    Log.d("2512", i + " "+cartProducts.get(i).name + " " + cartProducts.get(i).salesPrice + " " + cartProducts.get(i).tax + " " +cartProducts.get(i).total + " ");
//                }
//                //updateProducts();
//
//                switch (pos) {
//                    case 0:
//                        ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd[0] +"");
//
//                        ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(round(cost,2)+"");
//                        updateProducts();
//                        setCharges();
//                        try {
//                            getChargeList();
//                        } catch (UnsupportedEncodingException e) {
//                            e.printStackTrace();
//                        }
//                        ((TextView)findViewById(R.id.tvChargesProduct1)).setText(round(charge,2)+"");
//                        ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(round(disc,2)+"");
//
//                        ((TextView)findViewById(R.id.tvTotalCostProduct1)).setText(round((cost+charge-disc)* qtyToAdd[0],2) +"");
//
//                        break;
//                    case 1:
//                        ((TextView)findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
//
//                        ((TextView)findViewById(R.id.tvSalesPriceProduct2)).setText(round(cost,2)+"");
//                        updateProducts();
//                        setCharges();
//                        try {
//                            getChargeList();
//                        } catch (UnsupportedEncodingException e) {
//                            e.printStackTrace();
//                        }
//                        ((TextView)findViewById(R.id.tvChargesProduct2)).setText(round(charge,2)+"");
//                        ((TextView)findViewById(R.id.tvDiscountProduct2)).setText(round(disc,2)+"");
//
//                        ((TextView)findViewById(R.id.tvTotalCostProduct2)).setText(round((cost+charge-disc)* qtyToAdd[0],2) +"");
//
//                        break;
//                    case 2:
//                        ((TextView)findViewById(R.id.tvQty3)).setText(qtyToAdd[0] +"");
//
//                        ((TextView)findViewById(R.id.tvSalesPriceProduct3)).setText(round(cost,2)+"");
//                        updateProducts();
//                        setCharges();
//                        try {
//                            getChargeList();
//                        } catch (UnsupportedEncodingException e) {
//                            e.printStackTrace();
//                        }
//                        ((TextView)findViewById(R.id.tvChargesProduct3)).setText(round(charge,2)+"");
//                        ((TextView)findViewById(R.id.tvDiscountProduct3)).setText(round(disc,2)+"");
//
//                        ((TextView)findViewById(R.id.tvTotalCostProduct3)).setText(round((cost+charge-disc)* qtyToAdd[0],2) +"");
//
//                        break;
//                }
//
//                setCharges();
//                try {
//                    getChargeList();
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
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
////
////                Log.d("Duplicate", "onTextChanged started for sales price for prduct no."+pos + " value: " +charSequence.toString());
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView52)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString()),2) + "");
////                    selectedProduct.salesPrice = Double.parseDouble(charSequence.toString());
////                    selectedProduct.total = (selectedProduct.qty * Double.parseDouble(charSequence.toString())) + selectedProduct.tax - selectedProduct.discount+finalDocTotal;
////                    changedPrice[0] = Double.parseDouble(charSequence.toString());
////                }
////
////                ((TextView) dialog.findViewById(R.id.textView76)).setText(round((changedCharge[0]+changedPrice[0])* qtyToAdd[0],2) +"");
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
////                Log.d("Duplicate", "onTextChanged started for tax1 for prduct no."+pos + " value: " +charSequence.toString());
////
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView71)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString()),2) + "");
////                    boolean isTax = true;
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
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round((changedCharge[0]+changedPrice[0])* qtyToAdd[0],2) +"");
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
////                Log.d("Duplicate", "onTextChanged started for tax2 for prduct no."+pos + " value: " +charSequence.toString());
////
////                if (!charSequence.toString().isEmpty() && charSequence.toString().matches("[.0-9]+")) {
////                    ((TextView) dialog.findViewById(R.id.textView72)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString()),2) + "");
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
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round((changedCharge[0]+changedPrice[0])* qtyToAdd[0],2) +"");
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
////                    ((TextView) dialog.findViewById(R.id.textView73)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString()),2) + "");
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
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round(selectedProduct.total,2) +"");
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
////                    ((TextView) dialog.findViewById(R.id.textView74)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString()),2) + "");
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
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round(selectedProduct.total,2) +"");
////
////                }
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
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
////                    ((TextView) dialog.findViewById(R.id.textView75)).setText(round(selectedProduct.qty * Double.parseDouble(charSequence.toString()),2) + "");
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
////                    ((TextView) dialog.findViewById(R.id.textView76)).setText(round(selectedProduct.total,2) +"");
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
//        ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd[0] *(selectedProduct.salesPrice+selectedProduct.tax-selectedProduct.discount),2)+"");
//        dialog.show();
//    }

//    private void showEditDialog(CartProduct selectedProduct) {
////        dialog.findViewById(R.id.imageButton7).setOnClickListener(this);
////        dialog.findViewById(R.id.imageButton8).setOnClickListener(this);
//
//
//        dialog.findViewById(R.id.button16).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dialog.dismiss();
//            }
//        });
//        ((TextView) dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
//        ((TextView) dialog.findViewById(R.id.tvQty2)).setText(selectedProduct.qty + "");
//        ((TextView) dialog.findViewById(R.id.textView47)).setText(selectedProduct.UOM);
//        ((EditText) dialog.findViewById(R.id.editText)).setText(selectedProduct.salesPrice + "");
//        ((TextView) dialog.findViewById(R.id.textView52)).setText(selectedProduct.qty * selectedProduct.salesPrice + "");
//        dialog.findViewById(R.id.textView63).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText3).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView72).setVisibility(View.GONE);
//
//        double docTotal = 0.0;
//        for (int i = 0; i < docCharges.size(); i++) {
//            docTotal += docCharges.get(i).amount;
//            switch (i) {
//                case 0:
//                    ((TextView) dialog.findViewById(R.id.textView55)).setText(docCharges.get(i).name);
//                    ((EditText) dialog.findViewById(R.id.editText2)).setText(docCharges.get(i).amount + "");
//                    ((TextView) dialog.findViewById(R.id.textView71)).setText(docCharges.get(i).amount + "");
//                    break;
//                case 1:
//                    ((TextView) dialog.findViewById(R.id.textView63)).setText(docCharges.get(1).name);
//                    ((EditText) dialog.findViewById(R.id.editText3)).setText(docCharges.get(1).amount + "");
//                    ((TextView) dialog.findViewById(R.id.textView72)).setText(docCharges.get(1).amount + "");
//
//                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
//                    break;
//                case 2:
//                    ((TextView) dialog.findViewById(R.id.textView64)).setText(docCharges.get(2).name);
//                    ((EditText) dialog.findViewById(R.id.editText4)).setText(docCharges.get(2).amount + "");
//                    ((TextView) dialog.findViewById(R.id.textView73)).setText(docCharges.get(2).amount + "");
//
//                    dialog.findViewById(R.id.textView64).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText4).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView73).setVisibility(View.VISIBLE);
//                    break;
//                case 3:
//                    ((TextView) dialog.findViewById(R.id.textView65)).setText(docCharges.get(3).name);
//                    ((EditText) dialog.findViewById(R.id.editText5)).setText(docCharges.get(3).amount + "");
//                    ((TextView) dialog.findViewById(R.id.textView74)).setText(docCharges.get(3).amount + "");
//
//                    dialog.findViewById(R.id.textView65).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText5).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView74).setVisibility(View.VISIBLE);
//                    break;
//                case 4:
//                    ((TextView) dialog.findViewById(R.id.textView69)).setText(docCharges.get(4).name);
//                    ((EditText) dialog.findViewById(R.id.editText8)).setText(docCharges.get(4).amount + "");
//                    ((TextView) dialog.findViewById(R.id.textView75)).setText(docCharges.get(4).amount + "");
//
//                    dialog.findViewById(R.id.textView69).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText8).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView75).setVisibility(View.VISIBLE);
//                    break;
//            }
//        }
//
//        ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.qty * selectedProduct.salesPrice + docTotal + "");
//
//        double finalDocTotal = docTotal;
//        dialog.findViewById(R.id.imageButton8).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                selectedProduct.qty += 1;
//                selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.tax - selectedProduct.discount + finalDocTotal;
//                ((TextView) dialog.findViewById(R.id.tvQty2)).setText(selectedProduct.qty + "");
//                ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
//            }
//        });
//
//        dialog.findViewById(R.id.imageButton7).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                selectedProduct.qty -= 1;
//                selectedProduct.total = (selectedProduct.qty * selectedProduct.salesPrice) + selectedProduct.tax - selectedProduct.discount+finalDocTotal;
//                ((TextView) dialog.findViewById(R.id.tvQty2)).setText(selectedProduct.qty + "");
//
//                ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
//            }
//        });
//
//        dialog.findViewById(R.id.button14).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                for (int i = 0; i < cartProducts.size(); i++) {
//                    if (selectedProduct.name.matches(cartProducts.get(i).name)) {
//                        //cartProducts.remove(i);
//                        cartProducts.set(i, selectedProduct);
//                    }
//                }
//                updateProducts();
//                setCharges();
//                dialog.dismiss();
//            }
//        });
//
//        ((EditText) dialog.findViewById(R.id.editText)).addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!charSequence.toString().isEmpty()) {
//                    ((TextView) dialog.findViewById(R.id.textView52)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
//                    selectedProduct.salesPrice = Double.parseDouble(charSequence.toString());
//                    selectedProduct.total = (selectedProduct.qty * Double.parseDouble(charSequence.toString())) + selectedProduct.tax - selectedProduct.discount+finalDocTotal;
//                }
//
//                ((TextView) dialog.findViewById(R.id.textView76)).setText(selectedProduct.total +"");
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
//                if (!charSequence.toString().isEmpty()) {
//                    ((TextView) dialog.findViewById(R.id.textView71)).setText(selectedProduct.qty * Double.parseDouble(charSequence.toString()) + "");
//                    boolean isTax = docCharges.get(0).isApplied;
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
//        ((EditText) dialog.findViewById(R.id.editText3)).addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!charSequence.toString().isEmpty()) {
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
//        ((EditText) dialog.findViewById(R.id.editText4)).addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (!charSequence.toString().isEmpty()) {
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
//                if (!charSequence.toString().isEmpty()) {
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
//                if (!charSequence.toString().isEmpty()) {
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
//
//        dialog.show();
//    }

    void prepareProductPayload() {
        for (int i = 0; i < cartProducts.size(); i++) {
            productPayload += "{\n" +
                    "            \"Id\":" + (cartProducts.get(i).pID == null ? null : "\"" + cartProducts.get(i).pID + "\"") + ",\n" +
                    "            \"productId\": \"" + cartProducts.get(i).ID + "\",\n" +
                    "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
                    "            \"isDelete\": false,\n" +
                    "            \"quantity\": " + (double) cartProducts.get(i).qty + ",\n" +
                    "            \"status\": \"Open\",\n" +
                    "            \"allocatedQuantity\": 0.00,\n" +
                    "            \"salesPrice\": " + cartProducts.get(i).salesPrice + ",\n" +
                    "            \"discount\": "+getProductDiscount(cartProducts.get(i))+",\n" +
                    "            \"tax\": "+getProductTax(cartProducts.get(i))+"\n" +
                    "        }";
            productPayload += ",";

            for (int j = 0; j < cartProducts.get(i).charges.size(); j++) {
                productChargesPayload += "{\n" +
                        "            \"Id\":"+ (cartProducts.get(i).charges.get(j).id == null ? null : "\""+cartProducts.get(i).charges.get(j).id+"\"") +",\n" +
                        "            \"productId\": \""+cartProducts.get(i).pID+"\",\n" +
                        "            \"ChargeName\": \""+cartProducts.get(i).charges.get(j).chargeName+"\",\n" +
                        "            \"ChargeMaster\": \""+cartProducts.get(i).charges.get(j).chargeMaster+"\",\n" +
                        "            \"ChargeValue\": "+cartProducts.get(i).charges.get(j).chargeValue+",\n" +
                        "            \"ChargeValueType\": \""+cartProducts.get(i).charges.get(j).chargeValueType+"\",\n" +
                        "            \"ChargeType\": \""+cartProducts.get(i).charges.get(j).chargeType+"\",\n" +
                        "            \"TransactionAccount\": \""+cartProducts.get(i).charges.get(j).transactionAccount+"\",\n" +
                        "            \"Amount\": "+(cartProducts.get(i).charges.get(j).chargeValueType.equalsIgnoreCase("Fixed") ? cartProducts.get(i).charges.get(j).amount : (cartProducts.get(i).qty * cartProducts.get(i).salesPrice*cartProducts.get(i).charges.get(j).chargeValue)/100)+"\n" +
                        "        }";
                productChargesPayload += ",";
            }
        }

        for (int i = 0; i < removedProducts.size(); i++) {
            productPayload += "{\n" +
                    "            \"Id\": \"" + removedProducts.get(i).pID + "\",\n" +
                    "            \"productId\": \"" + removedProducts.get(i).ID + "\",\n" +
                    "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
                    "            \"isDelete\": true,\n" +
                    "            \"quantity\": " + (double) removedProducts.get(i).qty + ",\n" +
                    "            \"status\": \"Open\",\n" +
                    "            \"allocatedQuantity\": 0.00,\n" +
                    "            \"salesPrice\": " + removedProducts.get(i).salesPrice + ",\n" +
                    "            \"discount\": " + removedProducts.get(i).discount + ",\n" +
                    "            \"tax\": " + removedProducts.get(i).tax + "\n" +
                    "        }";
            productPayload += ",";
        }
        productPayload = productPayload.substring(0, productPayload.length() - 1);
        productChargesPayload = productChargesPayload.substring(0, productChargesPayload.length() - 1);
        Log.d("2515", "prepareProductPayload: " + productPayload);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()){
            case R.id.checkBox:
                docCharges.get(0).isApplied = b;
                setCharges();
                break;
            case R.id.checkBox3:
                docCharges.get(1).isApplied = b;
                setCharges();
                break;
            case R.id.checkBox4:
                docCharges.get(2).isApplied = b;
                setCharges();
                break;
            case R.id.checkBox5:
                docCharges.get(3).isApplied = b;
                setCharges();
                break;
            case R.id.checkBox6:
                docCharges.get(4).isApplied = b;
                setCharges();
                break;
        }
    }

    void prepareChargesPayload() {
        CheckBox[] boxes = {findViewById(R.id.checkBox),findViewById(R.id.checkBox3),findViewById(R.id.checkBox4),findViewById(R.id.checkBox5),findViewById(R.id.checkBox6)};

        for (int i = 0; i < docCharges.size(); i++) {
            docPayload += "        {\n" +
                    "            \"Id\": \"" + docCharges.get(i).id + "\",\n" +
                    "\t\t\t\"ChargeMaster\": \"" + docCharges.get(i).masterID + "\",\n" +
                    "            \"ChargeType\": " + (docCharges.get(i).isTax ? "\"Tax\"" : "\"Discount\"") + ",\n" +
                    "            \"Amount\": " + docCharges.get(i).amount + ",\n" +
                    "            \"AmountType\":"+(docCharges.get(i).isFloating ?"\"Floating\"":"\"Fixed\"")+",\n" +
                    "            \"IsApplied\": \""+(boxes[i].isChecked() ? "Yes" : "No")+"\",\n" +
                    "            \"Description\": \"" + docCharges.get(i).name + "\",\n" +
                    "            \"Value\": "+(docCharges.get(i).isFloating ? (Double.parseDouble(getSubtotal(cartProducts))+Double.parseDouble(getTax(cartProducts))-Double.parseDouble(getDiscount(cartProducts)))* (docCharges.get(i).amount)/100 :docCharges.get(i).amount)+ "\n" +
                    "        }";
            docPayload += ",";
        }

        if(docPayload.length() > 0) {
            docPayload = docPayload.substring(0, docPayload.length() - 1);
        }
        Log.d("2515", "prepareDocPayload: " + docPayload);
    }


    private String getPayload(){

        String payload = "{\n" +
                "   \n" +
                "    \"salesOrderTransactionChargeLst\": [\n" +
                productChargesPayload +
                "    ],\n" +
                "    \"salesOrderDocChargeLst\": [\n" +
                docPayload +
                "    ],\n" +
                "    \"salesOrderDetailLst\": [\n" +
                productPayload +
                "    ],\n" +
                " \"salesOrder\": \n" +
                "        {\n" +
                "\t\t\t\"Id\": \"" + orderObject.orderID + "\",\n" +
                "            \"State\": \"draft\",\n" +
                "            \"Status\": \"Draft\",\n" +
                "            \"DealerId\": \"" + orderObject.dealerID + "\",\n" +
                "            \"Narration\": \"" + orderObject.narration + "\",\n" +
                "            \"BranchId\": \"" + orderObject.branchID + "\",\n" +
                "            \"CustomerId\": \"" + orderObject.customerID + "\",\n" +
                "            \"TotalCost\": " + getTotal(Double.parseDouble(getSubtotal(cartProducts)), Double.parseDouble(getTax(cartProducts)), Double.parseDouble(getDiscount(cartProducts)), Double.parseDouble(getAddCost())-Double.parseDouble(getAddDiscount())) + ",\n" +
                "            \"Subtotal\": " + getSubtotal(cartProducts) + ",\n" +
                "            \"Tax\": " + getTax(orderObject.products) + ",\n" +
                "            \"Discount\": " + getDiscount(orderObject.products) + ",\n" +
                "            \"AdditionalChargeApplied\": " + getAddCost() + ",\n" +
                "            \"AdditionalDiscountApplied\": "+getAddDiscount()+"\n" +
                "        }\n" +
                "}";
        return payload;
    }

    private void submitPatch(String orderID) {
        String payload = "{\n" +
                "   \n" +
                "    \"salesOrderTransactionChargeLst\": [\n" +
                productChargesPayload +
                "    ],\n" +
                "    \"salesOrderDocChargeLst\": [\n" +
                docPayload +
                "    ],\n" +
                "    \"salesOrderDetailLst\": [\n" +
                productPayload +
                "    ],\n" +
                " \"salesOrder\": \n" +
                "        {\n" +
                "\t\t\t\"Id\": \"" + orderObject.orderID + "\",\n" +
                "            \"State\": \"draft\",\n" +
                "            \"Status\": \"" + orderID + "\",\n" +
                "            \"DealerId\": \"" + orderObject.dealerID + "\",\n" +
                "            \"Narration\": \"" + orderObject.narration + "\",\n" +
                "            \"BranchId\": \"" + orderObject.branchID + "\",\n" +
                "            \"CustomerId\": \"" + orderObject.customerID + "\",\n" +
                "            \"TotalCost\": " + getTotal(Double.parseDouble(getSubtotal(cartProducts)), Double.parseDouble(getTax(cartProducts)), Double.parseDouble(getDiscount(cartProducts)), Double.parseDouble(getAddCost())-Double.parseDouble(getAddDiscount())) + ",\n" +
                "            \"Subtotal\": " + getSubtotal(cartProducts) + ",\n" +
                "            \"Tax\": " + getTax(orderObject.products) + ",\n" +
                "            \"Discount\": " + getDiscount(orderObject.products) + ",\n" +
                "            \"AdditionalChargeApplied\": " + getAddCost() + ",\n" +
                "            \"AdditionalDiscountApplied\": "+getAddDiscount()+"\n" +
                "        }\n" +
                "}";

        Log.d("2515", "submitOrder: " + payload);
        Log.d("2515", "submitPatch: Trying to update order Id:" + orderObject.orderID);
        final MediaType JSON
                = MediaType.get("application/json; charset=utf-8");

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(payload, JSON);
        Request request = new Request.Builder()
                .url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/")
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Log.d("2515", response.body().string());
            Log.d("2515", response.toString());
            //new TinyDB(getApplicationContext()).putBoolean("CloseNewOrder", true);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (orderID) {
                        case "Draft":
                            Toast.makeText(getApplicationContext(), "Order saved as Draft!", Toast.LENGTH_SHORT).show();
                            break;
                        case "Submitted For Approval":
                            Toast.makeText(getApplicationContext(), "Order submitted for approval!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            });
            new TinyDB(this).putBoolean("RefreshOrders", true);
            dialog_load.dismiss();
            finish();
        } catch (IOException e) {
            Log.d("2515", "submitPatch: " + e);
            e.printStackTrace();
        }
    }

    void updateProducts() {

        findViewById(R.id.cardView12).setVisibility(View.GONE);
        findViewById(R.id.cardView13).setVisibility(View.GONE);
        findViewById(R.id.cardView14).setVisibility(View.GONE);
        for (int i = 0; i < cartProducts.size(); i++) {

            String name = cartProducts.get(i).name;
            String uom = cartProducts.get(i).UOM;
            final int[] qty = {cartProducts.get(i).qty};
            final double[] salesPrice = {cartProducts.get(i).salesPrice};
            final double[] tax1 = {cartProducts.get(i).tax};
            final double[] disc1 = {cartProducts.get(i).discount};
            final double[] total = {cartProducts.get(i).total};

            double fixedCharges = 0.0;
            double floatingCharges = 0.0;
            double fixedDisc = 0.0;
            double floatingDisc = 0.0;

            CartProduct selectedProduct = cartProducts.get(i);
            for (int j = 0; j < selectedProduct.charges.size(); j++) {
                if(selectedProduct.charges.get(j).chargeType.equals("Tax")){
                    if(selectedProduct.charges.get(j).chargeValueType.equals("Fixed")){
                        fixedCharges += selectedProduct.charges.get(j).chargeValue;
                    } else floatingCharges += selectedProduct.charges.get(j).chargeValue;
                } else{
                    if(selectedProduct.charges.get(j).chargeValueType.equals("Fixed")){
                        fixedDisc += selectedProduct.charges.get(j).chargeValue;
                    } else floatingDisc += selectedProduct.charges.get(j).chargeValue;
                }
            }

            ConstraintLayout constraintLayout = findViewById(R.id.cvParent);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            double finalFloatingCharges = floatingCharges;
            double finalFloatingDisc = floatingDisc;
            double finalFixedDisc = fixedDisc;
            double finalFixedCharges = fixedCharges;

            switch (i) {
                case 0:

                    double perSalesPrice = salesPrice[0] / qty[0];
                    double perTax1 = tax1[0] / qty[0];
                    double perDOsc1 = disc1[0] / qty[0];
                    double perTotal = total[0] / qty[0];

                    findViewById(R.id.cardView12).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.tvProduct1Name)).setText(name);
                    ((TextView) findViewById(R.id.tvUOMProduct1)).setText(uom);
                    ((TextView) findViewById(R.id.tvQty)).setText(qty[0] + "");
                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + round(salesPrice[0],2));
                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                    ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + round(total[0],2) + "");

                    if (true) {

                        findViewById(R.id.imageButton4).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (qty[0] > 1) {
                                    qty[0]--;
//                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
//                                    tax1[0] = tax1[0] -perTax1;
//                                    disc1[0] = disc1[0] -perDOsc1;
                                    //total[0] = total[0] - perTotal;
                                    total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;
                                    ((TextView) findViewById(R.id.tvQty)).setText(qty[0] + "");
//                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                    ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + (round(total[0],2)) + "");

                                    for (int j = 0; j < cartProducts.size(); j++) {
                                        if (cartProducts.get(j).name.equals(name)) {
                                            cartProducts.get(j).qty = qty[0];
                                            cartProducts.get(j).total = total[0];
                                        }
                                    }

                                    setCharges();

                                    try {
                                        getChargeList();
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });

                        findViewById(R.id.imageButton5).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                qty[0]++;
//                                salesPrice[0] = salesPrice[0] +perSalesPrice;
//                                tax1[0] = tax1[0] +perTax1;
//                                disc1[0] = disc1[0] +perDOsc1;
                                //total[0] = total[0] + perTotal;
                                total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                ((TextView) findViewById(R.id.tvQty)).setText(qty[0] + "");
//                                ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+(salesPrice[0]));
                                ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + (round(total[0],2)) + "");

                                for (int j = 0; j < cartProducts.size(); j++) {
                                    if (cartProducts.get(j).name.equals(name)) {
                                        cartProducts.get(j).qty = qty[0];
                                        cartProducts.get(j).total = total[0];
                                    }
                                }

                                setCharges();

                                try {
                                    getChargeList();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        findViewById(R.id.bRemoveProduct1).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                findViewById(R.id.cardView12).setVisibility(View.GONE);
                                for (int j = 0; j < cartProducts.size(); j++) {
                                    if (cartProducts.get(j).name.equals(name)) {
                                        removedProducts.add(cartProducts.get(j));
                                        cartProducts.remove(cartProducts.get(j));
                                    }
                                }
                                setCharges();
                                try {
                                    getChargeList();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        findViewById(R.id.bEditProduct1).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                for (int j = 0; j < cartProducts.size(); j++) {
                                    if (cartProducts.get(j).name.equals(name)) {
                                        showEditDialog(cartProducts.get(j),0);
                                    }
                                }
                            }
                        });
                    }
//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                    break;

                case 1:

                    findViewById(R.id.cardView13).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.tvProduct2Name)).setText(name);
                    ((TextView) findViewById(R.id.tvUOMProduct2)).setText(uom);
                    ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
                    ((TextView) findViewById(R.id.tvSalesPriceProduct2)).setText("" + round(salesPrice[0],2) + "");
                    ((TextView) findViewById(R.id.tvChargesProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                    ((TextView) findViewById(R.id.tvDiscountProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                    ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + round(total[0],2) + "");


                    double perTotal1 = total[0] / qty[0];

                    if (true) {
                        findViewById(R.id.imageButton42).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (qty[0] > 1) {
                                    qty[0]--;
//                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
//                                    tax1[0] = tax1[0] -perTax1;
//                                    disc1[0] = disc1[0] -perDOsc1;
                                    //total[0] = total[0] - perTotal1;
                                    total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                    ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
//                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
                                    ((TextView) findViewById(R.id.tvChargesProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                    ((TextView) findViewById(R.id.tvDiscountProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                    ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0],2)) + "");

                                    for (int j = 0; j < cartProducts.size(); j++) {
                                        if (cartProducts.get(j).name.equals(name)) {
                                            cartProducts.get(j).qty = qty[0];
                                            cartProducts.get(j).total = total[0];
                                        }
                                    }

                                    setCharges();
                                    try {
                                        getChargeList();
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });

                        findViewById(R.id.imageButton52).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                qty[0]++;
//                                salesPrice[0] = salesPrice[0] +perSalesPrice;
//                                tax1[0] = tax1[0] +perTax1;
//                                disc1[0] = disc1[0] +perDOsc1;
                                //total[0] = total[0] + perTotal1;
                                total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
//                                ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+(salesPrice[0]));
                                ((TextView) findViewById(R.id.tvChargesProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                ((TextView) findViewById(R.id.tvDiscountProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0],2)) + "");

                                for (int j = 0; j < cartProducts.size(); j++) {
                                    if (cartProducts.get(j).name.equals(name)) {
                                        cartProducts.get(j).qty = qty[0];
                                        cartProducts.get(j).total = total[0];
                                    }
                                }

                                setCharges();
                                try {
                                    getChargeList();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        findViewById(R.id.bRemoveProduct2).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                findViewById(R.id.cardView13).setVisibility(View.GONE);
                                for (int j = 0; j < cartProducts.size(); j++) {
                                    if (cartProducts.get(j).name.equals(name)) {
                                        removedProducts.add(cartProducts.get(j));
                                        cartProducts.remove(cartProducts.get(j));
                                    }
                                }
                                setCharges();
                                try {
                                    getChargeList();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        findViewById(R.id.bEditProduct2).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                for (int j = 0; j < cartProducts.size(); j++) {
                                    if (cartProducts.get(j).name.equals(name)) {
                                        showEditDialog(cartProducts.get(j),1);
                                    }
                                }
                            }
                        });
                    }
//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                    break;

                case 2:

                    double perTotal3 = total[0] / qty[0];

                    findViewById(R.id.cardView14).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.tvProduct3Name)).setText(name);
                    ((TextView) findViewById(R.id.tvUOMProduct3)).setText(uom);
                    ((TextView) findViewById(R.id.tvQty3)).setText(qty[0] + "");
                    ((TextView) findViewById(R.id.tvSalesPriceProduct3)).setText("" + round(salesPrice[0],2) + "");
                    ((TextView) findViewById(R.id.tvChargesProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                    ((TextView) findViewById(R.id.tvDiscountProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                    ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + round(total[0],2) + "");

                    if (true) {

                        findViewById(R.id.imageButton43).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (qty[0] > 1) {
                                    qty[0]--;
//                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
//                                    tax1[0] = tax1[0] -perTax1;
//                                    disc1[0] = disc1[0] -perDOsc1;
                                    //total[0] = total[0] - perTotal3;
                                    total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                    ((TextView) findViewById(R.id.tvQty3)).setText(qty[0] + "");
//                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
                                    ((TextView) findViewById(R.id.tvChargesProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                    ((TextView) findViewById(R.id.tvDiscountProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                    ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0],2)) + "");

                                    for (int j = 0; j < cartProducts.size(); j++) {
                                        if (cartProducts.get(j).name.equals(name)) {
                                            cartProducts.get(j).qty = qty[0];
                                            cartProducts.get(j).total = total[0];
                                        }
                                    }

                                    setCharges();
                                    try {
                                        getChargeList();
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });

                        findViewById(R.id.imageButton53).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                qty[0]++;
//                                salesPrice[0] = salesPrice[0] +perSalesPrice;
//                                tax1[0] = tax1[0] +perTax1;
//                                disc1[0] = disc1[0] +perDOsc1;
                                //total[0] = total[0] + perTotal3;
                                total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                ((TextView) findViewById(R.id.tvQty3)).setText(qty[0] + "");
//                                ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+(salesPrice[0]));
                                ((TextView) findViewById(R.id.tvChargesProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                ((TextView) findViewById(R.id.tvDiscountProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0],2)) + "");

                                for (int j = 0; j < cartProducts.size(); j++) {
                                    if (cartProducts.get(j).name.equals(name)) {
                                        cartProducts.get(j).qty = qty[0];
                                        cartProducts.get(j).total = total[0];
                                    }
                                }

                                setCharges();
                                try {
                                    getChargeList();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        findViewById(R.id.bRemoveProduct3).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                findViewById(R.id.cardView14).setVisibility(View.GONE);
                                for (int j = 0; j < cartProducts.size(); j++) {
                                    if (cartProducts.get(j).name.equals(name)) {
                                        removedProducts.add(cartProducts.get(j));
                                        cartProducts.remove(cartProducts.get(j));
                                    }
                                }
                                setCharges();
                                try {
                                    getChargeList();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        findViewById(R.id.bEditProduct3).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                for (int j = 0; j < cartProducts.size(); j++) {
                                    if (cartProducts.get(j).name.equals(name)) {
                                        showEditDialog(cartProducts.get(j),2);
                                    }
                                }
                            }
                        });
                    }

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                    break;
            }

        }
    }

//    void updateProducts() {
//        for (int i = 0; i < cartProducts.size(); i++) {
//            String name = cartProducts.get(i).name;
//            String uom = cartProducts.get(i).UOM;
//            final int[] qty = {cartProducts.get(i).qty};
//            final double[] salesPrice = {cartProducts.get(i).salesPrice};
//            final double[] tax1 = {cartProducts.get(i).tax};
//            final double[] disc1 = {cartProducts.get(i).discount};
//            final double[] total = {cartProducts.get(i).total};
//
//            ConstraintLayout constraintLayout = findViewById(R.id.cvParent);
//            ConstraintSet constraintSet = new ConstraintSet();
//            constraintSet.clone(constraintLayout);
//
//            switch (i) {
//                case 0:
//
//                    double perSalesPrice = salesPrice[0] / qty[0];
//                    double perTax1 = tax1[0] / qty[0];
//                    double perDOsc1 = disc1[0] / qty[0];
//                    double perTotal = total[0] / qty[0];
//
//                    findViewById(R.id.cardView12).setVisibility(View.VISIBLE);
//                    ((TextView) findViewById(R.id.tvProduct1Name)).setText(name);
//                    ((TextView) findViewById(R.id.tvUOMProduct1)).setText(uom);
//                    ((TextView) findViewById(R.id.tvQty)).setText(qty[0] + "");
//                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + round(salesPrice[0],2));
//                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (round(tax1[0],2)) + "");
//                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +(round(disc1[0],2)) + "");
//                    ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + (round(total[0],2)) + "");
//
//                    if (canEdit) {
//                        findViewById(R.id.imageButton4).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                if (qty[0] > 1) {
//                                    qty[0]--;
////                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
////                                    tax1[0] = tax1[0] -perTax1;
////                                    disc1[0] = disc1[0] -perDOsc1;
//                                    total[0] = total[0] - perTotal;
//
//                                    ((TextView) findViewById(R.id.tvQty)).setText(qty[0] + "");
////                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
////                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (tax1[0]) + "");
////                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +  (disc1[0]) + "");
//                                    ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + Double.parseDouble(round(total[0], 2)) + "");
//
//                                    for (int j = 0; j < cartProducts.size(); j++) {
//                                        if (cartProducts.get(j).name.equals(name)) {
//                                            cartProducts.get(j).qty = qty[0];
//                                            cartProducts.get(j).total = total[0];
//                                        }
//                                    }
//
//                                    setCharges();
//                                }
//                            }
//                        });
//
//                        findViewById(R.id.imageButton5).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                qty[0]++;
////                                salesPrice[0] = salesPrice[0] +perSalesPrice;
////                                tax1[0] = tax1[0] +perTax1;
////                                disc1[0] = disc1[0] +perDOsc1;
//                                total[0] = total[0] + perTotal;
//
//                                ((TextView) findViewById(R.id.tvQty)).setText(qty[0] + "");
////                                ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+(salesPrice[0]));
////                                ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+(tax1[0])+"");
////                                ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+(disc1[0])+"");
//                                ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + Double.parseDouble(round(total[0], 2)) + "");
//
//                                for (int j = 0; j < cartProducts.size(); j++) {
//                                    if (cartProducts.get(j).name.equals(name)) {
//                                        cartProducts.get(j).qty = qty[0];
//                                        cartProducts.get(j).total = total[0];
//                                    }
//                                }
//
//                                setCharges();
//                            }
//                        });
//
//                        findViewById(R.id.bRemoveProduct1).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                findViewById(R.id.cardView12).setVisibility(View.GONE);
//                                for (int j = 0; j < cartProducts.size(); j++) {
//                                    if (cartProducts.get(j).name.equals(name)) {
//                                        removedProducts.add(cartProducts.get(j));
//                                        cartProducts.remove(cartProducts.get(j));
//                                    }
//                                }
//                                setCharges();
//                            }
//                        });
//
//                        findViewById(R.id.bEditProduct1).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                for (int j = 0; j < cartProducts.size(); j++) {
//                                    if (cartProducts.get(j).name.equals(name)) {
//                                        showEditDialog(cartProducts.get(j),0);
//                                    }
//                                }
//                            }
//                        });
//                    }
////                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
////                        constraintSet.applyTo(constraintLayout);
//                    break;
//
//                case 1:
//
//                    findViewById(R.id.cardView13).setVisibility(View.VISIBLE);
//                    ((TextView) findViewById(R.id.tvProduct2Name)).setText(name);
//                    ((TextView) findViewById(R.id.tvUOMProduct2)).setText(uom);
//                    ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
//                    ((TextView) findViewById(R.id.tvSalesPriceProduct2)).setText("" + Double.parseDouble(round(salesPrice[0],2)) + "");
//                    ((TextView) findViewById(R.id.tvChargesProduct2)).setText("" + Double.parseDouble(round(tax1[0],2)) + "");
//                    ((TextView) findViewById(R.id.tvDiscountProduct2)).setText("" + Double.parseDouble(round(disc1[0],2)) + "");
//                    ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + Double.parseDouble(round(total[0],2)) + "");
//
//
//                    double perTotal1 = total[0] / qty[0];
//
//                    if (canEdit) {
//                        findViewById(R.id.imageButton42).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                if (qty[0] > 1) {
//                                    qty[0]--;
////                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
////                                    tax1[0] = tax1[0] -perTax1;
////                                    disc1[0] = disc1[0] -perDOsc1;
//                                    total[0] = total[0] - perTotal1;
//
//                                    ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
////                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
////                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (tax1[0]) + "");
////                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +  (disc1[0]) + "");
//                                    ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0], 2)) + "");
//
//                                    for (int j = 0; j < cartProducts.size(); j++) {
//                                        if (cartProducts.get(j).name.equals(name)) {
//                                            cartProducts.get(j).qty = qty[0];
//                                            cartProducts.get(j).total = total[0];
//                                        }
//                                    }
//
//                                    setCharges();
//                                }
//                            }
//                        });
//
//                        findViewById(R.id.imageButton52).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                qty[0]++;
////                                salesPrice[0] = salesPrice[0] +perSalesPrice;
////                                tax1[0] = tax1[0] +perTax1;
////                                disc1[0] = disc1[0] +perDOsc1;
//                                total[0] = total[0] + perTotal1;
//
//                                ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
////                                ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+(salesPrice[0]));
////                                ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+(tax1[0])+"");
////                                ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+(disc1[0])+"");
//                                ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0], 2)) + "");
//
//                                for (int j = 0; j < cartProducts.size(); j++) {
//                                    if (cartProducts.get(j).name.equals(name)) {
//                                        cartProducts.get(j).qty = qty[0];
//                                        cartProducts.get(j).total = total[0];
//                                    }
//                                }
//
//                                setCharges();
//                            }
//                        });
//
//                        findViewById(R.id.bRemoveProduct2).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                findViewById(R.id.cardView13).setVisibility(View.GONE);
//                                for (int j = 0; j < cartProducts.size(); j++) {
//                                    if (cartProducts.get(j).name.equals(name)) {
//                                        removedProducts.add(cartProducts.get(j));
//                                        cartProducts.remove(cartProducts.get(j));
//                                    }
//                                }
//                                setCharges();
//                            }
//                        });
//
//                        findViewById(R.id.bEditProduct2).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                for (int j = 0; j < cartProducts.size(); j++) {
//                                    if (cartProducts.get(j).name.equals(name)) {
//                                        showEditDialog(cartProducts.get(j),1);
//                                    }
//                                }
//                            }
//                        });
//                    }
////                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
////                        constraintSet.applyTo(constraintLayout);
//                    break;
//
//                case 2:
//
//                    double perTotal3 = total[0] / qty[0];
//
//                    findViewById(R.id.cardView14).setVisibility(View.VISIBLE);
//                    ((TextView) findViewById(R.id.tvProduct3Name)).setText(name);
//                    ((TextView) findViewById(R.id.tvUOMProduct3)).setText(uom);
//                    ((TextView) findViewById(R.id.tvQty3)).setText(qty[0] + "");
//                    ((TextView) findViewById(R.id.tvSalesPriceProduct3)).setText("" + round(salesPrice[0],2) + "");
//                    ((TextView) findViewById(R.id.tvChargesProduct3)).setText("" + round(tax1[0],2) + "");
//                    ((TextView) findViewById(R.id.tvDiscountProduct3)).setText("" + round(disc1[0],2) + "");
//                    ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + round(total[0],2) + "");
//
//                    if (canEdit) {
//
//                        findViewById(R.id.imageButton43).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                if (qty[0] > 1) {
//                                    qty[0]--;
////                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
////                                    tax1[0] = tax1[0] -perTax1;
////                                    disc1[0] = disc1[0] -perDOsc1;
//                                    total[0] = total[0] - perTotal3;
//
//                                    ((TextView) findViewById(R.id.tvQty3)).setText(qty[0] + "");
////                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
////                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (tax1[0]) + "");
////                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +  (disc1[0]) + "");
//                                    ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0], 2)) + "");
//
//                                    for (int j = 0; j < cartProducts.size(); j++) {
//                                        if (cartProducts.get(j).name.equals(name)) {
//                                            cartProducts.get(j).qty = qty[0];
//                                            cartProducts.get(j).total = total[0];
//                                        }
//                                    }
//
//                                    setCharges();
//                                }
//                            }
//                        });
//
//                        findViewById(R.id.imageButton53).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                qty[0]++;
////                                salesPrice[0] = salesPrice[0] +perSalesPrice;
////                                tax1[0] = tax1[0] +perTax1;
////                                disc1[0] = disc1[0] +perDOsc1;
//                                total[0] = total[0] + perTotal3;
//
//                                ((TextView) findViewById(R.id.tvQty3)).setText(qty[0] + "");
////                                ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+(salesPrice[0]));
////                                ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+(tax1[0])+"");
////                                ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+(disc1[0])+"");
//                                ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0], 2)) + "");
//
//                                for (int j = 0; j < cartProducts.size(); j++) {
//                                    if (cartProducts.get(j).name.equals(name)) {
//                                        cartProducts.get(j).qty = qty[0];
//                                        cartProducts.get(j).total = total[0];
//                                    }
//                                }
//
//                                setCharges();
//                            }
//                        });
//
//                        findViewById(R.id.bRemoveProduct3).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                findViewById(R.id.cardView14).setVisibility(View.GONE);
//                                for (int j = 0; j < cartProducts.size(); j++) {
//                                    if (cartProducts.get(j).name.equals(name)) {
//                                        removedProducts.add(cartProducts.get(j));
//                                        cartProducts.remove(cartProducts.get(j));
//                                    }
//                                }
//                                setCharges();
//                            }
//                        });
//
//                        findViewById(R.id.bEditProduct3).setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                for (int j = 0; j < cartProducts.size(); j++) {
//                                    if (cartProducts.get(j).name.equals(name)) {
//                                        showEditDialog(cartProducts.get(j),2);
//                                    }
//                                }
//                            }
//                        });
//                    }
//
////                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
////                        constraintSet.applyTo(constraintLayout);
//
//                    break;
//            }
//
//        }
//    }

    private double getTotal(double subtotal, double tax, double discount, double otherCost) {
        //totalCost = subtotal + tax - discount + otherCost;
        return Double.parseDouble(round(subtotal + tax - discount + otherCost, 2));
    }

    private String getSubtotal(ArrayList<CartProduct> products) {
        double cost = 0;
        for (int i = 0; i < products.size(); i++) {
            cost += products.get(i).salesPrice * products.get(i).qty;
        }
        // subtotal = cost;
        return round(cost, 2);
    }

    private String getTax(ArrayList<CartProduct> products) {
        double cost = 0;

        Log.d("TAX", "Checking for number: "+ products.size());
        for (int i = 0; i < products.size(); i++) {
            double fixedCharges = 0.0;
            double floatingCharges = 0.0;
            Log.d("TAX", "Charges for Product : "+ products.get(i).name);
            for (int j = 0; j < products.get(i).charges.size(); j++) {
                Log.d("TAX", "Adding Tax: "+products.get(i).charges.get(j).chargeName +" of "+products.get(i).charges.get(j).chargeValue);
                if(products.get(i).charges.get(j).chargeType.equals("Tax")){
                    if(products.get(i).charges.get(j).chargeValueType.equals("Fixed")){
                        fixedCharges += products.get(i).charges.get(j).chargeValue;
                    } else floatingCharges += products.get(i).charges.get(j).chargeValue;
                }
                //Log.d("TAX", "Added Tax: "+(products.get(i).qty*products.get(i).salesPrice* floatingCharges/100) + fixedCharges);
            }
            cost +=  (products.get(i).qty*products.get(i).salesPrice* floatingCharges/100) + fixedCharges;
        }
        // tax = cost;
        Log.d("TAX", "Total Tax: "+cost);
        return round(cost, 2);
    }

    private String getDiscount(ArrayList<CartProduct> products) {
        double cost = 0;
        for (int i = 0; i < products.size(); i++) {

            for (int j = 0; j < products.get(i).charges.size(); j++) {
                double fixedDisc = 0.0;
                double floatingDisc = 0.0;
                if(!products.get(i).charges.get(j).chargeType.equals("Tax")){
                    if(products.get(i).charges.get(j).chargeValueType.equals("Fixed")){
                        fixedDisc += products.get(i).charges.get(j).chargeValue;
                    } else floatingDisc += products.get(i).charges.get(j).chargeValue;
                }
                cost += (products.get(i).qty*products.get(i).salesPrice* floatingDisc/100) + fixedDisc;
            }
        }
        // disc = cost;
        return round(cost, 2);
    }

    private String getAddDiscount() {
        double cost = 0;
        for (int i = 0; i < docCharges.size(); i++) {
            View[] views = {findViewById(R.id.checkBox), findViewById(R.id.checkBox3), findViewById(R.id.checkBox4), findViewById(R.id.checkBox5), findViewById(R.id.checkBox6)};
            if (((CheckBox)views[i]).isChecked() && !docCharges.get(i).isTax) {
                cost += docCharges.get(i).isFloating ? (Double.parseDouble(getSubtotal(cartProducts))+Double.parseDouble(getTax(cartProducts))-Double.parseDouble(getDiscount(cartProducts)))* (docCharges.get(i).amount)/100 : docCharges.get(i).amount;
            }
        }
        return round(cost, 2);
    }

    private String getAddCost() {
        double cost = 0;
        View[] views = {findViewById(R.id.checkBox), findViewById(R.id.checkBox3), findViewById(R.id.checkBox4), findViewById(R.id.checkBox5), findViewById(R.id.checkBox6)};

        for (int i = 0; i < docCharges.size(); i++) {
            if(((CheckBox)views[i]).isChecked() && docCharges.get(i).isTax) {
                cost += docCharges.get(i).isFloating ? (Double.parseDouble(getSubtotal(cartProducts))+Double.parseDouble(getTax(cartProducts))-Double.parseDouble(getDiscount(cartProducts)))* (docCharges.get(i).amount)/100 : docCharges.get(i).amount;

                Log.d("2512", "getAddCost: Adding"+ docCharges.get(i).amount);
            }
        }
        return round(cost, 2);
    }

    private double getProductDiscount(CartProduct products) {
        double cost = 0;
        for (int j = 0; j < products.charges.size(); j++) {
            double fixedDisc = 0.0;
            double floatingDisc = 0.0;
            if(!products.charges.get(j).chargeType.equals("Tax")){
                if(products.charges.get(j).chargeValueType.equals("Fixed")){
                    fixedDisc += products.charges.get(j).chargeValue;
                } else floatingDisc += products.charges.get(j).chargeValue;
            }
            cost += (products.qty*products.salesPrice* floatingDisc/100) + fixedDisc;
        }
        //disc = cost;
        return Double.parseDouble(round(cost,2));
    }

    private double getProductTax(CartProduct products) {
        double cost = 0;
        for (int j = 0; j < products.charges.size(); j++) {
            double fixedDisc = 0.0;
            double floatingDisc = 0.0;
            if(products.charges.get(j).chargeType.equals("Tax")){
                if(products.charges.get(j).chargeValueType.equals("Fixed")){
                    fixedDisc += products.charges.get(j).chargeValue;
                } else floatingDisc += products.charges.get(j).chargeValue;
            }
            cost += (products.qty*products.salesPrice* floatingDisc/100) + fixedDisc;
        }

        //disc = cost;
        return Double.parseDouble(round(cost,2));
    }

    public static String round(double value, int places) {
//        if (places < 0) throw new IllegalArgumentException();
//
//        BigDecimal bd = BigDecimal.valueOf(value);
//        bd = bd.setScale(places, RoundingMode.HALF_UP);
//        return bd.doubleValue();
        String val = String.valueOf((double) Math.round(value * 100) / 100);

        return ((val.charAt(val.length()-1)) == '0' || (val.charAt(val.length()-2)) == '.') ? val+"0":val;
    }

    void setCharges() {
        ((TextView) findViewById(R.id.textView95)).setText(getSubtotal(cartProducts) + "");
        ((TextView) findViewById(R.id.textView96)).setText(getTax(cartProducts) + "");
        ((TextView) findViewById(R.id.textView97)).setText(getDiscount(cartProducts) + "");
        ((TextView) findViewById(R.id.textView98)).setText(getAddDiscount() + "");
        ((TextView) findViewById(R.id.textView99)).setText(getAddCost() + "");
//        ((TextView)findViewById(R.id.textView96)).setText(getTax(orderObject.products)+"");
//        ((TextView)findViewById(R.id.textView96)).setText(getTax(orderObject.products)+"");
        double total = Double.parseDouble(getSubtotal(cartProducts)) + Double.parseDouble(getTax(cartProducts)) - Double.parseDouble(getDiscount(cartProducts))  - Double.parseDouble(getAddDiscount()) + Double.parseDouble(getAddCost());

        ((TextView) findViewById(R.id.textView102)).setText("₹ " + round(total, 2));
    }



    private void getChargeList() throws UnsupportedEncodingException {

        docChargesNew.clear();

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
                            Log.d("Checkbox", "Result :" + result.toString());

                            JSONArray record = result.asJSONObject().getJSONArray("records");
                            for (int i = 0; i < record.length(); i++) {
                                String id = record.getJSONObject(i).getString("Id");
                                String masterid =record.getJSONObject(i).getString("ChargeMaster__c");
                                JSONObject charge = record.getJSONObject(i).getJSONObject("ChargeMaster__r");
                                String desc = charge.getString("Description__c");

                                String type =  charge.getString("AmountType__c");
                                String disc1 =  charge.getString("ChargeType__c");
                                double amount = type.equals("Fixed") ? charge.getDouble("Amount__c") : (Double.parseDouble(getSubtotal(cartProducts))+Double.parseDouble(getTax(cartProducts))-Double.parseDouble(getDiscount(cartProducts)))* (charge.getDouble("Amount__c")/100);
                                DocCharge d = new DocCharge(desc,id, masterid, disc1.equals("Tax"),amount,type.equals("Floating"),disc1.equals("Tax"));
                                docChargesNew.add(d);
                            }

//                            docCharges.clear();
//                            docCharges.addAll(docChargesNew);
                            //docCharges = docChargesNew;
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    setCharges();
                                    for (int i = 0; i < docChargesNew.size(); i++) {

                                        String name = docChargesNew.get(i).name;
                                        double amount = docChargesNew.get(i).amount;
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
                                                ((TextView)findViewById(R.id.textView103)).setText(round(amount,2)+"");

                                                //((CheckBox)findViewById(R.id.checkBox)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                break;

                                            case 1:

                                                findViewById(R.id.checkBox3).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView104).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView105).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox3)).setText(name);
                                                ((TextView)findViewById(R.id.textView105)).setText(round(amount,2)+"");

                                                //((CheckBox)findViewById(R.id.checkBox3)).setChecked(isApplied);



//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                break;

                                            case 2:

                                                findViewById(R.id.checkBox4).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView106).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView107).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox4)).setText(name);
                                                ((TextView)findViewById(R.id.textView107)).setText(round(amount,2)+"");

                                                //((CheckBox)findViewById(R.id.checkBox4)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                                break;

                                            case 3:

                                                findViewById(R.id.checkBox5).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView108).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView109).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox5)).setText(name);
                                                ((TextView)findViewById(R.id.textView109)).setText(round(amount,2)+"");

                                                //((CheckBox)findViewById(R.id.checkBox5)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                break;

                                            case 4:
                                                findViewById(R.id.checkBox6).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView110).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView111).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox6)).setText(name);
                                                ((TextView)findViewById(R.id.textView111)).setText(round(amount,2)+"");

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
            onBackPressed();
            return true;
        }
        if (id == R.id.itemEditOrder) {
            //OrderObject orderObject =  new OrderObject(orderName, dealerID,branchName, branchID,customerName, customerID,state,narration,cartProducts);

            //Toast.makeText(this,"In Progress...", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(getApplicationContext(), UpdateOrderActivity.class);

            prepareProductPayload();
            prepareChargesPayload();

            orderObject.products = cartProducts;
            orderObject.docCharges = docCharges;
            orderObject.combinedCharges = combinedCharges;
            i.putExtra("order", orderObject);
            i.putExtra("removed", removedProducts);
            i.putParcelableArrayListExtra("docCharges", docCharges);
            i.putExtra("payload", getPayload());
            startActivity(i);

//            checkpermissions(this);
//            takeScreenshot(getWindow().getDecorView().getRootView());

        }
        return super.onOptionsItemSelected(item);
    }

    protected File takeScreenshot(View view) {
        Date date = new Date();
        try {
            String dirpath;
            // Initialising the directory of storage
            dirpath = SalesOrderDetails.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + '/' + getString(R.string.app_name);
            File file = new File(dirpath);
            if (!file.exists()) {
                boolean mkdir = file.mkdir();
            }
            // File name : keeping file name unique using data time.
            String path = dirpath + "/" + date.getTime() + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            File imageurl = new File(path);
            FileOutputStream outputStream = new FileOutputStream(imageurl);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
            outputStream.flush();
            outputStream.close();
            Log.d("2512", "takeScreenshot Path: " + imageurl);
            Toast.makeText(SalesOrderDetails.this, "" + imageurl, Toast.LENGTH_LONG).show();
            return imageurl;
        } catch (FileNotFoundException io) {
            io.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void connectSF() {
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
                                fetchOrderDetails(finalToken, orderID);
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
                .url("https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/" + orderID)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            //Log.d("2514", "Request URL: https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h0000048l32EAA"+ "\n" +" Token: "+token +"\n"+ " Response: "+response.body().string());

            //String result =
            JSONObject records = new JSONObject(response.body().string());

            //Log.d("2514", "Request URL: https://dms-dev-ed.my.salesforce.com/services/apexrest/DemoService/a1K3h0000048l32EAA"+ "\n" +" Token: "+token +"\n"+ " Response: "+records.toString());
            log("Response: " + records.toString());
            JSONArray infoArray = records.getJSONArray("salesOrderList");

            String orderName = infoArray.getJSONObject(0).getString("Name");
            String state = "Draft";
            if (infoArray.getJSONObject(0).has("Status__c")) {
                state = infoArray.getJSONObject(0).getString("Status__c");
            }

            if (!state.equals("Draft")) {
                canEdit = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new CartProductAdapter(cartProducts, removedProducts, SalesOrderDetails.this, canEdit);
                        findViewById(R.id.button19).setVisibility(View.GONE);
                        findViewById(R.id.button20).setVisibility(View.GONE);

                        ((ImageView)findViewById(R.id.imageView7)).setImageTintList(ColorStateList.valueOf(getColor(R.color.disbaled_color)));
                        ((ImageView)findViewById(R.id.imageView72)).setImageTintList(ColorStateList.valueOf(getColor(R.color.disbaled_color)));
                        ((ImageView)findViewById(R.id.imageView73)).setImageTintList(ColorStateList.valueOf(getColor(R.color.disbaled_color)));
                        ((ImageView)findViewById(R.id.imageView8)).setImageTintList(ColorStateList.valueOf(getColor(R.color.disbaled_color)));
                        ((ImageView)findViewById(R.id.imageView82)).setImageTintList(ColorStateList.valueOf(getColor(R.color.disbaled_color)));
                        ((ImageView)findViewById(R.id.imageView83)).setImageTintList(ColorStateList.valueOf(getColor(R.color.disbaled_color)));

                        ((CheckBox)findViewById(R.id.checkBox)).setClickable(false);
                        ((CheckBox)findViewById(R.id.checkBox3)).setClickable(false);
                        ((CheckBox)findViewById(R.id.checkBox4)).setClickable(false);
                        ((CheckBox)findViewById(R.id.checkBox5)).setClickable(false);
                        ((CheckBox)findViewById(R.id.checkBox6)).setClickable(false);
                    }
                });
            }

            String narration = "N/A";
            if (infoArray.getJSONObject(0).has("Narration__c")) {
                narration = infoArray.getJSONObject(0).getString("Narration__c");
            }

            String dealer = infoArray.getJSONObject(0).getJSONObject("Dealer__r").getString("Name");
            String dealerID = infoArray.getJSONObject(0).getJSONObject("Dealer__r").getString("Id");
            String customer = infoArray.getJSONObject(0).getJSONObject("Customer__r").getString("Name");
            String customerID = infoArray.getJSONObject(0).getJSONObject("Customer__r").getString("Id");
            String branch = infoArray.getJSONObject(0).getJSONObject("Branch__r").getString("Name");
            String branchID = infoArray.getJSONObject(0).getJSONObject("Branch__r").getString("Id");


            JSONArray productChargesList = records.getJSONArray("salesOrderTransactionChargeList");
            for (int i = 0; i < productChargesList.length(); i++) {
                String Id = productChargesList.getJSONObject(i).getString("Id");
                String pId = productChargesList.getJSONObject(i).getString("TransactionDetail__c");
                String name = productChargesList.getJSONObject(i).getString("ChargeName__c");
                String chargeMaster = productChargesList.getJSONObject(i).getString("ChargeMaster__c");
                String chargeValueType = productChargesList.getJSONObject(i).getString("ChargeValueType__c");
                String chargeType = productChargesList.getJSONObject(i).getString("ChargeType__c");
                String transactionID = productChargesList.getJSONObject(i).getString("TransactionAccount__c");
                Double amount = productChargesList.getJSONObject(i).getDouble("Amount__c");

                Double chargeValue = productChargesList.getJSONObject(i).getDouble("ChargeValue__c");
                ProductCharges p = new ProductCharges(Id,pId,name,chargeMaster,chargeValueType,chargeType,transactionID,chargeValue,amount);
                productCharges.add(p);
                combinedCharges.add(p);
            }

            JSONArray chargesList = records.getJSONArray("salesOrderDocChargesList");
            String chargeListText = "";
            CheckBox[] boxes = {findViewById(R.id.checkBox),findViewById(R.id.checkBox3),findViewById(R.id.checkBox4),findViewById(R.id.checkBox5),findViewById(R.id.checkBox6)};

            for (int i = 0; i < chargesList.length(); i++) {

                String desc = chargesList.getJSONObject(i).getString("Description__c");
                String id = chargesList.getJSONObject(i).getString("Id");
                String type = chargesList.getJSONObject(i).getString("AmountType__c");
                String masterid = "";
                if (chargesList.getJSONObject(i).has("ChargeMaster__c")) {
                    masterid = chargesList.getJSONObject(i).getString("ChargeMaster__c");
                }
                double val = 0.0;
                if (chargesList.getJSONObject(i).has("Amount__c")) {
                    val = chargesList.getJSONObject(i).getDouble("Amount__c");
                }

                String ctype = chargesList.getJSONObject(i).getString("ChargeType__c");
                String isApplied = chargesList.getJSONObject(i).getString("IsApplied__c");

                DocCharge docCharge = new DocCharge(desc, id, masterid, isApplied.equalsIgnoreCase("Yes") ? true : false, val,type.equalsIgnoreCase("Floating") ? true : false,ctype.equalsIgnoreCase("Tax") ? true : false);//, type.matches("Floating") ? true : false);
                docCharges.add(docCharge);

                Log.d("Checkbox", "Adding Checkbox: "+docCharge.name + " : "+docCharge.amount + " : "+ docCharge.isApplied);

                if (chargesList.getJSONObject(i).getString("IsApplied__c").equals("Yes"))
                    chargeListText += desc + " : " + val + "\n";

                int finalI = i;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        boxes[finalI].setChecked(true);
//                    }
//                });
            }

//            docChargesNew.clear();
//            docChargesNew.addAll(docCharges);
            //docChargesNew = docCharges;

            if (chargeListText.length() > 5)
                chargeListText = chargeListText.substring(0, chargeListText.length() - 3);

            if (chargeListText.length() == 0)
                chargeListText = "N/A";

            JSONArray products = records.getJSONArray("salesOrderDetailsList");

            double totalCost = infoArray.getJSONObject(0).getDouble("TotalCost__c");
            double subTotal = infoArray.getJSONObject(0).getDouble("Subtotal__c");
            double tax = infoArray.getJSONObject(0).getDouble("Tax__c");
            double discount = infoArray.getJSONObject(0).getDouble("Discount__c");
            double addCharge = infoArray.getJSONObject(0).getDouble("AdditionalChargeApplied__c");
            double addDisc = infoArray.getJSONObject(0).getDouble("AdditionalDiscountApplied__c");

            String productName = "";
            Log.d("2515", "fetchOrderDetails: "+products.length());

            for (int i = 0; i < products.length(); i++) {
                String pId = products.getJSONObject(i).getString("Id");
                productName = products.getJSONObject(i).getJSONObject("Product__r").getString("Name");
                String id = products.getJSONObject(i).getJSONObject("Product__r").getString("Id");
                String uom = products.getJSONObject(i).getJSONObject("UOM__r").getString("Name");
                int Quantity__c = products.getJSONObject(i).getInt("Quantity__c");
                double SalesPrice__c = products.getJSONObject(i).getDouble("SalesPrice__c");
                double taxA = products.getJSONObject(i).getDouble("Tax__c");
                double disc = products.getJSONObject(i).getDouble("Discount__c");
                double total = Quantity__c * (SalesPrice__c + taxA - disc);

                ArrayList<ProductCharges> thisCharges = new ArrayList<>();
                for (int j = 0; j < productCharges.size(); j++) {
                    Log.d("TransactionID", "Comparing: "+pId +" To "+productCharges.get(j).productId);
                    if(pId.equals(productCharges.get(j).productId)){
                        Log.d("TransactionID", "Adding: "+pId +" To "+productCharges.get(j).productId);
                        productCharges.get(j).productId = id;
                        thisCharges.add(productCharges.get(j));
                    }
                }

                double fixedCharges = 0.0;
                double floatingCharges = 0.0;
                double fixedDisc = 0.0;
                double floatingDisc = 0.0;

                for (int j = 0; j < thisCharges.size(); j++) {
                    if(thisCharges.get(j).chargeType.equals("Tax")){
                        if(thisCharges.get(j).chargeValueType.equals("Fixed")){
                            fixedCharges += thisCharges.get(j).chargeValue;
                        } else floatingCharges += thisCharges.get(j).chargeValue;
                    } else{
                        if(thisCharges.get(j).chargeValueType.equals("Fixed")){
                            fixedDisc += thisCharges.get(j).chargeValue;
                        } else floatingDisc += thisCharges.get(j).chargeValue;
                    }
                }

                Double total1 = (Quantity__c * (SalesPrice__c + (SalesPrice__c*floatingCharges/100) - (SalesPrice__c*floatingDisc/100))) + fixedCharges - fixedDisc;

                CartProduct product = new CartProduct(productName, id, pId, uom, Quantity__c, SalesPrice__c, taxA, disc, total1, thisCharges);
                cartProducts.add(product);
            }

            Log.d("2515", "fetchOrderDetails: "+cartProducts.size());
            orderObject = new OrderObject(orderName, orderID, dealer, dealerID, branch, branchID, customer, customerID, state, narration, cartProducts, docCharges);

//            setCharges();
//            try {
//                getChargeList();
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(canEdit){
                        ((TextView) findViewById(R.id.textView83)).setText(": " + "Draft");
                    } else
                        ((TextView) findViewById(R.id.textView83)).setText(": " + "Final");

                    if (cartProducts.size() < 4) {
                        findViewById(R.id.button21).setVisibility(View.GONE);
                    }

                    for (int i = 0; i < cartProducts.size(); i++) {

                        String name = cartProducts.get(i).name;
                        String uom = cartProducts.get(i).UOM;
                        final int[] qty = {cartProducts.get(i).qty};
                        final double[] salesPrice = {cartProducts.get(i).salesPrice};
                        final double[] tax1 = {cartProducts.get(i).tax};
                        final double[] disc1 = {cartProducts.get(i).discount};
                        final double[] total = {cartProducts.get(i).total};

                        ConstraintLayout constraintLayout = findViewById(R.id.cvParent);
                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.clone(constraintLayout);

                        double fixedCharges = 0.0;
                        double floatingCharges = 0.0;
                        double fixedDisc = 0.0;
                        double floatingDisc = 0.0;

                        CartProduct selectedProduct = cartProducts.get(i);
                        for (int j = 0; j < selectedProduct.charges.size(); j++) {
                            if(selectedProduct.charges.get(j).chargeType.equals("Tax")){
                                if(selectedProduct.charges.get(j).chargeValueType.equals("Fixed")){
                                    fixedCharges += selectedProduct.charges.get(j).chargeValue;
                                } else floatingCharges += selectedProduct.charges.get(j).chargeValue;
                            } else{
                                if(selectedProduct.charges.get(j).chargeValueType.equals("Fixed")){
                                    fixedDisc += selectedProduct.charges.get(j).chargeValue;
                                } else floatingDisc += selectedProduct.charges.get(j).chargeValue;
                            }
                        }

                        double finalFloatingCharges = floatingCharges;
                        double finalFloatingDisc = floatingDisc;
                        double finalFixedDisc = fixedDisc;
                        double finalFixedCharges = fixedCharges;

                        switch (i) {
                            case 0:

                                double perSalesPrice = salesPrice[0] / qty[0];
                                double perTax1 = tax1[0] / qty[0];
                                double perDOsc1 = disc1[0] / qty[0];
                                double perTotal = total[0] / qty[0];

                                findViewById(R.id.cardView12).setVisibility(View.VISIBLE);
                                ((TextView) findViewById(R.id.tvProduct1Name)).setText(name);
                                ((TextView) findViewById(R.id.tvUOMProduct1)).setText(uom);
                                ((TextView) findViewById(R.id.tvQty)).setText(qty[0] + "");
                                ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + round(salesPrice[0],2));
                                ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + round(total[0],2) + "");

                                if (canEdit) {
                                    findViewById(R.id.imageButton4).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (qty[0] > 1) {
                                                qty[0]--;
//                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
//                                    tax1[0] = tax1[0] -perTax1;
//                                    disc1[0] = disc1[0] -perDOsc1;
                                                //total[0] = total[0] - perTotal;
                                                total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                                ((TextView) findViewById(R.id.tvQty)).setText(qty[0] + "");
//                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
                                                ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                                ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                                ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + (round(total[0], 2)) + "");

                                                for (int j = 0; j < cartProducts.size(); j++) {
                                                    if (cartProducts.get(j).name.equals(name)) {
                                                        cartProducts.get(j).qty = qty[0];
                                                        cartProducts.get(j).total = total[0];
                                                    }
                                                }

                                                setCharges();
                                                try {
                                                    getChargeList();
                                                } catch (UnsupportedEncodingException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    });

                                    findViewById(R.id.imageButton5).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            qty[0]++;
//                                salesPrice[0] = salesPrice[0] +perSalesPrice;
//                                tax1[0] = tax1[0] +perTax1;
//                                disc1[0] = disc1[0] +perDOsc1;
                                            //total[0] = total[0] + perTotal;
                                            total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                            ((TextView) findViewById(R.id.tvQty)).setText(qty[0] + "");
//                                ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+(salesPrice[0]));
                                            ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                            ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                            ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + (round(total[0], 2)) + "");

                                            for (int j = 0; j < cartProducts.size(); j++) {
                                                if (cartProducts.get(j).name.equals(name)) {
                                                    cartProducts.get(j).qty = qty[0];
                                                    cartProducts.get(j).total = total[0];
                                                }
                                            }

                                            setCharges();
                                            try {
                                                getChargeList();
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    findViewById(R.id.bRemoveProduct1).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            findViewById(R.id.cardView12).setVisibility(View.GONE);
                                            for (int j = 0; j < cartProducts.size(); j++) {
                                                if (cartProducts.get(j).name.equals(name)) {
                                                    removedProducts.add(cartProducts.get(j));
                                                    cartProducts.remove(cartProducts.get(j));
                                                }
                                            }
                                            setCharges();
                                            try {
                                                getChargeList();
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    findViewById(R.id.bEditProduct1).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            for (int j = 0; j < cartProducts.size(); j++) {
                                                if (cartProducts.get(j).name.equals(name)) {
                                                    showEditDialog(cartProducts.get(j),0);
                                                }
                                            }
                                        }
                                    });
                                }
//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                break;

                            case 1:

                                findViewById(R.id.cardView13).setVisibility(View.VISIBLE);
                                ((TextView) findViewById(R.id.tvProduct2Name)).setText(name);
                                ((TextView) findViewById(R.id.tvUOMProduct2)).setText(uom);
                                ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
                                ((TextView) findViewById(R.id.tvSalesPriceProduct2)).setText("" + round(salesPrice[0],2) + "");
                                ((TextView) findViewById(R.id.tvChargesProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                ((TextView) findViewById(R.id.tvDiscountProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + round(total[0],2) + "");


                                double perTotal1 = total[0] / qty[0];

                                if (canEdit) {
                                    findViewById(R.id.imageButton42).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (qty[0] > 1) {
                                                qty[0]--;
//                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
//                                    tax1[0] = tax1[0] -perTax1;
//                                    disc1[0] = disc1[0] -perDOsc1;
                                                //total[0] = total[0] - perTotal1;
                                                total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                                ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
//                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
                                                ((TextView) findViewById(R.id.tvChargesProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                                ((TextView) findViewById(R.id.tvDiscountProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                                ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0], 2)) + "");

                                                for (int j = 0; j < cartProducts.size(); j++) {
                                                    if (cartProducts.get(j).name.equals(name)) {
                                                        cartProducts.get(j).qty = qty[0];
                                                        cartProducts.get(j).total = total[0];
                                                    }
                                                }

                                                setCharges();
                                                try {
                                                    getChargeList();
                                                } catch (UnsupportedEncodingException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    });

                                    findViewById(R.id.imageButton52).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            qty[0]++;
//                                salesPrice[0] = salesPrice[0] +perSalesPrice;
//                                tax1[0] = tax1[0] +perTax1;
//                                disc1[0] = disc1[0] +perDOsc1;
                                            //total[0] = total[0] + perTotal1;
                                            total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                            ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
//                                ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+(salesPrice[0]));
                                            ((TextView) findViewById(R.id.tvChargesProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                            ((TextView) findViewById(R.id.tvDiscountProduct2)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                            ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0], 2)) + "");

                                            for (int j = 0; j < cartProducts.size(); j++) {
                                                if (cartProducts.get(j).name.equals(name)) {
                                                    cartProducts.get(j).qty = qty[0];
                                                    cartProducts.get(j).total = total[0];
                                                }
                                            }

                                            setCharges();
                                            try {
                                                getChargeList();
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    findViewById(R.id.bRemoveProduct2).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            findViewById(R.id.cardView13).setVisibility(View.GONE);
                                            for (int j = 0; j < cartProducts.size(); j++) {
                                                if (cartProducts.get(j).name.equals(name)) {
                                                    removedProducts.add(cartProducts.get(j));
                                                    cartProducts.remove(cartProducts.get(j));
                                                }
                                            }
                                            setCharges();
                                            try {
                                                getChargeList();
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    findViewById(R.id.bEditProduct2).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            for (int j = 0; j < cartProducts.size(); j++) {
                                                if (cartProducts.get(j).name.equals(name)) {
                                                    showEditDialog(cartProducts.get(j),1);
                                                }
                                            }
                                        }
                                    });
                                }
//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                break;

                            case 2:

                                double perTotal3 = total[0] / qty[0];

                                findViewById(R.id.cardView14).setVisibility(View.VISIBLE);
                                ((TextView) findViewById(R.id.tvProduct3Name)).setText(name);
                                ((TextView) findViewById(R.id.tvUOMProduct3)).setText(uom);
                                ((TextView) findViewById(R.id.tvQty3)).setText(qty[0] + "");
                                ((TextView) findViewById(R.id.tvSalesPriceProduct3)).setText("" + round(salesPrice[0],2) + "");
                                ((TextView) findViewById(R.id.tvChargesProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                ((TextView) findViewById(R.id.tvDiscountProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + round(total[0],2) + "");

                                if (canEdit) {

                                    findViewById(R.id.imageButton43).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (qty[0] > 1) {
                                                qty[0]--;
//                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
//                                    tax1[0] = tax1[0] -perTax1;
//                                    disc1[0] = disc1[0] -perDOsc1;
                                                //total[0] = total[0] - perTotal3;
                                                total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                                ((TextView) findViewById(R.id.tvQty3)).setText(qty[0] + "");
//                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
                                                ((TextView) findViewById(R.id.tvChargesProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                                ((TextView) findViewById(R.id.tvDiscountProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                                ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0], 2)) + "");

                                                for (int j = 0; j < cartProducts.size(); j++) {
                                                    if (cartProducts.get(j).name.equals(name)) {
                                                        cartProducts.get(j).qty = qty[0];
                                                        cartProducts.get(j).total = total[0];
                                                    }
                                                }

                                                setCharges();
                                                try {
                                                    getChargeList();
                                                } catch (UnsupportedEncodingException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    });

                                    findViewById(R.id.imageButton53).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            qty[0]++;
//                                salesPrice[0] = salesPrice[0] +perSalesPrice;
//                                tax1[0] = tax1[0] +perTax1;
//                                disc1[0] = disc1[0] +perDOsc1;
                                            //total[0] = total[0] + perTotal3;
                                            total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                            ((TextView) findViewById(R.id.tvQty3)).setText(qty[0] + "");
//                                ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+(salesPrice[0]));
                                            ((TextView) findViewById(R.id.tvChargesProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges,2) + "");
                                            ((TextView) findViewById(R.id.tvDiscountProduct3)).setText("" + round((qty[0]*salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc,2) + "");
                                            ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0], 2)) + "");

                                            for (int j = 0; j < cartProducts.size(); j++) {
                                                if (cartProducts.get(j).name.equals(name)) {
                                                    cartProducts.get(j).qty = qty[0];
                                                    cartProducts.get(j).total = total[0];
                                                }
                                            }

                                            setCharges();
                                            try {
                                                getChargeList();
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    findViewById(R.id.bRemoveProduct3).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            findViewById(R.id.cardView14).setVisibility(View.GONE);
                                            for (int j = 0; j < cartProducts.size(); j++) {
                                                if (cartProducts.get(j).name.equals(name)) {
                                                    removedProducts.add(cartProducts.get(j));
                                                    cartProducts.remove(cartProducts.get(j));
                                                }
                                            }
                                            setCharges();
                                            try {
                                                getChargeList();
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    findViewById(R.id.bEditProduct3).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            for (int j = 0; j < cartProducts.size(); j++) {
                                                if (cartProducts.get(j).name.equals(name)) {
                                                    showEditDialog(cartProducts.get(j),2);
                                                }
                                            }
                                        }
                                    });
                                }

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                break;
                        }

                    }

                    for (int i = 0; i < docCharges.size(); i++) {
                        Log.d("Checkbox", "Setting Up Checkboxes: ");
                        String name = docCharges.get(i).name;
                        //double amount = docCharges.get(i).amount;
                        double amount = docCharges.get(i).isFloating ?  (Double.parseDouble(getSubtotal(cartProducts))+ Double.parseDouble(getTax(cartProducts))- Double.parseDouble(getDiscount(cartProducts)))* (docCharges.get(i).amount/100) : docCharges.get(i).amount;
                        boolean isApplied = docCharges.get(i).isApplied;

                        Log.d("Checkbox", "run: "+docCharges.get(i).name + " is applied: " +docCharges.get(i).isApplied);
                        ConstraintLayout constraintLayout = findViewById(R.id.cvParent);
                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.clone(constraintLayout);

                        switch (i) {
                            case 0:
                                findViewById(R.id.checkBox).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView78).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView103).setVisibility(View.VISIBLE);
                                ((CheckBox) findViewById(R.id.checkBox)).setText(name);
                                ((TextView) findViewById(R.id.textView103)).setText(round(amount,2) + "");

                                ((CheckBox) findViewById(R.id.checkBox)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                break;

                            case 1:

                                findViewById(R.id.checkBox3).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView104).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView105).setVisibility(View.VISIBLE);
                                ((CheckBox) findViewById(R.id.checkBox3)).setText(name);
                                ((TextView) findViewById(R.id.textView105)).setText(round(amount,2) + "");

                                ((CheckBox) findViewById(R.id.checkBox3)).setChecked(isApplied);


//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                break;

                            case 2:

                                findViewById(R.id.checkBox4).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView106).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView107).setVisibility(View.VISIBLE);
                                ((CheckBox) findViewById(R.id.checkBox4)).setText(name);
                                ((TextView) findViewById(R.id.textView107)).setText(round(amount,2) + "");

                                ((CheckBox) findViewById(R.id.checkBox4)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                break;

                            case 3:

                                findViewById(R.id.checkBox5).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView108).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView109).setVisibility(View.VISIBLE);
                                ((CheckBox) findViewById(R.id.checkBox5)).setText(name);
                                ((TextView) findViewById(R.id.textView109)).setText(round(amount,2) + "");


                                ((CheckBox) findViewById(R.id.checkBox5)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                break;

                            case 4:

                                findViewById(R.id.checkBox6).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView110).setVisibility(View.VISIBLE);
                                findViewById(R.id.textView111).setVisibility(View.VISIBLE);
                                ((CheckBox) findViewById(R.id.checkBox6)).setText(name);
                                ((TextView) findViewById(R.id.textView111)).setText(round(amount,2) + "");


                                ((CheckBox) findViewById(R.id.checkBox6)).setChecked(isApplied);

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

                    ((TextView) findViewById(R.id.textView50)).setText(finalState);

                    if (finalState.equals("Closed")) {
                        ((TextView) findViewById(R.id.textView50)).setBackgroundResource(R.drawable.rounded_status_bg);
                        ((TextView) findViewById(R.id.textView50)).setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.status_completed)));
                    } else if (!finalState.equals("Draft")) {
                        ((TextView) findViewById(R.id.textView50)).setBackgroundResource(R.drawable.rounded_status_bg);
                        ((TextView) findViewById(R.id.textView50)).setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.status_yellow)));

                    } else {
                        if (menu != null)
                            menu.getItem(0).setVisible(true);
                    }

                    ((TextView) findViewById(R.id.textView80)).setText(": " + dealer);
                    ((TextView) findViewById(R.id.textView81)).setText(": " + branch);
                    ((TextView) findViewById(R.id.textView82)).setText(": " + customer);

                    ((TextView) findViewById(R.id.textView84)).setText(": " + finalNarration);

                    ((TextView) findViewById(R.id.textView95)).setText("" + round(subTotal,2));
                    ((TextView) findViewById(R.id.textView96)).setText("" + round(tax,2));
                    ((TextView) findViewById(R.id.textView97)).setText("" + round(discount,2));
                    ((TextView) findViewById(R.id.textView98)).setText("" + round(addDisc,2));
                    ((TextView) findViewById(R.id.textView99)).setText("" + round(addCharge,2));

                    ((TextView) findViewById(R.id.textView102)).setText("₹ " + round(totalCost,2));

                    //((TextView)findViewById(R.id.tvAdditionalCost)).setText(""+ finalChargeListText);

                    //((TextView)findViewById(R.id.tvProductNameProductTab)).setText(finalProductName);
                    RecyclerView rvProduct = findViewById(R.id.rcCartProducts);
                    rvProduct.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

                    //ViewCompat.setNestedScrollingEnabled(rvProduct,true);
                    CartProductAdapter adapter = new CartProductAdapter(cartProducts, removedProducts, getApplicationContext(), false);
                    rvProduct.setAdapter(adapter);

                    ((Button) findViewById(R.id.button21)).setText("View All " + cartProducts.size() + " Products");
                    findViewById(R.id.progressBar8).setVisibility(View.INVISIBLE);
                    findViewById(R.id.nv).setVisibility(View.VISIBLE);
                }
            });

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        getChargeList();
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
            Log.d("2514", "checkSalesOrderQuery: " + e.toString());
        } catch (JSONException e) {
            Log.d("2514", "checkSalesOrderQuery: " + e.toString());
            e.printStackTrace();
        }
    }

}