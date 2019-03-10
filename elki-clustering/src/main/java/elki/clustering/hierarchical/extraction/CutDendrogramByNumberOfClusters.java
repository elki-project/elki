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
import elki.clustering.hierarchical.PointerHierarchyRepresentationResult;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.database.ids.DBIDArrayIter;
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
 * @assoc - - - PointerHierarchyRepresentationResult
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
    super(algorithm, hierarchical);
    this.minclusters = minclusters;
  }

  @Override
  public Clustering<DendrogramModel> run(PointerHierarchyRepresentationResult pointerresult) {
    Clustering<DendrogramModel> result = new Instance(pointerresult).extractClusters();
    Metadata.hierarchyOf(result).addChild(pointerresult);
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
     * @param pointerresult Pointer result
     */
    public Instance(PointerHierarchyRepresentationResult pointerresult) {
      super(pointerresult);
    }

    @Override
    protected int findSplit(DBIDArrayIter it) {
      int split = ids.size() > minclusters ? ids.size() - minclusters : 0;
      it.seek(split);
      // Stop distance:
      final double stopdist = lambda.doubleValue(it);

      // Tie handling: decrement split.
      for(it.retract(); it.valid() && stopdist <= lambda.doubleValue(it); it.retract()) {
        split--;
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
  public static class Parameterizer extends AbstractCutDendrogram.Parameterizer {
    /**
     * The minimum number of clusters to extract.
     */
    public static final OptionID MINCLUSTERS_ID = new OptionID("hierarchical.minclusters", "The minimum number of clusters to extract (there may be more clusters when tied, and singletons may be merged into a noise cluster).");

    /**
     * Number of clusters to extract.
     */
    int minclusters = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter minclustersP = new IntParameter(MINCLUSTERS_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(minclustersP)) {
        minclusters = minclustersP.intValue();
      }
    }

    @Override
    protected CutDendrogramByNumberOfClusters makeInstance() {
      return new CutDendrogramByNumberOfClusters(algorithm, minclusters, hierarchical);
    }
  }
}
