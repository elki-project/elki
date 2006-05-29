package de.lmu.ifi.dbs.utilities.output;

/**
 * Helper interface for writing objects to output (e.g. to a file) in a user readable
 * string representation and for later restoring the objects from this
 * string representation.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface ObjectPrinter {
  /**
   * Get the object's print data.
   *
   * @param o the object to be printed
   * @return result  a string containing the ouput
   */
  String getPrintData(Object o);

  /**
   * Restores the object which is specified by the given String.
   *
   * @param s the string that specifies the object to be restored
   * @return the restored object
   */
  Object restoreObject(String s);
}
