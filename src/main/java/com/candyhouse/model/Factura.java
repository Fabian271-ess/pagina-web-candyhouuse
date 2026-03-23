package com.candyhouse.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "facturas")
@Data
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Long idFactura;

    @Column(name = "pago_total", precision = 15, scale = 2)
    private BigDecimal pagoTotal;

    @Column(name = "fecha_factura")
    private LocalDate fechaFactura;

    @Column(length = 250)
    private String descripcion;

    @Column(name = "cliente_doc")
    private Long clienteDoc;

    @Column(name = "id_pedido")
    private Long idPedido;
}
