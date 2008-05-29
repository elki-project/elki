package de.lmu.ifi.dbs.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalClusters;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalFractalDimensionCluster;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * todo arthur comment all
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 */
public class FracClus<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> {
    public static final String NUMBER_OF_SUPPORTERS_P = "supporters";

    public static final String NUMBER_OF_SUPPORTERS_D = "number of supporters (at least 2)";

    private int k;

    private IntParameter kParameter = new IntParameter(NUMBER_OF_SUPPORTERS_P, NUMBER_OF_SUPPORTERS_D, new GreaterEqualConstraint(2));

    private HierarchicalClusters<HierarchicalFractalDimensionCluster<V>, V> result;

    public FracClus() {
        super();
        optionHandler.put(kParameter);
    }

    public HierarchicalClusters<HierarchicalFractalDimensionCluster<V>, V> getResult() {
        return result;
    }


    @Override
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
            cluster = null;
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


    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        AttributeSettings myAttributeSettings = new AttributeSettings(this);
        myAttributeSettings.addSetting(NUMBER_OF_SUPPORTERS_P, Integer.toString(k));
        attributeSettings.add(myAttributeSettings);
        return attributeSettings;
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        k = getParameterValue(kParameter);
        return remainingParameters;
    }

    public Description getDescription() {
        // TODO Auto-generated method stub
        return new Description("FracClus", "Fractal Dimension based Clustering", "", "unpublished");
    }

}
