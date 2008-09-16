package de.lmu.ifi.dbs.elki.algorithm.result.outlier;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.ComparablePair;
import de.lmu.ifi.dbs.elki.utilities.Pair;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

/**
 * Provides the result of the LOCI algorithm.
 * 
 * @author Erich Schubert
 * @param <O> the type of DatabaseObjects handled by this Result
 */

public class LOCIResult<O extends DatabaseObject> extends AbstractResult<O> {

  /**
   * Marker for a file name containing LOCI results.
   */
  public static final String LOCI_MARKER = "loci";

  /**
   * Standard constructor. Constructs a new LOCIResult set from a database.
   * 
   * @param db the database from which the LOCI MDEFs have been computed
   */
  public LOCIResult(Database<O> db) {
    super(db);
    this.db = db;
  }

  public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try {
      File lociFile = new File(out.getAbsolutePath() + File.separator + LOCI_MARKER + FILE_EXTENSION);
      lociFile.getParentFile().mkdirs();
      PrintStream lofOut = new PrintStream(new FileOutputStream(lociFile));
      outputLOCI(lofOut, normalization, settings);
      lofOut.flush();
    }
    catch(Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
      output(outStream, normalization, settings);
    }
  }

  public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {

    outputLOCI(outStream, normalization, settings);
    outStream.flush();
  }

  /**
   * Writes the LOCI MDEFs to output.
   * 
   * @param outStream the stream to write to
   * @param normalization Normalization to restore original values according to,
   *        if this action is supported - may remain null.
   * @param settings the settings to be written into the header, if this
   *        parameter is <code>null</code>, no header will be written
   * @throws de.lmu.ifi.dbs.elki.utilities.UnableToComplyException if an error
   *         during normalization occurs
   */
  private void outputLOCI(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {

    writeHeader(outStream, settings, null);

    try {
      // build a list for sorting
      ArrayList<ComparablePair<Double,Integer>> l = new ArrayList<ComparablePair<Double,Integer>>(db.size());
      for (Iterator<Integer> it = db.iterator(); it.hasNext(); ) {
        Integer id = it.next();
        // TODO: allow using MDEF for sorting?
        Double locimdef_norm = db.getAssociation(AssociationID.LOCI_MDEF_NORM, id);
        l.add(new ComparablePair<Double, Integer>(locimdef_norm, id));
      }
      Collections.sort(l, Collections.reverseOrder());

      for (Pair<Double, Integer> p : l) {
        Integer id = p.getSecond();

        outStream.print("ID=");
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

        String externalID = db.getAssociation(AssociationID.EXTERNAL_ID, id);
        if(externalID != null) {
          outStream.print(externalID);
          outStream.print(" ");
        }

        double locimdef = db.getAssociation(AssociationID.LOCI_MDEF, id);
        double locimdef_norm = db.getAssociation(AssociationID.LOCI_MDEF_NORM, id);
        outStream.print("MDEF=");
        outStream.print(locimdef);
        outStream.print(" ");
        outStream.print("MDEFNORM=");
        outStream.print(locimdef_norm);
        outStream.println();
      }
    }
    catch(NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }

    outStream.flush();
  }

}