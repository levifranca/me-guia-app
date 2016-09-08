package edu.levifranca.meguia;

import java.util.List;

/**
 * Created by levifranca on 07/09/16.
 */
public class BeaconInfo {

    /*
    id: [integer],
    nome: [string],
    endereco_MAC: [string],
    descricao: [string],
    tags: [string_array],
    mensagem: [string],
    audio: [string], (caminho do arquivo),
    vibrar: [boolean],
    regiao: [integer],
    ativo: [boolean]
     */

    private Integer id;

    private String nome;

    private String endereco_MAC;

    private String descricao;

    private List<String> tags;

    private String mensagem;

    private String audio;

    private Boolean vibrar;

    private String regiao;

    private Boolean ativo;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEndereco_MAC() {
        return endereco_MAC;
    }

    public void setEndereco_MAC(String endereco_MAC) {
        this.endereco_MAC = endereco_MAC;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public Boolean getVibrar() {
        return vibrar;
    }

    public void setVibrar(Boolean vibrar) {
        this.vibrar = vibrar;
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BeaconInfo that = (BeaconInfo) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
