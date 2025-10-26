package rmi;

import java.io.Serializable;

public class InformacionProducto implements Serializable { // <--- AÑADIDO

    // Añadir serialVersionUID para control de versiones
    private static final long serialVersionUID = 1L;

    String vendedor;
    String producto;
    float precioInicial;
    float precioActual;

    public InformacionProducto(String v, String p, float pi) {
        vendedor = v;
        producto = p;
        precioInicial = pi;
        precioActual = pi;
    }

    // La lógica de actualización debe ser 'synchronized' si se usa en el servidor
    // pero aquí se pasa por valor, así que la sincronización
    // debe estar en el SubastaModeloRMI.
    public boolean actualizaPrecio(float monto) {
        if (monto > precioActual) {
            precioActual = monto;
            return true;
        } else {
            return false;
        }
    }

    public String getNombreProducto() {
        return producto;
    }

    public float getPrecioActual() {
        return precioActual;
    }
}