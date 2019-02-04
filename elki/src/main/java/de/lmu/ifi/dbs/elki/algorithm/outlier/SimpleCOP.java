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
package de.lmu.ifi.dbs.elki.algorithm.outlier;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.times;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import net.jafama.FastMath;

/**
 * Algorithm to compute local correlation outlier probability.
 * <p>
 * This is the simpler, original version of COP, as published in
 * <p>
 * Reference:
 * <p>
 * Arthur Zimek<br>
 * Application 2: Outlier Detection (Chapter 18)<br>
 * Correlation Clustering
 * <p>
 * which has then been refined to the method published as {@link COP}
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("Simple COP: Correlation Outlier Probability")
@Reference(authors = "Arthur Zimek", //
    title = "Application 2: Outlier Detection (Chapter 18)", //
    booktitle = "Correlation Clustering", //
    bibkey = "phd/dnb/Zimek08/Ch18")
public class SimpleCOP<V extends NumberVector> extends AbstractDistanceBasedAlgorithm<V, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SimpleCOP.class);

  /**
   * Number of neighbors to be considered.
   */
  int k;

  /**
   * Holds the object performing the dependency derivation
   */
  private DependencyDerivator<V> dependencyDerivator;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param k k Parameter
   * @param pca PCA runner
   * @param filter Filter for selecting eigenvectors
   */
  public SimpleCOP(DistanceFunction<? super V> distanceFunction, int k, PCARunner pca, EigenPairFilter filter) {
    super(distanceFunction);
    this.k = k;
    this.dependencyDerivator = new DependencyDerivator<>(null, FormatUtil.NF, pca, filter, 0, false);
  }

  public OutlierResult run(Database database, Relation<V> data) throws IllegalStateException {
    KNNQuery<V> knnQuery = QueryUtil.getKNNQuery(data, getDistanceFunction(), k + 1);

    DBIDs ids = data.getDBIDs();

    WritableDoubleDataStore cop_score = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDataStore<double[]> cop_err_v = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, double[].class);
    WritableDataStore<double[]> cop_datav = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, double[].class);
    WritableIntegerDataStore cop_dim = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, -1);
    WritableDataStore<CorrelationAnalysisSolution<?>> cop_sol = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, CorrelationAnalysisSolution.class);
    {// compute neighbors of each db object
      FiniteProgress progressLocalPCA = LOG.isVerbose() ? new FiniteProgress("Correlation Outlier Probabilities", data.size(), LOG) : null;
      double sqrt2 = MathUtil.SQRT2;
      for(DBIDIter id = data.iterDBIDs(); id.valid(); id.advance()) {
        KNNList neighbors = knnQuery.getKNNForDBID(id, k + 1);
        ModifiableDBIDs nids = DBIDUtil.newArray(neighbors);
        nids.remove(id);

        // TODO: do we want to use the query point as centroid?
        CorrelationAnalysisSolution<V> depsol = dependencyDerivator.generateModel(data, nids);

        double stddev = depsol.getStandardDeviation();
        double distance = FastMath.sqrt(depsol.squaredDistance(data.get(id)));
        double prob = NormalDistribution.erf(distance / (stddev * sqrt2));

        cop_score.putDouble(id, prob);

        cop_err_v.put(id, times(depsol.errorVector(data.get(id)), -1));

        cop_datav.put(id, depsol.dataVector(data.get(id)));

        cop_dim.putInt(id, depsol.getCorrelationDimensionality());

        cop_sol.put(id, depsol);

        LOG.incrementProcessed(progressLocalPCA);
      }
      LOG.ensureCompleted(progressLocalPCA);
    }
    // combine results.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Original Correlation Outlier Probabilities", "origcop-outlier", cop_score, ids);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    // extra results
    result.addChildResult(new MaterializedRelation<>("Local Dimensionality", COP.COP_DIM, TypeUtil.INTEGER, cop_dim, ids));
    result.addChildResult(new MaterializedRelation<>("Error vectors", COP.COP_ERRORVEC, TypeUtil.DOUBLE_ARRAY, cop_err_v, ids));
    result.addChildResult(new MaterializedRelation<>("Data vectors", "cop-datavec", TypeUtil.DOUBLE_ARRAY, cop_datav, ids));
    result.addChildResult(new MaterializedRelation<>("Correlation analysis", "cop-sol", new SimpleTypeInformation<CorrelationAnalysisSolution<?>>(CorrelationAnalysisSolution.class), cop_sol, ids));
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
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractDistanceBasedAlgorithm.Parameterizer<V> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its COP_SCORE, must be an integer greater
     * than 0.
     */
    public static final OptionID K_ID = new OptionID("cop.k", "The number of nearest neighbors of an object to be considered for computing its COP_SCORE.");

    /**
     * Parameter for the PCA runner class.
     */
    public static final OptionID PCARUNNER_ID = new OptionID("cop.pcarunner", "The class to compute (filtered) PCA.");

    /**
     * Number of neighbors to be considered.
     */
    int k;

    /**
     * Holds the object performing the dependency derivation
     */
    protected PCARunner pca;

    /**
     * Filter for selecting eigenvectors.
     */
    private EigenPairFilter filter;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }
      ObjectParameter<PCARunner> pcaP = new ObjectParameter<>(PCARUNNER_ID, PCARunner.class, PCARunner.class);
      if(config.grab(pcaP)) {
        pca = pcaP.instantiateClass(config);
      }
      ObjectParameter<EigenPairFilter> filterP = new ObjectParameter<>(EigenPairFilter.PCA_EIGENPAIR_FILTER, EigenPairFilter.class, PercentageEigenPairFilter.class);
      if(config.grab(filterP)) {
        filter = filterP.instantiateClass(config);
      }
    }

    @Override
    protected SimpleCOP<V> makeInstance() {
      return new SimpleCOP<>(distanceFunction, k, pca, filter);
    }
  }
}
