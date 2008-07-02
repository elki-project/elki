package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalClusters;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalFractalDimensionCluster;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * todo arthur comment class
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 */
public class FracClus<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> {

    /**
     * Parameter to specify the number of supporters,
     * must be an integer equal to or greater than 2.
     * <p>Key: {@code -fraclus.supporters} </p>
     */
    private final IntParameter NUMBER_OF_SUPPORTERS_PARAM = new IntParameter(OptionID.FRACLUS_NUMBER_OF_SUPPORTERS,
        new GreaterEqualConstraint(2));

    /**
     * Holds the value of the number of supporters parameter.
     */
    private int k;

    /**
     * Holds the result of the algorithm.
     */
    private HierarchicalClusters<HierarchicalFractalDimensionCluster<V>, V> result;

    /**
     * todo comment
     */
    public FracClus() {
        super();
        addOption(NUMBER_OF_SUPPORTERS_PARAM);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getResult()
     */
    public HierarchicalClusters<HierarchicalFractalDimensionCluster<V>, V> getResult() {
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.elki.database.Database)
     */
    protected void runInTime(Database<V> database) throws IllegalStateException {
        List<HierarchicalFractalDimensionCluster<V>> clusters = new ArrayList<HierarchicalFractalDimensionCluster<V>>();
        if (this.isVerbose()) {
            verbose("assigning database objects to base clusters");
        }
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            HierarchicalFractalDimensionCluster<V> point = new HierarchicalFractalDimensionCluster<V>(id, database, k);
            point.setLevel(0);
            point.setLabel("Level=" + 0 + "_ID=" + id + "_" + point.getLabel());
            clusters.add(point);
        }
        if (this.isVerbose()) {
            verbose("agglomerating");
        }
        Progress agglomeration = new Progress("agglomerating", database.size() - 1);
        for (int level = 1; level < database.size(); level++) {
            int indexI = 0;
            int indexJ = 1;
            double minimum = Double.MAX_VALUE;
            HierarchicalFractalDimensionCluster<V> cluster = null;
            for (int i = 0; i < clusters.size() - 1; i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    HierarchicalFractalDimensionCluster<V> currentCluster = new HierarchicalFractalDimensionCluster<V>(clusters.get(i), clusters.get(j), database, k);
                    double fractalDimension = currentCluster.getFractalDimension();
                    if (fractalDimension < minimum) {
                        minimum = fractalDimension;
                        indexI = i;
                        indexJ = j;
                        cluster = currentCluster;
                    }
                }
            }
            clusters.remove(indexJ);
            clusters.remove(indexI);
            cluster.setLevel(level);
            cluster.setLabel("Level=" + level + "_" + cluster.getLabel());
            for (HierarchicalFractalDimensionCluster<V> child : cluster.getChildren()) {
                child.getParents().add(cluster);
            }

            clusters.add(cluster);
            //cluster = null;
            if (this.isVerbose()) {
                agglomeration.setProcessed(level);
                progress(agglomeration);
            }
        }
        if (this.isVerbose()) {
            verbose();
        }
        result = new HierarchicalClusters<HierarchicalFractalDimensionCluster<V>, V>(clusters, database);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        k = getParameterValue(NUMBER_OF_SUPPORTERS_PARAM);
        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("FracClus", "Fractal Dimension based Clustering", "", "unpublished");
    }

}
