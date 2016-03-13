package com.mikhailkazhamiaka.viatickets;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by mikhailkazhamiaka on 2016-02-22.
 */

public class DBAdapter {

    public static abstract class TicketsEntry implements BaseColumns {
        public static final String DATABASE_NAME="VIATicketsDB";
        public static final String TABLE_NAME = "Tickets";
        public static final String COLUMN_NAME_ENTRY_ID = "entryid";
        public static final String COLUMN_NAME_ORIGIN = "origin";
        public static final String COLUMN_NAME_DESTINATION = "destination";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_DATETIME = "date_time";
        public static final String COLUMN_NAME_TRAIN_NUMBER = "train_number";
        public static final String COLUMN_NAME_CAR = "car";
        public static final String COLUMN_NAME_SEAT = "seat";
        public static final String COLUMN_NAME_PDF_FILE = "pdf_file";
        public static final String COLUMN_NAME_QR_FILE = "qr_file";

        public static final String TABLE_CREATE = "create table " + TABLE_NAME + " ("
                + _ID + " integer primary key autoincrement, "
                + COLUMN_NAME_ORIGIN + " VARCHAR not null, "
                + COLUMN_NAME_DESTINATION + " VARCHAR not null, "
                + COLUMN_NAME_DATE + " VARCHAR not null, "
                + COLUMN_NAME_TIME + " VARCHAR not null, "
                + COLUMN_NAME_DATETIME + " DATETIME not null, "
                + COLUMN_NAME_TRAIN_NUMBER + " INTEGER not null, "
                + COLUMN_NAME_CAR + " INTEGER not null, "
                + COLUMN_NAME_SEAT + " VARCHAR not null, "
                + COLUMN_NAME_PDF_FILE + " VARCHAR not null, "
                + COLUMN_NAME_QR_FILE + " VARCHAR not null);";

        public static final int DATABASE_VERSION = 2;
    }

    private final Context context;
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DBAdapter(Context c) {
        this.context = c;
        DBHelper = new DatabaseHelper(context);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context c) {
            super(c, TicketsEntry.DATABASE_NAME, null, TicketsEntry.DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(TicketsEntry.TABLE_CREATE);
            } catch(Exception e){
                Log.d("DB", "CREATING TABLES FAILED");
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //TODO: implement onUpgrade
            onCreate(db);
        }
    }

    public DBAdapter open() throws SQLException {
        db = DBHelper.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TicketsEntry.TABLE_CREATE.replace("create table", ""));
        return this;
    }

    public void close() {
        DBHelper.close();
    }

    // insert a new ticket
    public long insertTicket(Ticket ticket) {
        Log.i("INSERTING TICKET", "datetime value is " + ticket.getDatetimeFormat());
        ContentValues cv = new ContentValues();
        cv.put(TicketsEntry.COLUMN_NAME_ORIGIN, ticket.getOrigin());
        cv.put(TicketsEntry.COLUMN_NAME_DESTINATION, ticket.getDestination());
        cv.put(TicketsEntry.COLUMN_NAME_DATE, ticket.getDate());
        cv.put(TicketsEntry.COLUMN_NAME_TIME, ticket.getTime());
        cv.put(TicketsEntry.COLUMN_NAME_DATETIME, ticket.getDatetimeFormat());
        cv.put(TicketsEntry.COLUMN_NAME_TRAIN_NUMBER, Integer.parseInt(ticket.getTrainNumber()));
        cv.put(TicketsEntry.COLUMN_NAME_CAR, Integer.parseInt(ticket.getCar()));
        cv.put(TicketsEntry.COLUMN_NAME_SEAT, ticket.getSeat());
        cv.put(TicketsEntry.COLUMN_NAME_PDF_FILE, ticket.getPdfFile());
        cv.put(TicketsEntry.COLUMN_NAME_QR_FILE, ticket.getQrFile());

        return db.insert(TicketsEntry.TABLE_NAME, null, cv);
    }

    // returns all tickets sorted by datetime in descending order
    public Cursor getAllTickets() {
        return db.query(TicketsEntry.TABLE_NAME, null, null, null, null, null, TicketsEntry.COLUMN_NAME_DATETIME + " DESC");
    }

    // returns all tickets between date1 and date2
    public Cursor getTicketsBetweenDates(String date1, String date2) {
        //date1 = date1.replaceAll("/", "-");
        //date2 = date2.replaceAll("/", "-");
        //String split1[] = date1.split("/");
        //String split2[] = date2.split("/");
        //date1 = split1[2] + "-" + split1[1] + "-" + split1[0];
        //date2 = split2[2] + "-" + split2[1] + "-" + split2[0];
        //return db.query(TicketsEntry.TABLE_NAME, null,
         //       TicketsEntry.COLUMN_NAME_DATETIME + " > \"" + date1 + "\" AND " + TicketsEntry.COLUMN_NAME_DATETIME + " < \"" + date2 + "\"",
         //       null, null, null, TicketsEntry.COLUMN_NAME_DATETIME + " DESC");
        return db.query(TicketsEntry.TABLE_NAME, null,
                TicketsEntry.COLUMN_NAME_DATETIME + " BETWEEN \'" + date1 + " 00:00:00\' AND \'" + date2 + " 23:59:59\'",
                null, null, null, TicketsEntry.COLUMN_NAME_DATETIME + " DESC");
    }

    public void clearAllData() {
        db.delete(TicketsEntry.TABLE_NAME, null, null);
    }

}
