package com.github.triangle

import com.github.scala_android.crud.monitor.Logging
import collection.Map

/** A trait for {@link PortableField} for convenience such as when defining a List of heterogeneous Fields. */
trait BaseField {
  /**
   * Copies this field from <code>from</code> to <code>to</code>.
   * @returns true if successfully set a value
   */
  def copy(from: AnyRef, to: AnyRef): Boolean

  /**
   * Traverses all of the PortableFieldes in this PortableField, returning the desired information.
   * Anything not matched will be traversed deeper, if possible, or else ignored.
   * <pre>
   *   flatMap {
   *     case foo: BarField => List(foo.myInfo)
   *   }
   * </pre>
   */
  def flatMap[B](f: PartialFunction[BaseField, Traversable[B]]): Traversable[B] = {
    f.lift(this) match {
      case Some(t: Traversable[B]) => t
      case None => None
    }
  }
}

/**
 * A portable field of a specific type which applies to Cursors, Views, Model objects, etc.
 * <p>
 * Example:
 * <pre>
 * import com.github.triangle.PortableField._
 * import com.github.scala_android.crud.CursorField._
 * import com.github.scala_android.crud.PersistedType._
 * import com.github.scala_android.crud.ViewField._
 *
 * val fields = List(
 *   persisted[String]("name") + viewId(R.id.name, textView),
 *   persisted[Double]("score") + viewId(R.id.score, formatted[Double](textView))
 * )
 * </pre>
 * <p>
 * Usage of implicits and defaults make this syntax concise for the simple cases,
 * but allow for very complex situations as well by providing explicit values when needed.
 * @param T the value type that this PortableField gets and sets.
 * @see #getter
 * @see #setter
 */
trait PortableField[T] extends BaseField with Logging {
  /**
   * PartialFunction for getting an optional value from an AnyRef.
   */
  def getter: PartialFunction[AnyRef,Option[T]]

  /**
   * Overrides what to do if getter isn't applicable to a readable.
   * The default is to throw a MatchError.
   */
  def getOrReturn(readable: AnyRef, default: => Option[T]): Option[T] = getter.lift(readable).getOrElse {
    val defaultValue: Option[T] = default
    debug("Unable to find value in " + readable + " for field " + this + ", so returning default: " + defaultValue)
    defaultValue
  }

  /**
   * Gets the value, similar to {@link Map#apply}, and the value must not be None.
   * @see #getter
   * @returns the value
   * @throws NoSuchElementException if the value was None
   * @throws MatchError if readable is not an applicable type
   */
  def apply(readable:AnyRef): T = getter(readable).get

  /**
   * PartialFunction for setting an optional value in an AnyRef.
   */
  def setter: PartialFunction[AnyRef,Option[T] => Unit]

  /**
   * Sets a value in <code>to</code> by using all embedded PortableFields that can handle it.
   * @return true if any were successful
   */
  def setValue(to: AnyRef, value: Option[T]): Boolean = {
    val defined = setter.isDefinedAt(to)
    if (defined) setter(to)(value)
    if (!defined) debug("Unable to set value of field " + this + " into " + to + " to " + value + ".")
    defined
  }

  //inherited
  def copy(from: AnyRef, to: AnyRef): Boolean = {
    getter.lift(from) match {
      case Some(optionalValue) =>
        debug("Copying " + optionalValue + " from " + from + " to " + to + " for field " + this)
        setValue(to, optionalValue)
      case None =>
        false
    }
  }

  /**
   * Adds two PortableField objects together.
   */
  def +(other: PortableField[T]): PortableField[T] = {
    val self = this
    new PortableField[T] {
      lazy val getter = self.getter.orElse(other.getter)

      /**
       * Combines the two setters, calling only applicable ones (not just the first though).
       */
      lazy val setter = new PartialFunction[AnyRef,Option[T] => Unit] {
        def isDefinedAt(x: AnyRef) = self.setter.isDefinedAt(x) || other.setter.isDefinedAt(x)

        def apply(writable: AnyRef) = { value =>
          val definedFields = List(self, other).filter(_.setter.isDefinedAt(writable))
          if (definedFields.isEmpty) {
            throw new MatchError("setter in " + PortableField.this)
          } else {
            definedFields.foreach(_.setter(writable)(value))
          }
        }
      }

      override def flatMap[B](f: PartialFunction[BaseField, Traversable[B]]) = {
        val lifted = f.lift
        List(self, other).flatMap(field => lifted(field) match {
          case Some(t: Traversable[B]) => t
          case None => field.flatMap(f)
        })
      }
    }
  }
}

trait DelegatingPortableField[T] extends PortableField[T] {
  protected def delegate: PortableField[T]

  def getter = delegate.getter

  def setter = delegate.setter

  override def flatMap[B](f: PartialFunction[BaseField, Traversable[B]]) = {
    f.lift(this).getOrElse(delegate.flatMap(f))
  }
}

