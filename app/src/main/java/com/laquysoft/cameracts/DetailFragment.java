package com.laquysoft.cameracts;

/**
 * Created by joaobiriba on 03/12/14.
 */

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.laquysoft.cameracts.data.VotingContract;
import com.laquysoft.cameracts.data.VotingContract.VotingEntry;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String CAMERACTS_SHARE_HASHTAG = " #CamerActs";

    private static final String LOCATION_KEY = "location";

    private ShareActionProvider mShareActionProvider;
    private String mLocation;
    private String mForecast;

    private static final int DETAIL_LOADER = 0;


    private static final String[] VOTING_COLUMNS = {
            VotingEntry.TABLE_NAME + "." + VotingEntry._ID,
            VotingEntry.COLUMN_VOTING_URL,
            VotingEntry.COLUMN_DATETEXT,
            VotingEntry.COLUMN_NAME,
            VotingEntry.COLUMN_DESCRIPTION,
            VotingEntry.COLUMN_VOTERS_NUMBER,
            VotingEntry.COLUMN_FAVOUR_NUMBER,
            VotingEntry.COLUMN_AGAINST_NUMBER,
            VotingEntry.COLUMN_ABSTAINED_NUMBER

    };


    private ImageView mIconView;
    private TextView mFriendlyDateView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mFavourNumberView;
    private TextView mAgainstNumberView;
    private TextView mAbstainedNumberView;
    private TextView mWindView;
    private TextView mPressureView;

    private VotationChart mPie;
    private int mVotingId;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(LOCATION_KEY, mLocation);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mVotingId = arguments.getInt(DetailActivity.VOTING_KEY);
        }
        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getString(LOCATION_KEY);
        }


        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mFriendlyDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_description_textview);
        mFavourNumberView = (TextView) rootView.findViewById(R.id.favour_number_tv);
        mAgainstNumberView = (TextView) rootView.findViewById(R.id.against_number_tv);
        mAbstainedNumberView = (TextView) rootView.findViewById(R.id.abstained_number_tv);

        mPie = (VotationChart) rootView.findViewById(R.id.Pie);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.VOTING_KEY) &&
                mLocation != null &&
                !mLocation.equals(Utility.getPreferredLegislature(getActivity()))) {
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(LOG_TAG, "in onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_detail_fragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mForecast != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + CAMERACTS_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getString(LOCATION_KEY);
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        mLocation = Utility.getPreferredLegislature(getActivity());
        Uri votationByIdUri = VotingContract.VotingEntry.buildVotingUri(mVotingId);

        Log.v(LOG_TAG, votationByIdUri.toString());

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                votationByIdUri,
                VOTING_COLUMNS,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {

            String name = data.getString(data.getColumnIndex(VotingEntry.COLUMN_NAME));
            mFriendlyDateView.setText(name);

            String votingDescription = data.getString(data.getColumnIndex(VotingEntry.COLUMN_DESCRIPTION));
            mDescriptionView.setText(votingDescription);

            int against_number = data.getInt(data.getColumnIndex(VotingEntry.COLUMN_AGAINST_NUMBER));
            int favour_number = data.getInt(data.getColumnIndex(VotingEntry.COLUMN_FAVOUR_NUMBER));
            int abstained_number = data.getInt(data.getColumnIndex(VotingEntry.COLUMN_ABSTAINED_NUMBER));

            mFavourNumberView.setText(getActivity().getString(R.string.format_favour, favour_number));
            mAgainstNumberView.setText(getActivity().getString(R.string.format_against, against_number));
            mAbstainedNumberView.setText(getActivity().getString(R.string.format_abstained, abstained_number));


            mPie.reset();
            Resources res = getResources();
            mPie.addItem("Against", against_number, res.getColor(R.color.italy_red));
            mPie.addItem("Favour", favour_number, res.getColor(R.color.italy_green));
            mPie.addItem("Abstained", abstained_number, res.getColor(R.color.italy_white));

            // We still need this for the share intent
            mForecast = String.format("Voting %s %s - F:%s,AG:%s,AB:%s ", name, votingDescription,
                    favour_number, against_number,
                    abstained_number);


            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}