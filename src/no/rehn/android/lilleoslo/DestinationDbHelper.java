package no.rehn.android.lilleoslo;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class DestinationDbHelper {
    static final String LOG_CATEGORY = "db"; 
    static final String DATABASE_NAME = "destinations.db";
    static final String MAIN_TABLE = "mainTable";
    static final int VERSION = 1;
    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    SQLiteDatabase mDb;
    Context mContext;
    DbHelper mDbHelper;

    public DestinationDbHelper(Context context) {
        mContext = context;
        mDbHelper = new DbHelper(context, DATABASE_NAME,  null, VERSION);
        mDb = mDbHelper.getWritableDatabase();
    }
    
    public List<Destination> getAllEntries() {
        List<Destination> result = new ArrayList<Destination>();
        String[] columns = new String[] {KEY_ID, KEY_NAME, KEY_LATITUDE, KEY_LONGITUDE };
        Cursor cursor = mDb.query(MAIN_TABLE, columns, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                float lat = cursor.getFloat(2);
                float lon = cursor.getFloat(3);
                String name = cursor.getString(1);
                int id = cursor.getInt(0);
                result.add(new Destination(id, name, lat, lon));
            } while (cursor.moveToNext());
        }
        return result;
    }
    
    public boolean remove(Destination destination) {
        return mDb.delete(MAIN_TABLE, KEY_ID + "=" + destination.id, null) > 0;
    }
    
    public long save(Destination destination) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, destination.name);
        values.put(KEY_LONGITUDE, destination.location.getLongitude());
        values.put(KEY_LATITUDE, destination.location.getLatitude());
        Log.i(LOG_CATEGORY, "saved destination");
        return mDb.insert(MAIN_TABLE, null, values);
    }
    
    class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + MAIN_TABLE + " (" + 
                    KEY_ID + " integer primary key autoincrement, " +
                    KEY_NAME + " text, " + 
                    KEY_LATITUDE + " double, " + 
                    KEY_LONGITUDE + " double);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("drop table if exists " + MAIN_TABLE);
            onCreate(db);
        }
    }
}
