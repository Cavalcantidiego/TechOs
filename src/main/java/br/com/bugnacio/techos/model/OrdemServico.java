package br.com.bugnacio.techos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Selecione o equipamento.")
    @ManyToOne
    @JoinColumn(name = "equipamento_id")
    private Equipamento equipamento;

    // O que o cliente relatou
    @NotBlank(message = "Descreva o defeito relatado pelo cliente.")
    @Size(min = 5, max = 1000, message = "O defeito deve ter entre 5 e 1000 caracteres.")
    @Column(length = 1000)
    private String defeito;

    // O que o técnico encontrou e fez (obrigatório só para concluir a OS)
    @Size(max = 2000, message = "O laudo pode ter no máximo 2000 caracteres.")
    @Column(length = 2000)
    private String laudo;

    // Nome completo, até existir o cadastro de técnicos
    @NotBlank(message = "Informe o nome completo do técnico.")
    @Size(min = 5, max = 100, message = "Informe o nome completo do técnico (5 a 100 caracteres).")
    private String tecnicoNome;

    @Enumerated(EnumType.STRING)
    private StatusOS status = StatusOS.RECEBIDO;

    private LocalDateTime dataEntrada = LocalDateTime.now();
    private LocalDateTime dataConclusao;

    // Valor do serviço (mão de obra). Obrigatório só para concluir a OS.
    @DecimalMin(value = "0.00", message = "O valor não pode ser negativo.")
    @Digits(integer = 6, fraction = 2, message = "Valor inválido (máximo 999999,99).")
    private BigDecimal valor;

    @NotNull(message = "Informe a garantia em dias.")
    @Min(value = 0, message = "A garantia não pode ser negativa.")
    @Max(value = 365, message = "A garantia pode ter no máximo 365 dias.")
    private Integer garantiaDias = 90;

    // true = as peças já saíram do estoque (OS Concluída/Entregue)
    private boolean baixado = false;

    public boolean isEditavelPecas() {
        return !baixado && status != StatusOS.CANCELADA;
    }

    // Data final da garantia. Não vai para o banco.
    public LocalDate getGarantiaAte() {
        if (dataConclusao == null || garantiaDias == null || garantiaDias <= 0) return null;
        return dataConclusao.toLocalDate().plusDays(garantiaDias);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Equipamento getEquipamento() { return equipamento; }
    public void setEquipamento(Equipamento equipamento) { this.equipamento = equipamento; }

    public String getDefeito() { return defeito; }
    public void setDefeito(String defeito) { this.defeito = defeito; }

    public String getLaudo() { return laudo; }
    public void setLaudo(String laudo) { this.laudo = laudo; }

    public String getTecnicoNome() { return tecnicoNome; }
    public void setTecnicoNome(String tecnicoNome) { this.tecnicoNome = tecnicoNome; }

    public StatusOS getStatus() { return status; }
    public void setStatus(StatusOS status) { this.status = status; }

    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }

    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDateTime dataConclusao) { this.dataConclusao = dataConclusao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public Integer getGarantiaDias() { return garantiaDias; }
    public void setGarantiaDias(Integer garantiaDias) { this.garantiaDias = garantiaDias; }

    public boolean isBaixado() { return baixado; }
    public void setBaixado(boolean baixado) { this.baixado = baixado; }
}