package blender.distributed.Cliente;

import blender.distributed.Cliente.controller.Controller;
import blender.distributed.Cliente.view.GUICliente;
import blender.distributed.Records.RTrabajo;

import java.util.ArrayList;
import java.util.List;


public class RunCliente {


	public static void main(String[] args) {
		List<RTrabajo> listaTrabajos = new ArrayList<>();

		Cliente modelo = new Cliente(listaTrabajos);
		Controller controlador = new Controller(modelo);
		GUICliente iniciar = new GUICliente(controlador, listaTrabajos);
	}
}
