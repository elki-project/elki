package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.IDDoublePair;
import de.lmu.ifi.dbs.utilities.IDDoublePairComparatorDescDouble;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/**
 * Provides the result of the LOF algorithm.
 *
 * @author Peer Kro&uml;ger (<a href="mailto:kroegerp@dbs.ifi.lmu.de">kroegerp@dbs.ifi.lmu.de</a>)
 */

public class LOFResult<O extends DatabaseObject> extends AbstractResult<O> {

  /**
   * Contains the lof of each id.
   */
  protected IDDoublePair[] result;

  /**
   * Standard constructor. Constructs a new LOFResult set from a database and
   * an array of IDs and double values.
   *
   * @param db     the database from which the LOFs have been computed
   * @param result storing the result as an array of pairs composed of an integer
   *               (ID) and a double (Value)
   */
  public LOFResult(Database<O> db, IDDoublePair[] result) {
    super(db);
    this.db = db;
    this.result = result;
  }

  public void output(File out, Normalization<O> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e) {
      outStream = new PrintStream(
      new FileOutputStream(FileDescriptor.out));
    }

    output(outStream, normalization, settings);
  }

  public void output(PrintStream outStream, Normalization<O> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {

    writeHeader(outStream, settings, null);

    try {
      Arrays.sort(result, new IDDoublePairComparatorDescDouble());
      for (IDDoublePair idDoublePair : result) {
        double lof = idDoublePair.getValue();
        int objectID = idDoublePair.getID();

        outStream.print(objectID);
        outStream.print(" ");

        O object = db.get(objectID);
        if (normalization != null) {
          O restored = normalization.restore(object);
          outStream.print(restored.toString());
        }
        else {
          outStream.print(object.toString());
        }
        outStream.print(" ");

        String label = (String) db.getAssociation(AssociationID.LABEL, objectID);
        if (label != null) {
          outStream.print(label);
          outStream.print(" ");
        }

        ClassLabel classLabel = (ClassLabel) db.getAssociation(AssociationID.CLASS, objectID);
        if (classLabel != null) {
          outStream.print(classLabel);
          outStream.print(" ");
        }

        String externalID = (String) db.getAssociation(AssociationID.EXTERNAL_ID, objectID);
        if (externalID != null) {
          outStream.print(externalID);
          outStream.print(" ");
        }

        outStream.println(lof);
      }
    }
    catch (NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }

    outStream.flush();
  }

}
