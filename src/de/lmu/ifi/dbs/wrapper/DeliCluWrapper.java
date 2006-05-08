package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.DeLiClu;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.index.Index;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.rstar.deliclu.DeLiCluTree;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Wrapper class for the DeliClu algorithm.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeliCluWrapper extends FileBasedDatabaseConnectionWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    DeliCluWrapper wrapper = new DeliCluWrapper();
    try {
      wrapper.run(args);
    }
    catch (ParameterException e) {
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the parameter minpts, pagesize and cachesize in the parameter map
   * additionally to the parameters provided by super-classes.
   */
  public DeliCluWrapper() {
    super();
    parameterToDescription.put(DeLiClu.MINPTS_P + OptionHandler.EXPECTS_VALUE, DeLiClu.MINPTS_D);
    parameterToDescription.put(Index.PAGE_SIZE_P + OptionHandler.EXPECTS_VALUE, Index.PAGE_SIZE_D);
    parameterToDescription.put(Index.CACHE_SIZE_P + OptionHandler.EXPECTS_VALUE, Index.CACHE_SIZE_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws ParameterException {
    List<String> parameters = super.getParameters();

    // deliclu algorithm
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(DeLiClu.class.getName());

    // minpts
    parameters.add(OptionHandler.OPTION_PREFIX + DeLiClu.MINPTS_P);
    parameters.add(optionHandler.getOptionValue(DeLiClu.MINPTS_P));

    // normalization
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // database
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
    parameters.add(SpatialIndexDatabase.class.getName());

    // index
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.INDEX_P);
    parameters.add(DeLiCluTree.class.getName());

    // bulk load
    parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndex.BULK_LOAD_F);

    // page size
    if (optionHandler.isSet(Index.PAGE_SIZE_P)) {
      parameters.add(OptionHandler.OPTION_PREFIX + Index.PAGE_SIZE_P);
      parameters.add(optionHandler.getOptionValue(Index.PAGE_SIZE_P));
    }

    // cache size
    if (optionHandler.isSet(Index.CACHE_SIZE_P)) {
      parameters.add(OptionHandler.OPTION_PREFIX + Index.CACHE_SIZE_P);
      parameters.add(optionHandler.getOptionValue(Index.CACHE_SIZE_P));
    }

    return parameters;
  }
}
