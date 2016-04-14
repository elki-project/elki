package de.lmu.ifi.dbs.elki.result.textwriter;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.HierarchicalClassLabel;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorSingleCluster;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.result.textwriter.naming.NamingScheme;
import de.lmu.ifi.dbs.elki.result.textwriter.naming.SimpleEnumeratingScheme;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterDoubleArray;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterDoubleDoublePair;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterIgnore;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterIntArray;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterObjectArray;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterObjectInline;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterPair;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterTextWriteable;
import de.lmu.ifi.dbs.elki.utilities.HandlerList;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackedParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Class to write a result to human-readable text output.
 *
 * Note: these classes need to be rewritten. Contributions welcome!
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @apiviz.landmark
 * @apiviz.uses TextWriterStream oneway - - writesTo
 * @apiviz.composedOf TextWriterWriterInterface
 * @apiviz.has NamingScheme
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
    TextWriterWriterInterface<Object> ignorewriter = new TextWriterIgnore();
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
    writers.insertHandler(GeneratorSingleCluster.class, ignorewriter);
    // Objects that have an own writeToText method.
    writers.insertHandler(TextWriteable.class, new TextWriterTextWriteable());
  }

  /**
   * For producing unique filenames.
   */
  protected Map<String, Object> filenames = new HashMap<>();

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
    int i = 0;
    while(true) {
      String filename;
      if(i > 0) {
        filename = filenamepre + "-" + i;
      }
      else {
        filename = filenamepre;
      }
      Object existing = filenames.get(filename);
      if(existing == null || existing == result) {
        filenames.put(filename, result);
        return filename;
      }
      i++;
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
  public void output(Database db, Result r, StreamFactory streamOpener, Pattern filter) throws IOException {
    List<Relation<?>> ra = new LinkedList<>();
    List<OrderingResult> ro = new LinkedList<>();
    List<Clustering<?>> rc = new LinkedList<>();
    List<IterableResult<?>> ri = new LinkedList<>();
    List<SettingsResult> rs = new LinkedList<>();
    List<Result> otherres = new LinkedList<>();

    // Split result objects in different known types:
    {
      List<Result> results = ResultUtil.filterResults(db.getHierarchy(), r, Result.class);
      for(Result res : results) {
        if(filter != null) {
          final String nam = res.getShortName();
          if(nam == null || !filter.matcher(nam).find()) {
            continue;
          }
        }
        if(res instanceof Database) {
          continue;
        }
        if(res instanceof Relation) {
          ra.add((Relation<?>) res);
          continue;
        }
        if(res instanceof OrderingResult) {
          ro.add((OrderingResult) res);
          continue;
        }
        if(res instanceof Clustering) {
          rc.add((Clustering<?>) res);
          continue;
        }
        if(res instanceof IterableResult) {
          ri.add((IterableResult<?>) res);
          continue;
        }
        if(res instanceof SettingsResult) {
          rs.add((SettingsResult) res);
          continue;
        }
        otherres.add(res);
      }
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
    for(Result otherr : otherres) {
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
        String label = a.getShortName();
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
    String filename = null;
    if(naming != null) {
      filename = filenameFromLabel(naming.getNameFor(clus));
    }
    else {
      filename = "cluster";
    }

    PrintStream outStream = streamOpener.openStream(getFilename(clus, filename));
    TextWriterStream out = new TextWriterStream(outStream, writers);

    // Write cluster information
    out.commentPrintLn("Cluster: " + naming.getNameFor(clus));
    clus.writeToText(out, null);
    if(clustering.getClusterHierarchy().numParents(clus) > 0) {
      StringBuilder buf = new StringBuilder();
      buf.append("Parents:");
      for(Hierarchy.Iter<Cluster<Model>> iter = clustering.getClusterHierarchy().iterParents(clus); iter.valid(); iter.advance()) {
        buf.append(' ').append(naming.getNameFor(iter.get()));
      }
      out.commentPrintLn(buf.toString());
    }
    if(clustering.getClusterHierarchy().numChildren(clus) > 0) {
      StringBuilder buf = new StringBuilder();
      buf.append("Children:");
      for(Hierarchy.Iter<Cluster<Model>> iter = clustering.getClusterHierarchy().iterChildren(clus); iter.valid(); iter.advance()) {
        buf.append(' ').append(naming.getNameFor(iter.get()));
      }
      out.commentPrintLn(buf.toString());
    }
    out.flush();

    // print ids.
    DBIDs ids = clus.getIDs();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      printObject(out, db, iter, ra);
    }
    out.flush();
    streamOpener.closeStream(outStream);
  }

  private void writeIterableResult(StreamFactory streamOpener, IterableResult<?> ri) throws IOException {
    PrintStream outStream = streamOpener.openStream(getFilename(ri, ri.getShortName()));
    TextWriterStream out = new TextWriterStream(outStream, writers);

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
    PrintStream outStream = streamOpener.openStream(getFilename(or, or.getShortName()));
    TextWriterStream out = new TextWriterStream(outStream, writers);

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
    PrintStream outStream = streamOpener.openStream(getFilename(r, r.getShortName()));
    TextWriterStream out = new TextWriterStream(outStream, writers);
    // Write settings preamble
    out.commentPrintLn("Settings:");

    if(rs != null) {
      for(SettingsResult settings : rs) {
        Object last = null;
        for(TrackedParameter setting : settings.getSettings()) {
          if(setting.getOwner() != last && setting.getOwner() != null) {
            if(last != null) {
              out.commentPrintLn("");
            }
            String name;
            try {
              if(setting.getOwner() instanceof Class) {
                name = ((Class<?>) setting.getOwner()).getName();
              }
              else {
                name = setting.getOwner().getClass().getName();
              }
              if(ClassParameter.class.isInstance(setting.getOwner())) {
                name = ((ClassParameter<?>) setting.getOwner()).getValue().getName();
              }
            }
            catch(NullPointerException e) {
              name = "[null]";
            }
            out.commentPrintLn(name);
            last = setting.getOwner();
          }
          String name = setting.getParameter().getOptionID().getName();
          String value = "[unset]";
          try {
            if(setting.getParameter().isDefined()) {
              value = setting.getParameter().getValueAsString();
            }
          }
          catch(NullPointerException e) {
            value = "[null]";
          }
          out.commentPrintLn(SerializedParameterization.OPTION_PREFIX + name + " " + value);
        }
      }
    }
    out.flush();
    streamOpener.closeStream(outStream);
  }

  private void writeOtherResult(StreamFactory streamOpener, Result r) throws IOException {
    if(writers.getHandler(r) != null) {
      PrintStream outStream = streamOpener.openStream(getFilename(r, r.getShortName()));
      TextWriterStream out = new TextWriterStream(outStream, writers);
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
