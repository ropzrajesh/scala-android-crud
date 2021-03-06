package com.github.scala.android.crud.persistence

import com.github.triangle._
import com.github.scala.android.crud.common._
import PlatformTypes._
import CursorField.PersistedId
import UriPath.uriIdField

/** An entity configuration that provides information needed to map data to and from persistence.
  * This shouldn't depend on the platform (e.g. android).
  * @author Eric Pabst (epabst@gmail.com)
  */
trait EntityType extends FieldList with Logging {
  override lazy val logTag = Common.tryToEvaluate(entityName).getOrElse(Common.logTag)

  //this is the type used for internationalized strings
  def entityName: String

  object UriPathId extends Field[ID](uriIdField(entityName))

  /** This should only be used in order to override this.  IdField should be used instead of this.
    * A field that uses IdPk.id is NOT included here because it could match a related entity that also extends IdPk,
    * which results in many problems.
    */
  protected def idField: PortableField[ID] = UriPathId + PersistedId
  object IdField extends Field[ID](idField)

  /** The fields other than the primary key. */
  def valueFields: List[BaseField]

  /** The idField along with accessors for IdPk instances. */
  lazy val idPkField = IdField + Getter[IdPk,ID](_.id).withTransformer(e => e.id(_)) +
    Setter((e: MutableIdPk) => e.id = _)
  lazy val fieldsIncludingIdPk = FieldList((idPkField +: fields): _*)

  /** These are all of the entity's fields, which includes IdPk.idField and the valueFields. */
  final lazy val fields: List[BaseField] = IdField +: valueFields

  def toUri(id: ID) = UriPath(entityName, id.toString)

  lazy val defaultPortableValue = copyFrom(PortableField.UseDefaults)

  override def toString() = entityName
}
