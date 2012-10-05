package de.lmu.ifi.dbs.elki.result.textwriter;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.HierarchicalClassLabel;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.result.textwriter.naming.NamingScheme;
import de.lmu.ifi.dbs.elki.result.textwriter.naming.SimpleEnumeratingScheme;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterDoubleDoublePair;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterObjectArray;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterObjectComment;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterObjectInline;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterPair;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterTextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterTriple;
import de.lmu.ifi.dbs.elki.result.textwriter.writers.TextWriterVector;
import de.lmu.ifi.dbs.elki.utilities.HandlerList;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

/**
 * Class to write a result to human-readable text output
 * 
 * @author Erich Schubert
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
  public static final HandlerList<TextWriterWriterInterface<?>> writers = new HandlerList<TextWriterWriterInterface<?>>();

  /**
   * Add some default handlers
   */
  static {
    TextWriterObjectInline trivialwriter = new TextWriterObjectInline();
    writers.insertHandler(Object.class, new TextWriterObjectComment());
    writers.insertHandler(Pair.class, new TextWriterPair());
    writers.insertHandler(DoubleDoublePair.class, new TextWriterDoubleDoublePair());
    writers.insertHandler(Triple.class, new TextWriterTriple());
    writers.insertHandler(FeatureVector.class, trivialwriter);
    // these object can be serialized inline with toString()
    writers.insertHandler(String.class, trivialwriter);
    writers.insertHandler(Double.class, trivialwriter);
    writers.insertHandler(Integer.class, trivialwriter);
    writers.insertHandler(String[].class, new TextWriterObjectArray<String>());
    writers.insertHandler(Double[].class, new TextWriterObjectArray<Double>());
    writers.insertHandler(Integer[].class, new TextWriterObjectArray<Integer>());
    writers.insertHandler(BitSet.class, trivialwriter);
    writers.insertHandler(Vector.class, new TextWriterVector());
    writers.insertHandler(Distance.class, trivialwriter);
    writers.insertHandler(SimpleClassLabel.class, trivialwriter);
    writers.insertHandler(HierarchicalClassLabel.class, trivialwriter);
    writers.insertHandler(LabelList.class, trivialwriter);
    writers.insertHandler(DBID.class, trivialwriter);
    // Objects that have an own writeToText method.
    writers.insertHandler(TextWriteable.class, new TextWriterTextWriteable());
  }

  /**
   * For producing unique filenames.
   */
  protected Map<String, Object> filenames = new HashMap<String, Object>();

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
   * Writes a header providing information concerning the underlying database
   * and the specified parameter-settings.
   * 
   * @param out the print stream where to write
   * @param sr the settings to be written into the header
   */
  protected void printSettings(TextWriterStream out, List<SettingsResult> sr) {
    out.commentPrintSeparator();
    out.commentPrintLn("Settings:");

    if(sr != null) {
      for(SettingsResult settings : sr) {
        Object last = null;
        for(Pair<Object, Parameter<?, ?>> setting : settings.getSettings()) {
          if(setting.first != last && setting.first != null) {
            if(last != null) {
              out.commentPrintLn("");
            }
            String name;
            try {
              if(setting.first instanceof Class) {
                name = ((Class<?>) setting.first).getName();
              }
              else {
                name = setting.first.getClass().getName();
              }
              if(ClassParameter.class.isInstance(setting.first)) {
                name = ((ClassParameter<?>) setting.first).getValue().getName();
              }
            }
            catch(NullPointerException e) {
              name = "[null]";
            }
            out.commentPrintLn(name);
            last = setting.first;
          }
          String name = setting.second.getOptionID().getName();
          String value = "[unset]";
          try {
            if(setting.second.isDefined()) {
              value = setting.second.getValueAsString();
            }
          }
          catch(NullPointerException e) {
            value = "[null]";
          }
          out.commentPrintLn(SerializedParameterization.OPTION_PREFIX + name + " " + value);
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
   * @throws UnableToComplyException when no usable results were found
   * @throws IOException on IO error
   */
  public void output(Database db, Result r, StreamFactory streamOpener) throws UnableToComplyException, IOException {
    List<Relation<?>> ra = new LinkedList<Relation<?>>();
    List<OrderingResult> ro = new LinkedList<OrderingResult>();
    List<Clustering<?>> rc = new LinkedList<Clustering<?>>();
    List<IterableResult<?>> ri = new LinkedList<IterableResult<?>>();
    List<SettingsResult> rs = new LinkedList<SettingsResult>();
    List<Result> otherres = new LinkedList<Result>();

    // collect other results
    {
      List<Result> results = ResultUtil.filterResults(r, Result.class);
      for(Result res : results) {
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

    for(IterableResult<?> rii : ri) {
      writeIterableResult(streamOpener, rii, rs);
    }
    for(Clustering<?> c : rc) {
      NamingScheme naming = new SimpleEnumeratingScheme(c);
      for(Cluster<?> clus : c.getAllClusters()) {
        writeClusterResult(db, streamOpener, clus, ra, naming, rs);
      }
    }
    for(OrderingResult ror : ro) {
      writeOrderingResult(db, streamOpener, ror, ra, rs);
    }
    for(Result otherr : otherres) {
      writeOtherResult(streamOpener, otherr, rs);
    }
  }

  private void printObject(TextWriterStream out, Database db, final DBIDRef objID, List<Relation<?>> ra) throws UnableToComplyException, IOException {
    SingleObjectBundle bundle = db.getBundle(objID);
    // Write database element itself.
    for(int i = 0; i < bundle.metaLength(); i++) {
      Object obj = bundle.data(i);
      if(obj != null) {
        TextWriterWriterInterface<?> owriter = out.getWriterFor(obj);
        if(owriter == null) {
          throw new UnableToComplyException("No handler for database object itself: " + obj.getClass().getSimpleName());
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
    out.flush();
  }

  private void writeOtherResult(StreamFactory streamOpener, Result r, List<SettingsResult> rs) throws UnableToComplyException, IOException {
    PrintStream outStream = streamOpener.openStream(getFilename(r, r.getShortName()));
    TextWriterStream out = new TextWriterStream(outStream, writers);
    TextWriterWriterInterface<?> owriter = out.getWriterFor(r);
    if(owriter == null) {
      throw new UnableToComplyException("No handler for result class: " + r.getClass().getSimpleName());
    }
    // Write settings preamble
    printSettings(out, rs);
    // Write data
    owriter.writeObject(out, null, r);
    out.flush();
  }

  private void writeClusterResult(Database db, StreamFactory streamOpener, Cluster<?> clus, List<Relation<?>> ra, NamingScheme naming, List<SettingsResult> sr) throws FileNotFoundException, UnableToComplyException, IOException {
    String filename = null;
    if(naming != null) {
      filename = filenameFromLabel(naming.getNameFor(clus));
    }
    else {
      filename = "cluster";
    }

    PrintStream outStream = streamOpener.openStream(getFilename(clus, filename));
    TextWriterStream out = new TextWriterStream(outStream, writers);
    printSettings(out, sr);

    // Write cluster information
    out.commentPrintLn("Cluster: " + naming.getNameFor(clus));
    Model model = clus.getModel();
    if(model != ClusterModel.CLUSTER && model != null) {
      TextWriterWriterInterface<?> mwri = writers.getHandler(model);
      mwri.writeObject(out, null, model);
    }
    if(clus.getParents().size() > 0) {
      StringBuilder buf = new StringBuilder();
      buf.append("Parents:");
      for(Cluster<?> parent : clus.getParents()) {
        buf.append(" ").append(naming.getNameFor(parent));
      }
      out.commentPrintLn(buf.toString());
    }
    if(clus.getChildren().size() > 0) {
      StringBuilder buf = new StringBuilder();
      buf.append("Children:");
      for(Cluster<?> child : clus.getChildren()) {
        buf.append(" ").append(naming.getNameFor(child));
      }
      out.commentPrintLn(buf.toString());
    }
    out.flush();

    // print ids.
    DBIDs ids = clus.getIDs();
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      printObject(out, db, iter, ra);
    }
    out.commentPrintSeparator();
    out.flush();
  }

  private void writeIterableResult(StreamFactory streamOpener, IterableResult<?> ri, List<SettingsResult> sr) throws UnableToComplyException, IOException {
    PrintStream outStream = streamOpener.openStream(getFilename(ri, ri.getShortName()));
    TextWriterStream out = new TextWriterStream(outStream, writers);
    printSettings(out, sr);

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
    out.commentPrintSeparator();
    out.flush();
  }

  private void writeOrderingResult(Database db, StreamFactory streamOpener, OrderingResult or, List<Relation<?>> ra, List<SettingsResult> sr) throws IOException, UnableToComplyException {
    PrintStream outStream = streamOpener.openStream(getFilename(or, or.getShortName()));
    TextWriterStream out = new TextWriterStream(outStream, writers);
    printSettings(out, sr);

    for (DBIDIter i = or.iter(or.getDBIDs()).iter(); i.valid(); i.advance()) {
      printObject(out, db, i, ra);
    }
    out.commentPrintSeparator();
    out.flush();
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
