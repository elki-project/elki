package de.lmu.ifi.dbs.data.synthetic;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CorrelationGenerator {
  /**
   * The logger of this class.
   */
  private static Logger logger = Logger.getLogger(CorrelationGenerator.class.getName());
//  private static boolean DEBUG = true;
  private static boolean DEBUG = false;
  private static boolean VERBOSE = true;

  public static final NumberFormat NF = NumberFormat.getInstance(Locale.US);

  private static Random RANDOM = new Random(210571);

  private static double MIN = 0;
  private static double MAX = 1;

  private static double MAX_JITTER_PCT = 0.1;
  private static double JITTER_STANDARD_DEVIATION = MAX_JITTER_PCT * (MAX - MIN) / 100;

  public static void main(String[] args) {
    LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);

    NF.setMaximumFractionDigits(4);
    NF.setMinimumFractionDigits(4);
    try {
      correlationClusterHiSC();
//      combinedHiSC();
//      geradenKDDPaper();
//      classificationKDDPaper();
//      jitterKDDPaper();
//      dimKDDPaper();
    }

    catch (Exception e) {
      e.printStackTrace();
    }
  }

  static void geradenelki() throws FileNotFoundException {
    String dir = "";
    int dim = 2;
    double maxDist = ((MAX - MIN) + MIN) * Math.sqrt(dim);
    JITTER_STANDARD_DEVIATION = MAX_JITTER_PCT * maxDist / 100;

    Matrix point = centroid(dim);
    PrintStream outStream = new PrintStream(new FileOutputStream(dir + "elki.txt"));

    {// g1
      if (VERBOSE) {
        logger.info("\ngenerate g1...\n");
      }
      double[][] b = new double[2][1];
      b[0][0] = 1;
      b[1][0] = 1;
      Matrix basis = new Matrix(b);
      generateCorrelation(500, point, basis, false, outStream);
    }
    {// g2
      if (VERBOSE) {
        logger.info("\ngenerate g2...\n");
      }
      double[][] b = new double[2][1];
      b[0][0] = -1;
      b[1][0] = 0.5;
      Matrix basis = new Matrix(b);
      generateCorrelation(500, point, basis, false, outStream);
    }

    outStream.flush();
    outStream.close();

  }

  static void combinedHiSC() throws FileNotFoundException {
    String file = "p:/nfs/infdbs/Publication/PKDD06-HiSC/data/combined.txt";
    PrintStream outStream = new PrintStream(new FileOutputStream(file));

    int dim = 3;
    double maxDist = ((MAX - MIN) + MIN) * Math.sqrt(dim);
    JITTER_STANDARD_DEVIATION = MAX_JITTER_PCT * maxDist / 100;


    {// g1
      MIN = 0;
      MAX = 0.5;
      double[][] p = new double[][]{{0.5}, {0.5}, {0.5}};
      Matrix point = new Matrix(p);
      if (VERBOSE) {
        logger.info("\ngenerate g1... " + point);
      }
      double[][] b = new double[3][1];
      b[0][0] = 0;
      b[1][0] = 1;
      b[2][0] = 0;
      Matrix basis = new Matrix(b);
      generateCorrelation(500, point, basis, true, outStream);
    }

    {// g2
      MIN = 0.5;
      MAX = 0.9;
      double[][] p = new double[][]{{0.5}, {0.6}, {0.6}};
      Matrix point = new Matrix(p);

      if (VERBOSE) {
        logger.info("\ngenerate g2... " + point);
      }
      double[][] b = new double[3][1];
      b[0][0] = 0;
      b[1][0] = 0;
      b[2][0] = 1;
      Matrix basis = new Matrix(b);
      generateCorrelation(500, point, basis, true, outStream);
    }

    { //e1
      MIN = 0;
      MAX = 0.9;
      double[][] p = new double[][]{{0.5}, {0.5}, {0.5}};
      Matrix point = new Matrix(p);

      if (VERBOSE) {
        logger.info("\ngenerate e1... " + point);
      }
      double[][] b = new double[3][2];
      b[0][0] = 0;
      b[1][0] = 1;
      b[2][0] = 0;

      b[0][1] = 0;
      b[1][1] = 0;
      b[2][1] = 1;
      Matrix basis = new Matrix(b);
      generateCorrelation(500, point, basis, true, outStream);
    }

    { //e2
      MIN = 0;
      MAX = 0.9;
      double[][] p = new double[][]{{0.5}, {0.5}, {0.5}};
      Matrix point = new Matrix(p);

      if (VERBOSE) {
        logger.info("\ngenerate e1... " + point);
      }
      double[][] b = new double[3][2];
      b[0][0] = 0;
      b[1][0] = 1;
      b[2][0] = 0;

      b[0][1] = 0;
      b[1][1] = 0;
      b[2][1] = 1;
      Matrix basis = new Matrix(b);
      generateCorrelation(500, point, basis, true, outStream);
    }

    outStream.flush();
    outStream.close();


  }

  static void geradenKDDPaper() throws FileNotFoundException {
    String dir = "p:/nfs/infdbs/WissProj/CorrelationClustering/DependencyDerivator/experiments/synthetic/geraden/";
    int dim = 3;
    double maxDist = ((MAX - MIN) + MIN) * Math.sqrt(dim);
    JITTER_STANDARD_DEVIATION = MAX_JITTER_PCT * maxDist / 100;

    Matrix point = centroid(dim);

    {// g1
      if (VERBOSE) {
        logger.info("generate g1...");
      }
      double[][] b = new double[3][1];
      b[0][0] = 1;
      b[1][0] = -0.5;
      b[2][0] = 1;
      Matrix basis = new Matrix(b);
      PrintStream outStream = new PrintStream(new FileOutputStream(dir + "g1.txt"));
      generateCorrelation(1000, point, basis, true, outStream);
      outStream.flush();
      outStream.close();
    }
    {// g2
      if (VERBOSE) {
        logger.info("generate g2...");
      }
      double[][] b = new double[3][1];
      b[0][0] = 1;
      b[1][0] = 1;
      b[2][0] = 1;
      Matrix basis = new Matrix(b);
      PrintStream outStream = new PrintStream(new FileOutputStream(dir + "g2.txt"));
      generateCorrelation(1000, point, basis, true, outStream);
      outStream.flush();
      outStream.close();
    }
    {// g3
      if (VERBOSE) {
        logger.info("generate g3...");
      }
      double[][] b = new double[3][1];
      b[0][0] = -1;
      b[1][0] = 1;
      b[2][0] = 1;
      Matrix basis = new Matrix(b);
      PrintStream outStream = new PrintStream(new FileOutputStream(dir + "g3.txt"));
      generateCorrelation(1000, point, basis, true, outStream);
      outStream.flush();
      outStream.close();
    }

    {// g4
      if (VERBOSE) {
        logger.info("generate g4...");
      }
      double[][] b = new double[3][1];
      b[0][0] = 1;
      b[1][0] = -1;
      b[2][0] = 1;
      Matrix basis = new Matrix(b);
      PrintStream outStream = new PrintStream(new FileOutputStream(dir + "g4.txt"));
      generateCorrelation(1000, point, basis, true, outStream);
      outStream.flush();
      outStream.close();
    }

    {// g5
      if (VERBOSE) {
        logger.info("generate g5...");
      }
      double[][] b = new double[3][1];
      b[0][0] = 1;
      b[1][0] = 1;
      b[2][0] = -1;
      Matrix basis = new Matrix(b);
      PrintStream outStream = new PrintStream(new FileOutputStream(dir + "g5.txt"));
      generateCorrelation(1000, point, basis, true, outStream);
      outStream.flush();
      outStream.close();
    }

  }

  static void jitterKDDPaper() throws FileNotFoundException {
    String dir = "P:/nfs/infdbs/WissProj/CorrelationClustering/DependencyDerivator/experiments/synthetic/jitter/";

    int dim = 3;
    double maxDist = ((MAX - MIN) + MIN) * Math.sqrt(dim);
    Matrix point = centroid(dim);

    double[][] b = new double[dim][2];
    b[0][0] = 1;
    b[1][0] = 1;
    b[2][0] = 1;

    b[0][1] = 1;
    b[1][1] = 2;
    b[2][1] = 0;
    Matrix basis = new Matrix(b);

    PrintStream outStream = new PrintStream(new FileOutputStream(dir + "dim_" + dim + "_jitter_" + 0 + ".txt"));
    GeneratorResult result = generateCorrelation(4000, point, basis, false, outStream);

    for (int j = 1; j <= 5; j += 1) {
      MAX_JITTER_PCT = j;
      JITTER_STANDARD_DEVIATION = MAX_JITTER_PCT * maxDist / 100;
      List<DoubleVector> doubleVectors = new ArrayList<DoubleVector>();
      for (DoubleVector doubleVector : result.doubleVectors) {
        doubleVectors.add(jitter(doubleVector, result.dependency.normalVectors));
      }

      double std = standardDeviation(doubleVectors, point, result.dependency.basisVectors);
      if (VERBOSE) {
        logger.info("standard deviation " + std);
      }
      outStream = new PrintStream(new FileOutputStream(dir + "dim_" + dim + "_jitter_" + j + ".txt"));
      output(outStream, doubleVectors, true, result.dependency.dependency, std, null);
    }
  }

  static void classificationKDDPaper() throws FileNotFoundException {
    String dir = "p:/nfs/infdbs/WissProj/CorrelationClustering/DependencyDerivator/experiments/synthetic/";

    int dim = 2;
    double maxDist = ((MAX - MIN) + MIN) * Math.sqrt(dim);
    JITTER_STANDARD_DEVIATION = MAX_JITTER_PCT * maxDist / 100;


    PrintStream outStream = new PrintStream(new FileOutputStream(dir + "geraden_100_jitter_0.75.txt", false));
    int numPoints = 100;

    {// g1
      if (VERBOSE) {
        logger.info("generate g1...");
      }
      double[][] p = new double[2][1];
      p[0][0] = 0.5;
      p[1][0] = 0.5;
      Matrix point = new Matrix(p);

      double[][] b = new double[2][1];
      b[0][0] = 1;
      b[1][0] = 1;
      Matrix basis = new Matrix(b);
      generateCorrelation(numPoints, point, basis, true, outStream, "g1");
    }
    {// g2
      if (VERBOSE) {
        logger.info("generate g2...");
      }
      double[][] p = new double[2][1];
      p[0][0] = 0.5;
      p[1][0] = 0.5;
      Matrix point = new Matrix(p);

      double[][] b = new double[2][1];
      b[0][0] = -1;
      b[1][0] = 2;
      Matrix basis = new Matrix(b);
      generateCorrelation(numPoints, point, basis, true, outStream, "g2");
    }
    {// g3
      if (VERBOSE) {
        logger.info("generate g3...");
      }
      double[][] p = new double[2][1];
      p[0][0] = 0.5;
      p[1][0] = 0.25;
      Matrix point = new Matrix(p);

      double[][] b = new double[2][1];
      b[0][0] = 1;
      b[1][0] = 2;
      Matrix basis = new Matrix(b);
      generateCorrelation(numPoints, point, basis, true, outStream, "g3");
    }

    {// g4
      if (VERBOSE) {
        logger.info("generate g4...");
      }
      double[][] p = new double[2][1];
      p[0][0] = 0.1;
      p[1][0] = 0.2;
      Matrix point = new Matrix(p);

      double[][] b = new double[2][1];
      b[0][0] = 1;
      b[1][0] = 0;
      Matrix basis = new Matrix(b);
      generateCorrelation(numPoints, point, basis, true, outStream, "g4");
    }

    {// g5
      if (VERBOSE) {
        logger.info("generate g5...");
      }
      double[][] p = new double[2][1];
      p[0][0] = 0.3;
      p[1][0] = 0;
      Matrix point = new Matrix(p);

      double[][] b = new double[2][1];
      b[0][0] = 1;
      b[1][0] = -1;
      Matrix basis = new Matrix(b);
      generateCorrelation(numPoints, point, basis, true, outStream, "g5");
      outStream.flush();
      outStream.close();
    }
  }

  static void dimKDDPaper() throws FileNotFoundException {
    String dir = "P:/nfs/infdbs/WissProj/CorrelationClustering/DependencyDerivator/experiments/synthetic/dim/";
    for (int dim = 5; dim <= 50; dim += 5) {
      double maxDist = ((MAX - MIN) + MIN) * Math.sqrt(dim);
      JITTER_STANDARD_DEVIATION = MAX_JITTER_PCT * maxDist / 100;

      int corrDim = RANDOM.nextInt(dim - 1) + 1;
      Matrix point = centroid(dim);
      Matrix basis = correlationBasis(dim, corrDim);
      boolean jitter = true;
      PrintStream outStream = new PrintStream(new FileOutputStream(dir + "dim_" + dim + "_" + corrDim + "c.txt"));
      generateCorrelation(1000, point, basis, jitter, outStream);
      outStream.flush();
      outStream.close();
    }
  }

  static GeneratorResult generateCorrelation(int numberOfPoints, final Matrix point, final Matrix basis,
                                             boolean jitter, PrintStream outStream) {
    return generateCorrelation(numberOfPoints, point, basis, jitter, outStream, null);
  }

  static GeneratorResult generateCorrelation(int numberOfPoints, final Matrix point, final Matrix basis,
                                             boolean jitter, PrintStream outStream, String label) {

    if (point.getRowDimension() != basis.getRowDimension())
      throw new IllegalArgumentException("point.getRowDimension() != basis.getRowDimension()!");

    if (point.getColumnDimension() != 1)
      throw new IllegalArgumentException("point.getColumnDimension() != 1!");

    if (! inMinMax(point))
      throw new IllegalArgumentException("point not in min max!");

    Dependency dependency = determineDependency(point, basis);
    if (VERBOSE) {
      StringBuffer msg = new StringBuffer();
      msg.append(dependency.toString());
      logger.info(msg.toString());
    }

    Matrix b = dependency.basisVectors;

    List<DoubleVector> featureVectors = new ArrayList<DoubleVector>(numberOfPoints);
    while (featureVectors.size() != numberOfPoints) {
      Matrix featureVector = generateCorrelation(point, b);
      double distance = distance(featureVector, point, b);
      if (distance > 1E-13 && VERBOSE)
        logger.info("distance " + distance);
      if (jitter) {
        featureVector = jitter(featureVector, dependency.normalVectors);
      }
      if (inMinMax(featureVector)) {
        featureVectors.add(new DoubleVector(featureVector));
      }
    }

    double std = standardDeviation(featureVectors, point, b);
    if (VERBOSE) {
      logger.info("standard deviation " + std + "\n");
    }
    output(outStream, featureVectors, jitter, dependency.dependency, std, label);

    return new GeneratorResult(featureVectors, dependency);
  }

  static Dependency determineDependency(final Matrix point, final Matrix basis) {
    StringBuffer msg = new StringBuffer();

    // orthonormal basis of subvectorspace U
    Matrix orthonormalBasis_U = orthonormalize(basis);
    Matrix completeVectors = completeBasis(orthonormalBasis_U);
    if (DEBUG) {
      msg.append("\npoint ").append(point.toString(NF));
      msg.append("\nbasis ").append(basis.toString(NF));
      msg.append("\northonormal basis ").append(orthonormalBasis_U.toString(NF));
      msg.append("\ncomplete vectors ").append(completeVectors.toString(NF));
      logger.fine(msg.toString());
    }

    // orthonormal basis of vectorspace V
    Matrix basis_V = appendColumn(orthonormalBasis_U, completeVectors);
    basis_V = orthonormalize(basis_V);
    if (DEBUG) {
      logger.fine("basis V " + basis_V.toString(NF));
    }

    // normal vectors of U
    Matrix normalVectors_U = basis_V.getMatrix(0, basis_V.getRowDimension() - 1,
                                               basis.getColumnDimension(),
                                               basis.getRowDimension() - basis.getColumnDimension() + basis.getColumnDimension() - 1);
    if (DEBUG) {
      logger.fine("normal vector U " + normalVectors_U.toString(NF));
    }
    Matrix transposedNormalVectors = normalVectors_U.transpose();
    if (DEBUG) {
      logger.fine("tNV " + transposedNormalVectors.toString(NF));
      logger.fine("point " + point.toString(NF));
    }

    // gauss jordan
    Matrix B = transposedNormalVectors.times(point);
    if (DEBUG) {
      logger.fine("B " + B.toString(NF));
    }
    Matrix gaussJordan = new Matrix(transposedNormalVectors.getRowDimension(), transposedNormalVectors.getColumnDimension() + B.getColumnDimension());
    gaussJordan.setMatrix(0, transposedNormalVectors.getRowDimension() - 1, 0, transposedNormalVectors.getColumnDimension() - 1, transposedNormalVectors);
    gaussJordan.setMatrix(0, gaussJordan.getRowDimension() - 1, transposedNormalVectors.getColumnDimension(), gaussJordan.getColumnDimension() - 1, B);


    double[][] a = new double[transposedNormalVectors.getRowDimension()][transposedNormalVectors.getColumnDimension()];
    double[][] we = transposedNormalVectors.getArray();
    double[] b = B.getColumn(0).getRowPackedCopy();
    System.arraycopy(we, 0, a, 0, transposedNormalVectors.getRowDimension());

//    System.out.println("a "+new Matrix(a).toString(NF));
//    System.out.println("b "+Util.format(b, ",", 4));

    LinearEquationSystem lq = new LinearEquationSystem(a, b);
    lq.solveByTotalPivotSearch();
    Dependency dependency = new Dependency(orthonormalBasis_U, normalVectors_U, lq);
//    System.out.println("solution " + lq.equationsToString(NF.getMinimumFractionDigits(), NF.getMaximumFractionDigits()));
//    System.out.println("dep " + dependency);
    return dependency;
  }

  static Matrix generateCorrelation(Matrix point, Matrix basis) {
    Matrix featureVector = point.copy();
    for (int i = 0; i < basis.getColumnDimension(); i++) {
//      System.out.println("   d " + distance(featureVector, point, basis));
//      double lambda_i = RANDOM.nextDouble() * (0.5 * Math.sqrt(point.getRowDimension())) / point.getRowDimension();
//      double lambda_i = RANDOM.nextDouble();
      double lambda_i = RANDOM.nextGaussian();
      if (RANDOM.nextBoolean()) lambda_i *= -1;
      Matrix b_i = basis.getColumn(i);
      featureVector = featureVector.plus(b_i.times(lambda_i));

    }
    return featureVector;
  }

  static DoubleVector jitter(DoubleVector doubleVector, Matrix normalVectors) {
    Matrix m = jitter(doubleVector.getColumnVector(), normalVectors);
    return new DoubleVector(m);
  }

  static Matrix jitter(Matrix featureVector, Matrix normalVectors) {
    for (int i = 0; i < normalVectors.getColumnDimension(); i++) {
      Matrix n_i = normalVectors.getColumn(i);
      n_i.normalizeCols();
      double distance = RANDOM.nextGaussian() * JITTER_STANDARD_DEVIATION;
      featureVector = n_i.times(distance).plus(featureVector);
    }
    return featureVector;

//    int index = RANDOM.nextInt(normalVectors.getColumnDimension());
//    Matrix normalVector = normalVectors.getColumn(index);
//    double distance = RANDOM.nextGaussian() * JITTER_STANDARD_DEVIATION;
//    return normalVector.times(distance).plus(featureVector);

//    for (int i = 0; i < featureVector.getRowDimension(); i++) {
//      double j = (RANDOM.nextDouble() * 2 - 1) * (MAX_JITTER_PCT * (MAX - MIN) / 100.0);
//      featureVector.set(i, 0, featureVector.get(i, 0) + j);
//    }
//    return featureVector;
  }

  public static boolean inMinMax(Matrix featureVector) {
    for (int i = 0; i < featureVector.getRowDimension(); i++) {
      for (int j = 0; j < featureVector.getColumnDimension(); j++) {
        double value = featureVector.get(i, j);
        if (value < MIN) return false;
        if (value > MAX) return false;
      }
    }
    return true;
  }

  static void output(PrintStream outStream, List<DoubleVector> featureVectors, boolean jitter, LinearEquationSystem dependency, double std, String label) {
    outStream.println("########################################################");
    if (jitter) {
      outStream.println("### max Jitter " + MAX_JITTER_PCT + "%");
      outStream.println("### Randomized standard deviation " + JITTER_STANDARD_DEVIATION);
      outStream.println("### Real       standard deviation " + std);
      outStream.println("###");
    }

    outStream.print(dependency.equationsToString("### ", 4));
    outStream.println("########################################################");


    for (DoubleVector featureVector : featureVectors) {
      if (label == null)
        outStream.println(featureVector);
      else {
        outStream.print(featureVector);
        outStream.println(" " + label);
      }
    }
  }

  static Matrix completeBasis(Matrix b) {
    StringBuffer msg = new StringBuffer();

    Matrix e = Matrix.unitMatrix(b.getRowDimension());
    Matrix basis = b.copy();
    Matrix result = null;
    for (int i = 0; i < e.getColumnDimension(); i++) {
      Matrix e_i = e.getColumn(i);
      boolean li = basis.linearlyIndependent(e_i);

      if (DEBUG) {
        msg.append("\nbasis ").append(basis.toString(NF));
        msg.append("\ne_i ").append(e_i.toString(NF));
        msg.append("\nlinearlyIndependent ").append(li);
        logger.fine(msg.toString());
      }

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

  static Matrix appendColumn(Matrix m, Matrix column) {
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

  static Matrix orthonormalize(Matrix u) {
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

    v.normalizeCols();
    return v;
  }

  static Matrix centroid(int dim) {
    double[][] p = new double[dim][];
    for (int i = 0; i < p.length; i++) {
      p[i] = new double[]{(MAX - MIN) / 2};
    }
    return new Matrix(p);
  }

  static Matrix random(int dim) {
    double[][] p = new double[dim][];
    for (int i = 0; i < p.length; i++) {
      p[i] = new double[]{RANDOM.nextDouble() * (MAX - MIN) + MIN};
    }
    return new Matrix(p);
  }

  static Matrix correlationBasis(int dim, int correlationDimensionality) {
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

  static double standardDeviation(List<DoubleVector> featureVectors, Matrix point, Matrix basis) {
    double std_2 = 0;
    for (DoubleVector doubleVector : featureVectors) {
      double distance = distance(doubleVector.getColumnVector(), point, basis);
      std_2 += distance * distance;
    }
    return Math.sqrt(std_2 / featureVectors.size());
  }

  static double distance(Matrix p, Matrix point, Matrix basis) {
    Matrix p_minus_a = p.minus(point);
    Matrix proj = p_minus_a.projection(basis);
    return p_minus_a.minus(proj).euclideanNorm(0);
  }

  private static void generateAxesParallelDependency(int noPoints,
                                                     int corrDim,
                                                     int dataDim,
                                                     String label,
                                                     double[] min,
                                                     double[] max,
                                                     double maxJitter,
                                                     OutputStreamWriter out) throws IOException {

    if (corrDim > dataDim)
      throw new IllegalArgumentException("corrDim > dataDim");

    // randomize the dependent values
    Set<Integer> dependentValuesIndex = new HashSet<Integer>();
    while (dependentValuesIndex.size() != dataDim - corrDim) {
      dependentValuesIndex.add(RANDOM.nextInt(dataDim));
    }
    Double[] dependentValues = new Double[dataDim - corrDim];
    for (int d = 0; d < dataDim - corrDim; d++) {
      dependentValues[d] = RANDOM.nextDouble();
    }

    generateAxesParallelDependency(noPoints, dependentValuesIndex, dependentValues, corrDim, dataDim, label, min, max, maxJitter, out);
  }

  private static void generateAxesParallelDependency(int noPoints,
                                                     Set<Integer> dependentValuesIndex,
                                                     Double[] dependentValues,
                                                     int corrDim,
                                                     int dataDim,
                                                     String label,
                                                     double[] min,
                                                      double[] max,
                                                     double maxJitter,
                                                     OutputStreamWriter out) throws IOException {
    if (corrDim > dataDim)
      throw new IllegalArgumentException("corrDim > dataDim");

    if (dependentValuesIndex.size() != dataDim - corrDim)
      throw new IllegalArgumentException("dependentValuesIndex.size() != dataDim - corrDim");

    if (dependentValues.length != dataDim - corrDim)
      throw new IllegalArgumentException("dependentValues.length != dataDim - corrDim");

    out.write("########################################################\n");
    out.write("### corrDim " + corrDim + "\n");
    out.write("########################################################\n");

    // generate the feature vectors
    double[][] featureVectors = new double[noPoints][dataDim];
    for (int n = 0; n < noPoints; n++) {
      int dependentIndex = 0;
      for (int d = 0; d < dataDim; d++) {
        if (dependentValuesIndex.contains(d)) {
          featureVectors[n][d] = dependentValues[dependentIndex++];
        }
        else {
          featureVectors[n][d] = RANDOM.nextDouble();
        }
      }
    }

    // add start-value and add jitter
    for (int n = 0; n < noPoints; n++) {
      for (int d = 0; d < dataDim; d++) {
//        double jitter = RANDOM.nextDouble() * maxJitter;
//        if (RANDOM.nextBoolean()) jitter += -1;
//        featureVectors[n][d] = featureVectors[n][d] + start + jitter;
        featureVectors[n][d] = featureVectors[n][d]  * (max[d] - min[d]) + min[d];
      }
    }

    // write to output
    for (int n = 0; n < noPoints; n++) {
      for (int d = 0; d < dataDim; d++) {
        out.write(featureVectors[n][d] + " ");
      }
      out.write(label + "\n");
    }
  }

  private static void correlationClusterHiSC() {
    try {
      int size = 2000;
      int minDataDim = 3;
      int dataDimIncrement = 1;
      int minCorrDim = 1;
      int corrDimIncrement = 1;
      int steps = 1;
      double jitter = 0;

      {
        String file = "p:/nfs/infdbs/Publication/PKDD06-HiSC/data/combined_dim3";
        File output = new File(file);
        output.getParentFile().mkdirs();
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(output));

        // randomize the dependent values
        Set<Integer> dependentValuesIndex_g1 = new HashSet<Integer>();
        dependentValuesIndex_g1.add(0);
        dependentValuesIndex_g1.add(1);
        Double[] dependentValues_g1 = new Double[]{0.5,0.5};
        double[] min_g1 = new double[]{0,0,0};
        double[] max_g1 = new double[]{1,1,1};

        Set<Integer> dependentValuesIndex_g2 = new HashSet<Integer>();
        dependentValuesIndex_g2.add(0);
        dependentValuesIndex_g2.add(2);
        Double[] dependentValues_g2 = new Double[]{0.5,0.25};
        double[] min_g2 = new double[]{0,1,0};
        double[] max_g2 = new double[]{1,2,1};

        Set<Integer> dependentValuesIndex_e1 = new HashSet<Integer>();
        dependentValuesIndex_e1.add(0);
        Double[] dependentValues_e1 = new Double[]{0.5};
        double[] min_e1 = new double[]{0,0,0};
        double[] max_e1 = new double[]{1,2,1};

        Set<Integer> dependentValuesIndex_e2 = new HashSet<Integer>();
        dependentValuesIndex_e2.add(1);
        Double[] dependentValues_e2 = new Double[]{-0.5};
        double[] min_e2 = new double[]{0,0,0};
        double[] max_e2 = new double[]{1,1,1};


        generateAxesParallelDependency(500, dependentValuesIndex_g1, dependentValues_g1, 1, minDataDim, "g1", min_g1, max_g1, jitter, out);
        generateAxesParallelDependency(500, dependentValuesIndex_g2, dependentValues_g2, 1, minDataDim, "g2", min_g2, max_g2, jitter, out);
        generateAxesParallelDependency(500, dependentValuesIndex_e1, dependentValues_e1, 2, minDataDim, "e1", min_e1, max_e1, jitter, out);
        generateAxesParallelDependency(500, dependentValuesIndex_e2, dependentValues_e2, 2, minDataDim, "e2", min_e2, max_e2, jitter, out);

        out.flush();
        out.close();
        if (true) return;
      }


      for (int i = 0; i < steps; i++) {
        int dataDim = minDataDim + dataDimIncrement * i;
        System.out.println("dim " + minDataDim);

//        String file = "p:/nfs/infdbs/Publication/PKDD06-HiSC/data/dim" + dataDim;
        String file = "p:/nfs/infdbs/Publication/PKDD06-HiSC/data/combined_dim" + dataDim;
        File output = new File(file);
        output.getParentFile().mkdirs();
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(output));

        int sizePerPartition = size / (minDataDim) * corrDimIncrement;

        for (int d = minCorrDim; d <= minDataDim; d += corrDimIncrement) {
          if (d == minDataDim - 1) {
            sizePerPartition = sizePerPartition + size % (minDataDim - 1);
          }
//          generateAxesParallelDependency(sizePerPartition, d, minDataDim, "d" + d, d * 2, jitter, out);
//          generateAxesParallelDependency(sizePerPartition, d, minDataDim, "d" + d, 0, jitter, out);
        }

        out.flush();
        out.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static class Dependency {
    Matrix basisVectors;
    Matrix normalVectors;
    LinearEquationSystem dependency;

    public Dependency(Matrix basisVectors, Matrix normalvectors, LinearEquationSystem linearEquationSystem) {
      this.basisVectors = basisVectors;
      this.normalVectors = normalvectors;
      this.dependency = linearEquationSystem;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
      return
//      "basisVectors : " + basisVectors.toString(NF) +
//      "normalVectors: " + normalVectors.toString(NF) +
      "dependency: " + dependency.equationsToString(NF.getMaximumFractionDigits());
    }
  }

  private static class GeneratorResult {
    List<DoubleVector> doubleVectors;
    Dependency dependency;

    public GeneratorResult(List<DoubleVector> doubleVectors, Dependency dependency) {
      this.doubleVectors = doubleVectors;
      this.dependency = dependency;
    }
  }
}
