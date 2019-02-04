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
package de.lmu.ifi.dbs.elki.data.model;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;

import java.text.NumberFormat;
import java.util.Locale;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.Normalization;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import net.jafama.FastMath;

/**
 * A solution of correlation analysis is a matrix of equations describing the
 * dependencies.
 * 
 * @author Arthur Zimek
 * @since 0.2
 * 
 * @composed - - - LinearEquationSystem
 * 
 * @param <V> the type of NumberVector handled by this Result
 */
public class CorrelationAnalysisSolution<V extends NumberVector> implements TextWriteable, Result, Model {
  /**
   * Empty constant vector returned when no subspace was used.
   */
  private static final double[] EMPTY_VECTOR = new double[0];

  /**
   * Stores the solution equations.
   */
  private LinearEquationSystem linearEquationSystem;

  /**
   * Number format for output accuracy.
   */
  private NumberFormat nf;

  /**
   * The dimensionality of the correlation.
   */
  private int correlationDimensionality;

  /**
   * The standard deviation within this solution.
   */
  private final double standardDeviation;

  /**
   * The weak eigenvectors of the hyperplane induced by the correlation.
   */
  private final double[][] weakEigenvectors;

  /**
   * The strong eigenvectors of the hyperplane induced by the correlation.
   */
  private final double[][] strongEigenvectors;

  /**
   * The similarity matrix of the pca.
   */
  private final double[][] similarityMatrix;

  /**
   * The centroid if the objects belonging to the hyperplane induced by the
   * correlation.
   */
  private final double[] centroid;

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix.
   * 
   * @param solution the linear equation system describing the solution
   *        equations
   * @param db the database containing the objects
   * @param strongEigenvectors the strong eigenvectors of the hyperplane induced
   *        by the correlation
   * @param weakEigenvectors the weak eigenvectors of the hyperplane induced by
   *        the correlation
   * @param similarityMatrix the similarity matrix of the underlying distance
   *        computations
   * @param centroid the centroid if the objects belonging to the hyperplane
   *        induced by the correlation
   */
  public CorrelationAnalysisSolution(LinearEquationSystem solution, Relation<V> db, double[][] strongEigenvectors, double[][] weakEigenvectors, double[][] similarityMatrix, double[] centroid) {
    this(solution, db, strongEigenvectors, weakEigenvectors, similarityMatrix, centroid, NumberFormat.getInstance(Locale.US));
  }

