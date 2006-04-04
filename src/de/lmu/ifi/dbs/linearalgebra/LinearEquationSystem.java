package de.lmu.ifi.dbs.linearalgebra;

import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class for systems of linear equations.
 */
public class LinearEquationSystem {

  /**
   * Holds the class specific debug status.
   */
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;

  /**
   * Indicates trivial pivot search strategy.
   */
  private static final int TRIVAL_PIVOT_SEARCH = 0;

  /**
   * Indicates total pivot search strategy.
   */
  private static final int TOTAL_PIVOT_SEARCH = 1;

/**
   * Logger object for logging messages.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Indicates if linear equation system is solvable.
   */
  private boolean solvable;

  /**
   * Indicates if solvability has been checked.
   */
  private boolean solved;

  /**
   * The rank of the coefficient matrix.
   */
  private int rank;

  /**
   * The matrix of coefficients.
   */
  private double[][] coeff;

  /**
   * The right hand side of the equation system.
   */
  private double[] rhs;

  /**
   * Encodes row permutations, row i is at position row[i].
   */
  private int[] row;

  /**
   * Encodes column permutations, column j is at position col[j].
   */
  private int[] col;

  /**
   * Holds the special solution vector.
   */
  private double[] x_0;

  /**
   * Holds the space of solutions of the homogeneous linear equation system.
   */
  private double[][] u;

  /**
   * Indicates if linear equation system is in reduced row echelon form.
   */
  private boolean reducedRowEchelonForm;

  /**
   * Constructs a linear equation system with given coefficient matrix <code>a</code> and
   * right hand side <code>b</code>.
   *
   * @param a the matrix of the coefficients of the linear equation system
   * @param b the right hand side of the linear equation system
   */
  public LinearEquationSystem(double[][] a, double[] b) {
    if (a == null)
      throw new IllegalArgumentException("Coefficient array is null!");
    if (b == null)
      throw new IllegalArgumentException("Right hand side is null!");
    if (a.length != b.length) {
      throw new IllegalArgumentException("Coefficient matrix and right hand side " +
                                         "differ in row dimensionality!");
    }

    coeff = a;
    rhs = b;
    row = new int[coeff.length];
    for (int i = 0; i < coeff.length; i++) row[i] = i;
    col = new int[coeff[0].length];
    for (int j = 0; j < coeff[0].length; j++) col[j] = j;
    rank = 0;
    x_0 = null;
    solved = false;
    solvable = false;
    reducedRowEchelonForm = false;
  }

  /**
   * Constructs a linear equation system with given coefficient matrix <code>a</code> and
   * right hand side <code>b</code>.
   *
   * @param a                  the matrix of the coefficients of the linear equation system
   * @param b                  the right hand side of the linear equation system
   * @param rowPermutations    the row permutations, row i is at position row[i]
   * @param columnPermutations the column permutations, column i is at position column[i]
   */
  public LinearEquationSystem(double[][] a, double[] b, int[] rowPermutations, int[] columnPermutations) {
    if (a == null)
      throw new IllegalArgumentException("Coefficient array is null!");
    if (b == null)
      throw new IllegalArgumentException("Right hand side is null!");
    if (a.length != b.length) {
      throw new IllegalArgumentException("Coefficient matrix and right hand side " +
                                         "differ in row dimensionality!");
    }
    if (rowPermutations.length != a.length) {
      throw new IllegalArgumentException("Coefficient matrix and row permutation array " +
                                         "differ in row dimensionality!");
    }
    if (columnPermutations.length != a[0].length) {
      throw new IllegalArgumentException("Coefficient matrix and column permutation array " +
                                         "differ in column dimensionality!");
    }

    coeff = a;
    rhs = b;
    this.row = rowPermutations;
    this.col = columnPermutations;
    rank = 0;
    x_0 = null;
    solved = false;
    solvable = false;
    reducedRowEchelonForm = false;
  }

