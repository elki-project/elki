package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.ExternalObject;
import de.lmu.ifi.dbs.database.AssociationID;

import java.util.Map;

/**
 * Provides a DistanceFunction that is based on float distances given by a
 * distance matrix of an external file.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FileBasedFloatDistanceFunction extends
                                            FloatDistanceFunction<ExternalObject> {

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function.
   *
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public FloatDistance distance(ExternalObject o1, ExternalObject o2) {
    return distance(o1.getID(), o2.getID());
  }

  /**
   * Returns the distance between the two objcts specified by their obejct
   * ids. If a cache is used, the distance value is looked up in the cache. If
   * the distance does not yet exists in cache, it will be computed an put to
   * cache. If no cache is used, the distance is computed.
   *
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objcts specified by their obejct ids
   */
  public FloatDistance distance(Integer id1, Integer id2) {
    // the smaller id is the first key
    if (id1 > id2) {
      distance(id2, id1);
    }

    Map<Integer, FloatDistance> distances = (Map<Integer, FloatDistance>) getDatabase().getAssociation(AssociationID.CACHED_DISTANCES, id1);
    return distances.get(id2);
  }

  /**
   * Returns a description of the class and the required parameters. This
   * description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    return "File based double distance for database objects. No parameters required. "
           + "Pattern for defining a range: \""
           + requiredInputPattern()
           + "\".";

  }
}
