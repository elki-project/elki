package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.IDPair;

import java.util.List;
import java.util.Map;

/**
 * Provides a list of database objects and labels associated with these objects and a
 * mapping of id pairs to precomputed distances.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DistanceParsingResult<O extends DatabaseObject, D extends Distance> extends ParsingResult<O> {
  /**
   * The map of precomputed distances between the database objects.
   */
  private final Map<IDPair, D> distanceMap;

  /**
   * Provides a list of database objects, a list of label obejcts associated with these objects
   * and distances between these objects.
   *
   * @param objectAndLabelList the list of database objects and labels associated with these objects
   * @param distanceMap        the map of precomputed distances between the database objects
   */
  public DistanceParsingResult(List<ObjectAndLabels<O>> objectAndLabelList,
                               Map<IDPair, D> distanceMap) {
    super(objectAndLabelList);
    this.distanceMap = distanceMap;
  }

  /**
   * Returns the map of precomputed distances between the database objects.
   *
   * @return map of precomputed distances between the database objects
   */
  public Map<IDPair, D> getDistanceMap() {
    return distanceMap;
  }
}
