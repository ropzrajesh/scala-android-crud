package com.github.scala.android.crud.common

/**
 * A single place to specify types that could vary between different platforms.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 3/31/11
 * Time: 10:13 PM
 */

object PlatformTypes {
  /** An entity ID */
  type ID = Long
  /** A string key used with translation. */
  type SKey = Int
  /** An image key used with translation. */
  type ImgKey = Int
  /** A layout key. */
  type LayoutKey = Int
  /** A view key, which is a single element of a layout. */
  type ViewKey = Int
}