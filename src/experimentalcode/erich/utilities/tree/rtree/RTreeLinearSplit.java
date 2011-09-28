package experimentalcode.erich.utilities.tree.rtree;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialAdapter;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.SplitStrategy;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import experimentalcode.erich.utilities.mbr.DoubleRecMBRAdapter;
import experimentalcode.erich.utilities.mbr.MBRUtil;

@Reference(authors = "Antonin Guttman", title = "R-Trees: A Dynamic Index Structure For Spatial Searching", booktitle = "Proceedings of the 1984 ACM SIGMOD international conference on Management of data", url = "http://dx.doi.org/10.1145/971697.602266")
public class RTreeLinearSplit implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final RTreeLinearSplit STATIC = new RTreeLinearSplit();

  @Override
  public <E, A> BitSet split(A entries, ArrayAdapter<E, A> getter, SpatialAdapter<? super E> adapter, int minEntries) {
    final int num = getter.size(entries);
    // Object assignment, and processed objects
    BitSet assignment = new BitSet(num);
    BitSet assigned = new BitSet(num);
    // MBRs and Areas of current assignments
    double[] mbr1, mbr2;
    double area1 = 0, area2 = 0;
    // LinearPickSeeds - find worst pair
    {
      final int dim = adapter.getDimensionality(getter.get(entries, 0));
      // Best candidates
      double bestsep = Double.NEGATIVE_INFINITY;
      int w1 = -1, w2 = -1;
      // LPS1: find extreme rectangles
      for(int d = 0; d < dim; d++) {
        // We need to find two candidates each, in case of el==eh!
        double minlow = Double.POSITIVE_INFINITY;
        double maxlow = Double.NEGATIVE_INFINITY, maxlow2 = Double.NEGATIVE_INFINITY;
        double minhig = Double.POSITIVE_INFINITY, minhig2 = Double.POSITIVE_INFINITY;
        double maxhig = Double.NEGATIVE_INFINITY;
        int el = -1, el2 = -1;
        int eh = -1, eh2 = -1;
        for(int i = 0; i < num; i++) {
          E ei = getter.get(entries, i);
          final double low = adapter.getMin(ei, d);
          final double hig = adapter.getMax(ei, d);
          minlow = Math.min(minlow, low);
          maxhig = Math.max(maxhig, hig);
          if(low >= maxlow) {
            maxlow2 = maxlow;
            maxlow = low;
            el2 = el;
            el = i;
          }
          else if(low > maxlow2) {
            maxlow2 = low;
            el2 = i;
          }
          if(hig <= minhig) {
            minhig2 = minhig;
            minhig = hig;
            eh2 = eh;
            eh = i;
          }
          else if(hig < minhig2) {
            minhig2 = hig;
            eh2 = i;
          }
        }
        // Compute normalized separation
        final double normsep;
        if(el != eh) {
          normsep = minhig - maxlow / (maxhig - minlow);
        }
        else {
          // Resolve tie.
          double normsep1 = minhig - maxlow2 / (maxhig - minlow);
          double normsep2 = minhig2 - maxlow / (maxhig - minlow);
          if(normsep1 > normsep2) {
            el = el2;
            normsep = normsep1;
          }
          else {
            eh = eh2;
            normsep = normsep2;
          }
        }
        assert (eh != -1 && el != -1 && (eh != el));
        if(normsep > bestsep) {
          bestsep = normsep;
          w1 = el;
          w2 = eh;
        }
      }

      // Data to keep
      // Mark both as used
      assigned.set(w1);
      assigned.set(w2);
      // Assign second to second set
      assignment.set(w2);
      // Initial mbrs and areas
      final E w1i = getter.get(entries, w1);
      final E w2i = getter.get(entries, w2);
      area1 = adapter.getArea(w1i);
      area2 = adapter.getArea(w2i);
      mbr1 = DoubleRecMBRAdapter.cloneFrom(w1i, adapter);
      mbr2 = DoubleRecMBRAdapter.cloneFrom(w2i, adapter);
      LoggingUtil.warning("size:" + num + " minEntries: " + minEntries + " area1: " + area1 + " area2: " + area2 + " w1: " + w1i.toString() + " w2: " + w2i.toString());
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
        }
        else {
          in2++;
          assignment.set(best);
        }
        // Loop from QS2
      }
      // Note: "assigned" and "remaining" likely not updated!
      LoggingUtil.warning("size:" + num + " minEntries: " + minEntries + " area1: " + area1 + " area2: " + area2);
    }
    return assignment;
  }
}
