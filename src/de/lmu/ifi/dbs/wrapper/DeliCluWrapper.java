package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.DeLiClu;
import de.lmu.ifi.dbs.algorithm.clustering.SUBCLU;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.index.tree.TreeIndex;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.tree.spatial.rstarvariants.deliclu.DeLiCluTree;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.List;

/**
 * Wrapper class for the DeliClu algorithm.
 *
 * @author Elke Achtert
 */
public class DeliCluWrapper extends NormalizationWrapper {

    /**
     * The value of the minpts parameter.
     */
    private int minpts;

    /**
     * The value of the pageSize parameter.
     */
    private int pageSize;

    /**
     * The value of the cacheSize parameter.
     */
    private int cacheSize;

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
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
        }
    }

    /**
     * Sets the parameter minpts, pagesize and cachesize in the parameter map
     * additionally to the parameters provided by super-classes.
     */
    public DeliCluWrapper() {
        super();
        // parameter min points
        optionHandler.put(new IntParameter(DeLiClu.MINPTS_P, DeLiClu.MINPTS_D, new GreaterConstraint(0)));
        // parameter page size
        IntParameter pageSize = new IntParameter(TreeIndex.PAGE_SIZE_P, TreeIndex.PAGE_SIZE_D, new GreaterConstraint(0));
        pageSize.setDefaultValue(TreeIndex.DEFAULT_PAGE_SIZE);
        optionHandler.put(pageSize);

        // parameter cache size
        IntParameter cacheSize = new IntParameter(TreeIndex.CACHE_SIZE_P, TreeIndex.CACHE_SIZE_D, new GreaterEqualConstraint(0));
        cacheSize.setDefaultValue(TreeIndex.DEFAULT_CACHE_SIZE);
        optionHandler.put(cacheSize);
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // deliclu algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, DeLiClu.class.getName());

        // minpts
        parameters.add(OptionHandler.OPTION_PREFIX + DeLiClu.MINPTS_P);
        parameters.add(Integer.toString(minpts));

        // database
        parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
        parameters.add(SpatialIndexDatabase.class.getName());

        // index
        parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndexDatabase.INDEX_P);
        parameters.add(DeLiCluTree.class.getName());

        // bulk load
        parameters.add(OptionHandler.OPTION_PREFIX + SpatialIndex.BULK_LOAD_F);

        // page size
        parameters.add(OptionHandler.OPTION_PREFIX + TreeIndex.PAGE_SIZE_P);
        parameters.add(Integer.toString(pageSize));

        // cache size
        parameters.add(OptionHandler.OPTION_PREFIX + TreeIndex.CACHE_SIZE_P);
        parameters.add(Integer.toString(cacheSize));

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        // minpts
        minpts = (Integer) optionHandler.getOptionValue(DeLiClu.MINPTS_P);

        // pagesize
        pageSize = (Integer) optionHandler.getOptionValue(TreeIndex.PAGE_SIZE_P);

        // cachesize

        cacheSize = (Integer) optionHandler.getOptionValue(TreeIndex.CACHE_SIZE_P);

        return remainingParameters;
    }
}
