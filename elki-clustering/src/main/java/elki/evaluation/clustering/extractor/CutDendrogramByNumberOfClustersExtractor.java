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
package elki.evaluation.clustering.extractor;

import java.util.ArrayList;

import elki.Algorithm;
import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import elki.evaluation.Evaluator;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.ChainedParameterization;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Extract clusters from a hierarchical clustering, during the evaluation phase.
 * <p>
 * Usually, it is more elegant to use
 * {@link CutDendrogramByNumberOfClustersExtractor} as primary algorithm. But
 * in order to extract <em>multiple</em> partitionings from the same
 * clustering, this can be useful.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class CutDendrogramByNumberOfClustersExtractor implements Evaluator {
  /**
   * Class to perform the cluster extraction.
   */
  private CutDendrogramByNumberOfClusters inner;

  /**
   * Constructor.
   *
   * @param inner Inner algorithm instance.
   */
  public CutDendrogramByNumberOfClustersExtractor(CutDendrogramByNumberOfClusters inner) {
    this.inner = inner;
  }

  @Override
  public void processNewResult(Object newResult) {
    ArrayList<ClusterMergeHistory> hrs = ResultUtil.filterResults(newResult, ClusterMergeHistory.class);
    for(ClusterMergeHistory pointerresult : hrs) {
      Metadata.hierarchyOf(pointerresult).addChild(inner.run(pointerresult));
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Inner algorithm to extract a clustering.
     */
    CutDendrogramByNumberOfClusters inner;

    @Override
    public void configure(Parameterization config) {
      ListParameterization overrides = new ListParameterization();
      overrides.addParameter(Algorithm.Utils.ALGORITHM_ID, CutDendrogramByHeightExtractor.DummyHierarchicalClusteringAlgorithm.class);
      ChainedParameterization list = new ChainedParameterization(overrides, config);
      list.errorsTo(config);
      inner = list.tryInstantiate(CutDendrogramByNumberOfClusters.class);
    }

    @Override
    public CutDendrogramByNumberOfClustersExtractor make() {
      return new CutDendrogramByNumberOfClustersExtractor(inner);
    }
  }
}
