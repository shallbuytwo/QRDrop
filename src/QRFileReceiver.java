import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public class QRFileReceiver extends JFrame implements Runnable, ThreadFactory {
	private static final long serialVersionUID = 6441489157408381878L;
	private Executor executor = Executors.newSingleThreadExecutor(this);
	private Webcam webcam = null;
	private WebcamPanel panel = null;
	private JTextArea textarea = null;
	
	private int seqNumberOfBatchToReceive = 1;
	private JFrame jframe = new JFrame("QR Code Display Interface");
	private JLabel jlabel = new JLabel();
	private FileOutputStream out = new FileOutputStream("out.txt");

	public QRFileReceiver() throws IOException {
		super();
		
		jlabel.setHorizontalAlignment(JLabel.CENTER);
	    jlabel.setVerticalAlignment(JLabel.CENTER);
		jframe.getContentPane().add(jlabel);
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.pack();
        jframe.setExtendedState(JFrame.MAXIMIZED_BOTH);
        jframe.setVisible(true);
		
		setLayout(new FlowLayout());
		setTitle("Capturing Information from Sender");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Dimension size = WebcamResolution.VGA.getSize();
		
		webcam = Webcam.getWebcams().get(0);
		webcam.setViewSize(size);

		panel = new WebcamPanel(webcam);
		panel.setPreferredSize(size);

		textarea = new JTextArea();
		textarea.setEditable(false);
		textarea.setPreferredSize(size);

		add(panel);
		add(textarea);

		pack();
		setVisible(true);
		executor.execute(this);
	}

	@Override
	public void run() {
		do {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Result result = null;
			BufferedImage image = null;

			if (webcam.isOpen()) {
				if ((image = webcam.getImage()) == null) {
					continue;
				}
				LuminanceSource source = new BufferedImageLuminanceSource(image);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

				try {
					result = new MultiFormatReader().decode(bitmap);
				} catch (NotFoundException e) {
					// No QR code found
				}
			}

			if (result != null) {
				String resultString = result.getText();
				textarea.setText(resultString);
				String[] tokens = resultString.split(" ", 2);
				if (Integer.parseInt(tokens[0]) == seqNumberOfBatchToReceive) {
					seqNumberOfBatchToReceive++;
					try {
						writeToFile(tokens[1]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				generateFeedback(seqNumberOfBatchToReceive, tokens[1].length());
			}
		} while (true);
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "example-runner");
		t.setDaemon(true);
		return t;
	}

	public void generateFeedback(int seq, int num) {
		String feedback = Integer.toString(seq) + " " + Integer.toString(num);
		jlabel.setIcon(new ImageIcon(QRHelper.generateQRImage(QRHelper.generateQRByteMatrix(feedback))));
		jframe.setTitle("QR Code Display Interface. " + "Feedback Batch Number: " + seq + ". Data Size: " + num);
	}
	
	public void writeToFile(String text) throws IOException {
		out.write(text.getBytes());
	}
	
	public static void main(String[] args) throws IOException {
		new QRFileReceiver();
	}
}
