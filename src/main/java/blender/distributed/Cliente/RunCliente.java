package blender.distributed.Cliente;

import blender.distributed.Cliente.controller.Controller;
import blender.distributed.Cliente.view.GUICliente;

public class RunCliente {

	public static void main(String[] args) {
		Cliente modelo = new Cliente();
		Controller controlador = new Controller(modelo);
		GUICliente iniciar = new GUICliente(controlador);
	}
}
