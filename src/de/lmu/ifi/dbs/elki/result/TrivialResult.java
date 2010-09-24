package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;

/**
 * Class to mark a result as trivial (e.g. the data received from the parser)
 * and thus not worth writing out on its own.
 * 
 * @author Erich Schubert
 * 
 */
public class TrivialResult extends MultiResult {
  /**
   * Constructor.
   */
  public TrivialResult() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param results
   */
  public TrivialResult(ArrayList<Result> results) {
    super(results);
  }
}
