package com.pwc.d2ep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.EditText;
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
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CartActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    double otherCost = 0.0;
    RecyclerView rvProducts;
    String token = "";
    OrderObject orderObject;
    String productPayload = "",productChargesPayload = "", docPayload = "";
    ArrayList<CartProduct> cartProducts;
    ArrayList<DocCharge> docCharges;
    ArrayList<ProductCharges> productCharges;
    ArrayList<CartProduct> removedProducts;

    ArrayList<ProductCharges> combinedCharges;
    double totalCost, subtotal, tax,disc;
    private RestClient client1;
    private Dialog dialog, dialog_load;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        removedProducts = new ArrayList<>();
        docCharges = new ArrayList<>();
        productCharges = new ArrayList<>();
        combinedCharges = new ArrayList<>();

        orderObject = (OrderObject) getIntent().getParcelableExtra("order");
        combinedCharges =  orderObject.combinedCharges;
        //Toast.makeText(this,orderObject.products.get(0).name,Toast.LENGTH_SHORT).show();
        cartProducts = orderObject.products;
        if(orderObject.docCharges!= null) {
            docCharges = orderObject.docCharges;
        }

//        for (int i = 0; i < combinedCharges.size(); i++) {
//
//            Log.d("Charges", "Product: "+combinedCharges.get(i).productId + " "+combinedCharges.get(i).chargeName);
//            for (int j = 0; j < cartProducts.size(); j++) {
//                if(cartProducts.get(j).ID.equals(combinedCharges.get(i).productId)){
//                    Log.d("Charges", "Found match for Product: "+combinedCharges.get(i).productId + " as "+cartProducts.get(j).name);
//                }
//            }
//        }

        for (int i = 0; i < cartProducts.size(); i++) {
            ArrayList<ProductCharges> pCharges = new ArrayList<>();
            for (int j = 0; j < combinedCharges.size(); j++) {
                if(cartProducts.get(i).ID.equals(combinedCharges.get(j).productId)){
                    pCharges.add(combinedCharges.get(j));
                }
            }
            cartProducts.get(i).charges = pCharges;
        }

        for (int i = 0; i < cartProducts.size(); i++) {

            for (int j = 0; j < cartProducts.get(i).charges.size(); j++) {
                Log.d("Charges", "Product: "+cartProducts.get(i).ID + " = " +cartProducts.get(i).charges.get(j).productId +" : "+cartProducts.get(i).charges.get(j).chargeName);
            }
        }



//        for (int i = 0; i < combinedCharges.size(); i++) {
//            for (int j = 0; j < combinedCharges.get(i).size(); j++) {
//                Log.d("Charges", "Charge Product: "+combinedCharges.get(i).get(j).productId +" of "+combinedCharges.get(i).get(j).chargeName);
//            }
//        }

        setupSF();
        dialog = new Dialog(this);
        dialog_load = new Dialog(this);

        dialog.setContentView(R.layout.dialog_product);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        dialog_load.setContentView(R.layout.dialog_progress);
        //dialog_load.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog_load.setCancelable(false);
        dialog_load.setCanceledOnTouchOutside(false);

        Drawable d = getResources().getDrawable(R.drawable.rounded_appbar_bg,null);
        getSupportActionBar().setBackgroundDrawable(d);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(orderObject.orderName.length()>0?orderObject.orderName:"New Order");
        ((TextView)findViewById(R.id.textView80)).setText(": " +orderObject.dealer);
        ((TextView)findViewById(R.id.textView81)).setText(": " +orderObject.branch);
        ((TextView)findViewById(R.id.textView82)).setText(": " +orderObject.customer);
        //((TextView)findViewById(R.id.textView83)).setText(": " +orderObject.state);
        ((TextView)findViewById(R.id.textView84)).setText(": " +orderObject.narration);

        ((TextView)findViewById(R.id.textView95)).setText(round(getSubtotal(orderObject.products))+"");
        ((TextView)findViewById(R.id.textView96)).setText(round(getTax(orderObject.products))+"");
        ((TextView)findViewById(R.id.textView97)).setText(round(getDiscount(orderObject.products))+"");
        ((TextView)findViewById(R.id.textView98)).setText(""+round(getAddDiscount()));
        //((TextView)findViewById(R.id.textView99)).setText("3000");

        ((TextView)findViewById(R.id.textView102)).setText("₹ "+round(getTotal(getSubtotal(orderObject.products),getTax(orderObject.products),getDiscount(orderObject.products),getAddCost()-getAddDiscount())));

        rvProducts = findViewById(R.id.rcCartProducts);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        ((CheckBox)findViewById(R.id.checkBox)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox3)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox4)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox5)).setOnCheckedChangeListener(this);
        ((CheckBox)findViewById(R.id.checkBox6)).setOnCheckedChangeListener(this);
        CartProductAdapter cartProductAdapter = new CartProductAdapter(orderObject.products, removedProducts,this, true);
        rvProducts.setAdapter(cartProductAdapter);

//        final ArrayList<CartProduct>[] cartProductss = new ArrayList[]{orderObject.products};

        String product1Id = orderObject.products.size() > 0 ? orderObject.products.get(0).ID : "";
        String product2Id = orderObject.products.size() > 1 ? orderObject.products.get(1).ID:"";
        String product3Id = orderObject.products.size() > 2 ? orderObject.products.get(2).ID : "";

