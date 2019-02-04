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
package de.lmu.ifi.dbs.elki.datasource.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.ExternalID;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;

/**
 * Parser to load polygon data (2D and 3D only) from a simple format. One record
 * per line, points separated by whitespace, numbers separated by colons.
 * Multiple polygons components can be separated using {@code --}.
 * <p>
 * Unparseable parts will be treated as labels.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @has - - - PolygonsObject
 */
public class SimplePolygonParser extends AbstractStreamingParser {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SimplePolygonParser.class);

  /**
   * Pattern to catch coordinates
   */
  public static final Pattern COORD = Pattern.compile("^(" + CSVReaderFormat.NUMBER_PATTERN + "),\\s*(" + CSVReaderFormat.NUMBER_PATTERN + ")(?:,\\s*(" + CSVReaderFormat.NUMBER_PATTERN + "))?$");

  /**
   * Polygon separator
   */
  public static final String POLYGON_SEPARATOR = "--";

  /**
   * Event to report next.
   */
  Event nextevent = null;

  /**
   * Constructor.
   * 
   * @param format Input format
   */
  public SimplePolygonParser(CSVReaderFormat format) {
    super(format);
  }

  /**
   * Metadata.
   */
  protected BundleMeta meta = null;

  /**
   * Whether or not the data set has labels.
   */
  protected boolean haslabels = false;

  /**
   * Current polygon.
   */
  protected PolygonsObject curpoly = null;

  /**
   * Current labels.
   */
  protected LabelList curlbl = null;

  /**
   * Current external id.
   */
  protected ExternalID cureid = null;

  /**
   * (Reused) storage of coordinates.
   */
  final private List<double[]> coords = new ArrayList<>();

  /**
   * (Reused) storage of polygons.
   */
  final private List<Polygon> polys = new ArrayList<>();

  /**
   * (Reused) store for labels.
   */
  final private ArrayList<String> labels = new ArrayList<>();

  @Override
  public Event nextEvent() {
    if(nextevent != null) {
      Event ret = nextevent;
      nextevent = null;
      return ret;
    }
    try {
      while(reader.nextLineExceptComments()) {
        if(parseLine()) {
          if(meta == null || (curlbl != null && !haslabels)) {
            haslabels = haslabels || curlbl != null;
            buildMeta();
            nextevent = Event.NEXT_OBJECT;
            return Event.META_CHANGED;
          }
          return Event.META_CHANGED;
        }
      }
      return Event.END_OF_STREAM;
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + reader.getLineNumber() + ".");
    }
  }

  /**
   * Update the meta element.
   */
  protected void buildMeta() {
    if(haslabels) {
      meta = new BundleMeta(3);
      meta.add(TypeUtil.POLYGON_TYPE);
      meta.add(TypeUtil.EXTERNALID);
      meta.add(TypeUtil.LABELLIST);
    }
    else {
      meta = new BundleMeta(2);
      meta.add(TypeUtil.POLYGON_TYPE);
      meta.add(TypeUtil.EXTERNALID);
    }
  }

  @Override
  public BundleMeta getMeta() {
    return meta;
  }

  @Override
  public Object data(int rnum) {
    if(rnum > (haslabels ? 2 : 1)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return (rnum == 0) ? curpoly : (rnum == 1) ? cureid : curlbl;
  }

  /**
   * Parse a single line.
   * 
   * @return {@code true} if the line was read successful.
   */
  private boolean parseLine() {
    cureid = null;
    curpoly = null;
    curlbl = null;
    polys.clear();
    coords.clear();
    labels.clear();

    Matcher m = COORD.matcher(reader.getBuffer());
    for(/* initialized by nextLineExceptComments */; tokenizer.valid(); tokenizer.advance()) {
      m.region(tokenizer.getStart(), tokenizer.getEnd());
      if(m.find()) {
        try {
          double c1 = ParseUtil.parseDouble(m.group(1));
          double c2 = ParseUtil.parseDouble(m.group(2));
          if(m.group(3) != null) {
            double c3 = ParseUtil.parseDouble(m.group(3));
            coords.add(new double[] { c1, c2, c3 });
          }
          else {
            coords.add(new double[] { c1, c2 });
          }
          continue;
        }
        catch(NumberFormatException e) {
          LOG.warning("Looked like a coordinate pair but didn't parse: " + tokenizer.getSubstring());
        }
      }
      // Match polygon separator:
      // FIXME: Avoid unnecessary subSequence call.
      final int len = tokenizer.getEnd() - tokenizer.getStart();
      if(POLYGON_SEPARATOR.length() == len && //
          reader.getBuffer().subSequence(tokenizer.getStart(), tokenizer.getEnd()).equals(POLYGON_SEPARATOR)) {
        if(!coords.isEmpty()) {
          polys.add(new Polygon(new ArrayList<>(coords)));
        }
        continue;
      }
      String cur = tokenizer.getSubstring();
      // First label will become the External ID
      if(cureid == null) {
        cureid = new ExternalID(cur);
      }
      else {
        labels.add(cur);
      }
    }
    // Complete polygon
    if(!coords.isEmpty()) {
      polys.add(new Polygon(coords));
    }
    curpoly = new PolygonsObject(polys);
    curlbl = (haslabels || !labels.isEmpty()) ? LabelList.make(labels) : null;
    return true;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractStreamingParser.Parameterizer {
    @Override
    protected SimplePolygonParser makeInstance() {
      return new SimplePolygonParser(format);
    }
  }
}
