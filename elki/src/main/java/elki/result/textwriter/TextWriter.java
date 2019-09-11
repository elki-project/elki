/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.result.textwriter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;

import elki.data.*;
import elki.data.model.Model;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBID;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.relation.Relation;
import elki.datasource.bundle.SingleObjectBundle;
import elki.evaluation.classification.ConfusionMatrixEvaluationResult;
import elki.math.geometry.XYCurve;
import elki.result.*;
import elki.result.SettingsResult.SettingInformation;
import elki.result.textwriter.naming.NamingScheme;
import elki.result.textwriter.naming.SimpleEnumeratingScheme;
import elki.result.textwriter.writers.*;
import elki.utilities.HandlerList;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.parameterization.SerializedParameterization;
import elki.utilities.pairs.DoubleDoublePair;
import elki.utilities.pairs.Pair;

/**
 * Class to write a result to human-readable text output.
 * <p>
 * Note: these classes need to be <b>redesigned</b>. Contributions welcome!
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @opt nodefillcolor LemonChiffon
 * @navassoc - writesTo - TextWriterStream
 * @composed - - - TextWriterWriterInterface
 * @has - - - NamingScheme
 */
public class TextWriter {
  /**
   * Extension for txt-files.
   */
  public static final String FILE_EXTENSION = ".txt";

  /**
   * Hash map for supported classes in writer.
   */
  public static final HandlerList<TextWriterWriterInterface<?>> writers = new HandlerList<>();

  /**
   * Add some default handlers
   */
  static {
    TextWriterObjectInline trivialwriter = new TextWriterObjectInline();
    writers.insertHandler(Pair.class, new TextWriterPair());
    writers.insertHandler(DoubleDoublePair.class, new TextWriterDoubleDoublePair());
    writers.insertHandler(FeatureVector.class, trivialwriter);
    writers.insertHandler(double[].class, new TextWriterDoubleArray());
    writers.insertHandler(int[].class, new TextWriterIntArray());
    // these object can be serialized inline with toString()
    writers.insertHandler(String.class, trivialwriter);
    writers.insertHandler(Double.class, trivialwriter);
    writers.insertHandler(Integer.class, trivialwriter);
    writers.insertHandler(String[].class, new TextWriterObjectArray<String>());
    writers.insertHandler(Double[].class, new TextWriterObjectArray<Double>());
    writers.insertHandler(Integer[].class, new TextWriterObjectArray<Integer>());
    writers.insertHandler(SimpleClassLabel.class, trivialwriter);
    writers.insertHandler(HierarchicalClassLabel.class, trivialwriter);
    writers.insertHandler(LabelList.class, trivialwriter);
    writers.insertHandler(DBID.class, trivialwriter);
    writers.insertHandler(XYCurve.class, new TextWriterXYCurve());
    // Objects that have an own writeToText method.
    writers.insertHandler(TextWriteable.class, new TextWriterTextWriteable());
    writers.insertHandler(ConfusionMatrixEvaluationResult.class, new TextWriterConfusionMatrixResult());
  }

  /**
   * For producing unique filenames.
   */
  protected Map<String, Object> filenames = new HashMap<>();

  /**
   * Fallback writer for unknown objects.
   */
  private TextWriterWriterInterface<?> fallback = new TextWriterObjectComment();

  /**
   * Try to find a unique file name.
   *
   * @param result Result we print
   * @param filenamepre File name prefix to use
   * @return unique filename
   */
  protected String getFilename(Object result, String filenamepre) {
    if(filenamepre == null || filenamepre.length() == 0) {
      filenamepre = "result";
    }
    for(int i = 0;; i++) {
      String filename = i > 0 ? filenamepre + "-" + i : filenamepre;
      Object existing = filenames.get(filename);
      if(existing == null || existing == result) {
        filenames.put(filename, result);
        return filename;
      }
    }
  }

