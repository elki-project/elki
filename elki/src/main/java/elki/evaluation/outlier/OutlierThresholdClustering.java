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
package elki.evaluation.outlier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.DoubleRelation;
import elki.evaluation.Evaluator;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.result.outlier.OutlierResult;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleListParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.scaling.IdentityScaling;
import elki.utilities.scaling.ScalingFunction;
import elki.utilities.scaling.outlier.OutlierScaling;

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
  public void processNewResult(Object newResult) {
    List<OutlierResult> ors = ResultUtil.filterResults(newResult, OutlierResult.class);
    for(OutlierResult or : ors) {
      Metadata.hierarchyOf(or).addChild(split(or));
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
    Clustering<Model> result = new Clustering<>();
    Metadata.of(result).setLongName("Outlier threshold clustering");
    for(int i = 0; i <= threshold.length; i++) {
      String name = (i == 0) ? "Inlier" : "Outlier_" + threshold[i - 1];
      result.addToplevelCluster(new Cluster<>(name, idlists.get(i), (i > 0)));
    }
    return result;
  }

  /**
   * Parameterization helper
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
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
    public void configure(Parameterization config) {
      new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class) //
          .grab(config, x -> scaling = x);
      new DoubleListParameter(THRESHOLD_ID) //
          .grab(config, x -> threshold = x.clone());
    }

    @Override
    public OutlierThresholdClustering make() {
      return new OutlierThresholdClustering(scaling, threshold);
    }
  }
}
