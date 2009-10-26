package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.utilities.AnyMap;

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
   * Meta data.
   */
  private AnyMap<AssociationID<?>> meta;  
  
  /**
   * Constructor
   * 
   * @param results Array to use for results.
   */
  public MultiResult(ArrayList<Result> results) {
    super();
    this.results = results;
    this.meta = new AnyMap<AssociationID<?>>();
  }

  /**
   * Constructor
   */
  public MultiResult() {
    super();
    this.results = new ArrayList<Result>();
    this.meta = new AnyMap<AssociationID<?>>();
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

  /**
   * Put meta data.
   * 
   * @param <M> data class
   * @param meta key
   * @param value data
   */
  public <M> void setAssociation(AssociationID<M> metaid, M value) {
    meta.put(metaid, value);
  }
  
  /**
   * Get a meta data object.
   * 
   * @param <M> data class
   * @param meta Key
   * @return stored meta data or null
   */
  public <M> M getAssociation(AssociationID<M> metaid) {
    return meta.get(metaid, metaid.getType());
  }
  
  /**
   * Get a meta data object, with weaker compile time type checking.
   * 
   * Note that in this version, it is not verified that the restriction class
   * actually satisfies the return type M.
   * 
   * @param <M> data class
   * @param meta Key
   * @return stored meta data or null
   */
  @SuppressWarnings("unchecked")
  public <M> M getAssociationGenerics(AssociationID<?> metaid) {
    return (M) meta.getGenerics(metaid, metaid.getType());
  }
  
  /**
   * Get stored meta data associations.
   * 
   * @return Stored keys
   */
  public Collection<AssociationID<?>> getAssociations() {
    return meta.keySet();
  }
  
  @Override
  public String getName() {
    return "multi";
  }
}