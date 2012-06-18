package de.lmu.ifi.dbs.elki.application;

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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorSingleCluster;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.GeneratorXMLDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.DistributionWithRandom;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Generate a data set based on a specified model (using an XML specification)
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf GeneratorXMLDatabaseConnection
 */
public class GeneratorXMLSpec extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(GeneratorXMLSpec.class);

  /**
   * Line separator for output
   */
  public final static String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * Output file.
   */
  private File outputFile;

  /**
   * The original data source.
   */
  private GeneratorXMLDatabaseConnection generator;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param output Output file
   * @param generator GeneratorXMLDatabaseConnection
   */
  public GeneratorXMLSpec(boolean verbose, File output, GeneratorXMLDatabaseConnection generator) {
    super(verbose);
    this.outputFile = output;
    this.generator = generator;
  }

  /**
   * Runs the wrapper with the specified arguments.
   */
  @Override
  public void run() throws UnableToComplyException {
    MultipleObjectsBundle data = generator.loadData();
    if(logger.isVerbose()) {
      logger.verbose("Writing output ...");
    }
    try {
      if(outputFile.exists()) {
        if(logger.isVerbose()) {
          logger.verbose("The file " + outputFile + " already exists, " + "the generator result will be appended.");
        }
      }

      OutputStreamWriter outStream = new FileWriter(outputFile, true);
      writeClusters(outStream, data);

      outStream.flush();
      outStream.close();
    }
    catch(FileNotFoundException e) {
      throw new UnableToComplyException(e);
    }
    catch(IOException e) {
      throw new UnableToComplyException(e);
    }
    if(logger.isVerbose()) {
      logger.verbose("Done.");
    }
  }

  /**
   * Write the resulting clusters to an output stream.
   * 
   * @param outStream output stream
   * @param data Generated data
   * @throws IOException thrown on write errors
   */
  public void writeClusters(OutputStreamWriter outStream, MultipleObjectsBundle data) throws IOException {
    int modelcol = -1;
    { // Find model column
      for(int i = 0; i < data.metaLength(); i++) {
        if(TypeUtil.MODEL.isAssignableFromType(data.meta(i))) {
          modelcol = i;
          break;
        }
      }
    }
    if(modelcol < 0) {
      throw new AbortException("No model column found in bundle.");
    }
    ArrayList<Model> models = new ArrayList<Model>();
    Map<Model, TIntList> modelMap = new HashMap<Model, TIntList>();
    { // Build a map from model to the actual objects
      for(int i = 0; i < data.dataLength(); i++) {
        Model model = (Model) data.data(i, modelcol);
        TIntList modelids = modelMap.get(model);
        if(modelids == null) {
          models.add(model);
          modelids = new TIntArrayList();
          modelMap.put(model, modelids);
        }
        modelids.add(i);
      }
    }
    // compute global discard values
    int totalsize = 0;
    int totaldisc = 0;
    for(Entry<Model, TIntList> ent : modelMap.entrySet()) {
      totalsize = totalsize + ent.getValue().size();
      if(ent.getKey() instanceof GeneratorSingleCluster) {
        totaldisc = totaldisc + ((GeneratorSingleCluster) ent.getKey()).getDiscarded();
      }
    }
    double globdens = (double) (totalsize + totaldisc) / totalsize;
    outStream.write("########################################################" + LINE_SEPARATOR);
    outStream.write("## Number of clusters: " + models.size() + LINE_SEPARATOR);
    for(Model model : models) {
      TIntList ids = modelMap.get(model);
      outStream.write("########################################################" + LINE_SEPARATOR);
      outStream.write("## Size: " + ids.size() + LINE_SEPARATOR);
      if(model instanceof GeneratorSingleCluster) {
        GeneratorSingleCluster cursclus = (GeneratorSingleCluster) model;
        outStream.write("########################################################" + LINE_SEPARATOR);
        outStream.write("## Cluster: " + cursclus.getName() + LINE_SEPARATOR);
        Vector cmin = cursclus.getClipmin();
        Vector cmax = cursclus.getClipmax();
        if(cmin != null && cmax != null) {
          outStream.write("## Clipping: " + cmin.toString() + " - " + cmax.toString() + LINE_SEPARATOR);
        }
        outStream.write("## Density correction factor: " + cursclus.getDensityCorrection() + LINE_SEPARATOR);
        outStream.write("## Generators:" + LINE_SEPARATOR);
        for(int i = 0; i < cursclus.getDim(); i++) {
          DistributionWithRandom gen = cursclus.getDistribution(i);
          outStream.write("##   " + gen.toString() + LINE_SEPARATOR);
        }
        if(cursclus.getTransformation() != null && cursclus.getTransformation().getTransformation() != null) {
          outStream.write("## Affine transformation matrix:" + LINE_SEPARATOR);
          outStream.write(FormatUtil.format(cursclus.getTransformation().getTransformation(), "## ") + LINE_SEPARATOR);
        }
        outStream.write("## Discards: " + cursclus.getDiscarded() + " Retries left: " + cursclus.getRetries() + LINE_SEPARATOR);
        double corf = /* cursclus.overweight */(double) (cursclus.getSize() + cursclus.getDiscarded()) / cursclus.getSize() / globdens;
        outStream.write("## Density correction factor estimation: " + corf + LINE_SEPARATOR);

      }
      outStream.write("########################################################" + LINE_SEPARATOR);
      for(TIntIterator iter = ids.iterator(); iter.hasNext();) {
        int num = iter.next();
        for(int c = 0; c < data.metaLength(); c++) {
          if(c != modelcol) {
            if (c > 0) {
              outStream.write(" ");
            }
            outStream.write(data.data(num, c).toString());
          }
        }
        outStream.write(LINE_SEPARATOR);
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * Output file.
     */
    private File outputFile = null;

    /**
     * Data generator
     */
    private GeneratorXMLDatabaseConnection generator = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Output file
      outputFile = getParameterOutputFile(config, "the file to write the generated data set into, if the file already exists, the generated points will be appended to this file.");
      generator = config.tryInstantiate(GeneratorXMLDatabaseConnection.class);
    }

    @Override
    protected GeneratorXMLSpec makeInstance() {
      return new GeneratorXMLSpec(verbose, outputFile, generator);
    }
  }

  /**
   * Main method to run this application.
   * 
   * @param args the arguments to run this application
   */
  public static void main(String[] args) {
    runCLIApplication(GeneratorXMLSpec.class, args);
  }
}