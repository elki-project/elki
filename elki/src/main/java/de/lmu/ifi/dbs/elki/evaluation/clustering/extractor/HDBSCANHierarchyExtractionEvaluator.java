package de.lmu.ifi.dbs.elki.evaluation.clustering.extractor;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerDensityHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.HDBSCANHierarchyExtraction;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.evaluation.clustering.extractor.ExtractFlatClusteringFromHierarchyEvaluator.DummyHierarchicalClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * Extract clusters from a hierarchical clustering, during the evaluation phase.
 *
 * Usually, it is more elegant to use {@link HDBSCANHierarchyExtraction} as
 * primary algorithm. But in order to extract <em>multiple</em> partitionings
 * from the same clustering, this can be useful.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class HDBSCANHierarchyExtractionEvaluator implements Evaluator {
  /**
   * Class to perform the cluster extraction.
   */
  private HDBSCANHierarchyExtraction inner;

  /**
   * Constructor.
   *
   * @param inner Inner algorithm instance.
   */
  public HDBSCANHierarchyExtractionEvaluator(HDBSCANHierarchyExtraction inner) {
    this.inner = inner;
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    List<PointerHierarchyRepresentationResult> hrs = ResultUtil.filterResults(hier, newResult, PointerHierarchyRepresentationResult.class);
    for(PointerHierarchyRepresentationResult pointerresult : hrs) {
      DBIDs ids = pointerresult.getDBIDs();
      DBIDDataStore pi = pointerresult.getParentStore();
      DoubleDataStore lambda = pointerresult.getParentDistanceStore();
      DoubleDataStore coredist = null;
      if(pointerresult instanceof PointerDensityHierarchyRepresentationResult) {
        coredist = ((PointerDensityHierarchyRepresentationResult) pointerresult).getCoreDistanceStore();
      }

      Clustering<DendrogramModel> result = inner.extractClusters(ids, pi, lambda, coredist);
      pointerresult.addChildResult(result);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Inner algorithm to extract a clustering.
     */
    HDBSCANHierarchyExtraction inner;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ListParameterization overrides = new ListParameterization();
      overrides.addParameter(AlgorithmStep.Parameterizer.ALGORITHM_ID, DummyHierarchicalClusteringAlgorithm.class);
      ChainedParameterization list = new ChainedParameterization(overrides, config);
      inner = ClassGenericsUtil.parameterizeOrAbort(HDBSCANHierarchyExtraction.class, list);
    }

    @Override
    protected HDBSCANHierarchyExtractionEvaluator makeInstance() {
      return new HDBSCANHierarchyExtractionEvaluator(inner);
    }
  }
}
