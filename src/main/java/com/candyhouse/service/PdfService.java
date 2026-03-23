package com.candyhouse.service;

import com.candyhouse.model.Cliente;
import com.candyhouse.model.DetallePedido;
import com.candyhouse.model.Pedido;
import com.candyhouse.model.Producto;
import com.candyhouse.repository.ClienteRepository;
import com.candyhouse.repository.DetallePedidoRepository;
import com.candyhouse.repository.ProductoRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PdfService {

    @Autowired private DetallePedidoRepository detalleRepo;
    @Autowired private ProductoRepository      productoRepo;
    @Autowired private ClienteRepository       clienteRepo;

    // Colores de marca
    private static final Color ROSE        = new Color(227, 93, 122);
    private static final Color ROSE_DARK   = new Color(180, 50, 85);
    private static final Color PINK_LIGHT  = new Color(244, 140, 167);
    private static final Color BG_SECTION  = new Color(255, 244, 247);
    private static final Color BORDER_SOFT = new Color(243, 194, 204);
    private static final Color TEXT_DARK   = new Color(70, 40, 50);
    private static final Color TEXT_MED    = new Color(120, 80, 95);
    private static final Color TEXT_MUTED  = new Color(160, 120, 130);
    private static final Color GREEN_OK    = new Color(45, 158, 101);
    private static final Color WHITE       = Color.WHITE;
    private static final Color STRIPE      = new Color(255, 249, 251);

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "CO"));

    // =========================================================
    // MAIN
    // =========================================================
    public byte[] generarFacturaPdf(Pedido pedido, Long idUsuario) {

        List<DetallePedido> detalles = detalleRepo.findByIdPedido(pedido.getIdPedido());
        Optional<Cliente>   clienteOpt = clienteRepo.findByIdUsuario(idUsuario);

        BigDecimal total = detalles.stream()
                .map(d -> d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String nombreCliente = clienteOpt.map(c -> c.getNombreCli() + " " + c.getApellidoCli())
                .orElse("Cliente");
        String docCliente    = clienteOpt.map(c -> c.getTipoDoc() + " " + c.getClienteDoc())
                .orElse("—");
        String telCliente    = clienteOpt.map(c -> c.getTelefono() != null ? c.getTelefono().toString() : "—")
                .orElse("—");

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.LETTER, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── Fuentes ───────────────────────────────────────────
            Font fTitulo     = new Font(Font.HELVETICA, 26, Font.BOLD,   WHITE);
            Font fSubtitulo  = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(255, 220, 230));
            Font fH2         = new Font(Font.HELVETICA, 13, Font.BOLD,   TEXT_DARK);
            Font fLabel      = new Font(Font.HELVETICA,  9, Font.BOLD,   TEXT_MUTED);
            Font fValor      = new Font(Font.HELVETICA, 11, Font.NORMAL, TEXT_DARK);
            Font fValorBold  = new Font(Font.HELVETICA, 11, Font.BOLD,   TEXT_DARK);
            Font fTh         = new Font(Font.HELVETICA, 10, Font.BOLD,   WHITE);
            Font fTd         = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
            Font fTdBold     = new Font(Font.HELVETICA, 10, Font.BOLD,   TEXT_DARK);
            Font fNota       = new Font(Font.HELVETICA,  8, Font.ITALIC, TEXT_MUTED);
            Font fTotal      = new Font(Font.HELVETICA, 13, Font.BOLD,   WHITE);
            Font fTotalVal   = new Font(Font.HELVETICA, 14, Font.BOLD,   WHITE);
            Font fBadge      = new Font(Font.HELVETICA, 10, Font.BOLD,   WHITE);
            Font fFooter     = new Font(Font.HELVETICA,  9, Font.NORMAL, new Color(255, 220, 230));
            Font fFooterBold = new Font(Font.HELVETICA, 10, Font.BOLD,   WHITE);

            // ── ENCABEZADO (banda completa) ────────────────────────
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{40f, 60f});

            // Celda izquierda — logo + nombre
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBackgroundColor(ROSE_DARK);
            logoCell.setPadding(28);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            // Intentar cargar logo desde resources
            try {
                ClassPathResource logoRes = new ClassPathResource("static/imagenes/logo_candy_house.png");
                if (logoRes.exists()) {
                    Image logo = Image.getInstance(logoRes.getURL());
                    logo.scaleToFit(80, 80);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    logoCell.addElement(logo);
                }
            } catch (Exception ignored) {
                // Si no hay logo, solo texto
            }

            Paragraph marca = new Paragraph("Candy House", fTitulo);
            marca.setAlignment(Element.ALIGN_CENTER);
            marca.setSpacingBefore(8);
            logoCell.addElement(marca);

            Paragraph tagline = new Paragraph("Delicias Artesanales", fSubtitulo);
            tagline.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(tagline);
            header.addCell(logoCell);

            // Celda derecha — datos factura
            PdfPCell facCell = new PdfPCell();
            facCell.setBackgroundColor(ROSE);
            facCell.setPadding(28);
            facCell.setBorder(Rectangle.NO_BORDER);
            facCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Font fFacturaTit = new Font(Font.HELVETICA, 18, Font.BOLD, WHITE);
            Font fFacturaNro = new Font(Font.HELVETICA, 28, Font.BOLD, new Color(255, 220, 230));
            Font fFacturaLbl = new Font(Font.HELVETICA,  9, Font.NORMAL, new Color(255, 200, 215));
            Font fFacturaVal = new Font(Font.HELVETICA, 10, Font.NORMAL, WHITE);

            Paragraph facTit = new Paragraph("FACTURA", fFacturaTit);
            facTit.setAlignment(Element.ALIGN_RIGHT);
            facCell.addElement(facTit);

            Paragraph facNro = new Paragraph("#" + pedido.getIdPedido(), fFacturaNro);
            facNro.setAlignment(Element.ALIGN_RIGHT);
            facNro.setSpacingBefore(2);
            facCell.addElement(facNro);

            Paragraph facFecha = new Paragraph("Fecha de entrega: " + pedido.getFechaEntrega().format(FMT), fFacturaVal);
            facFecha.setAlignment(Element.ALIGN_RIGHT);
            facFecha.setSpacingBefore(10);
            facCell.addElement(facFecha);

            Paragraph facDir = new Paragraph("Dirección: " + pedido.getDireccionEnvio() + ", " + pedido.getCiudad(), fFacturaVal);
            facDir.setAlignment(Element.ALIGN_RIGHT);
            facDir.setSpacingBefore(2);
            facCell.addElement(facDir);

            // Badge estado
            Font fBadgeGreen = new Font(Font.HELVETICA, 9, Font.BOLD, WHITE);
            Paragraph estado = new Paragraph("✔  ENTREGADO", fBadgeGreen);
            estado.setAlignment(Element.ALIGN_RIGHT);
            estado.setSpacingBefore(10);
            facCell.addElement(estado);

            header.addCell(facCell);
            doc.add(header);

            // Margen interior
            float ml = 40, mr = 40;

            // ── DATOS DEL CLIENTE ───────────────────────────────────
            PdfPTable secCliente = buildSeccion("Datos del Cliente", fH2, BG_SECTION, BORDER_SOFT, ml, mr);
            doc.add(secCliente);

            PdfPTable infoCliente = new PdfPTable(4);
            infoCliente.setWidthPercentage(100);
            infoCliente.setWidths(new float[]{25f, 25f, 25f, 25f});
            infoCliente.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            configurarMargenes(infoCliente, ml, mr);

            addInfoCell(infoCliente, "Nombre",    nombreCliente, fLabel, fValorBold);
            addInfoCell(infoCliente, "Documento", docCliente,    fLabel, fValor);
            addInfoCell(infoCliente, "Teléfono",  telCliente,    fLabel, fValor);
            addInfoCell(infoCliente, "Estado",    "Entregado",   fLabel, new Font(Font.HELVETICA, 11, Font.BOLD, GREEN_OK));
            doc.add(infoCliente);
            doc.add(spacer(14));

            // ── TABLA DE PRODUCTOS ──────────────────────────────────
            PdfPTable secProd = buildSeccion("Detalle de Productos", fH2, BG_SECTION, BORDER_SOFT, ml, mr);
            doc.add(secProd);

            PdfPTable tabla = new PdfPTable(5);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{6f, 38f, 16f, 20f, 20f});
            configurarMargenes(tabla, ml, mr);
            tabla.setSpacingAfter(0);

            // Headers
            String[] headers = {"#", "Producto", "Cant.", "Precio Unit.", "Subtotal"};
            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, fTh));
                c.setBackgroundColor(ROSE);
                c.setPadding(10);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setBorder(Rectangle.NO_BORDER);
                tabla.addCell(c);
            }

            int fila = 0;
            for (DetallePedido d : detalles) {
                String   nombrePro = productoRepo.findById(d.getProductoCod())
                        .map(Producto::getNombrePro).orElse("?");
                BigDecimal subtotal = d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad()));
                Color bg = (fila % 2 == 0) ? WHITE : STRIPE;
                fila++;

                // # fila
                addTdCell(tabla, String.valueOf(fila), fTd, bg, Element.ALIGN_CENTER);

                // Producto + nota
                PdfPCell cProd = new PdfPCell();
                cProd.setBackgroundColor(bg);
                cProd.setPadding(10);
                cProd.setBorder(Rectangle.BOTTOM);
                cProd.setBorderColor(BORDER_SOFT);
                cProd.setBorderWidth(0.5f);
                cProd.addElement(new Phrase(nombrePro, fTdBold));
                if (d.getNota() != null && !d.getNota().isBlank()) {
                    Paragraph pNota = new Paragraph("  » " + d.getNota(), fNota);
                    pNota.setSpacingBefore(2);
                    cProd.addElement(pNota);
                }
                tabla.addCell(cProd);

                addTdCell(tabla, String.valueOf(d.getCantidad()),          fTd, bg, Element.ALIGN_CENTER);
                addTdCell(tabla, "$" + fmt(d.getPrecio()),                 fTd, bg, Element.ALIGN_RIGHT);
                addTdCell(tabla, "$" + fmt(subtotal),                      fTdBold, bg, Element.ALIGN_RIGHT);
            }

            // Fila total
            PdfPCell emptySpan = new PdfPCell(new Phrase(""));
            emptySpan.setColspan(3);
            emptySpan.setBorder(Rectangle.NO_BORDER);
            emptySpan.setBackgroundColor(WHITE);
            tabla.addCell(emptySpan);

            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL", fTotal));
            totalLabel.setBackgroundColor(ROSE_DARK);
            totalLabel.setPadding(12);
            totalLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalLabel.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(totalLabel);

            PdfPCell totalVal = new PdfPCell(new Phrase("$" + fmt(total), fTotalVal));
            totalVal.setBackgroundColor(ROSE_DARK);
            totalVal.setPadding(12);
            totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalVal.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(totalVal);

            doc.add(tabla);
            doc.add(spacer(20));

            // ── MENSAJE FINAL ───────────────────────────────────────
            PdfPTable msgBox = new PdfPTable(1);
            msgBox.setWidthPercentage(100);
            configurarMargenes(msgBox, ml, mr);

            PdfPCell msgCell = new PdfPCell();
            msgCell.setBackgroundColor(BG_SECTION);
            msgCell.setBorderColor(BORDER_SOFT);
            msgCell.setBorderWidth(1f);
            msgCell.setPadding(18);
            msgCell.setBorder(Rectangle.BOX);

            Font fMsgTit  = new Font(Font.HELVETICA, 12, Font.BOLD,   TEXT_DARK);
            Font fMsgBody = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_MED);

            Paragraph msgTit = new Paragraph("¡Gracias por tu compra, " + nombreCliente + "! 🎉", fMsgTit);
            msgTit.setSpacingAfter(4);
            msgCell.addElement(msgTit);

            Paragraph msgBody = new Paragraph(
                    "Esperamos que disfrutes tus delicias. Si tienes alguna duda o comentario, " +
                            "no dudes en contactarnos. ¡Esperamos verte pronto de nuevo en Candy House!", fMsgBody);
            msgCell.addElement(msgBody);
            msgBox.addCell(msgCell);
            doc.add(msgBox);

            // ── FOOTER ─────────────────────────────────────────────
            doc.add(spacer(24));
            PdfPTable footer = new PdfPTable(1);
            footer.setWidthPercentage(100);

            PdfPCell ftCell = new PdfPCell();
            ftCell.setBackgroundColor(ROSE);
            ftCell.setPadding(18);
            ftCell.setBorder(Rectangle.NO_BORDER);

            Paragraph ftNombre = new Paragraph("🍬  Delicias Candy House", fFooterBold);
            ftNombre.setAlignment(Element.ALIGN_CENTER);
            ftCell.addElement(ftNombre);

            Paragraph ftCopy = new Paragraph("© 2026 · Todos los derechos reservados · Este documento es tu comprobante de compra.", fFooter);
            ftCopy.setAlignment(Element.ALIGN_CENTER);
            ftCopy.setSpacingBefore(4);
            ftCell.addElement(ftCopy);

            footer.addCell(ftCell);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando factura PDF: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private PdfPTable buildSeccion(String titulo, Font font, Color bg, Color border,
                                   float ml, float mr) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6);
        t.setSpacingAfter(0);
        configurarMargenes(t, ml, mr);

        PdfPCell c = new PdfPCell(new Phrase(titulo, font));
        c.setBackgroundColor(bg);
        c.setBorderColor(border);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderWidth(2f);
        c.setPadding(12);
        c.setPaddingLeft(0);
        t.addCell(c);
        return t;
    }

    private void addInfoCell(PdfPTable t, String label, String valor,
                             Font fLabel, Font fValor) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(8);
        c.setBackgroundColor(WHITE);
        c.addElement(new Phrase(label.toUpperCase(), fLabel));
        Paragraph p = new Paragraph(valor, fValor);
        p.setSpacingBefore(2);
        c.addElement(p);
        t.addCell(c);
    }

    private void addTdCell(PdfPTable t, String texto, Font font,
                           Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(bg);
        c.setPadding(10);
        c.setHorizontalAlignment(align);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER_SOFT);
        c.setBorderWidth(0.5f);
        t.addCell(c);
    }

    private void configurarMargenes(PdfPTable t, float ml, float mr) {
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        // iText no tiene setPaddingLeft en PdfPTable directo,
        // lo simulamos con columnas vacías laterales
        // En su lugar simplemente ajustamos el widthPercentage y usamos setHorizontalAlignment
        t.setWidthPercentage(92);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
    }

    private Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(height);
        return p;
    }

    private String fmt(BigDecimal v) {
        return String.format("%,.0f", v);
    }
}