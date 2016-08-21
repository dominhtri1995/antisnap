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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jakewharton.rxbinding.view.RxView;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.functions.Action1;

/**
 * Created by an vo on 8/11/2016.
 */
public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.MyViewHolder> {
    private Context context;
    List<Friend> friendList = Collections.emptyList();
    private LayoutInflater inflater;

    public FriendRequestAdapter(Context context, List<Friend> friendList) {
        this.context = context;
        this.friendList= friendList;
        inflater = LayoutInflater.from(context);
    }
    @Override
    public FriendRequestAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recycler_view_friendrequest, parent, false);
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

        holder.accept.setImageResource(R.drawable.accept);
        holder.decline.setImageResource(R.drawable.decline);
    }

    public int getItemCount() {
        if(friendList!=null) {
            return friendList.size();
        }
        return 0;
    }
    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView profile;
        ImageView accept;
        ImageView decline;

        public MyViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            profile = (ImageView) itemView.findViewById(R.id.profile);
            accept = (ImageView) itemView.findViewById(R.id.accept);
            decline= (ImageView) itemView.findViewById(R.id.decline);
            RelativeLayout ln = (RelativeLayout) itemView.findViewById(R.id.frame);

            RxView.clicks(accept)
                    .throttleFirst(2, TimeUnit.SECONDS)
                    .subscribe(new Action1<Void>() {
                @Override
                public void call(Void aVoid) {
                    //Accept friend
                    EventBus.getDefault().post(new friendRequestAccepted(getLayoutPosition()));
                }
            });
            RxView.clicks(decline)
                    .throttleFirst(2,TimeUnit.SECONDS)
                    .subscribe(new Action1<Void>() {
                        @Override
                        public void call(Void aVoid) {

                            // Delete on Sever
                            EventBus.getDefault().post(new friendRequestDeclined(getLayoutPosition()));
                        }
                    });
        }

    }
    public Bitmap convertStringToImage(String imageFile) {
        byte[] imageAsBytes = Base64.decode(imageFile, Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
        return bmp;
    }
    public class friendRequestAccepted {
        public final int message;

        public friendRequestAccepted(int message) {
            this.message = message;
        }
    }
    public class friendRequestDeclined {
        public final int message;

        public friendRequestDeclined(int message) {
            this.message = message;
        }
    }
}
