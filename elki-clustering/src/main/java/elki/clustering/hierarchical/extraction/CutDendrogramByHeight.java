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
package elki.clustering.hierarchical.extraction;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Extract a flat clustering from a full hierarchy, represented in pointer form.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @assoc - - - HierarchicalClusteringAlgorithm
 * @assoc - - - PointerHierarchyResult
 */
public class CutDendrogramByHeight extends AbstractCutDendrogram implements ClusteringAlgorithm<Clustering<DendrogramModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CutDendrogramByHeight.class);

  /**
   * Threshold for extracting clusters.
   */
  private final double threshold;

  /**
   * Constructor.
   *
   * @param algorithm Algorithm to run
   * @param threshold Distance threshold
   * @param hierarchical Produce a hierarchical output
   */
  public CutDendrogramByHeight(HierarchicalClusteringAlgorithm algorithm, double threshold, boolean hierarchical) {
    this(algorithm, threshold, hierarchical, true);
  }

  /**
   * Constructor.
   *
   * @param algorithm Algorithm to run
   * @param threshold Distance threshold
   * @param hierarchical Produce a hierarchical output
   * @param simplify Simplify by putting single points into merge clusters
   */
  public CutDendrogramByHeight(HierarchicalClusteringAlgorithm algorithm, double threshold, boolean hierarchical, boolean simplify) {
    super(algorithm, hierarchical, simplify);
    this.threshold = threshold;
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
     * @param merges Pointer result
     */
    public Instance(ClusterMergeHistory merges) {
      super(merges);
    }

    @Override
    protected int findSplit() {
      int split = merges.numMerges() - 1;
      while(split > 1 && threshold <= merges.getMergeHeight(split - 1)) {
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
     * The threshold level for which to extract the clustering.
     */
    public static final OptionID THRESHOLD_ID = new OptionID("hierarchical.threshold", "The threshold level for which to extract the clusters.");

    /**
     * Threshold level.
     */
    double threshold = Double.NaN;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(THRESHOLD_ID) //
          .grab(config, x -> threshold = x);
    }

    @Override
    public CutDendrogramByHeight make() {
      return new CutDendrogramByHeight(algorithm, threshold, hierarchical, simplify);
    }
  }
}
