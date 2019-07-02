package unizar.master.simpleweather;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// https://abhiandroid.com/database/sqlite
public class myDbAdapter {

    myDbHelper myhelper;
    public myDbAdapter(Context context)
    {
        myhelper = new myDbHelper(context);
    }

    public long insertData(String name, String m, String cond, double n, int enabled)
    {
        SQLiteDatabase dbb = myhelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(myDbHelper.NAME, name);
        contentValues.put(myDbHelper.MEASURE, m);
        contentValues.put(myDbHelper.CONDITION, cond);
        contentValues.put(myDbHelper.NUMBER, n);
        contentValues.put(myDbHelper.ENABLED, enabled);
        long id = dbb.insert(myDbHelper.TABLE_NAME, null , contentValues);
        return id;
    }

    public Cursor getData()
    {
        SQLiteDatabase db = myhelper.getWritableDatabase();
        String[] columns = {myDbHelper.UID,myDbHelper.NAME,myDbHelper.MEASURE, myDbHelper.CONDITION, myDbHelper.NUMBER, myDbHelper.ENABLED};
        Cursor cursor =db.query(myDbHelper.TABLE_NAME,columns,null,null,null,null,null);
        return cursor;
    }

    public Cursor getData(long id){
        // Make a query
        SQLiteDatabase db = myhelper.getWritableDatabase();
        String dbQuery = "select * from " + myDbHelper.TABLE_NAME + " where " + myDbHelper.UID + " = " + id;
        Cursor c = null;
        try {
            c = db.rawQuery(dbQuery, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    public  int delete(long id)
    {
        SQLiteDatabase db = myhelper.getWritableDatabase();
        String[] whereArgs ={String.valueOf(id)};

        int count =db.delete(myDbHelper.TABLE_NAME ,myDbHelper.UID+" = ?",whereArgs);
        return  count;
    }

    public int updateData(long id, String name, String m, String cond, double n)//, int enabled)
    {
        SQLiteDatabase db = myhelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(myDbHelper.NAME, name);
        contentValues.put(myDbHelper.MEASURE, m);
        contentValues.put(myDbHelper.CONDITION, cond);
        contentValues.put(myDbHelper.NUMBER, n);
        //contentValues.put(myDbHelper.ENABLED, enabled);
        String[] whereArgs= {String.valueOf(id)};
        int count =db.update(myDbHelper.TABLE_NAME,contentValues, myDbHelper.UID+" = ?",whereArgs );
        return count;
    }

    public int updateEnabled(long id, int enabled)
    {
        SQLiteDatabase db = myhelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(myDbHelper.ENABLED, enabled);
        String[] whereArgs= {String.valueOf(id)};
        int count =db.update(myDbHelper.TABLE_NAME,contentValues, myDbHelper.UID+" = ?",whereArgs );
        return count;
    }

    public class myDbHelper extends SQLiteOpenHelper
    {
        private static final String DATABASE_NAME = "myDatabase";    // Database Name
        private static final String TABLE_NAME = "alarms";   // Table Name
        private static final int DATABASE_Version = 1;    // Database Version
        public static final String UID="_id";     // Column I (Primary Key)
        public static final String NAME = "name";    //Column II
        public static final String MEASURE = "measure";    // Column III
        public static final String CONDITION = "condition";    // Column III
        public static final String NUMBER = "num";    // Column III
        public static final String ENABLED = "enabled";    // Column IV
        private static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+
                " ("+UID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+NAME+" VARCHAR(255) ,"+ CONDITION+" VARCHAR(225),"
                +MEASURE+" VARCHAR(255) ,"+NUMBER+" REAL ,"+ ENABLED + " INTEGER);";
        private static final String DROP_TABLE ="DROP TABLE IF EXISTS "+TABLE_NAME;
        private Context context;

        public myDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_Version);
            this.context=context;
        }

        public void onCreate(SQLiteDatabase db) {

            try {
                db.execSQL(CREATE_TABLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                db.execSQL(DROP_TABLE);
                onCreate(db);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
