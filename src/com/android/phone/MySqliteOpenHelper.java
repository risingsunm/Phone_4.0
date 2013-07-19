package com.android.phone;
/*zhanglu.hoperun 2012.7.13 add this File to save PRL version and esn*/
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MySqliteOpenHelper extends SQLiteOpenHelper
{
  private static String dbName = "ahongPhoneInfo.db";
  public MySqliteOpenHelper(Context context, String name,
      CursorFactory factory, int version)
  {
    super(context, dbName, null, version);
  }

  @Override
  public void onCreate(SQLiteDatabase db)
  {
     String sql = "create table if not exists info(id integer primary key autoincrement,prl varchar(100),esn varchar(100))";
       db.execSQL(sql);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
  {
    String sql = "drop table if exists info ";
    db.execSQL(sql);
    onCreate(db);
  }

}
/*zhanglu.hoperun 2012.7.13 end*/