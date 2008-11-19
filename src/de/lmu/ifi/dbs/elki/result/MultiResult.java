package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;

/**
 * MultiResult is a result collection class.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 * @param <O>
 */
public class MultiResult implements Result {
  /**
   * Store the actual results
   */
  private ArrayList<Result> results;
  
  /**
   * Constructor
   * 
   * @param results Array to use for results.
   */
  public MultiResult(ArrayList<Result> results) {
    super();
    this.results = results;
  }

  /**
   * Constructor
   */
  public MultiResult() {
    super();
    this.results = new ArrayList<Result>();
  }

  /**
   * Retrieve result array. Accessor.
   * 
   * @return
   */
  public ArrayList<Result> getResults() {
    return results;
  }

  /**
   * Add a new result to the object
   * 
   * @param r
   */
  public void addResult(Result r) {
    this.results.add(r);
  }

  /**
   * Insert a new result at the beginning of the results list
   * 
   * @param r
   */
  public void prependResult(Result r) {
    this.results.add(0, r);
  }

  /**
   * Return only results of the given restriction class
   * 
   * @param <C>
   * @param restrictionClass
   * @return
   */
  // We can't ensure that restrictionClass matches C.
  @SuppressWarnings("unchecked")
  public <C> ArrayList<C> filterResults(Class<?> restrictionClass) {
    ArrayList<C> res = new ArrayList<C>();
    for (Result result : results)
      try {
        res.add((C) restrictionClass.cast(result));
      } catch (ClassCastException e) {
        // skip non-matching items
      }
    return res;
  }
}
