package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Associations;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.varianceanalysis.FirstNEigenPairFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates a mapping of subspace dimensionalities to a list of set of ids forming a cluster
 * in a specific subspace dimension.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SubspaceClusterMap {
    /**
     * The map holding the clusters.
     */
    private Map<Integer, List<Set<Integer>>> clusters;

    /**
     * The map holding the basis vectors of the clusters.
     */
    private Map<Integer, List<LinearEquationSystem>> dependencies;

    /**
     * The dimensionality of noise.
     */
    private int noiseDimensionality;

    /**
     * Provides a mapping of subspace dimensionalities to a list of set of ids forming a cluster
     * in a specific subspace dimension.
     *
     * @param noiseDimensionality the dimensionality of noise
     */
    public SubspaceClusterMap(int noiseDimensionality) {
        this.clusters = new HashMap<Integer, List<Set<Integer>>>();
        this.dependencies = new HashMap<Integer, List<LinearEquationSystem>>();
        this.noiseDimensionality = noiseDimensionality;
    }

    /**
     * Adds a cluster with the specified subspace dimensionality and the
     * specified ids to this map.
     *
     * @param dimensionality the subspace dimensionality of the cluster
     * @param ids            the ids forming the cluster
     * @param database       the database holding the objects
     */
    public void add(Integer dimensionality, Set<Integer> ids, Database<ParameterizationFunction> database) {
        List<Set<Integer>> clusterList = clusters.get(dimensionality);
        if (clusterList == null) {
            clusterList = new ArrayList<Set<Integer>>();
            clusters.put(dimensionality, clusterList);
        }
        clusterList.add(ids);

        LinearEquationSystem les = runDerivator(database, dimensionality, ids);
        List<LinearEquationSystem> dependencyList = dependencies.get(dimensionality);
        if (dependencyList == null) {
            dependencyList = new ArrayList<LinearEquationSystem>();
            dependencies.put(dimensionality, dependencyList);
        }
        dependencyList.add(les);
    }

    /**
     * Adds the specified ids to noise.
     *
     * @param ids the ids forming the noise
     */
    public void addToNoise(Set<Integer> ids) {
        List<Set<Integer>> clusterList = clusters.get(noiseDimensionality);
        if (clusterList == null) {
            clusterList = new ArrayList<Set<Integer>>();
            clusterList.add(ids);
            clusters.put(noiseDimensionality, clusterList);
        }
        else {
            clusterList.get(0).addAll(ids);
        }
    }

    /**
     * Returns a sorted list view of the subspace dimensionalities
     * contained in this cluster map.
     *
     * @return a sorted list view of the subspace dimensionalities
     *         contained in this map
     */
    public List<Integer> subspaceDimensionalities() {
        List<Integer> dims = new ArrayList<Integer>(clusters.keySet());
        Collections.sort(dims);
        return dims;
    }

    /**
     * Returns the list of clusters to which this map maps the specified subspaceDimension.
     *
     * @param subspaceDimension subspace dimension whose associated clusters are to be returned
     * @return the list of clusters to which this map maps the specified subspaceDimension
     */
    public List<Set<Integer>> getCluster(Integer subspaceDimension) {
        return clusters.get(subspaceDimension);
    }

    /**
     * Returns the list of dependencies to which this map maps the specified subspaceDimension.
     *
     * @param subspaceDimension subspace dimension whose associated dependencies are to be returned
     * @return the list of dependencies to which this map maps the specified subspaceDimension
     */
    public List<LinearEquationSystem> getDependencies(Integer subspaceDimension) {
        return dependencies.get(subspaceDimension);
    }

    /**
     * Returns the number of clusters (excl. noise) in this map.
     *
     * @return the number of clusters (excl. noise) in this map
     */
    public int numClusters() {
        int result = 0;
        for (Integer d : clusters.keySet()) {
            if (d == noiseDimensionality) continue;
            List<Set<Integer>> clusters_d = clusters.get(d);
            result += clusters_d.size();
        }
        return result;
    }

    /**
     * Runs the derivator on the specified inerval and assigns all points
     * having a distance less then the standard deviation of the derivator model
     * to the model to this model.
     *
     * @param database       the database containing the parametrization functions
     * @param ids            the ids to build the model
     * @param dimensionality the dimensionality of the subspace
     * @return a basis of the found subspace
     */
    private LinearEquationSystem runDerivator(Database<ParameterizationFunction> database,
                                              int dimensionality,
                                              Set<Integer> ids) {
        try {
            // build database for derivator
            Database<RealVector> derivatorDB = buildDerivatorDB(database, ids);

            DependencyDerivator derivator = new DependencyDerivator();

            List<String> parameters = new ArrayList<String>();
            Util.addParameter(parameters, OptionID.PCA_EIGENPAIR_FILTER, FirstNEigenPairFilter.class.getName());
            parameters.add(OptionHandler.OPTION_PREFIX + FirstNEigenPairFilter.N_P);
            parameters.add(Integer.toString(dimensionality));
            derivator.setParameters(parameters.toArray(new String[parameters.size()]));

            //noinspection unchecked
            derivator.run(derivatorDB);
            CorrelationAnalysisSolution model = derivator.getResult();
            // noinspection unchecked
            LinearEquationSystem les = model.getNormalizedLinearEquationSystem(null);
            return les;
        }
        catch (ParameterException e) {
            throw new IllegalStateException("Wrong parameter-setting for the derivator: " + e);
        }
        catch (UnableToComplyException e) {
            throw new IllegalStateException("Initialization of the database for the derivator failed: " + e);
        }
        catch (NonNumericFeaturesException e) {
            throw new IllegalStateException("Error during normalization" + e);
        }
    }

    /**
     * Builds a database for the derivator consisting of the ids
     * in the specified interval.
     *
     * @param database the database storing the paramterization functions
     * @param ids      the ids to build the database from
     * @return a database for the derivator consisting of the ids
     *         in the specified interval
     * @throws UnableToComplyException if initialization of the database is not possible
     */
    private Database<RealVector> buildDerivatorDB(Database<ParameterizationFunction> database,
                                                  Set<Integer> ids) throws UnableToComplyException {
        // build objects and associations
        List<ObjectAndAssociations<RealVector>> oaas = new ArrayList<ObjectAndAssociations<RealVector>>(database.size());

        for (Integer id : ids) {
            Associations associations = database.getAssociations(id);
            RealVector v = new DoubleVector(database.get(id).getRowVector().getRowPackedCopy());
            ObjectAndAssociations<RealVector> oaa = new ObjectAndAssociations<RealVector>(v, associations);
            oaas.add(oaa);
        }

        // insert into db
        Database<RealVector> result = new SequentialDatabase<RealVector>();
        result.insert(oaas);

        return result;
    }


}
