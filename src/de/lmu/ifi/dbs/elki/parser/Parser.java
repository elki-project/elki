package de.lmu.ifi.dbs.elki.parser;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

import java.io.InputStream;

/**
 * A Parser shall provide a ParsingResult by parsing an InputStream.
 *
 * @author Arthur Zimek
 */
public interface Parser<O extends DatabaseObject> extends Parameterizable {
  /**
   * Returns a list of the objects parsed from the specified input stream
   * and a list of the labels associated with the objects.
   *
   * @param in the stream to parse objects from
   * @return a list containing those objects parsed
   *         from the input stream and their associated labels.
   */
  ParsingResult<O> parse(InputStream in);
}
