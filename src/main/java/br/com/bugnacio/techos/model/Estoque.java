package br.com.bugnacio.techos.model;

// Enum: lista fixa de categorias. O usuário só pode escolher uma destas,
// então não existe erro de digitação (igual ao TipoEquipamento).
public enum Estoque {
    PECA, ACESSORIO, PERIFERICO, CONSUMIVEL, OUTRO
}