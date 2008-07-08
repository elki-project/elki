package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Abstract superclass for PROCLUS and ORCLUS.
 *
 * @author Elke Achtert
 * @param <V> the type of Realvector handled by this Algorithm
 */
public abstract class ProjectedClustering<V extends RealVector<V, ?>>
    extends AbstractAlgorithm<V> implements Clustering<V> {

    /**
     * OptionID for {@link #K_PARAM}
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID(
        "projectedclustering.k",
        "The number of clusters to find."
    );

    /**
     * OptionID for {@link #K_I_PARAM}
     */
    public static final OptionID K_I_ID = OptionID.getOrCreateOptionID(
        "projectedclustering.k_i",
        "The multiplier for the initial number of seeds."
    );

    /**
     * OptionID for {@link #L_PARAM}
     */
    public static final OptionID L_ID = OptionID.getOrCreateOptionID(
        "projectedclustering.l",
        "The dimensionality of the clusters to find."
    );

    /**
     * Parameter to specify the number of clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -projectedclustering.k} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(
        K_ID,
        new GreaterConstraint(0));

    /**
     * Parameter to specify the multiplier for the initial number of seeds,
     * must be an integer greater than 0.
     * <p>Default value: {@code 30} </p>
     * <p>Key: {@code -projectedclustering.k_i} </p>
     */
    private final IntParameter K_I_PARAM = new IntParameter(
        K_I_ID,
        new GreaterConstraint(0),
        30);

    /**
     * Parameter to specify the dimensionality of the clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -projectedclustering.l} </p>
     */
    private final IntParameter L_PARAM = new IntParameter(
        L_ID,
        new GreaterConstraint(0));

    /**
     * Number of clusters.
     */
    private int k;

    /**
     * Multiplier for initial number of seeds.
     */
    private int k_i;

    /**
     * Dimensionality of the clusters.
     */
    private int l;

    /**
     * The euklidean distance function.
     */
    private EuklideanDistanceFunction<V> distanceFunction = new EuklideanDistanceFunction<V>();

    /**
     * The result.
     */
    private Clusters<V> result;

    /**
     * Sets the parameter k and l the optionhandler additionally to the
     * parameters provided by super-classes.
     */
    public ProjectedClustering() {
        super();
        // parameter k
        addOption(K_PARAM);

        // parameter k_i
        addOption(K_I_PARAM);

        // parameter l
        addOption(L_PARAM);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // k
        k = getParameterValue(K_PARAM);

        // l
        l = getParameterValue(L_PARAM);

        // k_i
        k_i = getParameterValue(K_I_PARAM);

        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.clustering.Clustering#getResult()
     */
    public Clusters<V> getResult() {
        return result;
    }

    /**
     * Returns the distance function.
     *
     * @return the distance function
     */
    protected EuklideanDistanceFunction<V> getDistanceFunction() {
        return distanceFunction;
    }

    /**
     * Returns the value of parameter k.
     *
     * @return the number of clusters to be found
     */
    protected int getK() {
        return k;
    }

    /**
     * Returns the value of parameter k_i.
     *
     * @return the initial number of clusters
     */
    protected int getK_i() {
        return k_i;
    }

    /**
     * Returns the value of parameter l.
     *
     * @return the average dimesnionality of the clusters to be found
     */
    protected int getL() {
        return l;
    }

    /**
     * Sets the result of this algorithm.
     *
     * @param result the result to be set
     */
    protected void setResult(Clusters<V> result) {
        this.result = result;
    }

}
