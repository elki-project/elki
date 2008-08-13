package de.lmu.ifi.dbs.elki.algorithm.result.clustering;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.statistics.LinearRegression;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.DoublePair;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * todo arthur comment
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Result
 */
public class FractalDimensionTestResult<V extends RealVector<V, ?>> extends AbstractResult<V> {
    private Integer id1;

    private Integer id2;

    private List<Integer> id1Supporter;

    private List<Integer> id2Supporter;

    private V centroid;

    private List<Integer> centroidSupporter;

    private EuclideanDistanceFunction<V> distanceFunction = new EuclideanDistanceFunction<V>();

    public FractalDimensionTestResult(Database<V> database, Integer id1, Integer id2, List<Integer> id1_supporter, List<Integer> id2_supporter, V centroid, List<Integer> centroid_supporter) {
        super(database);
        this.id1 = id1;
        this.id2 = id2;
        this.id1Supporter = id1_supporter;
        this.id2Supporter = id2_supporter;
        this.centroid = centroid;
        this.centroidSupporter = centroid_supporter;
        this.distanceFunction.setDatabase(database, false, false);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.result.Result#output(java.io.PrintStream,de.lmu.ifi.dbs.elki.normalization.Normalization,java.util.List)
     */
    public void output(PrintStream outStream, Normalization normalization, List settings) throws UnableToComplyException {
        throw new UnsupportedOperationException("Specification of output file required.");

    }

    @Override
    public void output(File out, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        if (out == null) {
            throw new NullPointerException("File not set in method output in class " + this.getClass().getCanonicalName());
        }
        out.mkdirs();
        PrintStream pout;
        try {
            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "dataset" + FILE_EXTENSION));
            for (Integer id : this.getDatabase().getIDs()) {
                pout.println(this.getDatabase().get(id));
            }
            pout.flush();
            pout.close();


            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "id1" + FILE_EXTENSION));
            pout.println("# id1");
            pout.println(getDatabase().get(id1));
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "id1Supporter" + FILE_EXTENSION));
            pout.println("# id1 supporter");
            for (Integer id : id1Supporter) {
                pout.println(getDatabase().get(id));
            }
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "id1FractalDimension" + FILE_EXTENSION));
            pout.println("# id1 fractal dimension");
            List<DoublePair> points = new ArrayList<DoublePair>(this.id1Supporter.size());
            for (int i = 1; i <= id1Supporter.size(); i++) {
                points.add(new DoublePair(StrictMath.log(distanceFunction.distance(this.id1Supporter.get(i - 1), this.id1).getValue()),
                    StrictMath.log(i)));
            }
            LinearRegression id1FC = new LinearRegression(points);
            pout.println("# m=" + id1FC.getM());
            pout.println("# t=" + id1FC.getT());
            for (DoublePair p : points) {
                pout.print(p.getX());
                pout.print(" ");
                pout.println(p.getY());
            }
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "id2" + FILE_EXTENSION));
            pout.println("# id2");
            pout.println(getDatabase().get(id2));
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "id2Supporter" + FILE_EXTENSION));
            pout.println("# id2 supporter");
            for (Integer id : id2Supporter) {
                pout.println(getDatabase().get(id));
            }
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "id2FractalDimension" + FILE_EXTENSION));
            pout.println("# id2 fractal dimension");
            points = new ArrayList<DoublePair>(this.id2Supporter.size());
            for (int i = 1; i <= id2Supporter.size(); i++) {
                points.add(new DoublePair(StrictMath.log(distanceFunction.distance(this.id2Supporter.get(i - 1), this.id2).getValue()),
                    StrictMath.log(i)));
            }
            LinearRegression id2FC = new LinearRegression(points);
            pout.println("# m=" + id2FC.getM());
            pout.println("# t=" + id2FC.getT());
            for (DoublePair p : points) {
                pout.print(p.getX());
                pout.print(" ");
                pout.println(p.getY());
            }
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "centroid" + FILE_EXTENSION));
            pout.println("# centroid");
            pout.println(centroid);
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "centroidSupporter" + FILE_EXTENSION));
            pout.println("# centroid supporter");
            for (Integer id : centroidSupporter) {
                pout.println(getDatabase().get(id));
            }
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "centroidFractalDimension" + FILE_EXTENSION));
            pout.println("# centroid fractal dimension");
            points = new ArrayList<DoublePair>(this.centroidSupporter.size());
            for (int i = 1; i <= centroidSupporter.size(); i++) {
                points.add(new DoublePair(StrictMath.log(distanceFunction.distance(this.centroidSupporter.get(i - 1), this.centroid).getValue()),
                    StrictMath.log(i)));
            }
            LinearRegression centroidFC = new LinearRegression(points);
            pout.println("# m=" + centroidFC.getM());
            pout.println("# t=" + centroidFC.getT());
            for (DoublePair p : points) {
                pout.print(p.getX());
                pout.print(" ");
                pout.println(p.getY());
            }
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "loglogspace" + FILE_EXTENSION));
//            pout.println("plot \"centroidFractalDimension.txt\"");
//            pout.println("replot c(x) = x*m + t, m = "+centroidFC.getM()+", t = "+centroidFC.getT()+", c(x)");
            pout.println("plot [0:10] c(x) = x*m, m = " + centroidFC.getM() + ", c(x)");
//            pout.println("replot \"id1FractalDimension.txt\"");
//            pout.println("replot id1(x) = x*m + t, m = "+id1FC.getM()+", t = "+id1FC.getT()+", id1(x)");
            pout.println("replot id1(x) = x*m, m = " + id1FC.getM() + ", id1(x)");
//            pout.println("replot \"id2FractalDimension.txt\"");
//            pout.println("replot id2(x) = x*m + t, m = "+id2FC.getM()+", t = "+id2FC.getT()+", id2(x)");
            pout.println("replot id2(x) = x*m, m = " + id2FC.getM() + ", id2(x)");
            pout.println("pause -1");
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "dataspace_id1" + FILE_EXTENSION));
            pout.println("plot \"dataset.txt\"");
            pout.println("replot \"id1.txt\"");
            pout.println("replot \"id1Supporter.txt\"");
            pout.println("pause -1");
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "dataspace_id2" + FILE_EXTENSION));
            pout.println("plot \"dataset.txt\"");
            pout.println("replot \"id2.txt\"");
            pout.println("replot \"id2Supporter.txt\"");
            pout.println("pause -1");
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "dataspace_centroid" + FILE_EXTENSION));
            pout.println("plot \"dataset.txt\"");
            pout.println("replot \"centroid.txt\"");
            pout.println("replot \"centroidSupporter.txt\"");
            pout.println("pause -1");
            pout.flush();
            pout.close();

            pout = new PrintStream(new FileOutputStream(out.getAbsolutePath() + File.separator + "dataspace_complete" + FILE_EXTENSION));
            pout.println("plot \"dataset.txt\"");
            pout.println("replot \"id1.txt\"");
            pout.println("replot \"id1Supporter.txt\"");
            pout.println("replot \"id2.txt\"");
            pout.println("replot \"id2Supporter.txt\"");
            pout.println("replot \"centroid.txt\"");
            pout.println("replot \"centroidSupporter.txt\"");
            pout.println("pause -1");
            pout.flush();
            pout.close();
        }
        catch (FileNotFoundException e) {
            throw new UnableToComplyException(e);
        }
    }


}
