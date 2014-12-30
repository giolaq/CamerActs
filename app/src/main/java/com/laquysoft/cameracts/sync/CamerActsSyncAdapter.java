package com.laquysoft.cameracts.sync;

/**
 * Created by joaobiriba on 10/12/14.
 */


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;


import com.laquysoft.cameracts.R;
import com.laquysoft.cameracts.Utility;
import com.laquysoft.cameracts.data.VotingContract;
import com.laquysoft.cameracts.data.VotingContract.VotingEntry;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Vector;

public class CamerActsSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = CamerActsSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the weather, in milliseconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    public CamerActsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");
        // Getting the zipcode to send to the API
        String locationQuery = Utility.getPreferredLegislature(getContext());

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;


        try {


            final String VOTING_BASE_URL = "http://dati.camera.it/sparql?query=%23%23%23%23+tutte+le+votazioni+finali+della+XVII+Legislatura++%0D%0A%23%23%23%23+%28la+URI+%3Chttp%3A%2F%2Fdati.camera.it%2Focd%2Flegislatura.rdf%2F"+
                    "repubblica_"+ locationQuery +"%3E+identifica+la+Legislatura%29%0D%0A%0D%0ASELECT+distinct+*+WHERE+%7B%0D%0A%3Fvotazione+a+ocd%3Avotazione%3B+%0D%0Adc%3Adate+"+
                    "%3Fdata%3B+%0D%0Adc%3Atitle+%3Fdenominazione%3B+%0D%0Adc%3Adescription+%3Fdescrizione%3B%0D%0Aocd%3Avotanti+%3Fvotanti%3B%0D%0Aocd%3AvotazioneFinale+"+
                    "+1%3B%0D%0Aocd%3Afavorevoli+%3Ffavorevoli%3B%0D%0Aocd%3Acontrari+%3Fcontrari%3B%0D%0Aocd%3Aastenuti+%3Fastenuti%3B%0D%0Aocd%3Arif_leg+%3Chttp%3A%2F%2F"+
                    "dati.camera.it%2Focd%2Flegislatura.rdf%2Frepubblica_" +locationQuery+"%3E%7D+%0D%0AORDER+BY+DESC%28%3Fdata%29%0D%0A%09%09&debug=on&default-graph-uri=&format=application"+"" +
                    "%2Fsparql-results%2Bjson";

            Uri builtUri = Uri.parse(VOTING_BASE_URL).buildUpon()
                       .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_DATETIME = "dt";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";


        final String VOTING_RESULTS = "results";
        final String VOTING_BINDINGS = "bindings";


        final String VOTING_VOTING = "votazione";
        final String VOTING_DATE = "data";
        final String VOTING_NAME = "denominazione";
        final String VOTING_DESCRIPTION = "descrizione";
        final String VOTING_VOTERS = "votanti";
        final String VOTING_FAVOURS = "favorevoli";
        final String VOTING_AGAINST = "contrari";
        final String VOTING_ABSTAINED = "astenuti";

        final String VOTING_VALUE = "value";


        try {
            JSONObject votingJson = new JSONObject(forecastJsonStr);

            JSONObject votingResponseJson = votingJson.getJSONObject(VOTING_RESULTS);

            JSONArray votingArray = votingResponseJson.getJSONArray(VOTING_BINDINGS);


            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(votingArray.length());

            for (int i = 0; i < votingArray.length(); i++) {
                // These are the values that will be collected.

                String url;
                String date;
                String name;
                String description;
                int voters;
                int favour;
                int against;
                int abstained;


                // Get the JSON object representing the voting
                JSONObject voting = votingArray.getJSONObject(i);

                url = voting.getJSONObject(VOTING_VOTING).getString(VOTING_VALUE);

                date = voting.getJSONObject(VOTING_DATE).getString(VOTING_VALUE);

                name = voting.getJSONObject(VOTING_NAME).getString(VOTING_VALUE);

                description =  StringEscapeUtils.unescapeHtml4(
                        voting.getJSONObject(VOTING_DESCRIPTION).getString(VOTING_VALUE));

                voters = voting.getJSONObject(VOTING_VOTERS).getInt(VOTING_VALUE);
                favour = voting.getJSONObject(VOTING_FAVOURS).getInt(VOTING_VALUE);
                against = voting.getJSONObject(VOTING_AGAINST).getInt(VOTING_VALUE);
                abstained = voting.getJSONObject(VOTING_ABSTAINED).getInt(VOTING_VALUE);

                ContentValues votingValues = new ContentValues();

                votingValues.put(VotingEntry.COLUMN_VOTING_URL, url);
                votingValues.put(VotingEntry.COLUMN_DATETEXT, date);
                votingValues.put(VotingEntry.COLUMN_NAME, name);
                votingValues.put(VotingEntry.COLUMN_DESCRIPTION, description);
                votingValues.put(VotingEntry.COLUMN_VOTERS_NUMBER, voters);
                votingValues.put(VotingEntry.COLUMN_FAVOUR_NUMBER, favour);
                votingValues.put(VotingEntry.COLUMN_AGAINST_NUMBER, against);
                votingValues.put(VotingEntry.COLUMN_ABSTAINED_NUMBER, abstained);

                cVVector.add(votingValues);
            }
            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(VotingEntry.CONTENT_URI, cvArray);

               /* Calendar cal = Calendar.getInstance(); //Get's a calendar object with the current time.
                cal.add(Calendar.DATE, -1); //Signifies yesterday's date
                String yesterdayDate = VotingContract.getDbDateString(cal.getTime());
                getContext().getContentResolver().delete(WeatherEntry.CONTENT_URI,
                        WeatherEntry.COLUMN_DATETEXT + " <= ?",
                        new String[] {yesterdayDate});

                notifyWeather();*/

            }
            Log.d(LOG_TAG, "CamerActsSyncAdapter Complete updating " + cVVector.size() + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        // This will only happen if there was an error getting or parsing the forecast.
        return;
    }


    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */


        }
        return newAccount;
    }

    private
    static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        CamerActsSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


}