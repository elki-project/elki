package de.lmu.ifi.dbs.data.synthetic;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Util;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CorrelationGenerator {
  private static Random RANDOM = new Random();

  private static double JITTER_STANDARD_DEVIATION = 0.01;

  private static double MIN = 0;
  private static double MAX = 1;
  private static double COEFFICIENT_MIN = 0;
  private static double COEFFICIENT_MAX = 1;

  public static void main(String[] args) {
    try {
      String dir = "/nfs/infdbs/WissProj/CorrelationClustering/DependencyDerivator/experiments/synthetic/";
      for (int dim = 30; dim <= 50; dim += 5) {
        System.out.println("");
        System.out.println("");
        System.out.println("dim " + dim);
        int corrDim = RANDOM.nextInt(dim - 1) + 1;
        Matrix point = centroid(dim);
        Matrix basis = correlationBasis(dim, corrDim);
        System.out.println("basis " + basis);
        boolean jitter = true;
        PrintStream outStream = new PrintStream(new FileOutputStream(dir + "dim_" + dim + "_" + corrDim + "c.txt"));
        generateCorrelation(1000, point, basis, jitter, outStream, true);
        outStream.flush();
        outStream.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  private static void generateCorrelation(int numberOfPoints, final Matrix point, final Matrix basis,
                                          boolean jitter, PrintStream outStream, boolean verbose) {

    if (point.getRowDimension() != basis.getRowDimension())
      throw new IllegalArgumentException("point.getRowDimension() != basis.getRowDimension()!");

    if (point.getColumnDimension() != 1)
      throw new IllegalArgumentException("point.getColumnDimension() != 1!");

    if (! inMinMax(point))
      throw new IllegalArgumentException("point not in min max!");

    Dependency dependency = determineDependency(point, basis);
    if (verbose) {
      System.out.println("Generated dependency");
      double[][] dependencyArray = dependency.dependency.getArray();
      for (double[] d : dependencyArray) {
        System.out.println(Util.format(d, " ", 4));
      }
    }

    Matrix b = basis.copy();
    b.normalizeCols();

    List<DoubleVector> featureVectors = new ArrayList<DoubleVector>(numberOfPoints);
    while (featureVectors.size() != numberOfPoints) {
      Matrix featureVector = generateCorrelation(point, basis);
      if (jitter) {
        featureVector = jitter(featureVector, dependency.normalVectors);
      }
      if (inMinMax(featureVector)) {
        featureVectors.add(new DoubleVector(featureVector));
        if (verbose) {
          System.out.print("\r" + featureVectors.size());
        }
      }
    }


    output(outStream, featureVectors, jitter, dependency.dependency);
  }

  private static Dependency determineDependency(final Matrix point, final Matrix basis) {
    // orthogonal basis of subvectorspace U
    Matrix orthonogonalBasis_U = orthogonalize(basis);
    Matrix completeVectors = completeBasis(orthonogonalBasis_U);

    // orthogonal basis of vectorspace V
    Matrix basis_V = appendColumn(orthonogonalBasis_U, completeVectors);
    basis_V = orthogonalize(basis_V);

    // normal vectors of U
    Matrix normalVectors_U = basis_V.getMatrix(0, basis_V.getRowDimension() - 1,
                                               basis.getColumnDimension(),
                                               basis.getRowDimension() - basis.getColumnDimension() + basis.getColumnDimension() - 1);
    Matrix transposedNormalVectors = normalVectors_U.transpose();

    // gauss jordan
    Matrix B = transposedNormalVectors.times(point);
    Matrix gaussJordan = new Matrix(transposedNormalVectors.getRowDimension(), transposedNormalVectors.getColumnDimension() + B.getColumnDimension());
    gaussJordan.setMatrix(0, transposedNormalVectors.getRowDimension() - 1, 0, transposedNormalVectors.getColumnDimension() - 1, transposedNormalVectors);
    gaussJordan.setMatrix(0, gaussJordan.getRowDimension() - 1, transposedNormalVectors.getColumnDimension(), gaussJordan.getColumnDimension() - 1, B);

    Matrix solution = gaussJordan.exactGaussJordanElimination();
    return new Dependency(normalVectors_U, solution);
  }

  private static Matrix generateCorrelation(Matrix point, Matrix basis) {
    Matrix featureVector = point.copy();
    for (int i = 0; i < basis.getColumnDimension(); i++) {
      double domain = (COEFFICIENT_MAX - COEFFICIENT_MIN) + COEFFICIENT_MIN;
      double lambda_i = RANDOM.nextDouble() * (domain * 0.5 * Math.sqrt(point.getRowDimension())) / point.getRowDimension();
      if (RANDOM.nextBoolean()) lambda_i *= -1;
      Matrix b_i = basis.getColumn(i);
      featureVector = featureVector.plus(b_i.times(lambda_i));
    }
    return featureVector;
  }

  private static Matrix jitter(Matrix featureVector, Matrix normalVectors) {
    int index = RANDOM.nextInt(normalVectors.getColumnDimension());
    Matrix normalVector = normalVectors.getColumn(index);
    double distance = RANDOM.nextGaussian() * JITTER_STANDARD_DEVIATION;

    return normalVector.times(distance).plus(featureVector);
  }

  private static boolean inMinMax(Matrix featureVector) {
    for (int i = 0; i < featureVector.getRowDimension(); i++) {
      for (int j = 0; j < featureVector.getColumnDimension(); j++) {
        double value = featureVector.get(i, j);
        if (value < MIN) return false;
        if (value > MAX) return false;
      }
    }
    return true;
  }

  private static void output(PrintStream outStream, List<DoubleVector> featureVectors, boolean jitter, Matrix dependency) {
    outStream.println("########################################################");
    if (jitter) {
      outStream.println("### Jitter standard deviation " + JITTER_STANDARD_DEVIATION);
      outStream.println("###");
    }
    double[][] dependencyArray = dependency.getArray();
    for (double[] d : dependencyArray) {
      outStream.println("### " + Util.format(d, " ", 4));
    }
    outStream.println("########################################################");


    for (DoubleVector featureVector : featureVectors) {
      outStream.println(featureVector);
    }
  }

  private static Matrix completeBasis(Matrix b) {
    Matrix e = Matrix.unitMatrix(b.getRowDimension());

    Matrix basis = b.copy();
    Matrix result = null;
    for (int i = 0; i < e.getColumnDimension(); i++) {
      Matrix e_i = e.getColumn(i);
      boolean li = basis.linearlyIndependent(e_i);
      if (li) {
        if (result == null) {
          result = e_i.copy();
        }
        else {
          result = appendColumn(result, e_i);
        }
        basis = appendColumn(basis, e_i);
      }
    }


    return result;
  }

  private static Matrix appendColumn(Matrix m, Matrix column) {
    if (m.getRowDimension() != column.getRowDimension())
      throw new IllegalArgumentException("m.getRowDimension() != column.getRowDimension()");

    Matrix result = new Matrix(m.getRowDimension(), m.getColumnDimension() + column.getColumnDimension());
    for (int i = 0; i < result.getColumnDimension(); i++) {
      if (i < m.getColumnDimension()) {
        result.setColumn(i, m.getColumn(i));
      }
      else {
        result.setColumn(i, column.getColumn(i - m.getColumnDimension()));
      }
    }
    return result;
  }

  private static Matrix orthogonalize(Matrix u) {
    Matrix v = u.getColumn(0).copy();

    for (int i = 1; i < u.getColumnDimension(); i++) {
      Matrix u_i = u.getColumn(i);
      Matrix sum = new Matrix(u.getRowDimension(), 1);
      for (int j = 0; j < i; j++) {
        Matrix v_j = v.getColumn(j);
        double scalar = u_i.scalarProduct(0, v_j, 0) / v_j.scalarProduct(0, v_j, 0);
        sum = sum.plus(v_j.times(scalar));
      }
      Matrix v_i = u_i.minus(sum);
      v = appendColumn(v, v_i);
    }

    return v;
  }

  private static Matrix centroid(int dim) {
    double[][] p = new double[dim][];
    for (int i = 0; i < p.length; i++) {
      p[i] = new double[]{(MAX - MIN) / 2};
    }
    return new Matrix(p);
  }

  private static Matrix correlationBasis(int dim, int correlationDimensionality) {
    double[][] b = new double[dim][correlationDimensionality];
    for (int i = 0; i < b.length; i++) {
      if (i < correlationDimensionality) {
        b[i][i] = 1;
      }
      else {
        for (int j = 0; j < correlationDimensionality; j++) {
          b[i][j] = RANDOM.nextInt(10);
        }
      }
    }
    return new Matrix(b);
  }

  private static class Dependency {
    Matrix normalVectors;
    Matrix dependency;

    public Dependency(Matrix normalvectors, Matrix dependency) {
      this.normalVectors = normalvectors;
      this.dependency = dependency;
    }
  }


}
