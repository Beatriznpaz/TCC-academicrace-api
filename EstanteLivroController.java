package com.academicrace.api.controller;

import com.academicrace.api.model.EstanteLivro;
import com.academicrace.api.model.EstanteLivro.StatusLeitura;
import com.academicrace.api.model.Livro;
import com.academicrace.api.model.Usuario;
import com.academicrace.api.service.EstanteLivroService;
import com.academicrace.api.service.LivroService;
import com.academicrace.api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/estante")
public class EstanteLivroController {

    private static final int MAX_CAPA = 4_000_000; // caracteres de Base64 (uns 3 MB)

    private static final Set<String> GENEROS = Set.of(
            "Romance", "Fantasia", "Ficção científica", "Terror", "Mistério",
            "Biografia", "Autoajuda", "Poesia", "Clássicos", "Outros");

    @Autowired
    private EstanteLivroService estanteLivroService;

    @Autowired
    private LivroService livroService;

    @Autowired
    private UsuarioService usuarioService;

    public record NovoLivroRequest(String titulo, String autor, Integer ano, String genero,
                                   Integer totalPaginas, String capa, StatusLeitura status) {
    }

    public record AdicionarExistenteRequest(StatusLeitura status) {
    }

    public record AtualizarEstanteRequest(StatusLeitura status, Integer progresso, Integer nota, String comentario) {
    }

    // Sem a capa: o app baixa cada capa em /livros/{id}/capa
    public record ItemEstante(Long id, Long livroId, String titulo, String autor, Integer ano, String genero,
                              Integer totalPaginas, boolean temCapa, StatusLeitura status,
                              Integer progresso, Integer nota, String comentario) {
        static ItemEstante de(EstanteLivro e) {
            Livro l = e.getLivro();
            boolean temCapa = l.getCapa() != null && !l.getCapa().isBlank();
            return new ItemEstante(e.getId(), l.getId(), l.getTitulo(), l.getAutor(), l.getAno(), l.getGenero(),
                    l.getTotalPaginas(), temCapa, e.getStatus(), e.getProgresso(), e.getNota(), e.getComentario());
        }
    }

    public record ItemRankingLeitura(Long id, String nome, int livrosLidos) {
    }

    private ResponseEntity<String> erro(String mensagem) {
        return ResponseEntity.badRequest().body(mensagem);
    }

    // Estante do usuário logado
    @GetMapping("/me")
    public List<ItemEstante> listarMe(Authentication authentication) {
        Long usuarioId = usuarioService.buscarAtual(authentication).getId();
        return estanteLivroService.listarPorUsuario(usuarioId).stream()
                .map(ItemEstante::de)
                .toList();
    }

    // Cadastra um livro novo (ou reaproveita o do catálogo) e coloca na estante do usuário
    @PostMapping("/me")
    @Transactional
    public ResponseEntity<?> adicionarMe(Authentication authentication, @RequestBody NovoLivroRequest request) {
        String titulo = request.titulo() == null ? "" : request.titulo().trim();
        if (titulo.isEmpty() || titulo.length() > 200) {
            return erro("Informe o título do livro (até 200 caracteres).");
        }

        String autor = request.autor() == null ? "" : request.autor().trim();
        if (autor.length() > 100) {
            return erro("O autor pode ter até 100 caracteres.");
        }

        String genero = request.genero() == null ? "" : request.genero().trim();
        if (!genero.isEmpty() && !GENEROS.contains(genero)) {
            return erro("Gênero inválido.");
        }

        Integer ano = request.ano();
        if (ano != null && (ano < 1000 || ano > Year.now().getValue() + 1)) {
            return erro("Ano inválido.");
        }

        Integer paginas = request.totalPaginas();
        if (paginas != null && (paginas < 1 || paginas > 20000)) {
            return erro("Número de páginas inválido.");
        }

        String capa = request.capa();
        if (capa != null && capa.isBlank()) {
            capa = null;
        }
        if (capa != null && capa.length() > MAX_CAPA) {
            return erro("A capa é muito grande.");
        }

        Usuario usuario = usuarioService.buscarAtual(authentication);
        StatusLeitura status = request.status() != null ? request.status() : StatusLeitura.ESTANTE;

        Livro livro = encontrarOuCriarLivro(titulo, autor, ano, genero, paginas, capa);
        return ResponseEntity.ok(colocarNaEstante(usuario, livro, status));
    }

    // Adiciona um livro que já existe na biblioteca à estante do usuário (ou muda o status)
    @PostMapping("/me/livro/{livroId}")
    @Transactional
    public ResponseEntity<?> adicionarExistente(Authentication authentication, @PathVariable Long livroId,
                                                @RequestBody AdicionarExistenteRequest request) {
        Livro livro = livroService.buscarPorId(livroId).orElse(null);
        if (livro == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Livro não encontrado.");
        }

        Usuario usuario = usuarioService.buscarAtual(authentication);
        StatusLeitura status = request.status() != null ? request.status() : StatusLeitura.TBR;
        return ResponseEntity.ok(colocarNaEstante(usuario, livro, status));
    }

