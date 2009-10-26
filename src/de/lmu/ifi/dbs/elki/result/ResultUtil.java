package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Utilities for handling result objects
 * 
 * @author Erich Schubert
 * 
 */
public class ResultUtil {
  /**
   * Set global meta association.
   * 
   * @param <M> restriction class
   * @param result Result collection
   * @param meta Association
   * @param value Value
   */
  @Deprecated
  public static final <M> void setGlobalAssociation(MultiResult result, AssociationID<M> meta, M value) {
    result.setAssociation(meta, value);
  }

  /**
   * Get first Association from a MultiResult.
   * 
   * @param <M> restriction class
   * @param result Result collection
   * @param meta Association
   * @return first match or null
   */
  public static final <M> M getGlobalAssociation(Result result, AssociationID<M> meta) {
    if (result instanceof MultiResult) {
      MultiResult r = (MultiResult) result;
      return r.getAssociation(meta);
    }
    return null;
  }

  /**
   * (Try to) find an association of the given ID in the result.
   * 
   * @param <T> Association result type
   * @param result Result to find associations in
   * @param assoc Association
   * @return First matching annotation result or null
   */
  public static final <T> AnnotationResult<T> findAnnotationResult(Result result, AssociationID<T> assoc) {
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
  public static List<AnnotationResult<?>> getAnnotationResults(Result r) {
    if(r instanceof AnnotationResult<?>) {
      List<AnnotationResult<?>> anns = new ArrayList<AnnotationResult<?>>(1);
      anns.add((AnnotationResult<?>) r);
      return anns;
    }
    if(r instanceof MultiResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, ((MultiResult) r).filterResults(AnnotationResult.class));
    }
    return null;
  }

  /**
   * Collect all ordering results from a Result
   * 
   * @param r Result
   * @return List of ordering results
   */
  public static List<OrderingResult> getOrderingResults(Result r) {
    if(r instanceof OrderingResult) {
      List<OrderingResult> ors = new ArrayList<OrderingResult>(1);
      ors.add((OrderingResult) r);
      return ors;
    }
    if(r instanceof MultiResult) {
      return ((MultiResult) r).filterResults(OrderingResult.class);
    }
    return null;
  }

  /**
   * Collect all clustering results from a Result
   * 
   * @param r Result
   * @return List of clustering results
   */
  public static List<Clustering<?>> getClusteringResults(Result r) {
    if(r instanceof Clustering<?>) {
      List<Clustering<?>> crs = new ArrayList<Clustering<?>>(1);
      crs.add((Clustering<?>) r);
      return crs;
    }
    if(r instanceof MultiResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, ((MultiResult) r).filterResults(Clustering.class));
    }
    return null;
  }

  /**
   * Collect all collection results from a Result
   * 
   * @param r Result
   * @return List of collection results
   */
  public static List<CollectionResult<?>> getCollectionResults(Result r) {
    if(r instanceof CollectionResult<?>) {
      List<CollectionResult<?>> crs = new ArrayList<CollectionResult<?>>(1);
      crs.add((CollectionResult<?>) r);
      return crs;
    }
    if(r instanceof MultiResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, ((MultiResult) r).filterResults(CollectionResult.class));
    }
    return null;
  }

  /**
   * Return all Iterable results
   * 
   * @param r Result
   * @return List of iterable results
   */
  public static List<IterableResult<?>> getIterableResults(Result r) {
    if(r instanceof IterableResult<?>) {
      List<IterableResult<?>> irs = new ArrayList<IterableResult<?>>(1);
      irs.add((IterableResult<?>) r);
      return irs;
    }
    if(r instanceof MultiResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, ((MultiResult) r).filterResults(IterableResult.class));
    }
    return null;
  }

  /**
   * Filter results
   * 
   * @param r Result
   * @param restrictionClass Restriction
   * @return List of filtered results
   */
  @SuppressWarnings("unchecked")
  public static <C> List<C> filterResults(Result r, Class<?> restrictionClass) {
    if(restrictionClass.isInstance(r)) {
      List<C> irs = new ArrayList<C>(1);
      irs.add((C) r);
      return irs;
    }
    if(r instanceof MultiResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, ((MultiResult) r).filterResults(restrictionClass));
    }
    return null;
  }
}
