package com.academicrace.api.model;

public record LivroResumo(Long id, String titulo, String autor, Integer ano, String genero,
                          Integer totalPaginas, Boolean temCapa) {
}