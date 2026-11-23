package com.academicrace.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estante_livro")
public class EstanteLivro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusLeitura status;

    // TBR = quero ler, CR = lendo, ESTANTE = tenho na estante, LIDO = terminei, DNF = abandonei
    public enum StatusLeitura {
        TBR, CR, DNF, ESTANTE, LIDO
    }

    // Progresso da leitura, de 0 a 100
    private Integer progresso;

    // Nota da avaliação, de 1 a 5
    private Integer nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Livro getLivro() {
        return livro;
    }

    public void setLivro(Livro livro) {
        this.livro = livro;
    }

    public StatusLeitura getStatus() {
        return status;
    }

    public void setStatus(StatusLeitura status) {
        this.status = status;
    }

    public Integer getProgresso() {
        return progresso;
    }

    public void setProgresso(Integer progresso) {
        this.progresso = progresso;
    }

    public Integer getNota() {
        return nota;
    }

    public void setNota(Integer nota) {
        this.nota = nota;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}