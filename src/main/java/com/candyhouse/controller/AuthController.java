package com.candyhouse.controller;


import com.candyhouse.model.Usuario;
import com.candyhouse.repository.UsuarioRepository;
import com.candyhouse.service.CorreoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class AuthController {

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private CorreoService correoService;

    // Almacén temporal de tokens: token -> [idUsuario, timestampCreacion]
    private final Map<String, Long[]> tokenStore = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRACION_SEGUNDOS = 30 * 60; // 30 minutos

    // =========================================================
    // LOGIN  (GET "/" y POST "/")
    // =========================================================
    @GetMapping("/")
    public String loginForm(Model model) {
        model.addAttribute("mensaje", "");
        return "login";
    }

    @PostMapping("/")
    public String loginPost(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            Model model) {

        Optional<Usuario> u = usuarioRepo.findByNombre(username);

        if (u.isPresent() && u.get().getContrasena().equals(password)) {
            session.setAttribute("user",       u.get().getNombre());
            session.setAttribute("tipo",       u.get().getTipo().name());
            session.setAttribute("id_usuario", u.get().getIdUsuario());

            return u.get().getTipo() == Usuario.TipoUsuario.admin
                    ? "redirect:/dashboard"
                    : "redirect:/panel_usuario";
        }

        model.addAttribute("mensaje", "Usuario o contraseña incorrectos");
        return "login";
    }

    // =========================================================
    // REGISTRO
    // =========================================================
    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("mensaje", "");
        return "registro";
    }

    @PostMapping("/registro")
    public String registroPost(@RequestParam String username,
                               @RequestParam String correo,
                               @RequestParam String password,
                               @RequestParam String confirm,
                               Model model) {

        // Siempre devolver los valores para que no se borren los campos
        model.addAttribute("username", username);
        model.addAttribute("correo",   correo);

        if (!password.equals(confirm)) {
            model.addAttribute("mensaje", "Las contraseñas no coinciden");
            return "registro";
        }

        if (password.length() < 8 || password.length() > 15) {
            model.addAttribute("mensaje", "La contraseña debe tener entre 8 y 15 caracteres");
            return "registro";
        }

        //  verificar si el nombre de usuario ya existe
        if (usuarioRepo.findByNombre(username).isPresent()) {
            model.addAttribute("mensaje", "Ese nombre de usuario ya está en uso");
            return "registro";
        }

        //  verificar si el correo ya está registrado
        if (usuarioRepo.findByCorreo(correo).isPresent()) {
            model.addAttribute("mensaje", "Ese correo ya está registrado");
            return "registro";
        }

        Usuario u = new Usuario();
        u.setNombre(username);
        u.setCorreo(correo);
        u.setContrasena(password);
        u.setTipo(Usuario.TipoUsuario.usuario); // siempre usuario, ignorar cualquier valor externo
        usuarioRepo.save(u);

        return "redirect:/";
    }

    // =========================================================
    // LOGOUT
    // =========================================================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // =========================================================
    // OLVIDÉ MI CONTRASEÑA
    // =========================================================
    @GetMapping("/olvide_password")
    public String olvidaForm(Model model) {
        model.addAttribute("mensaje", "");
        return "olvide_password";
    }

    @PostMapping("/olvide_password")
    public String olvidaPost(@RequestParam String correo,
                             HttpServletRequest request,
                             Model model) {

        Optional<Usuario> u = usuarioRepo.findByCorreo(correo);

        if (u.isPresent()) {
            // Generar token único (equivale a URLSafeTimedSerializer de Flask)
            String token = UUID.randomUUID().toString();
            tokenStore.put(token, new Long[]{ u.get().getIdUsuario(), Instant.now().getEpochSecond() });

            String link = request.getScheme() + "://" +
                    request.getServerName() + ":" +
                    request.getServerPort() +
                    "/resetear_password/" + token;

            correoService.enviarEnlaceRecuperacion(correo, link);
        }

        // Siempre muestra el mismo mensaje por seguridad
        model.addAttribute("mensaje", "Si el correo existe, recibirás un enlace en tu bandeja.");
        return "olvide_password";
    }

    // =========================================================
    // RESETEAR CONTRASEÑA
    // =========================================================
    @GetMapping("/resetear_password/{token}")
    public String resetForm(@PathVariable String token, Model model) {
        if (!tokenValido(token)) {
            model.addAttribute("mensaje", "El enlace expiró o no es válido.");
            return "olvide_password";
        }
        model.addAttribute("token", token);
        model.addAttribute("mensaje", "");
        return "resetear_password";
    }


    @PostMapping("/resetear_password/{token}")
    public String resetPost(@PathVariable String token,
                            @RequestParam String password,
                            @RequestParam String confirm,
                            Model model) {

        if (!tokenValido(token)) {
            model.addAttribute("mensaje", "El enlace expiró o no es válido.");
            return "olvide_password";
        }

        if (!password.equals(confirm)) {
            model.addAttribute("token", token);
            model.addAttribute("mensaje", "Las contraseñas no coinciden");
            return "resetear_password";
        }

        if (password.length() < 8 || password.length() > 15) {
            model.addAttribute("token", token);
            model.addAttribute("mensaje", "La contraseña debe tener entre 8 y 15 caracteres");
            return "resetear_password";
        }

        Long idUsuario = tokenStore.get(token)[0];
        Usuario u = usuarioRepo.findById(idUsuario).orElseThrow();
        u.setContrasena(password);
        usuarioRepo.save(u);

        tokenStore.remove(token);
        return "redirect:/";
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private boolean tokenValido(String token) {
        Long[] datos = tokenStore.get(token);
        if (datos == null) return false;
        long segundosTranscurridos = Instant.now().getEpochSecond() - datos[1];
        return segundosTranscurridos <= TOKEN_EXPIRACION_SEGUNDOS;
    }
}