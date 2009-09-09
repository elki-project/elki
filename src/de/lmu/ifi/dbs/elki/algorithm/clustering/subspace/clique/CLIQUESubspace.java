package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import de.lmu.ifi.dbs.elki.data.Interval;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Represents a subspace of the original data space in the CLIQUE algorithm.
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector handled by this Algorithm
 */
public class CLIQUESubspace<V extends NumberVector<V,?>> extends Subspace<V> implements Comparable<CLIQUESubspace<V>>, Model, TextWriteable {
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
  public CLIQUESubspace(SortedSet<Integer> dimensions) {
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
      if(!getDimensions().contains(interval.getDimension())) {
        throw new IllegalArgumentException("Unit " + unit + "cannot be added to this subspace, " + "because of wrong dimensions!");
      }
    }

    denseUnits.add(unit);
    coverage += unit.numberOfFeatureVectors();
  }

  /**
   * Compares this subspace with the specified subspace for order. Returns a
   * negative integer, zero, or a positive integer if the coverage of this
   * subspace is less than, equal to, or greater than the coverage of the
   * specified subspace.
   * 
   * @param other the subspace to be compared
   * @return a negative integer, zero, or a positive integer if the coverage of
   *         this subspace is less than, equal to, or greater than the coverage
   *         of the specified subspace.
   */
  public int compareTo(CLIQUESubspace<V> other) {
    if(coverage == other.coverage) {
      if(this.getDimensions().size() != other.getDimensions().size()) {
        throw new IllegalArgumentException("different dimensions sizes!");
      }
      Iterator<Integer> it1 = this.getDimensions().iterator();
      Iterator<Integer> it2 = other.getDimensions().iterator();
      while(it1.hasNext()) {
        Integer d1 = it1.next();
        Integer d2 = it2.next();
        if(d1.equals(d2))
          continue;
        return d1.compareTo(d2);
      }
    }

    if(coverage < other.coverage)
      return 1;

    return -1;
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

    for(CLIQUEUnit<V> unit : denseUnits) {
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
   * finds all units it is connected to
   * 
   * @param unit the unit
   * @param cluster the ids of the feature vectors of the current cluster
   */
  public void dfs(CLIQUEUnit<V> unit, Set<Integer> cluster) {
    cluster.addAll(unit.getIds());
    unit.markAsAssigned();

    for(Integer dim : getDimensions()) {
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

    for(CLIQUEUnit<V> u : denseUnits) {
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

    for(CLIQUEUnit<V> u : denseUnits) {
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
   * Joins this subspace with the specified subspace. The join is only
   * successful if both subspaces have the first k-1 dimensions in common (where
   * k is the number of dimensions).
   * 
   * @param other the subspace to join
   * @param all the overall number of feature vectors
   * @param tau the density threshold for the selectivity of a unit
   * @return the join of this subspace with the specified subspace if the join
   *         condition is fulfilled, null otherwise.
   */
  public CLIQUESubspace<V> join(CLIQUESubspace<V> other, double all, double tau) {
    SortedSet<Integer> dimensions = joinDimensions(other);
    if(dimensions == null)
      return null;

    CLIQUESubspace<V> s = new CLIQUESubspace<V>(dimensions);
    for(int i = 0; i < this.denseUnits.size(); i++) {
      CLIQUEUnit<V> u1 = this.denseUnits.get(i);
      for(CLIQUEUnit<V> u2 : other.denseUnits) {
        CLIQUEUnit<V> u = u1.join(u2, all, tau);
        if(u != null) {
          s.addDenseUnit(u);
        }
      }
    }
    if(s.denseUnits.isEmpty())
      return null;
    return s;
  }

  /**
   * Returns a string representation of this subspace that contains the
   * coverage, the dimensions and the dense units of this subspace.
   * 
   * @param pre a string prefix
   * @return a string representation of this subspace
   */
  @Override
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    result.append(super.toString(pre));
    result.append(pre).append("Coverage: ").append(coverage).append("\n");
    result.append(pre).append("Units: " + "\n");
    for(CLIQUEUnit<V> denseUnit : denseUnits) {
      result.append(pre).append("   ").append(denseUnit.toString()).append("   ").append(denseUnit.getIds().size()).append(" objects\n");
    }
    return result.toString();
  }

  /**
   * Serialize using above toString method
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn(this.toString());
  }
}
