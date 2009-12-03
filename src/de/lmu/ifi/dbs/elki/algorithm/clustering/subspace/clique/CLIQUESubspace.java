package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import de.lmu.ifi.dbs.elki.data.Interval;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Represents a subspace of the original data space in the CLIQUE algorithm.
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector this subspace contains
 */
public class CLIQUESubspace<V extends NumberVector<V, ?>> extends Subspace<V> {
  /**
   * The dense units belonging to this subspace.
   */
  private List<CLIQUEUnit<V>> denseUnits;

  /**
   * The coverage of this subspace, which is the number of all feature vectors
   * that fall inside the dense units of this subspace.
   */
  private int coverage;

  /**
   * Creates a new one-dimensional subspace of the original data space.
   * 
   * @param dimension the dimension building this subspace
   */
  public CLIQUESubspace(int dimension) {
    super(dimension);
    denseUnits = new ArrayList<CLIQUEUnit<V>>();
    coverage = 0;
  }

  /**
   * Creates a new k-dimensional subspace of the original data space.
   * 
   * @param dimensions the dimensions building this subspace
   */
  public CLIQUESubspace(BitSet dimensions) {
    super(dimensions);
    denseUnits = new ArrayList<CLIQUEUnit<V>>();
    coverage = 0;
  }

  /**
   * Adds the specified dense unit to this subspace.
   * 
   * @param unit the unit to be added.
   */
  public void addDenseUnit(CLIQUEUnit<V> unit) {
    Collection<Interval> intervals = unit.getIntervals();
    for(Interval interval : intervals) {
      if(!getDimensions().get(interval.getDimension())) {
        throw new IllegalArgumentException("Unit " + unit + "cannot be added to this subspace, because of wrong dimensions!");
      }
    }

    getDenseUnits().add(unit);
    coverage += unit.numberOfFeatureVectors();
  }

  /**
   * Determines all clusters in this subspace by performing a depth-first search
   * algorithm to find connected dense units.
   * 
   * @param database the database containing the feature vectors
   * @return the clusters in this subspace and the corresponding cluster models
   */
  public Map<CLIQUESubspace<V>, Set<Integer>> determineClusters(Database<V> database) {
    Map<CLIQUESubspace<V>, Set<Integer>> clusters = new HashMap<CLIQUESubspace<V>, Set<Integer>>();

    for(CLIQUEUnit<V> unit : getDenseUnits()) {
      if(!unit.isAssigned()) {
        Set<Integer> cluster = new HashSet<Integer>();
        clusters.put(this, cluster);
        dfs(unit, cluster);
      }
    }

    return clusters;
  }

  /**
   * Depth-first search algorithm to find connected dense units in this subspace
   * that build a cluster. It starts with a unit, assigns it to a cluster and
   * finds all units it is connected to.
   * 
   * @param unit the unit
   * @param cluster the IDs of the feature vectors of the current cluster
   */
  public void dfs(CLIQUEUnit<V> unit, Set<Integer> cluster) {
    cluster.addAll(unit.getIds());
    unit.markAsAssigned();

    for(int dim = getDimensions().nextSetBit(0); dim >= 0; dim = getDimensions().nextSetBit(dim + 1)) {
      CLIQUEUnit<V> left = leftNeighbor(unit, dim);
      if(left != null && !left.isAssigned())
        dfs(left, cluster);

      CLIQUEUnit<V> right = rightNeighbor(unit, dim);
      if(right != null && !right.isAssigned())
        dfs(right, cluster);
    }
  }

  /**
   * Returns the left neighbor of the given unit in the specified dimension.
   * 
   * @param unit the unit to determine the left neighbor for
   * @param dim the dimension
   * @return the left neighbor of the given unit in the specified dimension
   */
  public CLIQUEUnit<V> leftNeighbor(CLIQUEUnit<V> unit, Integer dim) {
    Interval i = unit.getInterval(dim);

    for(CLIQUEUnit<V> u : getDenseUnits()) {
      if(u.containsLeftNeighbor(i))
        return u;
    }
    return null;
  }

