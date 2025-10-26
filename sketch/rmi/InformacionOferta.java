package rmi;

import java.io.Serializable;

public class InformacionOferta implements Serializable { // <--- AÑADIDO

    private static final long serialVersionUID = 1L;

    String comprador;
    String producto;
    float monto;

    public InformacionOferta(String c, String p, float m) {
        comprador = c;
        producto = p;
        monto = m;
    }
}