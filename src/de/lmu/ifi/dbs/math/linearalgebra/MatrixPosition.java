package de.lmu.ifi.dbs.math.linearalgebra;

/**
 * class for matrix positions
 */
public class MatrixPosition {
  int rowPos;
  int colPos;

  /**
   * Constructor
   *
   * @param i the row position
   * @param j the column position
   */
  MatrixPosition(int i, int j) {
    rowPos = i;
    colPos = j;
  }
}
