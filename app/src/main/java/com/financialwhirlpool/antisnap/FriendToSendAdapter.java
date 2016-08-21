package com.financialwhirlpool.antisnap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.financialwhirlpool.antisnap.Class.Friend;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.List;

/**
 * Created by an vo on 8/10/2016.
 */
public class FriendToSendAdapter extends RecyclerView.Adapter<FriendToSendAdapter.MyViewHolder> {
    private Context context;
    List<Friend> friendList = Collections.emptyList();
    private LayoutInflater inflater;

    public FriendToSendAdapter(Context context, List<Friend> friendList) {
        this.context = context;
        this.friendList= friendList;
        inflater = LayoutInflater.from(context);
    }
    @Override
    public FriendToSendAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recycler_view, parent, false);
        MyViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.name.setText(friendList.get(position).getName());

        String profile =friendList.get(position).getProfile();
        if(profile != null) {
            Bitmap myBitmap = convertStringToImage(profile);
            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), myBitmap);
            drawable.setCircular(true);
            holder.profile.setImageDrawable(drawable);
        }else{
            // Set default profile
            holder.profile.setImageResource(R.drawable.snapchat);
        }

    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }
    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView profile;

        public MyViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            profile = (ImageView) itemView.findViewById(R.id.profile);
            RelativeLayout ln = (RelativeLayout) itemView.findViewById(R.id.frame);

            ln.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EventBus.getDefault().post(new FriendToSendClickEvent(getLayoutPosition()));
                }
            });
        }

    }
    public Bitmap convertStringToImage(String imageFile) {
        byte[] imageAsBytes = Base64.decode(imageFile, Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
        return bmp;
    }
    public class FriendToSendClickEvent {
        public final int message;

        public FriendToSendClickEvent(int message) {
            this.message = message;
        }
    }

}
