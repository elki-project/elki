package de.lmu.ifi.dbs.elki.data.synthetic.bymodel;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;

/**
 * Generate a data set according to a given model.
 * 
 * Key idea of this generator is to re-generate points if they are
 * more likely to belong to a different cluster than the one they
 * were generated for. The benefit is that we should end up with a
 * data set that follows closely the model that we specified.
 * 
 * The drawbacks are that on one hand, specifications might be unsatisfiable.
 * For this a retry count is kept and an {@link UnableToComplyException} is thrown
 * when the maximum number of retries is exceeded.
 * 
 * On the other hand, the model might not be exactly as specified. When the generator
 * reports an "Density correction factor estimation" that differs from 1.0 this is an
 * indication that the result is not exact.
 * 
 * On the third hand, rejecting points introduces effects where one generator can
 * influence others, so random generator results will not be stable with respect to
 * the addition of new dimensions and similar if there are any rejects involved.
 * So this generator is not entirely optimal for generating data sets for scalability
 * tests on the number of dimensions, although if clusters overlap little enough (so
 * that no rejects happen) the results should be as expected.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public class GeneratorMain {
  /**
   * Line separator for output
   */
  public final static String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * List of clusters to generate
   */
  private LinkedList<GeneratorInterface> clusters = new LinkedList<GeneratorInterface>();

  /**
   * Add a cluster to the cluster list.
   *  
   * @param c cluster to add
   */
  public void addCluster(GeneratorInterface c) {
    clusters.add(c);
  }

  /**
   * Main loop to generate data set.
   * 
   * @throws UnableToComplyException when model not satisfiable or no clusters specified.  
   */
  public void generate() throws UnableToComplyException {
    // we actually need some clusters.
    if (clusters.size() < 1)
      throw new UnableToComplyException("No clusters specified.");
    // Assert that cluster dimensions agree.
    int dim = clusters.get(0).getDim();
    for (GeneratorInterface c : clusters)
      if (c.getDim() != dim)
        throw new UnableToComplyException("Cluster dimensions do not agree.");
    // generate clusters
    for(GeneratorInterface curclus : clusters) {
      while(curclus.getPoints().size() < curclus.getSize()) {
        // generate the "missing" number of points
        List<Vector> newp = curclus.generate(curclus.getSize() - curclus.getPoints().size());
        if (curclus instanceof GeneratorInterfaceDynamic) {
          GeneratorInterfaceDynamic cursclus = (GeneratorInterfaceDynamic) curclus;
          for(Vector p : newp) {
            double max = 0.0;
            double is = 0.0;
            for(GeneratorInterface other : clusters) {
              double d = other.getDensity(p) * other.getSize();
              if(other == curclus)
                is = d;
              else if(d > max)
                max = d;
            }
            // Only keep the point if the largest density was the cluster it was
            // generated for
            if(is >= max)
              cursclus.getPoints().add(p);
            else
              cursclus.addDiscarded(1);
          }
        }
      }
    }
  }

  public void writeClusters(OutputStreamWriter outStream) throws IOException {
    // compute global discard values
    int totalsize = 0;
    int totaldisc = 0;
    assert(clusters.size() > 0);
    for(GeneratorInterface curclus : clusters) {
      totalsize = totalsize + curclus.getSize();
      if (curclus instanceof GeneratorSingleCluster) {
        totaldisc = totaldisc + ((GeneratorSingleCluster)curclus).getDiscarded();
      }
    }
    double globdens = (double)(totalsize + totaldisc) / totalsize;
    outStream.write("########################################################" + LINE_SEPARATOR);
    outStream.write("## Number of clusters: " + clusters.size() + LINE_SEPARATOR);
    for(GeneratorInterface curclus : clusters) {
      outStream.write("########################################################" + LINE_SEPARATOR);
      outStream.write("## Cluster: " + curclus.getName() + LINE_SEPARATOR);
      outStream.write("########################################################" + LINE_SEPARATOR);
      outStream.write("## Size: " + curclus.getSize() + LINE_SEPARATOR);
      if (curclus instanceof GeneratorSingleCluster) {
        GeneratorSingleCluster cursclus = (GeneratorSingleCluster) curclus; 
        Vector cmin = cursclus.getClipmin();
        Vector cmax = cursclus.getClipmax();
        if (cmin != null && cmax !=  null)
          outStream.write("## Clipping: " + cmin.toString() + " - " + cmax.toString() + LINE_SEPARATOR);
        outStream.write("## Density correction factor: " + cursclus.getDensityCorrection() + LINE_SEPARATOR);
        outStream.write("## Generators:" + LINE_SEPARATOR);
        for(Distribution gen : cursclus.getAxes())
          outStream.write("##   " + gen.toString() + LINE_SEPARATOR);
        if(cursclus.getTrans() != null && cursclus.getTrans().getTransformation() != null) {
          outStream.write("## Affine transformation matrix:" + LINE_SEPARATOR);
          outStream.write(cursclus.getTrans().getTransformation().toString("## ") + LINE_SEPARATOR);
        }
      }
      if (curclus instanceof GeneratorInterfaceDynamic) {
        GeneratorSingleCluster cursclus = (GeneratorSingleCluster) curclus; 
        outStream.write("## Discards: " + cursclus.getDiscarded() + " Retries left: " + cursclus.getRetries() + LINE_SEPARATOR);
        double corf = /* cursclus.overweight * */ (double)(cursclus.getSize() + cursclus.getDiscarded()) / cursclus.getSize() / globdens;
        outStream.write("## Density correction factor estimation: " + corf  + LINE_SEPARATOR);
        
      }
      outStream.write("########################################################" + LINE_SEPARATOR);
      for(Vector p : curclus.getPoints()) {
        for(int i = 0; i < p.getRowDimensionality(); i++)
          outStream.write(p.get(i) + " ");
        outStream.write(curclus.getName());
        outStream.write(LINE_SEPARATOR);
      }
    }
  }
}
