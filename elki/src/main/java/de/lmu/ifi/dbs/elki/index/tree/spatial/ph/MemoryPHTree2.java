package de.lmu.ifi.dbs.elki.index.tree.spatial.ph;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2014
ETH Zurich, Switzerland and Tilmann Zaeschke

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

import ch.ethz.globis.pht.PhTreeF;
import ch.ethz.globis.pht.PhTreeF.PhEntryF;
import ch.ethz.globis.pht.PhTreeF.PhQueryF;
import ch.ethz.globis.pht.PhTreeF.PhQueryKNNF;
import ch.ethz.globis.pht.pre.IntegerPP;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.Norm;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.LPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SparseLPNormDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.DynamicIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Counter;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Implementation of an in-memory PH-tree. 
 * 
 * @author Tilmann Zaeschke, Erich Schubert
 * 
 * @apiviz.has PHTreeKNNQuery
 * @apiviz.has PHTreeRangeQuery
 * 
 * @param <O> Vector type
 */
@Reference(authors = "T. Zaeschke, C. Zimmerli, M.C. Norrie", title = "The PH-Tree -- A Space-Efficient Storage Structure and Multi-Dimensional Index", booktitle = "Proc. Intl. Conf. on Management of Data (SIGMOD'14), 2014", url = "http://dx.doi.org/10.1145/361002.361007")
public class MemoryPHTree2<O extends NumberVector> extends AbstractIndex<O> 
    implements DynamicIndex,  KNNIndex<O>, RangeIndex<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(MemoryPHTree2.class);

  /**
   * Distance computations performed.
   */
  private long distComputations = 0L;

  /**
   * The PH-Tree instance.
   */
  private final PhTreeF<DBID> tree;

  /**
   * The number of dimensions.
   */
  private int dims = -1;

  /**
   * Counter for comparisons.
   */
  private final Counter objaccess;

  /**
   * Counter for distance computations.
   */
  private final Counter distcalc;

  /**
   * Constructor.
   * 
   * @param relation Relation to index
   */
  public MemoryPHTree2(Relation<O> relation) {
    super(relation);
    if(LOG.isStatistics()) {
      String prefix = this.getClass().getName();
      this.objaccess = LOG.newCounter(prefix + ".objaccess");
      this.distcalc = LOG.newCounter(prefix + ".distancecalcs");
    }
    else {
      this.objaccess = null;
      this.distcalc = null;
    }
    dims = RelationUtil.dimensionality(relation);
    //TODO
    //standard preprocessor
    //tree = PhTreeF.create(dims);
    //IntegerPP: about 20% faster, but slightly less accurate
    tree = PhTreeF.create(dims, new IntegerPP(100L*1000L*1000L));
  }

  @Override
  public void initialize() {
    DBIDIter iter = relation.getDBIDs().iter();

    for(; iter.valid(); iter.advance()) {
      O o = relation.get(iter);
      double[] v = new double[dims];
      for (int k = 0; k < dims; k++) {
        v[k] = o.doubleValue(k);
      }
      DBID id = DBIDUtil.deref(iter);
      tree.put(v, id);
    }
  }


  @Override
  public String getLongName() {
    return "ph-tree";
  }

  @Override
  public String getShortName() {
    return "ph-tree";
  }

  @Override
  public void logStatistics() {
    LOG.statistics(new LongStatistic(this.getClass().getName() + ".distance-computations", 
        distComputations));
    if(objaccess != null) {
      LOG.statistics(objaccess);
    }
    if(distcalc != null) {
      LOG.statistics(distcalc);
    }
  }

  /**
   * Count a single object access.
   */
  protected void countObjectAccess() {
    if(objaccess != null) {
      objaccess.increment();
    }
  }

  /**
   * Count a distance computation.
   */
  protected void countDistanceComputation() {
    if(distcalc != null) {
      distcalc.increment();
    }
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    DistanceFunction<? super O> df = distanceQuery.getDistanceFunction();
    // TODO: if we know this works for other distance functions, add them, too!
    if(df instanceof LPNormDistanceFunction) {
      return new PHTreeKNNQuery(distanceQuery, (Norm<NumberVector>) df);
    }
    if(df instanceof SquaredEuclideanDistanceFunction) {
      return new PHTreeKNNQuery(distanceQuery, (Norm<NumberVector>) df);
    }
    if(df instanceof SparseLPNormDistanceFunction) {
      return new PHTreeKNNQuery(distanceQuery, (Norm<NumberVector>) df);
    }
    return null;
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    DistanceFunction<? super O> df = distanceQuery.getDistanceFunction();
    // TODO: if we know this works for other distance functions, add them, too!
    if(df instanceof LPNormDistanceFunction) {
      return new PHTreeRangeQuery(distanceQuery, (Norm<NumberVector>) df);
    }
    if(df instanceof SquaredEuclideanDistanceFunction) {
      return new PHTreeRangeQuery(distanceQuery, (Norm<NumberVector>) df);
    }
    if(df instanceof SparseLPNormDistanceFunction) {
      return new PHTreeRangeQuery(distanceQuery, (Norm<NumberVector>) df);
    }
    return null;
  }

  /**
   * kNN query for the ph-tree.
   * 
   * @author Tilmann Zaeschke
   */
  public class PHTreeKNNQuery extends AbstractDistanceKNNQuery<O> {
    /**
     * Norm to use.
     */
    private final Norm<O> norm;
    
    private final PhNorm<O> dist;
    
    private final PhQueryKNNF<DBID> query;
    
    private final double[] center;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     * @param norm Norm to use
     */
    public PHTreeKNNQuery(DistanceQuery<O> distanceQuery, Norm<? super O> norm) {
      super(distanceQuery);
      this.norm = (Norm<O>) norm;
      this.dist = new PhNorm(norm, dims, tree.getPreprocessor());
      this.center = new double[dims];
      //use 'k=0' to avoid executing a query here (center = {0,0,...})
      this.query = tree.nearestNeighbour(0, dist, new double[dims]);
    }

    @Override
    public KNNList getKNNForObject(O obj, int k) {
      final KNNHeap knns = DBIDUtil.newHeap(k);
      
      oToDouble(obj, center);
      query.reset(k, dist, null, center);
      while (query.hasNext()) {
        DBID id = query.nextValue();
        O o2 = relation.get(id);
        double distance = norm.distance(obj, o2);
        knns.insert(distance, id);
      }
      
      distComputations += dist.getAndResetDistanceCounter();
      
      return knns.toKNNList();
    }
  }

  /**
   * Range query for the ph-tree.
   */
  public class PHTreeRangeQuery extends AbstractDistanceRangeQuery<O> {
    /**
     * Norm to use.
     */
    private Norm<? super O> norm;

    /**
     * Query instance.
     */
    private PhQueryF<DBID> query;

    /**
     * The query rectangle.
     */
    private final double[] min, max;

    
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     * @param norm Norm to use
     */
    public PHTreeRangeQuery(DistanceQuery<O> distanceQuery, Norm<? super O> norm) {
      super(distanceQuery);
      this.norm = norm;
      this.min = new double[dims];
      this.max = new double[dims];
    }

    @Override
    public void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
      oToDouble(obj, min);
      oToDouble(obj, max);
      range = Math.abs(range);
      for (int i = 0; i < min.length; i++) {
        min[i] = obj.doubleValue(i) - range;
        max[i] = obj.doubleValue(i) + range;
      }
      
      if (query == null) {
        query = tree.query(min, max);
      } else {
        query.reset(min, max);
      }
      
      while (query.hasNext()) {
        PhEntryF<DBID> e = query.nextEntry();
        DBID id = e.getValue();
        O o2 = relation.get(id);
        double distance = norm.distance(obj, o2);
        if (distance <= range) {
          result.add(distance, id);
        }
      }
      result.sort();
    }
  }

  /**
   * Factory class
   * 
   * @author Tilmann Zaeschke
   * 
   * @apiviz.stereotype factory
   * @apiviz.has MinimalisticMemoryPHTree
   * 
   * @param <O> Vector type
   */
  @Alias({ "miniph", "ph" })
  public static class Factory<O extends NumberVector> implements IndexFactory<O, MemoryPHTree2<O>> {
    /**
     * Constructor. Trivial parameterizable.
     */
    public Factory() {
      super();
    }

    @Override
    public MemoryPHTree2<O> instantiate(Relation<O> relation) {
      return new MemoryPHTree2<>(relation);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }
    
    public static class Parametrizer extends AbstractParameterizer {
      @Override
      protected MemoryPHTree2.Factory<NumberVector> makeInstance() {
        return new MemoryPHTree2.Factory<>();
      }
    }
  }

  
  @Override
  public boolean delete(DBIDRef id) {
    O o = relation.get(id);
    return tree.remove(oToDouble(o, new double[dims])) != null;
  }

  @Override
  public void insert(DBIDRef id) {
    O o = relation.get(id);
    tree.put(oToDouble(o, new double[dims]), DBIDUtil.deref(id));
  }

  @Override
  public void deleteAll(DBIDs ids) {
    DBIDIter iter = ids.iter();
    for(; iter.valid(); iter.advance()) {
      delete(iter);
    }
  }

  @Override
  public void insertAll(DBIDs ids) {
    DBIDIter iter = ids.iter();
    for(; iter.valid(); iter.advance()) {
      insert(iter);
    }
  }
  
  private double[] oToDouble(O o, double[] v) {
    for (int k = 0; k < dims; k++) {
      v[k] = o.doubleValue(k);
    }
    return v;
  }
}
