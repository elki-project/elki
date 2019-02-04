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
package de.lmu.ifi.dbs.elki.algorithm.outlier.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.ensemble.EnsembleVoting;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Simple outlier ensemble method.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @composed - - - EnsembleVoting
 * @navassoc - reads - OutlierResult
 * @navassoc - create - OutlierResult
 */
public class SimpleOutlierEnsemble extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SimpleOutlierEnsemble.class);

  /**
   * The algorithms to run.
   */
  private List<OutlierAlgorithm> algorithms;

  /**
   * The voting in use.
   */
  private EnsembleVoting voting;

  /**
   * Constructor.
   * 
   * @param algorithms Algorithms to run
   * @param voting Voting method
   */
  public SimpleOutlierEnsemble(List<OutlierAlgorithm> algorithms, EnsembleVoting voting) {
    this.algorithms = algorithms;
    this.voting = voting;
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    int num = algorithms.size();
    // Run inner outlier algorithms
    ModifiableDBIDs ids = DBIDUtil.newHashSet();
    ArrayList<OutlierResult> results = new ArrayList<>(num);
    {
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Inner outlier algorithms", num, LOG) : null;
      for (Algorithm alg : algorithms) {
        Result res = alg.run(database);
        List<OutlierResult> ors = OutlierResult.getOutlierResults(res);
        for (OutlierResult or : ors) {
          results.add(or);
          ids.addDBIDs(or.getScores().getDBIDs());
        }
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
    }
    // Combine
    WritableDoubleDataStore sumscore = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();
    {
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Combining results", ids.size(), LOG) : null;
      for (DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        double[] scores = new double[num];
        int i = 0;
        for (OutlierResult r : results) {
          double score = r.getScores().doubleValue(id);
          if (!Double.isNaN(score)) {
            scores[i] = score;
            i++;
          } else {
            LOG.warning("DBID " + id + " was not given a score by result " + r);
          }
        }
        if (i > 0) {
          // Shrink array if necessary.
          if (i < scores.length) {
            scores = Arrays.copyOf(scores, i);
          }
          double combined = voting.combine(scores);
          sumscore.putDouble(id, combined);
          minmax.put(combined);
        } else {
          LOG.warning("DBID " + id + " was not given any score at all.");
        }
        LOG.incrementProcessed(cprog);
      }
      LOG.ensureCompleted(cprog);
    }
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    DoubleRelation scores = new MaterializedDoubleRelation("Simple Outlier Ensemble", "ensemble-outlier", sumscore, ids);
    return new OutlierResult(meta, scores);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    TypeInformation[] trs = new TypeInformation[algorithms.size()];
    for (int i = 0; i < trs.length; i++) {
      // FIXME: what if an algorithm needs more than one input data source?
      trs[i] = algorithms.get(i).getInputTypeRestriction()[0];
    }
    return TypeUtil.array(new CombinedTypeInformation(trs));
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Voting strategy to use in the ensemble.
     */
    public static final OptionID VOTING_ID = new OptionID("ensemble.voting", "Voting strategy to use in the ensemble.");

    /**
     * The algorithms to run.
     */
    private List<OutlierAlgorithm> algorithms;

    /**
     * The voting in use.
     */
    private EnsembleVoting voting;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectListParameter<OutlierAlgorithm> algP = new ObjectListParameter<>(AbstractAlgorithm.ALGORITHM_ID, OutlierAlgorithm.class);
      if (config.grab(algP)) {
        ListParameterization subconfig = new ListParameterization();
        ChainedParameterization chain = new ChainedParameterization(subconfig, config);
        chain.errorsTo(config);
        algorithms = algP.instantiateClasses(chain);
        subconfig.logAndClearReportedErrors();
      }
      ObjectParameter<EnsembleVoting> votingP = new ObjectParameter<>(VOTING_ID, EnsembleVoting.class);
      if (config.grab(votingP)) {
        voting = votingP.instantiateClass(config);
      }
    }

    @Override
    protected SimpleOutlierEnsemble makeInstance() {
      return new SimpleOutlierEnsemble(algorithms, voting);
    }
  }
}
