package elki.math.statistics.tests.mcde;

import elki.math.MathUtil;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.datastructures.arrays.IntegerArrayQuickSort;

public abstract class MCDETest<R extends MCDETest.RankStruct> {

  public MCDETest() {
  }

  /**
   * Structure to hold return values in index creation for MCDEDependenceEstimate
   */
  public class RankStruct {
    public int index;

    public double adjusted;

    public RankStruct(int index, double adjusted) {
      this.index = index;
      this.adjusted = adjusted;
    }
  }

  /**
   * Build a sorted index of objects.
   *
   * @param adapter Data adapter
   * @param data    Data array
   * @param len     Length of data
   * @return Sorted index
   */
  protected <A> int[] sortedIndex(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
    int[] s1 = MathUtil.sequence(0, len);
    IntegerArrayQuickSort.sort(s1, (x, y) -> Double.compare(adapter.getDouble(data, x), adapter.getDouble(data, y)));
    return s1;
  }

  /**
   * Overloaded wrapper for corrected_ranks()
   */
  public <A> R[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
    return corrected_ranks(adapter, data, sortedIndex(adapter, data, len));
  }

  /**
   * Subclass must implement computation of corrected rank index.
   *
   * @param adapter ELKI NumberArrayAdapter Subclass
   * @param data    One dimensional array containing one dimension of the data
   * @param idx     Return value of sortedIndex()
   * @return Array of RankStruct, acting as rank index
   */
  abstract public <A> R[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx);

  /**
   * Subclass must implement the computation of the statistical test, based on the slicing scheme.
   *
   * @param len             No of data instances
   * @param slice           An array of boolean resulting from a random slice
   * @param corrected_ranks the precomputed index structure for the reference dimension
   * @return a 1 - p-value
   */
  abstract public double statistical_test(int len, boolean[] slice, R[] corrected_ranks);
}
