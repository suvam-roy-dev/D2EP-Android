package com.pwc.d2ep;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.ViewHolder> {

    private static ArrayList<VisitTask> visits;
    private static Context c;
    // RecyclerView recyclerView;
    public TaskListAdapter(ArrayList<VisitTask> listdata, Context context) {
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
        final VisitTask myListData = visits.get(position);
        holder.tvName.setText(visits.get(position).getSubject());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date strDate = sdf.parse(visits.get(position).getDate());
            holder.tvDate.setText("Date: "+new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(strDate));

        } catch (ParseException e) {
            e.printStackTrace();
            holder.tvDate.setText("Date: "+visits.get(position).getDate());

        }


        if (visits.get(position).getPriority().equals("High")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(R.color.red,null));
        }

        if (visits.get(position).getPriority().equals("Not Started")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(android.R.color.holo_blue_dark,null));
        }

        if (visits.get(position).getPriority().equals("Completed")){
            holder.vPriority.setBackgroundColor(c.getResources().getColor(android.R.color.holo_green_dark,null));
        }
    }


    @Override
    public int getItemCount() {
        return visits.size();  //visits.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName;
        public TextView tvDate;
        public View vPriority;
        public ViewHolder(View itemView) {
            super(itemView);

            this.tvName = (TextView) itemView.findViewById(R.id.tvNameVisitItem);
            this.tvDate = (TextView) itemView.findViewById(R.id.tvDateVisitItem);
            this.vPriority = itemView.findViewById(R.id.vPriorityVisitItem);

            itemView.findViewById(R.id.cvVisitItem).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                        Intent i = new Intent(c, TaskDetailsActivity.class);
                        i.putExtra("TASKID", visits.get(getAdapterPosition()).getTaskId());
                    i.putExtra("VISITID", visits.get(getAdapterPosition()).getVisitId());
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        c.startActivity(i);

                }
            });
        }
    }
}
