package de.lmu.ifi.dbs.algorithm.clustering.clique;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Implementation of the CLIQUE algorithm, a grid-based algorithm to identify dense clusters
 * in subspaces of maximum dimensionality.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CLIQUE<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> implements Clustering<V> {

  /**
   * Parameter xsi.
   */
  public static final String XSI_P = "xsi";

  /**
   * Description for parameter xsi.
   */
  public static final String XSI_D = "number of intervals (units) in each dimension";

  /**
   * Parameter tau.
   */
  public static final String TAU_P = "tau";

  /**
   * Description for parameter tau.
   */
  public static final String TAU_D = "threshold for the selectivity of a unit, where the selectivity is" +
                                     "the fraction of total data points contained in this unit";

  /**
   * Flag prune.
   */
  public static final String PRUNE_F = "prune";

  /**
   * Description for flag prune.
   */
  public static final String PRUNE_D = "flag indicating that only subspaces with large coverage " +
                                       "(i.e. the fraction of the database that is covered by the dense units) " +
                                       "are selected, the rest will be pruned, default: no pruning";


  /**
   * Number of units in each dimension.
   */
  private int xsi;

  /**
   * Threshold for the selectivity of a unit.
   */
  private double tau;

  /**
   * Flag indicating that subspaces with low coverage are pruned.
   */
  private boolean prune;

  /**
   * The result of the algorithm;
   */
  private Clusters<V> result;


  public CLIQUE() {
    super();
    this.debug = true;

    //parameter xsi
    optionHandler.put(new IntParameter(XSI_P, XSI_D, new GreaterConstraint(0)));

    //parameter tau
    List<ParameterConstraint<Number>> tauConstraints = new ArrayList<ParameterConstraint<Number>>();
    tauConstraints.add(new GreaterConstraint(0));
    tauConstraints.add(new LessConstraint(1));
    optionHandler.put(new DoubleParameter(TAU_P, TAU_D, tauConstraints));

    //flag prune
    optionHandler.put(new Flag(PRUNE_F, PRUNE_D));
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public ClusteringResult<V> getResult() {
    return result;
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description("CLIQUE",
                           "Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications",
                           "Grid-based algorithm to identify dense clusters in subspaces of maximum dimensionality. ",
                           "R. Agrawal, J. Gehrke, D. Gunopulos, P. Raghavan: " +
                           "Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications. " +
                           "In Proc. SIGMOD Conference, Seattle, WA, 1998.");
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized
   *                               properly (e.g. the setParameters(String[]) method has been failed
   *                               to be called).
   *                               // todo
   */
  protected void runInTime(Database<V> database) throws IllegalStateException {
    // 1. Identification of subspaces that contain clusters
    if (isVerbose()) {
      verbose("1. Identification of subspaces that contain clusters");
    }
    initUnits(database);

    // 2. Identification of clusters
    // 3. Generation of minimal decription for the clusters

    Integer[][] clusters = new Integer[0][0];
    result = new Clusters<V>(clusters, database);
  }

  /**
   * Sets the parameters xsi, tau and prune additionally to the parameters set
   * by the super-class' method.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    xsi = (Integer) optionHandler.getOptionValue(XSI_P);
    tau = (Double) optionHandler.getOptionValue(TAU_P);
    prune = optionHandler.isSet(PRUNE_F);

    return remainingParameters;
  }

  /**
   * Initializes the units.
   *
   * @return
   */
  private void initUnits(Database<V> database) {
    int dimensionality = database.dimensionality();
    // initialize minima and maxima
    double[] minima = new double[dimensionality];
    double[] maxima = new double[dimensionality];
    for (int i = 0; i < dimensionality; i++) {
      maxima[i] = -Double.MAX_VALUE;
      minima[i] = Double.MAX_VALUE;
    }
    // update minima and maxima
    for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
      V featureVector = database.get(it.next());
      updateMinMax(featureVector, minima, maxima);
    }
    // determine the unit length in each dimension
    double[] unitLengths = new double[dimensionality];
    for (int dim = 0; dim < dimensionality; dim++) {
      unitLengths[dim] = (maxima[dim] - minima[dim]) / xsi;
    }

    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   minima: ").append(Util.format(minima, ", ", 2));
      msg.append("\n   maxima: ").append(Util.format(maxima, ", ", 2));
      msg.append("\n   unitLengths: ").append(Util.format(unitLengths, ", ", 2));
      debugFine(msg.toString());
    }

    // build the hyper points of the units
    double[][] point_coordinates = new double[xsi][dimensionality];
    for (int x = 0; x < xsi; x++) {
      for (int dim = 0; dim < dimensionality; dim++) {
        point_coordinates[x][dim] = minima[dim] + x * unitLengths[dim];
      }
    }
    if (debug) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n   point_coordinates: ").append(new Matrix(point_coordinates).toString("   "));
      debugFine(msg.toString());
    }

    // build the units
    for (int x = 0; x < xsi; x++) {
      for (int dim = 0; dim < dimensionality-1; dim++) {
        double[] fix_point_coordinates = new double[dimensionality - 1];
        for (int j = 0; j < fix_point_coordinates.length; j++) {
          fix_point_coordinates[j] = point_coordinates[x][dim];
        }
        buildUnit(fix_point_coordinates, point_coordinates, unitLengths);

        //CLIQUEUnit unit = new CLIQUEUnit(unit_minima[x][dim], unit_maxima[x][dim]);
      }
    }


  }

  private void buildUnit(double[] fix_point_coordinates, double[][] point_coordinates, double[] unitLengths) {
    int dim = point_coordinates[0].length;
    double[] min = new double[dim];
    double[] max = new double[dim];

    for (int i = 0; i < dim - 1; i++) {
      min[i] = fix_point_coordinates[i];
      max[i] = fix_point_coordinates[i] + unitLengths[i];
    }

    for (int x = 0; x < xsi; x++) {
      min[dim - 1] = point_coordinates[x][dim - 1];
      max[dim - 1] = min[dim - 1] + unitLengths[dim - 1];

      if (debug) {
        StringBuffer msg = new StringBuffer();
        msg.append("\n   fix_point_coordinates: ").append(Util.format(fix_point_coordinates, ", ", 2));
        msg.append("\n   min: ").append(Util.format(min, ", ", 2));
        msg.append("\n   max: ").append(Util.format(max, ", ", 2));
        debugFine(msg.toString());
      }
    }
  }


  /**
   * Updates the minima and maxima array according to the specified feature vector.
   *
   * @param featureVector the feature vector
   * @param minima        the array of minima
   * @param maxima        the array of maxima
   */
  private void updateMinMax(V featureVector, double[] minima, double[] maxima) {
    if (minima.length != featureVector.getDimensionality()) {
      throw new IllegalArgumentException("FeatureVectors differ in length.");
    }
    for (int d = 1; d <= featureVector.getDimensionality(); d++) {
      if ((featureVector.getValue(d).doubleValue()) > maxima[d - 1]) {
        maxima[d - 1] = (featureVector.getValue(d).doubleValue());
      }
      if ((featureVector.getValue(d).doubleValue()) < minima[d - 1]) {
        minima[d - 1] = (featureVector.getValue(d).doubleValue());
      }
    }
  }


}
