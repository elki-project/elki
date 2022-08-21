package elki.svm.data;

import java.util.Arrays;

import elki.utilities.datastructures.arrays.ArrayUtil;

/**
 * This is an efficient array based data set implementation.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class ByteWeightedArrayDataSet implements DataSet {
  DataSet inner;

  int[] idx;

  byte[] weight;

  int size = 0;

  public ByteWeightedArrayDataSet(DataSet inner, int size) {
    this.inner = inner;
    this.idx = new int[size];
    this.weight = new byte[size];
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

  public void add(int v, byte w) {
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
