package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

public class ZCurveSpatialSorter extends AbstractSpatialSorter {
  private static final double STOPVAL = 1E-10;

  @Override
  public <T extends SpatialComparable> void sort(List<T> objs) {
    double[] mm = computeMinMax(objs);
    zSort(objs, 0, objs.size(), mm, 0);
  }

  protected <T extends SpatialComparable> void zSort(List<T> objs, int start, int end, double[] mms, int dim) {
    // Find the splitting points.
    final double min = mms[2 * dim], max = mms[2 * dim + 1];
    double spos = (min + max) / 2.;
    // Safeguard against duplicate points:
    if(max - spos < STOPVAL || spos - min < STOPVAL) {
      boolean ok = false;
      for(int d = 0; d < mms.length; d += 2) {
        if(mms[d + 1] - mms[d] >= STOPVAL) {
          // LoggingUtil.warning("No: " + (mms[d + 1] - mms[d]));
          ok = true;
          break;
        }
      }
      if(!ok) {
        return;
      }
    }
    int split = pivotizeList1D(objs, start, end, dim + 1, spos, false);
    assert(start <= split && split <= end);
    int nextdim = (dim + 1) % objs.get(0).getDimensionality();
    // LoggingUtil.warning("dim: " + dim + " min: " + min + " split: " + spos + " max:" + max + " " + start + " < " + split + " < " + end);
    if(start < split - 1) {
      mms[2 * dim] = min;
      mms[2 * dim + 1] = spos;
      zSort(objs, start, split, mms, nextdim);
    }
    if(split < end - 1) {
      mms[2 * dim] = spos;
      mms[2 * dim + 1] = max;
      zSort(objs, split, end, mms, nextdim);
    }
    // Restore ranges
    mms[2 * dim] = min;
    mms[2 * dim + 1] = max;
    // FIXME: implement completely and test.
  }
}