    // Atualiza status, progresso, nota ou comentário de um livro da estante do usuário logado
    @PutMapping("/me/{id}")
    @Transactional
    public ResponseEntity<?> atualizarMe(Authentication authentication, @PathVariable Long id,
                                         @RequestBody AtualizarEstanteRequest request) {
        EstanteLivro estante = buscarDoUsuario(authentication, id);
        if (estante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Livro não encontrado na sua estante.");
        }

        if (request.status() != null) {
            estante.setStatus(request.status());
        }
        if (request.progresso() != null) {
            estante.setProgresso(Math.max(0, Math.min(100, request.progresso())));
        }
        if (request.nota() != null) {
            if (request.nota() < 1 || request.nota() > 5) {
                return erro("A nota deve ser de 1 a 5.");
            }
            estante.setNota(request.nota());
        }
        if (request.comentario() != null) {
            String comentario = request.comentario().trim();
            if (comentario.length() > 2000) {
                return erro("A descrição pode ter até 2000 caracteres.");
            }
            estante.setComentario(comentario.isEmpty() ? null : comentario);
        }

        // Chegou a 100% lendo: o livro passa a ser lido
        if (estante.getStatus() == StatusLeitura.CR && estante.getProgresso() != null
                && estante.getProgresso() >= 100) {
            estante.setStatus(StatusLeitura.LIDO);
        }
        if (estante.getStatus() == StatusLeitura.LIDO) {
            estante.setProgresso(100);
        }

        return ResponseEntity.ok(ItemEstante.de(estanteLivroService.salvar(estante)));
    }

    // Tira o livro da estante do usuário (o livro continua no catálogo)
    @DeleteMapping("/me/{id}")
    public ResponseEntity<?> removerMe(Authentication authentication, @PathVariable Long id) {
        EstanteLivro estante = buscarDoUsuario(authentication, id);
        if (estante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Livro não encontrado na sua estante.");
        }
        estanteLivroService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // Ranking de livros lidos (todos os usuários)
    @GetMapping("/ranking-leitura")
    public List<ItemRankingLeitura> rankingLeitura() {
        return usuarioService.listarTodos().stream()
                .map(u -> new ItemRankingLeitura(
                        u.getId(),
                        u.getNome(),
                        estanteLivroService.listarPorUsuarioEStatus(u.getId(), StatusLeitura.LIDO).size()))
                .sorted(Comparator.comparingInt(ItemRankingLeitura::livrosLidos).reversed())
                .toList();
    }

    // Procura só entre os livros do próprio usuário: ninguém mexe na estante dos outros
    private EstanteLivro buscarDoUsuario(Authentication authentication, Long id) {
        Long usuarioId = usuarioService.buscarAtual(authentication).getId();
        return estanteLivroService.listarPorUsuario(usuarioId).stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Mesmo título e autor = mesmo livro do catálogo; completa o que estiver faltando
    private Livro encontrarOuCriarLivro(String titulo, String autor, Integer ano, String genero,
                                        Integer paginas, String capa) {
        Optional<Livro> existente = livroService.buscarPorTitulo(titulo).stream()
                .filter(l -> l.getTitulo().equalsIgnoreCase(titulo))
                .filter(l -> (l.getAutor() == null ? "" : l.getAutor().trim()).equalsIgnoreCase(autor))
                .findFirst();

        Livro livro = existente.orElseGet(Livro::new);
        if (existente.isEmpty()) {
            livro.setTitulo(titulo);
            livro.setAutor(autor.isEmpty() ? null : autor);
        }
        if (livro.getAno() == null && ano != null) {
            livro.setAno(ano);
        }
        if ((livro.getGenero() == null || livro.getGenero().isBlank()) && !genero.isEmpty()) {
            livro.setGenero(genero);
        }
        if (livro.getTotalPaginas() == null && paginas != null) {
            livro.setTotalPaginas(paginas);
        }
        if ((livro.getCapa() == null || livro.getCapa().isBlank()) && capa != null) {
            livro.setCapa(capa);
        }
        return livroService.salvar(livro);
    }

    // Se o usuário já tem o livro, só muda o status; senão cria a entrada
    private ItemEstante colocarNaEstante(Usuario usuario, Livro livro, StatusLeitura status) {
        EstanteLivro estante = estanteLivroService.listarPorUsuario(usuario.getId()).stream()
                .filter(e -> e.getLivro().getId().equals(livro.getId()))
                .findFirst()
                .orElseGet(() -> {
                    EstanteLivro nova = new EstanteLivro();
                    nova.setUsuario(usuario);
                    nova.setLivro(livro);
                    return nova;
                });

        estante.setStatus(status);
        if (status == StatusLeitura.CR && estante.getProgresso() == null) {
            estante.setProgresso(0);
        }
        if (status == StatusLeitura.LIDO) {
            estante.setProgresso(100);
        }
        return ItemEstante.de(estanteLivroService.salvar(estante));
    }
}