package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;

/**
 * A DistanceParser shall provide a DistanceParsingResult by parsing an InputStream.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface DistanceParser<O extends DatabaseObject, D extends Distance> extends Parser<O> {

  /**
   * Returns the distance function of this parser.
   *
   * @return the distance function of this parser
   */
  DistanceFunction<O, D> getDistanceFunction();


}
