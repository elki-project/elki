package experimentalcode.erich.utilities.tree.rtree;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialAdapter;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.SplitStrategy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import experimentalcode.erich.utilities.mbr.DoubleRecMBRAdapter;
import experimentalcode.erich.utilities.mbr.MBRUtil;

@Reference(authors = "Antonin Guttman", title = "R-Trees: A Dynamic Index Structure For Spatial Searching", booktitle = "Proceedings of the 1984 ACM SIGMOD international conference on Management of data", url = "http://dx.doi.org/10.1145/971697.602266")
public class RTreeQuadraticSplit implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final RTreeQuadraticSplit STATIC = new RTreeQuadraticSplit();

  @Override
  public <E, A> BitSet split(A entries, ArrayAdapter<E, A> getter, SpatialAdapter<? super E> adapter, int minEntries) {
    final int num = getter.size(entries);
    // Object assignment, and processed objects
    BitSet assignment = new BitSet(num);
    BitSet assigned = new BitSet(num);
    // MBRs and Areas of current assignments
    double[] mbr1, mbr2;
    double area1 = 0, area2 = 0;
    // PickSeeds - find worst pair
    {
      double worst = Double.NEGATIVE_INFINITY;
      int w1 = 0, w2 = 0;

      // Compute individual areas
      double[] areas = new double[num];
      for(int e1 = 0; e1 < num - 1; e1++) {
        final E e1i = getter.get(entries, e1);
        areas[e1] = adapter.getArea(e1i);
      }
      // Compute area increase
      for(int e1 = 0; e1 < num - 1; e1++) {
        final E e1i = getter.get(entries, e1);
        for(int e2 = e1 + 1; e2 < num; e2++) {
          final E e2i = getter.get(entries, e2);
          final double areaJ = MBRUtil.areaUnion(e1i, adapter, e2i, adapter);
          final double d = areaJ - areas[e1] - areas[e2];
          if(d > worst) {
            worst = d;
            w1 = e1;
            w2 = e2;
          }
        }
      }
      // Data to keep
      // Mark both as used
      assigned.set(w1);
      assigned.set(w2);
      // Assign second to second set
      assignment.set(w2);
      // Initial mbrs and areas
      area1 = areas[w1];
      area2 = areas[w2];
      mbr1 = DoubleRecMBRAdapter.cloneFrom(getter.get(entries, w1), adapter);
      mbr2 = DoubleRecMBRAdapter.cloneFrom(getter.get(entries, w2), adapter);
    }
    // Second phase, QS2+QS3
    {
      int in1 = 1, in2 = 1;
      int remaining = num - 2;
      while(remaining > 0) {
        // Shortcut when minEntries must be fulfilled
        if(in1 + remaining <= minEntries) {
          // No need to updated assigned, no changes to assignment.
          break;
        }
        if(in2 + remaining <= minEntries) {
          // Mark unassigned for second.
          // Don't bother to update assigned, though
          for(int pos = assigned.nextClearBit(0); pos < num; pos = assigned.nextClearBit(pos + 1)) {
            assignment.set(pos);
          }
          break;
        }
        // PickNext
        double greatestPreference = Double.NEGATIVE_INFINITY;
        int best = -1;
        boolean preferSecond = false;
        for(int pos = assigned.nextClearBit(0); pos < num; pos = assigned.nextClearBit(pos + 1)) {
          // Cost of putting object into both mbrs
          final double d1 = MBRUtil.areaUnion(mbr1, DoubleRecMBRAdapter.STATIC, getter.get(entries, pos), adapter) - area1;
          final double d2 = MBRUtil.areaUnion(mbr2, DoubleRecMBRAdapter.STATIC, getter.get(entries, pos), adapter) - area2;
          // Preference
          final double preference = Math.abs(d1 - d2);
          if(preference > greatestPreference) {
            greatestPreference = preference;
            best = pos;
            // Prefer smaller increase
            preferSecond = (d2 < d1);
          }
        }
        // QS3: tie handling
        if(greatestPreference == 0) {
          // Prefer smaller area
          if(area1 != area2) {
            preferSecond = (area2 < area1);
          }
          else {
            // Prefer smaller group size
            preferSecond = (in2 < in1);
          }
        }
        // Mark as used.
        assigned.set(best);
        remaining--;
        // Assign
        if(!preferSecond) {
          in1++;
          DoubleRecMBRAdapter.extendInplace(mbr1, getter.get(entries, best), adapter);
          area1 = DoubleRecMBRAdapter.STATIC.getArea(mbr1);
        }
        else {
          in2++;
          assignment.set(best);
          DoubleRecMBRAdapter.extendInplace(mbr2, getter.get(entries, best), adapter);
          area2 = DoubleRecMBRAdapter.STATIC.getArea(mbr2);
        }
        // Loop from QS2
      }
      // Note: "assigned" and "remaining" likely not updated!
    }
    return assignment;
  }
}
