package de.lmu.ifi.dbs.elki.parser;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * A parser that can parse single line.
 * Will be used by a parser to re-read results.
 *
 * @author Erich Schubert
 * @param <O> object type
 */
public interface LinebasedParser<O extends DatabaseObject> {
  /**
   * Parse a single line into a database object
   * 
   * @param line single line
   * @return parsing result
   */
  public Pair<O, List<String>> parseLine(String line);
}
