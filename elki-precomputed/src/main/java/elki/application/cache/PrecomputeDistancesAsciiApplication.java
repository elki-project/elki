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
package elki.application.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.GZIPOutputStream;

import elki.application.AbstractApplication;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Application to precompute pairwise distances into an ascii file.
 * 
 * IDs in the output file will always begin at 0.
 *
 * The result can then be used with the DoubleDistanceParse.
 *
 * Symmetry is assumed.
 * 
 * @author Erich Schubert
 * @since 0.2
 *
 * @param <O> Object type
 */
public class PrecomputeDistancesAsciiApplication<O> extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PrecomputeDistancesAsciiApplication.class);

  /**
   * Gzip file name postfix.
   */
  public static final String GZIP_POSTFIX = ".gz";

  /**
   * Debug flag, to double-check all write operations.
   */
  private boolean debugExtraCheckSymmetry = false;

  /**
   * Data source to process.
   */
  private Database database;

  /**
   * Distance function that is to be cached.
   */
  private Distance<? super O> distance;

  /**
   * Output file.
   */
  private File out;

  /**
   * Constructor.
   * 
   * @param database Data source
   * @param distance Distance function
   * @param out Matrix output file
   */
  public PrecomputeDistancesAsciiApplication(Database database, Distance<? super O> distance, File out) {
    super();
    this.database = database;
    this.distance = distance;
    this.out = out;
  }

  @Override
  public void run() {
    database.initialize();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distance);

    DBIDRange ids = DBIDUtil.assertRange(relation.getDBIDs());
    final int size = ids.size();

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Precomputing distances", (int) (((size - 1) * (long) size) >>> 1), LOG) : null;
    try (PrintStream fout = openStream(out)) {
      DBIDArrayIter id1 = ids.iter(), id2 = ids.iter();
      for(; id1.valid(); id1.advance()) {
        String idstr1 = Integer.toString(id1.getOffset());
        if(debugExtraCheckSymmetry && distanceQuery.distance(id1, id1) != 0.) {
          LOG.warning("Distance function doesn't satisfy d(0,0) = 0.");
        }
        for(id2.seek(id1.getOffset() + 1); id2.valid(); id2.advance()) {
          double d = distanceQuery.distance(id1, id2);
          if(debugExtraCheckSymmetry) {
            double d2 = distanceQuery.distance(id2, id1);
            if(Math.abs(d - d2) > 0.0000001) {
              LOG.warning("Distance function doesn't appear to be symmetric!");
            }
          }
          fout.append(idstr1).append('\t')//
              .append(Integer.toString(id2.getOffset())).append('\t')//
              .append(Double.toString(d)).append('\n');
        }
        if(prog != null) {
          prog.setProcessed(prog.getProcessed() + (size - id1.getOffset() - 1), LOG);
        }
      }
    }
    catch(IOException e) {
      throw new AbortException("Could not write to output file.", e);
    }
    LOG.ensureCompleted(prog);
  }

  /**
   * Open the output stream, using gzip if necessary.
   * 
   * @param out Output file name.
   * @return Output stream
   * @throws IOException
   */
  private static PrintStream openStream(File out) throws IOException {
    OutputStream os = new FileOutputStream(out);
    os = out.getName().endsWith(GZIP_POSTFIX) ? new GZIPOutputStream(os) : os;
    return new PrintStream(os);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<O> extends AbstractApplication.Par {
    /**
     * Parameter that specifies the name of the directory to be re-parsed.
     */
    public static final OptionID DISTANCE_ID = new OptionID("loader.distance", "Distance function to cache.");

    /**
     * Data source to process.
     */
    private Database database = null;

    /**
     * Distance function that is to be cached.
     */
    private Distance<? super O> distance = null;

    /**
     * Output file.
     */
    private File out = null;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<Database>(DATABASE_ID, Database.class, StaticArrayDatabase.class) //
          .grab(config, x -> database = x);
      // Distance function parameter
      new ObjectParameter<Distance<? super O>>(DISTANCE_ID, Distance.class) //
          .grab(config, x -> distance = x);
      // Output file parameter
      out = getParameterOutputFile(config);
    }

    @Override
    public PrecomputeDistancesAsciiApplication<O> make() {
      return new PrecomputeDistancesAsciiApplication<>(database, distance, out);
    }
  }

  /**
   * Main method, delegate to super class.
   * 
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    runCLIApplication(PrecomputeDistancesAsciiApplication.class, args);
  }
}
