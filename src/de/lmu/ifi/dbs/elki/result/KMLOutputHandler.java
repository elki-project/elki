package de.lmu.ifi.dbs.elki.result;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierLinearScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * Class to handle KML output.
 * 
 * @author Erich Schubert
 */
// TODO: make configurable color scheme
public class KMLOutputHandler implements ResultHandler, Parameterizable {
  /**
   * Logger class to use.
   */
  public static final Logging logger = Logging.getLogger(KMLOutputHandler.class);

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
  OutlierScalingFunction scaling;

  /**
   * Compatibility mode.
   */
  private boolean compat;

  /**
   * Constructor.
   * 
   * @param filename Output filename
   * @param scaling Scaling function
   * @param compat Compatibility mode
   */
  public KMLOutputHandler(File filename, OutlierScalingFunction scaling, boolean compat) {
    super();
    this.filename = filename;
    this.scaling = scaling;
    this.compat = compat;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    ArrayList<OutlierResult> ors = ResultUtil.filterResults(newResult, OutlierResult.class);
    if(ors.size() > 1) {
      throw new AbortException("More than one outlier result found. The KML writer only supports a single outlier result!");
    }
    if(ors.size() == 1) {
      Database database = ResultUtil.findDatabase(baseResult);
      try {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(filename));
        out.putNextEntry(new ZipEntry("doc.kml"));
        writeKMLData(factory.createXMLStreamWriter(out), ors.get(0), database);
        out.closeEntry();
        out.flush();
        out.close();
      }
      catch(XMLStreamException e) {
        logger.exception(e);
        throw new AbortException("XML error in KML output.", e);
      }
      catch(IOException e) {
        logger.exception(e);
        throw new AbortException("IO error in KML output.", e);
      }
    }
  }

  private void writeKMLData(XMLStreamWriter out, OutlierResult outlierResult, Database database) throws XMLStreamException {
    Relation<Double> scores = outlierResult.getScores();
    Relation<PolygonsObject> polys = database.getRelation(TypeUtil.POLYGON_TYPE);
    Relation<String> labels = DatabaseUtil.guessObjectLabelRepresentation(database);

    Collection<Relation<?>> otherrel = new LinkedList<Relation<?>>(database.getRelations());
    otherrel.remove(scores);
    otherrel.remove(polys);
    otherrel.remove(labels);
    otherrel.remove(database.getRelation(TypeUtil.DBID));

    ArrayModifiableDBIDs ids = DBIDUtil.newArray(scores.getDBIDs());

    scaling.prepare(ids, outlierResult);

    out.writeStartDocument();
    out.writeCharacters("\n");
    out.writeStartElement("kml");
    out.writeDefaultNamespace("http://earth.google.com/kml/2.2");
    out.writeStartElement("Document");
    {
      // TODO: can we automatically generate more helpful data here?
      out.writeStartElement("name");
      out.writeCharacters("ELKI KML output for " + outlierResult.getLongName());
      out.writeEndElement(); // name
      writeNewlineOnDebug(out);
      // TODO: e.g. list the settings in the description?
      out.writeStartElement("description");
      out.writeCharacters("ELKI KML output for " + outlierResult.getLongName());
      out.writeEndElement(); // description
      writeNewlineOnDebug(out);
    }
    {
      // TODO: generate styles from color scheme
      for(int i = 0; i < NUMSTYLES; i++) {
        Color col = getColorForValue(i / (NUMSTYLES - 1.0));
        out.writeStartElement("Style");
        out.writeAttribute("id", "s" + i);
        writeNewlineOnDebug(out);
        {
          out.writeStartElement("LineStyle");
          out.writeStartElement("width");
          out.writeCharacters("0");
          out.writeEndElement(); // width

          out.writeEndElement(); // LineStyle
        }
        writeNewlineOnDebug(out);
        {
          out.writeStartElement("PolyStyle");
          out.writeStartElement("color");
          // KML uses AABBGGRR format!
          out.writeCharacters(String.format("%02x%02x%02x%02x", col.getAlpha(), col.getBlue(), col.getGreen(), col.getRed()));
          out.writeEndElement(); // color
          // out.writeStartElement("fill");
          // out.writeCharacters("1"); // Default 1
          // out.writeEndElement(); // fill
          out.writeStartElement("outline");
          out.writeCharacters("0");
          out.writeEndElement(); // outline
          out.writeEndElement(); // PolyStyle
        }
        writeNewlineOnDebug(out);
        out.writeEndElement(); // Style
        writeNewlineOnDebug(out);
      }
    }
    for(DBID id : outlierResult.getOrdering().iter(ids)) {
      Double score = scores.get(id);
      PolygonsObject poly = polys.get(id);
      String label = labels.get(id);
      if(score == null) {
        logger.warning("No score for object " + id);
      }
      if(poly == null) {
        logger.warning("No polygon for object " + id + " - skipping.");
        continue;
      }
      out.writeStartElement("Placemark");
      {
        out.writeStartElement("name");
        out.writeCharacters(score + " " + label);
        out.writeEndElement(); // name
        StringBuffer buf = makeDescription(otherrel, id);
        out.writeStartElement("description");
        out.writeCData("<div>" + buf.toString() + "</div>");
        out.writeEndElement(); // description
        out.writeStartElement("styleUrl");
        int style = (int) (scaling.getScaled(score) * NUMSTYLES);
        style = Math.max(0, Math.min(style, NUMSTYLES - 1));
        out.writeCharacters("#s" + style);
        out.writeEndElement(); // styleUrl
      }
      {
        out.writeStartElement("Polygon");
        writeNewlineOnDebug(out);
        if(compat) {
          out.writeStartElement("altitudeMode");
          out.writeCharacters("relativeToGround");
          out.writeEndElement(); // close altitude mode
          writeNewlineOnDebug(out);
        }
        // First polygon clockwise?
        boolean first = true;
        for(Polygon p : poly.getPolygons()) {
          if(first) {
            out.writeStartElement("outerBoundaryIs");
          }
          else {
            out.writeStartElement("innerBoundaryIs");
          }
          out.writeStartElement("LinearRing");
          out.writeStartElement("coordinates");

          // Reverse anti-clockwise polygons.
          boolean reverse = (p.testClockwise() >= 0);
          Iterator<Vector> it = reverse ? p.descendingIterator() : p.iterator();
          while(it.hasNext()) {
            Vector v = it.next();
            out.writeCharacters(FormatUtil.format(v.getArrayRef(), ","));
            if(compat && (v.getDimensionality() == 2)) {
              out.writeCharacters(",500");
            }
            out.writeCharacters(" ");
          }
          out.writeEndElement(); // close coordinates
          out.writeEndElement(); // close LinearRing
          out.writeEndElement(); // close *BoundaryIs
          first = false;
        }
        writeNewlineOnDebug(out);
        out.writeEndElement(); // Polygon
      }
      out.writeEndElement(); // Placemark
      writeNewlineOnDebug(out);
    }
    out.writeEndElement(); // Document
    out.writeEndElement(); // kml
    out.writeEndDocument();
  }

  /**
   * Make an HTML description.
   * 
   * @param relations Relations
   * @param id Object ID
   * @return Buffer
   */
  private StringBuffer makeDescription(Collection<Relation<?>> relations, DBID id) {
    StringBuffer buf = new StringBuffer();
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
   * Print a newline when debugging.
   * 
   * @param out Output XML stream
   * @throws XMLStreamException
   */
  private void writeNewlineOnDebug(XMLStreamWriter out) throws XMLStreamException {
    if(logger.isDebugging()) {
      out.writeCharacters("\n");
    }
  }

  private static final Color getColorForValue(double val) {
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for scaling functions
     * 
     * <p>
     * Key: {@code -kml.scaling}
     * </p>
     */
    public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("kml.scaling", "Additional scaling function for KML colorization.");

    /**
     * Parameter for compatibility mode.
     * 
     * <p>
     * Key: {@code -kml.compat}
     * </p>
     */
    public static final OptionID COMPAT_ID = OptionID.getOrCreateOptionID("kml.compat", "Use simpler KML objects, compatibility mode.");

    /**
     * Output file name
     */
    File filename;

    /**
     * Scaling function
     */
    OutlierScalingFunction scaling;

    /**
     * Compatibility mode
     */
    boolean compat;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter outputP = new FileParameter(OptionID.OUTPUT, FileParameter.FileType.OUTPUT_FILE);
      outputP.setShortDescription("Filename the KMZ file (compressed KML) is written to.");
      if(config.grab(outputP)) {
        filename = outputP.getValue();
      }

      ObjectParameter<OutlierScalingFunction> scalingP = new ObjectParameter<OutlierScalingFunction>(SCALING_ID, OutlierScalingFunction.class, OutlierLinearScaling.class);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }

      Flag compatF = new Flag(COMPAT_ID);
      if(config.grab(compatF)) {
        compat = compatF.getValue();
      }
    }

    @Override
    protected KMLOutputHandler makeInstance() {
      return new KMLOutputHandler(filename, scaling, compat);
    }
  }
}