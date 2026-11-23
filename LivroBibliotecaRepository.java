package com.academicrace.api.repository;

import com.academicrace.api.model.Livro;
import com.academicrace.api.model.LivroResumo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LivroBibliotecaRepository extends Repository<Livro, Long> {

    // Não traz a capa (Base64): só informa se existe. Os filtros nulos são ignorados.
    @Query("""
            select new com.academicrace.api.model.LivroResumo(
                l.id, l.titulo, l.autor, l.ano, l.genero, l.totalPaginas,
                case when l.capa is null then false else true end)
            from Livro l
            where (:titulo is null or lower(l.titulo) like :titulo)
              and (:autor is null or lower(l.autor) like :autor)
              and (:genero is null or l.genero = :genero)
              and (:ano is null or l.ano = :ano)
            order by l.titulo asc, l.id asc
            """)
    List<LivroResumo> buscar(@Param("titulo") String titulo,
                             @Param("autor") String autor,
                             @Param("genero") String genero,
                             @Param("ano") Integer ano,
                             Pageable pageable);
}