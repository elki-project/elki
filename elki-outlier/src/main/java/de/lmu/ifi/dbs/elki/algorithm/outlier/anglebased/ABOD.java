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
package de.lmu.ifi.dbs.elki.algorithm.outlier.anglebased;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.PolynomialKernelFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Angle-Based Outlier Detection / Angle-Based Outlier Factor.
 * <p>
 * Outlier detection using variance analysis on angles, especially for high
 * dimensional data sets. Exact version, which has cubic runtime (see also
 * {@link FastABOD} and {@link LBABOD} for faster versions).
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Matthias Schubert, Arthur Zimek<br>
 * Angle-Based Outlier Detection in High-dimensional Data<br>
 * Proc. 14th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining (KDD'08)
 * 
 * @author Matthias Schubert (Original Code)
 * @author Erich Schubert (ELKIfication)
 * @since 0.2
 *
 * @param <V> Vector type
 */
@Title("ABOD: Angle-Based Outlier Detection")
@Description("Outlier detection using variance analysis on angles, especially for high dimensional data sets.")
@Reference(authors = "Hans-Peter Kriegel, Matthias Schubert, Arthur Zimek", //
    title = "Angle-Based Outlier Detection in High-dimensional Data", //
    booktitle = "Proc. 14th ACM SIGKDD Int. Conf. Knowledge Discovery and Data Mining (KDD'08)", //
    url = "https://doi.org/10.1145/1401890.1401946", //
    bibkey = "DBLP:conf/kdd/KriegelSZ08")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.ABOD", "abod" })
public class ABOD<V extends NumberVector> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ABOD.class);

  /**
   * Store the configured Kernel version.
   */
  protected SimilarityFunction<? super V> kernelFunction;

  /**
   * Constructor for Angle-Based Outlier Detection (ABOD).
   *
   * @param kernelFunction kernel function to use
   */
  public ABOD(SimilarityFunction<? super V> kernelFunction) {
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
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    // Build a kernel matrix, to make O(n^3) slightly less bad.
    SimilarityQuery<V> sq = db.getSimilarityQuery(relation, kernelFunction);
    KernelMatrix kernelMatrix = new KernelMatrix(sq, relation, ids);

    WritableDoubleDataStore abodvalues = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmaxabod = new DoubleMinMax();

    MeanVariance s = new MeanVariance();
    DBIDArrayIter pA = ids.iter(), pB = ids.iter(), pC = ids.iter();
    for(; pA.valid(); pA.advance()) {
      final double abof = computeABOF(kernelMatrix, pA, pB, pC, s);
      minmaxabod.put(abof);
      abodvalues.putDouble(pA, abof);
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Angle-Based Outlier Degree", "abod-outlier", abodvalues, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmaxabod.getMin(), minmaxabod.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Compute the exact ABOF value.
   *
   * @param kernelMatrix Kernel matrix
   * @param pA Object A to compute ABOF for
   * @param pB Iterator over objects B
   * @param pC Iterator over objects C
   * @param s Statistics tracker
   * @return ABOF value
   */
  protected double computeABOF(KernelMatrix kernelMatrix, DBIDRef pA, DBIDArrayIter pB, DBIDArrayIter pC, MeanVariance s) {
    s.reset(); // Reused
    double simAA = kernelMatrix.getSimilarity(pA, pA);

    for(pB.seek(0); pB.valid(); pB.advance()) {
      if(DBIDUtil.equal(pB, pA)) {
        continue;
      }
      double simBB = kernelMatrix.getSimilarity(pB, pB);
      double simAB = kernelMatrix.getSimilarity(pA, pB);
      double sqdAB = simAA + simBB - simAB - simAB;
      if(!(sqdAB > 0.)) {
        continue;
      }
      for(pC.seek(pB.getOffset() + 1); pC.valid(); pC.advance()) {
        if(DBIDUtil.equal(pC, pA)) {
          continue;
        }
        double simCC = kernelMatrix.getSimilarity(pC, pC);
        double simAC = kernelMatrix.getSimilarity(pA, pC);
        double sqdAC = simAA + simCC - simAC - simAC;
        if(!(sqdAC > 0.)) {
          continue;
        }
        // Exploit bilinearity of scalar product:
        // <B-A, C-A> = <B,C-A> - <A,C-A>
        // = <B,C> - <B,A> - <A,C> + <A,A>
        double simBC = kernelMatrix.getSimilarity(pB, pC);
        double numerator = simBC - simAB - simAC + simAA;
        double div = 1. / (sqdAB * sqdAC);
        s.put(numerator * div, FastMath.sqrt(div));
      }
    }
    // Sample variance probably would be better here, but the ABOD publication
    // uses the naive variance.
    return s.getNaiveVariance();
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
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter for the kernel function.
     */
    public static final OptionID KERNEL_FUNCTION_ID = new OptionID("abod.kernelfunction", "Kernel function to use.");

    /**
     * Distance function.
     */
    protected SimilarityFunction<V> kernelFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<SimilarityFunction<V>> param = new ObjectParameter<>(KERNEL_FUNCTION_ID, SimilarityFunction.class, PolynomialKernelFunction.class);
      if(config.grab(param)) {
        kernelFunction = param.instantiateClass(config);
      }
    }

    @Override
    protected ABOD<V> makeInstance() {
      return new ABOD<>(kernelFunction);
    }
  }
}
