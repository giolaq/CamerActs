package com.laquysoft.cameracts;


import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;

import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class VotingAdapter extends CursorAdapter {


    public VotingAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    private static final int VIEW_TYPE_EVEN = 0;
    private static final int VIEW_TYPE_ODD = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    /**
     * Copy/paste note: Replace existing newView() method in VotingAdapter with this one.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        Resources res = context.getResources();

        View view = LayoutInflater.from(context).inflate(R.layout.list_item_voting, parent, false);


        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Read date from cursor
        String dateString = cursor.getString(VotingFragment.COL_VOTING_DATE);
        // Find TextView and set formatted date on it
        viewHolder.dateView.setText(Utility.getFormattedMonthDay(dateString, true));

        // Read date from cursor
        String nameString = cursor.getString(VotingFragment.COL_VOTING_NAME);
        // Find TextView and set formatted date on it
        viewHolder.nameView.setText(nameString);

        // Read date from cursor
        int votersString = cursor.getInt(VotingFragment.COL_VOTING_VOTERS);
        // Find TextView and set formatted date on it
        viewHolder.votersView.setText(Integer.toString(votersString));


        viewHolder.numberView.setText(Utility.getFormattedActName(nameString));


    }

    @Override
    public int getItemViewType(int position) {
        return position % 2 == 0 ? VIEW_TYPE_EVEN : VIEW_TYPE_ODD;
    }

    /**
     * Cache of the children views for a votation list item.
     */
    public static class ViewHolder {
        public final TextView dateView;
        public final TextView nameView;
        public final TextView votersView;
        public final TextView numberView;

        public ViewHolder(View view) {
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            nameView = (TextView) view.findViewById(R.id.list_item_name_textview);
            votersView = (TextView) view.findViewById(R.id.list_item_voters_textview);
            numberView = (TextView) view.findViewById(R.id.list_item_number);
        }
    }
}