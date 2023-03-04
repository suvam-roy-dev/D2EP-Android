package com.pwc.d2ep;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CartProductAdapter extends RecyclerView.Adapter<CartProductAdapter.ViewHolder> {

    private static ArrayList<CartProduct> visits;
    private static Context c;
    private boolean canDelete;
//    AppDatabase db;
//
//    TaskDao taskDao;
    // RecyclerView recyclerView;
    public CartProductAdapter(ArrayList<CartProduct> listdata, Context context, boolean canDelete) {
        this.visits = listdata;
        this.c = context;
        this.canDelete = canDelete;
//        db = Room.databaseBuilder(c,
//                AppDatabase.class, "d2ep_db").build();
//        taskDao = db.taskDao();
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
        holder.tvName.setText(visits.get(position).name);
        holder.tvQty.setText(visits.get(position).qty+"");
        holder.tvUOM.setText(visits.get(position).UOM);
        holder.tvSalePrice.setText(visits.get(position).salesPrice+"");
        holder.tvTax.setText(visits.get(position).salesPrice * (visits.get(position).tax/100)+"");
        holder.tvTotal.setText(visits.get(position).total+"");
        holder.tvDisc.setText("-"+visits.get(position).salesPrice * (visits.get(position).discount/100)+"");

//        String date = visits.get(position).getDate();
//
//        date = date.substring(0,date.length()-3);
//
//        String[] split = date.split(",");
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        try {
//            Date strDate = sdf.parse(split[0]);
//            holder.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate)+" | "+split[1]);
//
//        } catch (ParseException e) {
//            e.printStackTrace();
//            holder.tvDate.setText(split[0]+" | "+split[1]);
//        }

//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                int num = taskDao.loadVisitTasks(visits.get(position).getID()).length;
//
//                holder.tasks.setText(""+num);
//            }
//        });


        //holder.tvAddress.setText(visits.get(position).getAddress());
        //holder.tvAddress.setText(visits.get(position).getAddress());
//        if (visits.get(position).getPriority().equals("New")){
//            holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.red,null));
//        }

        //Log.d("2512 Date", "Visit Date: "+ strDate.toString()+" is: " +(new Date().after(strDate) ? "After" : "Before")+ new Date().toString());

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

//            this.tasks = itemView.findViewById(R.id.textView35);
//            itemView.findViewById(R.id.textView28).setVisibility(View.VISIBLE);
//            this.tasks.setVisibility(View.VISIBLE);

            itemView.findViewById(R.id.button13).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                   Dialog dialog = new Dialog(view.getContext());
//
//                    dialog.setContentView(R.layout.dialog_product);
//                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//                    dialog.setCancelable(true);
//                    dialog.setCanceledOnTouchOutside(true);
//
//                    dialog.show();
                }
            });

            itemView.findViewById(R.id.button12).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (canDelete)
                        removeAt(getAdapterPosition());

                }
            });
        }
    }

    public ArrayList<CartProduct> getUpdatedList(){
        return visits;
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
        visits.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, visits.size());
    }}
