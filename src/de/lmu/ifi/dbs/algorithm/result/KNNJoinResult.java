package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

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
  HashMap<Integer, KNNList> knnLists;

  /**
   * Creates a new KNNJoinResult.
   *
   * @param knnLists the kNN lists for each object
   */
  public KNNJoinResult(HashMap<Integer, KNNList> knnLists) {
    this.knnLists = knnLists;
  }

  /**
   * TODO ?
   *
   * @see Result#output(File, Normalization)
   */
  public void output(File out, Normalization<T> normalization) throws UnableToComplyException {
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
}
