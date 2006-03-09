package de.lmu.ifi.dbs.linearalgebra;

import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * class for systems of linear equations
 */
public class LinearEquation {

  private List<MatrixPosition[]> permutationHistory = new ArrayList<MatrixPosition[]>();

  /**
   * will be true if system is solvable
   */
  private boolean solvable;

  /**
   * will be true if solvability has been checked
   */
  private boolean solved;

  /**
   * the rank of the coefficient matrix
   */
  private int rank;

  /**
   * the matrix of coefficients
   */
  private double[][] coeff;

  /**
   * the right hand side
   */
  private double[] rhs;

  /**
   * encodes row permutations, row i is at position row[i]
   */
  private int[] row;

  /**
   * encodes column permutations, column j is at position col[j]
   */
  private int[] col;

  /**
   * holds the solution vector
   */
  private double[] solution;

  /**
   * is true if system is in triangular form
   */
  private boolean triangular;


  /**
   * default constructor
   */
  public LinearEquation() {
    solvable = false;
    solved = false;
    triangular = false;
    rank = 0;
    solution = null;
    coeff = null;
    rhs = null;
    row = null;
    col = null;
  }

  /**
   * constructs object with given coeff matrix and rhs
   * and initializes row and col
   *
   * @param a the matrix to which the coeff matrix is set
   * @param b the rhs to which the rhs is set
   */
  public LinearEquation(double[][] a, double[] b)
  throws NullPointerException, DimensionException {
    if (a == null || b == null) {
      throw new NullPointerException("zugewiesener Array "
                                     + "ist null");
    }
    if (a.length != b.length) {
      throw new DimensionException("unvertraegliche "
                                   + "Dimension");
    }
    coeff = a;
    rhs = b;
    row = new int[coeff.length];
    for (int i = 0; i < coeff.length; i++) row[i] = i;
    col = new int[coeff[0].length];
    for (int j = 0; j < coeff[0].length; j++) col[j] = j;
    rank = 0;
    solution = null;
    solved = false;
    solvable = false;
    triangular = false;


  }

  /**
   * tests if system has been tested for solvability
   *
   * @return true if a solutioan has already been computed
   */
  public boolean isSolved() {
    return solved;
  }


  /**
   * brings system into triangular form with choice of pivot method
   *
   * @throws NullPointerException
   */
  private void triangularForm(int method) throws NullPointerException {
    if (coeff == null || rhs == null) {
      throw new NullPointerException();
    }

    int m = coeff.length;
    int n = coeff[0].length;

    int k = -1;   // denotes current position on diagonal
    int pivotRow = 0; // row index of pivot element
    int pivotCol = 0; // column index of pivot element
    double pivot; // value of pivot element

    // main loop, transformation to triangle form
    boolean exitLoop = false;

    while (! exitLoop) {
      k++;

      // pivot search for entry in remaining matrix
      // (depends on chosen method in switch)
      // store position in pivotRow, pivotCol

      MatrixPosition pivotPos = new MatrixPosition(0, 0);
      MatrixPosition currPos = new MatrixPosition(k, k);

      switch (method) {
        case 0 :
          pivotPos = nonZeroPivotSearch(k);
          break;
        case 1 :
          pivotPos = totalPivotSearch(k);
          break;
      }
      pivotRow = pivotPos.rowPos;
      pivotCol = pivotPos.colPos;
      pivot = coeff[row[pivotRow]][col[pivotCol]];

      // permute rows and colums to get this entry onto
      // the diagonal
      permutePivot(pivotPos, currPos);

      // test conditions for exiting loop
      // after this iteration
      // reasons are: Math.abs(pivot) == 0
      //              k == m - 1 : no more rows
      //              k == n - 1 : no more colums
      if ((Math.abs(pivot) <= Matrix.DELTA) || (k == m - 1) || (k == n - 1)) {
        exitLoop = true;
      }

      // update  rank
      if (Math.abs(pivot) > Matrix.DELTA) {
        rank++;
      }

      // pivoting only if Math.abs(pivot) > 0
      //                  and k < m - 1
      if ((Math.abs(pivot) > Matrix.DELTA) && (k < m - 1)) {
        pivotOperation(k);
      }
    }//end while
    triangular = true;
  }

