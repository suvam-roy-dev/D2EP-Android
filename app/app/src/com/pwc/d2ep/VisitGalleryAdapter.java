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

public class VisitGalleryAdapter extends RecyclerView.Adapter<VisitGalleryAdapter.ViewHolder> {

    private static ArrayList<Visit> visits;
    private static Context c;
    // RecyclerView recyclerView;

    public VisitGalleryAdapter(ArrayList<Visit> listdata, Context context) {
        this.visits = listdata;
        this.c = context;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.visit_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Visit myListData = visits.get(position);
        holder.tvName.setText(visits.get(position).getName());

        String date = visits.get(position).getStartTime();

        date = date.substring(0,date.length()-3);

        String[] split = date.split(",");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date strDate = sdf.parse(split[0]);
            holder.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate)+" | "+split[1]);
            if(new Date().after(strDate) && !visits.get(position).getPriority().equals("Completed")){
                holder.ivMissed.setVisibility(View.VISIBLE);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            holder.tvDate.setText(split[0]+" | "+split[1]);

        }


//        Date strDate = null;
//        try {
//            strDate = sdf.parse(visits.get(position).getDate());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        holder.tvAddress.setText(visits.get(position).getAddress());
        if (visits.get(position).getStatus().equals("High")){
            holder.ivHigh.setVisibility(View.VISIBLE);
        }
//        if (visits.get(position).getPriority().equals("New")){
//            holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.red,null));
//        }

        if (visits.get(position).getPriority().equals("New")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(android.R.color.holo_blue_dark,null));
        }

        if (visits.get(position).getPriority().equals("InProgress")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.status_yellow,null));
        }

        if (visits.get(position).getPriority().equals("Completed")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.status_completed,null));
        }
    }


    @Override
    public int getItemCount() {
        return  visits.size();  //visits.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvAddress;
        public TextView tvDate;
        public View vPriority;
        ImageView ivHigh, ivMissed;
        public ViewHolder(View itemView) {
            super(itemView);

            this.tvName = (TextView) itemView.findViewById(R.id.tvNameVisitItem);
            this.tvDate = (TextView) itemView.findViewById(R.id.tvDateVisitItem);
            this.vPriority = itemView.findViewById(R.id.vPriorityVisitItem);
            this.tvAddress = (TextView) itemView.findViewById(R.id.tvAddressVisitItem);
            this.ivHigh = itemView.findViewById(R.id.imageView3);
            this.ivMissed = itemView.findViewById(R.id.imageView5);

            itemView.findViewById(R.id.cvVisitItem).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (visits.get(getAdapterPosition()).getType().equals("Visit")) {
                        Intent i = new Intent(c, VisitDetailsActivity.class);
                        i.putExtra("ID", visits.get(getAdapterPosition()).getID());

                        c.startActivity(i);
                    }

                    if (visits.get(getAdapterPosition()).getType().equals("Task")) {
                        Intent i = new Intent(c, TaskDetailsActivity.class);
                        i.putExtra("ID", visits.get(getAdapterPosition()).getID());
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        c.startActivity(i);
                    }
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
