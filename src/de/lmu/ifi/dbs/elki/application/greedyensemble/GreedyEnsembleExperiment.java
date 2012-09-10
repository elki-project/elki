package de.lmu.ifi.dbs.elki.application.greedyensemble;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.WeightedPearsonCorrelationDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TiedTopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Class to load an outlier detection summary file, as produced by
 * {@link ComputeKNNOutlierScores}, and compute a naive ensemble for it. Based
 * on this initial estimation, and optimized ensemble is built using a greedy
 * strategy. Starting with the best candidate only as initial ensemble, the most
 * diverse candidate is investigated at each step. If it improves towards the
 * (estimated) target vector, it is added, otherwise it is discarded.
 * 
 * This approach is naive, and it may be surprising that it can improve results.
 * The reason is probably that diversity will result in a comparable ensemble,
 * while the reduced ensemble size is actually responsible for the improvements,
 * by being more decisive and less noisy due to dropping "unhelpful" members.
 * 
 * This still leaves quite a bit of room for improvement. If you build upon this
 * basic approach, please acknowledge our proof of concept work.
 * 
 * Reference:
 * <p>
 * E. Schubert, R. Wojdanowski, A. Zimek, H.-P. Kriegel<br />
 * On Evaluation of Outlier Rankings and Outlier Scores<br/>
 * In Proceedings of the 12th SIAM International Conference on Data Mining
 * (SDM), Anaheim, CA, 2012.
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "E. Schubert, R. Wojdanowski, A. Zimek, H.-P. Kriegel", title = "On Evaluation of Outlier Rankings and Outlier Scores", booktitle = "Proc. 12th SIAM International Conference on Data Mining (SDM), Anaheim, CA, 2012.")
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
   * Constructor.
   * 
   * @param verbose Verbosity
   * @param inputstep Input step
   */
  public GreedyEnsembleExperiment(boolean verbose, InputStep inputstep) {
    super(verbose);
    this.inputstep = inputstep;
  }

  @Override
  public void run() {
    // Note: the database contains the *result vectors*, not the original data
    // points.
    final Database database = inputstep.getDatabase();
    final Relation<NumberVector<?>> relation = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    final NumberVector.Factory<NumberVector<?>, ?> factory = RelationUtil.getNumberVectorFactory(relation);
    final Relation<String> labels = DatabaseUtil.guessLabelRepresentation(database);
    final DBID firstid = DBIDUtil.deref(labels.iterDBIDs());
    final String firstlabel = labels.get(firstid);
    if(!firstlabel.matches("bylabel")) {
      throw new AbortException("No 'by label' reference outlier found, which is needed for weighting!");
    }

    // Dimensionality and reference vector
    final int dim = RelationUtil.dimensionality(relation);
    final NumberVector<?> refvec = relation.get(firstid);

    // Build the positive index set for ROC AUC.
    Set<Integer> positive = new TreeSet<Integer>();
    for(int d = 0; d < dim; d++) {
      if(refvec.doubleValue(d + 1) > 0) {
        positive.add(d);
      }
    }

    final int estimated_outliers = (int) (0.005 * dim);
    int union_outliers = 0;
    final int[] outliers_seen = new int[dim];
    // Find the top-k for each ensemble member
    {
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        // Skip "by label", obviously
        if(DBIDUtil.equal(firstid, iditer)) {
          continue;
        }
        final NumberVector<?> vec = relation.get(iditer);
        TiedTopBoundedHeap<DoubleIntPair> heap = new TiedTopBoundedHeap<DoubleIntPair>(estimated_outliers, Collections.reverseOrder());
        for(int i = 0; i < dim; i++) {
          heap.add(new DoubleIntPair(vec.doubleValue(i + 1), i));
        }
        if(heap.size() >= 2 * estimated_outliers) {
          LOG.warning("Too many ties. Expected: " + estimated_outliers + " got: " + heap.size());
        }
        for(DoubleIntPair pair : heap) {
          if(outliers_seen[pair.second] == 0) {
            outliers_seen[pair.second] = 1;
            union_outliers += 1;
          }
          else {
            outliers_seen[pair.second] += 1;
          }
        }
      }
    }
    LOG.verbose("Merged top " + estimated_outliers + " outliers to: " + union_outliers + " outliers");
    // Build the final weight vector.
    final double[] estimated_weights = new double[dim];
    final double[] estimated_truth = new double[dim];
    updateEstimations(outliers_seen, union_outliers, estimated_weights, estimated_truth);
    NumberVector<?> estimated_truth_vec = factory.newNumberVector(estimated_truth);

    PrimitiveDoubleDistanceFunction<NumberVector<?>> wdist = getDistanceFunction(estimated_weights);
    PrimitiveDoubleDistanceFunction<NumberVector<?>> tdist = wdist;

    // Build the naive ensemble:
    final double[] naiveensemble = new double[dim];
    {
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        if(DBIDUtil.equal(firstid, iditer)) {
          continue;
        }
        final NumberVector<?> vec = relation.get(iditer);
        for(int d = 0; d < dim; d++) {
          naiveensemble[d] += vec.doubleValue(d + 1);
        }
      }
      for(int d = 0; d < dim; d++) {
        naiveensemble[d] /= (relation.size() - 1);
      }
    }
    NumberVector<?> naivevec = factory.newNumberVector(naiveensemble);

    // Compute single AUC scores and estimations.
    // Remember the method most similar to the estimation
    double bestauc = 0.0;
    String bestaucstr = "";
    double bestcost = Double.POSITIVE_INFINITY;
    String bestcoststr = "";
    DBID bestid = null;
    double bestest = Double.POSITIVE_INFINITY;
    {
      // Compute individual scores
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        if(DBIDUtil.equal(firstid, iditer)) {
          continue;
        }
        // fout.append(labels.get(id));
        final NumberVector<?> vec = relation.get(iditer);
        double auc = computeROCAUC(vec, positive, dim);
        double estimated = wdist.doubleDistance(vec, estimated_truth_vec);
        double cost = tdist.doubleDistance(vec, refvec);
        LOG.verbose("ROC AUC: " + auc + " estimated " + estimated + " cost " + cost + " " + labels.get(iditer));
        if(auc > bestauc) {
          bestauc = auc;
          bestaucstr = labels.get(iditer);
        }
        if(cost < bestcost) {
          bestcost = cost;
          bestcoststr = labels.get(iditer);
        }
        if(estimated < bestest) {
          bestest = estimated;
          bestid = DBIDUtil.deref(iditer);
        }
      }
    }

    // Initialize ensemble with "best" method
    LOG.verbose("Distance function: " + wdist);
    LOG.verbose("Initial estimation of outliers: " + union_outliers);
    LOG.verbose("Initializing ensemble with: " + labels.get(bestid));
    ModifiableDBIDs ensemble = DBIDUtil.newArray(bestid);
    ModifiableDBIDs enscands = DBIDUtil.newHashSet(relation.getDBIDs());
    enscands.remove(bestid);
    enscands.remove(firstid);
    final double[] greedyensemble = new double[dim];
    {
      final NumberVector<?> vec = relation.get(bestid);
      for(int i = 0; i < dim; i++) {
        greedyensemble[i] = vec.doubleValue(i + 1);
      }
    }
    // Greedily grow the ensemble
    final double[] testensemble = new double[dim];
    while(enscands.size() > 0) {
      NumberVector<?> greedyvec = factory.newNumberVector(greedyensemble);

      // Weighting factors for combining:
      double s1 = ensemble.size() / (ensemble.size() + 1.);
      double s2 = 1. / (ensemble.size() + 1.);

      final int heapsize = enscands.size();
      TopBoundedHeap<DoubleDBIDPair> heap = new TopBoundedHeap<DoubleDBIDPair>(heapsize, Collections.reverseOrder());
      for (DBIDIter iter = enscands.iter(); iter.valid(); iter.advance()) {
        final NumberVector<?> vec = relation.get(iter);
        double diversity = wdist.doubleDistance(vec, greedyvec);
        heap.add(DBIDUtil.newPair(diversity, iter));
      }
      while(heap.size() > 0) {
        DBIDRef bestadd = heap.poll();
        enscands.remove(bestadd);
        // Update ensemble:
        final NumberVector<?> vec = relation.get(bestadd);
        for(int i = 0; i < dim; i++) {
          testensemble[i] = greedyensemble[i] * s1 + vec.doubleValue(i + 1) * s2;
        }
        NumberVector<?> testvec = factory.newNumberVector(testensemble);
        double oldd = wdist.doubleDistance(estimated_truth_vec, greedyvec);
        double newd = wdist.doubleDistance(estimated_truth_vec, testvec);
        // logger.verbose("Distances: " + oldd + " vs. " + newd);
        if(newd < oldd) {
          System.arraycopy(testensemble, 0, greedyensemble, 0, dim);
          ensemble.add(bestadd);
          // logger.verbose("Growing ensemble with: " + labels.get(bestadd));
          break; // Recompute heap
        }
        else {
          // logger.verbose("Discarding: " + labels.get(bestadd));
          if(refine_truth) {
            boolean refresh = false;
            // Update target vectors and weights
            TiedTopBoundedHeap<DoubleIntPair> oheap = new TiedTopBoundedHeap<DoubleIntPair>(estimated_outliers, Collections.reverseOrder());
            for(int i = 0; i < dim; i++) {
              oheap.add(new DoubleIntPair(vec.doubleValue(i + 1), i));
            }
            for(DoubleIntPair pair : oheap) {
              assert (outliers_seen[pair.second] > 0);
              outliers_seen[pair.second] -= 1;
              if(outliers_seen[pair.second] == 0) {
                union_outliers -= 1;
                refresh = true;
              }
            }
            if(refresh) {
              updateEstimations(outliers_seen, union_outliers, estimated_weights, estimated_truth);
              estimated_truth_vec = factory.newNumberVector(estimated_truth);
            }
          }
        }
      }
    }
    // Build the improved ensemble:
    StringBuffer greedylbl = new StringBuffer();
    {
      for (DBIDIter iter = ensemble.iter(); iter.valid(); iter.advance()) {
        if(greedylbl.length() > 0) {
          greedylbl.append(" ");
        }
        greedylbl.append(labels.get(iter));
      }
    }
    NumberVector<?> greedyvec = factory.newNumberVector(greedyensemble);
    LOG.verbose("Estimated outliers remaining: " + union_outliers);
    LOG.verbose("Greedy ensemble: " + greedylbl.toString());

    LOG.verbose("Best single ROC AUC: " + bestauc + " (" + bestaucstr + ")");
    LOG.verbose("Best single cost:    " + bestcost + " (" + bestcoststr + ")");
    // Evaluate the naive ensemble and the "shrunk" ensemble
    double naiveauc, naivecost;
    {
      naiveauc = computeROCAUC(naivevec, positive, dim);
      naivecost = tdist.doubleDistance(naivevec, refvec);
      LOG.verbose("Naive ensemble AUC:   " + naiveauc + " cost: " + naivecost);
      LOG.verbose("Naive ensemble Gain:  " + gain(naiveauc, bestauc, 1) + " cost gain: " + gain(naivecost, bestcost, 0));
    }
    double greedyauc, greedycost;
    {
      greedyauc = computeROCAUC(greedyvec, positive, dim);
      greedycost = tdist.doubleDistance(greedyvec, refvec);
      LOG.verbose("Greedy ensemble AUC:  " + greedyauc + " cost: " + greedycost);
      LOG.verbose("Greedy ensemble Gain to best:  " + gain(greedyauc, bestauc, 1) + " cost gain: " + gain(greedycost, bestcost, 0));
      LOG.verbose("Greedy ensemble Gain to naive: " + gain(greedyauc, naiveauc, 1) + " cost gain: " + gain(greedycost, naivecost, 0));
    }
    {
      MeanVariance meanauc = new MeanVariance();
      MeanVariance meancost = new MeanVariance();
      HashSetModifiableDBIDs candidates = DBIDUtil.newHashSet(relation.getDBIDs());
      candidates.remove(firstid);
      for(int i = 0; i < 5000; i++) {
        // Build the improved ensemble:
        final double[] randomensemble = new double[dim];
        {
          DBIDs random = DBIDUtil.randomSample(candidates, ensemble.size(), (long)i);
          for (DBIDIter iter = random.iter(); iter.valid(); iter.advance()) {
            assert (!DBIDUtil.equal(firstid, iter));
            // logger.verbose("Using: "+labels.get(id));
            final NumberVector<?> vec = relation.get(iter);
            for(int d = 0; d < dim; d++) {
              randomensemble[d] += vec.doubleValue(d + 1);
            }
          }
          for(int d = 0; d < dim; d++) {
            randomensemble[d] /= ensemble.size();
          }
        }
        NumberVector<?> randomvec = factory.newNumberVector(randomensemble);
        double auc = computeROCAUC(randomvec, positive, dim);
        meanauc.put(auc);
        double cost = tdist.doubleDistance(randomvec, refvec);
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

  protected void updateEstimations(final int[] outliers_seen, int union_outliers, final double[] estimated_weights, final double[] estimated_truth) {
    for(int i = 0; i < outliers_seen.length; i++) {
      if(outliers_seen[i] > 0) {
        estimated_weights[i] = 0.5 / union_outliers;
        estimated_truth[i] = 1.0;
      }
      else {
        estimated_weights[i] = 0.5 / (outliers_seen.length - union_outliers);
        estimated_truth[i] = 0.0;
      }
    }
  }

  private PrimitiveDoubleDistanceFunction<NumberVector<?>> getDistanceFunction(double[] estimated_weights) {
    // return new WeightedSquaredEuclideanDistanceFunction(estimated_weights);
    // return new WeightedLPNormDistanceFunction(1.0, estimated_weights);
    return new WeightedPearsonCorrelationDistanceFunction(estimated_weights);
  }

  private double computeROCAUC(NumberVector<?> vec, Set<Integer> positive, int dim) {
    final DoubleIntPair[] scores = new DoubleIntPair[dim];
    for(int d = 0; d < dim; d++) {
      scores[d] = new DoubleIntPair(vec.doubleValue(d + 1), d);
    }
    Arrays.sort(scores, Collections.reverseOrder(DoubleIntPair.BYFIRST_COMPARATOR));
    return XYCurve.areaUnderCurve(ROC.materializeROC(dim, positive, Arrays.asList(scores).iterator()));
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * Data source.
     */
    InputStep inputstep;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Data input
      inputstep = config.tryInstantiate(InputStep.class);
    }

    @Override
    protected AbstractApplication makeInstance() {
      return new GreedyEnsembleExperiment(verbose, inputstep);
    }
  }

  /**
   * Main method.
   * 
   * @param args Command line parameters.
   */
  public static void main(String[] args) {
    GreedyEnsembleExperiment.runCLIApplication(GreedyEnsembleExperiment.class, args);
  }
}