package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.AbortException;
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
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for multi represented OPTICS algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MROPTICSWrapper extends AbstractAlgorithmWrapper {
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
  public String[] setParameters(String[] args) {
    String[] remainingParameters = super.setParameters(args);
    epsilon = optionHandler.getOptionValue(EPSILON_P);
    try {
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
    }
    catch (NumberFormatException e) {
      WrongParameterValueException pe = new WrongParameterValueException(MINPTS_P, optionHandler.getOptionValue(MINPTS_P));
      pe.fillInStackTrace();
      throw pe;
    }
    return remainingParameters;
  }

  /**
   * @see AbstractAlgorithmWrapper#initParameters(java.util.List<java.lang.String>)
   */
  public List<String> initParameters(List<String> remainingParameters) {
    // text, f1, f2, f3, f5, f9, colorhisto, colormoments
//    String ct = "U(R:1:"+ CosineDistanceFunction.class.getName()+",I(I(I(I(I(I(R:2,R:3),R:4),R:5),R:6),R:7),R:8))";
    ArrayList<String> parameters = new ArrayList<String>(remainingParameters);

    // algorithm OPTICS
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(OPTICS.class.getName());

    // epsilon
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.EPSILON_P);
    parameters.add(epsilon);

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + OPTICS.MINPTS_P);
    parameters.add(Integer.toString(minpts));

    // distance function
//    params.add(OptionHandler.OPTION_PREFIX + OPTICS.DISTANCE_FUNCTION_P);
//    params.add(CombinationTree.class.getName());

    // combination tree
//    params.add(OptionHandler.OPTION_PREFIX + CombinationTree.TREE_P);
//    params.add(ct);

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(MultiRepresentedObjectNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // database connection
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.DATABASE_CONNECTION_P);
    parameters.add(MultipleFileBasedDatabaseConnection.class.getName());

    // database
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
    parameters.add(MTreeDatabase.class.getName());

    // distance function for db
//    params.add(OptionHandler.OPTION_PREFIX + MTreeDatabase.DISTANCE_FUNCTION_P);
//    params.add(CombinationTree.class.getName());

    // combination tree
//    params.add(OptionHandler.OPTION_PREFIX + CombinationTree.TREE_P);
//    params.add(ct);

    // distance cache
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabase.CACHE_F);

    // bulk load
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.BULK_LOAD_F);

    // page size
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.PAGE_SIZE_P);
    parameters.add("4000");

    // cache size
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.CACHE_SIZE_P);
    parameters.add("120000");

    // input
    parameters.add(OptionHandler.OPTION_PREFIX + MultipleFileBasedDatabaseConnection.INPUT_P);
    parameters.add(input);

    // parsers
    parameters.add(OptionHandler.OPTION_PREFIX + MultipleFileBasedDatabaseConnection.PARSER_P);
    parameters.add(SparseBitVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + "," +
               RealVectorLabelParser.class.getName() + ",");

    // output
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    parameters.add(output);

    if (time) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      parameters.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    return parameters;
  }

  public static void main(String[] args) {
    MROPTICSWrapper wrapper = new MROPTICSWrapper();
    try {
      wrapper.run(args);
    }
    catch (WrongParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (NoParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (UnusedParameterException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (AbortException e) {
      System.err.println(e.getMessage());
    }
    catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
    }
    catch (IllegalStateException e) {
      System.err.println(e.getMessage());
    }
  }
}
