package experimentalcode.erich.approxknn;

import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
/**
 * Simple experiment to estimate the effects of approximating the kNN distances.
 * 
 * @author Erich Schubert
 */
public class ApproxKNNExperiment {
  private static final Logging logger = Logging.getLogger(ApproxKNNExperiment.class);

  final int maxk = 100;

  DistanceFunction<? super NumberVector<?, ?>, DoubleDistance> distanceFunction = EuclideanDistanceFunction.STATIC;

  private void run() {
    Database db = loadDatabase();
    Relation<NumberVector<?, ?>> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    DistanceQuery<NumberVector<?, ?>, DoubleDistance> distanceQuery = db.getDistanceQuery(rel, distanceFunction);
    KNNQuery<NumberVector<?, ?>, DoubleDistance> knnq = db.getKNNQuery(distanceQuery, maxk);

    MeanVariance[] mvs = new MeanVariance[maxk];
    DoubleMinMax[] minmax = new DoubleMinMax[maxk];
    for(int i = 0; i < maxk; i++) {
      mvs[i] = new MeanVariance();
      minmax[i] = new DoubleMinMax();
    }

    FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Processed objects", rel.size(), logger) : null;
    for(DBID id : rel.iterDBIDs()) {
      KNNResult<DoubleDistance> knns = knnq.getKNNForDBID(id, maxk);
      double kdist = knns.getKNNDistance().doubleValue();
      for(int i = 0; i < maxk; i++) {
        double factor = knns.get(i).getDistance().doubleValue() / kdist;
        mvs[i].put(factor);
        minmax[i].put(factor);
      }
      if(prog != null) {
        prog.incrementProcessed(logger);
      }
    }
    if(prog != null) {
      prog.ensureCompleted(logger);
    }
    for(int i = 0; i < maxk; i++) {
      System.out.println((i + 1) + " " + mvs[i].getMean() + " " + mvs[i].getSampleStddev() + " " + minmax[i].getMin() + " " + minmax[i].getMax());
    }
  }

  private Database loadDatabase() {
    try {
      ListParameterization dbpar = new ListParameterization();
      dbpar.addParameter(FileBasedDatabaseConnection.INPUT_ID, "/nfs/multimedia/images/ALOI/ColorHistograms/aloi-hsb-7x2x2.csv.gz");
      // dbpar.addParameter(StaticArrayDatabase.INDEX_ID, "tree.spatial.rstarvariants.rstar.RStarTreeFactory");
      // dbpar.addParameter(TreeIndexFactory.PAGE_SIZE_ID, "5000");
      Database db = ClassGenericsUtil.tryInstantiate(Database.class, StaticArrayDatabase.class, dbpar);
      db.initialize();
      return db;
    }
    catch(Exception e) {
      throw new RuntimeException("Cannot load database.", e);
    }
  }

  public static void main(String[] args) {
    logger.getWrappedLogger().setLevel(Level.INFO);
    try {
      new ApproxKNNExperiment().run();
    }
    catch(Exception e) {
      logger.exception(e);
    }
  }
}