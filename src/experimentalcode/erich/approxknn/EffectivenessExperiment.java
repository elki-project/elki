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
import java.util.Random;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
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
public class EffectivenessExperiment extends AbstractSFCExperiment {
  private static final Logging LOG = Logging.getLogger(EffectivenessExperiment.class);

  PrimitiveDoubleDistanceFunction<? super NumberVector<?>> distanceFunction = ManhattanDistanceFunction.STATIC;

  @Override
  public void run() {
    final int samplesize = 10000;
    Duration load = new MillisTimeDuration("approxknn.load");
    load.begin();
    Database database = LoadImageNet.loadDatabase("ImageNet-Haralick-1", true);
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
    final int k = 101;
    final int[] halfwins = { 50, 100, 150, 200 }; // Half window widths
    Random rnd = new Random(0);
    final DBIDs subset = DBIDUtil.randomSample(ids, samplesize, rnd);

    // TODO: use a distance function that counts the number of distance
    // computations.
    DistanceQuery<NumberVector<?>, DoubleDistance> distq = database.getDistanceQuery(rel, distanceFunction);
    KNNQuery<NumberVector<?>, DoubleDistance> knnq = database.getKNNQuery(distq, k);

    // The curve combinations to test:
    String[] sfc_names = { "z1", "p1", "h1",//
    "z2", "p2", "h2",//
    "z3", "p3", "h3", //
    "z123", "p123", "h123", //
    "zph1", "all9", //
    "random", //
    };
    int[] sfc_masks = { 1, 2, 4, //
    8, 16, 32,//
    64, 128, 256, //
    1 | 8 | 64, 2 | 16 | 128, 4 | 32 | 256, //
    1 | 2 | 4, 0x1FF, //
    0, //
    };
    assert (sfc_names.length == sfc_masks.length);
    final int numvars = sfc_masks.length * halfwins.length;

    Duration qtime = new MillisTimeDuration("approxnn.querytime");
    qtime.begin();
    MeanVariance[] distcmv = MeanVariance.newArray(numvars);
    MeanVariance[] recallmv = MeanVariance.newArray(numvars);
    MeanVariance[] kdistmv = MeanVariance.newArray(numvars);
    for (DBIDIter id = subset.iter(); id.valid(); id.advance()) {
      NumberVector<?> vec = rel.get(id);
      // Get the exact nearest neighbors (use an index, luke!)
      final KNNList<DoubleDistance> trueNN = knnq.getKNNForObject(vec, k);
      SetDBIDs trueNNS = DBIDUtil.newHashSet(trueNN);
      double truedist = trueNN.getKNNDistance().doubleValue();

      int[] posi = positions.get(id);

      for (int c = 0; c < sfc_masks.length; c++) {
        for (int w = 0; w < halfwins.length; w++) {
          final int varnum = w * sfc_masks.length + c;
          if (sfc_masks[c] > 0) {
            DBIDs cands = mergeCandidates(ids, numcurves, sfc_masks[c], curves, halfwins[w], id, posi);
            // Number of distance computations; exclude self.
            distcmv[varnum].put(cands.size() - 1);
            // Recall of true kNNs
            recallmv[varnum].put(Math.min(1., DBIDUtil.intersectionSize(trueNNS, cands) / (double) k));
            // Compute kdist in approximated kNNs:
            if (truedist > 0) {
              DoubleDistanceKNNHeap heap = (DoubleDistanceKNNHeap) DBIDUtil.newHeap(DoubleDistance.ZERO_DISTANCE, k);
              for (DBIDIter iter = cands.iter(); iter.valid(); iter.advance()) {
                heap.add(distanceFunction.doubleDistance(vec, rel.get(iter)), id);
              }
              kdistmv[varnum].put((heap.doubleKNNDistance() - truedist) / truedist);
            } else {
              // Actually must be correct on duplicates, avoid div by 0.
              kdistmv[varnum].put(1.0);
            }
          } else {
            // Random sampling:
            ModifiableDBIDs cands = DBIDUtil.randomSample(ids, halfwins[w] * 2, rnd);
            cands.add(id);
            // Number of distance computations; exclude self.
            distcmv[varnum].put(cands.size() - 1);
            // Recall of true kNNs
            recallmv[varnum].put(Math.min(1., DBIDUtil.intersectionSize(trueNNS, cands) / (double) k));
            // Compute kdist in approximated kNNs:
            if (truedist > 0) {
              DoubleDistanceKNNHeap heap = (DoubleDistanceKNNHeap) DBIDUtil.newHeap(DoubleDistance.ZERO_DISTANCE, k);
              for (DBIDIter iter = cands.iter(); iter.valid(); iter.advance()) {
                heap.add(distanceFunction.doubleDistance(vec, rel.get(iter)), id);
              }
              kdistmv[varnum].put((heap.doubleKNNDistance() - truedist) / truedist);
            } else {
              // NOT WELL DEFINED, unfortunately. Ignore for now.
            }
          }
        }
      }
    }
    qtime.end();
    LOG.statistics(qtime);
    LOG.statistics(new LongStatistic("approxnn.query.size", ids.size()));
    LOG.statistics(new DoubleStatistic("approxnn.query.time.average", qtime.getDuration() / (double) ids.size()));
    for (int c = 0; c < sfc_masks.length; c++) {
      for (int w = 0; w < halfwins.length; w++) {
        final int varnum = w * sfc_masks.length + c;
        final String prefix = "approxnn." + sfc_names[c] + "-" + w;
        LOG.statistics(new DoubleStatistic(prefix + ".distc.mean", distcmv[varnum].getMean()));
        LOG.statistics(new DoubleStatistic(prefix + ".distc.stddev", distcmv[varnum].getMean()));
        LOG.statistics(new DoubleStatistic(prefix + ".recall.mean", recallmv[varnum].getMean()));
        LOG.statistics(new DoubleStatistic(prefix + ".recall.stddev", recallmv[varnum].getMean()));
        LOG.statistics(new DoubleStatistic(prefix + ".kdist.mean", kdistmv[varnum].getMean()));
        LOG.statistics(new DoubleStatistic(prefix + ".kdist.stddev", kdistmv[varnum].getMean()));
      }
    }
  }

  public ModifiableDBIDs mergeCandidates(DBIDs ids, final int numcurves, int mask, List<ArrayList<SpatialRef>> curves, final int halfwin, DBIDIter id, int[] posi) {
    assert (mask > 0);
    ModifiableDBIDs cands = DBIDUtil.newHashSet();
    cands.add(id);
    for (int c = 0; c < numcurves; c++) {
      // Skip if not selected.
      if (((1 << c) & mask) == 0) {
        continue;
      }
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
    return cands;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  public static void main(String[] args) {
    AbstractApplication.runCLIApplication(EffectivenessExperiment.class, args);
  }
}
