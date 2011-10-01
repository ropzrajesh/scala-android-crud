package com.github.scala.android.crud.action

import com.github.scala.android.crud.common.PlatformTypes
import com.github.triangle.ValueFormat._
import android.net.Uri

/**
 * A convenience wrapper for UriPath.
 * It helps in that UriPath.EMPTY is null when running unit tests, and helps prepare for multi-platform support.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 9/22/11
 * Time: 10:42 PM
 */
case class UriPath(segments: String*) extends PlatformTypes {
  private val idFormat = basicFormat[ID]

  def /(segment: String): UriPath = UriPath(segments :+ segment:_*)

  def /(id: ID): UriPath = this / idFormat.toString(id)

  def specify(finalSegments: String*): UriPath =
    UriPath.replacePathSegments(this, _.takeWhile(_ != finalSegments.head) ++ finalSegments.toList)

  def findId(entityName: String): Option[ID] =
    segments.dropWhile(_ != entityName).toList match {
      case _ :: idString :: tail => idFormat.toValue(idString)
      case _ => None
    }

  def upToIdOf(entityName: String): UriPath = specify(entityName, findId(entityName).get.toString)

  override def toString = segments.mkString("/", "/", "")
}

object UriPath {
  val EMPTY: UriPath = UriPath()

  import scala.collection.JavaConversions._
  def apply(uri: Uri): UriPath = UriPath(uri.getPathSegments.toList:_*)

  private[UriPath] def replacePathSegments(uri: UriPath, f: Seq[String] => Seq[String]): UriPath = {
    val path = f(uri.segments)
    UriPath(path: _*)
  }
}