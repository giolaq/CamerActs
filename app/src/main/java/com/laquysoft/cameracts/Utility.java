package com.laquysoft.cameracts;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.laquysoft.cameracts.data.VotingContract;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utility {
    public static String getPreferredLegislature(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_legislature_key),
                context.getString(R.string.pref_legislature_default));
    }


    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";


    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     *
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(String dateStr, boolean reduced) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        try {
            Date inputDate = dbDateFormat.parse(dateStr);
            SimpleDateFormat monthDayFormat;
            if ( !reduced ) {
                 monthDayFormat = new SimpleDateFormat("dd MMMM yyyy");
            }
            else
            {
                 monthDayFormat = new SimpleDateFormat("dd MM yy");
            }
            String monthDayString = monthDayFormat.format(inputDate);
            return monthDayString;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String getFormattedActName(String name) {
        String actName;
        if (name.contains("DDL")) {
             actName = name.substring(name.lastIndexOf("DDL"));
        } else if (name.contains("PDL")) {
             actName = name.substring(name.lastIndexOf("PDL"));
        }
        else actName = "N.A";
        return actName;
    }
}