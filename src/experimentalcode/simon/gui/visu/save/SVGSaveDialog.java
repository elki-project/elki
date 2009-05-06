/**
 * 
 */
package experimentalcode.simon.gui.visu.save;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.batik.transcoder.TranscoderException;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;


/**
 * A "generic" save dialog to save a SVG to disk. Supported formats:
 * <ul>
 * <li>JPEG (broken) (with width and height options)</li>
 * <li>PNG (with width and height options)</li>
 * <li>PDF</li>
 * <li>PS</li>
 * <li>EPS</li>
 * <li>SVG</li>
 * </ul>
 * 
 * @author simon
 * 
 */
public class SVGSaveDialog {

	/** The default title. "Save as ...". */
	public final static String DEFAULT_TITLE = "Save as ...";

	/** The file chooser. */
	private JFileChooser fc;

	/** The plot to be exported. */
	private SVGPlot svgPlot;
	/** The width of the exported image (if exported to JPEG or PNG). Default is 1024. */
	private int width = 1024;
	/** The height of the exported image (if exported to JPEG or PNG). Default is 768. */
	private int height = 768;
	/** The quality of the exported jpeg. Default is 1.0. */
	private double quality = 1.0;
	
	/** The panel for the options (JPEG and PNG options) */
	private SaveOptionsPanel optionsPanel;


	/**
	 * A constructor to specify width and height hints for export to JPEG/PNG.
	 * 
	 * @param The plot to be exported.
	 * @param width The width of the exported image (when export to JPEG/PNG).
	 * @param height The height of the exported image (when export to JPEG/PNG).
	 * @see {@link SVGSaveDialog(SVGPlot)}
	 * */
	public SVGSaveDialog(SVGPlot plot, int width, int height) {
		this.width = width;
		this.height = height;
		initUI(plot);
	}


	/** Creates the components and handles the saving logic. */
	private void initUI(SVGPlot plot) {
		fc = new JFileChooser();
		fc.setDialogTitle(DEFAULT_TITLE);
		//fc.setFileFilter(new ImageFilter());
		optionsPanel = new SaveOptionsPanel(fc, width, height);
		fc.setAccessory(optionsPanel);

		int ret = fc.showSaveDialog(null);
		fc.setDialogTitle("Saving... Please wait.");
		if (ret == JFileChooser.APPROVE_OPTION) {
			String ext = optionsPanel.getImageFormat();
			try {
				File file = fc.getSelectedFile();
				if (ext.equals("jpeg") || ext.equals("jpg")) {
					quality = optionsPanel.getQuality();
					plot.saveAsJPEG(file, width, height, quality);
				} else if (ext.equals("png")) {
					plot.saveAsPNG(file, width, height);
				} else if (ext.equals("ps")) {
					plot.saveAsPS(file);
				} else if (ext.equals("eps")) {
					plot.saveAsEPS(file);
				} else if (ext.equals("pdf")) {
					plot.saveAsPDF(file);
				} else if (ext.equals("svg")) {
					plot.saveAsSVG(file);
				} else {
					showError(fc, "Unsupported format: " + ext);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
				showError(fc, "Error: " + ioe);
			} catch (TranscoderException e) {
				e.printStackTrace();
				showError(fc, "Error: " + e);
			} catch (TransformerFactoryConfigurationError tfce) {
				tfce.printStackTrace();
				showError(fc, "Error: " + tfce);
			} catch (TransformerException te) {
				te.printStackTrace();
				showError(fc, "Error: " + te);
			}
		} else if (ret == JFileChooser.ERROR_OPTION) {
			showError(fc, "Unknown Error.");
		} else if (ret == JFileChooser.CANCEL_OPTION) {

		}	
	}

	

	/**
	 * Helper method to show a error message as "popup".
	 * Calls {@link JOptionPane#showMessageDialog(java.awt.Component, Object)}.
	 * 
	 * @param parent The parent component for the popup.
	 * @param msg The message to be displayed.
	 * */
	private void showError(Component parent, String msg) {
		JOptionPane.showMessageDialog(parent, msg);
	}

	

}
