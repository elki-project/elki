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
package de.lmu.ifi.dbs.elki.application.greedyensemble;

import java.util.ArrayList;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListMIter;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.WeightedPearsonCorrelationDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.WeightedEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.WeightedManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.WeightedSquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.scores.ROCEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DecreasingVectorIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.VectorNonZero;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.ensemble.EnsembleVoting;
import de.lmu.ifi.dbs.elki.utilities.ensemble.EnsembleVotingMean;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScaling;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Class to load an outlier detection summary file, as produced by
 * {@link ComputeKNNOutlierScores}, and compute a naive ensemble for it. Based
 * on this initial estimation, and optimized ensemble is built using a greedy
 * strategy. Starting with the best candidate only as initial ensemble, the most
 * diverse candidate is investigated at each step. If it improves towards the
 * (estimated) target vector, it is added, otherwise it is discarded.
 * <p>
 * This approach is naive, and it may be surprising that it can improve results.
 * The reason is probably that diversity will result in a comparable ensemble,
 * while the reduced ensemble size is actually responsible for the improvements,
 * by being more decisive and less noisy due to dropping "unhelpful" members.
 * <p>
 * This still leaves quite a bit of room for improvement. If you build upon this
 * basic approach, please acknowledge our proof of concept work.
 * <P>
 * Reference:
 * <p>
 * Erich Schubert, Remigius Wojdanowski, Arthur Zimek, Hans-Peter Kriegel<br>
 * On Evaluation of Outlier Rankings and Outlier Scores<br>
 * Proc. 12th SIAM Int. Conf. on Data Mining (SDM 2012)
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "Erich Schubert, Remigius Wojdanowski, Arthur Zimek, Hans-Peter Kriegel", //
    title = "On Evaluation of Outlier Rankings and Outlier Scores", //
    booktitle = "Proc. 12th SIAM Int. Conf. on Data Mining (SDM 2012)", //
    url = "https://doi.org/10.1137/1.9781611972825.90", //
    bibkey = "DBLP:conf/sdm/SchubertWZK12")
public class GreedyEnsembleExperiment extends AbstractApplication {
  /**
   * Get static logger.
   */
  private static final Logging LOG = Logging.getLogger(GreedyEnsembleExperiment.class);

  /**
   * The data input part.
   */
  private InputStep inputstep;

  /**
   * Variant, where the truth vector is also updated.
   */
  boolean refine_truth = false;

  /**
   * Ensemble voting method.
   */
  EnsembleVoting voting;

  /**
   * Outlier scaling to apply during preprocessing.
   */
  ScalingFunction prescaling;

  /**
   * Outlier scaling to apply to constructed ensembles.
   */
  ScalingFunction scaling;

  /**
   * Expected rate of outliers.
   */
  double rate;

  /**
   * Minimum votes.
   */
  int minvote = 1;

  /**
   * Distance modes.
   */
  public enum Distance {
    PEARSON, //
    SQEUCLIDEAN, //
    EUCLIDEAN, //
    MANHATTAN, //
  }

  /**
   * Distance in use.
   */
  Distance distance = Distance.PEARSON;

  /**
   * Constructor.
   *
   * @param inputstep Input step
   * @param voting Ensemble voting
   * @param distance Distance function
   * @param prescaling Scaling to apply to input data
   * @param scaling Scaling to apply to ensemble members
   * @param rate Expected rate of outliers
   */
  public GreedyEnsembleExperiment(InputStep inputstep, EnsembleVoting voting, Distance distance, ScalingFunction prescaling, ScalingFunction scaling, double rate) {
    super();
    this.inputstep = inputstep;
    this.voting = voting;
    this.distance = distance;
    this.prescaling = prescaling;
    this.scaling = scaling;
    this.rate = rate;
  }

