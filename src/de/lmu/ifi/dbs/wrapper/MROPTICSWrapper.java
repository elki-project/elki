package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.database.AbstractDatabase;
import de.lmu.ifi.dbs.database.MTreeDatabase;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.database.connection.MultipleFileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.normalization.MultiRepresentedObjectNormalization;
import de.lmu.ifi.dbs.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.parser.SparseBitVectorLabelParser;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;

/**
 * Wrapper class for multi represented OPTICS algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MROPTICSWrapper extends AbstractWrapper {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the distance function " + LocallyWeightedDistanceFunction.class.getName();

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Minimum points.
   */
  protected int minpts;

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since DBSCANWrapper is a non-abstract class,
   * finally optionHandler is initialized.
   */
  public MROPTICSWrapper() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Sets the parameters epsilon and minpts additionally to the parameters set
   * by the super-class' method. Both epsilon and minpts are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    super.setParameters(args);
    try {
      epsilon = optionHandler.getOptionValue(EPSILON_P);
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return new String[0];
  }

  /**
   * Runs the OPTICS algorithm.
   */
  public void runOPTICS() {
    // text, f1, f2, f3, f5, f9, colorhisto, colormoments
//    String ct = "U(R:1:"+ CosineDistanceFunction.class.getName()+",I(I(I(I(I(I(R:2,R:3),R:4),R:5),R:6),R:7),R:8))";
    ArrayList<String> params = getRemainingParameters();

    // algorithm OPTICS
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(OPTICS.class.getName());

    // epsilon
    params.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    params.add(epsilon);

    // minpts
    params.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    params.add(Integer.toString(minpts));

    // distance function
//    params.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
//    params.add(CombinationTree.class.getName());

    // combination tree
//    params.add(OptionHandler.OPTION_PREFIX + CombinationTree.TREE_P);
//    params.add(ct);

    // normalization
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    params.add(MultiRepresentedObjectNormalization.class.getName());
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // database connection
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.DATABASE_CONNECTION_P);
    params.add(MultipleFileBasedDatabaseConnection.class.getName());

    // database
    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
    params.add(MTreeDatabase.class.getName());

    // distance function for db
//    params.add(OptionHandler.OPTION_PREFIX + MTreeDatabase.DISTANCE_FUNCTION_P);
//    params.add(CombinationTree.class.getName());

    // combination tree
//    params.add(OptionHandler.OPTION_PREFIX + CombinationTree.TREE_P);
//    params.add(ct);

    // distance cache
    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabase.CACHE_F);

    // bulk load
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.BULK_LOAD_F);

    // page size
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.PAGE_SIZE_P);
    params.add("4000");

    // cache size
    params.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.CACHE_SIZE_P);
    params.add("120000");

    // input
    params.add(OptionHandler.OPTION_PREFIX + MultipleFileBasedDatabaseConnection.INPUT_P);
    params.add(input);

    // parsers
    params.add(OptionHandler.OPTION_PREFIX + MultipleFileBasedDatabaseConnection.PARSER_P);
    params.add(SparseBitVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + ",");

    // output
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }

  /**
   * Runs the COPAC algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public void run(String[] args) {
    this.setParameters(args);
    this.runOPTICS();
  }

  public static void main(String[] args) {
    MROPTICSWrapper optics = new MROPTICSWrapper();
    try {
      optics.run(args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
