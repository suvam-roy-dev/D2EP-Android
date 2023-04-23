package com.pwc.d2ep;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.pwc.d2ep.db.AppDatabase;
import com.pwc.d2ep.db.TaskDao;
import com.salesforce.androidsdk.rest.RestClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class VisitDashboardAdapter extends RecyclerView.Adapter<VisitDashboardAdapter.ViewHolder> {

    private static ArrayList<Visit> visits;
    private static Context c;
//    AppDatabase db;
//
//    TaskDao taskDao;
    // RecyclerView recyclerView;
    public VisitDashboardAdapter(ArrayList<Visit> listdata, Context context) {
        this.visits = listdata;
        this.c = context;
//        db = Room.databaseBuilder(c,
//                AppDatabase.class, "d2ep_db").build();
//        taskDao = db.taskDao();
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
            //holder.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate)+" | "+split[1]);
            String date1 = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate);
            String time1 = split[1];
            String sDate1 = date1 + " " + time1;
            String formattedDate = "";

            try {
                SimpleDateFormat sdf1 = new SimpleDateFormat("dd MMM yyyy hh:mm",Locale.getDefault());
                sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date11=sdf1.parse(sDate1);
                SimpleDateFormat s = new SimpleDateFormat("dd MMM yyyy hh:mm", Locale.getDefault());
                s.setTimeZone(TimeZone.getTimeZone("IST"));
                //  function will helps to get the GMT Timezone
                // using the getTimeZOne() method
                Calendar cal = Calendar.getInstance(); // creates calendar
                cal.setTime(date11);               // sets calendar time/date
                cal.add(Calendar.HOUR_OF_DAY, 5);
                cal.add(Calendar.MINUTE, 30);// adds one hour
                formattedDate = s.format(cal.getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            String ftime = formattedDate.substring(formattedDate.length()-5,formattedDate.length());

            holder.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate)+" | "+ftime);
        } catch (ParseException e) {
            e.printStackTrace();
            holder.tvDate.setText(split[0]+" | "+split[1]);
        }

//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                int num = taskDao.loadVisitTasks(visits.get(position).getID()).length;
//
//                holder.tasks.setText(""+num);
//            }
//        });


        holder.tvAddress.setText(visits.get(position).getAddress());
        //holder.tvAddress.setText(visits.get(position).getAddress());
//        if (visits.get(position).getPriority().equals("New")){
//            holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.red,null));
//        }

        Date strDate = null;
        if (!visits.get(position).getPriority().equals("Completed")) {
            try {
                strDate = sdf.parse(visits.get(position).getDate());
                if (new Date().after(strDate) && !visits.get(position).getPriority().equals("Completed")) {
                    holder.ivMissed.setVisibility(View.VISIBLE);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        //Log.d("2512 Date", "Visit Date: "+ strDate.toString()+" is: " +(new Date().after(strDate) ? "After" : "Before")+ new Date().toString());


        if (visits.get(position).getStatus().equals("High")){
            holder.ivHigh.setVisibility(View.VISIBLE);
        }

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
        return Math.min(10, visits.size());  //visits.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvAddress;
        public TextView tvDate;
        public View vPriority;
        ImageView ivHigh, ivMissed;
        TextView tasks;
        public ViewHolder(View itemView) {
            super(itemView);

            this.tvName = (TextView) itemView.findViewById(R.id.tvNameVisitItem);
            this.tvDate = (TextView) itemView.findViewById(R.id.tvDateVisitItem);
            this.vPriority = itemView.findViewById(R.id.vPriorityVisitItem);
            this.tvAddress = (TextView) itemView.findViewById(R.id.tvAddressVisitItem);
            this.ivHigh = itemView.findViewById(R.id.imageView3);
            this.ivMissed = itemView.findViewById(R.id.imageView5);
//            this.tasks = itemView.findViewById(R.id.textView35);
//            itemView.findViewById(R.id.textView28).setVisibility(View.VISIBLE);
//            this.tasks.setVisibility(View.VISIBLE);

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
