package br.com.bugnacio.techos.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.bugnacio.techos.model.ItemEstoque;
import br.com.bugnacio.techos.model.MovimentoEstoque;
import br.com.bugnacio.techos.model.ReservaEstoque;
import br.com.bugnacio.techos.model.TipoMovimento;
import br.com.bugnacio.techos.repository.ItemEstoqueRepository;
import br.com.bugnacio.techos.repository.MovimentoEstoqueRepository;
import br.com.bugnacio.techos.repository.ReservaEstoqueRepository;

/**
 * ÚNICO lugar que altera quantidades do estoque. Todo método grava o histórico.
 *
 * Uso pela tela de estoque:  entrada(...)
 * Uso FUTURO pela OS:        reservar(...), liberarReserva(...), baixarParaOs(...), devolverDaOs(...)
 *
 * Não existe saída sem OS: baixarParaOs exige o osId.
 */
@Service
public class EstoqueService {

    private final ItemEstoqueRepository itemRepo;
    private final MovimentoEstoqueRepository movRepo;
    private final ReservaEstoqueRepository reservaRepo;

    public EstoqueService(ItemEstoqueRepository itemRepo, MovimentoEstoqueRepository movRepo,
            ReservaEstoqueRepository reservaRepo) {
        this.itemRepo = itemRepo;
        this.movRepo = movRepo;
        this.reservaRepo = reservaRepo;
    }

    // ---------- ENTRADA (reposição / estoque inicial) ----------

    @Transactional
    public ItemEstoque entrada(Long itemId, int qtd, String motivo) {
        exigirQuantidade(qtd);
        ItemEstoque item = buscar(itemId);
        item.setQuantidade(item.getQuantidade() + qtd);
        itemRepo.save(item);
        registrar(item, TipoMovimento.ENTRADA, qtd, motivo, null, null, null);
        return item;
    }

    // ---------- OPERAÇÕES DA OS (serão chamadas pelo módulo de OS) ----------

    // A OS separa a peça. O estoque físico não muda, mas o "disponível" diminui.
    @Transactional
    public ItemEstoque reservar(Long itemId, int qtd, Long osId, Long tecnicoId, String tecnicoNome) {
        exigirQuantidade(qtd);
        exigirOs(osId);
        ItemEstoque item = buscar(itemId);

        int disponivel = item.getQuantidade() - reservaRepo.totalReservado(itemId).intValue();
        if (qtd > disponivel) {
            throw new IllegalArgumentException("Disponível insuficiente de " + item.getNome()
                    + " (disponível: " + disponivel + ").");
        }

        ReservaEstoque reserva = reservaRepo.findByItemIdAndOsId(itemId, osId).orElseGet(() -> {
            ReservaEstoque nova = new ReservaEstoque();
            nova.setItem(item);
            nova.setOsId(osId);
            return nova;
        });
        reserva.setQuantidade(reserva.getQuantidade() + qtd);
        reserva.setTecnicoId(tecnicoId);
        reserva.setTecnicoNome(tecnicoNome);
        reservaRepo.save(reserva);

        registrar(item, TipoMovimento.RESERVA, qtd, "Reservada para a OS #" + osId, osId, tecnicoId, tecnicoNome);
        return item;
    }

    // A OS desistiu da peça (removida da OS, OS cancelada antes de usar).
    @Transactional
    public ItemEstoque liberarReserva(Long itemId, Long osId, String motivo) {
        exigirOs(osId);
        ItemEstoque item = buscar(itemId);
        ReservaEstoque reserva = reservaRepo.findByItemIdAndOsId(itemId, osId)
                .orElseThrow(() -> new IllegalArgumentException("Esta OS não tem reserva desta peça."));

        int qtd = reserva.getQuantidade();
        reservaRepo.delete(reserva);
        registrar(item, TipoMovimento.LIBERACAO_RESERVA, qtd,
                (motivo == null || motivo.isBlank()) ? "Reserva liberada da OS #" + osId : motivo,
                osId, reserva.getTecnicoId(), reserva.getTecnicoNome());
        return item;
    }

    // A peça foi usada na OS: sai do estoque. Consome primeiro a reserva da própria OS.
    // Se a OS não reservou (ou reservou menos), o restante precisa estar disponível.
    @Transactional
    public ItemEstoque baixarParaOs(Long itemId, int qtd, Long osId, Long tecnicoId, String tecnicoNome) {
        exigirQuantidade(qtd);
        exigirOs(osId);
        ItemEstoque item = buscar(itemId);

        Optional<ReservaEstoque> opt = reservaRepo.findByItemIdAndOsId(itemId, osId);
        int daReserva = opt.map(ReservaEstoque::getQuantidade).orElse(0);
        int usadoDaReserva = Math.min(daReserva, qtd);
        int extra = qtd - usadoDaReserva;

        int livre = item.getQuantidade() - reservaRepo.totalReservado(itemId).intValue();
        if (extra > livre) {
            throw new IllegalArgumentException("Estoque insuficiente de " + item.getNome()
                    + " (disponível: " + (livre + usadoDaReserva) + ").");
        }

        if (opt.isPresent()) {
            ReservaEstoque reserva = opt.get();
            int resto = daReserva - usadoDaReserva;
            if (resto == 0) {
                reservaRepo.delete(reserva);
            } else {
                reserva.setQuantidade(resto);
                reservaRepo.save(reserva);
            }
        }

        item.setQuantidade(item.getQuantidade() - qtd);
        itemRepo.save(item);
        registrar(item, TipoMovimento.SAIDA_OS, qtd, "Usada na OS #" + osId, osId, tecnicoId, tecnicoNome);
        return item;
    }

    // A peça volta ao estoque (OS reaberta/cancelada depois de usar). Limitada ao que a OS usou.
    @Transactional
    public ItemEstoque devolverDaOs(Long itemId, int qtd, Long osId, Long tecnicoId, String tecnicoNome) {
        exigirQuantidade(qtd);
        exigirOs(osId);
        ItemEstoque item = buscar(itemId);

        long usado = movRepo.usoLiquidoDaOs(itemId, osId);
        if (qtd > usado) {
            throw new IllegalArgumentException("A OS #" + osId + " usou apenas " + usado
                    + " unidade(s) de " + item.getNome() + ".");
        }

        item.setQuantidade(item.getQuantidade() + qtd);
        itemRepo.save(item);
        registrar(item, TipoMovimento.DEVOLUCAO_OS, qtd, "Devolvida pela OS #" + osId, osId, tecnicoId, tecnicoNome);
        return item;
    }

    // ---------- Consultas ----------

    public int totalReservado(Long itemId) {
        return reservaRepo.totalReservado(itemId).intValue();
    }

    // ---------- Internos ----------

    private ItemEstoque buscar(Long itemId) {
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado."));
    }

    private void exigirQuantidade(int qtd) {
        if (qtd < 1) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
    }

    private void exigirOs(Long osId) {
        if (osId == null) throw new IllegalArgumentException("Informe a OS: toda saída precisa de uma OS.");
    }

    private void registrar(ItemEstoque item, TipoMovimento tipo, int qtd, String motivo,
            Long osId, Long tecnicoId, String tecnicoNome) {
        MovimentoEstoque m = new MovimentoEstoque();
        m.setItem(item);
        m.setTipo(tipo);
        m.setQuantidade(qtd);
        m.setSaldoApos(item.getQuantidade());
        m.setMotivo(motivo);
        m.setOsId(osId);
        m.setTecnicoId(tecnicoId);
        m.setTecnicoNome(tecnicoNome);
        movRepo.save(m);
    }
}