  @Override
  public void run() {
    // Note: the database contains the *result vectors*, not the original data.
    final Database database = inputstep.getDatabase();
    Relation<NumberVector> relation = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    final Relation<String> labels = DatabaseUtil.guessLabelRepresentation(database);
    final DBID firstid = DBIDUtil.deref(labels.iterDBIDs());
    final String firstlabel = labels.get(firstid);
    if(!firstlabel.matches("bylabel")) {
      throw new AbortException("No 'by label' reference outlier found, which is needed for weighting!");
    }
    relation = applyPrescaling(prescaling, relation, firstid);
    final int numcand = relation.size() - 1;

    // Dimensionality and reference vector
    final int dim = RelationUtil.dimensionality(relation);
    final NumberVector refvec = relation.get(firstid);

    // Build the positive index set for ROC AUC.
    VectorNonZero positive = new VectorNonZero(refvec);

    final int desired_outliers = (int) (rate * dim);
    int union_outliers = 0;
    final int[] outliers_seen = new int[dim];
    // Merge the top-k for each ensemble member, until we have enough
    // candidates.
    {
      int k = 0;
      ArrayList<DecreasingVectorIter> iters = new ArrayList<>(numcand);
      if(minvote >= numcand) {
        minvote = Math.max(1, numcand - 1);
      }
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        // Skip "by label", obviously
        if(DBIDUtil.equal(firstid, iditer)) {
          continue;
        }
        iters.add(new DecreasingVectorIter(relation.get(iditer)));
      }
      loop: while(union_outliers < desired_outliers) {
        for(DecreasingVectorIter iter : iters) {
          if(!iter.valid()) {
            LOG.warning("Union_outliers=" + union_outliers + " < desired_outliers=" + desired_outliers + " minvote=" + minvote);
            break loop;
          }
          int cur = iter.dim();
          outliers_seen[cur] += 1;
          if(outliers_seen[cur] == minvote) {
            union_outliers += 1;
          }
          iter.advance();
        }
        k++;
      }
      LOG.verbose("Merged top " + k + " outliers to: " + union_outliers + " outliers (desired: at least " + desired_outliers + ")");
    }
    // Build the final weight vector.
    final double[] estimated_weights = new double[dim];
    final double[] estimated_truth = new double[dim];
    updateEstimations(outliers_seen, union_outliers, estimated_weights, estimated_truth);
    DoubleVector estimated_truth_vec = DoubleVector.wrap(estimated_truth);

    PrimitiveDistanceFunction<NumberVector> wdist = getDistanceFunction(estimated_weights);
    PrimitiveDistanceFunction<NumberVector> tdist = wdist;

