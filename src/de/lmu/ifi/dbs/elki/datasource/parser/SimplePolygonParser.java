package de.lmu.ifi.dbs.elki.datasource.parser;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
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
   * @param colSep Column separator
   * @param quoteChars Quotation character
   * @param comment Comment pattern
   */
  public SimplePolygonParser(Pattern colSep, String quoteChars, Pattern comment) {
    super(colSep, quoteChars, comment);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;

    List<PolygonsObject> polys = new ArrayList<>();
    List<LabelList> labels = null;
    List<ExternalID> eids = new ArrayList<>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        // Skip empty lines and comments
        if(line.length() <= 0 || (comment != null && comment.matcher(line).matches())) {
          continue;
        }
        Object[] objs = parseLine(line);
        polys.add((PolygonsObject) objs[0]);
        if(objs[1] != null) {
          if(labels == null) {
            labels = new ArrayList<>();
            for(int i = 0; i < polys.size() - 1; i++) {
              labels.add(null);
            }
          }
          labels.add((LabelList) objs[1]);
        }
        eids.add((ExternalID) objs[2]);
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
    ExternalID eid = null;
    List<Polygon> polys = new ArrayList<>(1);
    ArrayList<String> labels = new ArrayList<>(); // TODO: reuse?

    List<Vector> coords = new ArrayList<>();
    for(tokenizer.initialize(line, 0, lengthWithoutLinefeed(line)); tokenizer.valid(); tokenizer.advance()) {
      Matcher m = COORD.matcher(line).region(tokenizer.getStart(), tokenizer.getEnd());
      if(m.find()) {
        try {
          double c1 = FormatUtil.parseDouble(m.group(1));
          double c2 = FormatUtil.parseDouble(m.group(2));
          if(m.group(3) != null) {
            double c3 = FormatUtil.parseDouble(m.group(3));
            coords.add(new Vector(new double[] { c1, c2, c3 }));
          }
          else {
            coords.add(new Vector(new double[] { c1, c2 }));
          }
          continue;
        }
        catch(NumberFormatException e) {
          LOG.warning("Looked like a coordinate pair but didn't parse: " + tokenizer.getSubstring());
        }
      }
      // Match polygon separator:
      final int len = tokenizer.getEnd() - tokenizer.getStart();
      if(POLYGON_SEPARATOR.length() == len && //
      POLYGON_SEPARATOR.regionMatches(0, line, tokenizer.getStart(), len)) {
        if(coords.size() > 0) {
          polys.add(new Polygon(coords));
          coords = new ArrayList<>();
        }
        continue;
      }
      String cur = tokenizer.getSubstring();
      // First label will become the External ID
      if(eid == null) {
        eid = new ExternalID(cur);
      }
      else {
        labels.add(cur);
      }
    }
    // Complete polygon
    if(coords.size() > 0) {
      polys.add(new Polygon(coords));
    }
    return new Object[] { new PolygonsObject(polys), LabelList.make(labels), eid };
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
      StringParameter quoteParam = new StringParameter(QUOTE_ID, QUOTE_CHARS);
      if(config.grab(quoteParam)) {
        quoteChars = quoteParam.getValue();
      }

      PatternParameter commentP = new PatternParameter(COMMENT_ID, COMMENT_PATTERN);
      if(config.grab(commentP)) {
        comment = commentP.getValue();
      }
    }

    @Override
    protected SimplePolygonParser makeInstance() {
      return new SimplePolygonParser(colSep, quoteChars, comment);
    }
  }
}
