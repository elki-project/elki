package de.lmu.ifi.dbs.elki.datasource.parser;

import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtilFrequentlyScanned;

/**
 * A parser that can parse single line.
 * Will be used by a parser to re-read results.
 *
 * @author Erich Schubert
 */
public interface LinebasedParser extends InspectionUtilFrequentlyScanned {
  /**
   * Parse a single line into a database object
   * 
   * @param line single line
   * @return parsing result
   */
  public SingleObjectBundle parseLine(String line);
}