/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import elki.Algorithm;
import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.ensemble.EnsembleVoting;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectListParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
public class SimpleOutlierEnsemble implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SimpleOutlierEnsemble.class);

  /**
   * The algorithms to run.
   */
  private List<? extends OutlierAlgorithm> algorithms;

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
  public SimpleOutlierEnsemble(List<? extends OutlierAlgorithm> algorithms, EnsembleVoting voting) {
    this.algorithms = algorithms;
    this.voting = voting;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    TypeInformation[] trs = new TypeInformation[algorithms.size()];
    for(int i = 0; i < trs.length; i++) {
      // FIXME: what if an algorithm needs more than one input data source?
      trs[i] = algorithms.get(i).getInputTypeRestriction()[0];
    }
    return TypeUtil.array(new CombinedTypeInformation(trs));
  }

  @Override
  public OutlierResult autorun(Database database) throws IllegalStateException {
    int num = algorithms.size();
    // Run inner outlier algorithms
    ModifiableDBIDs ids = DBIDUtil.newHashSet();
    ArrayList<OutlierResult> results = new ArrayList<>(num);
    {
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Inner outlier algorithms", num, LOG) : null;
      for(OutlierAlgorithm alg : algorithms) {
        OutlierResult or = alg.autorun(database);
        results.add(or);
        ids.addDBIDs(or.getScores().getDBIDs());
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
    }
    // Combine
    WritableDoubleDataStore sumscore = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();
    {
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Combining results", ids.size(), LOG) : null;
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        double[] scores = new double[num];
        int i = 0;
        for(OutlierResult r : results) {
          double score = r.getScores().doubleValue(id);
          if(!Double.isNaN(score)) {
            scores[i] = score;
            i++;
          }
          else {
            LOG.warning("DBID " + id + " was not given a score by result " + r);
          }
        }
        if(i > 0) {
          // Shrink array if necessary.
          if(i < scores.length) {
            scores = Arrays.copyOf(scores, i);
          }
          double combined = voting.combine(scores);
          sumscore.putDouble(id, combined);
          minmax.put(combined);
        }
        else {
          LOG.warning("DBID " + id + " was not given any score at all.");
        }
        LOG.incrementProcessed(cprog);
      }
      LOG.ensureCompleted(cprog);
    }
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    DoubleRelation scores = new MaterializedDoubleRelation("Simple Outlier Ensemble", ids, sumscore);
    return new OutlierResult(meta, scores);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Voting strategy to use in the ensemble.
     */
    public static final OptionID VOTING_ID = new OptionID("ensemble.voting", "Voting strategy to use in the ensemble.");

    /**
     * The algorithms to run.
     */
    private List<? extends OutlierAlgorithm> algorithms;

    /**
     * The voting in use.
     */
    private EnsembleVoting voting;

    @Override
    public void configure(Parameterization config) {
      new ObjectListParameter<OutlierAlgorithm>(Algorithm.Utils.ALGORITHM_ID, OutlierAlgorithm.class) //
          .grab(config, x -> algorithms = x);
      new ObjectParameter<EnsembleVoting>(VOTING_ID, EnsembleVoting.class) //
          .grab(config, x -> voting = x);
    }

    @Override
    public SimpleOutlierEnsemble make() {
      return new SimpleOutlierEnsemble(algorithms, voting);
    }
  }
}
