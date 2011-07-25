package de.lmu.ifi.dbs.elki.math.statistics;

/**
 * QuickSelect computes ("selects") the element at a given rank and can be used
 * to compute Medians and arbitrary quantiles by computing the appropriate rank.
 * 
 * This algorithm is essentially an incomplete QuickSort that only descends into
 * that part of the data that we are interested in, and also attributed to
 * Charles Antony Richard Hoare
 * 
 * @author Erich Schubert
 */
public class QuickSelect {
  /**
   * For small arrays, use a simpler method:
   */
  private static final int SMALL = 10;

  /**
   * QuickSelect is essentially quicksort, except that we only "sort" that half
   * of the array that we are interested in.
   * 
   * Note: the array is <b>modified</b> by this.
   * 
   * @param data Data to process
   * @param rank Rank position that we are interested in (integer!)
   * @return Value at the given rank
   */
  public static double quickSelect(double[] data, int rank) {
    quickSelect(data, 0, data.length - 1, rank);
    return data[rank];
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   * 
   * Note: the array is <b>modified</b> by this.
   * 
   * @param data Data to process
   * @return
   */
  public static double median(double[] data) {
    return median(data, 0, data.length - 1);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   * 
   * Note: the array is <b>modified</b> by this.
   * 
   * @param data Data to process
   * @param begin Begin of valid values
   * @param end End of valid values (inclusive!)
   * @return
   */
  public static double median(double[] data, int begin, int end) {
    final int length = (end + 1) - begin;
    assert (length > 0);
    // Integer division is "floor" since we are non-negative.
    final int left = begin + (length - 1) / 2;
    quickSelect(data, begin, end, left);
    if(length % 2 == 1) {
      return data[left];
    }
    else {
      quickSelect(data, begin, end, left + 1);
      return data[left] + (data[left + 1] - data[left]) / 2;
    }
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   * 
   * Note: the array is <b>modified</b> by this.
   * 
   * @param data Data to process
   * @param quant Quantile to compute
   * @return Value at quantile
   */
  public static double quantile(double[] data, double quant) {
    return quantile(data, 0, data.length - 1, quant);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   * 
   * Note: the array is <b>modified</b> by this.
   * 
   * @param data Data to process
   * @param begin Begin of valid values
   * @param end End of valid values (inclusive!)
   * @param quant Quantile to compute
   * @return Value at quantile
   */
  public static double quantile(double[] data, int begin, int end, double quant) {
    final int length = (end + 1) - begin;
    assert (length > 0);
    // Integer division is "floor" since we are non-negative.
    final double dleft = begin + (length + 1) * quant - 1;
    final int ileft = (int) Math.floor(dleft);
    final double err = dleft - ileft;

    quickSelect(data, begin, end, ileft);
    if(err <= Double.MIN_NORMAL) {
      return data[ileft];
    }
    else {
      quickSelect(data, begin, end, ileft + 1);
      // Mix:
      double mix = data[ileft] + (data[ileft + 1] - data[ileft]) * err;
      return mix;
    }
  }

  /**
   * QuickSelect is essentially quicksort, except that we only "sort" that half
   * of the array that we are interested in.
   * 
   * @param data Data to process
   * @param start Interval start
   * @param end Interval end (inclusive)
   * @param rank rank position we are interested in (starting at 0)
   */
  public static void quickSelect(double[] data, int start, int end, int rank) {
    // Optimization for small arrays
    // This also ensures a minimum size below
    if(start + SMALL > end) {
      insertionSort(data, start, end);
      return;
    }

    // Pick pivot from three candidates: start, middle, end
    // Since we compare them, we can also just "bubble sort" them.
    final int middle = (start + end) / 2;
    if(data[start] > data[middle]) {
      swap(data, start, middle);
    }
    if(data[start] > data[end]) {
      swap(data, start, end);
    }
    if(data[middle] > data[end]) {
      swap(data, middle, end);
    }
    // TODO: use more candidates for larger arrays?

    final double pivot = data[middle];
    // Move middle element out of the way, just before end
    // (Since we already know that "end" is bigger)
    swap(data, middle, end - 1);

    // Begin partitioning
    int i = start + 1, j = end - 2;
    // This is classic quicksort stuff
    while(true) {
      while(data[i] < pivot) {
        i++;
      }
      while(data[j] > pivot) {
        j--;
      }
      if(i >= j) {
        break;
      }
      swap(data, i, j);
    }

    // Move pivot (former middle element) back into the appropriate place
    swap(data, i, end - 1);

    // In contrast to quicksort, we only need to recurse into the half we are
    // interested in.
    if(rank < i) {
      quickSelect(data, start, i - 1, rank);
    }
    else if(rank > i) {
      quickSelect(data, i + 1, end, rank);
    }
  }

  /**
   * Sort a small array using repetitive insertion sort.
   * 
   * @param data Data to sort
   * @param start Interval start
   * @param end Interval end
   */
  private static void insertionSort(double[] data, int start, int end) {
    for(int i = start + 1; i <= end; i++) {
      for(int j = i; j > start && data[j - 1] > data[j]; j--) {
        swap(data, j, j - 1);
      }
    }
  }

  /**
   * The usual swap method.
   * 
   * @param data Array
   * @param a First index
   * @param b Second index
   */
  private static final void swap(double[] data, int a, int b) {
    double tmp = data[a];
    data[a] = data[b];
    data[b] = tmp;
  }
}