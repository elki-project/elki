package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.Hough;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.parser.ParameterizationFunctionLabelParser;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * Wrapper class for Hough algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HoughWrapper extends FileBasedDatabaseConnectionWrapper {

  /**
   * Minimum points.
   */
  private String minpts;

  /**
   * The maximum level for splitting the hypercube.
   */
  private String maxLevel;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    HoughWrapper wrapper = new HoughWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
//    catch (ParameterException e) {
//      Throwable cause = e.getCause() != null ? e.getCause() : e;
//      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
//    }
//    catch (AbortException e) {
//      wrapper.verbose(e.getMessage());
//    }
    catch (Exception e) {
      e.printStackTrace();
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameters epsilon and minpts in the parameter map additionally to the
   * parameters provided by super-classes.
   */
  public HoughWrapper() {
    super();
    // parameter min points
    optionHandler.put(Hough.MINPTS_P, new IntParameter(Hough.MINPTS_P, Hough.MINPTS_D, new GreaterConstraint(0)));

    // parameter max level
    optionHandler.put(Hough.MAXLEVEL_P, new IntParameter(Hough.MAXLEVEL_P, Hough.MAXLEVEL_D, new GreaterConstraint(0)));
  }

  /**
   * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

    // algorithm Hough
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(Hough.class.getName());

    // parser
    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.PARSER_P);
    parameters.add(ParameterizationFunctionLabelParser.class.getName());

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + Hough.MINPTS_P);
    parameters.add(minpts);

    // maxLevel
    parameters.add(OptionHandler.OPTION_PREFIX + Hough.MAXLEVEL_P);
    parameters.add(maxLevel);

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    //  minpts, maxLevel
    minpts = optionHandler.getOptionValue(Hough.MINPTS_P);
    maxLevel = optionHandler.getOptionValue(Hough.MAXLEVEL_P);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
//    AttributeSettings mySettings = settings.get(0);
    return settings;
  }

}
