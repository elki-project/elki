package de.lmu.ifi.dbs.elki.algorithm.projection;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

public class tSNE<O> extends AbstractDistanceBasedAlgorithm<O, Relation<DoubleVector>> {
  private static final double MIN_PIJ = 1e-12, MIN_QIJ = 1e-12;

  private static final double EARLY_EXAGGERATION = 4.;

  int maxIterations;

  double perplexity;

  int learningRate;

  double initialMomentum = 0.5, finalMomentum = 0.8;

  private final int dim = 2;

  // private final int alpha = 1;
  private static final Logging LOG = Logging.getLogger(tSNE.class);

  private static final double MIN_GAIN = 0.01;

  protected tSNE(DistanceFunction<? super O> distanceFunction, double momentum, int learning_rate, int iteration, double perplexity) {
    super(distanceFunction);
    this.maxIterations = iteration;
    this.perplexity = perplexity;
    this.learningRate = learning_rate;
    this.finalMomentum = momentum;

  }

  public Relation<DoubleVector> run(Relation<O> relation) {
    DistanceQuery<O> dq = relation.getDistanceQuery(getDistanceFunction());
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int size = ids.size();

    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    double[][] pij;
    { // Compute desired affinities.
      double[][] dist = buildDistanceMatrix(size, dq, ix, iy);
      pij = computePij(dist, Math.log(perplexity));
      dist = null; // No longer needed.
    }

    // Create initial solution.
    double[][] sol = new double[size][dim];
    Random r = new Random(); // TODO: Use a RandomParameter.
    for(int i = 0; i < size; i++) {
      for(int j = 0; j < dim; j++) {
        sol[i][j] = r.nextGaussian();
      }
    }

    double[][] qij = new double[size][size];
    double[][] grad = new double[size][dim]; // Gradient
    double[][] mov = new double[size][dim]; // Momentum
    double[][] gains = new double[size][dim]; // Adaptive learning rate
    for(int i = 0; i < size; i++) {
      Arrays.fill(gains[i], 1.); // Initial learning rate
    }
    for(int it = 0; it < maxIterations; it++) {
      double qij_sum = computeQij(qij, sol);
      computeGradient(pij, qij, qij_sum, sol, grad);
      updateSolution(sol, grad, mov, gains, it);
      if(it == 100) { // Remove EARLY_EXAGGERATION effect:
        for(int i = 0; i < size; i++) {
          double[] row_i = pij[i];
          for(int j = 0; j < size; j++) {
            row_i[j] /= EARLY_EXAGGERATION; //Warum beeinflust das pij?
          }
        }
      }
    }

    // Transform into output data format.
    WritableDataStore<DoubleVector> proj = DataStoreFactory.FACTORY.makeStorage(ids, DataStoreFactory.HINT_DB | DataStoreFactory.HINT_SORTED, DoubleVector.class);
    VectorFieldTypeInformation<DoubleVector> otype = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    for(ix.seek(0); ix.valid(); ix.advance()) {
      proj.put(ix, new DoubleVector(sol[ix.getOffset()]));
    }
    return new MaterializedRelation<>("tSNE", "t-SNE", otype, proj, ids);
  }

  private static double[][] buildDistanceMatrix(int size, DistanceQuery<?> dq, DBIDArrayIter ix, DBIDArrayIter iy) {
    double[][] dmat = new double[size][size];
    final boolean square = !SquaredEuclideanDistanceFunction.class.isInstance(dq.getDistanceFunction());
    for(ix.seek(0); ix.valid(); ix.advance()) {
      for(iy.seek(0); iy.getOffset() < ix.getOffset(); iy.advance()) {
        double dist = dq.distance(ix, iy);
        dist = square ? (dist * dist) : dist;
        dmat[ix.getOffset()][iy.getOffset()] = dist;
        dmat[iy.getOffset()][ix.getOffset()] = dist;
      }
    }
    return dmat;
  }

