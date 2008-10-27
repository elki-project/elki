package de.lmu.ifi.dbs.elki.algorithm.result.outlier;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparablePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.PairInterface;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides the short result of Multivariate Correlation Outlier Probability.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */

public class MCOPResult<O extends RealVector<O, ?>> extends AbstractResult<O> {

  /**
   * Marker for a file name containing mcop results.
   */
  public static final String MCOP_MARKER = "mcop";

  /**
   * A new MCOPResult set for a database.
   * <p/>
   * The database needs to contain associations for the computed mcops with
   * <code>AssociationID</code>
   * {@link de.lmu.ifi.dbs.elki.database.AssociationID#MCOP MCOP}.
   * 
   * @param db the database containing the MCOPs as association
   */
  public MCOPResult(Database<O> db) {
    super(db);
    this.db = db;
  }

  @Override
  public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try {
      File lofFile = new File(out.getAbsolutePath() + File.separator + MCOP_MARKER + FILE_EXTENSION);
      lofFile.getParentFile().mkdirs();
      PrintStream lofOut = new PrintStream(new FileOutputStream(lofFile));
      outputMCOP(lofOut, normalization, settings);
      lofOut.flush();

    }
    catch(Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
      output(outStream, normalization, settings);
    }
  }

  public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    outputMCOP(outStream, normalization, settings);
    outStream.flush();
  }

  /**
   * Writes the correlation lof data to output.
   * 
   * @param outStream the stream to write to
   * @param normalization Normalization to restore original values according to,
   *        if this action is supported - may remain null.
   * @param settings the settings to be written into the header, if this
   *        parameter is <code>null</code>, no header will be written
   */
  private void outputMCOP(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {

    writeHeader(outStream, settings, null);

    try {
      // build a list for sorting
      ArrayList<ComparablePair<Double, Integer>> l = new ArrayList<ComparablePair<Double, Integer>>(db.size());
      for(Iterator<Integer> it = db.iterator(); it.hasNext();) {
        Integer id = it.next();
        Double mcop = db.getAssociation(AssociationID.MCOP, id);
        l.add(new ComparablePair<Double, Integer>(mcop, id));
      }
      Collections.sort(l, Collections.reverseOrder());

      for(PairInterface<Double, Integer> p : l) {
        Integer id = p.getSecond();

        outStream.print(id);
        outStream.print(" ");

        O object = db.get(id);
        if(normalization != null) {
          O restored = normalization.restore(object);
          outStream.print(restored.toString());
        }
        else {
          outStream.print(object.toString());
        }
        outStream.print(" ");

        String label = db.getAssociation(AssociationID.LABEL, id);
        if(label != null) {
          outStream.print(label);
          outStream.print(" ");
        }

        ClassLabel classLabel = db.getAssociation(AssociationID.CLASS, id);
        if(classLabel != null) {
          outStream.print(classLabel);
          outStream.print(" ");
        }

        outStream.print("MCOP=");
        outStream.print(db.getAssociation(AssociationID.MCOP, id));
        outStream.print(" ");

        Vector errv = db.getAssociation(AssociationID.LOCAL_MODEL, id).getCentroid().minus(object.getColumnVector());
        if(normalization != null) {
          O ev = object.newInstance(errv.getRowPackedCopy());
          O restored = normalization.restore(ev);
          outStream.print(restored.toString());
        }
        else {
          outStream.print(errv.toString());
        }

        outStream.println();
      }
    }
    catch(NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }

    outStream.flush();
  }
}