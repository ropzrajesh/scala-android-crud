package com.github.scala.android.crud.view

import com.xtremelabs.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import android.view.View
import com.github.scala.android.crud.action.{ActivityWithVars, Operation}
import com.github.scala.android.crud.{CrudApplication, CrudContext}
import com.github.triangle.PortableField
import com.github.scala.android.crud.common.UriPath

/** A specification of [[com.github.scala.android.crud.view.OnClickOperationSetter]].
  * @author Eric Pabst (epabst@gmail.com)
  */
@RunWith(classOf[RobolectricTestRunner])
class OnClickOperationSetterSpec extends MockitoSugar {
  @Test
  def itMustSetOnClickListenerWhenClicableIsTrue() {
    val operation = mock[Operation]
    val view = mock[View]
    stub(view.isClickable).toReturn(true)
    val setter = OnClickOperationSetter[Unit](_ => operation)
    setter.setValue(view, None, List(UriPath.EMPTY, CrudContext(mock[MyActivityWithVars], mock[CrudApplication]), PortableField.UseDefaults))
    verify(view).setOnClickListener(any())
  }

  @Test
  def itMustNotSetOnClickListenerWhenClickableIsFalse() {
    val operation = mock[Operation]
    val view = mock[View]
    stub(view.isClickable).toReturn(false)
    val setter = OnClickOperationSetter[Unit](_ => operation)
    setter.setValue(view, None, List(UriPath.EMPTY, CrudContext(mock[MyActivityWithVars], mock[CrudApplication]), PortableField.UseDefaults))
    verify(view, never()).setOnClickListener(any())
  }
}

class MyActivityWithVars extends ActivityWithVars
