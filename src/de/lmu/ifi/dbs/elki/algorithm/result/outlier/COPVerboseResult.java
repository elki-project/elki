package de.lmu.ifi.dbs.elki.algorithm.result.outlier;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
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
 * Provides the verbose result of the correlation outlier probability algorithm.
 * 
 * @author Erich Schubert
 */

public class COPVerboseResult<O extends DatabaseObject> extends AbstractResult<O> {

  /**
   * Marker for a file name containing lofs.
   */
  public static final String COP_MARKER = "cop";

  /**
   * A new COPVerboseResult set for a database.
   * 
   * The database needs to contain associations for the computed COPs with
   * <code>AssociationID</code>
   * {@link de.lmu.ifi.dbs.elki.database.AssociationID#COP COP}.
   * 
   * @param db the database containing the COPs as association
   */
  public COPVerboseResult(Database<O> db) {
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
      File lofFile = new File(out.getAbsolutePath() + File.separator + COP_MARKER + FILE_EXTENSION);
      lofFile.getParentFile().mkdirs();
      PrintStream lofOut = new PrintStream(new FileOutputStream(lofFile));
      outputCOP(lofOut, normalization, settings);
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

    outputCOP(outStream, normalization, settings);
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
  private void outputCOP(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {

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

        outStream.print(db.getAssociation(AssociationID.COP, id));
        outStream.print(" ");

        outStream.print((Integer) db.getAssociation(AssociationID.COP_DIM, id));
        outStream.print(" ");

        Vector errv = (Vector) db.getAssociation(AssociationID.COP_ERROR_VECTOR, id);
        for(int i = 0; i < errv.getDimensionality(); i++) {
          outStream.print(errv.get(i));
          outStream.print(" ");
        }

        Matrix datavs = (Matrix) db.getAssociation(AssociationID.COP_DATA_VECTORS, id);
        for(int j = 0; j < datavs.getColumnDimensionality(); j++) {
          Vector datav = datavs.getColumnVector(j);
          for(int i = 0; i < datav.getDimensionality(); i++) {
            outStream.print(datav.get(i));
            outStream.print(" ");
          }
        }

        CorrelationAnalysisSolution sol = (CorrelationAnalysisSolution) db.getAssociation(AssociationID.COP_SOL, id);
        if(sol != null) {
          LinearEquationSystem lq = sol.getNormalizedLinearEquationSystem(normalization);
          if(lq != null) {
            String solution = lq.equationsToString(2);
            solution = solution.replace("\n", "\" \"");
            outStream.print('"' + solution + '"');
          }
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