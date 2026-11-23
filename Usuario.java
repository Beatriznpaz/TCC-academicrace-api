package com.academicrace.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    // LONGTEXT em vez de VARCHAR(255): guardamos a foto como data URI base64,
    // que facilmente passa dos 255 caracteres.
    @Column(columnDefinition = "LONGTEXT")
    private String foto;

    // Nome de usuário: a unicidade é garantida pelo índice uk_usuario_username no banco
    @Column(length = 30)
    private String username;

    // Nunca sai nas respostas JSON de outras rotas
    @JsonIgnore
    @Column(length = 20)
    private String telefone;

    @JsonIgnore
    @Column(name = "codigo_redefinicao", length = 100)
    private String codigoRedefinicao;

    @JsonIgnore
    @Column(name = "codigo_expira_em")
    private LocalDateTime codigoExpiraEm;

    @JsonIgnore
    @Column(name = "codigo_tentativas")
    private Integer codigoTentativas;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCodigoRedefinicao() {
        return codigoRedefinicao;
    }

    public void setCodigoRedefinicao(String codigoRedefinicao) {
        this.codigoRedefinicao = codigoRedefinicao;
    }

    public LocalDateTime getCodigoExpiraEm() {
        return codigoExpiraEm;
    }

    public void setCodigoExpiraEm(LocalDateTime codigoExpiraEm) {
        this.codigoExpiraEm = codigoExpiraEm;
    }

    public Integer getCodigoTentativas() {
        return codigoTentativas;
    }

    public void setCodigoTentativas(Integer codigoTentativas) {
        this.codigoTentativas = codigoTentativas;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}