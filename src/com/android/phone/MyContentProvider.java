package com.android.phone;
/*zhanglu.hoperun 2012.7.13 add this File to save PRL version and esn*/
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class MyContentProvider extends ContentProvider
{
  private SQLiteDatabase sqliteDatase;

  @Override
  public boolean onCreate()
  {
    MySqliteOpenHelper openHelper = new MySqliteOpenHelper(this.getContext(), null, null, 1);
    sqliteDatase = openHelper.getWritableDatabase();
    if(sqliteDatase==null)
      return false;
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) 
  {
    Cursor cursor = sqliteDatase.query
    (
      uri.getLastPathSegment(),
      projection,
      selection,
      selectionArgs,
      null,
      null,
      sortOrder
    );
    return cursor;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values)
  {
    sqliteDatase.insert(uri.getLastPathSegment(), null, values);
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    sqliteDatase.delete(uri.getLastPathSegment(), selection,selectionArgs );
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) {
    sqliteDatase.update(uri.getLastPathSegment(), values, selection, selectionArgs);
    return 0;
  }

}
/*zhanglu.hoperun 2012.7.13 end*/