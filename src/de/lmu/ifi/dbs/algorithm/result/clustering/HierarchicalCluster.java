package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.utilities.Enumeratable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract super class for a hierarchical cluster
 * that holds the ids of the objects
 * belonging to this cluster and the children and parents of this cluster.
 * Hierarchical clusters allow multiple inheritance and can be visualized in a graph.
 *
 * @author Elke Achtert
 * @param <C> the type of HierarchicalCluster used by this Result
 */
public class HierarchicalCluster<C extends HierarchicalCluster<C>> extends AbstractLoggable implements Comparable<C>, Enumeratable<C> {
    /**
     * Holds the ids belonging to this cluster.
     */
    private final Set<Integer> ids;

    /**
     * The children of this cluster.
     */
    private final List<C> children;

    /**
     * The parents of this cluster.
     */
    private final List<C> parents;

    /**
     * The level of this cluster within the graph visualization.
     */
    private int level;

    /**
     * The index of this cluster within the level, needed for graph visualization.
     */
    private int levelIndex;

    /**
     * The label of this cluster, needed for graph visualization.
     */
    private String label;

    /**
     * Provides a new empty hierarchical cluster with the
     * specified preference vector.
     */
    public HierarchicalCluster() {
        this(new HashSet<Integer>(), new ArrayList<C>(), new ArrayList<C>(), "", -1, -1);
    }

    /**
     * Provides a new hierarchical cluster the
     * that holds the ids of the objects
     * belonging to this cluster and the children and parents of this cluster.
     *
     * @param ids        the ids of the objects belonging to this cluster
     * @param children   the list of children of this cluster
     * @param parents    the list of parents of this cluster
     * @param label      the label of this cluster
     * @param level      the level of this cluster in the graph
     * @param levelIndex the index of this cluster within the level
     */
    public HierarchicalCluster(Set<Integer> ids,
                               List<C> children,
                               List<C> parents,
                               String label, int level, int levelIndex) {
        super(LoggingConfiguration.DEBUG);
        this.ids = ids;
        this.children = children;
        this.parents = parents;
        this.label = label;
        this.level = level;
        this.levelIndex = levelIndex;
    }

    /**
     * Adds a new id to this cluster.
     *
     * @param id the id to be added
     */
    public final void addID(Integer id) {
        ids.add(id);
    }

    /**
     * Adds the specified list of ids to this cluster.
     *
     * @param ids the ids to be added
     */
    public final void addIDs(List<Integer> ids) {
        this.ids.addAll(ids);
    }

    /**
     * Removes the specified id from the list of ids of this cluster.
     *
     * @param id the id to be removed
     */
    public final void removeID(Integer id) {
        ids.remove(id);
    }

    /**
     * Adds a child cluster to this cluster.
     *
     * @param child the child to be added.
     */
    public final void addChild(C child) {
        children.add(child);
    }

    /**
     * Returns the children of this cluster.
     *
     * @return the children of this cluster
     */
    public final List<C> getChildren() {
        return children;
    }

    /**
     * Adds a parent to this cluster.
     *
     * @param parent the parent to be added.
     */
    public final void addParent(C parent) {
        parents.add(parent);
    }

    /**
     * Returns the parents of this cluster.
     *
     * @return the parents of this cluster
     */
    public final List<C> getParents() {
        return parents;
    }

    /**
     * Returns the level of this cluster.
     *
     * @return the level of this cluster
     */
    public final int getLevel() {
        return level;
    }

    /**
     * Returns the index within the level of this cluster.
     *
     * @return the index within the level
     */
    public final int getLevelIndex() {
        return levelIndex;
    }

    /**
     * Sets the index within the level of this cluster.
     *
     * @param levelIndex the index to be set
     */
    public final void setLevelIndex(int levelIndex) {
        this.levelIndex = levelIndex;
    }

    /**
     * Sets the label of this cluster.
     *
     * @param label the label of this cluster
     */
    public final void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the label of this cluster.
     *
     * @return the label of this cluster
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the level of this cluster.
     *
     * @param level the level to be set
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Returns the label of this HierarchicalCluster.
     *
     * @return the label of this HierarchicalCluster
     */
    @Override
    public final String toString() {
        return label;
    }

    /**
     * Returns the ids belonging to this cluster.
     *
     * @return the ids belonging to this cluster
     */
    public final List<Integer> getIDs() {
        return new ArrayList<Integer>(ids);
    }

    /**
     * Compares this object with the specified object for order.
     * Compares the both clusters w.r.t. their level and their index within the level
     * in the graph.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    public final int compareTo(C o) {
        int comp = this.level - o.level;
        if (comp != 0) {
            return -comp;
        }

        return this.levelIndex - o.levelIndex;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two clusters are equal if they have equal levels and equal indices
     * within the level (i.e. if they have the same position in the graph).
     *
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HierarchicalCluster<C> that = (HierarchicalCluster<C>) o;

        if (level != that.level) {
            return false;
        }
        return (levelIndex == that.levelIndex);
    }

    /**
     * Returns the number of children.
     *
     * @return the number of children
     */
    public final int numChildren() {
        return children.size();
    }

    /**
     * Returns the number of parents.
     *
     * @return the number of parents
     */
    public final int numParents() {
        return parents.size();
    }

    /**
     * Returns the child at the specified index.
     *
     * @param i the index of the child to be returned
     * @return the child at the specified index
     */
    public final C getChild(int i) {
        return children.get(i);
    }

    /**
     * Returns the parent at the specified index.
     *
     * @param i the index of the parent to be returned
     * @return the parent at the specified index
     */
    public final C getParent(int i) {
        return parents.get(i);
    }
}
