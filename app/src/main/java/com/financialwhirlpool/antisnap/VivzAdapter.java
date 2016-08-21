package com.financialwhirlpool.antisnap;

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.financialwhirlpool.antisnap.Class.Friend;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.List;

/**
 * Created by an vo on 6/6/2016.
 */
public class VivzAdapter extends RecyclerView.Adapter<VivzAdapter.MyViewHolder> {
    private Context context;
    List<Friend> friendList = Collections.emptyList();
    private LayoutInflater inflater;

    public VivzAdapter(Context context, List<Friend> friendList) {
        this.context = context;
        this.friendList= friendList;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
        }

        // Has message - show SnapChat Icon
        if(friendList.get(position).isHasMessage()){
            holder.snapChat.setVisibility(View.VISIBLE);
            animateSnapChat(holder.snapChat);
            holder.numberOfMessage.setVisibility(View.VISIBLE);
            holder.numberOfMessage.setText(String.valueOf(friendList.get(position).getImageMessageList().size()));
        }else {
            holder.snapChat.setVisibility(View.INVISIBLE);
            holder.numberOfMessage.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView profile;
        ImageView snapChat;
        TextView numberOfMessage;
        public MyViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            profile = (ImageView) itemView.findViewById(R.id.profile);
            snapChat = (ImageView) itemView.findViewById(R.id.snapchat);
            numberOfMessage = (TextView) itemView.findViewById(R.id.numberofmessage);
            RelativeLayout ln = (RelativeLayout) itemView.findViewById(R.id.frame);

            ln.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    switch (motionEvent.getActionMasked()){
                        case MotionEvent.ACTION_DOWN:
                            Log.i("trido","Finger down in adapter");
                            EventBus.getDefault().post(new FingerDownMessageEvent(getLayoutPosition()));
                            return true;
                        case MotionEvent.ACTION_UP:
                            Log.i("trido","Finger up in adapter");
                            EventBus.getDefault().post(new FingerUpMessageEvent(getLayoutPosition()));
                            return true;
                    }
                    return false;
                }
            });
        }
    }

    public Bitmap convertStringToImage(String imageFile) {
        byte[] imageAsBytes = Base64.decode(imageFile, Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
        return bmp;
    }

    // EVENT BUS
    public class ImageMessageEvent {
        public final ImageView message;

        public ImageMessageEvent(ImageView message) {
            this.message = message;
        }
    }
    public class FingerDownMessageEvent {
        public final int message;

        public FingerDownMessageEvent(int message) {
            this.message = message;
        }
    }
    public class FingerUpMessageEvent {
        public final int messeage;
        public FingerUpMessageEvent(int messeage) {
            this.messeage = messeage;
        }
    }
    // alert has message
    public void animateSnapChat(final ImageView snapChat){
        ViewPropertyAnimator a= snapChat.animate().rotation(20f).setDuration(1000);
        a.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }
            @Override
            public void onAnimationEnd(Animator animator) {
                snapChat.animate().rotation(-20f).setDuration(2000);
            }
            @Override
            public void onAnimationCancel(Animator animator) {

            }
            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }
    // Add decor -- Line between items.
    public static class DividerItemDecoration extends RecyclerView.ItemDecoration {
        private final int[] ATTRS = new int[]{android.R.attr.listDividerAlertDialog};

        private Drawable mDivider;

        public DividerItemDecoration(Context context) {
            final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
            mDivider = styledAttributes.getDrawable(0);
            styledAttributes.recycle();
        }

        public DividerItemDecoration(Drawable divider) {
            mDivider = divider;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);

            if (parent.getChildAdapterPosition(view) == 0) {
                return;
            }

            outRect.top = mDivider.getIntrinsicHeight();
        }

        @Override
        public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            int dividerLeft = parent.getPaddingLeft();
            int dividerRight = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount - 1; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int dividerTop = child.getBottom() + params.bottomMargin;
                int dividerBottom = dividerTop + mDivider.getIntrinsicHeight();

                mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
                mDivider.draw(canvas);
            }
        }
    }
}
