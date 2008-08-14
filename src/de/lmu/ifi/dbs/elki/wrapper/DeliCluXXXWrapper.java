package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DeLiClu;
import de.lmu.ifi.dbs.elki.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.elki.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTree;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.List;

/**
 * Wrapper class for the DeliClu algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class DeliCluXXXWrapper extends NormalizationWrapper {

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -optics.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(DeLiClu.MINPTS_ID,
        new GreaterConstraint(0));

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
        DeliCluXXXWrapper wrapper = new DeliCluXXXWrapper();
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
     * Adds parameters
     * {@link #MINPTS_PARAM}, {@link }, and {@link } todo
     * to the option handler additionally to parameters of super class.
     */
    public DeliCluXXXWrapper() {
        super();
        // parameter minpts
        optionHandler.put(MINPTS_PARAM);

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
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        // database
        Util.addParameter(parameters, AbstractDatabaseConnection.DATABASE_ID, SpatialIndexDatabase.class.getName());

        // index
        Util.addParameter(parameters, SpatialIndexDatabase.INDEX_ID, DeLiCluTree.class.getName());

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
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // pagesize
        pageSize = (Integer) optionHandler.getOptionValue(TreeIndex.PAGE_SIZE_P);

        // cachesize

        cacheSize = (Integer) optionHandler.getOptionValue(TreeIndex.CACHE_SIZE_P);

        return remainingParameters;
    }
}
