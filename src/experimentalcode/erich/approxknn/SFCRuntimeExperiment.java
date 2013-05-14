package experimentalcode.erich.approxknn;

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
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.MillisTimeDuration;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import experimentalcode.erich.approxknn.SpacefillingKNNPreprocessor.SpatialRef;

/**
 * Simple experiment to estimate the effects of approximating the kNN with space
 * filling curves.
 * 
 * @author Erich Schubert
 */
public class SFCRuntimeExperiment extends AbstractSFCExperiment {
  private static final Logging LOG = Logging.getLogger(SFCRuntimeExperiment.class);

  PrimitiveDoubleDistanceFunction<? super NumberVector<?>> distanceFunction = ManhattanDistanceFunction.STATIC;

  @Override
  public void run() {
    // final int samplesize = 1000000;
    Duration load = new MillisTimeDuration("approxknn.load");
    load.begin();
    Database database = LoadImageNet.loadDatabase("ImageNet-Haralick-1", false);
    Relation<NumberVector<?>> rel = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    DBIDs ids = rel.getDBIDs();
    load.end();
    LOG.statistics(new LongStatistic("approxknn.dataset.numobj", ids.size()));
    LOG.statistics(new LongStatistic("approxknn.dataset.dims", RelationUtil.dimensionality(rel)));
    LOG.statistics(load);

    final int numcurves = 9;
    List<ArrayList<SpatialRef>> curves = initializeCurves(rel, ids, numcurves);
    WritableDataStore<int[]> positions = indexPositions(ids, numcurves, curves);

    // True kNN value
    final int k = 50;
    final int halfwin = k; // Half window width

    Duration qtime = new MillisTimeDuration("approxnn.querytime");
    qtime.begin();
    MeanVariance candmv = new MeanVariance();
    DBIDs subset = ids; // DBIDUtil.randomSample(ids, samplesize, 0L);
    for (DBIDIter id = subset.iter(); id.valid(); id.advance()) {
      int[] posi = positions.get(id);
      ModifiableDBIDs cands = DBIDUtil.newHashSet();
      cands.add(id);
      for (int c = 0; c < numcurves; c++) {
        ArrayList<SpatialRef> curve = curves.get(c);
        assert (DBIDUtil.equal(curve.get(posi[c]).id, id));
        for (int off = 1; off <= halfwin; off++) {
          if (posi[c] - off >= 0) {
            cands.add(curve.get(posi[c] - off).id);
          }
          if (posi[c] + off < ids.size()) {
            cands.add(curve.get(posi[c] + off).id);
          }
        }
      }
      candmv.put(cands.size());
      DoubleDistanceKNNHeap heap = (DoubleDistanceKNNHeap) DBIDUtil.newHeap(DoubleDistance.ZERO_DISTANCE, k);
      NumberVector<?> qo = rel.get(id);
      for (DBIDIter iter = cands.iter(); iter.valid(); iter.advance()) {
        heap.add(distanceFunction.doubleDistance(qo, rel.get(iter)), id);
      }
    }
    qtime.end();
    LOG.statistics(qtime);
    LOG.statistics(new LongStatistic("approxnn.query.size", ids.size()));
    LOG.statistics(new DoubleStatistic("approxnn.query.time.average", qtime.getDuration() / (double) ids.size()));
    LOG.statistics(new DoubleStatistic("approxnn.candidates.mean", candmv.getMean()));
    LOG.statistics(new DoubleStatistic("approxnn.candidates.stddev", candmv.getSampleStddev()));
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  public static void main(String[] args) {
    AbstractApplication.runCLIApplication(SFCRuntimeExperiment.class, args);
  }
}