    // Build the naive ensemble:
    final double[] naiveensemble = new double[dim];
    {
      double[] buf = new double[numcand];
      for(int d = 0; d < dim; d++) {
        int i = 0;
        for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
          if(DBIDUtil.equal(firstid, iditer)) {
            continue;
          }
          final NumberVector vec = relation.get(iditer);
          buf[i] = vec.doubleValue(d);
          i++;
        }
        naiveensemble[d] = voting.combine(buf, i);
        if(Double.isNaN(naiveensemble[d])) {
          LOG.warning("NaN after combining: " + FormatUtil.format(buf) + " i=" + i + " " + voting.toString());
        }
      }
    }
    DoubleVector naivevec = DoubleVector.wrap(naiveensemble);

    // Compute single AUC scores and estimations.
    // Remember the method most similar to the estimation
    double bestauc = 0.0;
    String bestaucstr = "";
    double bestcost = Double.POSITIVE_INFINITY;
    String bestcoststr = "";
    DBID bestid = null;
    double bestest = Double.POSITIVE_INFINITY;
    {
      final double[] greedyensemble = new double[dim];
      // Compute individual scores
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        if(DBIDUtil.equal(firstid, iditer)) {
          continue;
        }
        // fout.append(labels.get(id));
        final NumberVector vec = relation.get(iditer);
        singleEnsemble(greedyensemble, vec);
        double auc = ROCEvaluation.computeROCAUC(positive, new DecreasingVectorIter(DoubleVector.wrap(greedyensemble)));
        double estimated = wdist.distance(DoubleVector.wrap(greedyensemble), estimated_truth_vec);
        double cost = tdist.distance(DoubleVector.wrap(greedyensemble), refvec);
        LOG.verbose("ROC AUC: " + auc + " estimated " + estimated + " cost " + cost + " " + labels.get(iditer));
        if(auc > bestauc) {
          bestauc = auc;
          bestaucstr = labels.get(iditer);
        }
        if(cost < bestcost) {
          bestcost = cost;
          bestcoststr = labels.get(iditer);
        }
        if(estimated < bestest || bestid == null) {
          bestest = estimated;
          bestid = DBIDUtil.deref(iditer);
        }
      }
    }

    // Initialize ensemble with "best" method
    if(prescaling != null) {
      LOG.verbose("Input prescaling: " + prescaling);
    }
    LOG.verbose("Distance function: " + wdist);
    LOG.verbose("Ensemble voting: " + voting);
    if(scaling != null) {
      LOG.verbose("Ensemble rescaling: " + scaling);
    }
    LOG.verbose("Initial estimation of outliers: " + union_outliers);
    LOG.verbose("Initializing ensemble with: " + labels.get(bestid));
    ModifiableDBIDs ensemble = DBIDUtil.newArray(bestid);
    ModifiableDBIDs enscands = DBIDUtil.newHashSet(relation.getDBIDs());
    ModifiableDBIDs dropped = DBIDUtil.newHashSet(relation.size());
    dropped.add(firstid);
    enscands.remove(bestid);
    enscands.remove(firstid);
    final double[] greedyensemble = new double[dim];
    singleEnsemble(greedyensemble, relation.get(bestid));
    // Greedily grow the ensemble
    final double[] testensemble = new double[dim];
    while(enscands.size() > 0) {
      NumberVector greedyvec = DoubleVector.wrap(greedyensemble);
      final double oldd = wdist.distance(estimated_truth_vec, greedyvec);

      final int heapsize = enscands.size();
      ModifiableDoubleDBIDList heap = DBIDUtil.newDistanceDBIDList(heapsize);
      double[] tmp = new double[dim];
      for(DBIDIter iter = enscands.iter(); iter.valid(); iter.advance()) {
        final NumberVector vec = relation.get(iter);
        singleEnsemble(tmp, vec);
        double diversity = wdist.distance(DoubleVector.wrap(greedyensemble), greedyvec);
        heap.add(diversity, iter);
      }
      heap.sort();
      for(DoubleDBIDListMIter it = heap.iter(); heap.size() > 0; it.remove()) {
        it.seek(heap.size() - 1); // Last
        enscands.remove(it);
        final NumberVector vec = relation.get(it);
        // Build combined ensemble.
        {
          double[] buf = new double[ensemble.size() + 1];
          for(int i = 0; i < dim; i++) {
            int j = 0;
            for(DBIDIter iter = ensemble.iter(); iter.valid(); iter.advance()) {
              buf[j] = relation.get(iter).doubleValue(i);
              j++;
            }
            buf[j] = vec.doubleValue(i);
            testensemble[i] = voting.combine(buf, j + 1);
          }
        }
        applyScaling(testensemble, scaling);
        NumberVector testvec = DoubleVector.wrap(testensemble);
        double newd = wdist.distance(estimated_truth_vec, testvec);
        // LOG.verbose("Distances: " + oldd + " vs. " + newd + " " +
        // labels.get(bestadd));
        if(newd < oldd) {
          System.arraycopy(testensemble, 0, greedyensemble, 0, dim);
          ensemble.add(it);
          // logger.verbose("Growing ensemble with: " + labels.get(bestadd));
          break; // Recompute heap
        }
        else {
          dropped.add(it);
          // logger.verbose("Discarding: " + labels.get(bestadd));
          if(refine_truth) {
            // Update target vectors and weights
            ArrayList<DecreasingVectorIter> iters = new ArrayList<>(numcand);
            for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
              // Skip "by label", obviously
              if(DBIDUtil.equal(firstid, iditer) || dropped.contains(iditer)) {
                continue;
              }
              iters.add(new DecreasingVectorIter(relation.get(iditer)));
            }
            if(minvote >= iters.size()) {
              minvote = iters.size() - 1;
            }

            union_outliers = 0;
            Arrays.fill(outliers_seen, 0);
            while(union_outliers < desired_outliers) {
              for(DecreasingVectorIter iter : iters) {
                if(!iter.valid()) {
                  break;
                }
                int cur = iter.dim();
                if(outliers_seen[cur] == 0) {
                  outliers_seen[cur] = 1;
                }
                else {
                  outliers_seen[cur] += 1;
                }
                if(outliers_seen[cur] == minvote) {
                  union_outliers += 1;
                }
                iter.advance();
              }
            }
            LOG.warning("New num outliers: " + union_outliers);
            updateEstimations(outliers_seen, union_outliers, estimated_weights, estimated_truth);
            estimated_truth_vec = DoubleVector.wrap(estimated_truth);
          }
        }
      }
    }
    // Build the improved ensemble:
    StringBuilder greedylbl = new StringBuilder();
    {
      for(DBIDIter iter = ensemble.iter(); iter.valid(); iter.advance()) {
        if(greedylbl.length() > 0) {
          greedylbl.append(' ');
        }
        greedylbl.append(labels.get(iter));
      }
    }
    DoubleVector greedyvec = DoubleVector.wrap(greedyensemble);
    if(refine_truth) {
      LOG.verbose("Estimated outliers remaining: " + union_outliers);
    }
    LOG.verbose("Greedy ensemble (" + ensemble.size() + "): " + greedylbl.toString());

    LOG.verbose("Best single ROC AUC: " + bestauc + " (" + bestaucstr + ")");
    LOG.verbose("Best single cost:    " + bestcost + " (" + bestcoststr + ")");
    // Evaluate the naive ensemble and the "shrunk" ensemble
    double naiveauc, naivecost;
    {
      naiveauc = ROCEvaluation.computeROCAUC(positive, new DecreasingVectorIter(naivevec));
      naivecost = tdist.distance(naivevec, refvec);
      LOG.verbose("Naive ensemble AUC:   " + naiveauc + " cost: " + naivecost);
      LOG.verbose("Naive ensemble Gain:  " + gain(naiveauc, bestauc, 1) + " cost gain: " + gain(naivecost, bestcost, 0));
    }
    double greedyauc, greedycost;
    {
      greedyauc = ROCEvaluation.computeROCAUC(positive, new DecreasingVectorIter(greedyvec));
      greedycost = tdist.distance(greedyvec, refvec);
      LOG.verbose("Greedy ensemble AUC:  " + greedyauc + " cost: " + greedycost);
      LOG.verbose("Greedy ensemble Gain to best:  " + gain(greedyauc, bestauc, 1) + " cost gain: " + gain(greedycost, bestcost, 0));
      LOG.verbose("Greedy ensemble Gain to naive: " + gain(greedyauc, naiveauc, 1) + " cost gain: " + gain(greedycost, naivecost, 0));
    }
    {
      MeanVariance meanauc = new MeanVariance();
      MeanVariance meancost = new MeanVariance();
      HashSetModifiableDBIDs candidates = DBIDUtil.newHashSet(relation.getDBIDs());
      candidates.remove(firstid);
      for(int i = 0; i < 1000; i++) {
        // Build the improved ensemble:
        final double[] randomensemble = new double[dim];
        {
          DBIDs random = DBIDUtil.randomSample(candidates, ensemble.size(), (long) i);
          double[] buf = new double[random.size()];
          for(int d = 0; d < dim; d++) {
            int j = 0;
            for(DBIDIter iter = random.iter(); iter.valid(); iter.advance()) {
              assert (!DBIDUtil.equal(firstid, iter));
              final NumberVector vec = relation.get(iter);
              buf[j] = vec.doubleValue(d);
              j++;
            }
            randomensemble[d] = voting.combine(buf, j);
          }
        }
        applyScaling(randomensemble, scaling);
        NumberVector randomvec = DoubleVector.wrap(randomensemble);
        double auc = ROCEvaluation.computeROCAUC(positive, new DecreasingVectorIter(randomvec));
        meanauc.put(auc);
        double cost = tdist.distance(randomvec, refvec);
        meancost.put(cost);
      }
      LOG.verbose("Random ensemble AUC:  " + meanauc.getMean() + " + stddev: " + meanauc.getSampleStddev() + " = " + (meanauc.getMean() + meanauc.getSampleStddev()));
      LOG.verbose("Random ensemble Gain: " + gain(meanauc.getMean(), bestauc, 1));
      LOG.verbose("Greedy improvement:   " + (greedyauc - meanauc.getMean()) / meanauc.getSampleStddev() + " standard deviations.");
      LOG.verbose("Random ensemble Cost: " + meancost.getMean() + " + stddev: " + meancost.getSampleStddev() + " = " + (meancost.getMean() + meanauc.getSampleStddev()));
      LOG.verbose("Random ensemble Gain: " + gain(meancost.getMean(), bestcost, 0));
      LOG.verbose("Greedy improvement:   " + (meancost.getMean() - greedycost) / meancost.getSampleStddev() + " standard deviations.");
      LOG.verbose("Naive ensemble Gain to random: " + gain(naiveauc, meanauc.getMean(), 1) + " cost gain: " + gain(naivecost, meancost.getMean(), 0));
      LOG.verbose("Random ensemble Gain to naive: " + gain(meanauc.getMean(), naiveauc, 1) + " cost gain: " + gain(meancost.getMean(), naivecost, 0));
      LOG.verbose("Greedy ensemble Gain to random: " + gain(greedyauc, meanauc.getMean(), 1) + " cost gain: " + gain(greedycost, meancost.getMean(), 0));
    }
  }

  /**
   * Build a single-element "ensemble".
   *
   * @param ensemble
   * @param vec
   */
  protected void singleEnsemble(final double[] ensemble, final NumberVector vec) {
    double[] buf = new double[1];
    for(int i = 0; i < ensemble.length; i++) {
      buf[0] = vec.doubleValue(i);
      ensemble[i] = voting.combine(buf, 1);
      if(Double.isNaN(ensemble[i])) {
        LOG.warning("NaN after combining: " + FormatUtil.format(buf) + " " + voting.toString());
      }
    }
    applyScaling(ensemble, scaling);
  }

  /**
   * Prescale each vector (except when in {@code skip}) with the given scaling
   * function.
   *
   * @param scaling Scaling function
   * @param relation Relation to read
   * @param skip DBIDs to pass unmodified
   * @return New relation
   */
  public static Relation<NumberVector> applyPrescaling(ScalingFunction scaling, Relation<NumberVector> relation, DBIDs skip) {
    if(scaling == null) {
      return relation;
    }
    NumberVector.Factory<NumberVector> factory = RelationUtil.getNumberVectorFactory(relation);
    DBIDs ids = relation.getDBIDs();
    WritableDataStore<NumberVector> contents = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT, NumberVector.class);
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      NumberVector v = relation.get(iter);
      double[] raw = v.toArray();
      if(!skip.contains(iter)) {
        applyScaling(raw, scaling);
      }
      contents.put(iter, factory.newNumberVector(raw, ArrayLikeUtil.DOUBLEARRAYADAPTER));
    }
    return new MaterializedRelation<>(relation.getDataTypeInformation(), ids, "rescaled", contents);
  }

  private static void applyScaling(double[] raw, ScalingFunction scaling) {
    if(scaling == null) {
      return;
    }
    if(scaling instanceof OutlierScaling) {
      ((OutlierScaling) scaling).prepare(raw, ArrayLikeUtil.DOUBLEARRAYADAPTER);
    }
    for(int i = 0; i < raw.length; i++) {
      final double newval = scaling.getScaled(raw[i]);
      if(Double.isNaN(newval)) {
        LOG.warning("NaN after prescaling: " + raw[i] + " " + scaling.toString() + " -> " + newval);
      }
      raw[i] = newval;
    }
  }

  protected void updateEstimations(final int[] outliers, int numoutliers, final double[] weights, final double[] truth) {
    final double oweight = .5 / numoutliers;
    final double iweight = .5 / (outliers.length - numoutliers);
    // final double orate = union_outliers * 1.0 / (outliers_seen.length);
    final double oscore = 1.; // .5 - .5 * orate;
    final double iscore = 0.; // 1 - .5 * orate;
    for(int i = 0; i < outliers.length; i++) {
      if(outliers[i] >= minvote) {
        weights[i] = oweight;
        truth[i] = oscore;
      }
      else {
        weights[i] = iweight;
        truth[i] = iscore;
      }
    }
  }

  private PrimitiveDistanceFunction<NumberVector> getDistanceFunction(double[] estimated_weights) {
    switch(distance){
    case SQEUCLIDEAN:
      return new WeightedSquaredEuclideanDistanceFunction(estimated_weights);
    case EUCLIDEAN:
      return new WeightedEuclideanDistanceFunction(estimated_weights);
    case MANHATTAN:
      return new WeightedManhattanDistanceFunction(estimated_weights);
    case PEARSON:
      return new WeightedPearsonCorrelationDistanceFunction(estimated_weights);
    default:
      throw new AbortException("Unsupported distance mode: " + distance);
    }
  }

  /**
   * Compute the gain coefficient.
   *
   * @param score New score
   * @param ref Reference score
   * @param optimal Maximum score possible
   * @return Gain
   */
  double gain(double score, double ref, double optimal) {
    return 1 - ((optimal - score) / (optimal - ref));
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * Expected rate of outliers
     */
    public static final OptionID RATE_ID = new OptionID("greedy.rate", "Expected rate of outliers.");

    /**
     * Ensemble voting function.
     */
    public static final OptionID VOTING_ID = new OptionID("ensemble.voting", "Ensemble voting function.");

    /**
     * Scaling to apply to input scores.
     */
    public static final OptionID PRESCALING_ID = new OptionID("ensemble.prescaling", "Prescaling to apply to input scores.");

    /**
     * Scaling to apply to ensemble scores.
     */
    public static final OptionID SCALING_ID = new OptionID("ensemble.scaling", "Scaling to apply to ensemble.");

    /**
     * Similarity measure
     */
    public static final OptionID DISTANCE_ID = new OptionID("ensemble.measure", "Similarity measure.");

    /**
     * Data source.
     */
    InputStep inputstep;

    /**
     * Ensemble voting method.
     */
    EnsembleVoting voting;

    /**
     * Distance in use.
     */
    Distance distance = Distance.PEARSON;

    /**
     * Outlier scaling to apply during preprocessing.
     */
    ScalingFunction prescaling;

    /**
     * Outlier scaling to apply to constructed ensembles.
     */
    ScalingFunction scaling;

    /**
     * Expected rate of outliers
     */
    double rate = 0.01;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Data input
      inputstep = config.tryInstantiate(InputStep.class);
      // Voting method
      ObjectParameter<EnsembleVoting> votingP = new ObjectParameter<>(VOTING_ID, EnsembleVoting.class, EnsembleVotingMean.class);
      if(config.grab(votingP)) {
        voting = votingP.instantiateClass(config);
      }
      // Similarity measure
      EnumParameter<Distance> distanceP = new EnumParameter<>(DISTANCE_ID, Distance.class);
      if(config.grab(distanceP)) {
        distance = distanceP.getValue();
      }
      // Prescaling
      ObjectParameter<ScalingFunction> prescalingP = new ObjectParameter<>(PRESCALING_ID, ScalingFunction.class);
      prescalingP.setOptional(true);
      if(config.grab(prescalingP)) {
        prescaling = prescalingP.instantiateClass(config);
      }
      // Ensemble scaling
      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<>(SCALING_ID, ScalingFunction.class);
      scalingP.setOptional(true);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }
      // Expected rate of outliers
      DoubleParameter rateP = new DoubleParameter(RATE_ID, 0.01);
      if(config.grab(rateP)) {
        rate = rateP.doubleValue();
      }
    }

    @Override
    protected GreedyEnsembleExperiment makeInstance() {
      return new GreedyEnsembleExperiment(inputstep, voting, distance, prescaling, scaling, rate);
    }
  }

  /**
   * Main method.
   *
   * @param args Command line parameters.
   */
  public static void main(String[] args) {
    runCLIApplication(GreedyEnsembleExperiment.class, args);
  }
}
