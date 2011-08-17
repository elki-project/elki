package experimentalcode.shared.outlier.ensemble;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.util.ArrayList;
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
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.shared.outlier.ensemble.voting.EnsembleVoting;

/**
 * Simple outlier ensemble method.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf EnsembleVoting
 * @apiviz.uses OutlierResult oneway - - reads
 * @apiviz.uses OutlierResult oneway - - «create»
 * 
 * @param <O> object type
 */
public class OutlierEnsemble<O> extends AbstractAlgorithm<OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OutlierEnsemble.class);
  
  /**
   * Parameter for the individual algorithms
   */
  private ObjectListParameter<OutlierAlgorithm> ALGORITHMS_PARAM = new ObjectListParameter<OutlierAlgorithm>(OptionID.ALGORITHM, OutlierAlgorithm.class);

  /**
   * The actual algorithms
   */
  private List<OutlierAlgorithm> algorithms;

  /**
   * Voting strategy to use in the ensemble.
   */
  public final static OptionID VOTING_ID = OptionID.getOrCreateOptionID("ensemble.voting", "Voting strategy to use in the ensemble.");

  /**
   * Voting strategy parameter
   */
  private ObjectParameter<EnsembleVoting> VOTING_PARAM = new ObjectParameter<EnsembleVoting>(VOTING_ID, EnsembleVoting.class);

  /**
   * The voting in use.
   */
  private EnsembleVoting voting;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public OutlierEnsemble(Parameterization config) {
    super();
    config = config.descend(this);
    if(config.grab(ALGORITHMS_PARAM)) {
      ListParameterization subconfig = new ListParameterization();
      ChainedParameterization chain = new ChainedParameterization(subconfig, config);
      chain.errorsTo(config);
      algorithms = ALGORITHMS_PARAM.instantiateClasses(chain);
      subconfig.logAndClearReportedErrors();
    }
    if(config.grab(VOTING_PARAM)) {
      voting = VOTING_PARAM.instantiateClass(config);
    }
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    int num = algorithms.size();
    // Run inner outlier algorithms
    ModifiableDBIDs ids = DBIDUtil.newHashSet();
    ArrayList<OutlierResult> results = new ArrayList<OutlierResult>(num);
    {
      FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Inner outlier algorithms", num, logger) : null;
      for(Algorithm alg : algorithms) {
        Result res = alg.run(database);
        for(OutlierResult ors : ResultUtil.getOutlierResults(res)) {
          results.add(ors);
          ids.addDBIDs(ors.getScores().getDBIDs());
        }
        if(prog != null) {
          prog.incrementProcessed(logger);
        }
      }
      if(prog != null) {
        prog.ensureCompleted(logger);
      }
    }
    // Combine
    WritableDataStore<Double> sumscore = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_STATIC, Double.class);
    DoubleMinMax minmax = new DoubleMinMax();
    {
      FiniteProgress cprog = logger.isVerbose() ? new FiniteProgress("Combining results", ids.size(), logger) : null;
      for(DBID id : ids) {
        ArrayList<Double> scores = new ArrayList<Double>(num);
        for(OutlierResult r : results) {
          Double score = r.getScores().get(id);
          if(score != null) {
            scores.add(score);
          }
          else {
            logger.warning("DBID " + id + " was not given a score by result " + r);
          }
        }
        if(scores.size() > 0) {
          double combined = voting.combine(scores);
          sumscore.put(id, combined);
          minmax.put(combined);
        }
        else {
          logger.warning("DBID " + id + " was not given any score at all.");
        }
        if(cprog != null) {
          cprog.incrementProcessed(logger);
        }
      }
      if(cprog != null) {
        cprog.ensureCompleted(logger);
      }
    }
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    Relation<Double> scores = new MaterializedRelation<Double>("Outlier Ensemble", "ensemble-outlier", TypeUtil.DOUBLE, sumscore, ids);
    return new OutlierResult(meta, scores);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    TypeInformation[] trs = new TypeInformation[algorithms.size()];
    for (int i = 0; i < trs.length; i++) {
      // FIXME: only allow single-input algorithms?
      trs[i] = algorithms.get(i).getInputTypeRestriction()[0];
    }
    return TypeUtil.array(new CombinedTypeInformation(trs));
  }
}