/**
 * {@PortableField} support for getting a value as an Option if <code>readable</code> is of type R.
 * @param T the value type
 * @param R the Readable type to get the value out of
 */
abstract class FieldGetter[R,T](implicit readableManifest: ClassManifest[R]) extends PortableField[T] with Logging {
  /** An abstract method that must be implemented by subtypes. */
  def get(readable: R): Option[T]

  def getter = { case readable: R if readableManifest.erasure.isInstance(readable) => get(readable) }
}

trait NoSetter[T] extends PortableField[T] {
  def setter = PortableField.emptyPartialFunction
}

/**
 * {@PortableField} support for setting a value if <code>writable</code> is of type W.
 * This is a trait so that it can be mixed with FieldGetter.
 * @param W the Writable type to put the value into
 */
trait FieldSetter[W,T] extends PortableField[T] with Logging {
  protected def writableManifest: ClassManifest[W]

  /** An abstract method that must be implemented by subtypes. */
  def set(writable: W, value: Option[T])

  def setter = {
    case writable: W if writableManifest.erasure.isInstance(writable) => set(writable, _)
  }
}

/**
 * {@PortableField} support for getting and setting a value if <code>readable</code> and <code>writable</code>
 * are of the types R and W respectively.
 * @param T the value type
 * @param R the Readable type to get the value out of
 * @param W the Writable type to put the value into
 */
abstract class FlowField[R,W,T](implicit readableManifest: ClassManifest[R], _writableManifest: ClassManifest[W])
        extends FieldGetter[R,T] with FieldSetter[W,T] {
  protected def writableManifest = _writableManifest
}

/**
 * Factory methods for basic PortableFields.  This should be imported as PortableField._.
 */
object PortableField {
  def emptyPartialFunction[A,B] = new PartialFunction[A,B] {
    def isDefinedAt(x: A) = false

    def apply(v1: A) = throw new MatchError("emptyPartialFunction")
  }

  //This is here so that getters can be written more simply by not having to explicitly wrap the result in a "Some".
  implicit def toSome[T](value: T): Option[T] = Some(value)

  /** Defines read-only field for a Readable type. */
  def readOnly[R,T](getter1: R => Option[T])
                   (implicit typeManifest: ClassManifest[R]): FieldGetter[R,T] = {
    new FieldGetter[R,T] with NoSetter[T] {
      def get(readable: R) = getter1(readable)
    }
  }

  /** Defines write-only field for a Writable type. */
  def writeOnly[W,T](setter1: W => T => Unit, clearer: W => Unit = {_: W => })
                    (implicit typeManifest: ClassManifest[W]): FieldSetter[W,T] = {
    new FieldSetter[W,T] {
      protected def writableManifest = typeManifest

      def set(writable: W, valueOpt: Option[T]) {
        valueOpt match {
          case Some(value) => setter1(writable)(value)
          case None => clearer(writable)
        }
      }

      def getter = emptyPartialFunction
    }
  }

  /** Defines a default for a field value, used when copied from {@link Unit}. */
  def default[T](value: => T): PortableField[T] = new PortableField[T] with NoSetter[T] {
    def getter = { case Unit => Some(value) }
  }

  /**
   * Defines a flow for a field value from a Readable type to a Writable type.
   * The value never actually is taken directly from the Readable and set in the Writable.
   * It is copied to and from other objects.
   * @param R the Readable type to get the value out of
   * @param W the Writable type to put the value into
   * @param T the value type
   */
  def flow[R,W,T](getter1: R => Option[T], setter1: W => T => Unit, clearer: W => Unit = {_: W => })
                 (implicit readableManifest: ClassManifest[R], writableManifest: ClassManifest[W]): FlowField[R,W,T] = {
    new FlowField[R,W,T] {
      def get(readable: R) = getter1(readable)

      def set(writable: W, valueOpt: Option[T]) {
        valueOpt match {
          case Some(value) => setter1(writable)(value)
          case None => clearer(writable)
        }
      }
    }
  }

  /**
   *  Defines PortableField for a field value using a setter and getter.
   * @param M any mutable type
   * @param T the value type
   */
  def field[M,T](getter: M => Option[T], setter: M => T => Unit, clearer: M => Unit = {_: M => })
                 (implicit typeManifest: ClassManifest[M]): FlowField[M,M,T] = flow[M,M,T](getter, setter, clearer)

  def mapField[T](name: String): FlowField[Map[String,_ <: T],collection.mutable.Map[String,_ >: T],T] =
    flow(_.get(name), m => v => m.put(name, v), _.remove(name))

  def formatted[T](format: ValueFormat[T], field: PortableField[String]) = new PortableField[T] {
    def getter = field.getter.andThen(value => value.flatMap(format.toValue(_)))

    def setter = field.setter.andThen(setter => setter.compose(value => value.map(format.toString _)))
  }

  /**
   * formatted replacement for primitive values.
   */
  def formatted[T <: AnyVal](field: PortableField[String])(implicit m: Manifest[T]): PortableField[T] =
    formatted(new BasicValueFormat[T](), field)
}
