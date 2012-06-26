package de.lmu.ifi.dbs.elki.evaluation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Collection;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.clustering.EvaluateClustering;
import de.lmu.ifi.dbs.elki.evaluation.histogram.ComputeOutlierHistogram;
import de.lmu.ifi.dbs.elki.evaluation.outlier.OutlierPrecisionAtKCurve;
import de.lmu.ifi.dbs.elki.evaluation.outlier.OutlierPrecisionRecallCurve;
import de.lmu.ifi.dbs.elki.evaluation.outlier.OutlierROCCurve;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.scaling.LinearScaling;

/**
 * Evaluator that tries to auto-run a number of evaluation methods.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses OutlierResult
 * @apiviz.uses Clustering
 * @apiviz.composedOf OutlierROCCurve
 * @apiviz.composedOf OutlierPrecisionAtKCurve
 * @apiviz.composedOf OutlierPrecisionRecallCurve
 * @apiviz.composedOf ComputeOutlierHistogram
 * @apiviz.composedOf EvaluateClustering
 */
public class AutomaticEvaluation implements Evaluator {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(AutomaticEvaluation.class);

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    Database db = ResultUtil.findDatabase(baseResult);
    // Note: this *may* reinvoke this method!
    ResultUtil.ensureClusteringResult(db, baseResult);
    autoEvaluateClusterings(baseResult, newResult);
    autoEvaluateOutliers(baseResult, newResult);
  }

  protected void autoEvaluateOutliers(HierarchicalResult baseResult, Result newResult) {
    Collection<OutlierResult> outliers = ResultUtil.filterResults(newResult, OutlierResult.class);
    if(logger.isDebugging()) {
      logger.debug("Number of new outlier results: " + outliers.size());
    }
    if(outliers.size() > 0) {
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(baseResult, Clustering.class);
      if(clusterings.size() == 0) {
        logger.warning("Could not find a clustering result, even after running 'ensureClusteringResult'?!?");
        return;
      }
      Clustering<?> basec = clusterings.iterator().next();
      // Find minority class label
      int min = Integer.MAX_VALUE;
      int total = 0;
      String label = null;
      if(basec.getAllClusters().size() > 1) {
        for(Cluster<?> c : basec.getAllClusters()) {
          final int csize = c.getIDs().size();
          total += csize;
          if(csize < min) {
            min = csize;
            label = c.getName();
          }
        }
      }
      if(label == null) {
        logger.warning("Could not evaluate outlier results, as I could not find a minority label.");
        return;
      }
      if(min == 1) {
        logger.warning("The minority class label had a single object. Try using 'ClassLabelFilter' to identify the class label column.");
      }
      if(min > 0.05 * total) {
        logger.warning("The minority class I discovered (labeled '" + label + "') has " + (min * 100. / total) + "% of objects. Outlier classes should be more rare!");
      }
      logger.verbose("Evaluating using minority class: " + label);
      Pattern pat = Pattern.compile("^" + Pattern.quote(label) + "$");
      // Compute ROC curve
      new OutlierROCCurve(pat).processNewResult(baseResult, newResult);
      // Compute Precision at k
      new OutlierPrecisionAtKCurve(pat, min * 2).processNewResult(baseResult, newResult);
      // Compute ROC curve
      new OutlierPrecisionRecallCurve(pat).processNewResult(baseResult, newResult);
      // Compute outlier histogram
      new ComputeOutlierHistogram(pat, 50, new LinearScaling(), false).processNewResult(baseResult, newResult);
    }
  }

  protected void autoEvaluateClusterings(HierarchicalResult baseResult, Result newResult) {
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(newResult, Clustering.class);
    if(logger.isDebugging()) {
      logger.warning("Number of new clustering results: " + clusterings.size());
    }
    if (clusterings.size() > 0) {
      new EvaluateClustering(new ByLabelHierarchicalClustering(), false, true).processNewResult(baseResult, newResult);
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected AutomaticEvaluation makeInstance() {
      return new AutomaticEvaluation();
    }
  }
}
