package de.lmu.ifi.dbs.featureextraction.image;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.wrapper.AbstractWrapper;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * Calculates Haralick texture features of given images.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FeatureExtractor extends AbstractWrapper {
  /**
   * The separator between attributes in the output file.
   */
  public String SEPARATOR = " ";

  /**
   * The prefix for the classID in the output file.
   */
  public String CLASS_PREFIX = "c";

  /**
   * Label for parameter input.
   */
  public final static String CLASS_P = "class";

  /**
   * Description for parameter input.
   */
  public final static String CLASS_D = "<filename>classification file to be parsed.";

  /**
   * The file name for the classification file.
   */
  private String classFileName;

  /**
   * Sets the classification file parameter additionally to the
   * parameters provided by super-classes and initializes the option handler.
   */
  public FeatureExtractor() {
    parameterToDescription.put(CLASS_P + OptionHandler.EXPECTS_VALUE, CLASS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Runs the wrapper with the specified arguments.
   *
   * @param args parameter list
   */
  public void run(String[] args) {
    try {
      this.setParameters(args);
      // input
      File inputDir = new File(input);
      if (!inputDir.isDirectory()) {
        throw new IllegalStateException("Specified input file is not a directory!");
      }
      // output
      File outputDirectory = new File(output);
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      // create a mapping of image names to class id
      final Map<String, Integer> fileNameToClassId = readClassFile();
      Set<Integer> classIDs = new HashSet<Integer>(fileNameToClassId.values());
      // get the image files (jpg) in the input directory
      FileFilter filter = new FileFilter() {
        public boolean accept(File f) {
          String name = f.getName().toLowerCase();
          return f.isDirectory() || fileNameToClassId.containsKey(name) && name.endsWith(".jpg");
        }
      };
      List<File> files = new ArrayList<File>();
      listRecursiveFiles(filter, inputDir, files);

      Progress progress = new Progress(files.size());
      int processed = 0;

      StringBuffer classIDString = new StringBuffer();
      for (Integer id : classIDs) {
        classIDString.append(CLASS_PREFIX).append(id).append(", ");
      }
      
      // create the texture features for each image
//      FeatureArffWriter writer = new FeatureArffWriter(output, "classified_images", classIDs);
      FeatureWriter writer = new FeatureTxtWriter(output, classIDString.substring(0, classIDString.length()-2));

      for (File file : files) {
        if (verbose) {
          progress.setProcessed(processed++);
          System.out.print("\rProcessing image " + file + " " + progress.toString() + "                              ");
        }
        // read image
        FileInputStream in = new FileInputStream(file);
        JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
        BufferedImage image = decoder.decodeAsBufferedImage();
        in.close();
        // create an image descriptor
        ImageDescriptor descriptor = new ImageDescriptor(image);
        descriptor.setImageName(file.getName());
        descriptor.setClassID(fileNameToClassId.get(file.getName().toLowerCase()));
        writer.writeFeatures(SEPARATOR, CLASS_PREFIX, descriptor);
      }
      writer.flush();
      writer.close();

    }
    catch (IOException e) {
      e.printStackTrace();
    }

  }

  /**
   * Sets the attributes of the class accordingly to the given parameters.
   * Returns a new String array containing those entries of the
   * given array that are neither expected nor used by this
   * Parameterizable.
   *
   * @param args parameters to set the attributes accordingly to
   * @return String[] an array containing the unused parameters
   * @throws IllegalArgumentException in case of wrong parameter-setting
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    super.setParameters(args);
    try {
      classFileName = optionHandler.getOptionValue(CLASS_P);
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return new String[0];
  }

  /**
   * Reads the file containing the class ids for the images and returns a mapping
   * of the image name to the class id.
   *
   * @return a mapping of the image name to the class id
   */
  private Map<String, Integer> readClassFile() throws IOException {
    Map<String, Integer> res = new HashMap<String, Integer>();
    BufferedReader reader = new BufferedReader(new FileReader(classFileName));
    String line;
    while ((line = reader.readLine()) != null) {
      if (line.length() == 0) continue;
      StringTokenizer tok = new StringTokenizer(line, ";");
      Integer classId = Integer.parseInt(tok.nextToken());
      tok.nextToken();
      String imgName = tok.nextToken().toLowerCase();
      res.put(imgName, classId);
    }
    reader.close();
    return res;
  }

  /**
   * Returns an array of the files in the specified directory that
   * satisfy the specified filter. If a file is a directory
   * the directory path is searched recursively.
   *
   * @param filter the file filter
   * @param dir    the directory
   * @param result the arry containg the result
   */
  private void listRecursiveFiles(FileFilter filter, File dir, List<File> result) {
    File[] files = dir.listFiles(filter);
    for (File file : files) {
      if (file.isDirectory()) listRecursiveFiles(filter, file, result);
      else result.add(file);
    }
  }

  public static void main(String[] args) {
    FeatureExtractor extractor = new FeatureExtractor();
    try {
      extractor.run(args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
