package com.candyhouse.repository;

import com.candyhouse.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas para la generación de informes.
 * Usamos las entidades existentes (Pedido y Factura) para evitar duplicar datos.
 */
@Repository
public interface InformeRepository extends JpaRepository<Pedido, Long> {

    /** Pedidos entregados en un rango de fechas */
    @Query("SELECT p FROM Pedido p WHERE p.estado = 'entregado' " +
           "AND p.fechaEntrega BETWEEN :inicio AND :fin " +
           "ORDER BY p.fechaEntrega DESC")
    List<Pedido> pedidosEntregadosEnRango(
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin);

    /** Total vendido en un rango (suma de facturas) */
    @Query("SELECT COALESCE(SUM(f.pagoTotal), 0) FROM Factura f " +
           "JOIN Pedido p ON f.idPedido = p.idPedido " +
           "WHERE p.estado = 'entregado' " +
           "AND p.fechaEntrega BETWEEN :inicio AND :fin")
    BigDecimal totalVentasEnRango(
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin);

    /** Cantidad de pedidos entregados en el rango */
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estado = 'entregado' " +
           "AND p.fechaEntrega BETWEEN :inicio AND :fin")
    Long contarPedidosEntregados(
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin);

    /** Cantidad de pedidos cancelados en el rango */
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estado = 'cancelado' " +
           "AND p.fechaEntrega BETWEEN :inicio AND :fin")
    Long contarPedidosCancelados(
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin);

    /** Producto más vendido en el rango */
    @Query("SELECT d.productoCod, SUM(d.cantidad) AS total " +
           "FROM DetallePedido d " +
           "JOIN Pedido p ON d.idPedido = p.idPedido " +
           "WHERE p.estado = 'entregado' " +
           "AND p.fechaEntrega BETWEEN :inicio AND :fin " +
           "GROUP BY d.productoCod ORDER BY total DESC")
    List<Object[]> productosMasVendidosEnRango(
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin);
}
