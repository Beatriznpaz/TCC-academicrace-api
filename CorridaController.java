package com.academicrace.api.controller;

import com.academicrace.api.model.Grupo;
import com.academicrace.api.service.CorridaService;
import com.academicrace.api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/grupos")
public class CorridaController {

    private static final int MAX_FOTO = 4_000_000; // caracteres de Base64 (uns 3 MB)

    @Autowired
    private CorridaService corridaService;

    @Autowired
    private UsuarioService usuarioService;

    public record NovoGrupoRequest(String nome, String descricao, Grupo.TipoGrupo tipo, Integer metaDiaria,
                                   LocalDate dataInicio, LocalDate dataFim, String foto) {
    }

    public record EntrarRequest(String codigo) {
    }

    public record NovoProgressoRequest(Integer tempoMinutos, String observacao) {
    }

    public record GrupoResponse(Long id, String nome, String descricao, String tipo, Integer metaDiaria,
                                LocalDate dataInicio, LocalDate dataFim, String codigoConvite, String foto) {
        static GrupoResponse de(Grupo g) {
            return new GrupoResponse(g.getId(), g.getNome(), g.getDescricao(),
                    g.getTipo() == null ? null : g.getTipo().name(), g.getMetaDiaria(),
                    g.getDataInicio(), g.getDataFim(), g.getCodigoConvite(), g.getFoto());
        }
    }

    @GetMapping("/me")
    public List<GrupoResponse> meusGrupos(Authentication authentication) {
        return corridaService.listarGruposDoUsuario(usuarioService.buscarAtual(authentication)).stream()
                .map(GrupoResponse::de)
                .toList();
    }

    @PostMapping("/me")
    public GrupoResponse criar(Authentication authentication, @RequestBody NovoGrupoRequest request) {
        String nome = request.nome() == null ? "" : request.nome().trim();
        if (nome.isEmpty() || nome.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome do grupo (até 100 caracteres).");
        }

        String descricao = request.descricao() == null ? "" : request.descricao().trim();
        if (descricao.length() > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A bio pode ter até 300 caracteres.");
        }

        String foto = request.foto();
        if (foto != null && foto.isBlank()) {
            foto = null;
        }
        if (foto != null && foto.length() > MAX_FOTO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto é muito grande.");
        }

        Grupo grupo = new Grupo();
        grupo.setNome(nome);
        grupo.setDescricao(descricao.isEmpty() ? null : descricao);
        grupo.setTipo(request.tipo() == null ? Grupo.TipoGrupo.ESTUDO : request.tipo());
        grupo.setMetaDiaria(request.metaDiaria());
        grupo.setDataInicio(request.dataInicio());
        grupo.setDataFim(request.dataFim());
        grupo.setFoto(foto);
        return GrupoResponse.de(corridaService.criarGrupo(usuarioService.buscarAtual(authentication), grupo));
    }

    @PostMapping("/entrar")
    public GrupoResponse entrar(Authentication authentication, @RequestBody EntrarRequest request) {
        return GrupoResponse.de(
                corridaService.entrarPorCodigo(usuarioService.buscarAtual(authentication), request.codigo()));
    }

    @GetMapping("/{id}/ranking")
    public CorridaService.RankingGrupo ranking(Authentication authentication, @PathVariable Long id) {
        return corridaService.montarRanking(usuarioService.buscarAtual(authentication), id);
    }

    @PostMapping("/{id}/progresso")
    public void adicionarProgresso(Authentication authentication, @PathVariable Long id,
                                   @RequestBody NovoProgressoRequest request) {
        corridaService.adicionarProgresso(usuarioService.buscarAtual(authentication), id,
                request.tempoMinutos(), request.observacao());
    }
}