  /**
   * method for total pivot search
   *
   * @param k search starts at entry (k,k)
   * @return the position of the found pivot element
   */
  private MatrixPosition totalPivotSearch(int k) {
    double max = 0;
    int i, j, pivotRow = k, pivotCol = k;
    double absValue;
    for (i = k; i < coeff.length; i++) {
      for (j = k; j < coeff[0].length; j++) {

        // compute absolute value of
        // current entry in absValue
        absValue = Math.abs(coeff[row[i]][col[j]]);

        // compare absValue with value max
        // found so far
        if (max < absValue) {
          // remember new value and position
          max = absValue;
          pivotRow = i;
          pivotCol = j;
        }//end if
      }//end for j
    }//end for k
    return new MatrixPosition(pivotRow, pivotCol);
  }

  /**
   * method for trivial pivot search, searches for non-zero entry
   *
   * @param k search starts at entry (k,k)
   * @return the position of the found pivot element
   */
  private MatrixPosition nonZeroPivotSearch(int k) {

    int i, j;
    double absValue;
    for (i = k; i < coeff.length; i++) {
      for (j = k; j < coeff[0].length; j++) {

        // compute absolute value of
        // current entry in absValue
        absValue = Math.abs(coeff[row[i]][col[j]]);

        // check if absValue is non-zero
        if (absValue > 0) { // found a pivot element
          return new MatrixPosition(i, j);
        }//end if
      }//end for  j
    }//end for k
    return new MatrixPosition(k, k);
  }

  /**
   * permutes two matrix rows and two matrix columns
   *
   * @param pos1 the fist position for the permutation
   * @param pos2 the second position for the permutation
   */
  private void permutePivot(MatrixPosition pos1, MatrixPosition pos2) {
    int r1 = pos1.rowPos;
    int c1 = pos1.colPos;
    int r2 = pos2.rowPos;
    int c2 = pos2.colPos;
    int index;
    index = row[r2];
    row[r2] = row[r1];
    row[r1] = index;
    index = col[c2];
    col[c2] = col[c1];
    col[c1] = index;

    permutationHistory.add(new MatrixPosition[]{pos1, pos2});
  }

  /**
   * performs a pivot operation
   *
   * @param k pivoting takes place below (k,k)
   */
  private void pivotOperation(int k) {
    double pivot = coeff[row[k]][col[k]];

    // pivot row: set pivot to 1
    coeff[row[k]][col[k]] = 1;
    for (int i = k + 1; i < coeff.length; i++) {
      coeff[row[k]][col[i]] /= pivot;
    }
    rhs[row[k]] /= pivot;

//    for (int i = k + 1; i < coeff.length; i++) {
    for (int i = 0; i < coeff.length; i++) {
      if (i == k) continue;

      // compute factor
      double q = coeff[row[i]][col[k]];

      // modify entry a[i,k], i <> k
      coeff[row[i]][col[k]] = 0;

      // modify entries a[i,j], i > k fixed, j = k+1...n-1
      for (int j = k + 1; j < coeff[0].length; j++) {
        coeff[row[i]][col[j]] = coeff[row[i]][col[j]]
                                - coeff[row[k]][col[j]] * q;
      }//end for j

      // modify right-hand-side
      rhs[row[i]] = rhs[row[i]] - rhs[row[k]] * q;
    }//end for k

  }


  /**
   * solves linear system with the chosen method
   *
   * @param method the pivot search method
   */
  private void solve(int method) throws NullPointerException {
    if (solved) {
      return; // solution exists
    }
    if (! triangular) {
      triangularForm(method);
    }
    if (! isSolvable(method)) {
      return;
    }

    int n = coeff[0].length;
    double[] x = new double[n];

    // set x[rank] = ... = x[n-1] = 0
    if (rank < n) {
      for (int j = rank; j < n; j++) {
        x[col[j]] = 0;
      }
    }//end if

    // compute x[rank-1]
    x[col[rank - 1]] = rhs[row[rank - 1]]
                       / coeff[row[rank - 1]][col[rank - 1]];

    // compute remaining x[i] backwards
    for (int i = rank - 2; i >= 0; i--) {
      x[col[i]] = rhs[row[i]];
      for (int j = i + 1; j <= rank - 1; j++) {
        x[col[i]] = x[col[i]]
                    - coeff[row[i]][col[j]] * x[col[j]];
      }//end for j
      x[col[i]] = x[col[i]]
                  / (double) coeff[row[i]][col[i]];
    }//end for i

    solution = x;
    solved = true;
  }

  /**
   * solves linar system by total pivot search
   */
  public void solveByTotalPivotSearch() throws NullPointerException {
    solve(1);
  }

