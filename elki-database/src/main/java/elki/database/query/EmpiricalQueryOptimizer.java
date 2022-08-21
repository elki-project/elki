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
package elki.database.query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import elki.data.type.FieldTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDRef;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.LPNormDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.index.*;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.utilities.Alias;

/**
 * Class to automatically add indexes to a database.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
@Alias("auto")
public class EmpiricalQueryOptimizer implements QueryOptimizer {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(EmpiricalQueryOptimizer.class);

  /**
   * Megabytes for formatting memory.
   */
  private static final long MEGA = 1024 * 1024;

  /**
   * Distance matrix index class.
   */
  private final Constructor<? extends Index> matrixIndex;

  /**
   * kNN preprocessor class.
   */
  private final Constructor<? extends KNNIndex<?>> knnIndex;

  /**
   * cover tree index class.
   */
  private final Constructor<? extends Index> coverIndex;

  /**
   * vp tree index class.
   */
  private final Constructor<? extends Index> vpIndex;

  /**
   * k-d-tree index class.
   */
  private final Constructor<? extends Index> kdIndex;

  /**
   * Constructor.
   */
  @SuppressWarnings("unchecked")
  public EmpiricalQueryOptimizer() {
    Constructor<? extends DistanceIndex<?>> matrixIndex = null;
    try {
      Class<?> cls = this.getClass().getClassLoader().loadClass("elki.index.distancematrix.PrecomputedDistanceMatrix");
      matrixIndex = (Constructor<? extends DistanceIndex<?>>) cls.getConstructor(Relation.class, DBIDRange.class, Distance.class);
    }
    catch(ClassNotFoundException e) {
      LOG.verbose("PrecomputedDistanceMatrix is not available, and cannot be automatically used for optimization.");
    }
    catch(NoSuchMethodException | SecurityException e) {
      LOG.exception(e);
    }
    this.matrixIndex = matrixIndex;
    //
    Constructor<? extends KNNIndex<?>> knnIndex = null;
    try {
      Class<?> cls = this.getClass().getClassLoader().loadClass("elki.index.preprocessed.knn.MaterializeKNNPreprocessor");
      knnIndex = (Constructor<? extends KNNIndex<?>>) cls.getConstructor(Relation.class, DistanceQuery.class, int.class, boolean.class);
    }
    catch(ClassNotFoundException e) {
      LOG.verbose("MaterializeKNNPreprocessor is not available, and cannot be automatically used for optimization.");
    }
    catch(NoSuchMethodException | SecurityException e) {
      LOG.exception(e);
    }
    this.knnIndex = knnIndex;
    //
    Constructor<? extends Index> coverIndex = null;
    try {
      Class<?> cls = this.getClass().getClassLoader().loadClass("elki.index.tree.metrical.covertree.CoverTree");
      coverIndex = (Constructor<? extends Index>) cls.getConstructor(Relation.class, Distance.class, int.class);
    }
    catch(ClassNotFoundException e) {
      LOG.verbose("CoverTree is not available, and cannot be automatically used for optimization.");
    }
    catch(NoSuchMethodException | SecurityException e) {
      LOG.exception(e);
    }
    this.coverIndex = coverIndex;
    //
    Constructor<? extends Index> vpIndex = null;
    try {
      Class<?> cls = this.getClass().getClassLoader().loadClass("elki.index.tree.metrical.vptree.VPTree");
      vpIndex = (Constructor<? extends Index>) cls.getConstructor(Relation.class, Distance.class, int.class);
    }
    catch(ClassNotFoundException e) {
      LOG.verbose("VPTree is not available, and cannot be automatically used for optimization.");
    }
    catch(NoSuchMethodException | SecurityException e) {
      LOG.exception(e);
    }
    this.vpIndex = vpIndex;
    //
    Constructor<? extends Index> kdIndex = null;
    try {
      Class<?> cls = this.getClass().getClassLoader().loadClass("elki.index.tree.spatial.kd.MemoryKDTree");
      kdIndex = (Constructor<? extends Index>) cls.getConstructor(Relation.class, int.class);
    }
    catch(ClassNotFoundException e) {
      LOG.verbose("MemoryKDTree is not available, and cannot be automatically used for optimization.");
    }
    catch(NoSuchMethodException | SecurityException e) {
      LOG.exception(e);
    }
    this.kdIndex = kdIndex;
  }

  @Override
  public <O> DistanceQuery<O> getDistanceQuery(Relation<? extends O> relation, Distance<? super O> distance, int flags) {
    if((flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
      @SuppressWarnings("unchecked")
      DistanceIndex<O> idx = (DistanceIndex<O>) makeMatrixIndex(relation, distance);
      if(idx != null) {
        if((flags & QueryBuilder.FLAG_NO_CACHE) == 0) {
          Metadata.hierarchyOf(relation).addWeakChild(idx);
        }
        return ((DistanceIndex<O>) idx).getDistanceQuery(distance);
      }
    }
    return null;
  }

  @Override
  public <O> KNNSearcher<O> kNNByObject(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, int maxk, int flags) {
    KNNIndex<O> idx = null;
    // Try adding a preprocessor if requested:
    if((flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
      idx = makeKnnPreprocessor(relation, distanceQuery, maxk, flags & ~QueryBuilder.FLAG_PRECOMPUTE);
    }
    if(idx == null) { // try k-d-tree
      idx = makeKDTree(relation, distanceQuery.getDistance(), 3 /* empirical */);
    }
    if(idx == null) { // VP tree is cheap and fast
      idx = makeVPTree(relation, distanceQuery.getDistance(), 5 /* empirical */);
    }
    if(idx == null) { // cover tree is cheap and fast
      idx = makeCoverTree(relation, distanceQuery.getDistance(), 20 /* empirical */);
    }
    if(idx != null) {
      if((flags & QueryBuilder.FLAG_NO_CACHE) == 0) {
        Metadata.hierarchyOf(relation).addWeakChild(idx);
      }
      return idx.kNNByObject(distanceQuery, maxk, flags);
    }
    return null;
  }

  @Override
  public <O> KNNSearcher<DBIDRef> kNNByDBID(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, int maxk, int flags) {
    KNNIndex<O> idx = null;
    // Try adding a preprocessor if requested:
    if((flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
      idx = makeKnnPreprocessor(relation, distanceQuery, maxk, flags & ~QueryBuilder.FLAG_PRECOMPUTE);
    }
    if(idx == null) { // try k-d-tree
      idx = makeKDTree(relation, distanceQuery.getDistance(), 3 /* empirical */);
    }
    if(idx == null) { // VP tree is cheap and fast
      idx = makeVPTree(relation, distanceQuery.getDistance(), 5 /* empirical */);
    }
    if(idx == null) { // cover tree is cheap and fast
      idx = makeCoverTree(relation, distanceQuery.getDistance(), 20 /* empirical */);
    }
    if(idx == null && (flags & QueryBuilder.FLAG_PRECOMPUTE) != 0 && relation.getDBIDs() instanceof DBIDRange) {
      idx = makeMatrixIndex(relation, distanceQuery.getDistance());
    }
    if(idx != null) {
      if((flags & QueryBuilder.FLAG_NO_CACHE) == 0) {
        Metadata.hierarchyOf(relation).addWeakChild(idx);
      }
      return idx.kNNByDBID(distanceQuery, maxk, flags);
    }
    return null;
  }

  @Override
  public <O> RangeSearcher<O> rangeByObject(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    RangeIndex<O> idx = null;
    if(idx == null) { // Try k-d-tree
      idx = makeKDTree(relation, distanceQuery.getDistance(), 3 /* empirical */);
    }
    if(idx == null) { // VP tree is cheap and fast
      idx = makeVPTree(relation, distanceQuery.getDistance(), 5 /* empirical */);
    }
    if(idx == null) {
      idx = makeCoverTree(relation, distanceQuery.getDistance(), 20 /* empirical */);
    }
    if(idx == null) {
      return null;
    }
    if((flags & QueryBuilder.FLAG_NO_CACHE) == 0) {
      Metadata.hierarchyOf(relation).addWeakChild(idx);
    }
    return idx.rangeByObject(distanceQuery, maxrange, flags);
  }

  @Override
  public <O> RangeSearcher<DBIDRef> rangeByDBID(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    RangeIndex<O> idx = null;
    if(idx == null) { // Try k-d-tree
      idx = makeKDTree(relation, distanceQuery.getDistance(), 3 /* empirical */);
    }
    if(idx == null) { // VP tree is cheap and fast
      idx = makeVPTree(relation, distanceQuery.getDistance(), 5 /* empirical */);
    }
    if(idx == null) {
      idx = makeCoverTree(relation, distanceQuery.getDistance(), 20 /* empirical */);
    }
    if(idx == null && (flags & QueryBuilder.FLAG_PRECOMPUTE) != 0 && relation.getDBIDs() instanceof DBIDRange) {
      idx = makeMatrixIndex(relation, distanceQuery.getDistance());
    }
    if(idx == null) {
      return null;
    }
    if((flags & QueryBuilder.FLAG_NO_CACHE) == 0) {
      Metadata.hierarchyOf(relation).addWeakChild(idx);
    }
    return idx.rangeByDBID(distanceQuery, maxrange, flags);
  }

  @Override
  public <O> PrioritySearcher<O> priorityByObject(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    DistancePriorityIndex<O> idx = null;
    if(idx == null) { // VP tree is cheap and fast
      idx = makeVPTree(relation, distanceQuery.getDistance(), 8 /* empirical */);
    }
    if(idx == null) {
      idx = makeCoverTree(relation, distanceQuery.getDistance(), 20 /* empirical */);
    }
    if(idx == null) { // Try k-d-tree for squared Euclidean mostly
      idx = makeKDTree(relation, distanceQuery.getDistance(), 10 /* empirical */);
    }
    if(idx == null) {
      return null;
    }
    if((flags & QueryBuilder.FLAG_NO_CACHE) == 0) {
      Metadata.hierarchyOf(relation).addWeakChild(idx);
    }
    return idx.priorityByObject(distanceQuery, maxrange, flags);
  }

  @Override
  public <O> PrioritySearcher<DBIDRef> priorityByDBID(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    DistancePriorityIndex<O> idx = null;
    if(idx == null) { // VP tree is cheap and fast
      idx = makeVPTree(relation, distanceQuery.getDistance(), 8 /* needs optimization and benchmark */);
    }
    if(idx == null) {
      idx = makeCoverTree(relation, distanceQuery.getDistance(), 20 /* needs optimization and benchmark */);
    }
    if(idx == null) { // Try k-d-tree for squared Euclidean mostly
      idx = makeKDTree(relation, distanceQuery.getDistance(), 10 /* needs optimization and benchmark */);
    }
    if(idx == null && (flags & QueryBuilder.FLAG_PRECOMPUTE) != 0) {
      idx = makeMatrixIndex(relation, distanceQuery.getDistance());
    }
    if(idx == null) {
      return null;
    }
    if((flags & QueryBuilder.FLAG_NO_CACHE) == 0) {
      Metadata.hierarchyOf(relation).addWeakChild(idx);
    }
    return idx.priorityByDBID(distanceQuery, maxrange, flags);
  }

  private <O> DistancePriorityIndex<O> makeMatrixIndex(Relation<? extends O> relation, Distance<? super O> distance) {
    // TODO: make sure there is not matrix already!
    if(matrixIndex == null || relation.size() > 65536) {
      return null;
    }
    long freeMemory = getFreeMemory();
    final long msize = relation.size() * 4L * relation.size();
    if(msize > 0.8 * freeMemory) {
      LOG.warning("An automatic distance matrix would need about " + formatMemory(msize) + " memory, only " + formatMemory(freeMemory) + " are available.");
      return null;
    }
    if(!(relation.getDBIDs() instanceof DBIDRange)) {
      LOG.warning("Optimizer: Precomputed distance matrixes can currently only be generated for a fixed DBID range - performance may be suboptimal.");
      // TODO: add an automatic distance cache instead, c.f., CLARA?
      return null;
    }
    try {
      @SuppressWarnings("unchecked")
      DistancePriorityIndex<O> idx = (DistancePriorityIndex<O>) matrixIndex.newInstance(relation, (DBIDRange) relation.getDBIDs(), distance);
      LOG.verbose("Optimizer: automatically adding a distance matrix.");
      idx.initialize();
      return idx;
    }
    catch(InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      LOG.exception("Automatic distance-matrix creation failed.", e);
      return null;
    }
  }

  private <O> DistancePriorityIndex<O> makeCoverTree(Relation<? extends O> relation, Distance<? super O> distance, int leafsize) {
    // TODO: make sure there is no such cover tree already!
    if(coverIndex == null || !distance.isMetric()) {
      return null;
    }
    // TODO: auto-tune parameters based on dimensionality or sample?
    try {
      @SuppressWarnings("unchecked")
      DistancePriorityIndex<O> idx = (DistancePriorityIndex<O>) coverIndex.newInstance(relation, distance, leafsize);
      LOG.verbose("Optimizer: automatically adding a cover tree index.");
      idx.initialize();
      return idx;
    }
    catch(InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      LOG.exception("Automatic cover tree creation failed.", e);
    }
    return null;
  }

  private <O> DistancePriorityIndex<O> makeVPTree(Relation<? extends O> relation, Distance<? super O> distance, int leafsize) {
    // TODO: make sure there is no such VP tree already!
    if(vpIndex == null || !distance.isMetric()) {
      return null;
    }
    // TODO: auto-tune parameters based on dimensionality or sample?
    try {
      @SuppressWarnings("unchecked")
      DistancePriorityIndex<O> idx = (DistancePriorityIndex<O>) vpIndex.newInstance(relation, distance, leafsize);
      LOG.verbose("Optimizer: automatically adding a VP tree index.");
      idx.initialize();
      return idx;
    }
    catch(InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      LOG.exception("Automatic VP tree creation failed.", e);
    }
    return null;
  }

  private <O> DistancePriorityIndex<O> makeKDTree(Relation<? extends O> relation, Distance<? super O> distance, int k) {
    // TODO: make sure there is no such k-d-tree already!
    TypeInformation type = relation.getDataTypeInformation();
    if(kdIndex == null // not available
        || !TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(type) //
        || !(distance instanceof LPNormDistance || distance instanceof SquaredEuclideanDistance) //
        || ((FieldTypeInformation) type).getDimensionality() > 30) {
      return null;
    }
    try {
      @SuppressWarnings("unchecked")
      DistancePriorityIndex<O> idx = (DistancePriorityIndex<O>) kdIndex.newInstance(relation, k > 0 ? k : 0);
      LOG.verbose("Optimizer: automatically adding a k-d-tree index.");
      idx.initialize();
      return idx;
    }
    catch(InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      LOG.exception("Automatic k-d-tree creation failed.", e);
    }
    return null;
  }

  /**
   * Make a knn preprocessor.
   *
   * @param <O> Object type
   * @param relation Data relation
   * @param distanceQuery distance query
   * @param maxk Maximum k
   * @param flags Optimizer flags
   * @return knn preprocessor
   */
  @SuppressWarnings("unchecked")
  private <O> KNNIndex<O> makeKnnPreprocessor(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if(knnIndex == null) {
      return null;
    }
    long freeMemory = getFreeMemory();
    final long msize = maxk * 12L * relation.size();
    if(msize > 0.8 * freeMemory) {
      LOG.warning("Precomputing the kNN would need about " + formatMemory(msize) + " memory, only " + formatMemory(freeMemory) + " are available.");
      return null;
    }
    try {
      KNNIndex<O> idx = (KNNIndex<O>) knnIndex.newInstance(relation, distanceQuery, maxk, true);
      LOG.verbose("Optimizer: Automatically adding a knn preprocessor.");
      idx.initialize();
      if((flags & QueryBuilder.FLAG_NO_CACHE) == 0) {
        Metadata.hierarchyOf(relation).addWeakChild(idx);
      }
      return idx;
    }
    catch(InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      LOG.exception("Automatic knn preprocessor creation failed.", e);
    }
    return null;
  }

  /**
   * Get the currently free amount of memory.
   *
   * @return Free memory
   */
  private static long getFreeMemory() {
    final Runtime r = Runtime.getRuntime();
    return r.freeMemory() + r.maxMemory() - r.totalMemory();
  }

  /**
   * Format a memory amount.
   *
   * @param mem Memory
   * @return Memory amount
   */
  private static String formatMemory(long mem) {
    return mem < 2500 * MEGA ? ((int) (mem * 10. / MEGA)) / 10. + "M" : //
        ((int) (mem / 102.4 / MEGA)) / 10. + "G";
  }
}
