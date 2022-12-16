package blender.distributed.Cliente.view;

import io.github.cdimascio.dotenv.Dotenv;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.HeadlessException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class GUIFinishedWork extends JFrame {
    Dotenv dotenv = Dotenv.load();
    private String textHyperlink = "Enlace de Descarga";
    private JLabel hyperlink = new JLabel(textHyperlink);

    public GUIFinishedWork(String blendName, String gStorageZipName, Long timeTaken) throws HeadlessException {
        super();
        setTitle("Render Finalizado");
        String textLabel = "Render del blend " + blendName + " finalizado.";
        JLabel label1 = new JLabel(textLabel);
        label1.setAlignmentX(Component.CENTER_ALIGNMENT);
        String textLabel2 = "Tiempo tardado: " + timeTaken + " segundos.";
        JLabel label2 = new JLabel(textLabel2);
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);
        hyperlink.setAlignmentX(Component.CENTER_ALIGNMENT);
        hyperlink.setForeground(Color.BLUE.darker());
        hyperlink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        hyperlink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://storage.cloud.google.com/"+dotenv.get("FINAL_ZIP_BUCKET_NAME")+"/"+gStorageZipName));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });

        getContentPane().add(Box.createVerticalGlue());
        getContentPane().add(label1);
        getContentPane().add(label2);
        getContentPane().add(hyperlink);
        getContentPane().add(Box.createVerticalGlue());
        setLayout(new BoxLayout (getContentPane(), BoxLayout.Y_AXIS));

        setSize(400, 150);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}