  /**
   * Returns a copy of the coefficient array of this linear equation system.
   *
   * @return a copy of the coefficient array of this linear equation system
   */
  public double[][] getCoefficents() {
    return coeff.clone();
  }

  /**
   * Returns a copy of the right hand side of this linear equation system.
   *
   * @return a copy of the right hand side of this linear equation system
   */
  public double[] getRHS() {
    return rhs.clone();
  }

  /**
   * Returns a copy of the row permutations, row i is at position row[i].
   *
   * @return a copy of the row permutations
   */
  public int[] getRowPermutations() {
    return row.clone();
  }

  /**
   * Returns a copy of the column permutations, column i is at position column[i].
   *
   * @return a copy of the column permutations
   */
  public int[] getColumnPermutations() {
    return col.clone();
  }

  /**
   * Tests if system has already been tested for solvability.
   *
   * @return true if a solution has already been computed, false otherwise.
   */
  public boolean isSolved() {
    return solved;
  }

  /**
   * todo
   * Solves this linear equation system by total pivot search.
   * "Total pivot search" takes as pivot element the element in the current
   * column having the biggest value.
   * If we have: <br>
   * <code>
   * ( a_11 &nbsp;&nbsp;&nbsp;&nbsp; ... &nbsp;&nbsp;&nbsp;&nbsp; a_1n      ) <br>
   * (  0 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;  ...   &nbsp;&nbsp;&nbsp;&nbsp; a_2n      ) <br>
   * (  0 ... a_ii     &nbsp;&nbsp;&nbsp; ... a_in      )<br>
   * (  0 ... a_(i+1)i ... a_(i+1)n  ) <br>
   * (  0 ... a_ni     &nbsp;&nbsp;&nbsp; ... a_nn      ) <br>
   * </code>
   * Then we search for x,y in {i,...n}, so that |a_xy| > |a_ij|
   */
  public void solveByTotalPivotSearch() {
    solve(TOTAL_PIVOT_SEARCH);
  }

  /**
   * Solves this linear equation system by trivial pivot search.
   * "Trivial pivot search" takes as pivot element the next element in the current
   * column beeing non zero.
   */
  public void solveByTrivialPivotSearch() {
    solve(TRIVAL_PIVOT_SEARCH);
  }

  /**
   * Checks if a solved system is solvable.
   *
   * @return true if this linear equation system is solved and solvable
   */
  public boolean isSolvable() {
    return solvable && solved;
  }

  /**
   * Returns a string representation of this equation system.
   *
   * @param prefix         the prefix of each line
   * @param fractionDigits the number of fraction digits for output accuracy
   * @return a string representation of this equation system
   */
  public String equationsToString(String prefix, int fractionDigits) {
    if ((coeff == null) || (rhs == null)
        || (row == null) || (col == null)) {
      throw new NullPointerException();
    }

    DecimalFormat nf = new DecimalFormat();
    nf.setMinimumFractionDigits(fractionDigits);
    nf.setMaximumFractionDigits(fractionDigits);
    nf.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    nf.setNegativePrefix("");
    nf.setPositivePrefix("");

    int[] coeffDigits = maxIntegerDigits(coeff);
    int rhsDigits = maxIntegerDigits(rhs);

    StringBuffer buffer = new StringBuffer();
    buffer.append("\n").append(prefix);
    for (int i = 0; i < coeff.length; i++) {
      for (int j = 0; j < coeff[row[0]].length; j++) {
        format(nf, buffer, coeff[row[i]][col[j]], coeffDigits[col[j]]);
        buffer.append(" * x_" + col[j]);
      }
      buffer.append(" =");
      format(nf, buffer, rhs[row[i]], rhsDigits);

      if (i < coeff.length - 1) buffer.append("\n" + prefix);
      else buffer.append("\n");
    }
    return buffer.toString();
  }

