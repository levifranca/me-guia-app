package edu.levifranca.meguia;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by levifranca on 19/10/16.
 */
public class Regiao {

    private Integer id;
    private boolean ativo;
    private String nome;
    private String descricao;


    public static Regiao getInstanceFromJSON(JSONObject regiaoJson) throws JSONException {
        Regiao r = new Regiao();

        r.setId(regiaoJson.getInt("id"));
        r.setAtivo(regiaoJson.getBoolean("ativo"));
        r.setNome(regiaoJson.getString("nome"));
        r.setDescricao(regiaoJson.getString("descricao"));

        return r;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

}
