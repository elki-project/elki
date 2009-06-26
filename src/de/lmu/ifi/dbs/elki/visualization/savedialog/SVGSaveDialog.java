package de.lmu.ifi.dbs.elki.visualization.savedialog;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.batik.transcoder.TranscoderException;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * A save dialog to save/export a SVG image to a file.
 * 
 * Supported formats:
 * <ul>
 * <li>JPEG (broken) (with width and height options)</li>
 * <li>PNG (with width and height options)</li>
 * <li>PDF</li>
 * <li>PS</li>
 * <li>EPS</li>
 * <li>SVG</li>
 * </ul>
 * 
 * @author Simon Mittermüller
 */
public class SVGSaveDialog {
	/** The default title. "Save as ...". */
	public final static String DEFAULT_TITLE = "Save as ...";
	
	/** Static logger reference */
	private final static Logging logger = Logging.getLogger(SVGSaveDialog.class);
	
	/** Automagic file format */
	final static String automagic_format = "automatic";

	/** Supported file format (extensions) */
  final static String[] formats = { "svg", "png", "jpeg", "jpg", "pdf", "ps", "eps" };

  /** Visible file formats */
  final static String[] visibleformats = { automagic_format, "svg", "png", "jpeg", "pdf", "ps", "eps" };
	
	/**
	 * Show a "Save as" dialog.
	 * 
   * @param plot The plot to be exported.
   * @param width The width of the exported image (when export to JPEG/PNG).
   * @param height The height of the exported image (when export to JPEG/PNG).
   * @return Result from {@link JFileChooser#showSaveDialog}
	 */
	public static int showSaveDialog(SVGPlot plot, int width, int height) {
	  double quality = 1.0;
	  int ret = -1;

	  JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(DEFAULT_TITLE);
		//fc.setFileFilter(new ImageFilter());
		SaveOptionsPanel optionsPanel = new SaveOptionsPanel(fc, width, height);
		fc.setAccessory(optionsPanel);

		ret = fc.showSaveDialog(null);
		fc.setDialogTitle("Saving... Please wait.");
		if (ret == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
			String format = optionsPanel.getSelectedFormat();
			if (format == null || format == automagic_format) {
			  format = guessFormat(file.getName());
			}
			try {
			  if (format == null) {
          showError(fc, "Error saving image.", "File format not recognized.");
			  } else if (format.equals("jpeg") || format.equals("jpg")) {
					quality = optionsPanel.getJPEGQuality();
					plot.saveAsJPEG(file, width, height, quality);
				} else if (format.equals("png")) {
					plot.saveAsPNG(file, width, height);
				} else if (format.equals("ps")) {
					plot.saveAsPS(file);
				} else if (format.equals("eps")) {
					plot.saveAsEPS(file);
				} else if (format.equals("pdf")) {
					plot.saveAsPDF(file);
				} else if (format.equals("svg")) {
					plot.saveAsSVG(file);
				} else {
					showError(fc, "Error saving image.", "Unsupported format: " + format);
				}
			} catch (java.lang.IncompatibleClassChangeError e) {
        showError(fc, "Error saving image.", "It seems that your Java version is incompatible with this version of Batik and Jpeg writing. Sorry.");
			} catch (IOException e) {
				logger.exception(e);
				showError(fc, "Error saving image.", e.toString());
			} catch (TranscoderException e) {
        logger.exception(e);
        showError(fc, "Error saving image.", e.toString());
			} catch (TransformerFactoryConfigurationError e) {
        logger.exception(e);
        showError(fc, "Error saving image.", e.toString());
			} catch (TransformerException e) {
        logger.exception(e);
        showError(fc, "Error saving image.", e.toString());
      } catch (Exception e) {
        logger.exception(e);
        showError(fc, "Error saving image.", e.toString());
      }
		} else if (ret == JFileChooser.ERROR_OPTION) {
			showError(fc, "Error in file dialog.", "Unknown Error.");
		} else if (ret == JFileChooser.CANCEL_OPTION) {
		  // do nothing - except return result
		}	
		return ret;
	}
	
	/**
	 * Guess a supported format from the file name. For "auto" format handling.
	 * 
	 * @param name File name
	 * @return format or "null"
	 */
	public static String guessFormat(String name) {
    String ext = FileUtil.getFilenameExtension(name);
    int index = Util.arrayFind(formats, ext);
    if (index >= 0) {
      return ext;
    } else {
      return null;
    }
	}

  /**
   * @return the formats
   */
	public static String[] getFormats() {
    return formats;
  }

  /**
   * @return the visibleformats
   */
  public static String[] getVisibleFormats() {
    return visibleformats;
  }

	/**
	 * Helper method to show a error message as "popup".
	 * Calls {@link JOptionPane#showMessageDialog(java.awt.Component, Object)}.
	 * 
	 * @param parent The parent component for the popup.
	 * @param msg The message to be displayed.
	 * */
	private static void showError(Component parent, String title, String msg) {
		JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
	}	
}
