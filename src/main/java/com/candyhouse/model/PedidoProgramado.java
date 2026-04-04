package com.candyhouse.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "pedidos_programados")

@Data
public class PedidoProgramado {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id_programado")
        private Long idProgramado;

        /** Pedido padre al que pertenece esta programación */
        @Column(name = "id_pedido", nullable = false, unique = true)
        private Long idPedido;

        /** Total de unidades solicitadas (suma de todos los productos) */
        @Column(name = "cantidad_total", nullable = false)
        private Integer cantidadTotal;

        /** Fecha a partir de la cual se empieza a preparar / entregar */
        @Column(name = "fecha_inicio", nullable = false)
        private LocalDate fechaInicio;

        /** Fecha límite de entrega total */
        @Column(name = "fecha_fin", nullable = false)
        private LocalDate fechaFin;

        /**
         * Cada cuántos días se hace una entrega parcial.
         * Ejemplo: 7 = entregas semanales.
         */
        @Column(name = "intervalo_dias", nullable = false)
        private Integer intervaloDias = 1;

        @Column(length = 300)
        private String notas;

        @Enumerated(EnumType.STRING)
        private EstadoProgramado estado = EstadoProgramado.activo;

        @Column(name = "creado_en")
        private LocalDateTime creadoEn;

        @PrePersist
        protected void onCreate() {
            if (creadoEn == null) creadoEn = LocalDateTime.now();
        }

        public enum EstadoProgramado {
            activo, completado, cancelado
        }

        /** Umbral de unidades a partir del cual se sugiere programar el pedido */
        public static final int UMBRAL_PEDIDO_GRANDE = 50;
    }