//        findViewById(R.id.bRemoveProduct1).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                removeProduct(product1Id);
//                //cartProducts[0].remove(0);
//                findViewById(R.id.cardView12).setVisibility(View.GONE);
//                if (orderObject.products.size()>3){
//                    findViewById(R.id.button23).setVisibility(View.VISIBLE);
//                    ((Button)findViewById(R.id.button23)).setText("View All "+ orderObject.products.size()+" Products");
//                }else  findViewById(R.id.button23).setVisibility(View.INVISIBLE);
//            }
//        });
//
//        findViewById(R.id.bRemoveProduct2).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                removeProduct(product2Id);
//                //cartProducts[0].remove(1);
//                findViewById(R.id.cardView13).setVisibility(View.GONE);
//                if (orderObject.products.size()>3){
//                    findViewById(R.id.button23).setVisibility(View.VISIBLE);
//                    ((Button)findViewById(R.id.button23)).setText("View All "+ orderObject.products.size()+" Products");
//                }else  findViewById(R.id.button23).setVisibility(View.INVISIBLE);
//            }
//        });
//
//        findViewById(R.id.bRemoveProduct3).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                removeProduct(product3Id);
//                //cartProducts[0].remove(2);
//                findViewById(R.id.cardView14).setVisibility(View.GONE);
//                if (orderObject.products.size()>3){
//                    findViewById(R.id.button23).setVisibility(View.VISIBLE);
//                    ((Button)findViewById(R.id.button23)).setText("View All "+ orderObject.products.size()+" Products");
//                }else  findViewById(R.id.button23).setVisibility(View.INVISIBLE);
//            }
//        });

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
                final CartProductAdapter[] adapter = {new CartProductAdapter(cartProducts, removedProducts, CartActivity.this, true)};
                rvProduct.setAdapter(adapter[0]);

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        cartProducts = adapter[0].getUpdatedList();
                        adapter[0] = new CartProductAdapter(cartProducts, removedProducts,getApplicationContext(),true);

                        rvProduct.setAdapter(adapter[0]);

                        if (cartProducts.size()>3){
                            findViewById(R.id.button23).setVisibility(View.VISIBLE);
                            ((Button)findViewById(R.id.button23)).setText("View All "+ cartProducts.size()+" Products");
                        }else  findViewById(R.id.button23).setVisibility(View.INVISIBLE);
                    }
                });
                dialog.findViewById(R.id.button22).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        removedProducts = adapter[0].getUpdatedRemovedList();
                        cartProducts = adapter[0].getUpdatedList();
                        updateProducts();
                        setCharges();
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

        if (cartProducts.size()>3){
            findViewById(R.id.button23).setVisibility(View.VISIBLE);
            ((Button)findViewById(R.id.button23)).setText("View All "+ cartProducts.size()+" Products");
        }

        setUpProducts();

        findViewById(R.id.button17).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(cartProducts.size() == 0){
                    Toast.makeText(getApplicationContext(),"Please add at least one product!", Toast.LENGTH_SHORT).show();
                    return;
                }
                prepareProductPayload();
                prepareChargesPayload();
                //Toast.makeText(getApplicationContext(),"Submitting Order...", Toast.LENGTH_SHORT).show();
                dialog_load.show();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {

                        if (orderObject.orderName.length()>0){
                            submitPatch("Submitted For Approval");
                        }else {

                            submitPatch("Submitted For Approval");
                        }
                    }
                });
            }
        });

        findViewById(R.id.button18).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cartProducts.size() == 0){
                    Toast.makeText(getApplicationContext(),"Please add at least one product!", Toast.LENGTH_SHORT).show();
                    return;
                }
                prepareProductPayload();
                prepareChargesPayload();
                //Toast.makeText(getApplicationContext(),"Saving as Draft...", Toast.LENGTH_SHORT).show();
                dialog_load.show();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (orderObject.orderName.length()>0){
                            submitPatch("draft");
                        }else {

                            submitPatch("draft");
                        }
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

    void setUpProducts(){

        for (int i = 0; i < cartProducts.size(); i++) {
            String name = cartProducts.get(i).name;
            String uom = cartProducts.get(i).UOM;
            final int[] qty = {cartProducts.get(i).qty};
            final double[] salesPrice = {cartProducts.get(i).salesPrice};
            final double[] tax1 = {cartProducts.get(i).tax};
            final double[] disc1 = {cartProducts.get(i).discount};
            final double[] total = {cartProducts.get(i).total};

            ConstraintLayout constraintLayout = findViewById(R.id.cvParentCart);
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

            switch (i){
                case 0:

                    double perSalesPrice = salesPrice[0] /qty[0];
                    double perTax1 =  tax1[0] /qty[0];
                    double perDOsc1 =  disc1[0] /qty[0];
                    double perTotal =  total[0] /qty[0];

                    findViewById(R.id.cardView12).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.tvProduct1Name)).setText(name);
                    ((TextView)findViewById(R.id.tvUOMProduct1)).setText(uom);
                    ((TextView)findViewById(R.id.tvQty)).setText(qty[0] +"");
                    ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(""+ round(salesPrice[0]));
                    ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+ round(tax1[0]) +"");
                    ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+ round(disc1[0]) +"");
                    ((TextView)findViewById(R.id.tvTotalCostProduct1)).setText(""+ round(total[0]) +"");



                    boolean canEdit = true;
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
//                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (tax1[0]) + "");
//                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +  (disc1[0]) + "");
                                    ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + (round(total[0])) + "");

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
//                                ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+(tax1[0])+"");
//                                ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+(disc1[0])+"");
                                ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + (round(total[0])) + "");

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
                                updateProducts();
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
                    ((TextView)findViewById(R.id.tvProduct2Name)).setText(name);
                    ((TextView)findViewById(R.id.tvUOMProduct2)).setText(uom);
                    ((TextView)findViewById(R.id.tvQty2)).setText(qty[0] +"");
                    ((TextView)findViewById(R.id.tvSalesPriceProduct2)).setText(""+ round(salesPrice[0]) +"");
                    ((TextView)findViewById(R.id.tvChargesProduct2)).setText(""+ round(tax1[0]) +"");
                    ((TextView)findViewById(R.id.tvDiscountProduct2)).setText(""+ round(disc1[0]) +"");
                    ((TextView)findViewById(R.id.tvTotalCostProduct2)).setText(""+ round(total[0]) +"");


                    double perTotal1 =  total[0] /qty[0];

                    if(true) {
                        findViewById(R.id.imageButton42).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (qty[0] > 1) {
                                    qty[0]--;
//                                    salesPrice[0] = salesPrice[0] -perSalesPrice;
//                                    tax1[0] = tax1[0] -perTax1;
//                                    disc1[0] = disc1[0] -perDOsc1;
                                   // total[0] = total[0] - perTotal1;
                                    total[0] = (qty[0] * (salesPrice[0] + (salesPrice[0]*finalFloatingCharges/100) - (salesPrice[0]*finalFloatingDisc/100))) + finalFixedCharges - finalFixedDisc;

                                    ((TextView) findViewById(R.id.tvQty2)).setText(qty[0] + "");
//                                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + (salesPrice[0]));
//                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (tax1[0]) + "");
//                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +  (disc1[0]) + "");
                                    ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0])) + "");

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
//                                ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+(tax1[0])+"");
//                                ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+(disc1[0])+"");
                                ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0])) + "");

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
                                updateProducts();
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

                    double perTotal3 =  total[0] /qty[0];

                    findViewById(R.id.cardView14).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.tvProduct3Name)).setText(name);
                    ((TextView)findViewById(R.id.tvUOMProduct3)).setText(uom);
                    ((TextView)findViewById(R.id.tvQty3)).setText(qty[0] +"");
                    ((TextView)findViewById(R.id.tvSalesPriceProduct3)).setText(""+ round(salesPrice[0]) +"");
                    ((TextView)findViewById(R.id.tvChargesProduct3)).setText(""+ round(tax1[0]) +"");
                    ((TextView)findViewById(R.id.tvDiscountProduct3)).setText(""+ round(disc1[0]) +"");
                    ((TextView)findViewById(R.id.tvTotalCostProduct3)).setText(""+ round(total[0]) +"");


                    if(true) {

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
//                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (tax1[0]) + "");
//                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +  (disc1[0]) + "");
                                    ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0])) + "");

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
//                                ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+(tax1[0])+"");
//                                ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+(disc1[0])+"");
                                ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0])) + "");

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
                                updateProducts();
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
                } else floatingCharges += thisCharges.get(j).chargeValue;
            } else{
                if(thisCharges.get(j).chargeValueType.equals("Fixed")){
                    fixedDisc += thisCharges.get(j).chargeValue;
                } else floatingDisc += thisCharges.get(j).chargeValue;
            }
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
        ((EditText)dialog.findViewById(R.id.editText)).setText(round(selectedProduct.salesPrice)+"");
        ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *selectedProduct.salesPrice)+"");

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

                    ((TextView)dialog.findViewById(R.id.textView55)).setText(thisCharges.get(i).chargeName + (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText)dialog.findViewById(R.id.editText2)).setText(round(thisCharges.get(i).chargeValue)+"");
                    double total = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).amount : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
                    ((TextView)dialog.findViewById(R.id.textView71)).setText(round(total)+"");
                    break;
                case 1:
                    ((TextView)dialog.findViewById(R.id.textView63)).setText(thisCharges.get(i).chargeName+ (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText)dialog.findViewById(R.id.editText3)).setText(round(thisCharges.get(i).chargeValue)+"");
                    double total1 = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).amount : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
                    ((TextView)dialog.findViewById(R.id.textView72)).setText(round(total1)+"");

                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
                    break;

                case 2:
                    ((TextView) dialog.findViewById(R.id.textView64)).setText(thisCharges.get(i).chargeName+ (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText4)).setText(round(thisCharges.get(i).chargeValue)+"");
                    double total2 = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).amount : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
                    ((TextView) dialog.findViewById(R.id.textView73)).setText(round(total2) + "");

                    dialog.findViewById(R.id.textView64).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText4).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView73).setVisibility(View.VISIBLE);
                    break;
                case 3:
                    ((TextView) dialog.findViewById(R.id.textView65)).setText(thisCharges.get(i).chargeName+ (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText5)).setText(round(thisCharges.get(i).chargeValue)+"");
                    double total3 = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).amount : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;

                    ((TextView) dialog.findViewById(R.id.textView74)).setText(round(total3) + "");
                    dialog.findViewById(R.id.textView65).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText5).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView74).setVisibility(View.VISIBLE);
                    break;
                case 4:
                    ((TextView) dialog.findViewById(R.id.textView69)).setText(thisCharges.get(i).chargeName+ (thisCharges.get(i).chargeValueType.equals("Fixed") ? "" : " (%)"));
                    ((EditText) dialog.findViewById(R.id.editText8)).setText(round(thisCharges.get(i).chargeValue)+"");
                    double total4 = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).amount : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;

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
                                    thisCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = fixedDiscount;
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
                                    thisCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = fixedDiscount;
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
                                    thisCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = fixedDiscount;
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
                                    thisCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = fixedDiscount;
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
                                    thisCharges.get(i).amount = fixedCharge;
                                } else {
                                    floatingCharge += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue / 100;
                                }
                            } else {
                                if (thisCharges.get(i).chargeValueType.equals("Fixed")) {
                                    fixedDiscount += thisCharges.get(i).chargeValue;
                                    thisCharges.get(i).amount = fixedDiscount;
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
//                }

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

                setUpProducts();
                switch (pos) {
                    case 0:
                        ((TextView)findViewById(R.id.tvQty)).setText(qtyToAdd[0] +"");

                        ((TextView)findViewById(R.id.tvSalesPriceProduct1)).setText(round(cost)+"");
                        updateProducts();
                        setCharges();
                        try {
                            getChargeList();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        ((TextView)findViewById(R.id.tvChargesProduct1)).setText(round(selectedProduct.tax)+"");
                        ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(round(selectedProduct.discount)+"");


                        ((TextView)findViewById(R.id.tvTotalCostProduct1)).setText(round(selectedProduct.total) +"");

                        break;
                    case 1:
                        ((TextView)findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
                        ((TextView)findViewById(R.id.tvSalesPriceProduct2)).setText(round(cost)+"");
                        updateProducts();
                        setCharges();
                        try {
                            getChargeList();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        ((TextView)findViewById(R.id.tvChargesProduct2)).setText(round(fixedCharge + floatingCharge)+"");
                        ((TextView)findViewById(R.id.tvDiscountProduct2)).setText(round(fixedDiscount + floatingDiscount)+"");

                        ((TextView)findViewById(R.id.tvTotalCostProduct2)).setText(round(selectedProduct.total) +"");

                        break;
                    case 2:
                        ((TextView)findViewById(R.id.tvQty3)).setText(qtyToAdd[0] +"");
                        ((TextView)findViewById(R.id.tvSalesPriceProduct3)).setText(round(cost)+"");
                        updateProducts();
                        setCharges();
                        try {
                            getChargeList();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        ((TextView)findViewById(R.id.tvChargesProduct3)).setText(round(fixedCharge + floatingCharge)+"");
                        ((TextView)findViewById(R.id.tvDiscountProduct3)).setText(round(fixedDiscount + floatingDiscount)+"");
                        ((TextView)findViewById(R.id.tvTotalCostProduct3)).setText(round(selectedProduct.total) +"");

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

        ((TextView)dialog.findViewById(R.id.textView76)).setText(round(selectedProduct.total)+"");


        dialog.show();
    }

//    private void showEditDialog(CartProduct selectedProduct) {
////        dialog.findViewById(R.id.imageButton7).setOnClickListener(this);
////        dialog.findViewById(R.id.imageButton8).setOnClickListener(this);
//
//
//
//        dialog.findViewById(R.id.button16).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                dialog.dismiss();
//            }
//        });
//
//        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
//        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(selectedProduct.qty+"");
//        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.UOM);
//        ((EditText)dialog.findViewById(R.id.editText)).setText(selectedProduct.salesPrice+"");
//        ((TextView)dialog.findViewById(R.id.textView52)).setText(selectedProduct.qty*selectedProduct.salesPrice+"");
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
//                    ((EditText)dialog.findViewById(R.id.editText2)).setText(docCharges.get(i).amount+"");
//                    ((TextView)dialog.findViewById(R.id.textView71)).setText(docCharges.get(i).amount+"");
//                    break;
//                case 1:
//                    ((TextView)dialog.findViewById(R.id.textView63)).setText(docCharges.get(1).name);
//                    ((EditText)dialog.findViewById(R.id.editText3)).setText(docCharges.get(1).amount+"");
//                    ((TextView)dialog.findViewById(R.id.textView72)).setText(docCharges.get(1).amount+"");
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
//        ((TextView)dialog.findViewById(R.id.textView76)).setText(selectedProduct.qty*selectedProduct.salesPrice+docTotal+"");
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

            ConstraintLayout constraintLayout = findViewById(R.id.cvParentCart);
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
                    ((TextView) findViewById(R.id.tvSalesPriceProduct1)).setText("" + round(salesPrice[0]));
                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + round((salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges) + "");
                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" + round((salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc) + "");
                    ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + round(total[0]) + "");

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
//                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (tax1[0]) + "");
//                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +  (disc1[0]) + "");
                                    ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + (round(total[0])) + "");

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
//                                ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+(tax1[0])+"");
//                                ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+(disc1[0])+"");
                                ((TextView) findViewById(R.id.tvTotalCostProduct1)).setText("" + (round(total[0])) + "");

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
                    ((TextView) findViewById(R.id.tvSalesPriceProduct2)).setText("" + round(salesPrice[0]) + "");
                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + round((salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges) + "");
                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" + round((salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc) + "");
                    ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + round(total[0]) + "");


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
//                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (tax1[0]) + "");
//                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +  (disc1[0]) + "");
                                    ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0])) + "");

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
//                                ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+(tax1[0])+"");
//                                ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+(disc1[0])+"");
                                ((TextView) findViewById(R.id.tvTotalCostProduct2)).setText("" + (round(total[0])) + "");

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
                    ((TextView) findViewById(R.id.tvSalesPriceProduct3)).setText("" + round(salesPrice[0]) + "");
                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + round((salesPrice[0]*finalFloatingCharges/100)+finalFixedCharges) + "");
                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" + round((salesPrice[0]*finalFloatingDisc/100)+ finalFixedDisc) + "");
                    ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + round(total[0]) + "");

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
//                                    ((TextView) findViewById(R.id.tvChargesProduct1)).setText("" + (tax1[0]) + "");
//                                    ((TextView) findViewById(R.id.tvDiscountProduct1)).setText("" +  (disc1[0]) + "");
                                    ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0])) + "");

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
//                                ((TextView)findViewById(R.id.tvChargesProduct1)).setText(""+(tax1[0])+"");
//                                ((TextView)findViewById(R.id.tvDiscountProduct1)).setText(""+(disc1[0])+"");
                                ((TextView) findViewById(R.id.tvTotalCostProduct3)).setText("" + (round(total[0])) + "");

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

    void removeProduct(String id){

        for (int i = 0; i < orderObject.products.size(); i++) {
            if(orderObject.products.get(i).ID.equals(id)){
                removedProducts.add(orderObject.products.get(i));
                orderObject.products.remove(orderObject.products.get(i));

            }
        }
        setCharges();
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
               //showWarning();
                Intent intent = new Intent();
                intent.putExtra("order", cartProducts);
                setResult(RESULT_OK, intent);
                super.onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private double getTotal(double subtotal, double tax, double discount, double otherCost) {
        totalCost = subtotal + tax - discount + otherCost;
        return Double.parseDouble(round(subtotal + tax - discount + otherCost));
    }

    private double getSubtotal(ArrayList<CartProduct> products) {
        double cost = 0;
        for (int i = 0; i < products.size(); i++) {
            cost += products.get(i).salesPrice*products.get(i).qty;
        }
        subtotal = cost;
        return Double.parseDouble(round(cost));
    }

    private double getTax(ArrayList<CartProduct> products) {
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
        tax = cost;
        return Double.parseDouble(round(cost));
    }

    private double getDiscount(ArrayList<CartProduct> products) {
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
        disc = cost;
        return Double.parseDouble(round(cost));
    }

    private double getAddDiscount() {
        double cost = 0;
        for (int i = 0; i < docCharges.size(); i++) {
            View[] views = {findViewById(R.id.checkBox),findViewById(R.id.checkBox3),findViewById(R.id.checkBox4),findViewById(R.id.checkBox5),findViewById(R.id.checkBox6)};
            if(((CheckBox)views[i]).isChecked() && i < 2) {
                cost += docCharges.get(i).amount;
            }
        }
        return Double.parseDouble(round(cost));
    }

    private double getAddCost() {
        double cost = 0;
        View[] views = {findViewById(R.id.checkBox),findViewById(R.id.checkBox3),findViewById(R.id.checkBox4),findViewById(R.id.checkBox5),findViewById(R.id.checkBox6)};

        for (int i = 0; i < docCharges.size(); i++) {
            if(((CheckBox)views[i]).isChecked() && i > 1) {
                cost += docCharges.get(i).amount;
                Log.d("2512", "getAddCost: Adding"+ docCharges.get(i).amount);
            }
        }
        return Double.parseDouble(round(cost));
    }

    public static String round(double value) {
//        if (places < 0) throw new IllegalArgumentException();
//
//        BigDecimal bd = BigDecimal.valueOf(value);
//        bd = bd.setScale(places, RoundingMode.HALF_EVEN);
//        return bd.doubleValue();
        String val = String.valueOf((double) Math.round(value * 100) / 100);

        return ((val.charAt(val.length()-1)) == '0' || (val.charAt(val.length()-2)) == '.') ? val+"0":val;
    }

    void setCharges(){
        Log.d("2512", "setCharges: "+docCharges.size());
        ((TextView)findViewById(R.id.textView95)).setText(round(getSubtotal(cartProducts))+"");
        ((TextView)findViewById(R.id.textView96)).setText(round(getTax(cartProducts))+"");
        ((TextView)findViewById(R.id.textView97)).setText(round(getDiscount(cartProducts))+"");

        ((TextView)findViewById(R.id.textView98)).setText(round(getAddDiscount())+"");
        ((TextView)findViewById(R.id.textView99)).setText(round(getAddCost())+"");
//        ((TextView)findViewById(R.id.textView96)).setText(getTax(orderObject.products)+"");
//        ((TextView)findViewById(R.id.textView96)).setText(getTax(orderObject.products)+"");
        double total = getSubtotal(cartProducts) + getTax(cartProducts) - getDiscount(cartProducts) - getAddDiscount() + getAddCost();

        ((TextView)findViewById(R.id.textView102)).setText("₹ "+round(total));
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

                                    if(docCharges.size() == 0) {
                                        getChargeList();
                                    }else {
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
                                                            ((TextView)findViewById(R.id.textView103)).setText(round(amount)+"");

                                                            ((CheckBox)findViewById(R.id.checkBox)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                            break;

                                                        case 1:

                                                            findViewById(R.id.checkBox3).setVisibility(View.VISIBLE);
                                                            findViewById(R.id.textView104).setVisibility(View.VISIBLE);
                                                            findViewById(R.id.textView105).setVisibility(View.VISIBLE);
                                                            ((CheckBox)findViewById(R.id.checkBox3)).setText(name);
                                                            ((TextView)findViewById(R.id.textView105)).setText(round(amount)+"");

                                                            ((CheckBox)findViewById(R.id.checkBox3)).setChecked(isApplied);



//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                            break;

                                                        case 2:

                                                            findViewById(R.id.checkBox4).setVisibility(View.VISIBLE);
                                                            findViewById(R.id.textView106).setVisibility(View.VISIBLE);
                                                            findViewById(R.id.textView107).setVisibility(View.VISIBLE);
                                                            ((CheckBox)findViewById(R.id.checkBox4)).setText(name);
                                                            ((TextView)findViewById(R.id.textView107)).setText(round(amount)+"");
                                                            ((CheckBox)findViewById(R.id.checkBox4)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                                            break;

                                                        case 3:

                                                            findViewById(R.id.checkBox5).setVisibility(View.VISIBLE);
                                                            findViewById(R.id.textView108).setVisibility(View.VISIBLE);
                                                            findViewById(R.id.textView109).setVisibility(View.VISIBLE);
                                                            ((CheckBox)findViewById(R.id.checkBox5)).setText(name);
                                                            ((TextView)findViewById(R.id.textView109)).setText(round(amount)+"");

                                                            ((CheckBox)findViewById(R.id.checkBox5)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                            break;

                                                        case 4:
                                                            findViewById(R.id.checkBox6).setVisibility(View.VISIBLE);
                                                            findViewById(R.id.textView110).setVisibility(View.VISIBLE);
                                                            findViewById(R.id.textView111).setVisibility(View.VISIBLE);
                                                            ((CheckBox)findViewById(R.id.checkBox6)).setText(name);
                                                            ((TextView)findViewById(R.id.textView111)).setText(round(amount)+"");

                                                            ((CheckBox)findViewById(R.id.checkBox6)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                                            break;
                                                    }

                                                }
                                            }
                                        });
                                    }

                                } catch (JSONException | UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
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

            for (int j = 0; j < cartProducts.get(i).charges.size(); j++) {
                productChargesPayload += "{\n" +
                        "            \"Id\":"+ (cartProducts.get(i).charges.get(j).id == null ? null : "\""+cartProducts.get(i).charges.get(j).id+"\"") +",\n" +
                        "            \"productId\": \""+cartProducts.get(i).charges.get(j).productId+"\",\n" +
                        "            \"ChargeName\": \""+cartProducts.get(i).charges.get(j).chargeName+"\",\n" +
                        "            \"ChargeMaster\": \""+cartProducts.get(i).charges.get(j).chargeMaster+"\",\n" +
                        "            \"ChargeValue\": "+cartProducts.get(i).charges.get(j).chargeValue+",\n" +
                        "            \"ChargeValueType\": \""+cartProducts.get(i).charges.get(j).chargeValueType+"\",\n" +
                        "            \"ChargeType\": \""+cartProducts.get(i).charges.get(j).chargeType+"\",\n" +
                        "            \"TransactionAccount\": \""+cartProducts.get(i).charges.get(j).transactionAccount+"\",\n" +
                        "            \"Amount\": "+cartProducts.get(i).charges.get(j).amount+"\n" +
                        "        }";
                productChargesPayload += ",";
            }
        }

        if (removedProducts.size() > 0) {
            if (removedProducts.get(0).pID != null) {
                for (int i = 0; i < removedProducts.size(); i++) {
                    productPayload += "{\n" +
                            "            \"Id\":" + (removedProducts.get(i).pID == null ? null : "\"" + removedProducts.get(i).pID + "\"") + ",\n" +
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
            }
        }
        productPayload = productPayload.substring(0,productPayload.length()-1);
        productChargesPayload = productChargesPayload.substring(0,productChargesPayload.length()-1);


        Log.d("2515", "prepareProductPayload: "+productPayload);
    }

    void prepareChargesPayload() {

        CheckBox[] boxes = {findViewById(R.id.checkBox),findViewById(R.id.checkBox3),findViewById(R.id.checkBox4),findViewById(R.id.checkBox5),findViewById(R.id.checkBox6)};
        docPayload = "";
        for (int i = 0; i < docCharges.size(); i++) {
            docPayload += "        {\n" +
                    "            \"Id\": \""+docCharges.get(i).id+"\",\n" +
                    "\t\t\t\"ChargeMaster\": \""+docCharges.get(i).masterID+"\",\n" +
                    "            \"ChargeType\": "+(docCharges.get(i).isApplied ?"\"Tax\"":"\"Discount\"")+",\n" +
                    "            \"Amount\": "+docCharges.get(i).amount+",\n" +
                    "            \"AmountType\":"+(docCharges.get(i).isFloating ?"\"Floating\"":"\"Fixed\"")+",\n" +
                    "            \"IsApplied\": \""+(boxes[i].isChecked() ? "Yes" : "No")+"\",\n" +
                    "            \"Description\": \""+docCharges.get(i).name+"\",\n" +
                    "            \"Value\": "+docCharges.get(i).amount+"\n" +
                    "        }";
            docPayload += ",";
        }

        docPayload = docPayload.substring(0,docPayload.length()-1);
        Log.d("2515", "prepareDocPayload: "+docPayload);
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
//        String payload = "{\n" +
//                "\n" +
//                "\"salesOrderTransactionChargeLst\": [\n" +
//                "        {\n" +
//                "            \n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"productId\": \"01t3h0000019DJ0AAM\",\n" +
//                "            \"ChargeName\": \"Fixed Charge\",\n" +
//                "            \"ChargeMaster\": \"a033h000016zXuxAAE\",\n" +
//                "            \"ChargeValue\": 1000.00,\n" +
//                "            \"ChargeValueType\": \"Fixed\",\n" +
//                "            \"ChargeType\": \"Tax\",\n" +
//                "            \"Description\": \"Dealer\",\n" +
//                "            \"TransactionAccount\": \"a0A3h000002y1XaEAI\",\n" +
//                "            \"Amount\": 1000.00      \n" +
//                "        },\n" +
//                "        {\n" +
//                "           \n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"productId\": \"01t3h0000019DJ0AAM\",\n" +
//                "            \"ChargeName\": \"Packing Type\",\n" +
//                "            \"ChargeMaster\": \"a033h000003j26jAAA\",\n" +
//                "            \"ChargeValue\": 150.00,\n" +
//                "            \"ChargeValueType\": \"Fixed\",\n" +
//                "            \"ChargeType\": \"Tax\",\n" +
//                "            \"Description\": \"Dealer\",\n" +
//                "            \"TransactionAccount\": \"a0A3h000002y1XeEAI\",\n" +
//                "            \"Amount\": 150.00\n" +
//                "            \n" +
//                "        },\n" +
//                "        {\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"productId\": \"01t3h0000019DJ0AAM\",\n" +
//                "            \"ChargeName\": \"Reel Dia (in cm)\",\n" +
//                "            \"ChargeMaster\": \"a033h000003j17tAAA\",\n" +
//                "            \"ChargeValue\": 4.00,\n" +
//                "            \"ChargeValueType\": \"Floating\",\n" +
//                "            \"ChargeType\": \"Discount\",\n" +
//                "            \"Description\": \"OEM\",\n" +
//                "            \"TransactionAccount\": \"a0A3h000002y1XFEAY\",\n" +
//                "            \"Amount\": 36.00\n" +
//                "           \n" +
//                "        },\n" +
//                "\t\t {\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"productId\": \"01t3h000002jAjuAAE\",\n" +
//                "            \"ChargeName\": \"Fixed Charge\",\n" +
//                "            \"ChargeMaster\": \"a033h000016zXuxAAE\",\n" +
//                "            \"ChargeValue\": 1000.00,\n" +
//                "            \"ChargeValueType\": \"Fixed\",\n" +
//                "            \"ChargeType\": \"Tax\",\n" +
//                "            \"Description\": \"Dealer\",\n" +
//                "            \"TransactionAccount\": \"a0A3h000002y1XaEAI\",\n" +
//                "            \"Amount\": 1000.00\n" +
//                "           \n" +
//                "        },\n" +
//                "\t\t {\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"productId\": \"01t3h000002jAjuAAE\",\n" +
//                "            \"ChargeName\": \"Additional Discount\",\n" +
//                "            \"ChargeMaster\": \"a033h000004vLBQAA2\",\n" +
//                "            \"ChargeValue\": 1.00,\n" +
//                "            \"ChargeValueType\": \"Floating\",\n" +
//                "            \"ChargeType\": \"Discount\",\n" +
//                "            \"Description\": \"OEM\",\n" +
//                "            \"TransactionAccount\": \"a0A3h000002y1XUEAY\",\n" +
//                "            \"Amount\": 4.00\n" +
//                "           \n" +
//                "        }\n" +
//                "\t\t],\n" +
//                "    \"salesOrderDocChargeLst\": [\n" +
//                "        {\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"ChargeMaster\": \"a033h000004vLBUAA2\",\n" +
//                "            \"ChargeType\": \"Tax\",\n" +
//                "            \"Amount\": 4.00,\n" +
//                "            \"AmountType\": \"Floating\",\n" +
//                "            \"IsApplied\": \"Yes\",\n" +
//                "            \"Description\": \"Core  Dia (in cm)\",\n" +
//                "            \"Value\": 800.00\n" +
//                "        },\n" +
//                "        {\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"ChargeMaster\": \"a033h000004vLBVAA2\",\n" +
//                "            \"ChargeType\": \"Discount\",\n" +
//                "            \"Amount\": 3.00,\n" +
//                "            \"AmountType\": \"Fixed\",\n" +
//                "            \"IsApplied\": \"Yes\",\n" +
//                "            \"Description\": \"Employee Discount\",\n" +
//                "            \"Value\": 3.00\n" +
//                "        },\n" +
//                "        {\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"ChargeMaster\": \"a033h000004vLBQAA2\",\n" +
//                "            \"ChargeType\": \"Discount\",\n" +
//                "            \"Amount\": 1.00,\n" +
//                "            \"AmountType\": \"Floating\",\n" +
//                "            \"IsApplied\": \"Yes\",\n" +
//                "            \"Description\": \"Additional Discount\",\n" +
//                "            \"Value\": 450.00\n" +
//                "        },\n" +
//                "        {\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"ChargeMaster\": \"a033h000003j26jAAA\",\n" +
//                "            \"ChargeType\": \"Tax\",\n" +
//                "            \"Amount\": 150.00,\n" +
//                "            \"AmountType\": \"Fixed\",\n" +
//                "            \"IsApplied\": \"Yes\",\n" +
//                "            \"Description\": \"Packing Type\",\n" +
//                "            \"Value\": 150.00\n" +
//                "        },\n" +
//                "        {\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"ChargeMaster\": \"a033h000003j188AAA\",\n" +
//                "            \"ChargeType\": \"Tax\",\n" +
//                "            \"Amount\": 8.00,\n" +
//                "            \"AmountType\": \"Floating\",\n" +
//                "            \"IsApplied\": \"Yes\",\n" +
//                "            \"Description\": \"Size/Width (in cm)\",\n" +
//                "            \"Value\": 240.00\n" +
//                "        }\n" +
//                "    ],\n" +
//                "    \"salesOrderDetailLst\": [\n" +
//                "        {\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"productId\": \"01t3h0000019DJ0AAM\",\n" +
//                "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
//                "            \"quantity\": 10.00,\n" +
//                "\t\t\t\"isDelete\": false,\n" +
//                "            \"status\": \"Open\",\n" +
//                "            \"allocatedQuantity\": 0.00,\n" +
//                "            \"salesPrice\": 300.00,\n" +
//                "            \"discount\": 120.00,\n" +
//                "            \"tax\": 1150.00\n" +
//                "        },\n" +
//                "\t{\n" +
//                "            \"Id\": null,\n" +
//                "\t\t\t\"productId\": \"01t3h000002jAjuAAE\",\n" +
//                "            \"uOMId\": \"a0H3h000001T3KwEAK\",\n" +
//                "            \"quantity\": 5.00,\n" +
//                "\t\t\t\"isDelete\": false,\n" +
//                "            \"status\": \"Open\",\n" +
//                "            \"allocatedQuantity\": 0.00,\n" +
//                "            \"salesPrice\": 200.00,\n" +
//                "            \"discount\": 4.00,\n" +
//                "            \"tax\": 1000.00\n" +
//                "        }\n" +
//                "\t\t\n" +
//                "    ],\n" +
//                " \"salesOrder\": \n" +
//                "        {\n" +
//                "\t\t\t\"Id\": null,\n" +
//                "            \"State\": \"Draft\",\n" +
//                "            \"DealerId\": \"0013h0000092PXNAA2\",\n" +
//                "            \"Narration\": \"abc narration\",\n" +
//                "            \"BranchId\": \"0013h0000092PY5AAM\",\n" +
//                "            \"CustomerId\": \"0033h00000OZJG2AAP\",\n" +
//                "\t\t\t\"status\": \"Draft\",\n" +
//                "            \"TotalCost\": 4507.00,\n" +
//                "            \"Subtotal\": 3000.00,\n" +
//                "            \"Tax\": 1150.00,\n" +
//                "            \"Discount\": 120.00,\n" +
//                "            \"AdditionalChargeApplied\": 510.00,\n" +
//                "            \"AdditionalDiscountApplied\": 33.00\n" +
//                "        }\n" +
//                "}";
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
                "\t\t\t\"Id\": \""+orderObject.orderID+"\",\n" +
                "            \"State\": \"draft\",\n" +
                "            \"Status\": \""+orderID+"\",\n" +
                "            \"DealerId\": \""+orderObject.dealerID+"\",\n" +
                "            \"Narration\": \""+orderObject.narration+"\",\n" +
                "            \"BranchId\": \""+orderObject.branchID+"\",\n" +
                "            \"CustomerId\": \""+orderObject.customerID+"\",\n" +
                "            \"TotalCost\": "+getTotal(subtotal,tax,disc,getAddCost()-getAddDiscount())+",\n" +
                "            \"Subtotal\": "+subtotal+",\n" +
                "            \"Tax\": "+getTax(orderObject.products)+",\n" +
                "            \"Discount\": "+getDiscount(orderObject.products)+",\n" +
                "            \"AdditionalChargeApplied\": "+getAddCost()+",\n" +
                "            \"AdditionalDiscountApplied\": "+getAddDiscount()+"\n" +
                "        }\n" +
                "}";

        Log.d("2515", "submitOrder: "+payload);
        Log.d("2515", "submitPatch: Trying to update order Id:"+ orderObject.orderID);
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
            Log.d("CreateSalesOrder", response.body().string());
            //Log.d("2515", response.toString());
            new TinyDB(getApplicationContext()).putBoolean("CloseNewOrder", true);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (orderID){
                        case "Draft":
                            dialog_load.dismiss();
                            Toast.makeText(getApplicationContext(),"Order saved as Draft!", Toast.LENGTH_SHORT).show();
                            break;
                        case "Submitted For Approval":
                            dialog_load.dismiss();
                            Toast.makeText(getApplicationContext(),"Order submitted for approval!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            });

            finish();
        } catch (IOException e) {
            Log.d("2515", "submitPatch: "+e);
            e.printStackTrace();
        }
    }

    private void getChargeList() throws UnsupportedEncodingException {

        docCharges.clear();

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
                                String id = record.getJSONObject(i).getString("Id");
                                String masterid =record.getJSONObject(i).getString("ChargeMaster__c");
                                JSONObject charge = record.getJSONObject(i).getJSONObject("ChargeMaster__r");
                                String desc = charge.getString("Description__c");

                                String type =  charge.getString("AmountType__c");
                                String disc1 =  charge.getString("ChargeType__c");
                                double amount = type.equals("Fixed") ? charge.getDouble("Amount__c") : (getSubtotal(cartProducts)+getTax(cartProducts)-getDiscount(cartProducts))* (charge.getDouble("Amount__c")/100);
                                DocCharge d = new DocCharge(desc,id, masterid, disc1.equals("Tax"),amount);
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
                                                ((TextView)findViewById(R.id.textView103)).setText(round(amount)+"");

                                                //((CheckBox)findViewById(R.id.checkBox)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView12,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                break;

                                            case 1:

                                                findViewById(R.id.checkBox3).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView104).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView105).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox3)).setText(name);
                                                ((TextView)findViewById(R.id.textView105)).setText(round(amount)+"");

                                                //((CheckBox)findViewById(R.id.checkBox3)).setChecked(isApplied);



