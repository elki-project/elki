package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.KList;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

/**
 * Provides the result of a kNN-Join.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class KNNJoinResult<T extends MetricalObject> implements Result<T> {
  /**
   * The kNN lists for each object.
   */
  HashMap<Integer, KList<Distance, QueryResult<Distance>>> knnLists;

  /**
   * Creates a new KNNJoinResult.
   *
   * @param knnLists the kNN lists for each object
   */
  public KNNJoinResult(HashMap<Integer, KList<Distance, QueryResult<Distance>>> knnLists) {
    this.knnLists = knnLists;
  }

  /**
   * TODO: evtl. anderer output
   *
   * @see Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization,
   *      java.util.List<de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings>)
   */
  public void output(File out, Normalization<T> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
    }

    for (Integer id : knnLists.keySet()) {
      outStream.println(id + " " + knnLists.get(id));
    }
    outStream.flush();
  }

  /**
   * Returns the knn distance of the object with the specified id.
   *
   * @param id the id of the object
   * @return the knn distance of the object with the specified id
   */
  public Distance getKNNDistance(Integer id) {
    KList<Distance, QueryResult<Distance>> list = knnLists.get(id);
    return list.getMaximumKey();
  }

  /**
   * Returns the knns of the object with the specified id.
   *
   * @param id the id of the object
   * @return the knns of the object with the specified id
   */
  public KList getKNNs(Integer id) {
    return knnLists.get(id);
  }
}
