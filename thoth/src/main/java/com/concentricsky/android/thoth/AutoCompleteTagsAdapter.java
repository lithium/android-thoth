package com.concentricsky.android.thoth;

import android.*;
import android.R;
import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorAdapter;

/**
 * Created by wiggins on 7/7/13.
 */
public class AutoCompleteTagsAdapter extends SimpleCursorAdapter implements SimpleCursorAdapter.CursorToStringConverter {
    public AutoCompleteTagsAdapter(Context context) {
        super(context, R.layout.simple_list_item_1, null, new String[] {"title"}, new int[] {android.R.id.text1}, 0);
//        setCursorToStringConverter(this);
    }

    @Override
    public CharSequence convertToString(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow("title"));
    }
//    SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
//            android.R.layout.simple_list_item_1,
//            cursor, new String[] {"title"}, new int[] {android.R.id.text1}, 0);
//    adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
//        @Override
//        public CharSequence convertToString(Cursor cursor) {
//            return cursor.getString(cursor.getColumnIndexOrThrow("title"));
//        }
//    });
}
