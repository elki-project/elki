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
package de.lmu.ifi.dbs.elki.result;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.geometry.FilteredConvexHull2D;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayListIter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierLinearScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScaling;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;
import net.jafama.FastMath;

/**
 * Class to handle KML output.
 * <p>
 * Reference:
 * <p>
 * Erich Achtert, Ahmed Hettab, Hans-Peter Kriegel, Erich Schubert, Arthur
 * Zimek<br>
 * Spatial Outlier Detection: Data, Algorithms, Visualizations<br>
 * Proc. 12th Int. Symp. Spatial and Temporal Databases (SSTD 2011)
 * <p>
 * Note: This class - currently - is an ugly hack. This code needs to be
 * modularized to make it more reusable, to support multiple results and
 * different result types.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
// TODO: make configurable color scheme
@Reference(authors = "Erich Achtert, Ahmed Hettab, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
    title = "Spatial Outlier Detection: Data, Algorithms, Visualizations", //
    booktitle = "Proc. 12th Int. Symp. Spatial and Temporal Databases (SSTD 2011)", //
    url = "https://doi.org/10.1007/978-3-642-22922-0_41", //
    bibkey = "DBLP:conf/ssd/AchtertHKSZ11")
public class KMLOutputHandler implements ResultHandler {
  /**
   * Logger class to use.
   */
  private static final Logging LOG = Logging.getLogger(KMLOutputHandler.class);

  /**
   * Number of styles to use (lower reduces rendering complexity a bit)
   */
  private static final int NUMSTYLES = 20;

  /**
   * Output file name
   */
  File filename;

  /**
   * Scaling function
   */
  OutlierScaling scaling;

  /**
   * Compatibility mode.
   */
  private boolean compat;

  /**
   * Automatically open at the end
   */
  private boolean autoopen;

  /**
   * Constructor.
   * 
   * @param filename Output filename
   * @param scaling Scaling function
   * @param compat Compatibility mode
   * @param autoopen Automatically open
   */
  public KMLOutputHandler(File filename, OutlierScaling scaling, boolean compat, boolean autoopen) {
    super();
    this.filename = filename;
    this.scaling = scaling;
    this.compat = compat;
    this.autoopen = autoopen;
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    ArrayList<OutlierResult> ors = ResultUtil.filterResults(hier, newResult, OutlierResult.class);
    ArrayList<Clustering<?>> crs = ResultUtil.filterResults(hier, newResult, Clustering.class);
    if(ors.size() + crs.size() > 1) {
      throw new AbortException("More than one visualizable result found. The KML writer only supports a single result!");
    }
    Database database = ResultUtil.findDatabase(hier);
    for(OutlierResult outlierResult : ors) {
      try {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(filename));
        out.putNextEntry(new ZipEntry("doc.kml"));
        final XMLStreamWriter xmlw = factory.createXMLStreamWriter(out);
        writeOutlierResult(xmlw, outlierResult, database);
        xmlw.flush();
        xmlw.close();
        out.closeEntry();
        out.flush();
        out.close();
        if(autoopen) {
          Desktop.getDesktop().open(filename);
        }
      }
      catch(XMLStreamException e) {
        LOG.exception(e);
        throw new AbortException("XML error in KML output.", e);
      }
      catch(IOException e) {
        LOG.exception(e);
        throw new AbortException("IO error in KML output.", e);
      }
    }
    for(Clustering<?> clusteringResult : crs) {
      try {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(filename));
        out.putNextEntry(new ZipEntry("doc.kml"));
        final XMLStreamWriter xmlw = factory.createXMLStreamWriter(out);
        @SuppressWarnings("unchecked")
        Clustering<Model> cres = (Clustering<Model>) clusteringResult;
        writeClusteringResult(xmlw, cres, database);
        xmlw.flush();
        xmlw.close();
        out.closeEntry();
        out.flush();
        out.close();
        if(autoopen) {
          Desktop.getDesktop().open(filename);
        }
      }
      catch(XMLStreamException e) {
        LOG.exception(e);
        throw new AbortException("XML error in KML output.", e);
      }
      catch(IOException e) {
        LOG.exception(e);
        throw new AbortException("IO error in KML output.", e);
      }
    }
  }

  private void writeOutlierResult(XMLStreamWriter xmlw, OutlierResult outlierResult, Database database) throws XMLStreamException {
    Relation<PolygonsObject> polys = database.getRelation(TypeUtil.POLYGON_TYPE);
    Relation<String> labels = DatabaseUtil.guessObjectLabelRepresentation(database);

    xmlw.writeStartDocument();
    xmlw.writeCharacters("\n");
    xmlw.writeStartElement("kml");
    xmlw.writeDefaultNamespace("http://earth.google.com/kml/2.2");
    xmlw.writeStartElement("Document");
    {
      // TODO: can we automatically generate more helpful data here?
      xmlw.writeStartElement("name");
      xmlw.writeCharacters("ELKI KML output for " + outlierResult.getLongName());
      xmlw.writeEndElement(); // name
      writeNewlineOnDebug(xmlw);
      // TODO: e.g. list the settings in the description?
      xmlw.writeStartElement("description");
      xmlw.writeCharacters("ELKI KML output for " + outlierResult.getLongName());
      xmlw.writeEndElement(); // description
      writeNewlineOnDebug(xmlw);
    }
    {
      // TODO: generate styles from color scheme
      for(int i = 0; i < NUMSTYLES; i++) {
        Color col = getColorForValue(i / (NUMSTYLES - 1.0));
        xmlw.writeStartElement("Style");
        xmlw.writeAttribute("id", "s" + i);
        writeNewlineOnDebug(xmlw);
        {
          xmlw.writeStartElement("LineStyle");
          xmlw.writeStartElement("width");
          xmlw.writeCharacters("0");
          xmlw.writeEndElement(); // width

          xmlw.writeEndElement(); // LineStyle
        }
        writeNewlineOnDebug(xmlw);
        {
          xmlw.writeStartElement("PolyStyle");
          xmlw.writeStartElement("color");
          // KML uses AABBGGRR format!
          xmlw.writeCharacters(String.format("%02x%02x%02x%02x", col.getAlpha(), col.getBlue(), col.getGreen(), col.getRed()));
          xmlw.writeEndElement(); // color
          // out.writeStartElement("fill");
          // out.writeCharacters("1"); // Default 1
          // out.writeEndElement(); // fill
          xmlw.writeStartElement("outline");
          xmlw.writeCharacters("0");
          xmlw.writeEndElement(); // outline
          xmlw.writeEndElement(); // PolyStyle
        }
        writeNewlineOnDebug(xmlw);
        xmlw.writeEndElement(); // Style
        writeNewlineOnDebug(xmlw);
      }
    }

    DoubleRelation scores = outlierResult.getScores();
    Collection<Relation<?>> otherrel = new LinkedList<>(database.getRelations());
    otherrel.remove(scores);
    otherrel.remove(polys);
    otherrel.remove(labels);
    otherrel.remove(database.getRelation(TypeUtil.DBID));

    ArrayModifiableDBIDs ids = DBIDUtil.newArray(scores.getDBIDs());

    scaling.prepare(outlierResult);

    for(DBIDIter iter = outlierResult.getOrdering().order(ids).iter(); iter.valid(); iter.advance()) {
      double score = scores.doubleValue(iter);
      PolygonsObject poly = polys.get(iter);
      String label = labels.get(iter);
      if(Double.isNaN(score)) {
        LOG.warning("No score for object " + DBIDUtil.toString(iter));
      }
      if(poly == null) {
        LOG.warning("No polygon for object " + DBIDUtil.toString(iter) + " - skipping.");
        continue;
      }
      xmlw.writeStartElement("Placemark");
      {
        xmlw.writeStartElement("name");
        xmlw.writeCharacters(score + " " + label);
        xmlw.writeEndElement(); // name
        StringBuilder buf = makeDescription(otherrel, iter);
        xmlw.writeStartElement("description");
        xmlw.writeCData("<div>" + buf.toString() + "</div>");
        xmlw.writeEndElement(); // description
        xmlw.writeStartElement("styleUrl");
        int style = (int) (scaling.getScaled(score) * NUMSTYLES);
        style = Math.max(0, Math.min(style, NUMSTYLES - 1));
        xmlw.writeCharacters("#s" + style);
        xmlw.writeEndElement(); // styleUrl
      }
      {
        xmlw.writeStartElement("Polygon");
        writeNewlineOnDebug(xmlw);
        if(compat) {
          xmlw.writeStartElement("altitudeMode");
          xmlw.writeCharacters("relativeToGround");
          xmlw.writeEndElement(); // close altitude mode
          writeNewlineOnDebug(xmlw);
        }
        // First polygon clockwise?
        boolean first = true;
        for(Polygon p : poly.getPolygons()) {
          if(first) {
            xmlw.writeStartElement("outerBoundaryIs");
          }
          else {
            xmlw.writeStartElement("innerBoundaryIs");
          }
          xmlw.writeStartElement("LinearRing");
          xmlw.writeStartElement("coordinates");

          // Reverse anti-clockwise polygons.
          boolean reverse = (p.testClockwise() >= 0);
          ArrayListIter<double[]> it = p.iter();
          if(reverse) {
            it.seek(p.size() - 1);
          }
          while(it.valid()) {
            double[] v = it.get();
            xmlw.writeCharacters(FormatUtil.format(v, ","));
            if(compat && (v.length == 2)) {
              xmlw.writeCharacters(",50");
            }
            xmlw.writeCharacters(" ");
            if(!reverse) {
              it.advance();
            }
            else {
              it.retract();
            }
          }
          xmlw.writeEndElement(); // close coordinates
          xmlw.writeEndElement(); // close LinearRing
          xmlw.writeEndElement(); // close *BoundaryIs
          first = false;
        }
        writeNewlineOnDebug(xmlw);
        xmlw.writeEndElement(); // Polygon
      }
      xmlw.writeEndElement(); // Placemark
      writeNewlineOnDebug(xmlw);
    }
    xmlw.writeEndElement(); // Document
    xmlw.writeEndElement(); // kml
    xmlw.writeEndDocument();
  }

  private void writeClusteringResult(XMLStreamWriter xmlw, Clustering<Model> clustering, Database database) throws XMLStreamException {
    xmlw.writeStartDocument();
    xmlw.writeCharacters("\n");
    xmlw.writeStartElement("kml");
    xmlw.writeDefaultNamespace("http://earth.google.com/kml/2.2");
    xmlw.writeStartElement("Document");
    {
      // TODO: can we automatically generate more helpful data here?
      xmlw.writeStartElement("name");
      xmlw.writeCharacters("ELKI KML output for " + clustering.getLongName());
      xmlw.writeEndElement(); // name
      writeNewlineOnDebug(xmlw);
      // TODO: e.g. list the settings in the description?
      xmlw.writeStartElement("description");
      xmlw.writeCharacters("ELKI KML output for " + clustering.getLongName());
      xmlw.writeEndElement(); // description
      writeNewlineOnDebug(xmlw);
    }

    List<Cluster<Model>> clusters = clustering.getAllClusters();
    Relation<NumberVector> coords = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD_2D);
    List<Cluster<Model>> topc = clustering.getToplevelClusters();
    Hierarchy<Cluster<Model>> hier = clustering.getClusterHierarchy();
    Map<Object, DoubleObjPair<Polygon>> hullmap = new HashMap<>();
    for(Cluster<Model> clu : topc) {
      buildHullsRecursively(clu, hier, hullmap, coords);
    }

    {
      final double projarea = 360. * 180. * .01;
      // TODO: generate styles from color scheme
      Iterator<Cluster<Model>> it = clusters.iterator();
      for(int i = 0; it.hasNext(); i++) {
        Cluster<Model> clus = it.next();
        // This is a prime based magic number, to produce a colorful output
        Color col = Color.getHSBColor(i / 4.294967291f, 1.f, .5f);
        DoubleObjPair<Polygon> pair = hullmap.get(clus);
        // Approximate area (using bounding box)
        double hullarea = SpatialUtil.volume(pair.second);
        final double relativeArea = Math.max(1. - (hullarea / projarea), 0.);
        // final double relativeSize = pair.first / coords.size();
        final double opacity = .65 * FastMath.sqrt(relativeArea) + .1;
        xmlw.writeStartElement("Style");
        xmlw.writeAttribute("id", "s" + i);
        writeNewlineOnDebug(xmlw);
        {
          xmlw.writeStartElement("LineStyle");
          xmlw.writeStartElement("width");
          xmlw.writeCharacters("0");
          xmlw.writeEndElement(); // width

          xmlw.writeEndElement(); // LineStyle
        }
        writeNewlineOnDebug(xmlw);
        {
          xmlw.writeStartElement("PolyStyle");
          xmlw.writeStartElement("color");
          // KML uses AABBGGRR format!
          xmlw.writeCharacters(String.format("%02x%02x%02x%02x", (int) (255 * Math.min(.75, opacity)), col.getBlue(), col.getGreen(), col.getRed()));
          xmlw.writeEndElement(); // color
          // out.writeStartElement("fill");
          // out.writeCharacters("1"); // Default 1
          // out.writeEndElement(); // fill
          xmlw.writeStartElement("outline");
          xmlw.writeCharacters("0");
          xmlw.writeEndElement(); // outline
          xmlw.writeEndElement(); // PolyStyle
        }
        writeNewlineOnDebug(xmlw);
        xmlw.writeEndElement(); // Style
        writeNewlineOnDebug(xmlw);
      }
    }

    Cluster<?> ignore = topc.size() == 1 ? topc.get(0) : null;
    Iterator<Cluster<Model>> it = clusters.iterator();
    for(int cnum = 0; it.hasNext(); cnum++) {
      Cluster<?> c = it.next();
      // Ignore sole toplevel cluster (usually: noise)
      if(c == ignore) {
        continue;
      }
      Polygon p = hullmap.get(c).second;
      xmlw.writeStartElement("Placemark");
      {
        xmlw.writeStartElement("name");
        xmlw.writeCharacters(c.getNameAutomatic());
        xmlw.writeEndElement(); // name
        xmlw.writeStartElement("description");
        xmlw.writeCData(makeDescription(c).toString());
        xmlw.writeEndElement(); // description
        xmlw.writeStartElement("styleUrl");
        xmlw.writeCharacters("#s" + cnum);
        xmlw.writeEndElement(); // styleUrl
      }
      {
        xmlw.writeStartElement("Polygon");
        writeNewlineOnDebug(xmlw);
        if(compat) {
          xmlw.writeStartElement("altitudeMode");
          xmlw.writeCharacters("relativeToGround");
          xmlw.writeEndElement(); // close altitude mode
          writeNewlineOnDebug(xmlw);
        }
        {
          xmlw.writeStartElement("outerBoundaryIs");
          xmlw.writeStartElement("LinearRing");
          xmlw.writeStartElement("coordinates");

          // Reverse anti-clockwise polygons.
          boolean reverse = (p.testClockwise() >= 0);
          ArrayListIter<double[]> itp = p.iter();
          if(reverse) {
            itp.seek(p.size() - 1);
          }
          while(itp.valid()) {
            double[] v = itp.get();
            xmlw.writeCharacters(FormatUtil.format(v, ","));
            if(compat && (v.length == 2)) {
              xmlw.writeCharacters(",100");
            }
            xmlw.writeCharacters(" ");
            if(!reverse) {
              itp.advance();
            }
            else {
              itp.retract();
            }
          }
          xmlw.writeEndElement(); // close coordinates
          xmlw.writeEndElement(); // close LinearRing
          xmlw.writeEndElement(); // close *BoundaryIs
        }
        writeNewlineOnDebug(xmlw);
        xmlw.writeEndElement(); // Polygon
      }
      xmlw.writeEndElement(); // Placemark
      writeNewlineOnDebug(xmlw);
    }
    xmlw.writeEndElement(); // Document
    xmlw.writeEndElement(); // kml
    xmlw.writeEndDocument();
  }

  /**
   * Recursively step through the clusters to build the hulls.
   * 
   * @param clu Current cluster
   * @param hier Clustering hierarchy
   * @param hulls Hull map
   */
  private DoubleObjPair<Polygon> buildHullsRecursively(Cluster<Model> clu, Hierarchy<Cluster<Model>> hier, Map<Object, DoubleObjPair<Polygon>> hulls, Relation<? extends NumberVector> coords) {
    final DBIDs ids = clu.getIDs();

    FilteredConvexHull2D hull = new FilteredConvexHull2D();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      hull.add(coords.get(iter).toArray());
    }
    double weight = ids.size();
    if(hier != null) {
      final int numc = hier.numChildren(clu);
      if(numc > 0) {
        for(It<Cluster<Model>> iter = hier.iterChildren(clu); iter.valid(); iter.advance()) {
          final Cluster<Model> iclu = iter.get();
          DoubleObjPair<Polygon> poly = hulls.get(iclu);
          if(poly == null) {
            poly = buildHullsRecursively(iclu, hier, hulls, coords);
          }
          // Add inner convex hull to outer convex hull.
          for(ArrayListIter<double[]> vi = poly.second.iter(); vi.valid(); vi.advance()) {
            hull.add(vi.get());
          }
          weight += poly.first / numc;
        }
      }
    }
    DoubleObjPair<Polygon> pair = new DoubleObjPair<>(weight, hull.getHull());
    hulls.put(clu, pair);
    return pair;
  }

  /**
   * Make an HTML description.
   * 
   * @param relations Relations
   * @param id Object ID
   * @return Buffer
   */
  private StringBuilder makeDescription(Collection<Relation<?>> relations, DBIDRef id) {
    StringBuilder buf = new StringBuilder();
    for(Relation<?> rel : relations) {
      Object o = rel.get(id);
      if(o == null) {
        continue;
      }
      String s = o.toString();
      // FIXME: strip html characters
      if(s != null) {
        if(buf.length() > 0) {
          buf.append("<br />");
        }
        buf.append(s);
      }
    }
    return buf;
  }

  /**
   * Make an HTML description.
   * 
   * @param c Cluster
   * @return Buffer
   */
  private StringBuilder makeDescription(Cluster<?> c) {
    return new StringBuilder(200).append("<div>")//
        .append(c.getNameAutomatic())//
        .append("<br />")//
        .append("Size: ").append(c.size()) //
        .append("</div>");
  }

  /**
   * Print a newline when debugging.
   * 
   * @param out Output XML stream
   * @throws XMLStreamException
   */
  private void writeNewlineOnDebug(XMLStreamWriter out) throws XMLStreamException {
    if(LOG.isDebugging()) {
      out.writeCharacters("\n");
    }
  }

  /**
   * Get color from a simple heatmap.
   * 
   * @param val Score value
   * @return Color in heatmap
   */
  public static final Color getColorForValue(double val) {
    // Color positions
    double[] pos = new double[] { 0.0, 0.6, 0.8, 1.0 };
    // Colors at these positions
    Color[] cols = new Color[] { new Color(0.0f, 0.0f, 0.0f, 0.6f), new Color(0.0f, 0.0f, 1.0f, 0.8f), new Color(1.0f, 0.0f, 0.0f, 0.9f), new Color(1.0f, 1.0f, 0.0f, 1.0f) };
    assert (pos.length == cols.length);
    if(val < pos[0]) {
      val = pos[0];
    }
    // Linear interpolation:
    for(int i = 1; i < pos.length; i++) {
      if(val <= pos[i]) {
        Color prev = cols[i - 1];
        Color next = cols[i];
        final double mix = (val - pos[i - 1]) / (pos[i] - pos[i - 1]);
        final int r = (int) ((1 - mix) * prev.getRed() + mix * next.getRed());
        final int g = (int) ((1 - mix) * prev.getGreen() + mix * next.getGreen());
        final int b = (int) ((1 - mix) * prev.getBlue() + mix * next.getBlue());
        final int a = (int) ((1 - mix) * prev.getAlpha() + mix * next.getAlpha());
        Color col = new Color(r, g, b, a);
        return col;
      }
    }
    return cols[cols.length - 1];
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for scaling functions
     */
    public static final OptionID SCALING_ID = new OptionID("kml.scaling", "Additional scaling function for KML colorization.");

    /**
     * Parameter for compatibility mode.
     */
    public static final OptionID COMPAT_ID = new OptionID("kml.compat", "Use simpler KML objects, compatibility mode.");

    /**
     * Parameter for automatically opening the output file.
     */
    public static final OptionID AUTOOPEN_ID = new OptionID("kml.autoopen", "Automatically open the result file.");

    /**
     * Output file name
     */
    File filename;

    /**
     * Scaling function
     */
    OutlierScaling scaling;

    /**
     * Compatibility mode
     */
    boolean compat;

    /**
     * Automatically open at the end
     */
    boolean autoopen = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter outputP = new FileParameter(OutputStep.Parameterizer.OUTPUT_ID, FileParameter.FileType.OUTPUT_FILE);
      outputP.setShortDescription("Filename the KMZ file (compressed KML) is written to.");
      if(config.grab(outputP)) {
        filename = outputP.getValue();
      }

      ObjectParameter<OutlierScaling> scalingP = new ObjectParameter<>(SCALING_ID, OutlierScaling.class, OutlierLinearScaling.class);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }

      Flag compatF = new Flag(COMPAT_ID);
      if(config.grab(compatF)) {
        compat = compatF.getValue();
      }

      Flag autoopenF = new Flag(AUTOOPEN_ID);
      if(config.grab(autoopenF)) {
        autoopen = autoopenF.getValue();
      }
    }

    @Override
    protected KMLOutputHandler makeInstance() {
      return new KMLOutputHandler(filename, scaling, compat, autoopen);
    }
  }
}
