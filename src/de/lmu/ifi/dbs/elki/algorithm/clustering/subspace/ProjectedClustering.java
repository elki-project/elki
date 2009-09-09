package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Abstract superclass for projected clustering algorithms,
 * like {@link PROCLUS} and {@link de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ORCLUS}.
 *
 * @author Elke Achtert
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
public abstract class ProjectedClustering<V extends NumberVector<V,?>>
    extends AbstractAlgorithm<V, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>,V> {

    /**
     * OptionID for {@link #K_PARAM}
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID(
        "projectedclustering.k",
        "The number of clusters to find."
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
     * Holds the value of {@link #K_PARAM}.
     */
    private int k;

    /**
     * OptionID for {@link #K_I_PARAM}
     */
    public static final OptionID K_I_ID = OptionID.getOrCreateOptionID(
        "projectedclustering.k_i",
        "The multiplier for the initial number of seeds."
    );


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
     * Holds the value of {@link #K_I_PARAM}.
     */
    private int k_i;

    /**
     * OptionID for {@link #L_PARAM}
     */
    public static final OptionID L_ID = OptionID.getOrCreateOptionID(
        "projectedclustering.l",
        "The dimensionality of the clusters to find."
    );

    /**
     * Parameter to specify the dimensionality of the clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -projectedclustering.l} </p>
     */
    private final IntParameter L_PARAM = new IntParameter(
        L_ID,
        new GreaterConstraint(0));

    /**
     * Holds the value of {@link #L_PARAM}.
     */
    private int l;

    /**
     * The euclidean distance function.
     */
    private EuclideanDistanceFunction<V> distanceFunction = new EuclideanDistanceFunction<V>();

    /**
     * The result.
     */
    private Clustering<Model> result;

    /**
     * Adds parameters
     * {@link #K_PARAM}, {@link #K_I_PARAM}, and {@link #L_PARAM}
     * to the option handler additionally to parameters of super class.
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
     * Calls the super method
     * and sets additionally the value of the parameters
     * {@link #K_PARAM}, {@link #K_I_PARAM}, and {@link #L_PARAM}.
     */
    @Override
    public List<String> setParameters(List<String> args) throws ParameterException {
        List<String> remainingParameters = super.setParameters(args);

        // k
        k = K_PARAM.getValue();

        // l
        l = L_PARAM.getValue();

        // k_i
        k_i = K_I_PARAM.getValue();

        return remainingParameters;
    }

    public Clustering<Model> getResult() {
        return result;
    }

    /**
     * Returns the distance function.
     *
     * @return the distance function
     */
    protected EuclideanDistanceFunction<V> getDistanceFunction() {
        return distanceFunction;
    }

    /**
     * Returns the value of {@link #K_PARAM}.
     *
     * @return the number of clusters to be found
     */
    protected int getK() {
        return k;
    }

    /**
     * Returns the value of {@link #K_I_PARAM}.
     *
     * @return the initial number of clusters
     */
    protected int getK_i() {
        return k_i;
    }

    /**
     * Returns the value of {@link #L_PARAM}..
     *
     * @return the average dimensionality of the clusters to be found
     */
    protected int getL() {
        return l;
    }

    /**
     * Sets the result of this algorithm.
     *
     * @param result the result to be set
     */
    protected void setResult(Clustering<Model> result) {
        this.result = result;
    }

}
