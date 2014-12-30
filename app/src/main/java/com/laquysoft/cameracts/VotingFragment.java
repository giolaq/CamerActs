package com.laquysoft.cameracts;

/**
 * Created by joaobiriba on 10/11/14.
 */

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Date;


import com.laquysoft.cameracts.data.VotingContract;
import com.laquysoft.cameracts.data.VotingContract.VotingEntry;
import com.laquysoft.cameracts.sync.CamerActsSyncAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class VotingFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private VotingAdapter mVotingAdapter;
    private final static String LOG_TAG = VotingFragment.class.getSimpleName();

    private static final int VOTING_LOADER = 0;
    private String mLegislature;


    public static final int COL_VOTING_ID = 0;
    public static final int COL_VOTING_URL = 1;
    public static final int COL_VOTING_DATE = 2;
    public static final int COL_VOTING_NAME = 3;
    public static final int COL_VOTING_DESCRIPTION = 4;
    public static final int COL_VOTING_VOTERS = 5;
    public static final int COL_VOTING_FAVOUR = 6;
    public static final int COL_VOTING_AGAINST = 7;
    public static final int COL_VOTING_ABSTAINED = 8;

 
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


    private int mPosition = ListView.INVALID_POSITION;
    private static final String SELECTED_KEY = "selected_position";
    private ListView listView;
    private boolean mUseTodayLayout;


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(int id);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(VOTING_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.votingfragment, menu);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_data_camera) {
            openCameraOpenDataWebSite();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openCameraOpenDataWebSite() {

        if ( null != mVotingAdapter) {
            Cursor c = mVotingAdapter.getCursor();
            if ( null != c ) {
                c.moveToPosition(0);
                Uri dataCameraUrl = Uri.parse("http://dati.camera.it/it/");

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(dataCameraUrl);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + dataCameraUrl.toString() + ", no receiving apps installed!");
                }
            }

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // The SimpleCursorAdapter will take data from the database through the
        // Loader and use it to populate the ListView it's attached to.
        mVotingAdapter = new VotingAdapter(
                getActivity(),
                null,
                0
        );


        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        listView = (ListView) rootView.findViewById(R.id.listview_voting);
        listView.setAdapter(mVotingAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mVotingAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    ((Callback) getActivity())
                            .onItemSelected(cursor.getInt(COL_VOTING_ID));
                }
                mPosition = position;

            }

        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }


        return rootView;
    }


    private void updateWeather() {
        CamerActsSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        String startDate = VotingContract.getDbDateString(new Date());
        mLegislature = Utility.getPreferredLegislature(getActivity());
       
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                VotingEntry.CONTENT_URI,
                VOTING_COLUMNS,
                null,
                null,
                null
        );
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mVotingAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            listView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVotingAdapter.swapCursor(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLegislature != null && !mLegislature.equals(Utility.getPreferredLegislature(getActivity()))) {
            getLoaderManager().restartLoader(VOTING_LOADER, null, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }


}