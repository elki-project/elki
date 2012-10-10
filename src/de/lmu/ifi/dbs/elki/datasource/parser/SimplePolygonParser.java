package de.lmu.ifi.dbs.elki.datasource.parser;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.ExternalID;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.StringLengthConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Parser to load polygon data (2D and 3D only) from a simple format. One record
 * per line, points separated by whitespace, numbers separated by colons.
 * Multiple polygons components can be separated using
 * {@link #POLYGON_SEPARATOR}.
 * 
 * Unparseable parts will be treated as labels.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has PolygonsObject
 */
public class SimplePolygonParser extends AbstractParser implements Parser {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SimplePolygonParser.class);

  /**
   * Pattern to catch coordinates
   */
  public static final Pattern COORD = Pattern.compile("^(" + NUMBER_PATTERN + "),\\s*(" + NUMBER_PATTERN + ")(?:,\\s*(" + NUMBER_PATTERN + "))?$");

  /**
   * Polygon separator
   */
  public static final String POLYGON_SEPARATOR = "--";

  /**
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   */
  public SimplePolygonParser(Pattern colSep, char quoteChar) {
    super(colSep, quoteChar);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;

    List<PolygonsObject> polys = new ArrayList<PolygonsObject>();
    List<LabelList> labels = null;
    List<ExternalID> eids = new ArrayList<ExternalID>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          Object[] objs = parseLine(line);
          polys.add((PolygonsObject) objs[0]);
          if(objs[1] != null) {
            if(labels == null) {
              labels = new ArrayList<LabelList>();
              for(int i = 0; i < polys.size() - 1; i++) {
                labels.add(null);
              }
            }
            labels.add((LabelList) objs[1]);
          }
          eids.add((ExternalID) objs[2]);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    if(labels != null) {
      return MultipleObjectsBundle.makeSimple(TypeUtil.POLYGON_TYPE, polys, TypeUtil.LABELLIST, labels, TypeUtil.EXTERNALID, eids);
    }
    else {
      return MultipleObjectsBundle.makeSimple(TypeUtil.POLYGON_TYPE, polys, TypeUtil.EXTERNALID, eids);
    }
  }

  /**
   * Parse a single line.
   * 
   * @param line Line to parse
   * 
   * @return Parsed polygon
   */
  private Object[] parseLine(String line) {
    List<String> entries = tokenize(line);
    Iterator<String> iter = entries.iterator();

    ExternalID eid = null;
    LabelList labels = null;
    List<Polygon> polys = new ArrayList<Polygon>(1);

    List<Vector> coords = new ArrayList<Vector>();
    while(iter.hasNext()) {
      String cur = iter.next();
      Matcher m = COORD.matcher(cur);
      if(m.find()) {
        try {
          double c1 = Double.parseDouble(m.group(1));
          double c2 = Double.parseDouble(m.group(2));
          if(m.group(3) != null) {
            double c3 = Double.parseDouble(m.group(3));
            coords.add(new Vector(new double[] { c1, c2, c3 }));
          }
          else {
            coords.add(new Vector(new double[] { c1, c2 }));
          }
          continue;
        }
        catch(NumberFormatException e) {
          LOG.warning("Looked like a coordinate pair but didn't parse: " + cur);
        }
      }
      // Polygon separator.
      if(cur.equals(POLYGON_SEPARATOR)) {
        if(coords.size() > 0) {
          polys.add(new Polygon(coords));
          coords = new ArrayList<Vector>();
        }
        continue;
      }
      // First label will become the External ID
      if(eid == null) {
        eid = new ExternalID(cur);
      }
      else {
        // Label
        if(labels == null) {
          labels = new LabelList(1);
        }
        labels.add(cur);
      }
    }
    // Complete polygon
    if(coords.size() > 0) {
      polys.add(new Polygon(coords));
    }
    return new Object[] { new PolygonsObject(polys), labels, eid };
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParser.Parameterizer {
    @Override
    protected void makeOptions(Parameterization config) {
      PatternParameter colParam = new PatternParameter(COLUMN_SEPARATOR_ID, "\\s+");
      if(config.grab(colParam)) {
        colSep = colParam.getValue();
      }
      StringParameter quoteParam = new StringParameter(QUOTE_ID, new StringLengthConstraint(1, 1), String.valueOf(QUOTE_CHAR));
      if(config.grab(quoteParam)) {
        quoteChar = quoteParam.getValue().charAt(0);
      }
    }

    @Override
    protected SimplePolygonParser makeInstance() {
      return new SimplePolygonParser(colSep, quoteChar);
    }
  }
}