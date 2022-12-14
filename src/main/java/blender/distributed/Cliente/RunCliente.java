package blender.distributed.Cliente;

import blender.distributed.Cliente.controller.Controller;
import blender.distributed.Cliente.view.GUICliente;
import blender.distributed.Enums.ENodo;
import org.slf4j.MDC;

public class RunCliente {

	public static void main(String[] args) {
		MDC.put("log.name", ENodo.CLIENTE.name());
		Cliente modelo = new Cliente();
		Controller controlador = new Controller(modelo);
		GUICliente iniciar = new GUICliente(controlador);
	}
}
