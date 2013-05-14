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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.logging.statistics.MillisTimeDuration;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.AbstractSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.HilbertSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.PeanoSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.SpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorter;
import experimentalcode.erich.approxknn.SpacefillingKNNPreprocessor.SpatialRef;

/**
 * Simple experiment to estimate the effects of approximating the kNN with space
 * filling curves.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractSFCExperiment extends AbstractApplication {
  protected WritableDataStore<int[]> indexPositions(DBIDs ids, final int numcurves, List<ArrayList<SpatialRef>> curves) {
    // Build position index, DBID -> position in the three curves
    WritableDataStore<int[]> positions = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, int[].class);
    {
      {
        ArrayList<SpatialRef> first = curves.get(0);
        Iterator<SpatialRef> it = first.iterator();
        for (int i = 0; it.hasNext(); i++) {
          SpatialRef r = it.next();
          final int[] buf = new int[numcurves];
          Arrays.fill(buf, -1);
          buf[0] = i;
          positions.put(r.id, buf);
        }
      }
      for (int c = 1; c < numcurves; c++) {
        Iterator<SpatialRef> it = curves.get(c).iterator();
        for (int i = 0; it.hasNext(); i++) {
          SpatialRef r = it.next();
          int[] data = positions.get(r.id);
          data[c] = i;
          positions.put(r.id, data);
        }
      }
    }
    return positions;
  }

  protected List<ArrayList<SpatialRef>> initializeCurves(Relation<NumberVector<?>> rel, DBIDs ids, final int numcurves) {
    Duration proj = new MillisTimeDuration("approxnn.project");
    proj.begin();
    List<ArrayList<SpatialRef>> curves = allocateCurves(rel, ids, numcurves);

    // Sort spatially
    double[] mms = AbstractSpatialSorter.computeMinMax(curves.get(0));
    // Actually use the maximal extends across axes, to avoid distortion!
    {
      DoubleMinMax mm = new DoubleMinMax();
      mm.put(mms);
      for (int i = 0; i < mms.length; i += 2) {
        mms[i] = mm.getMin();
        mms[i + 1] = mm.getMax();
      }
    }

    sortSpatially("z1", new ZCurveSpatialSorter(), curves.get(0), mms);
    sortSpatially("p1", new PeanoSpatialSorter(), curves.get(1), mms);
    sortSpatially("h1", new HilbertSpatialSorter(), curves.get(2), mms);

    if (numcurves > 3) {
      double[] mms2 = new double[mms.length], mms3 = new double[mms.length];
      for (int i = 0; i < mms.length; i += 2) {
        double len = mms[i + 1] - mms[i];
        // Grow each axis by the same factor, to avoid distortion!
        mms2[i] = mms[i] - len * .12345678;
        mms2[i + 1] = mms[i + 1] + len * .3784123;
        mms3[i] = mms[i] - len * .321078;
        mms3[i + 1] = mms[i + 1] + len * .51824172;
      }
      sortSpatially("z2", new ZCurveSpatialSorter(), curves.get(3), mms2);
      sortSpatially("p2", new PeanoSpatialSorter(), curves.get(4), mms2);
      sortSpatially("h2", new HilbertSpatialSorter(), curves.get(5), mms2);
      if (numcurves > 6) {
        sortSpatially("z3", new ZCurveSpatialSorter(), curves.get(6), mms3);
        sortSpatially("p3", new PeanoSpatialSorter(), curves.get(7), mms3);
        sortSpatially("h3", new HilbertSpatialSorter(), curves.get(8), mms3);
      }
    }
    // End all projections:
    proj.end();
    getLogger().statistics(proj);
    return curves;
  }

  abstract protected Logging getLogger();

  protected static List<ArrayList<SpatialRef>> allocateCurves(Relation<NumberVector<?>> rel, DBIDs ids, final int numcurves) {
    List<ArrayList<SpatialRef>> curves = new ArrayList<>(numcurves);
    ArrayList<SpatialRef> first = new ArrayList<>(ids.size());
    for (DBIDIter id = ids.iter(); id.valid(); id.advance()) {
      final NumberVector<?> v = rel.get(id);
      SpatialRef ref = new SpatialRef(DBIDUtil.deref(id), v);
      first.add(ref);
    }
    curves.add(first);
    for (int i = 1; i < numcurves; i++) {
      curves.add(new ArrayList<>(first));
    }
    return curves;
  }

  protected void sortSpatially(String name, SpatialSorter spatialSorter, ArrayList<SpatialRef> c, double[] mms) {
    Duration dur = new MillisTimeDuration("approxnn.sort-" + name);
    dur.begin();
    spatialSorter.sort(c, 0, c.size(), mms);
    dur.end();
    getLogger().statistics(dur);
  }
}
