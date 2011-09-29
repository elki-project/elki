package de.lmu.ifi.dbs.elki.data.spatial;

/**
 * Class to use SpatialComparable objects with the - slightly more flexible -
 * adapter interface.
 * 
 * @author Erich Schubert
 */
public class SpatialComparableAdapter implements SpatialAdapter<SpatialComparable> {
  /**
   * Static instance
   */
  public static final SpatialComparableAdapter STATIC = new SpatialComparableAdapter();

  @Override
  public int getDimensionality(SpatialComparable obj) {
    return obj.getDimensionality();
  }

  @Override
  public double getMin(SpatialComparable obj, int dim) {
    return obj.getMin(dim + 1);
  }

  @Override
  public double getMax(SpatialComparable obj, int dim) {
    return obj.getMax(dim + 1);
  }

  @Override
  public double getLen(SpatialComparable obj, int dim) {
    return obj.getMax(dim + 1) - obj.getMin(dim + 1);
  }

  @Override
  public double getVolume(SpatialComparable obj) {
    return SpatialUtil.volume(obj);
  }

  @Override
  public boolean equal(SpatialComparable obj1, SpatialComparable obj2) {
    return SpatialUtil.equals(obj1, obj2);
  }
}
