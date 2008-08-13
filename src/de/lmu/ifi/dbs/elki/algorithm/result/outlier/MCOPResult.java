package de.lmu.ifi.dbs.elki.algorithm.result.outlier;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * Provides the short result of Multivariate Correlation Outlier Probability.
 * 
 * @author Erich Schubert
 */

public class MCOPResult<O extends DatabaseObject> extends AbstractResult<O> {

  /**
   * Marker for a file name containing lofs.
   */
  public static final String MCOP_MARKER = "mcop";

  /**
   * A new MCOPResult set for a database.
   * 
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

  /**
   * @see AbstractResult#output(java.io.File,
   *      de.lmu.ifi.dbs.elki.normalization.Normalization, java.util.List)
   */
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

  /**
   * @see AbstractResult#output(java.io.PrintStream,
   *      de.lmu.ifi.dbs.elki.normalization.Normalization, java.util.List)
   */
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

      // write lofs
      for(Iterator<Integer> it = db.iterator(); it.hasNext();) {
        Integer id = it.next();

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

        String label = (String) db.getAssociation(AssociationID.LABEL, id);
        if(label != null) {
          outStream.print(label);
          outStream.print(" ");
        }

        ClassLabel<?> classLabel = (ClassLabel<?>) db.getAssociation(AssociationID.CLASS, id);
        if(classLabel != null) {
          outStream.print(classLabel);
          outStream.print(" ");
        }

        outStream.print(db.getAssociation(AssociationID.MCOP, id));
        outStream.println();
      }
    }
    catch(NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }

    outStream.flush();
  }

}