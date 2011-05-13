package de.lmu.ifi.dbs.elki.index.tree.query;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Candidate for expansion in a distance search (generic implementation).
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class GenericDistanceSearchCandidate<D extends Distance<D>> implements Comparable<GenericDistanceSearchCandidate<D>> {
  /**
   * Distance value
   */
  public D mindist;

  /**
   * Page id
   */
  public Integer nodeID;

  /**
   * Constructor.
   * 
   * @param mindist The minimum distance to this candidate
   * @param pagenr The page number of this candidate
   */
  public GenericDistanceSearchCandidate(final D mindist, final Integer pagenr) {
    super();
    this.mindist = mindist;
    this.nodeID = pagenr;
  }

  @Override
  public boolean equals(Object obj) {
    final GenericDistanceSearchCandidate<?> other = (GenericDistanceSearchCandidate<?>) obj;
    return this.nodeID == other.nodeID;
  }

  @Override
  public int compareTo(GenericDistanceSearchCandidate<D> o) {
    return this.mindist.compareTo(o.mindist);
  }
}