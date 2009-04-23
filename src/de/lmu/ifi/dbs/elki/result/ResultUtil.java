package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.AssociationID;

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
  public static final <T> AnnotationResult<T> findAnnotationResult(MultiResult result, AssociationID<T> assoc) {
    List<Result> anns = result.filterResults(AnnotationResult.class);
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
  public static final <T> AnnotationResult<T> findAnnotationResult(List<Result> anns, AssociationID<T> assoc) {
    if(anns == null) {
      return null;
    }
    for(Result r : anns) {
      if(r instanceof AnnotationResult) {
        AnnotationResult<?> a = (AnnotationResult<?>) r;
        if(a.getAssociationID() == assoc) { // == should be okay - unique objects
          return (AnnotationResult<T>) a;
        }
      }
      // recurse into MultiResults - no loop detection!
      if (r instanceof MultiResult) {
        AnnotationResult<T> rec = findAnnotationResult((MultiResult)r, assoc); 
        if (rec != null) {
          return rec;
        }
      }
    }
    return null;
  }
}
