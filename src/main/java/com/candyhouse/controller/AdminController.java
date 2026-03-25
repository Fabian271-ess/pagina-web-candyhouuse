package com.candyhouse.controller;

import com.candyhouse.model.*;
import com.candyhouse.repository.*;
import com.candyhouse.service.CorreoService;
import com.candyhouse.service.PdfService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private PedidoRepository pedidoRepo;
    @Autowired private DetallePedidoRepository detalleRepo;
    @Autowired private InsumoRepository insumoRepo;
    @Autowired private FacturaRepository facturaRepo;
    @Autowired private CorreoService correoService;
    @Autowired private PdfService pdfService;

    private static final int PAGE_SIZE = 5;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        return "dashboard";
    }

    // =========================================================
    // PRODUCTOS — paginado
    // =========================================================
    @GetMapping("/productos")
    public String productos(HttpSession session, Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "") String buscar) {
        if (!esAdmin(session)) return "redirect:/";
        Page<Producto> paginado = buscar.trim().isEmpty()
                ? productoRepo.findAll(PageRequest.of(page, 8, Sort.by("productoCod").descending()))
                : productoRepo.buscar(buscar.trim(), PageRequest.of(page, 8, Sort.by("productoCod").descending()));
        model.addAttribute("productos",    paginado.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", paginado.getTotalPages());
        model.addAttribute("buscar",       buscar);
        return "productos";
    }

    @GetMapping("/add_producto")
    public String addProductoForm(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/";
        model.addAttribute("mensaje", "");
        return "add_producto";
    }

    @PostMapping("/add_producto")
    public String addProductoPost(@RequestParam String nombre, @RequestParam String categoria,
                                  @RequestParam Integer precio, @RequestParam Integer existencia,
                                  @RequestParam(required = false) String descripcion,
                                  @RequestParam(required = false) String imagenUrl,
                                  HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Producto p = new Producto();
        p.setNombrePro(nombre); p.setCategoria(categoria); p.setPrecio(precio);
        p.setExistenciaProd(existencia); p.setDescripcion(descripcion); p.setImagenUrl(imagenUrl);
        productoRepo.save(p);
        return "redirect:/productos";
    }

    @GetMapping("/editar_producto/{id}")
    public String editarProductoForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/";
        model.addAttribute("producto", productoRepo.findById(id).orElseThrow());
        return "editar_producto";
    }

    @PostMapping("/editar_producto/{id}")
    public String editarProductoPost(@PathVariable Long id, @RequestParam String nombre,
                                     @RequestParam String categoria, @RequestParam Integer precio,
                                     @RequestParam Integer existencia,
                                     @RequestParam(required = false) String descripcion,
                                     @RequestParam(required = false) String imagenUrl,
                                     HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Producto p = productoRepo.findById(id).orElseThrow();
        p.setNombrePro(nombre); p.setCategoria(categoria); p.setPrecio(precio);
        p.setExistenciaProd(existencia); p.setDescripcion(descripcion); p.setImagenUrl(imagenUrl);
        productoRepo.save(p);
        return "redirect:/productos";
    }

    @PostMapping("/eliminar_producto/{id}")
    public String eliminarProducto(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        try {
            productoRepo.deleteById(id);
        } catch (Exception e) {
            return "redirect:/productos?errorEliminar=1";
        }
        return "redirect:/productos";
    }

    // =========================================================
    // USUARIOS — paginado
    // =========================================================
    @GetMapping("/usuarios")
    public String usuarios(HttpSession session, Model model,
                           @RequestParam(defaultValue = "0") int page) {
        if (!esAdmin(session)) return "redirect:/";
        Page<Usuario> paginado = usuarioRepo.findAll(
                PageRequest.of(page, PAGE_SIZE, Sort.by("idUsuario").descending()));
        model.addAttribute("usuarios",     paginado.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", paginado.getTotalPages());
        return "usuarios";
    }

    @GetMapping("/editar_usuario/{nombre}")
    public String editarUsuarioForm(@PathVariable String nombre, HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/";
        model.addAttribute("nombre", nombre);
        model.addAttribute("mensaje", "");
        return "editar_usuario";
    }

    @PostMapping("/editar_usuario/{nombre}")
    public String editarUsuarioPost(@PathVariable String nombre, @RequestParam("new") String nuevaPass,
                                    @RequestParam String confirm, HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/";
        if (!nuevaPass.equals(confirm)) {
            model.addAttribute("nombre", nombre); model.addAttribute("mensaje", "No coinciden");
            return "editar_usuario";
        }
        if (nuevaPass.length() < 8) {
            model.addAttribute("nombre", nombre); model.addAttribute("mensaje", "Minimo 8 caracteres");
            return "editar_usuario";
        }
        usuarioRepo.findByNombre(nombre).ifPresent(u -> {
            u.setContrasena(nuevaPass);
            usuarioRepo.save(u);
        });
        return "redirect:/usuarios";
    }

    // =========================================================
    // CLIENTES — paginado
    // =========================================================
    @GetMapping("/clientes")
    public String clientes(HttpSession session, Model model,
                           @RequestParam(defaultValue = "0") int page) {
        if (!esAdmin(session)) return "redirect:/";
        Page<Cliente> paginado = clienteRepo.findAll(
                PageRequest.of(page, PAGE_SIZE, Sort.by("clienteDoc").descending()));
        model.addAttribute("clientes",     paginado.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", paginado.getTotalPages());
        return "clientes";
    }

    // =========================================================
    // PEDIDOS ADMIN — paginado + búsqueda por nombre
    // =========================================================
    @GetMapping("/ver_pedidos")
    public String verPedidos(HttpSession session, Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "") String buscar) {
        if (!esAdmin(session)) return "redirect:/";

        Page<Pedido> paginado;

        if (!buscar.trim().isEmpty()) {
            // Buscar usuarios cuyo nombre contenga el texto
            List<Long> ids = usuarioRepo.findAll().stream()
                    .filter(u -> u.getNombre().toLowerCase().contains(buscar.toLowerCase()))
                    .map(Usuario::getIdUsuario)
                    .collect(Collectors.toList());
            if (ids.isEmpty()) {
                paginado = Page.empty();
            } else {
                paginado = pedidoRepo.findByIdUsuarioIn(ids,
                        PageRequest.of(page, PAGE_SIZE, Sort.by("idPedido").descending()));
            }
        } else {
            paginado = pedidoRepo.findAll(
                    PageRequest.of(page, PAGE_SIZE, Sort.by("idPedido").descending()));
        }

        List<PedidoViewModel> pedidosVM = paginado.getContent().stream().map(p -> {
            String nombreUsuario = usuarioRepo.findById(p.getIdUsuario())
                    .map(Usuario::getNombre).orElse("Desconocido");
            return new PedidoViewModel(p, nombreUsuario);
        }).collect(Collectors.toList());

        model.addAttribute("pedidos",      pedidosVM);
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", paginado.getTotalPages());
        model.addAttribute("buscar",       buscar);
        return "pedidos_admin";
    }

    // =========================================================
    // DETALLE PEDIDO
    // =========================================================
    @GetMapping("/detalle_pedido_admin/{idPedido}")
    public String detallePedidoAdmin(@PathVariable Long idPedido, HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/";
        List<DetallePedido> detalles = detalleRepo.findByIdPedido(idPedido);
        List<DetalleViewModel> detallesVM = detalles.stream().map(d -> {
            String nombrePro = productoRepo.findById(d.getProductoCod())
                    .map(Producto::getNombrePro).orElse("?");
            BigDecimal subtotal = d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad()));
            return new DetalleViewModel(nombrePro, d.getCantidad(), d.getPrecio(), subtotal, d.getNota());
        }).collect(Collectors.toList());
        BigDecimal total = detallesVM.stream()
                .map(DetalleViewModel::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("detalles", detallesVM);
        model.addAttribute("total",    total);
        return "detalle_pedido_admin";
    }

    // =========================================================
    // CAMBIAR ESTADO PEDIDO
    // =========================================================
    @PostMapping("/cambiar_estado/{idPedido}")
    public String cambiarEstado(@PathVariable Long idPedido,
                                @RequestParam String estado,
                                HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Pedido pedido = pedidoRepo.findById(idPedido).orElseThrow();
        Pedido.EstadoPedido nuevoEstado = Pedido.EstadoPedido.valueOf(estado);
        pedido.setEstado(nuevoEstado);
        pedidoRepo.save(pedido);
        if (nuevoEstado == Pedido.EstadoPedido.entregado) {
            BigDecimal total = facturaRepo.findByIdPedido(idPedido)
                    .map(Factura::getPagoTotal).orElse(BigDecimal.ZERO);
            Optional<Usuario> usuario = usuarioRepo.findById(pedido.getIdUsuario());
            if (usuario.isPresent() && usuario.get().getCorreo() != null) {
                Optional<Cliente> cliente = clienteRepo.findByIdUsuario(pedido.getIdUsuario());
                String nombreCliente = cliente.isPresent()
                        ? cliente.get().getNombreCli() + " " + cliente.get().getApellidoCli()
                        : usuario.get().getNombre();
                byte[] pdfBytes = null;
                try { pdfBytes = pdfService.generarFacturaPdf(pedido, pedido.getIdUsuario()); }
                catch (Exception e) { System.err.println("Error PDF: " + e.getMessage()); }
                try { correoService.enviarPedidoEntregado(usuario.get().getCorreo(), nombreCliente, pedido, total, pdfBytes); }
                catch (Exception e) { System.err.println("Error correo: " + e.getMessage()); }
            }
        }

        if (nuevoEstado == Pedido.EstadoPedido.cancelado) {
            BigDecimal total = facturaRepo.findByIdPedido(idPedido)
                    .map(Factura::getPagoTotal).orElse(BigDecimal.ZERO);
            Optional<Usuario> usuario = usuarioRepo.findById(pedido.getIdUsuario());
            if (usuario.isPresent() && usuario.get().getCorreo() != null) {
                Optional<Cliente> cliente = clienteRepo.findByIdUsuario(pedido.getIdUsuario());
                String nombreCliente = cliente.isPresent()
                        ? cliente.get().getNombreCli() + " " + cliente.get().getApellidoCli()
                        : usuario.get().getNombre();
                try { correoService.enviarPedidoCancelado(usuario.get().getCorreo(), nombreCliente, pedido, total); }
                catch (Exception e) { System.err.println("Error correo cancelación: " + e.getMessage()); }
            }
        }
        return "redirect:/ver_pedidos";
    }

    // =========================================================
    // INSUMOS — paginado
    // =========================================================
    @GetMapping("/insumos")
    public String insumos(HttpSession session, Model model,
                          @RequestParam(defaultValue = "0") int page) {
        if (!esAdmin(session)) return "redirect:/";
        Page<Insumo> paginado = insumoRepo.findAll(
                PageRequest.of(page, PAGE_SIZE, Sort.by("insumoCod").descending()));
        model.addAttribute("insumos",      paginado.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", paginado.getTotalPages());
        return "insumos";
    }

    @GetMapping("/add_insumo")
    public String addInsumoForm(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/";
        model.addAttribute("mensaje", "");
        return "add_insumo";
    }

    @PostMapping("/add_insumo")
    public String addInsumoPost(@RequestParam String nombre, @RequestParam String categoria,
                                @RequestParam String marca, @RequestParam BigDecimal costo,
                                @RequestParam Integer existencia,
                                @RequestParam(required = false) String descripcion,
                                HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Insumo i = new Insumo();
        i.setNombreIns(nombre); i.setCategoria(categoria); i.setMarca(marca);
        i.setCosto(costo); i.setExistenciaIns(existencia);
        i.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion : null);
        insumoRepo.save(i);
        return "redirect:/insumos";
    }

    @GetMapping("/editar_insumo/{id}")
    public String editarInsumoForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/";
        model.addAttribute("insumo", insumoRepo.findById(id).orElseThrow());
        model.addAttribute("mensaje", "");
        return "editar_insumo";
    }

    @PostMapping("/editar_insumo/{id}")
    public String editarInsumoPost(@PathVariable Long id, @RequestParam String nombre,
                                   @RequestParam String categoria, @RequestParam String marca,
                                   @RequestParam BigDecimal costo, @RequestParam Integer existencia,
                                   @RequestParam(required = false) String descripcion,
                                   HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/";
        Insumo i = insumoRepo.findById(id).orElseThrow();
        i.setNombreIns(nombre); i.setCategoria(categoria); i.setMarca(marca);
        i.setCosto(costo); i.setExistenciaIns(existencia);
        i.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion : null);
        insumoRepo.save(i);
        return "redirect:/insumos";
    }

    @PostMapping("/eliminar_insumo/{id}")
    public String eliminarInsumo(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        try {
            insumoRepo.deleteById(id);
        } catch (Exception e) {
            return "redirect:/insumos?errorEliminar=1";
        }
        return "redirect:/insumos";
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private boolean esAdmin(HttpSession s) {
        return "admin".equals(s.getAttribute("tipo"));
    }

    public static class PedidoViewModel {
        private final Pedido pedido;
        private final String nombreUsuario;
        public PedidoViewModel(Pedido p, String n) {
            this.pedido = p;
            this.nombreUsuario = n; }
        public Pedido getPedido()        {
            return pedido; }
        public String getNombreUsuario() {
            return nombreUsuario; }
    }

    public static class DetalleViewModel {
        private final String nombreProducto;
        private final Integer cantidad;
        private final BigDecimal precioUnit;
        private final BigDecimal subtotal;
        private final String nota;
        public DetalleViewModel(String n, Integer c, BigDecimal p, BigDecimal s, String nota) {
            this.nombreProducto = n;
            this.cantidad = c;
            this.precioUnit = p;
            this.subtotal = s;
            this.nota = nota;
        }
        public String     getNombreProducto() {
            return nombreProducto; }
        public Integer    getCantidad()        {
            return cantidad; }
        public BigDecimal getPrecioUnit()      {
            return precioUnit; }
        public BigDecimal getSubtotal()        {
            return subtotal; }
        public String     getNota()            {
            return nota; }
    }
}