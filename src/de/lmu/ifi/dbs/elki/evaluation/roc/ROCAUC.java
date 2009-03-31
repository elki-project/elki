package de.lmu.ifi.dbs.elki.evaluation.roc;

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

/**
 * Compute the ROC AUC (Receiver Operating Characteristics  Area Under Curve)
 * 
 * @author Erich Schubert
 *
 */
public class ROCAUC {
  /**
   * Compute a ROC curves Area-under-curve for a QueryResult and a Cluster.
   * 
   * @param size Database size
   * @param clus Cluster object
   * @param nei Query result
   * @return area under curve
   */
  public static double computeROCAUC(int size, Cluster<?> clus, List<DistanceResultPair<DoubleDistance>> nei) {
    // TODO: ensure the collection has efficient "contains".    
    return computeROCAUC(size, clus.getIDs(), nei);
  }

  /**
   * Compute a ROC curves Area-under-curve for a QueryResult and a Cluster.
   * 
   * @param size Database size
   * @param ids Collection of positive IDs, should support efficient contains()
   * @param nei Query Result
   * @return area under curve
   */
  public static double computeROCAUC(int size, Collection<Integer> ids, List<DistanceResultPair<DoubleDistance>> nei) {
    int postot = ids.size();
    int negtot = size - postot;
    int poscnt = 0;
    int negcnt = 0;
    double lastpos = 0.0;
    double lastneg = 0.0;
    double result = 0.0;
  
    DistanceResultPair<DoubleDistance> prev = null;
    for(DistanceResultPair<DoubleDistance> cur : nei) {
      // positive or negative match?
      if(ids.contains(cur.getID())) {
        poscnt += 1;
      }
      else {
        negcnt += 1;
      }
      // defer calculation if this points distance equals the previous points
      // distance
      if((prev == null) || (prev.getDistance().compareTo(cur.getDistance()) != 0)) {
        double curpos = ((double) poscnt) / postot;
        double curneg = ((double) negcnt) / negtot;
        // width * height at half way.
        result += (curneg - lastneg) * (curpos + lastpos) / 2;
        lastpos = curpos;
        lastneg = curneg;
      }
      prev = cur;
    }
    // compute any remaining curve area.
    // note: if lastneg == curneg, this is just a += 0
    {
      double curpos = ((double) poscnt) / postot;
      double curneg = ((double) negcnt) / negtot;
      result += (curneg - lastneg) * (curpos + lastpos) / 2;
    }
  
    return result;
  }

}
