package elki.svm.data;

/**
 * API to plug in custom data representations into libSVM.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public interface DataSet {
  /**
   * Size of data set.
   * 
   * @return Data set size.
   */
  int size();

  /**
   * Get the ith element.
   *
   * @param i Element offset
   * @param j Element offset
   * @return Element
   */
  double similarity(int i, int j);

  /**
   * Get the value of the ith element.
   *
   * @param i Element offset
   * @return Value
   */
  double value(int i);

  /**
   * Get the class number of the ith element.
   *
   * @param i Element offset
   * @return Class number
   */
  int classnum(int i);

  /**
   * Swap two elements.
   * 
   * @param i First position
   * @param j Second position
   */
  void swap(int i, int j);
}
