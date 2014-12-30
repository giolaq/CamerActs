package com.laquysoft.cameracts.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.laquysoft.cameracts.data.VotingContract.VotingEntry;

/**
 * Manages a local database for data.
 */
public class VotingDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "cameracts.db";

    public VotingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {


        final String SQL_CREATE_VOTING_TABLE = "CREATE TABLE " + VotingEntry.TABLE_NAME + " (" +
                VotingEntry._ID + " INTEGER PRIMARY KEY," +
                VotingEntry.COLUMN_VOTING_URL + " TEXT UNIQUE NOT NULL, " +
                VotingEntry.COLUMN_DATETEXT + " TEXT NOT NULL, " +
                VotingEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                VotingEntry.COLUMN_DESCRIPTION + " TEXT NOT NULL, " +
                VotingEntry.COLUMN_VOTERS_NUMBER + " INTEGER NOT NULL, " +
                VotingEntry.COLUMN_FAVOUR_NUMBER + " INTEGER NOT NULL, " +
                VotingEntry.COLUMN_AGAINST_NUMBER + " INTEGER NOT NULL, " +
                VotingEntry.COLUMN_ABSTAINED_NUMBER + " INTEGER NOT NULL, " +
                " UNIQUE (" + VotingEntry.COLUMN_DATETEXT + ", " +
                VotingEntry.COLUMN_NAME + ") ON CONFLICT REPLACE);";


        sqLiteDatabase.execSQL(SQL_CREATE_VOTING_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + VotingEntry.TABLE_NAME);

        onCreate(sqLiteDatabase);

    }
}