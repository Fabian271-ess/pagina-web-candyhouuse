package com.candyhouse.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Data
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long idPedido;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estado = EstadoPedido.pendiente;

    @Column(name = "direccion_envio", length = 150)
    private String direccionEnvio;

    @Column(length = 50)
    private String ciudad;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "fecha_pedido")
    private LocalDateTime fechaPedido;

    @PrePersist
    protected void onCreate() {
        if (fechaPedido == null) {
            fechaPedido = LocalDateTime.now();
        }
    }

    public enum EstadoPedido {
        entregado, pendiente, cancelado
    }
}