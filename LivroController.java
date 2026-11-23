package com.academicrace.api.controller;

import com.academicrace.api.model.Livro;
import com.academicrace.api.model.LivroResumo;
import com.academicrace.api.repository.LivroBibliotecaRepository;
import com.academicrace.api.service.LivroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/livros")
public class LivroController {

    private static final int TAMANHO_PAGINA = 20;

    @Autowired
    private LivroService livroService;

    @Autowired
    private LivroBibliotecaRepository bibliotecaRepository;

    // Vira "%texto%" em minúsculas; texto vazio vira null (filtro ignorado)
    private static String padrao(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim().toLowerCase().replaceAll("[%_\\\\]", "");
        return limpo.isEmpty() ? null : "%" + limpo + "%";
    }

    // Biblioteca: catálogo de todos os livros, com filtros e páginas de 20
    @GetMapping("/biblioteca")
    public List<LivroResumo> biblioteca(@RequestParam(required = false) String titulo,
                                        @RequestParam(required = false) String autor,
                                        @RequestParam(required = false) String genero,
                                        @RequestParam(required = false) Integer ano,
                                        @RequestParam(defaultValue = "0") int pagina) {
        String generoLimpo = (genero == null || genero.isBlank()) ? null : genero.trim();
        return bibliotecaRepository.buscar(padrao(titulo), padrao(autor), generoLimpo, ano,
                PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA));
    }

    // Devolve a capa como imagem de verdade (decodifica o Base64)
    @GetMapping("/{id}/capa")
    public ResponseEntity<byte[]> capa(@PathVariable Long id) {
        Livro livro = livroService.buscarPorId(id).orElse(null);
        if (livro == null || livro.getCapa() == null || livro.getCapa().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        String dados = livro.getCapa();
        MediaType tipo = MediaType.IMAGE_JPEG;
        if (dados.startsWith("data:")) {
            int virgula = dados.indexOf(',');
            if (virgula > 0 && dados.substring(0, virgula).contains("image/png")) {
                tipo = MediaType.IMAGE_PNG;
            }
            dados = virgula >= 0 ? dados.substring(virgula + 1) : dados;
        }

        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(dados);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(tipo)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(bytes);
    }
}