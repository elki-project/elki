package de.lmu.ifi.dbs.elki.result.textwriter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.HierarchicalClassLabel;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.cluster.naming.NamingScheme;
import de.lmu.ifi.dbs.elki.data.cluster.naming.SimpleEnumeratingScheme;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterDatabaseObjectInline;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterObjectComment;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterObjectInline;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterTextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterTriple;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterVector;
import de.lmu.ifi.dbs.elki.utilities.HandlerList;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

/**
 * Class to write a result to human-readable text output
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
@SuppressWarnings("unchecked")
public class TextWriter<O extends DatabaseObject> {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(TextWriter.class);

  /**
   * Extension for txt-files.
   */
  public static final String FILE_EXTENSION = ".txt";

  /**
   * Hash map for supported classes in writer.
   */
  public final static HandlerList<TextWriterWriterInterface<?>> writers = new HandlerList<TextWriterWriterInterface<?>>();

  /**
   * Add some default handlers
   */
  static {
    TextWriterObjectInline trivialwriter = new TextWriterObjectInline();
    writers.insertHandler(Object.class, new TextWriterObjectComment());
    writers.insertHandler(DatabaseObject.class, new TextWriterDatabaseObjectInline<DatabaseObject>());
    // these object can be serialized inline with toString()
    writers.insertHandler(String.class, trivialwriter);
    writers.insertHandler(Double.class, trivialwriter);
    writers.insertHandler(Integer.class, trivialwriter);
    writers.insertHandler(BitSet.class, trivialwriter);
    writers.insertHandler(Vector.class, new TextWriterVector());
    writers.insertHandler(Distance.class, trivialwriter);
    writers.insertHandler(SimpleClassLabel.class, trivialwriter);
    writers.insertHandler(HierarchicalClassLabel.class, trivialwriter);
    writers.insertHandler(Triple.class, new TextWriterTriple());
    // Objects that have an own writeToText method.
    writers.insertHandler(TextWriteable.class, new TextWriterTextWriteable());
  }

  /**
   * Normalization to use.
   */
  private Normalization<O> normalization;

  /**
   * Writes a header providing information concerning the underlying database
   * and the specified parameter-settings.
   * 
   * @param db to retrieve meta information from
   * @param out the print stream where to write
   * @param settings the settings to be written into the header
   */
  protected void printSettings(Database<O> db, TextWriterStream out, List<AttributeSettings> settings) {
    out.commentPrintSeparator();
    out.commentPrintLn("Settings and meta information:");
    out.commentPrintLn("db size = " + db.size());
    // noinspection EmptyCatchBlock
    try {
      int dimensionality = db.dimensionality();
      out.commentPrintLn("db dimensionality = " + dimensionality);
    }
    catch(UnsupportedOperationException e) {
      // dimensionality is unsupported - do nothing
    }
    out.commentPrintLn("");

    if(settings != null) {
      for(AttributeSettings setting : settings) {
        if(!setting.getSettings().isEmpty()) {
          out.commentPrintLn(setting.toString());
          out.commentPrintLn("");
        }
      }
    }

    out.commentPrintSeparator();
    out.flush();
  }

  /**
   * Stream output.
   * 
   * @param db Database object
   * @param r Result class
   * @param streamOpener output stream manager
   * @throws UnableToComplyException
   * @throws IOException
   */
  public void output(Database<O> db, Result r, StreamFactory streamOpener) throws UnableToComplyException, IOException {
    List<AnnotationResult<?>> ra = null;
    List<OrderingResult> ro = null;
    List<Clustering<? extends Model>> rc = null;
    List<IterableResult<?>> ri = null;

    Collection<DatabaseObjectGroup> groups = null;

    ra = ResultUtil.getAnnotationResults(r);
    ro = ResultUtil.getOrderingResults(r);
    rc = ResultUtil.getClusteringResults(r);
    ri = ResultUtil.getIterableResults(r);

    if(ra == null && ro == null && rc == null && ri == null) {
      throw new UnableToComplyException("No printable result found.");
    }

    NamingScheme naming = null;
    // Process groups or all data in a flat manner?
    if(rc != null && rc.size() > 0) {
      groups = new HashSet<DatabaseObjectGroup>(rc.get(0).getAllClusters());
      // force an update of cluster names.
      naming = new SimpleEnumeratingScheme(rc.get(0));
    }
    else {
      groups = new ArrayList<DatabaseObjectGroup>();
      groups.add(new DatabaseObjectGroupCollection<Collection<Integer>>(db.getIDs()));
    }
    
    List<AttributeSettings> settings = ResultUtil.getGlobalAssociation((MultiResult)r, AssociationID.META_SETTINGS);

    if(ri != null && ri.size() > 0) {
      writeIterableResult(db, streamOpener, ri.get(0), settings);
    }
    if(groups != null) {
      for(DatabaseObjectGroup group : groups) {
        writeGroupResult(db, streamOpener, group, ra, ro, naming, settings);
      }
    }
  }

  private void printObject(TextWriterStream out, O obj, List<Pair<String, Object>> anns) throws UnableToComplyException, IOException {
    // Write database element itself.
    {
      TextWriterWriterInterface<?> owriter = out.getWriterFor(obj);
      if(owriter == null) {
        throw new UnableToComplyException("No handler for database object itself: " + obj.getClass().getSimpleName());
      }
      owriter.writeObject(out, null, obj);
    }

    // print the annotations
    if(anns != null) {
      for(Pair<String, Object> a : anns) {
        if(a.getSecond() == null) {
          continue;
        }
        TextWriterWriterInterface<?> writer = out.getWriterFor(a.getSecond());
        if(writer == null) {
          throw new UnableToComplyException("No handler for annotation " + a.getFirst() + " in Output: " + a.getSecond().getClass().getSimpleName());
        }

        writer.writeObject(out, a.getFirst(), a.getSecond());
      }
    }
    out.flush();
  }

  private void writeGroupResult(Database<O> db, StreamFactory streamOpener, DatabaseObjectGroup group, List<AnnotationResult<?>> ra, List<OrderingResult> ro, NamingScheme naming, List<AttributeSettings> settings) throws FileNotFoundException, UnableToComplyException, IOException {
    String filename = null;
    // for clusters, use naming.
    if(group instanceof Cluster) {
      if(naming != null) {
        filename = filenameFromLabel(naming.getNameFor(group));
      }
    }

    PrintStream outStream = streamOpener.openStream(filename);
    TextWriterStream out = new TextWriterStreamNormalizing<O>(outStream, writers, getNormalization());

    printSettings(db, out, settings);
    // print group information...
    if(group instanceof TextWriteable) {
      TextWriterWriterInterface<?> writer = out.getWriterFor(group);
      out.commentPrintLn("Group class: " + group.getClass().getCanonicalName());
      if(writer != null) {
        writer.writeObject(out, null, group);
        out.commentPrintSeparator();
        out.flush();
      }
    }

    // print ids.
    Collection<Integer> ids = group.getIDs();
    Iterator<Integer> iter = ids.iterator();
    // apply sorting.
    if(ro != null && ro.size() > 0) {
      try {
        iter = ro.get(0).iter(ids);
      } catch (Exception e) {
        logger.warning("Exception while trying to sort results.",e);
      }
    }

    while(iter.hasNext()) {
      Integer objID = iter.next();
      if(objID == null) {
        // shoulnd't really happen?
        continue;
      }
      O obj = db.get(objID.intValue());
      if(obj == null) {
        continue;
      }
      // do we have annotations to print?
      List<Pair<String, Object>> objs = new ArrayList<Pair<String, Object>>();
      if(ra != null) {
        for (AnnotationResult a : ra) {
          objs.add(new Pair<String, Object>(a.getAssociationID().getLabel(), a.getValueFor(objID)));
        }
      }
      // print the object with its annotations.
      printObject(out, obj, objs);
    }
    out.commentPrintSeparator();
    out.flush();
  }

  private void writeIterableResult(Database<O> db, StreamFactory streamOpener, IterableResult<?> ri, List<AttributeSettings> settings) throws UnableToComplyException, IOException {
    String filename = "list";
    PrintStream outStream = streamOpener.openStream(filename);
    TextWriterStream out = new TextWriterStreamNormalizing<O>(outStream, writers, getNormalization());
    printSettings(db, out, settings);

    // hack to print collectionResult header information
    if(ri instanceof CollectionResult<?>) {
      for(String header : ((CollectionResult<?>) ri).getHeader()) {
        out.commentPrintLn(header);
      }
      out.flush();
    }
    Iterator<?> i = ri.iter();
    while(i.hasNext()) {
      Object o = i.next();
      TextWriterWriterInterface<?> writer = out.getWriterFor(o);
      if(writer != null) {
        writer.writeObject(out, null, o);
      }
      out.flush();
    }
    out.commentPrintSeparator();
    out.flush();
  }

  /**
   * Setter for normalization
   * 
   * @param normalization new normalization object
   */
  public void setNormalization(Normalization<O> normalization) {
    this.normalization = normalization;
  }

  /**
   * Getter for normalization
   * 
   * @return normalization object
   */
  public Normalization<O> getNormalization() {
    return normalization;
  }

  /**
   * Derive a file name from the cluster label.
   * 
   * @param label cluster label
   * @return cleaned label suitable for file names.
   */
  private String filenameFromLabel(String label) {
    return label.toLowerCase().replaceAll("[^a-zA-Z0-9_.\\[\\]-]", "_");
  }
}
