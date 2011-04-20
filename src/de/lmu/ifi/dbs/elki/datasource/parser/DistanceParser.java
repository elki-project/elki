package de.lmu.ifi.dbs.elki.datasource.parser;

import java.io.InputStream;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * A DistanceParser shall provide a DistanceParsingResult by parsing an InputStream.
 *
 * @author Arthur Zimek
 * 
 * @apiviz.uses DistanceParsingResult oneway - - «create»
 * 
 * @param <D> distance type
 */
public interface DistanceParser<D extends Distance<D>> {
  /**
   * Returns a list of the objects parsed from the specified input stream
   * and a list of the labels associated with the objects.
   *
   * @param in the stream to parse objects from
   * @return a list containing those objects parsed
   *         from the input stream and their associated labels.
   */
  DistanceParsingResult<D> parse(InputStream in);
}
