package de.lmu.ifi.dbs.elki.index.tree.query;


/**
 * Candidate for expansion in a distance search (generic implementation).
 * 
 * @author Erich Schubert
 */
public class DoubleDistanceSearchCandidate implements Comparable<DoubleDistanceSearchCandidate> {
  /**
   * Distance value
   */
  public double mindist;

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
  public DoubleDistanceSearchCandidate(final double mindist, final Integer pagenr) {
    super();
    this.mindist = mindist;
    this.nodeID = pagenr;
  }

  @Override
  public boolean equals(Object obj) {
    final DoubleDistanceSearchCandidate other = (DoubleDistanceSearchCandidate) obj;
    return this.nodeID == other.nodeID;
  }

  @Override
  public int compareTo(DoubleDistanceSearchCandidate o) {
    return Double.compare(this.mindist, o.mindist);
  }
}