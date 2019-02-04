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
package de.lmu.ifi.dbs.elki.index.distancematrix;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.SimilarityIndex;
import de.lmu.ifi.dbs.elki.index.SimilarityRangeIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Precomputed similarity matrix, for a small data set.
 * <p>
 * This class uses a linear memory layout (not a ragged array), and assumes
 * symmetry as well as strictness. This way, it only stores the upper triangle
 * matrix with double precision. It has to store (n-1) * (n-2) similarity values
 * in memory, requiring 8 * (n-1) * (n-2) bytes. Since Java has a size limit of
 * arrays of 31 bits (signed integer), we can store at most \(2^16\) objects
 * (precisely, 65536 objects) in a single array, which needs about 16 GB of RAM.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - PrecomputedSimilarityQuery
 * @has - - - PrecomputedSimilarityRangeQuery
 *
 * @param <O> Object type
 */
public class PrecomputedSimilarityMatrix<O> extends AbstractIndex<O> implements SimilarityIndex<O>, SimilarityRangeIndex<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(PrecomputedSimilarityMatrix.class);

  /**
   * Nested similarity function.
   */
  final protected SimilarityFunction<? super O> similarityFunction;

  /**
   * Nested similarity query.
   */
  protected SimilarityQuery<O> similarityQuery;

  /**
   * Similarity matrix.
   */
  private double[] matrix = null;

  /**
   * DBID range.
   */
  private DBIDRange ids;

  /**
   * Size of DBID range.
   */
  private int size;

  /**
   * Constructor.
   *
   * @param relation Data relation
   * @param similarityFunction Similarity function
   */
  public PrecomputedSimilarityMatrix(Relation<O> relation, SimilarityFunction<? super O> similarityFunction) {
    super(relation);
    this.similarityFunction = similarityFunction;

    if(!similarityFunction.isSymmetric()) {
      throw new AbortException("Similarity matrixes currently only support symmetric similarity functions (Patches welcome).");
    }
  }

  @Override
  public void initialize() {
    DBIDs rids = relation.getDBIDs();
    if(!(rids instanceof DBIDRange)) {
      throw new AbortException("Similarity matrixes are currently only supported for DBID ranges (as used by static databases) for performance reasons (Patches welcome).");
    }
    ids = (DBIDRange) rids;
    size = ids.size();
    if(size > 65536) {
      throw new AbortException("Similarity matrixes currently have a limit of 65536 objects (~16 GB). After this, the array size exceeds the Java integer range, and a different data structure needs to be used.");
    }

    similarityQuery = similarityFunction.instantiate(relation);

    int msize = triangleSize(size);
    matrix = new double[msize];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Precomputing similarity matrix", msize, LOG) : null;
    int pos = 0;
    for(ix.seek(0); ix.valid(); ix.advance()) {
      // y < x -- must match {@link #getOffset}!
      for(iy.seek(0); iy.getOffset() < ix.getOffset(); iy.advance()) {
        matrix[pos] = similarityQuery.similarity(ix, iy);
        pos++;
      }
      if(prog != null) {
        prog.setProcessed(prog.getProcessed() + ix.getOffset(), LOG);
      }
    }
    LOG.ensureCompleted(prog);
  }

  /**
   * Compute the size of a complete x by x triangle (minus diagonal)
   *
   * @param x Offset
   * @return Size of complete triangle
   */
  protected static int triangleSize(int x) {
    return (x * (x - 1)) >>> 1;
  }

  /**
   * Array offset computation.
   *
   * @param x X parameter
   * @param y Y parameter
   * @return Array offset
   */
  private int getOffset(int x, int y) {
    return (y < x) ? (triangleSize(x) + y) : (triangleSize(y) + x);
  }

  @Override
  public void logStatistics() {
    if(matrix != null) {
      LOG.statistics(new LongStatistic(this.getClass().getName() + ".matrix-size", matrix.length));
    }
  }

  @Override
  public String getLongName() {
    return "Precomputed Similarity Matrix";
  }

  @Override
  public String getShortName() {
    return "similarity-matrix";
  }

  @Override
  public SimilarityQuery<O> getSimilarityQuery(SimilarityFunction<? super O> similarityFunction, Object... hints) {
    if(this.similarityFunction.equals(similarityFunction)) {
      return new PrecomputedSimilarityQuery();
    }
    return null;
  }

  @Override
  public RangeQuery<O> getSimilarityRangeQuery(SimilarityQuery<O> simQuery, Object... hints) {
    if(this.similarityFunction.equals(simQuery.getSimilarityFunction())) {
      return new PrecomputedSimilarityRangeQuery();
    }
    return null;
  }

  /**
   * Similarity query using the precomputed matrix.
   *
   * @author Erich Schubert
   */
  private class PrecomputedSimilarityQuery implements SimilarityQuery<O> {
    @Override
    public double similarity(DBIDRef id1, DBIDRef id2) {
      final int x = ids.getOffset(id1), y = ids.getOffset(id2);
      return (x != y) ? matrix[getOffset(x, y)] : 0.;
    }

    @Override
    public double similarity(O o1, DBIDRef id2) {
      return similarityQuery.similarity(o1, id2);
    }

    @Override
    public double similarity(DBIDRef id1, O o2) {
      return similarityQuery.similarity(id1, o2);
    }

    @Override
    public double similarity(O o1, O o2) {
      return similarityQuery.similarity(o1, o2);
    }

    @Override
    public SimilarityFunction<? super O> getSimilarityFunction() {
      return similarityQuery.getSimilarityFunction();
    }

    @Override
    public Relation<? extends O> getRelation() {
      return relation;
    }
  }

  /**
   * Range query using the distance matrix.
   *
   * @author Erich Schubert
   */
  private class PrecomputedSimilarityRangeQuery implements RangeQuery<O> {
    @Override
    public DoubleDBIDList getRangeForDBID(DBIDRef id, double range) {
      ModifiableDoubleDBIDList ret = DBIDUtil.newDistanceDBIDList();
      getRangeForDBID(id, range, ret);
      ret.sort();
      return ret;
    }

    @Override
    public void getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
      result.add(0., id);
      DBIDArrayIter it = ids.iter();

      final int x = ids.getOffset(id);
      // Case y < x: triangleSize(x) + y
      int pos = triangleSize(x);
      for(int y = 0; y < x; y++) {
        final double sim = matrix[pos];
        if(sim >= range) {
          result.add(sim, it.seek(y));
        }
        pos++;
      }
      assert (pos == triangleSize(x + 1));
      // Case y > x: triangleSize(y) + x
      pos = triangleSize(x + 1) + x;
      for(int y = x + 1; y < size; y++) {
        final double sim = matrix[pos];
        if(sim >= range) {
          result.add(sim, it.seek(y));
        }
        pos += y;
      }
    }

    @Override
    public DoubleDBIDList getRangeForObject(O obj, double range) {
      throw new AbortException("Preprocessor KNN query only supports ID queries.");
    }

    @Override
    public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
      throw new AbortException("Preprocessor KNN query only supports ID queries.");
    }
  }

  /**
   * Factory for the index.
   *
   * @author Erich Schubert
   *
   * @has - - - PrecomputedSimilarityMatrix
   *
   * @param <O> Object type
   */
  public static class Factory<O> implements IndexFactory<O> {
    /**
     * Nested similarity function.
     */
    final protected SimilarityFunction<? super O> similarityFunction;

    /**
     * Constructor.
     *
     * @param similarityFunction Similarity function
     */
    public Factory(SimilarityFunction<? super O> similarityFunction) {
      super();
      this.similarityFunction = similarityFunction;
    }

    @Override
    public PrecomputedSimilarityMatrix<O> instantiate(Relation<O> relation) {
      return new PrecomputedSimilarityMatrix<>(relation, similarityFunction);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return similarityFunction.getInputTypeRestriction();
    }

    /**
     * Parameterizer.
     *
     * @author Erich Schubert
     *
     * @hidden
     *
     * @param <O> Object type
     */
    public static class Parameterizer<O> extends AbstractParameterizer {
      /**
       * Option parameter for the precomputed similarity matrix.
       */
      public static final OptionID DISTANCE_ID = new OptionID("matrix.similarity", "Similarity function for the precomputed similarity matrix.");

      /**
       * Nested similarity function.
       */
      protected SimilarityFunction<? super O> similarityFunction;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        ObjectParameter<SimilarityFunction<? super O>> similarityP = new ObjectParameter<>(DISTANCE_ID, SimilarityFunction.class);
        if(config.grab(similarityP)) {
          similarityFunction = similarityP.instantiateClass(config);
        }
      }

      @Override
      protected Factory<O> makeInstance() {
        return new Factory<>(similarityFunction);
      }
    }
  }
}
