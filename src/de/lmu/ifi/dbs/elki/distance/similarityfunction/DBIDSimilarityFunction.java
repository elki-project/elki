package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface DBIDSimilarityFunction describes the requirements of any similarity
 * function defined over object IDs.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * 
 * @param <D> distance type
 */
public interface DBIDSimilarityFunction<D extends Distance<D>> extends SimilarityFunction<DatabaseObject, D> {
  /**
   * Computes the similarity between two given DatabaseObjects according to this
   * similarity function.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the similarity between two given DatabaseObjects according to this
   *         similarity function
   */
  D similarity(DBID id1, DBID id2);
}