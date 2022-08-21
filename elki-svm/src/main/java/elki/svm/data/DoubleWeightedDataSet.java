package elki.svm.data;

import java.util.Arrays;

import elki.utilities.datastructures.arrays.ArrayUtil;

/**
 * This is an efficient array based data set implementation.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class DoubleWeightedDataSet implements DataSet {
  DataSet inner;

  int[] idx;

  double[] weight;

  int size = 0;

  public DoubleWeightedDataSet(DataSet inner, int size) {
    this.inner = inner;
    idx = new int[size];
    weight = new double[size];
  }

  @Override
  public double similarity(int i, int j) {
    return inner.similarity(idx[i], idx[j]);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public double value(int i) {
    return weight[i];
  }

  @Override
  public int classnum(int i) {
    // Probably not what you meant to do!
    return (int) weight[i];
  }

  @Override
  public void swap(int i, int j) {
    ArrayUtil.swap(idx, i, j);
    ArrayUtil.swap(weight, i, j);
  }

  public void add(int v, double w) {
    if(size == idx.length) {
      final int newlen = idx.length << 1;
      idx = Arrays.copyOf(idx, newlen);
      weight = Arrays.copyOf(weight, newlen);
    }
    idx[size] = v;
    weight[size++] = w;
  }

  public void clear() {
    size = 0;
  }
}
