package com.github.scala.android.crud

import common.UriPath
import com.github.triangle._
import persistence.EntityType
import view.ViewField._
import persistence.CursorField._
import res.R

/** An EntityType for testing.
  * @author Eric Pabst (epabst@gmail.com)
  */

class MyEntityType extends EntityType {
  def entityName: String = "MyMap"

  def valueFields = List[BaseField](
    persisted[String]("name") + viewId(R.id.name, textView),
    persisted[Int]("age") + viewId(R.id.age, intView),
    //here to test a non-UI field
    persisted[String]("uri") + Getter[UriPath,String](u => Some(u.toString)))
}

object MyEntityType extends MyEntityType
