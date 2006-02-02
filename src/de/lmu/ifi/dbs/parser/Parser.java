package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

import java.io.InputStream;

/**
 * A Parser shall provide a ParsingResult by parsing an InputStream.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Parser<M extends MetricalObject> extends Parameterizable {
  /**
   * Returns a list of the objects parsed from the specified input stream
   * and a list of the labels associated with the objects.
   *
   * @param in the stream to parse objects from
   * @return a list containing those objects parsed
   *         from the input stream and their associated labels.
   */
  ParsingResult<M> parse(InputStream in);
}
