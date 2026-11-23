package com.academicrace.api.controller;

import com.academicrace.api.model.Usuario;
import com.academicrace.api.security.JwtUtil;
import com.academicrace.api.service.EmailService;
import com.academicrace.api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final int MINUTOS_VALIDADE_CODIGO = 15;
    private static final int SEGUNDOS_ENTRE_ENVIOS = 60;
    private static final int MAX_TENTATIVAS = 5;

    private final SecureRandom random = new SecureRandom();

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public record CadastroRequest(String nome, String email, String senha) {
    }

    public record LoginRequest(String email, String senha) {
    }

    public record EsqueciSenhaRequest(String email) {
    }

    public record RedefinirSenhaRequest(String email, String codigo, String novaSenha) {
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@RequestBody CadastroRequest request) {
        String nome = request.nome() == null ? "" : request.nome().trim();
        String email = normalizar(request.email());
        String senha = request.senha() == null ? "" : request.senha();

        if (nome.isEmpty() || nome.length() > 100) {
            return ResponseEntity.badRequest().body("Informe seu nome (até 100 caracteres).");
        }
        if (email.length() > 100 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body("E-mail inválido.");
        }
        if (senha.length() < 6) {
            return ResponseEntity.badRequest().body("A senha precisa ter pelo menos 6 caracteres.");
        }
        if (usuarioService.buscarPorEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Email já cadastrado!");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setDataCriacao(LocalDateTime.now());

        try {
            Usuario salvo = usuarioService.salvar(usuario);
            return ResponseEntity.ok(Map.of("id", salvo.getId(), "nome", salvo.getNome(), "email", salvo.getEmail()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Email já cadastrado!");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String email = normalizar(request.email());
        String senha = request.senha() == null ? "" : request.senha();

        Optional<Usuario> usuario = usuarioService.buscarPorEmail(email);

        if (usuario.isEmpty() || !passwordEncoder.matches(senha, usuario.get().getSenha())) {
            return ResponseEntity.status(401).body("Email ou senha inválidos!");
        }

        String token = jwtUtil.gerarToken(usuario.get().getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }

    // Sempre responde o mesmo, exista ou não o e-mail
    @PostMapping("/esqueci-senha")
    public ResponseEntity<?> esqueciSenha(@RequestBody EsqueciSenhaRequest request) {
        usuarioService.buscarPorEmail(normalizar(request.email())).ifPresent(usuario -> {
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime expira = usuario.getCodigoExpiraEm();

            // Já enviou um código há menos de 60 segundos: não envia outro
            boolean enviadoHaPouco = expira != null
                    && expira.minusMinutes(MINUTOS_VALIDADE_CODIGO).plusSeconds(SEGUNDOS_ENTRE_ENVIOS).isAfter(agora);
            if (enviadoHaPouco) {
                return;
            }

            String codigo = String.format("%06d", random.nextInt(1_000_000));
            usuario.setCodigoRedefinicao(passwordEncoder.encode(codigo));
            usuario.setCodigoExpiraEm(agora.plusMinutes(MINUTOS_VALIDADE_CODIGO));
            usuario.setCodigoTentativas(0);
            usuarioService.salvar(usuario);

            emailService.enviarCodigoRedefinicao(usuario.getEmail(), codigo);
        });

        return ResponseEntity.ok("Se o e-mail estiver cadastrado, enviamos um código.");
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<?> redefinirSenha(@RequestBody RedefinirSenhaRequest request) {
        String invalido = "Código inválido ou expirado.";
        String novaSenha = request.novaSenha() == null ? "" : request.novaSenha();

        if (novaSenha.length() < 6) {
            return ResponseEntity.badRequest().body("A nova senha precisa ter pelo menos 6 caracteres.");
        }

        Optional<Usuario> encontrado = usuarioService.buscarPorEmail(normalizar(request.email()));
        if (encontrado.isEmpty()) {
            return ResponseEntity.badRequest().body(invalido);
        }
        Usuario usuario = encontrado.get();

        if (usuario.getCodigoRedefinicao() == null
                || usuario.getCodigoExpiraEm() == null
                || usuario.getCodigoExpiraEm().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(invalido);
        }

        int tentativas = usuario.getCodigoTentativas() == null ? 0 : usuario.getCodigoTentativas();
        if (tentativas >= MAX_TENTATIVAS) {
            return ResponseEntity.badRequest().body("Muitas tentativas. Peça um novo código.");
        }

        String codigo = request.codigo() == null ? "" : request.codigo().trim();
        if (!passwordEncoder.matches(codigo, usuario.getCodigoRedefinicao())) {
            usuario.setCodigoTentativas(tentativas + 1);
            usuarioService.salvar(usuario);
            return ResponseEntity.badRequest().body(invalido);
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setCodigoRedefinicao(null);
        usuario.setCodigoExpiraEm(null);
        usuario.setCodigoTentativas(null);
        usuarioService.salvar(usuario);

        return ResponseEntity.ok("Senha alterada com sucesso.");
    }
}