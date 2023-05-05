package com.pwc.d2ep;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class CartProductAdapter extends RecyclerView.Adapter<CartProductAdapter.ViewHolder> {

    private static ArrayList<CartProduct> visits;
    private static ArrayList<CartProduct> removedVisits;
    private final Context c;
    private final boolean canDelete;
//    AppDatabase db;
//
//    TaskDao taskDao;
    // RecyclerView recyclerView;
    public CartProductAdapter(ArrayList<CartProduct> listdata, ArrayList<CartProduct> removedVisits, Context context, boolean canDelete) {
        this.visits = listdata;
        this.removedVisits = removedVisits;
        this.c = context;
        this.canDelete = canDelete;

    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.cart_product_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //final SalesOrder myListData = visits.get(position);
        double fixedCharges = 0.0;
        double floatingCharges = 0.0;
        double fixedDisc = 0.0;
        double floatingDisc = 0.0;

        CartProduct selectedProduct = visits.get(position);
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

        holder.tvName.setText(visits.get(position).name);
        holder.tvQty.setText(visits.get(position).qty+"");
        holder.tvUOM.setText(visits.get(position).UOM);
        holder.tvSalePrice.setText(round(visits.get(position).salesPrice)+"");
        holder.tvTax.setText(round((visits.get(position).qty*visits.get(position).salesPrice*finalFloatingCharges/100)+finalFixedCharges)+"");
        holder.tvDisc.setText(round((visits.get(position).qty*visits.get(position).salesPrice*finalFloatingDisc/100)+finalFixedDisc)+"");
        holder.tvTotal.setText(round(visits.get(position).total)+"");

        //holder.tvDisc.setText(round(visits.get(position).discount));
    }

    @Override
    public int getItemCount() {
        return  visits.size();  //visits.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvQty, tvUOM;
        public TextView tvSalePrice, tvTotal, tvTax, tvDisc;
        public ViewHolder(View itemView) {
            super(itemView);

            this.tvName = (TextView) itemView.findViewById(R.id.tvProductNameProductTab);

            this.tvQty = (TextView) itemView.findViewById(R.id.tvQty);
            this.tvUOM = (TextView) itemView.findViewById(R.id.tvUOM);
            this.tvSalePrice = (TextView) itemView.findViewById(R.id.tvSalesPrice);
            this.tvTax = (TextView) itemView.findViewById(R.id.tvCharges);
            this.tvDisc = (TextView) itemView.findViewById(R.id.tvDiscount);
            this.tvTotal = (TextView) itemView.findViewById(R.id.tvTotalCost);

            if(canDelete) {
                itemView.findViewById(R.id.button13).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showEditDialog(visits.get(getAdapterPosition()));
                    }
                });

                itemView.findViewById(R.id.button12).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                            removeAt(getAdapterPosition());
                    }
                });
            }else {
                ((ImageView) itemView.findViewById(R.id.imageView7)).setImageTintList(ColorStateList.valueOf(c.getColor(R.color.disbaled_color)));
                ((ImageView) itemView.findViewById(R.id.imageView8)).setImageTintList(ColorStateList.valueOf(c.getColor(R.color.disbaled_color)));

            }

            itemView.findViewById(R.id.imageButton4).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    reduceQty(visits.get(getAdapterPosition()),false);
                }
            });

            itemView.findViewById(R.id.imageButton5).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    reduceQty(visits.get(getAdapterPosition()),true);
                }
            });
        }
    }

    private void reduceQty(CartProduct selectedProduct, boolean add){


        ArrayList<ProductCharges> thisCharges = selectedProduct.charges;


        final int[] qtyToAdd = {selectedProduct.qty};

        if(add) {
            qtyToAdd[0]++;
        }else {
            if (qtyToAdd[0] > 1){
                qtyToAdd[0]--;
            }else {
                return;
            }
        }

        double fixedCharge = 0;
        double floatingCharge = 0;
        double fixedDiscount = 0;
        double floatingDiscount = 0;

        for (int i = 0; i < thisCharges.size(); i++) {
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
        }

        selectedProduct.qty = qtyToAdd[0];
        selectedProduct.tax = fixedCharge + (qtyToAdd[0]*selectedProduct.salesPrice*floatingCharge/100);
        selectedProduct.discount = fixedDiscount + (qtyToAdd[0]*selectedProduct.salesPrice*floatingDiscount/100);
        selectedProduct.total = qtyToAdd[0]*(selectedProduct.salesPrice+ (selectedProduct.salesPrice*floatingCharge/100) - (selectedProduct.salesPrice*floatingDiscount/100))+ fixedCharge - fixedDiscount;

        for (int i = 0; i < visits.size(); i++) {
            if (selectedProduct.name.matches(visits.get(i).name)) {
                //cartProducts.remove(i);
                visits.set(i, selectedProduct);
                break;
            }
        }
        notifyDataSetChanged();
        }

    private void showEditDialog(CartProduct selectedProduct) {

//        final double[] changedPrice = {selectedProduct.salesPrice};
//        final double[] changedCharge = {selectedProduct.tax};
//        final double[] changedDisc = {selectedProduct.discount};
//
//        final int[] qtyToAdd = {selectedProduct.qty};
//
//        Dialog dialog = new Dialog(c);
//
//        dialog.setContentView(R.layout.dialog_product);
//        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        dialog.setCancelable(true);
//        dialog.setCanceledOnTouchOutside(false);
//
//        dialog.findViewById(R.id.imageButton7).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (qtyToAdd[0] >1) qtyToAdd[0]--;
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *changedPrice[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView71)).setText(round(qtyToAdd[0] *changedCharge[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView72)).setText(round(qtyToAdd[0] *changedDisc[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd[0] *(changedPrice[0]+changedCharge[0]-changedDisc[0]))+"");
//            }
//        });
//        dialog.findViewById(R.id.imageButton8).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                qtyToAdd[0]++;
//                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *changedPrice[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView71)).setText(round(qtyToAdd[0] *changedCharge[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView72)).setText(round(qtyToAdd[0] *changedDisc[0])+"");
//                ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd[0] *(changedPrice[0]+changedCharge[0]-changedDisc[0]))+"");
//            }
//        });
//
//        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
//        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
//        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.UOM);
//        ((EditText)dialog.findViewById(R.id.editText)).setText(round(selectedProduct.salesPrice)+"");
//        ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *selectedProduct.salesPrice)+"");
//
//        dialog.findViewById(R.id.textView63).setVisibility(View.GONE);
//        dialog.findViewById(R.id.editText3).setVisibility(View.GONE);
//        dialog.findViewById(R.id.textView72).setVisibility(View.GONE);
//
//        for (int i = 0; i < 2; i++) {
//            switch (i){
//                case 0:
//                    ((TextView)dialog.findViewById(R.id.textView55)).setText("Charges");
//                    ((EditText)dialog.findViewById(R.id.editText2)).setText(round(selectedProduct.tax)+"");
//                    ((TextView)dialog.findViewById(R.id.textView71)).setText(round(qtyToAdd[0] *selectedProduct.tax)+"");
//                    break;
//                case 1:
//                    ((TextView)dialog.findViewById(R.id.textView63)).setText("Discounts");
//                    ((EditText)dialog.findViewById(R.id.editText3)).setText(round(selectedProduct.discount)+"");
//                    ((TextView)dialog.findViewById(R.id.textView72)).setText(round(qtyToAdd[0] *selectedProduct.discount)+"");
//                    dialog.findViewById(R.id.textView63).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.editText3).setVisibility(View.VISIBLE);
//                    dialog.findViewById(R.id.textView72).setVisibility(View.VISIBLE);
//                    break;
//            }
//        }
//
//        dialog.findViewById(R.id.button16).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
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
//                for (int i = 0; i < visits.size(); i++) {
//                    if (selectedProduct.name.matches(visits.get(i).name)) {
//                        //cartProducts.remove(i);
//                        visits.set(i, selectedProduct);
//                        break;
//                    }
//                }

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
        Dialog dialog = new Dialog(c);

        dialog.setContentView(R.layout.dialog_product);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        dialog.findViewById(R.id.imageButton7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (qtyToAdd[0] >1) qtyToAdd[0]--;
                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
                //((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *changedPrice[0])+"");
                ((TextView)dialog.findViewById(R.id.textView71)).setText(round((qtyToAdd[0]*changedPrice[0]*finalFloatingCharges/100)+finalFixedCharges)+"");
                ((TextView)dialog.findViewById(R.id.textView72)).setText(round((qtyToAdd[0]*changedPrice[0]*finalFloatingDisc/100)+finalFixedDisc)+"");

                ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd[0]*(changedPrice[0]+ (changedPrice[0]*finalFloatingCharges/100) - (changedPrice[0]*finalFloatingDisc/100))+ finalFixedCharges - finalFixedDisc)+"");

            }
        });
        dialog.findViewById(R.id.imageButton8).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qtyToAdd[0]++;
                ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