//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView13,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                break;

                                            case 2:

                                                findViewById(R.id.checkBox4).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView106).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView107).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox4)).setText(name);
                                                ((TextView)findViewById(R.id.textView107)).setText(round(amount)+"");

                                                //((CheckBox)findViewById(R.id.checkBox4)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);

                                                break;

                                            case 3:

                                                findViewById(R.id.checkBox5).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView108).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView109).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox5)).setText(name);
                                                ((TextView)findViewById(R.id.textView109)).setText(round(amount)+"");

                                                //((CheckBox)findViewById(R.id.checkBox5)).setChecked(isApplied);

//                        constraintSet.connect(R.id.cardView11,ConstraintSet.TOP,R.id.cardView14,ConstraintSet.BOTTOM,16);
//                        constraintSet.applyTo(constraintLayout);
                                                break;

                                            case 4:
                                                findViewById(R.id.checkBox6).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView110).setVisibility(View.VISIBLE);
                                                findViewById(R.id.textView111).setVisibility(View.VISIBLE);
                                                ((CheckBox)findViewById(R.id.checkBox6)).setText(name);
                                                ((TextView)findViewById(R.id.textView111)).setText(round(amount)+"");

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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("order", cartProducts);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    private void showWarning(){

        new AlertDialog.Builder(this)
                .setMessage("All the changes you've made will be discarded! Do you want to go back?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //HostActivity.super.onBackPressed();
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();

    }
}
