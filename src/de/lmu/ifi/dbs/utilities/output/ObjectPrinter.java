package de.lmu.ifi.dbs.utilities.output;

/**
 * Helper interface for printing objects. Implementing classes are used if the output
 * of an object (e.g. to a file) differs from the object's toString() method.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface ObjectPrinter {
  /**
  *  Get the object's print data.
  *
  *  @param o        the object to be printed
  *  @return result  a string containing the ouput
  */
  String getPrintData(Object o);
}
