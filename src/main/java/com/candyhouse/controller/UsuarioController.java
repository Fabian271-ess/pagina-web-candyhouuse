package com.candyhouse.controller;

import com.candyhouse.model.*;
import com.candyhouse.repository.*;
import com.candyhouse.service.CorreoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class UsuarioController {

    @Autowired private ProductoRepository productoRepo;
    @Autowired private CarritoRepository carritoRepo;
    @Autowired private PedidoRepository pedidoRepo;
    @Autowired private DetallePedidoRepository detalleRepo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private FacturaRepository facturaRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private CorreoService correoService;

    // =========================================================
    // PANEL USUARIO
    // =========================================================
    @GetMapping("/panel_usuario")
    public String panelUsuario(HttpSession session) {
        if (!esUsuario(session)) return "redirect:/";
        Long idUsuario = (Long) session.getAttribute("id_usuario");
        Optional<Cliente> cliente = clienteRepo.findByIdUsuario(idUsuario);
        return cliente.isPresent() ? "panel_usuario" : "redirect:/datos_cliente";
    }

    // =========================================================
    // DATOS CLIENTE
    // =========================================================
    @GetMapping("/datos_cliente")
    public String datosClienteForm(HttpSession session) {
        if (!esUsuario(session)) return "redirect:/";
        return "datos_cliente";
    }

    @PostMapping("/guardar_cliente")
    public String guardarCliente(@RequestParam String tipo_doc,
                                 @RequestParam Long cliente_doc,
                                 @RequestParam String nombre_cli,
                                 @RequestParam String apellido_cli,
                                 @RequestParam Long telefono,
                                 HttpSession session,
                                 Model model) {
        if (!esUsuario(session)) return "redirect:/";

        if (clienteRepo.findByClienteDoc(cliente_doc).isPresent()) {
            model.addAttribute("mensaje", "Ese número de documento ya está registrado");
            return "datos_cliente";
        }

        String telefonoStr = telefono.toString();
        if (telefonoStr.length() != 10) {
            model.addAttribute("mensaje", "El teléfono debe tener exactamente 10 dígitos");
            return "datos_cliente";
        }

        if (clienteRepo.findByTelefono(telefono).isPresent()) {
            model.addAttribute("mensaje", "Ese número de teléfono ya está registrado");
            return "datos_cliente";
        }

        Long idUsuario = (Long) session.getAttribute("id_usuario");
        Cliente c = new Cliente();
        c.setClienteDoc(cliente_doc);
        c.setTipoDoc(tipo_doc);
        c.setNombreCli(nombre_cli);
        c.setApellidoCli(apellido_cli);
        c.setTelefono(telefono);
        c.setIdUsuario(idUsuario);
        clienteRepo.save(c);
        return "redirect:/panel_usuario";
    }

    // =========================================================
    // PRODUCTOS
    // =========================================================
    @GetMapping("/ver_productos_usuario")
    public String verProductosUsuario(HttpSession session, Model model,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "") String buscar) {
        if (!esUsuario(session)) return "redirect:/";
        Page<Producto> paginado = buscar.trim().isEmpty()
                ? productoRepo.findAll(PageRequest.of(page, 8, Sort.by("productoCod").descending()))
                : productoRepo.buscar(buscar.trim(), PageRequest.of(page, 8, Sort.by("productoCod").descending()));
        model.addAttribute("productos",    paginado.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", paginado.getTotalPages());
        model.addAttribute("buscar",       buscar);
        return "productos_usuario";
    }

    // =========================================================
    // CARRITO
    // =========================================================
    @GetMapping("/carrito")
    public String verCarrito(HttpSession session, Model model) {
        if (!esUsuario(session)) return "redirect:/";
        Long idUsuario = (Long) session.getAttribute("id_usuario");
        List<Carrito> items = carritoRepo.findByIdUsuario(idUsuario);

        List<CarritoItem> carritoItems = items.stream().map(item -> {
            Producto p = productoRepo.findById(item.getProductoCod()).orElse(null);
            if (p == null) return null;
            return new CarritoItem(item.getProductoCod(), p.getNombrePro(), p.getPrecio(), item.getCantidad());
        }).filter(i -> i != null).toList();

        model.addAttribute("productos", carritoItems);
        return "carrito";
    }

    @PostMapping("/carrito/agregar")
    public String agregarCarritoCantidad(@RequestParam Long productoCod,
                                         @RequestParam Integer cantidad,
                                         HttpSession session) {
        if (!esUsuario(session)) return "redirect:/";
        Long idUsuario = (Long) session.getAttribute("id_usuario");

        Producto prod = productoRepo.findById(productoCod).orElse(null);
        if (prod == null) return "redirect:/ver_productos_usuario";

        int cantidadEnCarrito = carritoRepo
                .findByIdUsuarioAndProductoCod(idUsuario, productoCod)
                .map(Carrito::getCantidad)
                .orElse(0);

        if (cantidadEnCarrito + cantidad > prod.getExistenciaProd()) {
            return "redirect:/ver_productos_usuario?errorStock=" + prod.getNombrePro();
        }

        Optional<Carrito> existe = carritoRepo.findByIdUsuarioAndProductoCod(idUsuario, productoCod);
        if (existe.isPresent()) {
            Carrito item = existe.get();
            item.setCantidad(item.getCantidad() + cantidad);
            carritoRepo.save(item);
        } else {
            Carrito item = new Carrito();
            item.setIdUsuario(idUsuario);
            item.setProductoCod(productoCod);
            item.setCantidad(cantidad);
            carritoRepo.save(item);
        }

        return "redirect:/ver_productos_usuario";
    }

    @Transactional
    @GetMapping("/eliminar_carrito/{idProducto}")
    public String eliminarCarrito(@PathVariable Long idProducto, HttpSession session) {
        if (!esUsuario(session)) return "redirect:/";
        Long idUsuario = (Long) session.getAttribute("id_usuario");
        carritoRepo.deleteByIdUsuarioAndProductoCod(idUsuario, idProducto);
        return "redirect:/carrito";
    }

    // =========================================================
    // FINALIZAR COMPRA — con validacion de stock
    // =========================================================
    @Transactional
    @PostMapping("/finalizar_compra")
    public String finalizarCompra(
            @RequestParam(value = "productos_seleccionados", required = false) List<Long> productosSeleccionados,
            @RequestParam String fecha_entrega,
            @RequestParam String direccion,
            @RequestParam String ciudad,
            @RequestParam String telefono,
            @RequestParam Map<String, String> allParams,
            HttpSession session) {

        if (!esUsuario(session)) return "redirect:/";
        if (productosSeleccionados == null || productosSeleccionados.isEmpty()) return "redirect:/carrito";

        LocalDate fechaEntrega = LocalDate.parse(fecha_entrega);
        if (fechaEntrega.isBefore(LocalDate.now())) {
            return "redirect:/carrito?errorFecha=1";
        }

        Long idUsuario = (Long) session.getAttribute("id_usuario");

        // 1. Validar stock de todos los productos antes de crear el pedido
        for (Long idProducto : productosSeleccionados) {
            Optional<Carrito> itemCarrito = carritoRepo.findByIdUsuarioAndProductoCod(idUsuario, idProducto);
            if (itemCarrito.isPresent()) {
                Producto prod = productoRepo.findById(idProducto).orElse(null);
                if (prod == null) continue;

                if (prod.getExistenciaProd() < itemCarrito.get().getCantidad()) {
                    return "redirect:/carrito?error=" + prod.getNombrePro();
                }
            }
        }

        // 2. Crear pedido
        Pedido pedido = new Pedido();
        pedido.setIdUsuario(idUsuario);
        pedido.setFechaEntrega(fechaEntrega);
        pedido.setDireccionEnvio(direccion);
        pedido.setCiudad(ciudad);
        pedido.setTelefonoContacto(telefono);
        pedido.setEstado(Pedido.EstadoPedido.pendiente);
        Pedido pedidoGuardado = pedidoRepo.save(pedido);
        Long idPedido = pedidoGuardado.getIdPedido();

        BigDecimal total = BigDecimal.ZERO;

        // 3. Procesar cada producto seleccionado
        for (Long idProducto : productosSeleccionados) {
            Optional<Carrito> itemCarrito = carritoRepo.findByIdUsuarioAndProductoCod(idUsuario, idProducto);

            if (itemCarrito.isPresent()) {
                Carrito item = itemCarrito.get();
                Producto prod = productoRepo.findById(idProducto).orElse(null);
                if (prod == null) continue;

                BigDecimal precio   = BigDecimal.valueOf(prod.getPrecio());
                Integer cantidad    = item.getCantidad();
                BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
                total = total.add(subtotal);

                String nota = allParams.get("nota_" + idProducto);
                if (nota != null && nota.isBlank()) nota = null;

                DetallePedido detalle = new DetallePedido();
                detalle.setCantidad(cantidad);
                detalle.setPrecio(precio);
                detalle.setProductoCod(idProducto);
                detalle.setIdPedido(idPedido);
                detalle.setNota(nota);
                detalleRepo.save(detalle);

                prod.setExistenciaProd(prod.getExistenciaProd() - cantidad);
                productoRepo.save(prod);

                carritoRepo.deleteByIdUsuarioAndProductoCod(idUsuario, idProducto);
            }
        }

        // 4. Crear factura
        Optional<Cliente> cliente = clienteRepo.findByIdUsuario(idUsuario);
        if (cliente.isPresent()) {
            Factura factura = new Factura();
            factura.setPagoTotal(total);
            factura.setFechaFactura(LocalDate.now());
            factura.setDescripcion("Compra realizada");
            factura.setClienteDoc(cliente.get().getClienteDoc());
            factura.setIdPedido(idPedido);
            facturaRepo.save(factura);
        }

        // 5. Enviar correo de confirmacion
        try {
            Optional<Usuario> usuario = usuarioRepo.findById(idUsuario);
            if (usuario.isPresent() && usuario.get().getCorreo() != null) {
                String nombreCliente = cliente.isPresent()
                        ? cliente.get().getNombreCli() + " " + cliente.get().getApellidoCli()
                        : usuario.get().getNombre();
                correoService.enviarConfirmacionPedido(
                        usuario.get().getCorreo(), nombreCliente, pedidoGuardado, total);
            }
        } catch (Exception e) {
            System.err.println("Error enviando correo: " + e.getMessage());
        }

        return "redirect:/mis_pedidos";
    }

    // =========================================================
    // EDITAR DATOS CLIENTE
    // =========================================================
    @GetMapping("/editar_cliente")
    public String editarClienteForm(HttpSession session, Model model) {
        if (!esUsuario(session)) return "redirect:/";
        Long idUsuario = (Long) session.getAttribute("id_usuario");
        Optional<Cliente> cliente = clienteRepo.findByIdUsuario(idUsuario);
        if (cliente.isEmpty()) return "redirect:/datos_cliente";
        model.addAttribute("cliente", cliente.get());
        model.addAttribute("mensaje", "");
        return "editar_cliente";
    }

    @PostMapping("/editar_cliente")
    public String editarClientePost(@RequestParam String tipo_doc,
                                    @RequestParam String nombre_cli,
                                    @RequestParam String apellido_cli,
                                    @RequestParam Long telefono,
                                    HttpSession session, Model model) {
        if (!esUsuario(session)) return "redirect:/";
        Long idUsuario = (Long) session.getAttribute("id_usuario");
        Optional<Cliente> clienteOpt = clienteRepo.findByIdUsuario(idUsuario);
        if (clienteOpt.isEmpty()) return "redirect:/datos_cliente";

        Cliente c = clienteOpt.get();
        c.setTipoDoc(tipo_doc);
        c.setNombreCli(nombre_cli);
        c.setApellidoCli(apellido_cli);
        c.setTelefono(telefono);
        clienteRepo.save(c);
        return "redirect:/panel_usuario";
    }

    // =========================================================
    // MIS PEDIDOS
    // =========================================================
    @GetMapping("/mis_pedidos")
    public String misPedidos(HttpSession session, Model model,
                             @RequestParam(defaultValue = "0") int page) {
        if (!esUsuario(session)) return "redirect:/";
        Long idUsuario = (Long) session.getAttribute("id_usuario");
        Page<Pedido> paginado = pedidoRepo.findByIdUsuario(
                idUsuario, PageRequest.of(page, 5, Sort.by("idPedido").descending()));
        model.addAttribute("pedidos",      paginado.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", paginado.getTotalPages());
        return "mis_pedidos";
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private boolean esUsuario(HttpSession s) {
        return "usuario".equals(s.getAttribute("tipo"));
    }

    public static class CarritoItem {
        private final Long productoCod;
        private final String nombre;
        private final Integer precio;
        private final Integer cantidad;

        public CarritoItem(Long cod, String n, Integer p, Integer c) {
            this.productoCod = cod; this.nombre = n; this.precio = p; this.cantidad = c;
        }
        public Long    getProductoCod() { return productoCod; }
        public String  getNombre()      { return nombre; }
        public Integer getPrecio()      { return precio; }
        public Integer getCantidad()    { return cantidad; }
        public Integer getSubtotal()    { return precio * cantidad; }
    }
}