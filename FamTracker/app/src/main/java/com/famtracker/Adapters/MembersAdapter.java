package com.famtracker.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.famtracker.Activities.MembersActivity;
import com.famtracker.Activities.MembersLocationOnMapActivity;
import com.famtracker.Models.Members;
import com.famtracker.R;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by darshan on 03/05/17.
 */

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.Holder> {

    Context context;
    ArrayList<Members> membersArrayList;
    ImageLoader imageLoader;
    DisplayImageOptions  options;

    public MembersAdapter(Context context, ArrayList<Members> membersArrayList) {
        this.context = context;
        this.membersArrayList = membersArrayList;
    }

    private void initUil() {

        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc(true).cacheInMemory(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .displayer(new FadeInBitmapDisplayer(300)).build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .memoryCache(new WeakMemoryCache())
                .discCacheSize(100 * 1024 * 1024).build();

        ImageLoader.getInstance().init(config);

        dispImg();
    }

    private void dispImg() {

        imageLoader = ImageLoader.getInstance();
        options = new DisplayImageOptions.Builder().cacheInMemory(true)
                .cacheOnDisc(true).resetViewBeforeLoading(true)
                .build();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.single_member_view, parent, false);
        return new Holder(view);

    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {

        final Members mMembers = membersArrayList.get(position);
        initUil();
        String imageUrl = mMembers.getPhotoUrl()+"";

        holder.memberName.setText(mMembers.getName());
        holder.emailId.setText(mMembers.getEmail());
        if (imageUrl != null || !(imageUrl.equals(" "))) {
                imageLoader.displayImage(imageUrl,holder.imageView,options);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uid = mMembers.getUid();
                Intent intent = new Intent(context, MembersLocationOnMapActivity.class);
                intent.putExtra("uid", uid);
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return membersArrayList.size();
    }

    class Holder extends RecyclerView.ViewHolder {

        TextView memberName, emailId;
        CircleImageView imageView;

        public Holder(View itemView) {
            super(itemView);

            memberName = (TextView) itemView.findViewById(R.id.memberName);
            emailId = (TextView) itemView.findViewById(R.id.emailId);
            imageView = (CircleImageView) itemView.findViewById(R.id.profile_image);

        }
    }

}
