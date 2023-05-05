package com.pwc.d2ep;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SalesOrdersAdapter extends RecyclerView.Adapter<SalesOrdersAdapter.ViewHolder> {

    private static ArrayList<SalesOrder> visits;
    private static Context c;
//    AppDatabase db;
//
//    TaskDao taskDao;
    // RecyclerView recyclerView;
    public SalesOrdersAdapter(ArrayList<SalesOrder> listdata, Context context) {
        this.visits = listdata;
        this.c = context;
//        db = Room.databaseBuilder(c,
//                AppDatabase.class, "d2ep_db").build();
//        taskDao = db.taskDao();
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.order_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
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

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final SalesOrder myListData = visits.get(position);
        holder.tvName.setText(visits.get(position).getCustomerName());
        holder.tvAddress.setText(visits.get(position).getBranchName());
        holder.tvDate.setText(visits.get(position).getDate());
        holder.tvTotal.setText("₹ "+round(Double.parseDouble(visits.get(position).getTotalCost())));
        holder.tvProductCount.setText(visits.get(position).getProductCount());
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

        holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.status_yellow,null));

        if (visits.get(position).getStatus().equals("null")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.colorPrimary,null));
        }

        if (visits.get(position).getStatus().equals("Draft")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(android.R.color.holo_blue_dark,null));
        }
        if (visits.get(position).getStatus().equals("Final")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.status_yellow,null));
        }

        if (visits.get(position).getStatus().equals("Closed")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.status_completed,null));
        }


    }

    @Override
    public int getItemCount() {
        return  visits.size();  //visits.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvAddress;
        public TextView tvDate, tvTotal;
        public View vPriority;
        ImageView ivHigh, ivMissed;
        TextView tasks, tvProductCount;
        public ViewHolder(View itemView) {
            super(itemView);

            this.tvName = (TextView) itemView.findViewById(R.id.tvNameVisitItem);
            this.tvDate = (TextView) itemView.findViewById(R.id.tvDateVisitItem);
            this.vPriority = itemView.findViewById(R.id.vPriorityVisitItem);
            this.tvAddress = (TextView) itemView.findViewById(R.id.tvAddressVisitItem);
            this.ivHigh = itemView.findViewById(R.id.imageView3);
            this.ivMissed = itemView.findViewById(R.id.imageView5);
            this.tvTotal = itemView.findViewById(R.id.tvTotalCostOrderItem);
            this.tvProductCount = itemView.findViewById(R.id.tvProductCountOrderItem);
//            this.tasks = itemView.findViewById(R.id.textView35);
//            itemView.findViewById(R.id.textView28).setVisibility(View.VISIBLE);
//            this.tasks.setVisibility(View.VISIBLE);

            itemView.findViewById(R.id.cvVisitItem).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        Intent i = new Intent(c, SalesOrderDetails.class);
                        i.putExtra("orderID", visits.get(getAdapterPosition()).oderID);
                    i.putExtra("orderName", visits.get(getAdapterPosition()).orderName);
                        c.startActivity(i);
                }
            });
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
