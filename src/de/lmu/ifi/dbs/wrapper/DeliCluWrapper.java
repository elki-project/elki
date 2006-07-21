package de.lmu.ifi.dbs.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.DeLiClu;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.index.Index;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.deliclu.DeLiCluTree;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Wrapper class for the DeliClu algorithm.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeliCluWrapper extends NormalizationWrapper {

  /**
   * The value of the minpts parameter.
   */
  private String minpts;

  /**
   * The value of the pageSize parameter.
   */
  private String pageSize;

  /**
   * The value of the cacheSize parameter.
   */
  private String cacheSize;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    DeliCluWrapper wrapper = new DeliCluWrapper();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
    	wrapper.verbose(e.getMessage());
    }
    catch (Exception e) {
      e.printStackTrace();
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter minpts, pagesize and cachesize in the parameter map
   * additionally to the parameters provided by super-classes.
   */
  public DeliCluWrapper() {
    super();
    optionHandler.put(DeLiClu.MINPTS_P, new Parameter(DeLiClu.MINPTS_P,DeLiClu.MINPTS_D));
    optionHandler.put(Index.PAGE_SIZE_P, new Parameter(Index.PAGE_SIZE_P,Index.PAGE_SIZE_D));
    optionHandler.put(Index.CACHE_SIZE_P, new Parameter(Index.CACHE_SIZE_P,Index.CACHE_SIZE_D));
  }

  /**
   * @see KDDTaskWrapper#getKDDTaskParameters()
   */
  public List<String> getKDDTaskParameters() {
    List<String> parameters = super.getKDDTaskParameters();

    // deliclu algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(DeLiClu.class.getName());

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + DeLiClu.MINPTS_P);
    parameters.add(minpts);

    // database
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
    parameters.add(SpatialIndexDatabase.class.getName());

    // index
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.INDEX_P);
    parameters.add(DeLiCluTree.class.getName());

    // bulk load
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndex.BULK_LOAD_F);

    // page size
    parameters.add(OptionHandler.OPTION_PREFIX + Index.PAGE_SIZE_P);
    parameters.add(pageSize);

    // cache size
    parameters.add(OptionHandler.OPTION_PREFIX + Index.CACHE_SIZE_P);
    parameters.add(cacheSize);

    return parameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // minpts
    minpts = optionHandler.getOptionValue(DeLiClu.MINPTS_P);
    // pagesize
    if (optionHandler.isSet(Index.PAGE_SIZE_P)) {
      pageSize = optionHandler.getOptionValue(Index.PAGE_SIZE_P);
    }
    else {
      pageSize = Integer.toString(Index.DEFAULT_PAGE_SIZE);
    }
    // cachesize
    if (optionHandler.isSet(Index.CACHE_SIZE_P)) {
      cacheSize = optionHandler.getOptionValue(Index.CACHE_SIZE_P);
    }
    else {
      cacheSize = Integer.toString(Index.DEFAULT_CACHE_SIZE);
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(DeLiClu.MINPTS_P, minpts);
    mySettings.addSetting(Index.PAGE_SIZE_P, pageSize);
    mySettings.addSetting(Index.CACHE_SIZE_P, cacheSize);
    return settings;
  }
}