  /**
   * Returns the right neighbor of the given unit in the specified dimension.
   * 
   * @param unit the unit to determine the right neighbor for
   * @param dim the dimension
   * @return the right neighbor of the given unit in the specified dimension
   */
  public CLIQUEUnit<V> rightNeighbor(CLIQUEUnit<V> unit, Integer dim) {
    Interval i = unit.getInterval(dim);

    for(CLIQUEUnit<V> u : getDenseUnits()) {
      if(u.containsRightNeighbor(i))
        return u;
    }
    return null;
  }

  /**
   * Returns the coverage of this subspace, which is the number of all feature
   * vectors that fall inside the dense units of this subspace.
   * 
   * @return the coverage of this subspace
   */
  public int getCoverage() {
    return coverage;
  }

  /**
   * @return the denseUnits
   */
  public List<CLIQUEUnit<V>> getDenseUnits() {
    return denseUnits;
  }

  /**
   * Joins this subspace and its dense units with the specified subspace and its
   * dense units. The join is only successful if both subspaces have the first
   * k-1 dimensions in common (where k is the number of dimensions) and the last
   * dimension of this subspace is less than the last dimension of the specified
   * subspace.
   * 
   * @param other the subspace to join
   * @param all the overall number of feature vectors
   * @param tau the density threshold for the selectivity of a unit
   * @return the join of this subspace with the specified subspace if the join
   *         condition is fulfilled, null otherwise.
   * @see Subspace#joinLastDimensions(Subspace)
   */
  public CLIQUESubspace<V> join(CLIQUESubspace<V> other, double all, double tau) {
    BitSet dimensions = joinLastDimensions(other);
    if(dimensions == null)
      return null;

    CLIQUESubspace<V> s = new CLIQUESubspace<V>(dimensions);
    for(CLIQUEUnit<V> u1 : this.getDenseUnits()) {
      for(CLIQUEUnit<V> u2 : other.getDenseUnits()) {
        CLIQUEUnit<V> u = u1.join(u2, all, tau);
        if(u != null) {
          s.addDenseUnit(u);
        }
      }
    }
    if(s.getDenseUnits().isEmpty())
      return null;
    return s;
  }

  /**
   * Calls the super method and adds additionally the coverage, and the dense
   * units of this subspace.
   */
  @Override
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    result.append(super.toString(pre));
    result.append(pre).append("\nCoverage: ").append(coverage);
    result.append(pre).append("\nUnits: " + "\n");
    for(CLIQUEUnit<V> denseUnit : getDenseUnits()) {
      result.append(pre).append("   ").append(denseUnit.toString()).append("   ").append(denseUnit.getIds().size()).append(" objects\n");
    }
    return result.toString();
  }

  /**
   * A partial comparator for CLIQUESubspaces based on their coverage. The
   * CLIQUESubspaces are reverse ordered by the values of their coverage.
   * 
   * Note: this comparator provides an ordering that is inconsistent with
   * equals.
   * 
   * @author Elke Achtert
   */
  public static class CoverageComparator implements Comparator<CLIQUESubspace<?>> {
    /**
     * Compares the two specified CLIQUESubspaces for order. Returns a negative
     * integer, zero, or a positive integer if the coverage of the first
     * subspace is greater than, equal to, or less than the coverage of the
     * second subspace. I.e. the subspaces are reverse ordered by the values of
     * their coverage.
     * 
     * Note: this comparator provides an ordering that is inconsistent with
     * equals.
     * 
     * @param s1 the first subspace to compare
     * @param s2 the second subspace to compare
     * @return a negative integer, zero, or a positive integer if the coverage
     *         of the first subspace is greater than, equal to, or less than the
     *         coverage of the second subspace
     */
    @Override
    public int compare(CLIQUESubspace<?> s1, CLIQUESubspace<?> s2) {
      return -(s1.getCoverage() - s2.getCoverage());
    }
  }
}
