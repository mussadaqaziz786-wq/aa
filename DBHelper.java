package com.example.dockyardworkerrecord;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "dockyard.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE = "records";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create = "CREATE TABLE " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "pno TEXT, name TEXT, rank TEXT, workshop TEXT, contact TEXT, place TEXT, hoist TEXT, cardno TEXT, entry_time TEXT, exit_time TEXT, date TEXT" +
                ")";
        db.execSQL(create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public long insertRecord(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(TABLE, null, values);
    }

    public int markExit(String cardno, String exitTime) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("exit_time", exitTime);
        return db.update(TABLE, cv, "cardno=? AND exit_time IS NULL", new String[]{cardno});
    }

    public Cursor getTodayRecords(String date) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE + " WHERE date=? ORDER BY id", new String[]{date});
    }

}
