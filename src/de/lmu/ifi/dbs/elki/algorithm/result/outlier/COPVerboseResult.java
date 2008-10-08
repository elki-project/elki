package de.lmu.ifi.dbs.elki.algorithm.result.outlier;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparablePair;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides the verbose result of the correlation outlier probability algorithm.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */

public class COPVerboseResult<O extends RealVector<O, ?>> extends AbstractResult<O> {

  /**
   * Marker for a file name containing lofs.
   */
  public static final String COP_MARKER = "cop";

  /**
   * A new COPVerboseResult set for a database. <p/> The database needs to
   * contain associations for the computed COPs with <code>AssociationID</code>
   * {@link de.lmu.ifi.dbs.elki.database.AssociationID#COP COP}.
   * 
   * @param db the database containing the COPs as association
   */
  public COPVerboseResult(Database<O> db) {
    super(db);
    this.db = db;
  }

  @Override
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

  public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    outputCOP(outStream, normalization, settings);
    outStream.flush();
  }

  /**
   * Writes the correlation outlier probability data to output.
   * 
   * @param outStream the stream to write to
   * @param normalization Normalization to restore original values according to,
   *        if this action is supported - may remain null.
   * @param settings the settings to be written into the header, if this
   *        parameter is <code>null</code>, no header will be written
   */
  @SuppressWarnings("unchecked")
  private void outputCOP(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {

    writeHeader(outStream, settings, null);

    try {
      // build a list for sorting
      ArrayList<ComparablePair<Double, Integer>> l = new ArrayList<ComparablePair<Double, Integer>>(db.size());
      for (Iterator<Integer> it = db.iterator(); it.hasNext(); ) {
        Integer id = it.next();
        Double cop = db.getAssociation(AssociationID.COP, id);
        l.add(new ComparablePair<Double, Integer>(cop, id));
      }
      Collections.sort(l, Collections.reverseOrder());

      // write cop scores
      for(ComparablePair<Double, Integer> p : l) {
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

        outStream.print("COP=");
        outStream.print(db.getAssociation(AssociationID.COP, id));
        outStream.print(" ");

        outStream.print(db.getAssociation(AssociationID.COP_DIM, id));
        outStream.print(" ");

        if (true) {
          Vector errv = db.getAssociation(AssociationID.COP_ERROR_VECTOR, id);
          if(normalization != null) {
            O ev = object.newInstance(errv.getRowPackedCopy());
            O restored = normalization.restore(ev);
            outStream.print(restored.toString());
          }
          else {
            outStream.print(errv.toString());
          }
        }

        if (false) {
          Matrix datavs = db.getAssociation(AssociationID.COP_DATA_VECTORS, id);
          for(int j = 0; j < datavs.getColumnDimensionality(); j++) {
            Vector datav = datavs.getColumnVector(j);
            if(normalization != null) {
              O dv = object.newInstance(datav.getRowPackedCopy());
              O restored = normalization.restore(dv);
              outStream.print(restored.toString());
            }
            else {
              outStream.print(datav.toString());
            }
          }
        }

        if (false) {
          CorrelationAnalysisSolution<O> sol = (CorrelationAnalysisSolution<O>) db.getAssociation(AssociationID.COP_SOL, id);
          if(sol != null) {
            // test first if we actually have an equation system.
            if(sol.getNormalizedLinearEquationSystem(null) != null) {
              LinearEquationSystem lq = sol.getNormalizedLinearEquationSystem(normalization);
              String solution = lq.equationsToString(2);
              solution = solution.replace("\n", "\" \"");
              outStream.print('"' + solution + '"');
            }
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