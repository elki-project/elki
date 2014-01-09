package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.PolynomialKernelFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Angle-Based Outlier Detection / Angle-Based Outlier Factor.
 * 
 * Outlier detection using variance analysis on angles, especially for high
 * dimensional data sets. Exact version, which has cubic runtime (see also
 * {@link FastABOD} and {@link LBABOD} for faster versions).
 * 
 * H.-P. Kriegel, M. Schubert, and A. Zimek: Angle-Based Outlier Detection in
 * High-dimensional Data. In: Proc. 14th ACM SIGKDD Int. Conf. on Knowledge
 * Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008.
 * 
 * @author Matthias Schubert (Original Code)
 * @author Erich Schubert (ELKIfication)
 * 
 * @param <V> Vector type
 */
@Title("ABOD: Angle-Based Outlier Detection")
@Description("Outlier detection using variance analysis on angles, especially for high dimensional data sets.")
@Reference(authors = "H.-P. Kriegel, M. Schubert, and A. Zimek", title = "Angle-Based Outlier Detection in High-dimensional Data", booktitle = "Proc. 14th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining (KDD '08), Las Vegas, NV, 2008", url = "http://dx.doi.org/10.1145/1401890.1401946")
public class ABOD<V extends NumberVector<?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ABOD.class);

  /**
   * Store the configured Kernel version.
   */
  protected SimilarityFunction<? super V, DoubleDistance> kernelFunction;

  /**
   * Constructor for Angle-Based Outlier Detection (ABOD).
   * 
   * @param kernelFunction kernel function to use
   */
  public ABOD(SimilarityFunction<? super V, DoubleDistance> kernelFunction) {
    super();
    this.kernelFunction = kernelFunction;
  }

  /**
   * Run ABOD on the data set.
   * 
   * @param relation Relation to process
   * @return Outlier detection result
   */
  public OutlierResult run(Database db, Relation<V> relation) {
    DBIDs ids = relation.getDBIDs();
    // Build a kernel matrix, to make O(n^3) slightly less bad.
    SimilarityQuery<V, DoubleDistance> sq = db.getSimilarityQuery(relation, kernelFunction);
    KernelMatrix kernelMatrix = new KernelMatrix(sq, relation, ids);

    WritableDoubleDataStore abodvalues = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmaxabod = new DoubleMinMax();

    MeanVariance s = new MeanVariance();
    for (DBIDIter pA = ids.iter(); pA.valid(); pA.advance()) {
      final double abof = computeABOF(relation, kernelMatrix, pA, s);
      minmaxabod.put(abof);
      abodvalues.putDouble(pA, abof);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<>("Angle-Based Outlier Degree", "abod-outlier", TypeUtil.DOUBLE, abodvalues, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmaxabod.getMin(), minmaxabod.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Compute the exact ABOF value.
   * 
   * @param relation Relation
   * @param kernelMatrix Kernel matrix
   * @param pA Object A to compute ABOF for
   * @param s Statistics tracker
   * @return ABOF value
   */
  protected double computeABOF(Relation<V> relation, KernelMatrix kernelMatrix, DBIDRef pA, MeanVariance s) {
    s.reset(); // Reused
    double simAA = kernelMatrix.getSimilarity(pA, pA);

    for (DBIDIter nB = relation.iterDBIDs(); nB.valid(); nB.advance()) {
      if (DBIDUtil.equal(nB, pA)) {
        continue;
      }
      double simBB = kernelMatrix.getSimilarity(nB, nB);
      double simAB = kernelMatrix.getSimilarity(pA, nB);
      double sqdAB = simAA + simBB - simAB - simAB;
      if (!(sqdAB > 0.)) {
        continue;
      }
      for (DBIDIter nC = relation.iterDBIDs(); nC.valid(); nC.advance()) {
        if (DBIDUtil.equal(nC, pA) || DBIDUtil.compare(nC, nB) < 0) {
          continue;
        }
        double simCC = kernelMatrix.getSimilarity(nC, nC);
        double simAC = kernelMatrix.getSimilarity(pA, nC);
        double sqdAC = simAA + simCC - simAC;
        if (!(sqdAC > 0.)) {
          continue;
        }
        // Exploit bilinearity of scalar product:
        // <B-A, C-A> = <B, C-A> - <A,C-A>
        // = <B,C> - <B,A> - <A,C> + <A,A>
        // For computing variance, AA is a constant and can be ignored.
        double simBC = kernelMatrix.getSimilarity(nB, nC);
        double numerator = simBC - simAB - simAC; // + simAA;
        double val = numerator / (sqdAB * sqdAC);
        s.put(val, 1. / Math.sqrt(sqdAB * sqdAC));
      }
    }
    // Sample variance probably would be correct, but the ABOD publication
    // uses the naive variance.
    final double abof = s.getNaiveVariance();
    return abof;
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
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Parameter for the kernel function.
     */
    public static final OptionID KERNEL_FUNCTION_ID = new OptionID("abod.kernelfunction", "Kernel function to use.");

    /**
     * Distance function.
     */
    protected SimilarityFunction<V, DoubleDistance> kernelFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<SimilarityFunction<V, DoubleDistance>> param = new ObjectParameter<>(KERNEL_FUNCTION_ID, SimilarityFunction.class, PolynomialKernelFunction.class);
      if (config.grab(param)) {
        kernelFunction = param.instantiateClass(config);
      }
    }

    @Override
    protected ABOD<V> makeInstance() {
      return new ABOD<>(kernelFunction);
    }
  }
}