//                ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *changedPrice[0])+"");
                ((TextView)dialog.findViewById(R.id.textView71)).setText(round((qtyToAdd[0]*changedPrice[0]*finalFloatingCharges/100)+finalFixedCharges)+"");
                ((TextView)dialog.findViewById(R.id.textView72)).setText(round((qtyToAdd[0]*changedPrice[0]*finalFloatingDisc/100)+finalFixedDisc)+"");
                ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd[0]*(changedPrice[0]+ (changedPrice[0]*finalFloatingCharges/100) - (changedPrice[0]*finalFloatingDisc/100))+ finalFixedCharges - finalFixedDisc)+"");
            }
        });


        ((TextView)dialog.findViewById(R.id.textView46)).setText(selectedProduct.name);
        ((TextView)dialog.findViewById(R.id.tvQty2)).setText(qtyToAdd[0] +"");
        ((TextView)dialog.findViewById(R.id.textView47)).setText(selectedProduct.UOM);
        ((EditText)dialog.findViewById(R.id.editText)).setText(round(selectedProduct.salesPrice)+"");
        ((TextView)dialog.findViewById(R.id.textView52)).setText(round(qtyToAdd[0] *selectedProduct.salesPrice)+"");

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
                    ((EditText)dialog.findViewById(R.id.editText2)).setText(round(thisCharges.get(i).chargeValue)+"");
                    double total = thisCharges.get(i).chargeValueType.equals("Fixed") ? thisCharges.get(i).amount : qtyToAdd[0] * selectedProduct.salesPrice * thisCharges.get(i).chargeValue/100;
                    ((TextView)dialog.findViewById(R.id.textView71)).setText(round(total)+"");

                    dialog.findViewById(R.id.textView55).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.editText2).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.textView71).setVisibility(View.VISIBLE);
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

                selectedProduct.charges = thisCharges;
                selectedProduct.salesPrice = cost;
                selectedProduct.qty = qtyToAdd[0];
                selectedProduct.tax = fixedCharge + (qtyToAdd[0]*cost*floatingCharge/100);
                selectedProduct.discount = fixedDiscount + (qtyToAdd[0]*cost*floatingDiscount/100);
                selectedProduct.total = qtyToAdd[0]*(cost+ (cost*floatingCharge/100) - (cost*floatingDiscount/100))+ fixedCharge - fixedDiscount;

                for (int i = 0; i < visits.size(); i++) {
                    if (selectedProduct.name.matches(visits.get(i).name)) {
                        //cartProducts.remove(i);
                        visits.set(i, selectedProduct);
                        break;
                    }
                }
                dialog.dismiss();

                notifyDataSetChanged();

            }
        });

        ((TextView)dialog.findViewById(R.id.textView76)).setText(round(qtyToAdd[0]*(changedPrice[0]+ (changedPrice[0]*finalFloatingCharges/100) - (changedPrice[0]*finalFloatingDisc/100))+ finalFixedCharges - finalFixedDisc)+"");
        dialog.show();
    }

    public static String round(double value) {
        String val = String.valueOf((double) Math.round(value * 100) / 100);
        return ((val.charAt(val.length()-1)) == '0' || (val.charAt(val.length()-2)) == '.') ? val+"0":val;
    }

    public ArrayList<CartProduct> getUpdatedList(){
        return visits;
    }
    public ArrayList<CartProduct> getUpdatedRemovedList(){
        return removedVisits;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public void removeAt(int position) {
        removedVisits.add(visits.get(position));
        visits.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, visits.size());
    }}
