package geeks.crud

import geeks.crud.util.Logging

/** A trait for {@link Field} for convenience such as when defining a List of heterogeneous Fields. */
trait CopyableField {
  /**
   * Copies this field from <code>from</code> to <code>to</code>.
   * @returns true if successfullly set a value
   */
  def copy(from: AnyRef, to: AnyRef): Boolean
}

/**
 * A Field of a specific type which has any number of FieldAccesses to Cursors, Views, Model objects, etc.
 * <p>
 * Example:
 * <pre>
 * import geeks.crud.android._
 * import geeks.crud.android.CursorFieldAccess._
 * import geeks.crud.android.PersistedType._
 * import geeks.crud.android.ViewFieldAccess._
 * import android.widget.{TextView, ListView}
 *
 * val fields = List(
 *   new Field[String](persisted("name"), viewId[TextView,String](R.id.name)),
 *   new Field[Double](persisted("score"), viewId[TextView,Double](R.id.score))
 * )
 * </pre>
 * <p>
 * Usage of implicits make this syntax concise for the simple cases, but allow for very complex situations as well
 * by providing custom implementations for the implicits.
 */
final class Field[T](fieldAccessArgs: PartialFieldAccess[T]*) extends CopyableField with Logging {
  val fieldAccesses: List[PartialFieldAccess[T]] = fieldAccessArgs.toList
  /**
   * Finds a value out of <code>from</code> by using the FieldAccess that can handle it.
   * @returns Some(value) if successful, otherwise None
   */
  def findValue(from: AnyRef): Option[T] = {
    for (fieldAccess <- fieldAccesses) {
      val value = fieldAccess.partialGet(from)
      if (value.isDefined) return value
    }
    None
  }

  /**
   * Sets a value in <code>to</code> by using all FieldAccesses that can handle it.
   * @return true if any were successful
   */
  def setValue(to: AnyRef, value: T): Boolean = {
    fieldAccesses.foldLeft(false)((result, access) => access.partialSet(to, value) || result)
  }

  //inherited
  def copy(from: AnyRef, to: AnyRef): Boolean = {
    findValue(from).map(value => {
      debug("Copying " + value + " from " + from + " to " + to + " for field " + this)
      setValue(to, value)
    }).getOrElse(false)
  }
}

/**
 * The base trait of all FieldAccesses.  This is based on PartialFunction.
 * @param T the value type that this FieldAccess consumes and provides.
 * @see #partialGet
 * @see #partialSet
 */
trait PartialFieldAccess[T] {
  /**
   * Tries to get the value from <code>readable</code>.
   * @param readable any kind of Object.  If it is not supported by this FieldAccess, this simply returns None.
   * @returns Some(value) if successful, otherwise None
   */
  def partialGet(readable: AnyRef): Option[T]

  /**
   * Tries to set the value in <code>writable</code>.
   * @param writable any kind of Object.  If it is not supported by this FieldAccess, this simply returns false.
   */
  def partialSet(writable: AnyRef, value: T): Boolean
}

/**
 * {@PartialFieldAccess} support for getting a value as an Option if <code>readable</code> is of type R.
 * @param T the value type
 * @param R the Readable type to get the value out of
 */
abstract class FieldGetter[R,T](implicit readableManifest: ClassManifest[R]) extends PartialFieldAccess[T] with Logging {
  /** An abstract method that must be implemented by subtypes. */
  def get(readable: R): Option[T]

  final def partialGet(readable: AnyRef) = {
    debug("Comparing " + readableManifest.erasure + " with param " + readable)
    if (readableManifest.erasure.isInstance(readable))
      get(readable.asInstanceOf[R])
    else
      None
  }
}

/**
 * {@PartialFieldAccess} support for setting a value if <code>writable</code> is of type W.
 * This is a trait so that it can be mixed with FieldGetter.
 * @param W the Writable type to put the value into
 */
trait FieldSetter[W,T] extends PartialFieldAccess[T] {
  protected def writableManifest: ClassManifest[W]

  /** An abstract method that must be implemented by subtypes. */
  def set(writable: W, value: T)

  final override def partialSet(writable: AnyRef, value: T) = {
    if (writableManifest.erasure.isInstance(writable)) {
      set(writable.asInstanceOf[W], value)
      true
    } else false
  }
}

/**
 * {@PartialFieldAccess} support for getting and setting a value if <code>readable</code> and <code>writable</code>
 * are of the types R and W respectively.
 * @param T the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
abstract class FieldAccess[R,W,T](implicit readableManifest: ClassManifest[R], _writableManifest: ClassManifest[W])
        extends FieldGetter[R,T] with FieldSetter[W,T] {
  protected def writableManifest = _writableManifest
}

/**
 * Factory methods for basic FieldAccesses.  This should be imported as Field._.
 */
object Field {
  //This is here so that getters can be written more simply by not having to explicitly wrap the result in a "Some".
  implicit def toSome[T](value: T): Option[T] = Some(value)

  /** Defines read-only fieldAccess for a field value for a Readable type. */
  def readOnly[R,T](getter: R => Option[T])
                   (implicit typeManifest: ClassManifest[R]): FieldGetter[R,T] = {
    new FieldGetter[R,T] {
      def get(readable: R) = getter(readable)

      def partialSet(writable: AnyRef, value: T) = false
    }
  }

  /** Defines write-only fieldAccess for a field value for a Writable type. */
  def writeOnly[W,T](setter: W => T => Unit)
                    (implicit typeManifest: ClassManifest[W]): FieldSetter[W,T] = {
    new FieldSetter[W,T] {
      protected def writableManifest = typeManifest

      def set(writable: W, value: T) = setter(writable)(value)

      def partialGet(readable: AnyRef) = None
    }
  }


  /** Defines a default for a field value, used when copied from {@link Unit}. */
  def default[T](value: => T): PartialFieldAccess[T] = new PartialFieldAccess[T] {
    def partialGet(readable: AnyRef) = Some(value)

    def partialSet(writable: AnyRef, value: T) = false
  }

  /**
   * Defines a flow for a field value from a Readable type to a Writable type.
   * The value never actually is taken directly from the Readable and set in the Writable.
   * It is copied to and from other objects.
   * @param R the Readable type to get the value out of
   * @param W the Writable type to put the value into
   * @param T the value type
   */
  def flow[R,W,T](getter: R => Option[T], setter: W => T => Unit)
                 (implicit readableManifest: ClassManifest[R], writableManifest: ClassManifest[W]): FieldAccess[R,W,T] = {
    new FieldAccess[R,W,T] {
      def get(readable: R) = getter(readable)

      def set(writable: W, value: T) = setter(writable)(value)
    }
  }

  /**
   *  Defines fieldAccess for a field value using a setter and getter.
   * @param M any mutable type
   * @param T the value type
   */
  def fieldAccess[M,T](getter: M => Option[T], setter: M => T => Unit)
                 (implicit typeManifest: ClassManifest[M]): FieldAccess[M,M,T] = flow[M,M,T](getter, setter)

  /**
   * Allow creating a Field without using "new".
   */
  def apply[T](fieldAccesses: PartialFieldAccess[T]*): Field[T] = new Field[T](fieldAccesses :_*)
}

