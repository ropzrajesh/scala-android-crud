package com.github.scala_android.crud

import android.database.sqlite.SQLiteOpenHelper
import android.database.Cursor
import android.content.{ContentValues, Context}
import android.widget.{CursorAdapter, ListAdapter}

/**
 * A CrudEntityType for SQLite.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/24/11
 * Time: 11:22 PM
 */

trait SQLiteCrudEntityType extends CrudEntityType[SQLiteCriteria,Cursor,Cursor,ContentValues] {
  def newWritable = new ContentValues

  def openEntityPersistence(crudContext: CrudContext) = new SQLiteEntityPersistence(this, crudContext)

  def refreshAfterSave(listAdapter: ListAdapter) {
    listAdapter.asInstanceOf[CursorAdapter].getCursor.requery
  }

  def getDatabaseSetup(context: Context): SQLiteOpenHelper
}
