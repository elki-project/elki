/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Represents a subspace of the original data space in the CLIQUE algorithm.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @has - - - CoverageComparator
 * @composed - - - CLIQUEUnit
 */
public class CLIQUESubspace extends Subspace {
  /**
   * The dense units belonging to this subspace.
   */
  private List<CLIQUEUnit> denseUnits;

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
    denseUnits = new ArrayList<>();
    coverage = 0;
  }

  /**
   * Creates a new k-dimensional subspace of the original data space.
   * 
   * @param dimensions the dimensions building this subspace
   */
  public CLIQUESubspace(long[] dimensions) {
    super(dimensions);
    denseUnits = new ArrayList<>();
    coverage = 0;
  }

  /**
   * Adds the specified dense unit to this subspace.
   * 
   * @param unit the unit to be added.
   */
  public void addDenseUnit(CLIQUEUnit unit) {
    int numdim = unit.dimensionality();
    for(int i = 0; i < numdim; i++) {
      BitsUtil.setI(getDimensions(), unit.getDimension(i));
    }

    denseUnits.add(unit);
    coverage += unit.numberOfFeatureVectors();
  }

  /**
   * Determines all clusters in this subspace by performing a depth-first search
   * algorithm to find connected dense units.
   * 
   * @return the clusters in this subspace and the corresponding cluster models
   */
  public List<Pair<Subspace, ModifiableDBIDs>> determineClusters() {
    List<Pair<Subspace, ModifiableDBIDs>> clusters = new ArrayList<>();

    for(CLIQUEUnit unit : denseUnits) {
      if(!unit.isAssigned()) {
        ModifiableDBIDs cluster = DBIDUtil.newHashSet();
        CLIQUESubspace model = new CLIQUESubspace(getDimensions());
        clusters.add(new Pair<Subspace, ModifiableDBIDs>(model, cluster));
        dfs(unit, cluster, model);
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
   * @param model the model of the cluster
   */
  public void dfs(CLIQUEUnit unit, ModifiableDBIDs cluster, CLIQUESubspace model) {
    cluster.addDBIDs(unit.getIds());
    unit.markAsAssigned();
    model.addDenseUnit(unit);

    final long[] dims = getDimensions();
    for(int dim = BitsUtil.nextSetBit(dims, 0); dim >= 0; dim = BitsUtil.nextSetBit(dims, dim + 1)) {
      CLIQUEUnit left = leftNeighbor(unit, dim);
      if(left != null && !left.isAssigned()) {
        dfs(left, cluster, model);
      }

      CLIQUEUnit right = rightNeighbor(unit, dim);
      if(right != null && !right.isAssigned()) {
        dfs(right, cluster, model);
      }
    }
  }

  /**
   * Returns the left neighbor of the given unit in the specified dimension.
   * 
   * @param unit the unit to determine the left neighbor for
   * @param dim the dimension
   * @return the left neighbor of the given unit in the specified dimension
   */
  protected CLIQUEUnit leftNeighbor(CLIQUEUnit unit, int dim) {
    for(CLIQUEUnit u : denseUnits) {
      if(u.containsLeftNeighbor(unit, dim)) {
        return u;
      }
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
  protected CLIQUEUnit rightNeighbor(CLIQUEUnit unit, int dim) {
    for(CLIQUEUnit u : denseUnits) {
      if(u.containsRightNeighbor(unit, dim)) {
        return u;
      }
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
   * @see de.lmu.ifi.dbs.elki.data.Subspace#joinLastDimensions
   */
  public CLIQUESubspace join(CLIQUESubspace other, double all, double tau) {
    long[] dimensions = joinLastDimensions(other);
    if(dimensions == null) {
      return null;
    }

    CLIQUESubspace s = new CLIQUESubspace(dimensions);
    for(CLIQUEUnit u1 : this.denseUnits) {
      for(CLIQUEUnit u2 : other.denseUnits) {
        CLIQUEUnit u = u1.join(u2, all, tau);
        if(u != null) {
          s.addDenseUnit(u);
        }
      }
    }
    if(s.denseUnits.isEmpty()) {
      return null;
    }
    return s;
  }

  /**
   * Calls the super method and adds additionally the coverage, and the dense
   * units of this subspace.
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder() //
        .append(super.toString()) //
        .append("\nCoverage: ").append(coverage) //
        .append("\nUnits: \n");
    for(CLIQUEUnit denseUnit : denseUnits) {
      result.append("   ").append(denseUnit.toString()).append("   ").append(denseUnit.getIds().size()).append(" objects\n");
    }
    return result.toString();
  }

  /**
   * A partial comparator for CLIQUESubspaces based on their coverage. The
   * CLIQUESubspaces are reverse ordered by the values of their coverage.
   * <p>
   * Compares the two specified CLIQUESubspaces for order. Returns a negative
   * integer, zero, or a positive integer if the coverage of the first
   * subspace is greater than, equal to, or less than the coverage of the
   * second subspace. I.e. the subspaces are reverse ordered by the values of
   * their coverage.
   * <p>
   * Note: this comparator provides an ordering that is inconsistent with
   * equals.
   */
  public static Comparator<CLIQUESubspace> BY_COVERAGE = new Comparator<CLIQUESubspace>() {
    @Override
    public int compare(CLIQUESubspace s1, CLIQUESubspace s2) {
      return Integer.compare(s2.getCoverage(), s1.getCoverage());
    }
  };
}
