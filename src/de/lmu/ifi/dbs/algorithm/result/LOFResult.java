package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
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
   * the result
   */
  protected IDDoublePair[] result;

  /**
   * Standard constructor.
   * Constructs a new LOFResult set from a database and an array of IDs and double values.
   *
   * @param db     the database from which the LOFs have been computed
   * @param result storing the result as an array of pairs composed of an integer (ID) and a double (Value)
   */
  public LOFResult(Database<O> db, IDDoublePair[] result) {
    super(db);
    this.db = db;
    this.result = result;
  }

  public void output(File out, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    PrintStream outStream;
    try {
      outStream = new PrintStream(new FileOutputStream(out));
    }
    catch (Exception e) {
      outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
    }

    writeHeader(outStream, settings);

    Arrays.sort(result, new IDDoublePairComparatorDescDouble());

    for (int i = 0; i < result.length; i++) {
      double lof = result[i].getValue();
      Integer objectID = new Integer(result[i].getID());
      outStream.print(db.get(objectID).toString());
      outStream.print(" ");
      outStream.print(db.getAssociation(AssociationID.LABEL, objectID));
      outStream.print(" ");
      outStream.print("ID_");
      outStream.print(objectID.toString());
      outStream.print(" ");
      outStream.println(lof);
    }

    outStream.flush();
  }

}
