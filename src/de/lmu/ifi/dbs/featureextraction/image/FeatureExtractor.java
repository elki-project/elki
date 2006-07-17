package de.lmu.ifi.dbs.featureextraction.image;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.wrapper.StandAloneInputWrapper;

/**
 * Calculates Haralick texture features of given images.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FeatureExtractor extends StandAloneInputWrapper {
//  /**
//   * Holds the class specific debug status.
//   */
//  @SuppressWarnings({"UNUSED_SYMBOL"})
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//
//  /**
//   * The logger of this class.
//   */
//  private Logger logger = Logger.getLogger(this.getClass().getName());

  static {
    INPUT_D = "<dirname>the directory containing the input files";
    OUTPUT_D = "<dirname>the directory to write the output files into";
  }

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

  public static void main(String[] args) {
//    LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
    FeatureExtractor wrapper = new FeatureExtractor();
    try {
      wrapper.setParameters(args);
      wrapper.run();
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
//      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (AbortException e) {
    	wrapper.verbose(e.getMessage());
//      wrapper.logger.info(e.getMessage());
    }
    catch (Exception e) {
    	wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
//      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  /**
   * Sets the classification file parameter additionally to the parameters
   * provided by super-classes and initializes the option handler.
   */
  public FeatureExtractor() {
    parameterToDescription.put(CLASS_P + OptionHandler.EXPECTS_VALUE, CLASS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * Runs the wrapper with the specified arguments.
   */
  public void run() throws UnableToComplyException {
    try {
      // input
      File inputDir = new File(getInput());
      if (!inputDir.isDirectory()) {
        throw new IllegalStateException("Specified input file is not a directory!");
      }

      // output
      File outputDirectory = new File(getOutput());
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

      Progress progress = new Progress("FeatureExtraction", files.size());
      int processed = 0;

      StringBuffer classIDString = new StringBuffer();
      for (Integer id : classIDs) {
        classIDString.append(CLASS_PREFIX).append(id).append(", ");
      }

      // create the features for each image
      FeatureWriter writer = null;
      for (File file : files) {
        if (isVerbose()) {
          progress.setProcessed(processed++);
          verbose("\rProcessing image " + file + " " + progress.toString());
//          logger.info("\rProcessing image " + file + " " + progress.toString());
        }
        // read image
        FileInputStream in = new FileInputStream(file);
        JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
        BufferedImage decodeimage = decoder.decodeAsBufferedImage();
        in.close();

        // scale image to a given size
        int newsize = 1000000; // desired size in pixel
        int oldsize = decodeimage.getWidth(null)
                      * decodeimage.getHeight(null); // current image size
        if (newsize < oldsize) {
        	warning("\nWarning, reducing size of image which might lead to a loss in quality\n");
//          logger.warning("\nWarning, reducing size of image which might lead to a loss in quality\n");
        }
        double scaling = Math.sqrt((double) newsize / (double) oldsize);
        int newwidth = (int) (decodeimage.getWidth(null) * scaling);
        int newheight = (int) (decodeimage.getHeight(null) * scaling);
        Image scaledimage = decodeimage.getScaledInstance(newwidth, newheight, Image.SCALE_SMOOTH);

        // convert back to BufferedImage
        BufferedImage bufferimage = new BufferedImage(scaledimage.getWidth(null),
                                                      scaledimage.getHeight(null),
                                                      decodeimage.getType());
        // copy image to buffered image
        Graphics graph = bufferimage.createGraphics();
        // paint the image onto the buffered image
        graph.drawImage(scaledimage, 0, 0, null);
        graph.dispose();

        // create an image descriptor
        ImageDescriptor descriptor = new ImageDescriptor(bufferimage);

        if (writer == null) {
          writer = new FeatureArffWriter(descriptor.featureInfos, getOutput(), "image", classIDString.substring(0, classIDString.length() - 2));
//           writer = new FeatureTxtWriter(descriptor.featureInfos, getOutput(), classIDString.substring(0, classIDString.length() - 2));
        }

        // dump the extracted features
        writer.writeFeatures(descriptor.featureInfos, file.getName(), fileNameToClassId.get(file.getName().toLowerCase()), SEPARATOR, CLASS_PREFIX);
      }
      if (writer != null) {
        writer.flush();
        writer.close();
      }

    }
    catch (IOException e) {
      e.printStackTrace();
    }
    if (isVerbose()) {
    	verbose("");
//      logger.info("\n");
    }
  }

  /**
   * Reads the file containing the class ids for the images and returns a
   * mapping of the image name to the class id.
   *
   * @return a mapping of the image name to the class id
   */
  private Map<String, Integer> readClassFile() throws IOException {
    Map<String, Integer> res = new HashMap<String, Integer>();
    BufferedReader reader = new BufferedReader(new FileReader(classFileName));
    String line;
    while ((line = reader.readLine()) != null) {
      if (line.length() == 0)
        continue;
      StringTokenizer tok = new StringTokenizer(line, ";");
      Integer classId = Integer.parseInt(tok.nextToken());
      tok.nextToken();
      String imgName = tok.nextToken().toLowerCase().trim();
      res.put(imgName, classId);
    }
    reader.close();
    return res;
  }

  /**
   * Returns an array of the files in the specified directory that satisfy the
   * specified filter. If a file is a directory the directory path is searched
   * recursively.
   *
   * @param filter the file filter
   * @param dir    the directory
   * @param result the arry containg the result
   */
  private void listRecursiveFiles(FileFilter filter, File dir,
                                  List<File> result) {
    File[] files = dir.listFiles(filter);
    for (File file : files) {
      if (file.isDirectory())
        listRecursiveFiles(filter, file, result);
      else
        result.add(file);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    // class
    classFileName = optionHandler.getOptionValue(CLASS_P);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(CLASS_P, classFileName);
    return settings;
  }
}
