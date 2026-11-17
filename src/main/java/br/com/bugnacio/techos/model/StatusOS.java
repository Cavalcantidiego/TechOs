package br.com.bugnacio.techos.model;

public enum StatusOS {
    RECEBIDO("Recebido", "secondary"),
    EM_ANALISE("Em análise", "info"),
    AGUARDANDO_PECA("Aguardando peça", "warning"),
    EM_REPARO("Em reparo", "primary"),
    CONCLUIDA("Concluída", "success"),
    ENTREGUE("Entregue", "dark"),
    CANCELADA("Cancelada", "danger");

    private final String descricao;
    private final String cor; // cor do selo Bootstrap

    StatusOS(String descricao, String cor) {
        this.descricao = descricao;
        this.cor = cor;
    }

    public String getDescricao() { return descricao; }
    public String getCor() { return cor; }

    // Concluída e Entregue: o reparo acabou e as peças já saíram do estoque
    public boolean isFinalizada() {
        return this == CONCLUIDA || this == ENTREGUE;
    }
}