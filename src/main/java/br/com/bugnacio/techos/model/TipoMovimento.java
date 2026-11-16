package br.com.bugnacio.techos.model;

public enum TipoMovimento {
    ENTRADA("Entrada", "success"),                  // chegou peça (reposição)
    RESERVA("Reservada para OS", "secondary"),      // OS separou a peça (estoque físico não muda)
    LIBERACAO_RESERVA("Reserva liberada", "secondary"),
    SAIDA_OS("Saída para OS", "warning"),           // peça usada em uma OS
    DEVOLUCAO_OS("Devolução de OS", "info");        // peça voltou ao estoque

    private final String descricao;
    private final String cor; // cor do selo Bootstrap

    TipoMovimento(String descricao, String cor) {
        this.descricao = descricao;
        this.cor = cor;
    }

    public String getDescricao() { return descricao; }
    public String getCor() { return cor; }
}