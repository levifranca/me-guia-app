package edu.levifranca.meguia;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by levifranca on 07/09/16.
 */
public class BeaconInfo {

    private Integer id;

    private String nome;

    private String endereco_MAC;

    private String descricao;

    private List<String> tags;

    private String mensagem;

    private String audio;

    private Boolean vibrar;

    private Regiao regiao;

    private Boolean ativo;

    public static BeaconInfo getInstanceFromJSON(JSONObject beaconJson) throws JSONException {
        BeaconInfo bInfo = new BeaconInfo();

        bInfo.setId(beaconJson.getInt("id"));
        bInfo.setNome(beaconJson.getString("nome"));
        bInfo.setEndereco_MAC(beaconJson.getString("endereco_mac"));
        bInfo.setDescricao(beaconJson.getString("descricao"));
        bInfo.setTags(getTagListFromJSONArray(beaconJson.getJSONArray("tags")));
        bInfo.setMensagem(beaconJson.getString("mensagem"));
        bInfo.setAudio(beaconJson.getString("audio"));
        bInfo.setVibrar(beaconJson.getBoolean("vibrar"));
        bInfo.setRegiao(Regiao.getInstanceFromJSON(beaconJson.getJSONObject("regiao")));
        bInfo.setAtivo(beaconJson.getBoolean("ativo"));

        return bInfo;
    }

    private static List<String> getTagListFromJSONArray(JSONArray jsonArray) throws JSONException {
        List<String> tags = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            String tag = jsonArray.getString(i);
            tags.add(tag);
        }
        return tags;
    }

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

    public Regiao getRegiao() {
        return regiao;
    }

    public void setRegiao(Regiao regiao) {
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
