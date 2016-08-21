package com.financialwhirlpool.antisnap;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.CursorAdapter;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by an vo on 8/10/2016.
 */
public class SuggestionAdapter extends CursorAdapter{
    private LayoutInflater cursorInflater;
    public SuggestionAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        cursorInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return cursorInflater.inflate(R.layout.suggestion_view,viewGroup, false);

    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView suggestionText = (TextView) view.findViewById(R.id.suggestionText);
        ImageView suggestionIcon= (ImageView) view.findViewById(R.id.suggestionIcon);
        suggestionText.setText(cursor.getString(2));

        String profile = cursor.getString(3);
        if(profile !=null) {
            Bitmap myBitmap = convertStringToImage(profile);
            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), myBitmap);
            drawable.setCircular(true);
            suggestionIcon.setImageDrawable(drawable);
        }
        else{
            suggestionIcon.setImageResource(R.drawable.snapchat);
        }
    }

    public Bitmap convertStringToImage(String imageFile) {
        byte[] imageAsBytes = Base64.decode(imageFile, Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
        return bmp;
    }
}
