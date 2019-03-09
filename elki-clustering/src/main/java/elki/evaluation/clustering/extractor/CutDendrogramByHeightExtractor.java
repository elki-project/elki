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

import elki.algorithm.AbstractAlgorithm;
import elki.algorithm.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import elki.algorithm.clustering.hierarchical.extraction.CutDendrogramByHeight;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.evaluation.Evaluator;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.parameterization.ChainedParameterization;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Extract clusters from a hierarchical clustering, during the evaluation phase.
 * <p>
 * Usually, it is more elegant to use {@link CutDendrogramByHeight} as primary
 * algorithm. But in order to extract <em>multiple</em> partitionings from the
 * same clustering, this can be useful.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class CutDendrogramByHeightExtractor implements Evaluator {
  /**
   * Class to perform the cluster extraction.
   */
  private CutDendrogramByHeight inner;

  /**
   * Constructor.
   *
   * @param inner Inner algorithm instance.
   */
  public CutDendrogramByHeightExtractor(CutDendrogramByHeight inner) {
    this.inner = inner;
  }

  @Override
  public void processNewResult(Object newResult) {
    ArrayList<PointerHierarchyRepresentationResult> hrs = ResultUtil.filterResults(newResult, PointerHierarchyRepresentationResult.class);
    for(PointerHierarchyRepresentationResult pointerresult : hrs) {
      Metadata.hierarchyOf(pointerresult).addChild(inner.run(pointerresult));
    }
  }

  /**
   * Dummy instance.
   *
   * @author Erich Schubert
   * 
   * @hidden
   */
  protected static class DummyHierarchicalClusteringAlgorithm implements HierarchicalClusteringAlgorithm {
    /**
     * Constructor.
     */
    public DummyHierarchicalClusteringAlgorithm() {
      super();
    }

    @Override
    public TypeInformation[] getInputTypeRestriction() {
      return TypeUtil.array();
    }

    @Override
    public PointerHierarchyRepresentationResult run(Database db) {
      throw new AbortException("This must not be called");
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Inner algorithm to extract a clustering.
     */
    CutDendrogramByHeight inner;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ListParameterization overrides = new ListParameterization();
      overrides.addParameter(AbstractAlgorithm.ALGORITHM_ID, DummyHierarchicalClusteringAlgorithm.class);
      ChainedParameterization list = new ChainedParameterization(overrides, config);
      list.errorsTo(config);
      inner = list.tryInstantiate(CutDendrogramByHeight.class);
    }

    @Override
    protected CutDendrogramByHeightExtractor makeInstance() {
      return new CutDendrogramByHeightExtractor(inner);
    }
  }
}
