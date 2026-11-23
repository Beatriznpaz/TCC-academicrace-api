package com.academicrace.api.controller;

import com.academicrace.api.model.Usuario;
import com.academicrace.api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._]{3,30}$");

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public record PerfilResponse(Long id, String nome, String username, String email, String telefone, String foto) {
        static PerfilResponse de(Usuario u) {
            return new PerfilResponse(u.getId(), u.getNome(), u.getUsername(), u.getEmail(), u.getTelefone(), u.getFoto());
        }
    }

    public record AtualizarPerfilRequest(String nome, String username, String telefone, String senha, String foto) {
    }

    public record UsuarioResumo(Long id, String nome) {
    }

    @GetMapping("/me")
    public PerfilResponse me(Authentication authentication) {
        return PerfilResponse.de(usuarioService.buscarAtual(authentication));
    }

    // Campos null não mudam. Texto vazio em username ou telefone apaga o valor.
    @PutMapping("/me")
    public ResponseEntity<?> atualizarMe(Authentication authentication, @RequestBody AtualizarPerfilRequest request) {
        Usuario usuario = usuarioService.buscarAtual(authentication);

        if (request.nome() != null) {
            String nome = request.nome().trim();
            if (nome.isEmpty() || nome.length() > 100) {
                return ResponseEntity.badRequest().body("Informe seu nome (até 100 caracteres).");
            }
            usuario.setNome(nome);
        }

        if (request.username() != null) {
            String username = request.username().trim().toLowerCase();
            if (username.isEmpty()) {
                usuario.setUsername(null);
            } else {
                if (!USERNAME.matcher(username).matches()) {
                    return ResponseEntity.badRequest()
                            .body("O usuário deve ter de 3 a 30 caracteres: letras, números, ponto ou _.");
                }
                Optional<Usuario> existente = usuarioService.buscarPorUsername(username);
                if (existente.isPresent() && !existente.get().getId().equals(usuario.getId())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Esse nome de usuário já está em uso.");
                }
                usuario.setUsername(username);
            }
        }

        if (request.telefone() != null) {
            String digitos = request.telefone().replaceAll("\\D", "");
            if (digitos.isEmpty()) {
                usuario.setTelefone(null);
            } else if (digitos.length() < 10 || digitos.length() > 13) {
                return ResponseEntity.badRequest().body("Telefone inválido. Use o DDD e o número.");
            } else {
                usuario.setTelefone(digitos);
            }
        }

        if (request.senha() != null && !request.senha().isBlank()) {
            if (request.senha().length() < 6) {
                return ResponseEntity.badRequest().body("A senha precisa ter pelo menos 6 caracteres.");
            }
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }

        if (request.foto() != null) {
            usuario.setFoto(request.foto());
        }

        try {
            return ResponseEntity.ok(PerfilResponse.de(usuarioService.salvar(usuario)));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Esse nome de usuário já está em uso.");
        }
    }

    // Os outros usuários (só id e nome)
    @GetMapping("/outros")
    public List<UsuarioResumo> outros(Authentication authentication) {
        Long meuId = usuarioService.buscarAtual(authentication).getId();
        return usuarioService.listarTodos().stream()
                .filter(u -> !u.getId().equals(meuId))
                .map(u -> new UsuarioResumo(u.getId(), u.getNome()))
                .toList();
    }
}