package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;

/**
 * A DistanceParser shall provide a DistanceParsingResult by parsing an InputStream.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface DistanceParser<M extends MetricalObject, D extends Distance> extends Parser<M> {

  /**
   * Returns the distance function of this parser.
   *
   * @return the distance function of this parser
   */
  DistanceFunction<M, D> getDistanceFunction();
}
