/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.hierarchical.extraction;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Extract a flat clustering from a full hierarchy, represented in pointer form.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @assoc - - - HierarchicalClusteringAlgorithm
 * @assoc - - - PointerHierarchyResult
 */
public class CutDendrogramByNumberOfClusters extends AbstractCutDendrogram implements ClusteringAlgorithm<Clustering<DendrogramModel>> {
  /**
   * Class logger.
   */
  static final Logging LOG = Logging.getLogger(CutDendrogramByNumberOfClusters.class);

  /**
   * Minimum number of clusters to extract
   */
  private final int minclusters;

  /**
   * Constructor.
   *
   * @param algorithm Algorithm to run
   * @param minclusters Minimum number of clusters
   * @param hierarchical Produce a hierarchical output
   */
  public CutDendrogramByNumberOfClusters(HierarchicalClusteringAlgorithm algorithm, int minclusters, boolean hierarchical) {
    this(algorithm, minclusters, hierarchical, true);
  }

  /**
   * Constructor.
   *
   * @param algorithm Algorithm to run
   * @param minclusters Minimum number of clusters
   * @param hierarchical Produce a hierarchical output
   * @param simplify Simplify by putting single points into merge clusters
   */
  public CutDendrogramByNumberOfClusters(HierarchicalClusteringAlgorithm algorithm, int minclusters, boolean hierarchical, boolean simplify) {
    super(algorithm, hierarchical, simplify);
    this.minclusters = minclusters;
  }

  @Override
  public Clustering<DendrogramModel> run(ClusterMergeHistory merges) {
    Clustering<DendrogramModel> result = new Instance(merges).extractClusters();
    Metadata.hierarchyOf(result).addChild(merges);
    return result;
  }

  /**
   * Instance for a single data set.
   * 
   * @author Erich Schubert
   */
  protected class Instance extends AbstractCutDendrogram.Instance {
    /**
     * Constructor.
     *
     * @param merges cluster merge history
     */
    public Instance(ClusterMergeHistory merges) {
      super(merges);
    }

    @Override
    protected int findSplit() {
      if(merges.size() <= minclusters) {
        return 0; // no splits
      }
      int split = merges.size() - minclusters;
      // Stop distance:
      final double stopdist = merges.getMergeHeight(split);
      // Tie handling: decrement split.
      while(split > 0 && stopdist == merges.getMergeHeight(split - 1)) {
        --split;
      }
      return split;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par extends AbstractCutDendrogram.Par {
    /**
     * The minimum number of clusters to extract.
     */
    public static final OptionID MINCLUSTERS_ID = new OptionID("hierarchical.minclusters", "The minimum number of clusters to extract (there may be more clusters when tied, and singletons may be merged into a noise cluster).");

    /**
     * Number of clusters to extract.
     */
    int minclusters = -1;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(MINCLUSTERS_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> minclusters = x);
    }

    @Override
    public CutDendrogramByNumberOfClusters make() {
      return new CutDendrogramByNumberOfClusters(algorithm, minclusters, hierarchical, simplify);
    }
  }
}