  /**
   * Stream output.
   *
   * @param db Database object
   * @param r Result class
   * @param streamOpener output stream manager
   * @param filter Filter pattern
   * @throws IOException on IO error
   */
  @SuppressWarnings("unchecked")
  public void output(Database db, Object r, StreamFactory streamOpener, Pattern filter) throws IOException {
    List<Relation<?>> ra = new LinkedList<>();
    List<OrderingResult> ro = new LinkedList<>();
    List<Clustering<?>> rc = new LinkedList<>();
    List<IterableResult<?>> ri = new LinkedList<>();
    List<SettingsResult> rs = new LinkedList<>();
    List<Object> otherres = new LinkedList<>();

    // Split result objects in different known types:
    {
      Metadata.hierarchyOf(r).iterDescendantsSelf().forEach(res -> {
        if(filter != null) {
          final String nam = Metadata.of(res).getLongName();
          if(nam == null || !filter.matcher(nam).find()) {
            return;
          }
        }
        if(res instanceof Database) {
          return;
        }
        else if(res instanceof Relation) {
          ra.add((Relation<?>) res);
        }
        else if(res instanceof OrderingResult) {
          ro.add((OrderingResult) res);
        }
        else if(res instanceof Clustering) {
          rc.add((Clustering<?>) res);
        }
        else if(res instanceof IterableResult) {
          ri.add((IterableResult<?>) res);
        }
        else if(res instanceof SettingsResult) {
          rs.add((SettingsResult) res);
        }
        else {
          otherres.add(res);
        }
      });
    }

    writeSettingsResult(streamOpener, rs);

    for(IterableResult<?> rii : ri) {
      writeIterableResult(streamOpener, rii);
    }
    for(Clustering<?> c : rc) {
      NamingScheme naming = new SimpleEnumeratingScheme(c);
      for(Cluster<?> clus : c.getAllClusters()) {
        writeClusterResult(db, streamOpener, (Clustering<Model>) c, (Cluster<Model>) clus, ra, naming);
      }
    }
    for(OrderingResult ror : ro) {
      writeOrderingResult(db, streamOpener, ror, ra);
    }
    for(Object otherr : otherres) {
      writeOtherResult(streamOpener, otherr);
    }
  }

  private void printObject(TextWriterStream out, Database db, final DBIDRef objID, List<Relation<?>> ra) throws IOException {
    SingleObjectBundle bundle = db.getBundle(objID);
    // Write database element itself.
    for(int i = 0; i < bundle.metaLength(); i++) {
      Object obj = bundle.data(i);
      if(obj != null) {
        TextWriterWriterInterface<?> owriter = out.getWriterFor(obj);
        if(owriter == null) {
          throw new IOException("No handler for database object itself: " + obj.getClass().getSimpleName());
        }
        String lbl = null;
        // TODO: ugly compatibility hack...
        if(TypeUtil.DBID.isAssignableFromType(bundle.meta(i))) {
          lbl = "ID";
        }
        owriter.writeObject(out, lbl, obj);
      }
    }

    Collection<Relation<?>> dbrels = db.getRelations();
    // print the annotations
    if(ra != null) {
      for(Relation<?> a : ra) {
        // Avoid duplicated output.
        if(dbrels.contains(a)) {
          continue;
        }
        String label = Metadata.of(a).getLongName();
        Object value = a.get(objID);
        if(value == null) {
          continue;
        }
        TextWriterWriterInterface<?> writer = out.getWriterFor(value);
        if(writer == null) {
          // Ignore
          continue;
        }
        writer.writeObject(out, label, value);
      }
    }
    out.flush();
  }

