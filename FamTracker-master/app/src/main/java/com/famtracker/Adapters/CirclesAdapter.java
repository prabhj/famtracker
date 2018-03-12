package com.famtracker.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.famtracker.Activities.MembersActivity;
import com.famtracker.Models.Circles;
import com.famtracker.R;

import java.util.ArrayList;

/**
 * Created by prabhjot on 03/05/17.
 */

public class CirclesAdapter extends RecyclerView.Adapter<CirclesAdapter.Holder> {

    Context context;
    ArrayList<Circles> mArrayList;

    public CirclesAdapter(Context context, ArrayList<Circles> mArrayList) {
        this.context = context;
        this.mArrayList = mArrayList;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.singlee_circle_view, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, final int position) {

        final Circles mCircles = mArrayList.get(position);

        holder.circlesName.setText(mCircles.getCircleName());
        holder.description.setText(mCircles.getDescription());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mCirclesId = mCircles.getId();
                goToMemberActivity(mCircles);
            }
        });

    }

    private void goToMemberActivity(Circles circle) {

        Intent intent = new Intent(context, MembersActivity.class);
        intent.putExtra("id", circle.getId());
        intent.putExtra("name",circle.getCircleName());
        intent.putExtra("description",circle.getDescription());

        context.startActivity(intent);

    }

    @Override
    public int getItemCount() {
        return mArrayList.size();
    }

    public class Holder extends RecyclerView.ViewHolder {

        TextView circlesName, description;

        public Holder(View itemView) {
            super(itemView);
            circlesName = (TextView) itemView.findViewById(R.id.circleName);
            description = (TextView) itemView.findViewById(R.id.description);
        }
    }
}
