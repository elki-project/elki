package de.lmu.ifi.dbs.elki.result;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.ArrayList;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Abstract evaluation result.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf EvaluationResult.MeasurementGroup
 */
public class EvaluationResult extends BasicResult implements TextWriteable, Iterable<EvaluationResult.MeasurementGroup> {
  /**
   * Measurements.
   */
  ArrayList<EvaluationResult.MeasurementGroup> groups = new ArrayList<>();

  /**
   * Header lines.
   */
  ArrayList<String> header = new ArrayList<>();

  /**
   * Constructor.
   *
   * @param name Evaluation name
   * @param shortname Short name
   */
  public EvaluationResult(String name, String shortname) {
    super(name, shortname);
  }

  /**
   * Add a new measurement group.
   * 
   * @param string Group name
   * @return Measurement group.
   */
  public EvaluationResult.MeasurementGroup newGroup(String string) {
    EvaluationResult.MeasurementGroup g = new MeasurementGroup(string);
    groups.add(g);
    return g;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    for(EvaluationResult.MeasurementGroup g : groups) {
      out.commentPrintLn(g.getName());
      for(MeasurementGroup.Measurement m : g) {
        out.inlinePrintNoQuotes(m.name);
        out.inlinePrintNoQuotes(m.val);
      }
    }
  }

  /**
   * Add a header line to this result.
   * 
   * @param line Header line
   */
  public void addHeader(String line) {
    header.add(line);
  }

  /**
   * Get the header lines.
   * 
   * @return Header lines
   */
  public Iterable<String> getHeaderLines() {
    return header;
  }

  @Override
  public Iterator<MeasurementGroup> iterator() {
    return groups.iterator();
  }

  /**
   * A group of evaluation measurements.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf MeasurementGroup.Measurement
   */
  public static class MeasurementGroup implements Iterable<MeasurementGroup.Measurement> {
    /**
     * Group name
     */
    private String groupname;

    /**
     * Measurements in this group.
     */
    private ArrayList<MeasurementGroup.Measurement> measurements = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param gname Group name
     */
    protected MeasurementGroup(String gname) {
      this.groupname = gname;
    }

    /**
     * Get the group name.
     * 
     * @return Group name
     */
    public String getName() {
      return groupname;
    }

    /**
     * Add a single measurement.
     * 
     * @param name Measurement name
     * @param val Observed value
     * @param max Maximum value
     */
    public void addMeasure(String name, double val, double max) {
      measurements.add(new Measurement(name, val, max));
    }

    @Override
    public Iterator<Measurement> iterator() {
      return measurements.iterator();
    }

    /**
     * Class representing a single measurement.
     * 
     * TODO: add minimum and optimal fields, too?
     * 
     * @author Erich Schubert
     */
    public static class Measurement {
      /**
       * Constructor.
       *
       * @param name Name
       * @param val Value
       * @param max Maximum
       */
      protected Measurement(String name, double val, double max) {
        super();
        this.name = name;
        this.val = val;
        this.max = max;
      }

      /**
       * Measurement name.
       */
      String name;

      /**
       * Observed value, maximum value.
       */
      double val, max;

      /**
       * Get the name of this measurement.
       * 
       * @return Measurement name.
       */
      public String getName() {
        return name;
      }

      /**
       * Get the observed value.
       * 
       * @return observed value.
       */
      public double getVal() {
        return val;
      }

      /**
       * Get the maximum value.
       * 
       * @return Maximum value.
       */
      public double getMax() {
        return max;
      }
    }
  }
}