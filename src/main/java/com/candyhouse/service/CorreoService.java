package com.candyhouse.service;

import com.candyhouse.model.DetallePedido;
import com.candyhouse.model.Pedido;
import com.candyhouse.model.Producto;
import com.candyhouse.repository.DetallePedidoRepository;
import com.candyhouse.repository.ProductoRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class CorreoService {

    @Autowired private JavaMailSender mailSender;
    @Autowired private DetallePedidoRepository detalleRepo;
    @Autowired private ProductoRepository productoRepo;

    @Value("${spring.mail.username}")
    private String remitente;

    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "CO"));

    // =========================================================
    // RECUPERACIÓN DE CONTRASEÑA
    // =========================================================
    public void enviarEnlaceRecuperacion(String correoDestino, String link) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(correoDestino);
        mensaje.setSubject("Recuperar Contraseña - Candy House");
        mensaje.setText(
                "Hola, recibiste este correo porque solicitaste restablecer tu contraseña.\n\n" +
                        "Haz clic en el siguiente enlace (válido por 30 minutos):\n\n" +
                        link + "\n\n" +
                        "Si no fuiste tú, ignora este mensaje."
        );
        mailSender.send(mensaje);
    }

    // =========================================================
    // CONFIRMACIÓN DE PEDIDO — correo HTML elegante
    // =========================================================
    public void enviarConfirmacionPedido(String correoDestino, String nombreCliente,
                                         Pedido pedido, BigDecimal total) {
        try {
            List<DetallePedido> detalles = detalleRepo.findByIdPedido(pedido.getIdPedido());

            StringBuilder filasProductos = new StringBuilder();
            for (DetallePedido d : detalles) {
                String nombrePro = productoRepo.findById(d.getProductoCod())
                        .map(Producto::getNombrePro).orElse("Producto");
                BigDecimal subtotal = d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad()));
                String nota = (d.getNota() != null && !d.getNota().isBlank())
                        ? "<div style='font-size:11px;color:#b07080;margin-top:3px;font-style:italic;'>» " + d.getNota() + "</div>"
                        : "";
                filasProductos.append(
                        "<tr>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;'>" +
                                "    <span style='font-weight:600;color:#5a3a33;'>" + nombrePro + "</span>" + nota +
                                "  </td>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;text-align:center;color:#7a5a55;'>" + d.getCantidad() + "</td>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;text-align:right;color:#7a5a55;'>$" + String.format("%,.0f", d.getPrecio()) + "</td>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;text-align:right;font-weight:600;color:#5a3a33;'>$" + String.format("%,.0f", subtotal) + "</td>" +
                                "</tr>"
                );
            }

            String fechaPedido   = pedido.getFechaPedido().toLocalDate().format(FMT_FECHA);
            String fechaEntrega  = pedido.getFechaEntrega().format(FMT_FECHA);

            String html = buildHtmlConfirmacion(
                    nombreCliente, pedido.getIdPedido(),
                    fechaPedido, fechaEntrega,
                    pedido.getDireccionEnvio() + ", " + pedido.getCiudad(),
                    pedido.getTelefonoContacto(),
                    filasProductos.toString(),
                    total
            );

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(correoDestino);
            helper.setSubject("🍬 Pedido #" + pedido.getIdPedido() + " confirmado — Candy House");
            helper.setText(html, true);

            // Adjuntar logo como recurso inline (CID)
            try {
                ClassPathResource logo = new ClassPathResource("static/imagenes/logo_candy_house.png");
                if (logo.exists()) {
                    helper.addInline("logoCandyHouse", logo);
                }
            } catch (Exception ignored) { }

            mailSender.send(mime);

        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando confirmación de pedido: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // PEDIDO ENTREGADO — correo HTML + PDF adjunto
    // =========================================================
    public void enviarPedidoEntregado(String correoDestino, String nombreCliente,
                                      Pedido pedido, BigDecimal total,
                                      byte[] pdfBytes) {
        try {
            List<DetallePedido> detalles = detalleRepo.findByIdPedido(pedido.getIdPedido());

            StringBuilder filasProductos = new StringBuilder();
            for (DetallePedido d : detalles) {
                String nombrePro = productoRepo.findById(d.getProductoCod())
                        .map(Producto::getNombrePro).orElse("Producto");
                BigDecimal subtotal = d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad()));
                String nota = (d.getNota() != null && !d.getNota().isBlank())
                        ? "<div style='font-size:11px;color:#b07080;margin-top:3px;font-style:italic;'>» " + d.getNota() + "</div>"
                        : "";
                filasProductos.append(
                        "<tr>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;'>" +
                                "    <span style='font-weight:600;color:#5a3a33;'>" + nombrePro + "</span>" + nota +
                                "  </td>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;text-align:center;color:#7a5a55;'>" + d.getCantidad() + "</td>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;text-align:right;color:#7a5a55;'>$" + String.format("%,.0f", d.getPrecio()) + "</td>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;text-align:right;font-weight:600;color:#5a3a33;'>$" + String.format("%,.0f", subtotal) + "</td>" +
                                "</tr>"
                );
            }

            String fechaEntrega = pedido.getFechaEntrega().format(FMT_FECHA);

            String html = buildHtmlEntregado(
                    nombreCliente, pedido.getIdPedido(),
                    fechaEntrega,
                    pedido.getDireccionEnvio() + ", " + pedido.getCiudad(),
                    filasProductos.toString(),
                    total
            );

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(correoDestino);
            helper.setSubject("✅ Tu pedido #" + pedido.getIdPedido() + " fue entregado — Candy House");
            helper.setText(html, true);

            if (pdfBytes != null && pdfBytes.length > 0) {
                ByteArrayDataSource ds = new ByteArrayDataSource(pdfBytes, "application/pdf");
                helper.addAttachment("factura_" + pedido.getIdPedido() + ".pdf", ds);
            }

            mailSender.send(mime);

        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando correo de entrega: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // PEDIDO CANCELADO — correo HTML
    // =========================================================
    public void enviarPedidoCancelado(String correoDestino, String nombreCliente,
                                      Pedido pedido, BigDecimal total) {
        try {
            List<DetallePedido> detalles = detalleRepo.findByIdPedido(pedido.getIdPedido());

            StringBuilder filasProductos = new StringBuilder();
            for (DetallePedido d : detalles) {
                String nombrePro = productoRepo.findById(d.getProductoCod())
                        .map(Producto::getNombrePro).orElse("Producto");
                BigDecimal subtotal = d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad()));
                filasProductos.append(
                        "<tr>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;'>" +
                                "    <span style='font-weight:600;color:#5a3a33;'>" + nombrePro + "</span>" +
                                "  </td>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;text-align:center;color:#7a5a55;'>" + d.getCantidad() + "</td>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;text-align:right;color:#7a5a55;'>$" + String.format("%,.0f", d.getPrecio()) + "</td>" +
                                "  <td style='padding:12px 16px;border-bottom:1px solid #fce4ec;text-align:right;font-weight:600;color:#5a3a33;'>$" + String.format("%,.0f", subtotal) + "</td>" +
                                "</tr>"
                );
            }

            String fechaPedido = pedido.getFechaPedido().toLocalDate().format(FMT_FECHA);

            String html = buildHtmlCancelado(
                    nombreCliente, pedido.getIdPedido(),
                    fechaPedido,
                    pedido.getDireccionEnvio() + ", " + pedido.getCiudad(),
                    filasProductos.toString(),
                    total
            );

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(correoDestino);
            helper.setSubject("❌ Tu pedido #" + pedido.getIdPedido() + " fue cancelado — Candy House");
            helper.setText(html, true);

            try {
                ClassPathResource logo = new ClassPathResource("static/imagenes/logo_candy_house.png");
                if (logo.exists()) helper.addInline("logoCandyHouse", logo);
            } catch (Exception ignored) { }

            mailSender.send(mime);

        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando correo de cancelación: " + e.getMessage(), e);
        }
    }

    private String buildHtmlCancelado(String nombre, Long idPedido,
                                      String fechaPedido, String direccion,
                                      String filasProductos, BigDecimal total) {
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<link href='https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap' rel='stylesheet'>" +
                "</head><body style='margin:0;padding:0;background:#fdf2f2;font-family:Poppins,sans-serif;'>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#fdf2f2;padding:32px 0;'><tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;border-radius:24px;overflow:hidden;box-shadow:0 8px 40px rgba(180,50,50,0.12);'>" +

                "<tr><td style='background:linear-gradient(135deg,#6b6b6b 0%,#4a4a4a 60%,#2d2d2d 100%);padding:48px 40px 40px;text-align:center;'>" +
                "  <div style='margin-bottom:16px;'>" +
                "    <img src='cid:logoCandyHouse' alt='Candy House' width='80' height='80' style='border-radius:50%;border:3px solid rgba(255,255,255,0.3);object-fit:cover;display:block;margin:0 auto;opacity:0.85;'>" +
                "  </div>" +
                "  <h1 style='margin:0 0 8px;color:#fff;font-size:26px;font-weight:700;letter-spacing:-0.5px;'>Pedido Cancelado</h1>" +
                "  <p style='margin:0;color:rgba(255,255,255,0.7);font-size:15px;'>Hola <strong style='color:#fff;'>" + nombre + "</strong>, tu pedido ha sido cancelado.</p>" +
                "</td></tr>" +

                "<tr><td style='background:#ffffff;padding:40px;'>" +

                "<div style='text-align:center;margin-bottom:32px;'>" +
                "  <span style='display:inline-block;background:#f5f5f5;border:2px solid #ccc;border-radius:50px;padding:10px 28px;font-size:14px;font-weight:700;color:#666;letter-spacing:1px;'>PEDIDO #" + idPedido + " · CANCELADO</span>" +
                "</div>" +

                "<div style='background:#fff8e6;border-left:4px solid #f0c040;border-radius:0 12px 12px 0;padding:16px 20px;margin-bottom:28px;'>" +
                "  <div style='font-size:13px;font-weight:700;color:#7a5a00;margin-bottom:4px;'>⚠️ ¿Por qué fue cancelado?</div>" +
                "  <div style='font-size:13px;color:#9a7a20;line-height:1.6;'>Tu pedido fue cancelado por el equipo de Candy House. Si crees que esto fue un error o necesitas más información, no dudes en contactarnos.</div>" +
                "</div>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:28px;border-radius:16px;overflow:hidden;border:1px solid #ebebeb;'>" +
                "  <tr style='background:#f9f9f9;'>" +
                "    <td style='padding:16px 20px;border-right:1px solid #ebebeb;'>" +
                "      <div style='font-size:11px;color:#999;font-weight:600;letter-spacing:1px;text-transform:uppercase;margin-bottom:4px;'>📅 Fecha del pedido</div>" +
                "      <div style='font-size:14px;color:#333;font-weight:500;'>" + fechaPedido + "</div>" +
                "    </td>" +
                "    <td style='padding:16px 20px;'>" +
                "      <div style='font-size:11px;color:#999;font-weight:600;letter-spacing:1px;text-transform:uppercase;margin-bottom:4px;'>📍 Dirección</div>" +
                "      <div style='font-size:14px;color:#333;font-weight:500;'>" + direccion + "</div>" +
                "    </td>" +
                "  </tr>" +
                "</table>" +

                "<h3 style='margin:0 0 14px;color:#444;font-size:15px;font-weight:700;'>🛍️ Productos del pedido cancelado</h3>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='border-radius:12px;overflow:hidden;border:1px solid #ebebeb;margin-bottom:28px;'>" +
                "  <thead><tr style='background:#666;'>" +
                "    <th style='padding:12px 16px;text-align:left;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>PRODUCTO</th>" +
                "    <th style='padding:12px 16px;text-align:center;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>CANT.</th>" +
                "    <th style='padding:12px 16px;text-align:right;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>PRECIO</th>" +
                "    <th style='padding:12px 16px;text-align:right;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>SUBTOTAL</th>" +
                "  </tr></thead>" +
                "  <tbody style='background:#fff;'>" + filasProductos + "</tbody>" +
                "  <tfoot><tr style='background:#4a4a4a;'>" +
                "    <td colspan='3' style='padding:14px 16px;color:#fff;font-weight:700;font-size:14px;'>TOTAL (no cobrado)</td>" +
                "    <td style='padding:14px 16px;text-align:right;color:#ccc;font-weight:700;font-size:16px;text-decoration:line-through;'>$" + String.format("%,.0f", total) + "</td>" +
                "  </tr></tfoot>" +
                "</table>" +

                "<p style='margin:0;font-size:14px;color:#888;line-height:1.7;'>Lamentamos los inconvenientes. Esperamos poder atenderte pronto en <strong style='color:#e35d7a;'>Candy House</strong>. 🍰</p>" +
                "</td></tr>" +

                "<tr><td style='background:linear-gradient(to right,#f48ca7,#e35d7a);padding:24px 40px;text-align:center;'>" +
                "  <p style='margin:0 0 6px;color:#fff;font-weight:700;font-size:15px;'>🍬 Delicias Candy House</p>" +
                "  <p style='margin:0;color:rgba(255,255,255,0.8);font-size:12px;'>© 2026 · Todos los derechos reservados</p>" +
                "</td></tr>" +

                "</table></td></tr></table></body></html>";
    }

    private String buildHtmlConfirmacion(String nombre, Long idPedido,
                                         String fechaPedido, String fechaEntrega,
                                         String direccion, String telefono,
                                         String filasProductos, BigDecimal total) {
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<link href='https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap' rel='stylesheet'>" +
                "</head><body style='margin:0;padding:0;background:#fff0f3;font-family:Poppins,sans-serif;'>" +

                // Wrapper
                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#fff0f3;padding:32px 0;'><tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;border-radius:24px;overflow:hidden;box-shadow:0 8px 40px rgba(227,93,122,0.15);'>" +

                // HEADER con gradiente
                "<tr><td style='background:linear-gradient(135deg,#f48ca7 0%,#e35d7a 60%,#c94568 100%);padding:48px 40px 40px;text-align:center;'>" +
                "  <div style='margin-bottom:16px;'>" +
                "    <img src='cid:logoCandyHouse' alt='Candy House' width='90' height='90' style='border-radius:50%;border:3px solid rgba(255,255,255,0.5);box-shadow:0 4px 20px rgba(0,0,0,0.2);object-fit:cover;display:block;margin:0 auto;'>" +
                "  </div>" +
                "  <h1 style='margin:0 0 8px;color:#fff;font-size:28px;font-weight:700;letter-spacing:-0.5px;'>¡Pedido Confirmado!</h1>" +
                "  <p style='margin:0;color:rgba(255,255,255,0.85);font-size:15px;'>Hola <strong>" + nombre + "</strong>, tu pedido está en camino 🎉</p>" +
                "</td></tr>" +

                // CUERPO
                "<tr><td style='background:#ffffff;padding:40px;'>" +

                // Badge número de pedido
                "<div style='text-align:center;margin-bottom:32px;'>" +
                "  <span style='display:inline-block;background:#fff0f3;border:2px solid #f48ca7;border-radius:50px;padding:10px 28px;font-size:14px;font-weight:700;color:#e35d7a;letter-spacing:1px;'>PEDIDO #" + idPedido + "</span>" +
                "</div>" +

                // Info del pedido en grid 2x2
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:32px;border-radius:16px;overflow:hidden;border:1px solid #fce4ec;'>" +
                "  <tr style='background:#fff8fa;'>" +
                "    <td style='padding:16px 20px;width:50%;border-right:1px solid #fce4ec;border-bottom:1px solid #fce4ec;'>" +
                "      <div style='font-size:11px;color:#c4808f;font-weight:600;letter-spacing:1px;text-transform:uppercase;margin-bottom:4px;'>📅 Fecha del pedido</div>" +
                "      <div style='font-size:14px;color:#5a3a33;font-weight:500;'>" + fechaPedido + "</div>" +
                "    </td>" +
                "    <td style='padding:16px 20px;border-bottom:1px solid #fce4ec;'>" +
                "      <div style='font-size:11px;color:#c4808f;font-weight:600;letter-spacing:1px;text-transform:uppercase;margin-bottom:4px;'>🚚 Fecha de entrega</div>" +
                "      <div style='font-size:14px;color:#5a3a33;font-weight:500;'>" + fechaEntrega + "</div>" +
                "    </td>" +
                "  </tr>" +
                "  <tr style='background:#fff8fa;'>" +
                "    <td style='padding:16px 20px;border-right:1px solid #fce4ec;'>" +
                "      <div style='font-size:11px;color:#c4808f;font-weight:600;letter-spacing:1px;text-transform:uppercase;margin-bottom:4px;'>📍 Dirección</div>" +
                "      <div style='font-size:14px;color:#5a3a33;font-weight:500;'>" + direccion + "</div>" +
                "    </td>" +
                "    <td style='padding:16px 20px;'>" +
                "      <div style='font-size:11px;color:#c4808f;font-weight:600;letter-spacing:1px;text-transform:uppercase;margin-bottom:4px;'>📞 Teléfono</div>" +
                "      <div style='font-size:14px;color:#5a3a33;font-weight:500;'>" + (telefono != null ? telefono : "—") + "</div>" +
                "    </td>" +
                "  </tr>" +
                "</table>" +

                // Tabla de productos
                "<h3 style='margin:0 0 14px;color:#5a3a33;font-size:16px;font-weight:700;'>🛍️ Productos de tu pedido</h3>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='border-radius:12px;overflow:hidden;border:1px solid #fce4ec;margin-bottom:24px;'>" +
                "  <thead><tr style='background:linear-gradient(to right,#f48ca7,#e35d7a);'>" +
                "    <th style='padding:12px 16px;text-align:left;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>PRODUCTO</th>" +
                "    <th style='padding:12px 16px;text-align:center;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>CANT.</th>" +
                "    <th style='padding:12px 16px;text-align:right;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>PRECIO</th>" +
                "    <th style='padding:12px 16px;text-align:right;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>SUBTOTAL</th>" +
                "  </tr></thead>" +
                "  <tbody style='background:#fff;'>" + filasProductos + "</tbody>" +
                "  <tfoot><tr style='background:linear-gradient(to right,#e35d7a,#c94568);'>" +
                "    <td colspan='3' style='padding:14px 16px;color:#fff;font-weight:700;font-size:14px;'>TOTAL</td>" +
                "    <td style='padding:14px 16px;text-align:right;color:#fff;font-weight:700;font-size:16px;'>$" + String.format("%,.0f", total) + "</td>" +
                "  </tr></tfoot>" +
                "</table>" +

                // Estado badge
                "<div style='background:#f0fff8;border:1px solid #a8e6c8;border-radius:12px;padding:16px 20px;margin-bottom:28px;display:flex;align-items:center;gap:12px;'>" +
                "  <span style='font-size:22px;'>⏳</span>" +
                "  <div><div style='font-size:13px;font-weight:700;color:#2d7a50;'>Estado actual: Pendiente</div>" +
                "  <div style='font-size:12px;color:#5a9a78;margin-top:2px;'>Recibirás tu factura en PDF cuando el pedido sea entregado.</div></div>" +
                "</div>" +

                // Mensaje cierre
                "<p style='margin:0;font-size:14px;color:#7a5a55;line-height:1.7;'>Si tienes alguna duda puedes contactarnos. ¡Gracias por elegir <strong style='color:#e35d7a;'>Candy House</strong>! 🍰</p>" +
                "</td></tr>" +

                // FOOTER
                "<tr><td style='background:linear-gradient(to right,#f48ca7,#e35d7a);padding:24px 40px;text-align:center;'>" +
                "  <p style='margin:0 0 6px;color:#fff;font-weight:700;font-size:15px;'>🍬 Delicias Candy House</p>" +
                "  <p style='margin:0;color:rgba(255,255,255,0.8);font-size:12px;'>© 2026 · Todos los derechos reservados</p>" +
                "</td></tr>" +

                "</table></td></tr></table></body></html>";
    }

    private String buildHtmlEntregado(String nombre, Long idPedido,
                                      String fechaEntrega, String direccion,
                                      String filasProductos, BigDecimal total) {
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<link href='https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap' rel='stylesheet'>" +
                "</head><body style='margin:0;padding:0;background:#f0fff8;font-family:Poppins,sans-serif;'>" +

                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0fff8;padding:32px 0;'><tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;border-radius:24px;overflow:hidden;box-shadow:0 8px 40px rgba(79,191,136,0.18);'>" +

                // HEADER verde éxito
                "<tr><td style='background:linear-gradient(135deg,#4fbf88 0%,#2d9e65 60%,#1a7a4a 100%);padding:48px 40px 40px;text-align:center;'>" +
                "  <div style='display:inline-block;background:rgba(255,255,255,0.2);border:2px solid rgba(255,255,255,0.4);border-radius:50%;width:80px;height:80px;line-height:80px;font-size:38px;margin-bottom:20px;'>✅</div>" +
                "  <h1 style='margin:0 0 8px;color:#fff;font-size:28px;font-weight:700;letter-spacing:-0.5px;'>¡Pedido Entregado!</h1>" +
                "  <p style='margin:0;color:rgba(255,255,255,0.85);font-size:15px;'>Hola <strong>" + nombre + "</strong>, tu pedido llegó con éxito 🎉</p>" +
                "</td></tr>" +

                "<tr><td style='background:#ffffff;padding:40px;'>" +

                "<div style='text-align:center;margin-bottom:32px;'>" +
                "  <span style='display:inline-block;background:#f0fff8;border:2px solid #4fbf88;border-radius:50px;padding:10px 28px;font-size:14px;font-weight:700;color:#2d9e65;letter-spacing:1px;'>PEDIDO #" + idPedido + " · ENTREGADO</span>" +
                "</div>" +

                // Info resumen
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:32px;border-radius:16px;overflow:hidden;border:1px solid #c8f0dc;'>" +
                "  <tr style='background:#f5fdf8;'>" +
                "    <td style='padding:16px 20px;border-right:1px solid #c8f0dc;'>" +
                "      <div style='font-size:11px;color:#5a9a78;font-weight:600;letter-spacing:1px;text-transform:uppercase;margin-bottom:4px;'>📅 Fecha de entrega</div>" +
                "      <div style='font-size:14px;color:#2d4a38;font-weight:500;'>" + fechaEntrega + "</div>" +
                "    </td>" +
                "    <td style='padding:16px 20px;'>" +
                "      <div style='font-size:11px;color:#5a9a78;font-weight:600;letter-spacing:1px;text-transform:uppercase;margin-bottom:4px;'>📍 Dirección</div>" +
                "      <div style='font-size:14px;color:#2d4a38;font-weight:500;'>" + direccion + "</div>" +
                "    </td>" +
                "  </tr>" +
                "</table>" +

                // Tabla productos
                "<h3 style='margin:0 0 14px;color:#2d4a38;font-size:16px;font-weight:700;'>🛍️ Detalle de tu compra</h3>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='border-radius:12px;overflow:hidden;border:1px solid #c8f0dc;margin-bottom:24px;'>" +
                "  <thead><tr style='background:linear-gradient(to right,#4fbf88,#2d9e65);'>" +
                "    <th style='padding:12px 16px;text-align:left;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>PRODUCTO</th>" +
                "    <th style='padding:12px 16px;text-align:center;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>CANT.</th>" +
                "    <th style='padding:12px 16px;text-align:right;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>PRECIO</th>" +
                "    <th style='padding:12px 16px;text-align:right;color:#fff;font-size:12px;font-weight:600;letter-spacing:0.5px;'>SUBTOTAL</th>" +
                "  </tr></thead>" +
                "  <tbody style='background:#fff;'>" + filasProductos + "</tbody>" +
                "  <tfoot><tr style='background:linear-gradient(to right,#2d9e65,#1a7a4a);'>" +
                "    <td colspan='3' style='padding:14px 16px;color:#fff;font-weight:700;font-size:14px;'>TOTAL PAGADO</td>" +
                "    <td style='padding:14px 16px;text-align:right;color:#fff;font-weight:700;font-size:16px;'>$" + String.format("%,.0f", total) + "</td>" +
                "  </tr></tfoot>" +
                "</table>" +

                // Aviso PDF
                "<div style='background:#fff8e6;border:1px solid #f0d080;border-radius:12px;padding:16px 20px;margin-bottom:28px;'>" +
                "  <span style='font-size:20px;'>📎</span>" +
                "  <span style='font-size:13px;color:#7a6020;font-weight:500;margin-left:10px;'>Tu <strong>factura en PDF</strong> está adjunta a este correo como comprobante de compra.</span>" +
                "</div>" +

                "<p style='margin:0;font-size:14px;color:#4a6a55;line-height:1.7;'>Gracias por tu confianza. Esperamos que disfrutes tus productos. ¡Vuelve pronto a <strong style='color:#2d9e65;'>Candy House</strong>! 🍰</p>" +
                "</td></tr>" +

                // FOOTER rosa — mantiene identidad Candy House
                "<tr><td style='background:linear-gradient(to right,#f48ca7,#e35d7a);padding:24px 40px;text-align:center;'>" +
                "  <p style='margin:0 0 6px;color:#fff;font-weight:700;font-size:15px;'>🍬 Delicias Candy House</p>" +
                "  <p style='margin:0;color:rgba(255,255,255,0.8);font-size:12px;'>© 2026 · Todos los derechos reservados</p>" +
                "</td></tr>" +

                "</table></td></tr></table></body></html>";
    }
}