package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;

/**
 * MultiResult is a result collection class.
 * 
 * @author Erich Schubert
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
   * Retrieve result array.
   * 
   * @return results list
   */
  public ArrayList<Result> getResults() {
    return results;
  }

  /**
   * Add a new result to the object
   * 
   * @param r new result
   */
  public void addResult(Result r) {
    this.results.add(r);
  }

  /**
   * Insert a new result at the beginning of the results list
   * 
   * @param r new result
   */
  public void prependResult(Result r) {
    this.results.add(0, r);
  }

  /**
   * Return only results of the given restriction class
   * 
   * @param <C>
   * @param restrictionClass
   * @return filtered results list
   */
  // We can't ensure that restrictionClass matches C.
  public <C> ArrayList<C> filterResults(Class<C> restrictionClass) {
    ArrayList<C> res = new ArrayList<C>();
    for(Result result : results) {
      if(result != null) {
        try {
          res.add(restrictionClass.cast(result));
        }
        catch(ClassCastException e) {
          // skip non-matching items
        }
      }
    }
    return res;
  }

  /**
   * Return first results of the given restriction class
   * 
   * @param <C>
   * @param restrictionClass
   * @return first matching result
   */
  public <C> C getFirstFilteredResult(Class<C> restrictionClass) {
    for(Result result : results) {
      if(result != null) {
        try {
          return restrictionClass.cast(result);
        }
        catch(ClassCastException e) {
          // skip non-matching items
        }
      }
    }
    return null;
  }
}
