package de.lmu.ifi.dbs.featureextraction.image;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FeatureWriter {

//String dirName;
  private BufferedWriter writers[];

  private boolean useClassLabel = true;
  private boolean useID = true;

  private Integer[] clNames;

  private static final int histWriterIndex = 0;

  private static final int momentsWriterIndex = 1;

  private static final int f1_WriterIndex = 2;

  private static final int f2_WriterIndex = 3;

  private static final int f3_WriterIndex = 4;

  private static final int f4_WriterIndex = 5;

  private static final int f5_WriterIndex = 6;

  private static final int f6_WriterIndex = 7;

  private static final int f7_WriterIndex = 8;

  private static final int f8_WriterIndex = 9;

  private static final int f9_WriterIndex = 10;

  private static final int f10_WriterIndex = 11;

  private static final int f11_WriterIndex = 12;

  private static final int f12_WriterIndex = 13;

  private static final int f13_WriterIndex = 14;

  private static final String[] specifier = { "colorhist", "colormoments",
      "f1", "f2", "f3", "f4","f5", "f6", "f7", "f8", "f9", "f10", "f11", "f12",
      "f13" };

  private static final int[] featNumbs = { 32, 9 };

  FeatureWriter(String dirName, String videoID, Integer[] classNames) {
    writers = new BufferedWriter[15];
    clNames = classNames;
    for (int i = 0; i < writers.length; i++) {
      try {
        String dName = dirName + File.separator
                       + specifier[i] + File.separator;
        File dir = new File(dName);
        if(!dir.exists()) {
          dir.mkdir();
        }
        writers[i] = new BufferedWriter(new FileWriter(dName + videoID + ".arff"));
        int numFeats = 4;
        if(i<featNumbs.length) numFeats = featNumbs[i];
        printHeader(writers[i], videoID, specifier[i], numFeats);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {

      public void run() {
        close();
        Runtime.getRuntime().runFinalization();
      }
    });

  }
  public void setClassNames(Integer classNames[]) {
    this.clNames = classNames;
  }

  private void printHeader(BufferedWriter w, String videoID, String featName, int numFeat) {
    try {
      w.write("@relation " + videoID+"_"+featName);
      w.newLine();
      if(useClassLabel ) {
        w.write("@attribute id string\n");
      }
      w.newLine();
      for (int j = 0; j < numFeat; j++) {
        w.write("@attribute d" + j + " numeric");
        w.newLine();
      }
      if(useClassLabel ) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < this.clNames.length; i++) {
          if(i>0)buf.append(",");
          buf.append(clNames[i]);
        }
        w.write("@attribute class {"+buf.toString()+"}\n");
        //w.write("@attribute key string\n");
      }
      w.write("\n@data");
      w.newLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void flush() {
    for (int i = 0; i < writers.length; i++) {
      if (writers[i] != null) {
        try {
          writers[i].flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }


  public void print(ImageDescriptor desc) {
    desc.writeColorHistogram(writers[0]);
    desc.writeColorMoments(writers[1]);
    desc.writeF1(writers[2]);
    desc.writeF2(writers[3]);
    desc.writeF3(writers[4]);
    desc.writeF4(writers[5]);
    desc.writeF5(writers[6]);
    desc.writeF6(writers[7]);
    desc.writeF7(writers[8]);
    desc.writeF8(writers[9]);
    desc.writeF9(writers[10]);
    desc.writeF10(writers[11]);
    desc.writeF11(writers[12]);
    desc.writeF12(writers[13]);
    desc.writeF13(writers[14]);
  }

  /**
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }

  public void close() {
    for (int i = 0; i < writers.length; i++) {
      if (writers[i] != null) {
        try {

          writers[i].flush();
          writers[i].close();
        } catch (IOException e) {
          //e.printStackTrace();
        }
      }
    }
  }
  
}
