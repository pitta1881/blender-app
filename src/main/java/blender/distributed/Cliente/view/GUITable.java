package blender.distributed.Cliente.view;

import blender.distributed.Enums.EStatus;
import blender.distributed.Records.RTrabajo;
import io.github.cdimascio.dotenv.Dotenv;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GUITable extends JFrame {
    List<RTrabajo> listaTrabajos;
    Dotenv dotenv = Dotenv.load();
    GUITable(List<RTrabajo> listaTrabajos){
        super();
        this.listaTrabajos = listaTrabajos;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        setTitle("Lista de Trabajos");

        List<String> columns = new ArrayList<String>();
        List<String[]> values = new ArrayList<String[]>();

        columns.add("Proyecto");
        columns.add("Fecha");
        columns.add("Estado");
        columns.add("Rango Frames");
        columns.add("Link");

        synchronized (this.listaTrabajos) {
            listaTrabajos.forEach(trabajo -> {
                String url = "";
                String estado = "En Proceso";
                if(trabajo.gStorageZipName() != null){
                    url = "https://storage.cloud.google.com/" + dotenv.get("FINAL_ZIP_BUCKET_NAME") + "/" + trabajo.gStorageZipName();
                }
                if(trabajo.estado().equals(EStatus.DONE)){
                    estado = "Finalizado";
                }
                values.add(new String[]{trabajo.blendName(), LocalDateTime.parse(trabajo.createdAt()).format(formatter), estado, trabajo.startFrame() + "-" + trabajo.endFrame() , url });
            });
        }
        TableModel tableModel = new DefaultTableModel(values.toArray(new Object[][] {}), columns.toArray());
        JTable jTable = new JTable(tableModel);

        jTable.setBounds(30,40,600,250);
        JScrollPane jScrollPane=new JScrollPane(jTable);
        add(jScrollPane);
        setSize(700,400);
        setResizable(true);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}