  private double[][] computePij(double[][] dist, double logPerp) {
    double[][] pij = new double[dist.length][dist.length];
    double ssum = 0.;
    for(int i = 0; i < pij.length; i++) {
      ssum += computePi(i, dist[i], pij[i], logPerp);
    }
    if(LOG.isVeryVerbose()) {
      LOG.veryverbose("Average Sigma: " + (ssum / pij.length));
    }
    // Scale pij to have the desired sum EARLY_EXAGGERATION
    double sum = 0.;
    for(int i = 1; i < pij.length; i++) {
      double[] row = pij[i];
      for(int j = 0; j < i; j++) {
        sum += (row[j] += pij[j][i]);
      }
    }
    // Scaling taken from original tSNE code:
    final double scale = EARLY_EXAGGERATION / (2. * sum);
    for(int i = 1; i < pij.length; i++) {
      double[] row = pij[i];
      for(int j = 0; j < i; j++) {
        row[j] = pij[j][i] = Math.max(row[j] * scale, MIN_PIJ);
      }
    }
    return pij;
  }

  /**
   * Compute row pij[i], using binary search on the kernel bandwidth sigma to
   * obtain the desired perplexity.
   *
   * @param i Current point
   * @param dist_i Distance matrix row pij[i]
   * @param pij_i Output row
   * @param logPerp Desired perplexity
   * @return Sigma value
   */
  private static double computePi(int i, double[] dist_i, double[] pij_i, double logPerp) {
    final double error = 1e-5;
    double beta = 1; // beta = 1. / (2*sigma*sigma)
    double diff = computeH(dist_i, pij_i, i, -beta) - logPerp;
    double betaMin = 0., betaMax = Double.POSITIVE_INFINITY;
    for(int tries = 0; tries < 50 && Math.abs(diff) > error; ++tries) {
      if(diff > 0) { //Sigma immer kleiner 1? Warum keine Verdoppelung?
        betaMin = beta;
        beta += Double.isInfinite(betaMax) ? beta : ((betaMax - beta) * .5);
      }
      else {
        betaMax = beta;
        beta = .5 * (beta + betaMin);
      }
      diff = computeH(dist_i, pij_i, i, -beta) - logPerp;
    }
    return Math.sqrt(.5 / beta); //Warum auch Wurzel aus 0.5?
  }

  /**
   * 
   * @param dist_i Distance matrix row (input)
   * @param pij_i Row pij[i]
   * @param i Current point i
   * @param mbeta {@code -1. / (2 * sigma * sigma)}
   * @return Observed perplexity
   */
  private static double computeH(double[] dist_i, double[] pij_i, final int i, double mbeta) {
    double sumP = 0.;
    // Skip point "i", break loop in two:
    for(int j = 0; j < i; j++) {
      sumP += (pij_i[j] = Math.exp(dist_i[j] * mbeta));
    }
    for(int j = i + 1; j < dist_i.length; j++) {
      sumP += (pij_i[j] = Math.exp(dist_i[j] * mbeta));
    }
    assert (sumP > 0.);
    // Scale output array:
    final double s = 1. / sumP;
    double sum = 0.;
    // While we could skip pi[i], it should be 0 anyway.
    for(int j = 0; j < dist_i.length; j++) {
      sum += dist_i[j] * (pij_i[j] *= s);
    }
    return Math.log(sumP) - mbeta * sum; //Welche Formel wird hier verwendet?
  }

  private double computeQij(double[][] qij, double[][] solution) {
    double qij_sum = 0;
    for(int i = 1; i < qij.length; i++) {
      final double[] qij_i = qij[i];
      final double[] vi = solution[i];
      for(int j = 0; j < i; j++) {
        qij_sum += qij_i[j] = qij[j][i] = 1. / (1. + sqDist(vi, solution[j]));
      }
    }
    qij_sum *= 2; // Symmetry
    if(LOG.isVeryVerbose()) {
      LOG.veryverbose("Qij sum prior to normalization: " + qij_sum);
    }
    return qij_sum;
  }