  private void writeClusterResult(Database db, StreamFactory streamOpener, Clustering<Model> clustering, Cluster<Model> clus, List<Relation<?>> ra, NamingScheme naming) throws FileNotFoundException, IOException {
    String cname = naming.getNameFor(clus);
    String filename = filenameFromLabel(cname);

    PrintStream outStream = streamOpener.openStream(getFilename(clus, filename));
    TextWriterStream out = new TextWriterStream(outStream, writers, fallback);

    // Write cluster information
    out.commentPrintLn("Cluster: " + cname);
    clus.writeToText(out, null);
    if(clustering.getClusterHierarchy().numParents(clus) > 0) {
      StringBuilder buf = new StringBuilder(100).append("Parents:");
      for(It<Cluster<Model>> iter = clustering.getClusterHierarchy().iterParents(clus); iter.valid(); iter.advance()) {
        buf.append(' ').append(naming.getNameFor(iter.get()));
      }
      out.commentPrintLn(buf.toString());
    }
    if(clustering.getClusterHierarchy().numChildren(clus) > 0) {
      StringBuilder buf = new StringBuilder(100).append("Children:");
      for(It<Cluster<Model>> iter = clustering.getClusterHierarchy().iterChildren(clus); iter.valid(); iter.advance()) {
        buf.append(' ').append(naming.getNameFor(iter.get()));
      }
      out.commentPrintLn(buf.toString());
    }
    out.flush();

    for(DBIDIter iter = clus.getIDs().iter(); iter.valid(); iter.advance()) {
      printObject(out, db, iter, ra);
    }
    out.flush();
    streamOpener.closeStream(outStream);
  }

  private void writeIterableResult(StreamFactory streamOpener, IterableResult<?> ri) throws IOException {
    PrintStream outStream = streamOpener.openStream(getFilename(ri, Metadata.of(ri).getLongName()));
    TextWriterStream out = new TextWriterStream(outStream, writers, fallback);

    // hack to print collectionResult header information
    if(ri instanceof CollectionResult<?>) {
      final Collection<String> hdr = ((CollectionResult<?>) ri).getHeader();
      if(hdr != null) {
        for(String header : hdr) {
          out.commentPrintLn(header);
        }
        out.flush();
      }
    }
    Iterator<?> i = ri.iterator();
    while(i.hasNext()) {
      Object o = i.next();
      TextWriterWriterInterface<?> writer = out.getWriterFor(o);
      if(writer != null) {
        writer.writeObject(out, null, o);
      }
      out.flush();
    }
    out.flush();
    streamOpener.closeStream(outStream);
  }

  private void writeOrderingResult(Database db, StreamFactory streamOpener, OrderingResult or, List<Relation<?>> ra) throws IOException {
    PrintStream outStream = streamOpener.openStream(getFilename(or, Metadata.of(or).getLongName()));
    TextWriterStream out = new TextWriterStream(outStream, writers, fallback);

    for(DBIDIter i = or.order(or.getDBIDs()).iter(); i.valid(); i.advance()) {
      printObject(out, db, i, ra);
    }
    out.flush();
    streamOpener.closeStream(outStream);
  }

  private void writeSettingsResult(StreamFactory streamOpener, List<SettingsResult> rs) throws IOException {
    if(rs.isEmpty()) {
      return;
    }
    SettingsResult r = rs.get(0);
    PrintStream outStream = streamOpener.openStream(getFilename(r, Metadata.of(r).getLongName()));
    TextWriterStream out = new TextWriterStream(outStream, writers, fallback);
    // Write settings preamble
    out.commentPrintLn("Settings:");

    for(SettingsResult settings : rs) {
      Object last = null;
      for(SettingInformation setting : settings.getSettings()) {
        if(setting.owner != last) {
          if(last != null) {
            out.commentPrintLn("");
          }
          out.commentPrintLn(setting.owner);
          last = setting.owner;
        }
        out.commentPrintLn(SerializedParameterization.OPTION_PREFIX + setting.name + " " + setting.value);
      }
    }
    out.flush();
    streamOpener.closeStream(outStream);
  }

  private void writeOtherResult(StreamFactory streamOpener, Object r) throws IOException {
    if(writers.getHandler(r) != null) {
      PrintStream outStream = streamOpener.openStream(getFilename(r, Metadata.of(r).getLongName()));
      TextWriterStream out = new TextWriterStream(outStream, writers, fallback);
      TextWriterWriterInterface<?> owriter = out.getWriterFor(r);
      if(owriter == null) {
        throw new IOException("No handler for result class: " + r.getClass().getSimpleName());
      }
      // Write data
      owriter.writeObject(out, null, r);
      out.flush();
      streamOpener.closeStream(outStream);
    }
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
