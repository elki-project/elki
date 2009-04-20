package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
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
}
