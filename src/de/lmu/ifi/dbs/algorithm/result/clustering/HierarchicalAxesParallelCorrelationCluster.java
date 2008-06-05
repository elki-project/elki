package de.lmu.ifi.dbs.algorithm.result.clustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides a hierarchical axes parallel correlation cluster
 * that holds the preference vector of this cluster, the ids of the objects
 * belonging to this cluster and the children and parents of this cluster.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HierarchicalAxesParallelCorrelationCluster extends HierarchicalCluster<HierarchicalAxesParallelCorrelationCluster> {
    /**
     * The preference vector of this cluster.
     */
    private final BitSet preferenceVector;

    /**
     * Provides a new empty hierarchical axes parallel correlation cluster with the
     * specified preference vector.
     *
     * @param preferenceVector the preference vector of this cluster
     */
    public HierarchicalAxesParallelCorrelationCluster(BitSet preferenceVector) {
        this(preferenceVector, new HashSet<Integer>(),
            new ArrayList<HierarchicalAxesParallelCorrelationCluster>(),
            new ArrayList<HierarchicalAxesParallelCorrelationCluster>(),
            "", -1, -1);
    }

    /**
     * Provides a hierarchical axes parallel correlation cluster
     * that holds the preference vector of this cluster, the ids of the objects
     * belonging to this cluster and the children and parents of this cluster.
     *
     * @param preferenceVector the preference vector of this cluster
     * @param ids              the idsa of the objects belonging to this cluster
     * @param children         the list of children of this cluster
     * @param parents          the list of parents of this cluster
     * @param label            the label of this cluster
     * @param level            the level of this cluster in the graph
     * @param levelIndex       the index of this cluster within the level
     */
    public HierarchicalAxesParallelCorrelationCluster(BitSet preferenceVector,
                                                      Set<Integer> ids,
                                                      List<HierarchicalAxesParallelCorrelationCluster> children,
                                                      List<HierarchicalAxesParallelCorrelationCluster> parents,
                                                      String label, int level, int levelIndex) {
        super(ids, children, parents, label, level, levelIndex);
        this.preferenceVector = preferenceVector;
    }

    /**
     * Returns the preference vector of this cluster.
     *
     * @return the preference vector of this cluster
     */
    public BitSet getPreferenceVector() {
        return preferenceVector;
    }

    /**
     * Returns a hash code value for this cluster.
     *
     * @return a hash code value for this cluster
     */
    @Override
    public int hashCode() {
        return preferenceVector.hashCode();
    }
}
