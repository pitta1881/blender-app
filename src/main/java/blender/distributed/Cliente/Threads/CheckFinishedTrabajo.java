package blender.distributed.Cliente.Threads;

import blender.distributed.Enums.EStatus;
import blender.distributed.Records.RGateway;
import blender.distributed.Records.RTrabajo;
import blender.distributed.Servidor.Cliente.ClienteAction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static blender.distributed.Cliente.Tools.connectRandomGatewayRMI;

public class CheckFinishedTrabajo implements Runnable{
    Logger log = LoggerFactory.getLogger(ClienteAction.class);
    List<RGateway> listaGateways;
    RTrabajo recordTrabajo;
    Gson gson = new Gson();
    Type RTrabajoType = new TypeToken<RTrabajo>(){}.getType();
    Dotenv dotenv = Dotenv.load();

    public CheckFinishedTrabajo(List<RGateway> listaGateways, RTrabajo recordTrabajo){
        this.listaGateways = listaGateways;
        this.recordTrabajo = recordTrabajo;
    }

    @Override
    public void run() {
        boolean salir = false;
        LocalTime initTime = LocalTime.now();
        log.info("Trabajo enviado: " + this.recordTrabajo.toString());
        log.info("Tiempo inicio:\t" + initTime.toString());
        String recordTrabajoJson;
        RTrabajo recordTrabajo = null;
        while (!salir) {
            try {
                recordTrabajoJson = connectRandomGatewayRMI(this.listaGateways).getTrabajo(this.recordTrabajo.uuid());
                recordTrabajo = gson.fromJson(recordTrabajoJson, RTrabajoType);
                if (recordTrabajo != null && recordTrabajo.estado() == EStatus.DONE && recordTrabajo.gStorageZipName() != null) {
                    salir = true;
                }
                Thread.sleep(1000);
            } catch (RemoteException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        LocalTime finishTime = LocalTime.now();
        Long timeTaken = Duration.between(initTime, finishTime).toSeconds();
        log.info("Tiempo fin:\t" + finishTime.toString());
        log.info("Tiempo tardado:\t" + timeTaken + " segundos.");
        new HyperlinkDemo(recordTrabajo.blendName(), recordTrabajo.gStorageZipName(), timeTaken).setVisible(true);
    }

    public class HyperlinkDemo extends JFrame {
        private String textHyperlink = "Enlace de Descarga";
        private JLabel hyperlink = new JLabel(textHyperlink);

        public HyperlinkDemo(String blendName, String gStorageZipName, Long timeTaken) throws HeadlessException {
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

}
