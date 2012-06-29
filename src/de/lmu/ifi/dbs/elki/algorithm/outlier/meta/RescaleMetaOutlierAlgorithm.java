package de.lmu.ifi.dbs.elki.algorithm.outlier.meta;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * Scale another outlier score using the given scaling function.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf OutlierAlgorithm
 */
public class RescaleMetaOutlierAlgorithm extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(RescaleMetaOutlierAlgorithm.class);

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("metaoutlier.scaling", "Class to use as scaling function.");

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
    Result innerresult = algorithm.run(database);

    OutlierResult or = getOutlierResult(innerresult);
    final Relation<Double> scores = or.getScores();
    if(scaling instanceof OutlierScalingFunction) {
      ((OutlierScalingFunction) scaling).prepare(or);
    }

    WritableDoubleDataStore scaledscores = DataStoreUtil.makeDoubleStorage(scores.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);

    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iditer = scores.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double val = scaling.getScaled(scores.get(iditer));
      scaledscores.putDouble(iditer, val);
      minmax.put(val);
    }

    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), scaling.getMin(), scaling.getMax());
    Relation<Double> scoresult = new MaterializedRelation<Double>("Scaled Outlier", "scaled-outlier", TypeUtil.DOUBLE, scaledscores, scores.getDBIDs());
    OutlierResult result = new OutlierResult(meta, scoresult);
    result.addChildResult(innerresult);

    return result;
  }

  /**
   * Find an OutlierResult to work with.
   * 
   * @param result Result object
   * @return Iterator to work with
   */
  private OutlierResult getOutlierResult(Result result) {
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if(ors.size() > 0) {
      return ors.get(0);
    }
    throw new IllegalStateException("Comparison algorithm expected at least one outlier result.");
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return algorithm.getInputTypeRestriction();
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
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

      ObjectParameter<Algorithm> algP = new ObjectParameter<Algorithm>(OptionID.ALGORITHM, OutlierAlgorithm.class);
      if(config.grab(algP)) {
        algorithm = algP.instantiateClass(config);
      }

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class);
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