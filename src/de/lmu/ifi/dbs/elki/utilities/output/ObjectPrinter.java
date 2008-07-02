package de.lmu.ifi.dbs.elki.utilities.output;

/**
 * Helper interface for writing objects to output (e.g. to a file) in a user readable
 * string representation and for later restoring the objects from this
 * string representation.
 *
 * @author Elke Achtert 
 */
public interface ObjectPrinter<O extends Object> {
  /**
   * Get the object's print data.
   *
   * @param o the object to be printed
   * @return result  a string containing the ouput
   */
  String getPrintData(O o);

  /**
   * Restores the object which is specified by the given String.
   *
   * @param s the string that specifies the object to be restored
   * @return the restored object
   */
  O restoreObject(String s);
}