  /**
   * solves linar system without pivot search
   */
  public void solveWithoutPivotSearch() throws NullPointerException {
    solve(0);
  }

  /**
   * checks solvability of linar system with the chosen method
   *
   * @param method the pivot search method
   * @return true if linear system in solvable
   */
  private boolean isSolvable(int method) throws NullPointerException {
    if (solved) {
      return solvable;
    }
    if (! triangular) {
      triangularForm(method);
    }
    for (int i = rank; i < rhs.length; i++) {
      if (Math.abs(rhs[row[i]]) > 0) {
        solvable = false;
        return false;  // not solvable
      }// end if
    }// end for
    solvable = true;
    return true;
  }

  /**
   * checks if a solved system is solvable
   *
   * @return true if linear system is solved and solvable
   */
  public boolean isSolvable() {
    return solvable && solved;
  }

  /**
   * returns the solution
   *
   * @return <code>double</code> array representing a solution
   */
  public double[] getSolution() {
    return solution;
  }

  public Matrix getEquationMatrix() {

    double[][] m = new double[coeff.length][];
    for (int i = 0; i < m.length; i++) {
      m[i] = new double[coeff.length + 1];
      for (int j = 0; j < m[i].length; j++) {
        if (j < coeff.length) {
          m[i][j] = coeff[i][j];
        }
        else {
          m[i][j] = rhs[i];
        }
        if (Math.abs(m[i][j]) < Matrix.DELTA) m[i][j] = 0;
      }
    }

//    List<Double[]> m = new ArrayList<Double[]>();
//    for (int i = 0; i < coeff.length; i++) {
//      Double[] m_i = new Double[coeff.length + 1];
//      for (int j = 0; j < m_i.length; j++) {
//        if (j < coeff.length) m_i[j] = coeff[i][j];
//        else m_i[j] = rhs[i];
//      }
//      m.add(m_i);
//    }

    /*
    int index = 0;
    while (true) {
      System.out.println(equationsToString());
      Double[] doubles = m.get(index);
      for (int i = 0; i < doubles.length; i++) {

      }
      if (doubles[index] == 0 && index != m.size()-1) {
        m.remove(index);
        m.add(doubles);
      }
      else {
        index++;
        break;
      }
    }               */

//    double[][] mm = new double[m.size()][];
//    for (int i = 0; i < m.size(); i++) {
//      Double[] doubles = m.get(i);
//      mm[i] = Util.unbox(doubles);
//    }

    return new Matrix(m).gaussJordanElimination();
  }

  public String equationsToString() throws NullPointerException {
    if ((coeff == null) || (rhs == null)
        || (row == null) || (col == null)) {
      throw new NullPointerException();
    }

    StringBuffer strBuf = new StringBuffer();
    String str = "      ";
    strBuf.append(str + "\n");
    for (int i = 0; i < coeff.length; i++) {
      str = "";
      for (int j = 0; j < coeff[0].length; j++) {
        str = str + "  " + Util.format(coeff[i][j], 4);
      }
      str = str + " " + Util.format(rhs[i], 4);
      strBuf.append(str + "\n");
    }
    return strBuf.toString();
  }

  /**
   * returns current matrix (A|b) as String
   *
   * @return String representing current matrix
   */
  public String equationsToStringOLD() throws NullPointerException {
    if ((coeff == null) || (rhs == null)
        || (row == null) || (col == null)) {
      throw new NullPointerException();
    }

//    System.out.println(new Matrix(coeff));

    StringBuffer strBuf = new StringBuffer();
    String str = "      ";
    strBuf.append(str + "\n");
    for (int i = 0; i < coeff.length; i++) {
      str = "";
      for (int j = 0; j < coeff[0].length; j++) {
        str = str + "  " + Util.format(coeff[row[i]][col[j]], 4);
      }
      str = str + " " + Util.format(rhs[row[i]], 4);
      strBuf.append(str + "\n");
    }
    return strBuf.toString();
  }

  /**
   * returns solution as String
   *
   * @return string representing solution vector
   */
  public String solutionToString() throws NullPointerException {
    if (solution == null) throw new NullPointerException();

    StringBuffer strBuf = new StringBuffer();
    for (int j = 0; j < solution.length; j++) {
      strBuf.append("x_" + j + " = " + solution[j] + "\n");
    }

    return strBuf.toString();
  }
}
