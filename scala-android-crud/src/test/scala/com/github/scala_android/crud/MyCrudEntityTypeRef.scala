package com.github.scala_android.crud

/**
 * A simple CrudEntityTypeRef for testing.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/15/11
 * Time: 10:40 PM
 */
object MyCrudEntityTypeRef extends CrudEntityTypeRef {
  def entityName = "MyEntity"

  def addItemString = R.string.add_item

  def editItemString = R.string.edit_item

  def cancelItemString = R.string.cancel_item

  def activityClass = classOf[CrudActivity[_,_,_,_]]

  def listActivityClass = classOf[CrudListActivity[_,_,_,_]]
}