  /**
   * Provides a new CorrelationAnalysisSolution holding the specified matrix and
   * number format.
   * 
   * @param solution the linear equation system describing the solution
   *        equations
   * @param db the database containing the objects
   * @param strongEigenvectors the strong eigenvectors of the hyperplane induced
   *        by the correlation
   * @param weakEigenvectors the weak eigenvectors of the hyperplane induced by
   *        the correlation
   * @param similarityMatrix the similarity matrix of the underlying distance
   *        computations
   * @param centroid the centroid if the objects belonging to the hyperplane
   *        induced by the correlation
   * @param nf the number format for output accuracy
   */
  public CorrelationAnalysisSolution(LinearEquationSystem solution, Relation<V> db, double[][] strongEigenvectors, double[][] weakEigenvectors, double[][] similarityMatrix, double[] centroid, NumberFormat nf) {
    this.linearEquationSystem = solution;
    this.correlationDimensionality = strongEigenvectors[0].length;
    this.strongEigenvectors = strongEigenvectors;
    this.weakEigenvectors = weakEigenvectors;
    this.similarityMatrix = similarityMatrix;
    this.centroid = centroid;
    this.nf = nf;

    // determine standard deviation
    double variance = 0;
    DBIDs ids = db.getDBIDs();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      variance += squaredDistance(db.get(iter));
    }
    standardDeviation = FastMath.sqrt(variance / ids.size());
  }

  /**
   * Returns the linear equation system for printing purposes. If normalization
   * is null the linear equation system is returned, otherwise the linear
   * equation system will be transformed according to the normalization.
   * 
   * @param normalization the normalization, can be null
   * @return the linear equation system for printing purposes
   * @throws NonNumericFeaturesException if the linear equation system is not
   *         compatible with values initialized during normalization
   */
  public LinearEquationSystem getNormalizedLinearEquationSystem(Normalization<?> normalization) throws NonNumericFeaturesException {
    if(normalization != null) {
      LinearEquationSystem lq = normalization.transform(linearEquationSystem);
      lq.solveByTotalPivotSearch();
      return lq;
    }
    else {
      return linearEquationSystem;
    }
  }

  /**
   * Return the correlation dimensionality.
   * 
   * @return the correlation dimensionality
   */
  public int getCorrelationDimensionality() {
    return correlationDimensionality;
  }

  /**
   * Returns the distance of NumberVector p from the hyperplane underlying this
   * solution.
   * 
   * @param p a vector in the space underlying this solution
   * @return the distance of p from the hyperplane underlying this solution
   */
  public double squaredDistance(V p) {
    // V_affin = V + a
    // dist(p, V_affin) = d(p-a, V) = ||p - a - proj_V(p-a) ||
    double[] p_minus_a = minusEquals(p.toArray(), centroid);
    return squareSum(minusEquals(p_minus_a, times(strongEigenvectors, transposeTimes(strongEigenvectors, p_minus_a))));
  }

  /**
   * Returns the error vectors after projection.
   * 
   * @param p a vector in the space underlying this solution
   * @return the error vectors
   */
  public double[] errorVector(V p) {
    return weakEigenvectors.length > 0 ? times(weakEigenvectors, transposeTimes(weakEigenvectors, minusEquals(p.toArray(), centroid))) : EMPTY_VECTOR;
  }

  /**
   * Returns the data vectors after projection.
   * 
   * @param p a vector in the space underlying this solution
   * @return the error vectors
   */
  public double[] dataVector(V p) {
    return strongEigenvectors.length > 0 ? times(strongEigenvectors, transposeTimes(strongEigenvectors, minusEquals(p.toArray(), centroid))) : EMPTY_VECTOR;
  }

  /**
   * Returns the standard deviation of the distances of the objects belonging to
   * the hyperplane underlying this solution.
   * 
   * @return the standard deviation of this solution
   */
  public double getStandardDeviation() {
    return standardDeviation;
  }

  /**
   * Returns the strong eigenvectors.
   * 
   * @return the strong eigenvectors
   */
  public double[][] getStrongEigenvectors() {
    return strongEigenvectors;
  }

  /**
   * Returns the weak eigenvectors.
   * 
   * @return the weak eigenvectors
   */
  public double[][] getWeakEigenvectors() {
    return weakEigenvectors;
  }

  /**
   * Returns the similarity matrix of the pca.
   * 
   * @return the similarity matrix of the pca
   */
  public double[][] getSimilarityMatrix() {
    return similarityMatrix;
  }

  /**
   * Returns the centroid of this model.
   * 
   * @return the centroid of this model
   */
  public double[] getCentroid() {
    return centroid;
  }

  /**
   * Text output of the equation system
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn("Model class: " + this.getClass().getName());
    try {
      if(getNormalizedLinearEquationSystem(null) != null) {
        // TODO: more elegant way of doing normalization here?
        /*
         * if(out instanceof TextWriterStreamNormalizing) {
         * TextWriterStreamNormalizing<V> nout =
         * (TextWriterStreamNormalizing<V>) out; LinearEquationSystem lq =
         * getNormalizedLinearEquationSystem(nout.getNormalization());
         * out.commentPrint("Linear Equation System: ");
         * out.commentPrintLn(lq.equationsToString(nf)); } else {
         */
        LinearEquationSystem lq = getNormalizedLinearEquationSystem(null);
        out.commentPrint("Linear Equation System: ");
        out.commentPrintLn(lq.equationsToString(nf));
        // }
      }
    }
    catch(NonNumericFeaturesException e) {
      LoggingUtil.exception(e);
    }
  }

  @Override
  public String getLongName() {
    return "Correlation Analysis Solution";
  }

  @Override
  public String getShortName() {
    return "correlationanalysissolution";
  }
}