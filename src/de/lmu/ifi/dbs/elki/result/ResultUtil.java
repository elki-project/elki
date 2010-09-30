package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Utilities for handling result objects
 * 
 * @author Erich Schubert
 */
public class ResultUtil {
  /**
   * (Try to) find an association of the given ID in the result.
   * 
   * @param <T> Association result type
   * @param result Result to find associations in
   * @param assoc Association
   * @return First matching annotation result or null
   */
  public static final <T> AnnotationResult<T> findAnnotationResult(AnyResult result, AssociationID<T> assoc) {
    List<AnnotationResult<?>> anns = getAnnotationResults(result);
    return findAnnotationResult(anns, assoc);
  }

  /**
   * (Try to) find an association of the given ID in the result.
   * 
   * @param <T> Association result type
   * @param anns List of Results
   * @param assoc Association
   * @return First matching annotation result or null
   */
  @SuppressWarnings("unchecked")
  public static final <T> AnnotationResult<T> findAnnotationResult(List<AnnotationResult<?>> anns, AssociationID<T> assoc) {
    if(anns == null) {
      return null;
    }
    for(AnnotationResult<?> a : anns) {
      if(a.getAssociationID() == assoc) { // == okay to use: association IDs are
        // unique objects
        return (AnnotationResult<T>) a;
      }
    }
    return null;
  }

  /**
   * Collect all Annotation results from a Result
   * 
   * @param r Result
   * @return List of all annotation results
   */
  public static List<AnnotationResult<?>> getAnnotationResults(AnyResult r) {
    if(r instanceof AnnotationResult<?>) {
      List<AnnotationResult<?>> anns = new ArrayList<AnnotationResult<?>>(1);
      anns.add((AnnotationResult<?>) r);
      return anns;
    }
    if(r instanceof Result) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, filterResults((Result) r, AnnotationResult.class));
    }
    return null;
  }

  /**
   * Collect all ordering results from a Result
   * 
   * @param r Result
   * @return List of ordering results
   */
  public static List<OrderingResult> getOrderingResults(AnyResult r) {
    if(r instanceof OrderingResult) {
      List<OrderingResult> ors = new ArrayList<OrderingResult>(1);
      ors.add((OrderingResult) r);
      return ors;
    }
    if(r instanceof Result) {
      return filterResults((Result) r, OrderingResult.class);
    }
    return null;
  }

  /**
   * Collect all clustering results from a Result
   * 
   * @param r Result
   * @return List of clustering results
   */
  public static List<Clustering<? extends Model>> getClusteringResults(AnyResult r) {
    if(r instanceof Clustering<?>) {
      List<Clustering<?>> crs = new ArrayList<Clustering<?>>(1);
      crs.add((Clustering<?>) r);
      return crs;
    }
    if(r instanceof Result) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, filterResults((Result) r, Clustering.class));
    }
    return null;
  }

  /**
   * Collect all collection results from a Result
   * 
   * @param r Result
   * @return List of collection results
   */
  public static List<CollectionResult<?>> getCollectionResults(AnyResult r) {
    if(r instanceof CollectionResult<?>) {
      List<CollectionResult<?>> crs = new ArrayList<CollectionResult<?>>(1);
      crs.add((CollectionResult<?>) r);
      return crs;
    }
    if(r instanceof Result) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, filterResults((Result) r, CollectionResult.class));
    }
    return null;
  }

  /**
   * Return all Iterable results
   * 
   * @param r Result
   * @return List of iterable results
   */
  public static List<IterableResult<?>> getIterableResults(AnyResult r) {
    if(r instanceof IterableResult<?>) {
      List<IterableResult<?>> irs = new ArrayList<IterableResult<?>>(1);
      irs.add((IterableResult<?>) r);
      return irs;
    }
    if(r instanceof Result) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, filterResults((Result) r, IterableResult.class));
    }
    return null;
  }

  /**
   * Collect all outlier results from a Result
   * 
   * @param r Result
   * @return List of outlier results
   */
  public static List<OutlierResult> getOutlierResults(AnyResult r) {
    if(r instanceof OutlierResult) {
      List<OutlierResult> ors = new ArrayList<OutlierResult>(1);
      ors.add((OutlierResult) r);
      return ors;
    }
    if(r instanceof Result) {
      return filterResults((Result) r, OutlierResult.class);
    }
    return null;
  }

  /**
   * Collect all settings results from a Result
   * 
   * @param r Result
   * @return List of settings results
   */
  public static List<SettingsResult> getSettingsResults(AnyResult r) {
    if(r instanceof SettingsResult) {
      List<SettingsResult> ors = new ArrayList<SettingsResult>(1);
      ors.add((SettingsResult) r);
      return ors;
    }
    if(r instanceof Result) {
      return filterResults((Result) r, SettingsResult.class);
    }
    return null;
  }

  /**
   * Return only results of the given restriction class
   * 
   * @param <C> Class type
   * @param restrictionClass Class restriction
   * @return filtered results list
   */
  // We can't ensure that restrictionClass matches C.
  @SuppressWarnings("unchecked")
  public static <C> ArrayList<C> filterResults(AnyResult r, Class<?> restrictionClass) {
    ArrayList<C> res = new ArrayList<C>();
    try {
      res.add((C) restrictionClass.cast(r));
    }
    catch(ClassCastException e) {
      // ignore
    }
    if(r instanceof Result) {
      Result parent = (Result) r;
      for(AnyResult result : parent.getPrimary()) {
        if(result != null) {
          try {
            res.add((C) restrictionClass.cast(result));
          }
          catch(ClassCastException e) {
            // skip non-matching items, except if we can descend.
            if(result instanceof Result) {
              // Recurse into nested MultiResults
              res.addAll((List<C>) filterResults((Result) result, restrictionClass));
            }
          }
        }
      }
      for(AnyResult result : parent.getDerived()) {
        if(result != null) {
          try {
            res.add((C) restrictionClass.cast(result));
          }
          catch(ClassCastException e) {
            // skip non-matching items, except if we can descend.
            if(result instanceof Result) {
              // Recurse into nested MultiResults
              res.addAll((List<C>) filterResults((Result) result, restrictionClass));
            }
          }
        }
      }
    }
    return res;
  }

  /**
   * Ensure the result is a MultiResult, otherwise wrap it in one.
   * 
   * @param result Original result
   * @return MultiResult, either result itself or a MultiResult containing
   *         result.
   */
  public static Result ensureFullResult(AnyResult result) {
    if(result instanceof Result) {
      return (Result) result;
    }
    TreeResult mr = new TreeResult(result.getLongName(), result.getShortName());
    mr.addPrimaryResult(result);
    return mr;
  }

  /**
   * Get the (first) TrivialResult.
   * 
   * @return trivial result
   */
  public static TrivialResult getEnsureTrivialResult(Result result) {
    Collection<TrivialResult> alltriv = ResultUtil.filterResults(result, TrivialResult.class);
    if(alltriv.size() > 0) {
      // Return the first
      return alltriv.iterator().next();
    }
    else {
      // ... or create a new one.
      TrivialResult trivial = new TrivialResult();
      // FIXME: not really derived ...
      result.addDerivedResult(trivial);
      return trivial;
    }
  }
}