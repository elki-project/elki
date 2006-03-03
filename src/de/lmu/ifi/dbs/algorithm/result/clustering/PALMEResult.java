package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.clustering.PALME;
import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * TODO: weiss noch nicht, was das werden soll
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PALMEResult<O extends DatabaseObject, D extends Distance<D>, M extends MultiRepresentedObject<O>> extends AbstractResult<M> {
  List<List<PALME<O, D, M>.DistanceObject>> result;

//  public PALMEResult(Database<M> db, List<List<PALME<O, D, M>.DistanceObject>> result) {
//    super(db);
//    this.result = result;
//  }

  public PALMEResult(Database<M> db) {
    super(db);
  }

  /**
   * @see Result#output(java.io.PrintStream, de.lmu.ifi.dbs.normalization.Normalization, java.util.List<de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings>)
   */
  public void output(PrintStream outStream, Normalization<M> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    if (true) return;
    writeHeader(outStream, settings);

    int r = 1;
    for (List<PALME<O, D, M>.DistanceObject> distanceObjects : result) {
      System.out.println("r " + r);
      outStream.println("### Representation " + (r++));
      for (PALME.DistanceObject object : distanceObjects) {
        outStream.println(object);
      }
      outStream.println("#############################################################");
    }
    outStream.flush();
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
   */
  public void output(File out, Normalization<M> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    if (true) return;
    try {
      int r = 1;
      for (List<PALME<O, D, M>.DistanceObject> distanceObjects : result) {
        String marker = "_r" + (r++);
        File file = new File(out.getAbsolutePath() + File.separator + marker);
        file.getParentFile().mkdirs();
        PrintStream outStream = new PrintStream(new FileOutputStream(file));

        writeHeader(outStream, settings);
        outStream.println("### Representation " + (r++));
        for (PALME.DistanceObject object : distanceObjects) {
          outStream.println(object);
        }
        outStream.println("#############################################################");
      }

    }
    catch (FileNotFoundException e) {
      throw new UnableToComplyException(e);
    }
  }
}
