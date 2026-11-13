package br.com.bugnacio.techos.controller;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;

import br.com.bugnacio.techos.model.Equipamento;
import jakarta.validation.Valid;

/**
 * EquipamentoRepository
 */
public class EquipamentoRepository {

    public @Nullable Object findByMarcaContainingIgnoreCaseOrModeloContainingIgnoreCase(String busca, String busca2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByMarcaContainingIgnoreCaseOrModeloContainingIgnoreCase'");
    }

    public @Nullable Object findAll(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    public void save(Equipamento equipamento) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    public void deleteById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
    }

}
