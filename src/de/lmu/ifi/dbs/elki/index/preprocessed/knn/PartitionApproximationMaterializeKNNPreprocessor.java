package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

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

import java.util.HashMap;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Partitioning Approximate kNN Preprocessor")
@Description("Caterializes the (approximate) k nearest neighbors of objects of a database by partitioning and only computing kNN within each partition.")
public class PartitionApproximationMaterializeKNNPreprocessor<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor<O, D, KNNResult<D>> {
  // TODO: randomize/shuffle?

  /**
   * Logger to use
   */
  private static final Logging LOG = Logging.getLogger(PartitionApproximationMaterializeKNNPreprocessor.class);

  /**
   * Number of partitions to use.
   */
  private final int partitions;

  /**
   * Constructor
   * 
   * @param relation Relation to process
   * @param distanceFunction the distance function to use
   * @param k query k
   * @param partitions Number of partitions
   */
  public PartitionApproximationMaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k, int partitions) {
    super(relation, distanceFunction, k);
    this.partitions = partitions;
  }

  @Override
  protected void preprocess() {
    DistanceQuery<O, D> distanceQuery = relation.getDatabase().getDistanceQuery(relation, distanceFunction);
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNResult.class);
    MeanVariance ksize = new MeanVariance();
    if(LOG.isVerbose()) {
      LOG.verbose("Approximating nearest neighbor lists to database objects");
    }

    ArrayDBIDs aids = DBIDUtil.ensureArray(relation.getDBIDs());
    int minsize = (int) Math.floor(aids.size() / partitions);

    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Processing partitions.", partitions, LOG) : null;
    for(int part = 0; part < partitions; part++) {
      int size = (partitions * minsize + part >= aids.size()) ? minsize : minsize + 1;
      // Collect the ids in this node.
      ArrayModifiableDBIDs ids = DBIDUtil.newArray(size);
      {
        DBIDArrayIter iter = ids.iter();
        // Offset
        iter.seek(part);
        // Seek in steps of "partitions".
        for(; iter.valid(); iter.advance(partitions)) {
          ids.add(iter);
        }
      }
      HashMap<DBIDPair, D> cache = new HashMap<DBIDPair, D>(size * size * 3 / 8);
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        KNNHeap<D> kNN = KNNUtil.newHeap(distanceFunction, k);
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          DBIDPair key = DBIDUtil.newPair(iter, iter2);
          D d = cache.remove(key);
          if(d != null) {
            // consume the previous result.
            kNN.add(d, iter2);
          }
          else {
            // compute new and store the previous result.
            d = distanceQuery.distance(iter, iter2);
            kNN.add(d, iter2);
            // put it into the cache, but with the keys reversed
            key = DBIDUtil.newPair(iter2, iter);
            cache.put(key, d);
          }
        }
        ksize.put(kNN.size());
        storage.put(iter, kNN.toKNNList());
      }
      if(LOG.isDebugging()) {
        if(cache.size() > 0) {
          LOG.warning("Cache should be empty after each run, but still has " + cache.size() + " elements.");
        }
      }
      if(progress != null) {
        progress.incrementProcessed(LOG);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(LOG);
    }
    if(LOG.isVerbose()) {
      LOG.verbose("On average, " + ksize.getMean() + " +- " + ksize.getSampleStddev() + " neighbors returned.");
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public String getLongName() {
    return "Random partition kNN approximation";
  }

  @Override
  public String getShortName() {
    return "random-partition-knn";
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses PartitionApproximationMaterializeKNNPreprocessor oneway - -
   *              «create»
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Factory<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory<O, D, KNNResult<D>> {
    /**
     * Parameter to specify the number of partitions to use for materializing
     * the kNN. Must be an integer greater than 1.
     * <p>
     * Key: {@code -partknn.p}
     * </p>
     */
    public static final OptionID PARTITIONS_ID = OptionID.getOrCreateOptionID("partknn.p", "The number of partitions to use for approximate kNN.");

    /**
     * The number of partitions to use
     */
    int partitions;

    /**
     * Constructor.
     * 
     * @param k k
     * @param distanceFunction distance function
     * @param partitions number of partitions
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction, int partitions) {
      super(k, distanceFunction);
      this.partitions = partitions;
    }

    @Override
    public PartitionApproximationMaterializeKNNPreprocessor<O, D> instantiate(Relation<O> relation) {
      PartitionApproximationMaterializeKNNPreprocessor<O, D> instance = new PartitionApproximationMaterializeKNNPreprocessor<O, D>(relation, distanceFunction, k, partitions);
      return instance;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O, D> {
      protected int partitions = 0;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final IntParameter partitionsP = new IntParameter(PARTITIONS_ID);
        partitionsP.addConstraint(new GreaterConstraint(1));
        if(config.grab(partitionsP)) {
          partitions = partitionsP.getValue();
        }
      }

      @Override
      protected Factory<O, D> makeInstance() {
        return new Factory<O, D>(k, distanceFunction, partitions);
      }
    }
  }
}