package de.lmu.ifi.dbs.elki.data.type;

/**
 * Class wrapping a particular data type.
 * 
 * @author Erich Schubert
 */
public interface TypeInformation {
  /**
   * Test whether this type is assignable from another type.
   * 
   * @param type Other type
   * @return true when the other type is accepted as subtype.
   */
  public boolean isAssignableFromType(TypeInformation type);

  /**
   * Test whether this type is assignable from a given object instance.
   * 
   * @param other Other object
   * @return true when the other type is an acceptable instance.
   */
  public boolean isAssignableFrom(Object other);
}