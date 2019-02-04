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
package de.lmu.ifi.dbs.elki.evaluation.outlier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScaling;

/**
 * Pseudo clustering algorithm that builds clusters based on their outlier
 * score. Useful for transforming a numeric outlier score into a 2-class
 * dataset.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class OutlierThresholdClustering implements Evaluator {
  /**
   * Scaling function to use
   */
  ScalingFunction scaling = null;

  /**
   * Thresholds to use
   */
  double[] threshold;

  /**
   * Constructor.
   * 
   * @param scaling Scaling function
   * @param threshold Threshold
   */
  public OutlierThresholdClustering(ScalingFunction scaling, double[] threshold) {
    super();
    this.scaling = scaling;
    this.threshold = threshold;
    Arrays.sort(this.threshold);
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    List<OutlierResult> ors = ResultUtil.filterResults(hier, newResult, OutlierResult.class);
    for(OutlierResult or : ors) {
      hier.add(or, split(or));
    }
  }

  private Clustering<Model> split(OutlierResult or) {
    DoubleRelation scores = or.getScores();
    if(scaling instanceof OutlierScaling) {
      ((OutlierScaling) scaling).prepare(or);
    }
    ArrayList<ModifiableDBIDs> idlists = new ArrayList<>(threshold.length + 1);
    for(int i = 0; i <= threshold.length; i++) {
      idlists.add(DBIDUtil.newHashSet());
    }
    for(DBIDIter iter = scores.getDBIDs().iter(); iter.valid(); iter.advance()) {
      double score = scores.doubleValue(iter);
      if(scaling != null) {
        score = scaling.getScaled(score);
      }
      int i = 0;
      for(; i < threshold.length; i++) {
        if(score < threshold[i]) {
          break;
        }
      }
      idlists.get(i).add(iter);
    }
    Clustering<Model> c = new Clustering<>("Outlier threshold clustering", "threshold-clustering");
    for(int i = 0; i <= threshold.length; i++) {
      String name = (i == 0) ? "Inlier" : "Outlier_" + threshold[i - 1];
      c.addToplevelCluster(new Cluster<>(name, idlists.get(i), (i > 0)));
    }
    return c;
  }

  /**
   * Parameterization helper
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify a scaling function to use.
     */
    public static final OptionID SCALING_ID = new OptionID("thresholdclust.scaling", "Class to use as scaling function.");

    /**
     * Parameter to specify the threshold
     */
    public static final OptionID THRESHOLD_ID = new OptionID("thresholdclust.threshold", "Threshold(s) to apply.");

    /**
     * Scaling function to use
     */
    ScalingFunction scaling = null;

    /**
     * Threshold to use
     */
    double[] threshold;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }

      DoubleListParameter thresholdP = new DoubleListParameter(THRESHOLD_ID);
      if(config.grab(thresholdP)) {
        threshold = thresholdP.getValue().clone();
      }
    }

    @Override
    protected OutlierThresholdClustering makeInstance() {
      return new OutlierThresholdClustering(scaling, threshold);
    }
  }
}