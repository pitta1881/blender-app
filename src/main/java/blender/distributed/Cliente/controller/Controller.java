package blender.distributed.Cliente.controller;


import blender.distributed.Cliente.Cliente;

import java.io.File;

public class Controller{
	Cliente cliente;
	
	public Controller(Cliente modelo) {
		this.cliente = modelo;
	}
	public void setFile(File f) {
		this.cliente.setFile(f);
	}
	public void enviarFile(int startFrame, int endFrame) {
		this.cliente.enviarFile(startFrame, endFrame);
	}
	public boolean isReady() {
		return this.cliente.isReady();
	}
}
