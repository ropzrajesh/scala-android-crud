package com.github.scala.android.crud.generate

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.github.triangle.{PortableField, ValueFormat}
import PortableField._
import com.github.scala.android.crud.view.ViewField._
import com.github.scala.android.crud.ParentField._
import com.github.scala.android.crud.testres.R
import com.github.scala.android.crud._
import org.scalatest.mock.MockitoSugar

/** A behavior specification for [[com.github.scala.android.crud.generate.EntityFieldInfo]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[JUnitRunner])
class EntityFieldInfoSpec extends Spec with MustMatchers with MockitoSugar {
  describe("viewFields") {
    it("must find all ViewFields") {
      val dummyFormat = ValueFormat[String](s => Some(s + "."), _.stripSuffix("."))
      val fieldList = mapField[String]("foo") + textView + formatted[String](dummyFormat, textView) + viewId(45, textView)
      val info = ViewFieldInfo("foo", fieldList)
      info.viewFields must be(List(textView, textView, textView))
    }
  }

  it("must handle a viewId name that does not exist") {
    val fieldInfo = EntityFieldInfo(viewId(classOf[R.id], "bogus", textView), List(classOf[R])).viewFieldInfos.head
    fieldInfo.id must be ("bogus")
  }

  it("must consider a ParentField displayable if it has a viewId field") {
    val fieldInfo = EntityFieldInfo(ParentField(MyEntityType) + viewId(classOf[R], "foo", longView), Seq(classOf[R]))
    fieldInfo.displayable must be (true)
  }

  it("must not include a ParentField if it has no viewId field") {
    val fieldInfos = EntityFieldInfo(ParentField(MyEntityType), Seq(classOf[R])).viewFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include adjustment fields") {
    val fieldInfos = EntityFieldInfo(adjustment[String](_ + "foo"), Seq(classOf[R])).viewFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include adjustmentInPlace fields") {
    val fieldInfos = EntityFieldInfo(adjustmentInPlace[StringBuffer] { s => s.append("foo"); Unit }, Seq(classOf[R])).viewFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include the default primary key field") {
    val fieldInfos = EntityFieldInfo(MyCrudType.entityType.IdField, Seq(classOf[R])).viewFieldInfos
    fieldInfos must be (Nil)
  }

  it("must not include a ForeignKey if it has no viewId field") {
    val fieldInfo = EntityFieldInfo(foreignKey(MyEntityType), Seq(classOf[R]))
    fieldInfo.updateable must be (false)
  }

  it("must detect multiple ViewFields in the same field") {
    val fieldInfos = EntityFieldInfo(viewId(R.id.foo, textView) + viewId(R.id.bar, textView), Seq(classOf[R.id])).viewFieldInfos
    fieldInfos.map(_.id) must be (List("foo", "bar"))
  }
}