  /**
   * Returns a string representation of this equation system.
   *
   * @param fractionDigits the number of fraction digits for output accuracy
   * @return a string representation of this equation system
   */
  public String equationsToString(int fractionDigits) {
    return equationsToString("", fractionDigits);
  }

  /**
   * Returns a string representation of the solution of this equation system.
   *
   * @return a string representation of the solution of this equation system
   */
  public String solutionToString(int fractionDigits) {
    if (! isSolvable()) {
      throw new IllegalStateException("System is not solvable!");
    }

    DecimalFormat nf = new DecimalFormat();
    nf.setMinimumFractionDigits(fractionDigits);
    nf.setMaximumFractionDigits(fractionDigits);
    nf.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    nf.setNegativePrefix("");
    nf.setPositivePrefix("");

    int row = coeff[0].length / 2;
    int params = u.length;
    int paramsDigits = integerDigits(params);

    int x0Digits = maxIntegerDigits(x_0);
    int[] uDigits = maxIntegerDigits(u);
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < x_0.length; i++) {
      double value = x_0[i];
      format(nf, buffer, value, x0Digits);
      for (int j = 0; j < u[0].length; j++) {
        if (i == row) {
          buffer.append("  +  a_" + j + " * ");
        }
        else {
          buffer.append("          ");
          for (int d = 0; d < paramsDigits; d++) buffer.append(" ");
        }
        format(nf, buffer, u[i][j], uDigits[j]);

      }
      buffer.append("\n");
    }
    return buffer.toString();
  }

  /**
   * Brings this linear equation system into reduced row echelon form
   * with choice of pivot method.
   *
   * @param method the pivot search method to use
   */
  private void reducedRowEchelonForm(int method) {
    final int rows = coeff.length;
    final int cols = coeff[0].length;

    int k = -1;   // denotes current position on diagonal
    int pivotRow; // row index of pivot element
    int pivotCol; // column index of pivot element
    double pivot; // value of pivot element

    // main loop, transformation to reduced row echelon form
    boolean exitLoop = false;

    while (! exitLoop) {
      k++;

      // pivot search for entry in remaining matrix
      // (depends on chosen method in switch)
      // store position in pivotRow, pivotCol
      MatrixPosition pivotPos = new MatrixPosition(0, 0);
      MatrixPosition currPos = new MatrixPosition(k, k);

      switch (method) {
        case TRIVAL_PIVOT_SEARCH :
          pivotPos = nonZeroPivotSearch(k);
          break;
        case TOTAL_PIVOT_SEARCH :
          pivotPos = totalPivotSearch(k);
          break;
      }
      pivotRow = pivotPos.rowPos;
      pivotCol = pivotPos.colPos;
      pivot = coeff[this.row[pivotRow]][col[pivotCol]];

      if (DEBUG) {
        StringBuffer msg = new StringBuffer();
        msg.append("equations ").append(equationsToString(4));
        msg.append("  *** pivot at (").append(pivotRow).append(",").append(pivotCol).append(") = ").append(pivot).append("\n");
        logger.fine(msg.toString());
      }

      // permute rows and colums to get this entry onto
      // the diagonal
      permutePivot(pivotPos, currPos);

      // test conditions for exiting loop
      // after this iteration
      // reasons are: Math.abs(pivot) == 0
      if ((Math.abs(pivot) <= Matrix.DELTA)) {
        exitLoop = true;
      }

      // pivoting only if Math.abs(pivot) > 0
      //                  and k <= m - 1
      if ((Math.abs(pivot) > Matrix.DELTA)) {
        rank++;
        pivotOperation(k);
      }

      // test conditions for exiting loop
      // after this iteration
      // reasons are: k == rows-1 : no more rows
      //              k == cols-1 : no more colums
      if (k == rows - 1 || k == cols - 1) {
        exitLoop = true;
      }
    }//end while

    reducedRowEchelonForm = true;
  }

  /**
   * Method for total pivot search, searches for
   * x,y in {k,...n}, so that |a_xy| > |a_ij|
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
   * Method for trivial pivot search, searches for non-zero entry.
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
    for (int i = k + 1; i < coeff[k].length; i++) {
      coeff[row[k]][col[i]] /= pivot;
    }
    rhs[row[k]] /= pivot;

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("set pivot element to 1 ").append(equationsToString(4));
      logger.info(msg.toString());
    }

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

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("after pivot operation ").append(equationsToString(4));
      logger.fine(msg.toString());
    }
  }

  /**
   * solves linear system with the chosen method
   *
   * @param method the pivot search method
   */
  private void solve(int method) throws NullPointerException {
    // solution exists
    if (solved) {
      return;
    }

    // bring in reduced row echelon form
    if (! reducedRowEchelonForm) {
      reducedRowEchelonForm(method);
    }

    if (! isSolvable(method)) {
      if (DEBUG) {
        logger.fine("Equation system is not solvable!");
      }
      return;
    }

    // compute one special solution
    int cols = coeff[0].length;
    List<Integer> boundIndices = new ArrayList<Integer>();
    x_0 = new double[cols];
    for (int i = 0; i < coeff.length; i++) {
      for (int j = i; j < coeff[row[i]].length; j++) {
        if (coeff[row[i]][col[j]] == 1) {
          x_0[col[i]] = rhs[row[i]];
          boundIndices.add(col[i]);
          break;
        }
      }
    }
    List<Integer> freeIndices = new ArrayList<Integer>();
    for (int i = 0; i < coeff[0].length; i++) {
      if (boundIndices.contains(i)) continue;
      freeIndices.add(i);
    }

    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("\nSpecial solution x_0 = [").append(Util.format(x_0, ",", 4)).append("]");
      msg.append("\nbound Indices ").append(boundIndices);
      msg.append("\nfree Indices ").append(freeIndices);
    }

    // compute solution space of homogeneous linear equation system
    Integer[] freeParameters = freeIndices.toArray(new Integer[freeIndices.size()]);
    Integer[] boundParameters = boundIndices.toArray(new Integer[boundIndices.size()]);
    Arrays.sort(boundParameters);
    int freeIndex = 0;
    int boundIndex = 0;
    u = new double[cols][freeIndices.size()];

    for (int j = 0; j < u[0].length; j++) {
      for (int i = 0; i < u.length; i++) {
        if (freeIndex < freeParameters.length && i == freeParameters[freeIndex]) {
          u[i][j] = 1;
        }
        else if (boundIndex < boundParameters.length && i == boundParameters[boundIndex]) {
          u[i][j] = -coeff[row[boundIndex]][freeParameters[freeIndex]];
          boundIndex++;
        }
      }
      freeIndex++;
      boundIndex = 0;

    }

    if (DEBUG) {
      msg.append("\nU");
      for (double[] anU : u) {
        msg.append("\n").append(Util.format(anU, ",", 4));
      }
      logger.fine(msg.toString());
    }

    solved = true;
  }

  /**
   * Checks solvability of this linear equation system with the chosen method.
   *
   * @param method the pivot search method
   * @return true if linear system in solvable
   */
  private boolean isSolvable(int method) throws NullPointerException {
    if (solved) {
      return solvable;
    }

    if (! reducedRowEchelonForm) {
      reducedRowEchelonForm(method);
    }

    // test if rank(coeff) == rank(coeff|rhs)
    for (int i = rank; i < rhs.length; i++) {
      if (Math.abs(rhs[row[i]]) > Matrix.DELTA) {
        solvable = false;
        return false;  // not solvable
      }
    }

    solvable = true;
    return true;
  }

  /**
   * Returns the maximum integer digits in each column of the specified values.
   *
   * @param values the values array
   * @return the maximum integer digits in each column of the specified values
   */
  private int[] maxIntegerDigits(double[][] values) {
    int[] digits = new int[values[0].length];
    for (int j = 0; j < values[0].length; j++) {
      for (double[] value : values) {
        digits[j] = Math.max(digits[j], integerDigits(value[j]));
      }
    }
    return digits;
  }

  /**
   * Returns the maximum integer digits of the specified values.
   *
   * @param values the values array
   * @return the maximum integer digits of the specified values
   */
  private int maxIntegerDigits(double[] values) {
    int digits = 0;
    for (double value : values) {
      digits = Math.max(digits, integerDigits(value));
    }
    return digits;
  }

  /**
   * Returns the integer digits of the specified double value.
   *
   * @param d the double value
   * @return the integer digits of the specified double value
   */
  private int integerDigits(double d) {
    double value = Math.abs(d);
    if (value < 10) return 1;
    return (int) (Math.log(value) / Math.log(10) + 1);
  }

  /**
   * Helper method for output of equations and solution. Appends the specified double value
   * to the given string buffer according the number format and the maximum
   * number of integer digits.
   *
   * @param nf               the number format
   * @param buffer           the string buffer to append the value to
   * @param value            the value to append
   * @param maxIntegerDigits the maximum number of integer digits
   */
  private void format(NumberFormat nf, StringBuffer buffer, double value, int maxIntegerDigits) {
    if (value >= 0) buffer.append(" + ");
    else buffer.append(" - ");
    int digits = maxIntegerDigits - integerDigits(value);
    for (int d = 0; d < digits; d++) buffer.append(" ");
    buffer.append(nf.format(value));
  }

  public static void main(String[] args) {
    double[][] a;
    double[] b;
    LinearEquationSystem lq;

    // a)
    a = new double[][]{
    {+1, 0, 2},
    {+3, 2, 1},
    {+4, 1, 3}
    };
    b = new double[]{1, 0, 0};
    lq = new LinearEquationSystem(a, b);
    lq.solveByTotalPivotSearch();
    System.out.println("solvable " + lq.isSolvable());
    if (lq.isSolvable()) {
      System.out.println("sol \n" + lq.solutionToString(4));
    }
    System.out.println("eq     " + lq.equationsToString("### ", 4));

    // b)
//  double[][] a = new double[][]{
//    {+1, +1, -1, -1},
//    {+2, +5, -7, -5},
//    {+2, -1, +1, +3},
//    {+5, +2, -4, +2}
//    };
//    double[] b = new double[]{1,-2,4,6};

    // c)
//    double[][] a = new double[][]{
//    {+0, +2, -1},
//    {+2, +1, -1}
//    };
//    double[] b = new double[]{-1, 1};

    // d)
//    double[][] a = new double[][]{
//    {+2, -1, +3}
//    };
//    double[] b = new double[]{7};

    // e)
//    a = new double[][]{
//    {+1, -1, 0, +0, +1},
//    {+1, +1, 0, -3, +0},
//    {+2, -1, 0, +1, -1},
//    {-1, +2, 0, -2, -1}
//    };
//    b = new double[]{3, 6, 5, -1};

    // f)
    a = new double[][]{
    {+0, +1, 0, 0, +1, +0},
    {+0, +0, 0, 1, +1, +0},
    {+0, +1, 0, 0, +0, +1}
    };
    b = new double[]{20, -1, 1};
    lq = new LinearEquationSystem(a, b);
    lq.solveByTotalPivotSearch();
//    lq.solveByTrivialPivotSearch();
    NumberFormat nf = NumberFormat.getInstance(Locale.US);
    nf.setMaximumFractionDigits(4);
    nf.setMinimumFractionDigits(4);

    System.out.println("solvable " + lq.isSolvable());
    if (lq.isSolvable()) {
      System.out.println("sol \n" + lq.solutionToString(4));
    }
    System.out.println("eq     " + lq.equationsToString("### ", 4));
  }
}
