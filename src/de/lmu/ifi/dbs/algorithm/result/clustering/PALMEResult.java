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
  private List<List<PALME<O, D, M>.Ranges>> result;
  private List<D> maxDistances;

  public PALMEResult(Database<M> db, List<List<PALME<O, D, M>.Ranges>> result, List<D> maxDistances) {
    super(db);
    this.result = result;
    this.maxDistances = maxDistances;
  }

  /**
   * @see Result#output(java.io.PrintStream, de.lmu.ifi.dbs.normalization.Normalization, java.util.List<de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings>)
   */
  public void output(PrintStream outStream, Normalization<M> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    writeHeader(outStream, settings);

    int r = 1;
    for (List<PALME<O, D, M>.Ranges> distanceObjects : result) {
      System.out.println("r " + r);
      outStream.println("### Representation " + (r++));
      for (PALME.Ranges object : distanceObjects) {
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
    try {
      int r = 1;
      for (List<PALME<O, D, M>.Ranges> ranges : result) {
        String marker = "ranges_rep_" + r + ".txt";
        File file = new File(out.getAbsolutePath() + File.separator + marker);
        file.getParentFile().mkdirs();
        PrintStream outStream = new PrintStream(new FileOutputStream(file));

//        writeHeader(outStream, settings);
//        outStream.println("### Representation " + (r++));
        outStream.println("### maximum distance " + maxDistances.get(r-1));
        outStream.println(ranges.get(0).getDescription());
        for (PALME.Ranges object : ranges) {
          outStream.println(object);
        }
//        outStream.println("#############################################################");
        r++;
      }

    }
    catch (FileNotFoundException e) {
      throw new UnableToComplyException(e);
    }
  }
}
