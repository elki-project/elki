package de.lmu.ifi.dbs.elki.algorithm.outlier.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.em.EM;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * outlier detection algorithm using EM Clustering. If an object does not belong
 * to any cluster it is supposed to be an outlier. If the probability for an
 * object to belong to the most probable cluster is still relatively low this
 * object is an outlier.
 * 
 * @author Lisa Reichert
 * 
 * @apiviz.has EM
 * 
 * @param <V> Vector type
 */
// TODO: re-use an existing EM when present?
@Title("EM Outlier: Outlier Detection based on the generic EM clustering")
@Description("The outlier score assigned is based on the highest cluster probability obtained from EM clustering.")
public class EMOutlier<V extends NumberVector> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EMOutlier.class);

  /**
   * Inner algorithm.
   */
  private EM<V, ?> emClustering;

  /**
   * Constructor with an existing em clustering algorithm.
   * 
   * @param emClustering EM clustering algorithm to use.
   */
  public EMOutlier(EM<V, ?> emClustering) {
    super();
    this.emClustering = emClustering;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   * 
   * @param database Database to process
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<V> relation) {
    emClustering.setSoft(true);
    Clustering<?> emresult = emClustering.run(database, relation);
    Relation<double[]> soft = null;
    for(Iter<Result> iter = emresult.getHierarchy().iterChildren(emresult); iter.valid(); iter.advance()) {
      if(!(iter.get() instanceof Relation)) {
        continue;
      }
      if(((Relation<?>) iter.get()).getDataTypeInformation() == EM.SOFT_TYPE) {
        @SuppressWarnings("unchecked")
        Relation<double[]> rel = (Relation<double[]>) iter.get();
        soft = rel;
      }
    }

    double globmax = 0.0;
    WritableDoubleDataStore emo_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double maxProb = Double.POSITIVE_INFINITY;
      double[] probs = soft.get(iditer);
      for(double prob : probs) {
        maxProb = Math.min(1. - prob, maxProb);
      }
      emo_score.putDouble(iditer, maxProb);
      globmax = Math.max(maxProb, globmax);
    }
    DoubleRelation scoreres = new MaterializedDoubleRelation("EM outlier scores", "em-outlier", emo_score, relation.getDBIDs());
    OutlierScoreMeta meta = new ProbabilisticOutlierScore(0.0, globmax);
    // combine results.
    OutlierResult result = new OutlierResult(meta, scoreres);
    // TODO: add a keep-EM flag?
    result.addChildResult(emresult);
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * EM clustering algorithm to run.
     */
    protected EM<V, ?> em;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Class<EM<V, ?>> cls = ClassGenericsUtil.uglyCastIntoSubclass(EM.class);
      em = config.tryInstantiate(cls);
    }

    @Override
    protected EMOutlier<V> makeInstance() {
      return new EMOutlier<>(em);
    }
  }
}
