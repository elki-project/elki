package de.lmu.ifi.dbs.elki.parser;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;

/**
 * A DistanceParser shall provide a DistanceParsingResult by parsing an InputStream.
 *
 * @author Arthur Zimek
 */
public interface DistanceParser<O extends DatabaseObject, D extends Distance<D>> extends Parser<O> {

  /**
   * Returns the distance function of this parser.
   *
   * @return the distance function of this parser
   */
  DistanceFunction<O, D> getDistanceFunction();


}
