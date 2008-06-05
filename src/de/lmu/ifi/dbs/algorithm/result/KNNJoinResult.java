package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
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
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 * @param <O> the type of DatabaseObjects handled by this Result
 * @param <D> the type of Distance used by this Result 
 */
public class KNNJoinResult<O extends DatabaseObject, D extends Distance<D>> implements Result<O> {
  /**
   * The kNN lists for each object.
   */
  private HashMap<Integer, KNNList<D>> knnLists;

  /**
   * Creates a new KNNJoinResult.
   *
   * @param knnLists the kNN lists for each object
   */
  public KNNJoinResult(HashMap<Integer, KNNList<D>> knnLists) {
    this.knnLists = knnLists;
  }

  /**
   * @see Result#output(File, Normalization, List)
   */
  public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
    }

    output(outStream, normalization, settings);
  }


  public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
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
  public D getKNNDistance(Integer id) {
    KNNList<D> list = knnLists.get(id);
    return list.getKNNDistance();
  }

  /**
   * Returns the knns of the object with the specified id.
   *
   * @param id the id of the object
   * @return the knns of the object with the specified id
   */
  public KNNList<D> getKNNs(Integer id) {
    return knnLists.get(id);
  }
}
