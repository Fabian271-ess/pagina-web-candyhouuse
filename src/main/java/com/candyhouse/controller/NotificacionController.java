package com.candyhouse.controller;

import com.candyhouse.model.Insumo;
import com.candyhouse.model.Pedido;
import com.candyhouse.model.Producto;
import com.candyhouse.repository.InsumoRepository;
import com.candyhouse.repository.PedidoRepository;
import com.candyhouse.repository.ProductoRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class NotificacionController {

    @Autowired
    private ProductoRepository productoRepo;

    @Autowired
    private InsumoRepository insumoRepo;

    @Autowired
    private PedidoRepository pedidoRepo;

    private static final int UMBRAL_STOCK = 5;

    @GetMapping("/api/notificaciones")
    public Map<String, Object> obtenerNotificaciones(HttpSession session) {

        // Validar que sea admin
        if (!"admin".equals(session.getAttribute("tipo"))) {
            return Map.of("error", "No autorizado");
        }

        List<Map<String, Object>> alertas = new ArrayList<>();

        // 🔹 Revisar productos con bajo stock
        for (Producto p : productoRepo.findAll()) {
            if (p.getExistenciaProd() != null &&
                    p.getExistenciaProd() <= UMBRAL_STOCK) {

                alertas.add(Map.of(
                        "tipo", "producto",
                        "mensaje", "Producto \"" + p.getNombrePro()
                                + "\" tiene solo "
                                + p.getExistenciaProd()
                                + " unidades en stock",
                        "link", "/editar_producto/" + p.getProductoCod()
                ));
            }
        }

        // 🔹 Revisar insumos con bajo stock
        for (Insumo i : insumoRepo.findAll()) {
            if (i.getExistenciaIns() != null &&
                    i.getExistenciaIns() <= UMBRAL_STOCK) {

                alertas.add(Map.of(
                        "tipo", "insumo",
                        "mensaje", "Insumo \"" + i.getNombreIns()
                                + "\" tiene solo "
                                + i.getExistenciaIns()
                                + " unidades en stock",
                        "link", "/editar_insumo/" + i.getInsumoCod()
                ));
            }
        }

        // 🔹 Revisar pedidos pendientes
        for (Pedido p : pedidoRepo.findAllByOrderByIdPedidoDesc()) {
            if (p.getEstado() == Pedido.EstadoPedido.pendiente) {

                alertas.add(Map.of(
                        "tipo", "pedido",
                        "mensaje", "Pedido #"
                                + p.getIdPedido()
                                + " está pendiente de entrega",
                        "link", "/detalle_pedido_admin/"
                                + p.getIdPedido()
                ));
            }
        }

        // 🔹 Retornar respuesta en formato JSON
        return Map.of(
                "total", alertas.size(),
                "alertas", alertas
        );
    }
}