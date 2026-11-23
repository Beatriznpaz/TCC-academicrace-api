package com.academicrace.api.controller;

import com.academicrace.api.model.RegistroEstudo;
import com.academicrace.api.service.RegistroEstudoService;
import com.academicrace.api.service.StreakService;
import com.academicrace.api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/ranking")
public class RankingController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RegistroEstudoService registroEstudoService;

    @Autowired
    private StreakService streakService;

    public record ItemRanking(Long id, String nome, int streak, int tempoEstudadoHoje, int materiasEstudadas, boolean voce) {
    }

    @GetMapping
    public List<ItemRanking> ranking(Authentication authentication) {
        Long meuId = usuarioService.buscarAtual(authentication).getId();
        LocalDate hoje = LocalDate.now();

        return usuarioService.listarTodos().stream()
                .map(usuario -> {
                    List<RegistroEstudo> registros = registroEstudoService.listarPorUsuario(usuario.getId());

                    int tempoHoje = registros.stream()
                            .filter(r -> r.getData() != null && r.getData().toLocalDate().equals(hoje))
                            .mapToInt(r -> r.getDuracaoMinutos() == null ? 0 : r.getDuracaoMinutos())
                            .sum();

                    int materias = (int) registros.stream()
                            .map(RegistroEstudo::getTitulo)
                            .filter(Objects::nonNull)
                            .map(t -> t.trim().toLowerCase())
                            .distinct()
                            .count();

                    int streak = streakService.obterAtualizado(usuario.getId())
                            .map(s -> s.getDiasConsecutivos() == null ? 0 : s.getDiasConsecutivos())
                            .orElse(0);

                    return new ItemRanking(
                            usuario.getId(),
                            usuario.getNome(),
                            streak,
                            tempoHoje,
                            materias,
                            usuario.getId().equals(meuId));
                })
                .sorted(Comparator.comparingInt(ItemRanking::streak).reversed())
                .toList();
    }
}