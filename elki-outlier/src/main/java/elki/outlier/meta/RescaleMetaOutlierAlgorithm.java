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
package elki.outlier.meta;

import elki.outlier.OutlierAlgorithm;
import elki.AbstractAlgorithm;
import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.result.Metadata;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.scaling.ScalingFunction;
import elki.utilities.scaling.outlier.OutlierScaling;

/**
 * Scale another outlier score using the given scaling function.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - OutlierAlgorithm
 */
public class RescaleMetaOutlierAlgorithm extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(RescaleMetaOutlierAlgorithm.class);

  /**
   * Parameter to specify a scaling function to use.
   */
  public static final OptionID SCALING_ID = new OptionID("metaoutlier.scaling", "Class to use as scaling function.");

  /**
   * Holds the algorithm to run.
   */
  private Algorithm algorithm;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  /**
   * Constructor.
   * 
   * @param algorithm Inner algorithm
   * @param scaling Scaling to apply.
   */
  public RescaleMetaOutlierAlgorithm(Algorithm algorithm, ScalingFunction scaling) {
    super();
    this.algorithm = algorithm;
    this.scaling = scaling;
  }

  @Override
  public OutlierResult run(Database database) {
    Object innerresult = algorithm.run(database);

    OutlierResult or = getOutlierResult(innerresult);
    final DoubleRelation scores = or.getScores();
    if(scaling instanceof OutlierScaling) {
      ((OutlierScaling) scaling).prepare(or);
    }

    WritableDoubleDataStore scaledscores = DataStoreUtil.makeDoubleStorage(scores.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);

    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iditer = scores.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double val = scaling.getScaled(scores.doubleValue(iditer));
      scaledscores.putDouble(iditer, val);
      minmax.put(val);
    }

    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), scaling.getMin(), scaling.getMax());
    DoubleRelation scoresult = new MaterializedDoubleRelation("Scaled Outlier", scores.getDBIDs(), scaledscores);
    OutlierResult result = new OutlierResult(meta, scoresult);
    Metadata.hierarchyOf(result).addChild(innerresult);
    return result;
  }

  /**
   * Find an OutlierResult to work with.
   * 
   * @param result Result object
   * 
   * @return Iterator to work with
   */
  private OutlierResult getOutlierResult(Object result) {
    It<OutlierResult> it = Metadata.hierarchyOf(result).iterDescendantsSelf().filter(OutlierResult.class);
    if(it.valid()) {
      return it.get();
    }
    throw new IllegalStateException("Comparison algorithm expected at least one outlier result.");
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return algorithm.getInputTypeRestriction();
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Holds the algorithm to run.
     */
    private Algorithm algorithm;

    /**
     * Scaling function to use
     */
    private ScalingFunction scaling;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<Algorithm> algP = new ObjectParameter<>(AbstractAlgorithm.ALGORITHM_ID, OutlierAlgorithm.class);
      if(config.grab(algP)) {
        algorithm = algP.instantiateClass(config);
      }

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<>(SCALING_ID, ScalingFunction.class);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }
    }

    @Override
    protected RescaleMetaOutlierAlgorithm makeInstance() {
      return new RescaleMetaOutlierAlgorithm(algorithm, scaling);
    }
  }
}
