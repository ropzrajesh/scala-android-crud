package geeks.crud

/**
 * Persistence support for an entity.
 * @author Eric Pabst (epabst@gmail.com)
 * Date: 2/2/11
 * Time: 4:12 PM
 * @param ID the ID type for the entity such as String or Long.
 * @param L the type of findAll (e.g. Cursor)
 * @param R the type to read from (e.g. Cursor)
 * @param W the type to write to (e.g. ContentValues)
 */

trait EntityPersistence[ID,L,R,W] {
  def findAll: L

  /** Find an entity by ID. */
  def find(id: ID): R

  def newWritable: W

  /** Save a created or updated entity. */
  def save(id: Option[ID], writable: W): ID

  /** Delete a list of entities by ID. */
  def delete(ids: List[ID])
}