  /**
   * Squared distance, in projection space.
   * 
   * @param v1 First vector
   * @param v2 Second vector
   * @return Squared distance
   */
  private static double sqDist(double[] v1, double[] v2) {
    double sum = 0;
    for(int i = 0; i < v1.length; i++) {
      final double diff = v1[i] - v2[i];
      sum += diff * diff;
    }
    return sum;
  }

  private void computeGradient(double[][] pij, double[][] qij, double qij_sum, double[][] sol, double[][] grad) {
    for(int i = 0; i < pij.length; i++) {
      double[] grad_i = grad[i];
      Arrays.fill(grad_i, 0.);
      for(int j = 0; j < pij.length; j++) {
        if(i == j) {
          continue;
        }
        // Qij after scaling!
        final double q = Math.max(qij[i][j] / qij_sum, MIN_QIJ);
        double a = (pij[i][j] - q) * qij[i][j]; //Warum am Ende qij statt q?
        for(int k = 0; k < dim; k++) {
          double[] sol_i = sol[i];
          grad_i[k] += a * (sol_i[k] - sol[j][k]);
        }
      }
    }
  }

  private void updateSolution(double[][] sol, double[][] gradient, double[][] mov, double[][] gains, int it) {
    double mom = (it < 20 && initialMomentum < finalMomentum) ? initialMomentum : finalMomentum;
    for(int i = 0; i < sol.length; i++) {
      double[] sol_i = sol[i], grad_i = gradient[i];
      double[] mov_i = mov[i], gain_i = gains[i];
      for(int k = 0; k < dim; k++) { //was passiert hier?
        // Adjust learning rate:
        gain_i[k] = MathUtil.max(((grad_i[k] > 0) != (mov_i[k] > 0)) ? (gain_i[k] + 0.2) : (gain_i[k] * 0.8), MIN_GAIN);
        mov_i[k] *= mom; // Dampening the previous momentum
        mov_i[k] -= learningRate * grad_i[k] * gain_i[k]; // Learn
        sol_i[k] += mov_i[k];
      }
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {

    double momentum;

    double perplexity;

    int learning_rate;

    int iteration;

    public static final OptionID MOMENTUM_ID = new OptionID("tSNE.momentum", "The value for the momentum");

    public static final OptionID LEARNING_RATE_ID = new OptionID("tSNE.learning_rate", "");

    public static final OptionID ITERATION_ID = new OptionID("tSNE.iteration", "");

    public static final OptionID PERPLEXITY_ID = new OptionID("tSNE.perplexity", "");

    @Override
    protected void makeOptions(Parameterization config) {
      // super.makeOptions(config);
      ObjectParameter<DistanceFunction<O>> distanceFunctionP = makeParameterDistanceFunction(EuclideanDistanceFunction.class, DistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }

      DoubleParameter p_momentum = new DoubleParameter(MOMENTUM_ID)//
          .setDefaultValue(0.8);
      if(config.grab(p_momentum)) {
        momentum = p_momentum.getValue();
      }

      DoubleParameter p_perplexity = new DoubleParameter(PERPLEXITY_ID)//
          .setDefaultValue(40.0);
      if(config.grab(p_perplexity))
        perplexity = p_perplexity.getValue();

      IntParameter p_learning_rate = new IntParameter(LEARNING_RATE_ID)//
          .setDefaultValue(100);
      if(config.grab(p_learning_rate))
        learning_rate = p_learning_rate.getValue();

      IntParameter p_iteration = new IntParameter(ITERATION_ID)//
          .setDefaultValue(300);
      if(config.grab(p_iteration))
        iteration = p_iteration.getValue();

    }

    @Override
    protected tSNE<O> makeInstance() {
      return new tSNE<>(distanceFunction, momentum, learning_rate, iteration, perplexity);
    }
  }
}
