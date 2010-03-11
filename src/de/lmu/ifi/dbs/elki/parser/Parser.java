package de.lmu.ifi.dbs.elki.parser;

import java.io.InputStream;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;

/**
 * A Parser shall provide a ParsingResult by parsing an InputStream.
 *
 * @author Arthur Zimek
 * @param <O> object type
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
