package de.lmu.ifi.dbs.elki.parser;

import java.io.InputStream;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * A DistanceParser shall provide a DistanceParsingResult by parsing an InputStream.
 *
 * @author Arthur Zimek
 * 
 * @apiviz.landmark
 * @apiviz.has de.lmu.ifi.dbs.elki.parser.DistanceParsingResult oneway - - produces
 * 
 * @param <O> object type
 * @param <D> distance type
 */
public interface DistanceParser<O extends DatabaseObject, D extends Distance<D>> extends Parser<O> {
  /**
   * Returns a list of the objects parsed from the specified input stream
   * and a list of the labels associated with the objects.
   *
   * @param in the stream to parse objects from
   * @return a list containing those objects parsed
   *         from the input stream and their associated labels.
   */
  @Override
  DistanceParsingResult<O, D> parse(InputStream in);
}
