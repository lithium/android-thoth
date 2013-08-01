package com.concentricsky.android.pensive;

import android.content.Context;

/**
 * Created by wiggins on 6/5/13.
 */
public class DateUtils {

    public static String fuzzyTimestamp(Context context, long timestamp)
    {
        final long millis = System.currentTimeMillis() - timestamp;
        final double seconds = millis / 1000;
        final double minutes = seconds / 60;
        final double hours = minutes / 60;
        final double days = hours / 24;
        final double years = days / 365;

        final String out;
        if (seconds < 45)
            out = context.getString(R.string.time_ago_seconds);
        else if (seconds < 90)
            out = context.getString(R.string.time_ago_minute);
        else if (minutes < 45)
            out = context.getString(R.string.time_ago_minutes, (long)Math.round(minutes));
        else if (minutes < 90)
            out = context.getString(R.string.time_ago_hour);
        else if (hours < 24)
            out = context.getString(R.string.time_ago_hours, (long)Math.round(hours));
        else if (hours < 48)
            out = context.getString(R.string.time_ago_day);
        else if (days < 30)
            out = context.getString(R.string.time_ago_days, (long)Math.floor(days));
        else if (days < 60)
            out = context.getString(R.string.time_ago_month);
        else if (days < 365)
            out = context.getString(R.string.time_ago_months, (long)Math.floor(days / 30));
        else if (years < 2)
            out = context.getString(R.string.time_ago_year);
        else
            out = context.getString(R.string.time_ago_years, (long)Math.floor(years));

        return out;
    }
}
