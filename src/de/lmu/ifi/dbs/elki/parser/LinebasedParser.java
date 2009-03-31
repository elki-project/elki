package de.lmu.ifi.dbs.elki.parser;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * A parser that can parse single line.
 * Will be used by a parser to re-read results.
 *
 * @author Erich Schubert
 */
public interface LinebasedParser<O extends DatabaseObject> extends Parameterizable {
  /**
   * Parse a single line into a database object
   * 
   * @param line single line
   * @return parsing result
   */
  public Pair<O, List<String>> parseLine(String line);
}
