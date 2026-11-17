package br.com.bugnacio.techos.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.bugnacio.techos.model.ItemEstoque;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.PecaUsada;
import br.com.bugnacio.techos.model.StatusOS;
import br.com.bugnacio.techos.repository.ItemEstoqueRepository;
import br.com.bugnacio.techos.repository.OrdemServicoRepository;
import br.com.bugnacio.techos.repository.PecaUsadaRepository;

// Regras da OS que mexem no estoque.
// Toda mudança de quantidade passa pelo EstoqueService, que grava o histórico.
@Service
public class OrdemServicoService {

    private final OrdemServicoRepository osRepo;
    private final PecaUsadaRepository pecaRepo;
    private final ItemEstoqueRepository itemRepo;
    private final EstoqueService estoque;

    public OrdemServicoService(OrdemServicoRepository osRepo, PecaUsadaRepository pecaRepo,
            ItemEstoqueRepository itemRepo, EstoqueService estoque) {
        this.osRepo = osRepo;
        this.pecaRepo = pecaRepo;
        this.itemRepo = itemRepo;
        this.estoque = estoque;
    }

    // ---------- Adicionar peça: RESERVA no estoque (ainda não baixa) ----------
    @Transactional
    public void adicionarPeca(Long osId, String codigo, int qtd) {
        OrdemServico os = buscar(osId);
        exigirEditavel(os);

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Informe o código da peça.");
        }
        // Aceita o código interno ou o código de barras (pistola)
        ItemEstoque item = itemRepo.buscarPorLeitura(codigo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada: " + codigo.trim()));

        // Se faltar disponível, o EstoqueService lança erro com a mensagem pronta
        estoque.reservar(item.getId(), qtd, os.getId(), null, os.getTecnicoNome());

        PecaUsada peca = pecaRepo.findByOsIdAndItemId(osId, item.getId()).orElseGet(() -> {
            PecaUsada nova = new PecaUsada();
            nova.setOs(os);
            nova.setItem(item);
            return nova;
        });
        peca.setQuantidade(peca.getQuantidade() + qtd);
        pecaRepo.save(peca);
    }

    // ---------- Remover peça: libera a reserva ----------
    @Transactional
    public void removerPeca(Long osId, Long pecaId) {
        OrdemServico os = buscar(osId);
        exigirEditavel(os);

        PecaUsada peca = pecaRepo.findById(pecaId)
                .orElseThrow(() -> new IllegalArgumentException("Peça da OS não encontrada."));
        if (!peca.getOs().getId().equals(osId)) {
            throw new IllegalArgumentException("Esta peça não pertence a esta OS.");
        }

        estoque.liberarReserva(peca.getItem().getId(), osId, "Peça removida da OS #" + osId);
        pecaRepo.delete(peca);
    }

    // ---------- Mudar status (é aqui que o estoque é baixado ou devolvido) ----------
    @Transactional
    public void mudarStatus(Long osId, StatusOS novo) {
        OrdemServico os = buscar(osId);
        StatusOS atual = os.getStatus();

        if (atual == novo) return;
        if (atual == StatusOS.CANCELADA) {
            throw new IllegalArgumentException("OS cancelada não pode ser alterada.");
        }

        List<PecaUsada> pecas = pecaRepo.findByOsIdOrderByIdAsc(osId);
        String tecnico = os.getTecnicoNome();

        if (novo == StatusOS.CANCELADA) {
            // Já baixada: a peça volta ao estoque. Ainda reservada: só libera a reserva.
            for (PecaUsada p : pecas) {
                if (os.isBaixado()) {
                    estoque.devolverDaOs(p.getItem().getId(), p.getQuantidade(), osId, null, tecnico);
                } else {
                    estoque.liberarReserva(p.getItem().getId(), osId, "OS #" + osId + " cancelada");
                }
            }
            os.setBaixado(false);
            os.setDataConclusao(null);

        } else if (novo.isFinalizada()) {
            // Concluída/Entregue: as peças saem do estoque (uma vez só)
            if (!os.isBaixado()) {
                for (PecaUsada p : pecas) {
                    estoque.baixarParaOs(p.getItem().getId(), p.getQuantidade(), osId, null, tecnico);
                }
                os.setBaixado(true);
            }
            if (os.getDataConclusao() == null) {
                os.setDataConclusao(LocalDateTime.now());
            }

        } else if (os.isBaixado()) {
            // Reabriu uma OS concluída: devolve as peças e volta a reservá-las
            for (PecaUsada p : pecas) {
                estoque.devolverDaOs(p.getItem().getId(), p.getQuantidade(), osId, null, tecnico);
                estoque.reservar(p.getItem().getId(), p.getQuantidade(), osId, null, tecnico);
            }
            os.setBaixado(false);
            os.setDataConclusao(null);
        }

        os.setStatus(novo);
        osRepo.save(os);
    }

    // ---------- Internos ----------
    private OrdemServico buscar(Long osId) {
        return osRepo.findById(osId)
                .orElseThrow(() -> new IllegalArgumentException("OS não encontrada."));
    }

    private void exigirEditavel(OrdemServico os) {
        if (!os.isEditavelPecas()) {
            throw new IllegalArgumentException(
                    "As peças desta OS não podem ser alteradas. Reabra a OS (mude o status) para editar.");
        }
    }
}