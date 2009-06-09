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
   * Set global meta association. It tries to replace an existing Association
   * first, then tries to insert in the first {@link MetadataResult} otherwise
   * creating a new {@link MetadataResult} if none was found.
   * 
   * @param <M> restriction class
   * @param result Result collection
   * @param meta Association
   * @param value Value
   */
  public static final <M> void setGlobalAssociation(MultiResult result, AssociationID<M> meta, M value) {
    ArrayList<MetadataResult> mrs = result.filterResults(MetadataResult.class);
    // first try to overwrite an existing result.
    for(MetadataResult mri : mrs) {
      M res = mri.getAssociation(meta);
      if(res != null) {
        mri.setAssociation(meta, value);
        return;
      }
    }
    // otherwise, set in first
    if(mrs.size() > 0) {
      mrs.get(0).setAssociation(meta, value);
      return;
    }
    // or create a new MetadataResult.
    MetadataResult mr = new MetadataResult();
    mr.setAssociation(meta, value);
    result.addResult(mr);
  }

  /**
   * Get first Association from a MultiResult.
   * 
   * @param <M> restriction class
   * @param result Result collection
   * @param meta Association
   * @return first match or null
   */
  public static final <M> M getGlobalAssociation(MultiResult result, AssociationID<M> meta) {
    ArrayList<MetadataResult> mrs = result.filterResults(MetadataResult.class);
    for(MetadataResult mr : mrs) {
      M res = mr.getAssociation(meta);
      if(res != null) {
        return res;
      }
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
      if(a.getAssociationID() == assoc) { // == okay to use: association IDs are unique objects
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
    if (r instanceof AnnotationResult) {
      List<AnnotationResult<?>> anns = new ArrayList<AnnotationResult<?>>(1);
      anns.add((AnnotationResult<?>) r);
      return anns;
    }
    if(r instanceof MultiResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, ((MultiResult)r).filterResults(AnnotationResult.class));
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
    if (r instanceof OrderingResult) {
      List<OrderingResult> ors = new ArrayList<OrderingResult>(1);
      ors.add((OrderingResult) r);
      return ors;
    }
    if(r instanceof MultiResult) {
      return ((MultiResult)r).filterResults(OrderingResult.class);
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
    if (r instanceof Clustering) {
      List<Clustering<?>> crs = new ArrayList<Clustering<?>>(1);
      crs.add((Clustering<?>) r);
      return crs;
    }
    if(r instanceof MultiResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, ((MultiResult)r).filterResults(Clustering.class));
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
    if (r instanceof IterableResult) {
      List<IterableResult<?>> irs = new ArrayList<IterableResult<?>>(1);
      irs.add((IterableResult<?>) r);
      return irs;
    }
    if(r instanceof MultiResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, ((MultiResult)r).filterResults(IterableResult.class));
    }
    return null;
  }
  
  /**
   * Return all Metadata results
   * 
   * @param r Result
   * @return List of metadata results
   */
  public static List<MetadataResult> getMetadataResults(Result r) {
    if (r instanceof MetadataResult) {
      List<MetadataResult> irs = new ArrayList<MetadataResult>(1);
      irs.add((MetadataResult) r);
      return irs;
    }
    if(r instanceof MultiResult) {
      return ClassGenericsUtil.castWithGenericsOrNull(List.class, ((MultiResult)r).filterResults(MetadataResult.class));
    }
    return null;
  }
}
