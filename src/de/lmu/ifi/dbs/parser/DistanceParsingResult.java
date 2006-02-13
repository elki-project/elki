package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.IDPair;

import java.util.List;
import java.util.Map;

/**
 * Proviedes a list of metrical objects and a list of labels associated with these objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DistanceParsingResult<O extends DatabaseObject, D extends Distance> extends ParsingResult<O> {
  /**
   * The map of precomputed distances between the metrical objects.
   */
  private final Map<IDPair, D> distanceMap;

  /**
   * Proviedes a list of metrical objects, a list of label obejcts associated with these objects
   * and distances between these objects.
   *
   * @param objects     the list of metrical objects
   * @param labels      the list of label objects associated with the metrical objects
   * @param distanceMap the map of precomputed distances between the metrical objects
   */
  public DistanceParsingResult(List<O> objects, List<String> labels,
                               Map<IDPair, D> distanceMap) {
    super(objects, labels);
    this.distanceMap = distanceMap;
  }

  /**
   * Returns the map of precomputed distances between the metrical objects.
   *
   * @return map of precomputed distances between the metrical objects
   */
  public Map<IDPair, D> getDistanceMap() {
    return distanceMap;
  }
}
