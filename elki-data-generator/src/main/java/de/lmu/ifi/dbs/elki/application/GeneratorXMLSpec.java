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
package de.lmu.ifi.dbs.elki.application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorSingleCluster;
import de.lmu.ifi.dbs.elki.datasource.GeneratorXMLDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Generate a data set based on a specified model (using an XML specification)
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @composed - - - GeneratorXMLDatabaseConnection
 */
public class GeneratorXMLSpec extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(GeneratorXMLSpec.class);

  /**
   * Line separator for output
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

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
   * @param output Output file
   * @param generator GeneratorXMLDatabaseConnection
   */
  public GeneratorXMLSpec(File output, GeneratorXMLDatabaseConnection generator) {
    super();
    this.outputFile = output;
    this.generator = generator;
  }

  /**
   * Runs the wrapper with the specified arguments.
   */
  @Override
  public void run() {
    MultipleObjectsBundle data = generator.loadData();
    if(LOG.isVerbose()) {
      LOG.verbose("Writing output ...");
    }
    try {
      if(outputFile.exists() && LOG.isVerbose()) {
        LOG.verbose("The file " + outputFile + " already exists, " + "the generator result will be APPENDED.");
      }

      try (OutputStreamWriter outStream = new FileWriter(outputFile, true)) {
        writeClusters(outStream, data);
      }
    }
    catch(IOException e) {
      throw new AbortException("IO Error in data generator.", e);
    }
    if(LOG.isVerbose()) {
      LOG.verbose("Done.");
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
        if(Model.TYPE.isAssignableFromType(data.meta(i))) {
          modelcol = i;
          break;
        }
      }
    }
    if(modelcol < 0) {
      throw new AbortException("No model column found in bundle.");
    }
    ArrayList<Model> models = new ArrayList<>();
    Map<Model, IntArrayList> modelMap = new HashMap<>();
    { // Build a map from model to the actual objects
      for(int i = 0; i < data.dataLength(); i++) {
        Model model = (Model) data.data(i, modelcol);
        IntArrayList modelids = modelMap.get(model);
        if(modelids == null) {
          models.add(model);
          modelids = new IntArrayList();
          modelMap.put(model, modelids);
        }
        modelids.add(i);
      }
    }
    // compute global discard values
    int totalsize = 0, totaldisc = 0;
    for(Entry<Model, IntArrayList> ent : modelMap.entrySet()) {
      totalsize += ent.getValue().size();
      if(ent.getKey() instanceof GeneratorSingleCluster) {
        totaldisc += ((GeneratorSingleCluster) ent.getKey()).getDiscarded();
      }
    }
    double globdens = (double) (totalsize + totaldisc) / totalsize;
    outStream.append("########################################################").append(LINE_SEPARATOR);
    outStream.append("## Number of clusters: " + models.size()).append(LINE_SEPARATOR);
    for(Model model : models) {
      IntArrayList ids = modelMap.get(model);
      outStream.append("########################################################").append(LINE_SEPARATOR);
      outStream.append("## Size: " + ids.size()).append(LINE_SEPARATOR);
      if(model instanceof GeneratorSingleCluster) {
        GeneratorSingleCluster cursclus = (GeneratorSingleCluster) model;
        outStream.append("########################################################").append(LINE_SEPARATOR);
        outStream.append("## Cluster: ").append(cursclus.getName()).append(LINE_SEPARATOR);
        double[] cmin = cursclus.getClipmin();
        double[] cmax = cursclus.getClipmax();
        if(cmin != null && cmax != null) {
          outStream.append("## Clipping: ").append(FormatUtil.format(cmin))//
          .append(" - ").append(FormatUtil.format(cmax)).append(LINE_SEPARATOR);
        }
        outStream.append("## Density correction factor: " + cursclus.getDensityCorrection()).append(LINE_SEPARATOR);
        outStream.append("## Generators:").append(LINE_SEPARATOR);
        for(int i = 0; i < cursclus.getDim(); i++) {
          Distribution gen = cursclus.getDistribution(i);
          outStream.append("##   ").append(gen.toString()).append(LINE_SEPARATOR);
        }
        if(cursclus.getTransformation() != null && cursclus.getTransformation().getTransformation() != null) {
          outStream.append("## Affine transformation matrix:").append(LINE_SEPARATOR);
          outStream.append(FormatUtil.format(cursclus.getTransformation().getTransformation(), "## ")).append(LINE_SEPARATOR);
        }
        outStream.append("## Discards: " + cursclus.getDiscarded() + " Retries left: " + cursclus.getRetries()).append(LINE_SEPARATOR);
        double corf = /* cursclus.overweight */(double) (cursclus.getSize() + cursclus.getDiscarded()) / cursclus.getSize() / globdens;
        outStream.append("## Density correction factor estimation: " + corf).append(LINE_SEPARATOR);
      }
      outStream.append("########################################################").append(LINE_SEPARATOR);
      for(IntIterator iter = ids.iterator(); iter.hasNext();) {
        int num = iter.nextInt();
        for(int c = 0; c < data.metaLength(); c++) {
          if(c != modelcol) {
            if(c > 0) {
              outStream.append(' ');
            }
            outStream.append(data.data(num, c).toString());
          }
        }
        outStream.append(LINE_SEPARATOR);
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
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
      generator = config.tryInstantiate(GeneratorXMLDatabaseConnection.class);
      // Output file
      outputFile = getParameterOutputFile(config, "The file to write the generated data set into, if the file already exists, the generated points will be appended to this file.");
    }

    @Override
    protected GeneratorXMLSpec makeInstance() {
      return new GeneratorXMLSpec(outputFile, generator);
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
