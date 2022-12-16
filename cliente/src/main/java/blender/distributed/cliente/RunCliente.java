package blender.distributed.cliente;

import blender.distributed.cliente.controller.Controller;
import blender.distributed.cliente.view.GUICliente;

public class RunCliente {

	public static void main(String[] args) {
		Cliente modelo = new Cliente();
		Controller controlador = new Controller(modelo);
		GUICliente iniciar = new GUICliente(controlador);
	}
}
