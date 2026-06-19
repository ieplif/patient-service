package br.com.clinicahumaniza.patient_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.clinicahumaniza.patient_service.dto.PagamentoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PagamentoStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.PagamentoUpdateDTO;
import br.com.clinicahumaniza.patient_service.dto.ParcelaStatusDTO;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.PagamentoMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import br.com.clinicahumaniza.patient_service.repository.PagamentoRepository;
import br.com.clinicahumaniza.patient_service.repository.ParcelaRepository;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import br.com.clinicahumaniza.patient_service.spec.PagamentoSpecification;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final ParcelaRepository parcelaRepository;
    private final PatientRepository patientRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PagamentoMapper pagamentoMapper;

    @Autowired
    public PagamentoService(
            PagamentoRepository pagamentoRepository,
            ParcelaRepository parcelaRepository,
            PatientRepository patientRepository,
            AssinaturaRepository assinaturaRepository,
            AgendamentoRepository agendamentoRepository,
            PagamentoMapper pagamentoMapper) {
        this.pagamentoRepository = pagamentoRepository;
        this.parcelaRepository = parcelaRepository;
        this.patientRepository = patientRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.pagamentoMapper = pagamentoMapper;
    }

    /**
     * Soma o valor de todas as parcelas PAGAS cuja dataPagamento cai no período.
     * Reflete o dinheiro efetivamente recebido — inclui pagamentos parcialmente pagos.
     */
    public BigDecimal calcularReceitaPorParcelas(LocalDate inicio, LocalDate fim) {
        LocalDateTime dtInicio = inicio.atStartOfDay();
        LocalDateTime dtFim = fim.atTime(java.time.LocalTime.MAX);
        return parcelaRepository.sumParcelasPagasBetween(dtInicio, dtFim);
    }

    @Transactional
    public Pagamento createPagamento(PagamentoRequestDTO dto) {
        Patient paciente = patientRepository
                .findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", dto.getPacienteId()));

        List<Assinatura> assinaturas = new ArrayList<>();
        if (dto.getAssinaturaIds() != null) {
            for (UUID assinaturaId : dto.getAssinaturaIds()) {
                Assinatura assinatura = assinaturaRepository
                        .findById(assinaturaId)
                        .orElseThrow(() -> new ResourceNotFoundException("Assinatura", assinaturaId));
                assinaturas.add(assinatura);
            }
        }

        Agendamento agendamento = null;
        if (dto.getAgendamentoId() != null) {
            agendamento = agendamentoRepository
                    .findById(dto.getAgendamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agendamento", dto.getAgendamentoId()));
        }

        Pagamento pagamento = pagamentoMapper.toEntity(dto, paciente, assinaturas, agendamento);
        pagamento.setStatus(StatusPagamento.PENDENTE);

        gerarParcelas(pagamento);

        return pagamentoRepository.save(pagamento);
    }

    /**
     * Cria uma cobrança PENDENTE a partir de uma assinatura (usado pela geração automática
     * de mensalidades recorrentes). Valor padrão = preço vigente do serviço; forma de pagamento
     * default PIX (ajustável ao receber). Uma única parcela.
     */
    @Transactional
    public Pagamento gerarCobrancaPendente(Assinatura assinatura, BigDecimal valor, LocalDate dataVencimento) {
        Pagamento pagamento = new Pagamento();
        pagamento.setPaciente(assinatura.getPaciente());
        pagamento.getAssinaturas().add(assinatura);
        pagamento.setValor(valor);
        pagamento.setFormaPagamento(FormaPagamento.PIX);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setNumeroParcelas(1);
        pagamento.setDataVencimento(dataVencimento);
        pagamento.setObservacoes("Cobrança gerada automaticamente em "
                + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " (mensalidade Pilates). Confirme a forma de pagamento ao receber.");

        gerarParcelas(pagamento);

        return pagamentoRepository.save(pagamento);
    }

    public Pagamento getPagamentoById(UUID id) {
        return pagamentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pagamento", id));
    }

    public Page<Pagamento> getAllPagamentos(
            StatusPagamento status,
            List<StatusPagamento> statusIn,
            FormaPagamento formaPagamento,
            UUID pacienteId,
            String pacienteNome,
            LocalDate inicio,
            LocalDate fim,
            LocalDate pagamentoInicio,
            LocalDate pagamentoFim,
            Pageable pageable) {
        Specification<Pagamento> spec = Specification.allOf(
                PagamentoSpecification.hasStatus(status),
                PagamentoSpecification.hasStatusIn(statusIn),
                PagamentoSpecification.hasFormaPagamento(formaPagamento),
                PagamentoSpecification.hasPaciente(pacienteId),
                PagamentoSpecification.hasPacienteNome(pacienteNome),
                PagamentoSpecification.betweenVencimento(inicio, fim),
                PagamentoSpecification.betweenDataPagamento(pagamentoInicio, pagamentoFim));
        return pagamentoRepository.findAll(spec, comDesempate(pageable));
    }

    /**
     * Acrescenta o id como último critério de ordenação para garantir paginação
     * estável. Sem isso, quando muitos registros compartilham a mesma data
     * (vencimento/pagamento), o banco pode reordenar os empates entre páginas —
     * duplicando um pagamento e omitindo outro.
     */
    private Pageable comDesempate(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().and(Sort.by("id")));
    }

    public byte[] exportCsv(LocalDate inicio, LocalDate fim, StatusPagamento status) {
        Specification<Pagamento> spec = Specification.allOf(
                PagamentoSpecification.hasStatus(status), PagamentoSpecification.betweenVencimento(inicio, fim));
        List<Pagamento> pagamentos = pagamentoRepository.findAll(spec);
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Paciente,Valor,FormaPagamento,Status,DataVencimento,DataPagamento\n");
        for (Pagamento p : pagamentos) {
            csv.append(String.join(
                            ",",
                            p.getId().toString(),
                            escapeCsv(p.getPaciente().getNomeCompleto()),
                            p.getValor().toString(),
                            p.getFormaPagamento().name(),
                            p.getStatus().name(),
                            p.getDataVencimento() != null
                                    ? p.getDataVencimento().toString()
                                    : "",
                            p.getDataPagamento() != null ? p.getDataPagamento().toString() : ""))
                    .append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public List<Pagamento> getPagamentosByPaciente(UUID pacienteId) {
        return pagamentoRepository.findByPacienteId(pacienteId);
    }

    public List<Pagamento> getPagamentosByAssinatura(UUID assinaturaId) {
        return pagamentoRepository.findByAssinaturasId(assinaturaId);
    }

    public List<Pagamento> getPagamentosByAgendamento(UUID agendamentoId) {
        return pagamentoRepository.findByAgendamentoId(agendamentoId);
    }

    public List<Pagamento> getPagamentosByPeriodo(LocalDate inicio, LocalDate fim) {
        return pagamentoRepository.findByDataVencimentoBetween(inicio, fim);
    }

    @Transactional
    public Pagamento updatePagamento(UUID id, PagamentoUpdateDTO dto) {
        Pagamento pagamento =
                pagamentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pagamento", id));

        // Resolve entidades relacionadas se foram enviadas no DTO
        Patient paciente = null;
        if (dto.getPacienteId() != null) {
            paciente = patientRepository
                    .findById(dto.getPacienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente", dto.getPacienteId()));
        }

        List<Assinatura> assinaturas = null;
        if (dto.getAssinaturaIds() != null) {
            assinaturas = new ArrayList<>();
            for (UUID assinaturaId : dto.getAssinaturaIds()) {
                Assinatura assinatura = assinaturaRepository
                        .findById(assinaturaId)
                        .orElseThrow(() -> new ResourceNotFoundException("Assinatura", assinaturaId));
                assinaturas.add(assinatura);
            }
        }

        Agendamento agendamento = null;
        if (dto.getAgendamentoId() != null) {
            agendamento = agendamentoRepository
                    .findById(dto.getAgendamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agendamento", dto.getAgendamentoId()));
        }

        // Detecta se valor / parcelas / vencimento mudaram → precisa regenerar parcelas
        boolean valorMudou = dto.getValor() != null && pagamento.getValor().compareTo(dto.getValor()) != 0;
        boolean parcelasMudou =
                dto.getNumeroParcelas() != null && !dto.getNumeroParcelas().equals(pagamento.getNumeroParcelas());
        boolean vencimentoMudou =
                dto.getDataVencimento() != null && !dto.getDataVencimento().equals(pagamento.getDataVencimento());

        pagamentoMapper.updateEntityFromDto(dto, pagamento, paciente, assinaturas, agendamento);

        if (valorMudou || parcelasMudou || vencimentoMudou) {
            pagamento.getParcelas().clear();
            gerarParcelas(pagamento);
        }

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public Pagamento updateStatus(UUID id, PagamentoStatusDTO dto) {
        Pagamento pagamento =
                pagamentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pagamento", id));

        validarTransicaoStatus(pagamento.getStatus(), dto.getStatus());

        pagamento.setStatus(dto.getStatus());

        if (dto.getStatus() == StatusPagamento.PAGO) {
            // Permite registrar pagamentos retroativos com a data correta do recibo
            LocalDateTime dataPag =
                    dto.getDataPagamento() != null ? dto.getDataPagamento().atStartOfDay() : LocalDateTime.now();
            pagamento.setDataPagamento(dataPag);
            // Propaga PAGO para as parcelas que ainda estavam pendentes —
            // evita inconsistência visual e garante que o cálculo de receita
            // (que soma parcelas pagas) reflita o pagamento.
            for (Parcela parcela : pagamento.getParcelas()) {
                if (parcela.getStatus() == StatusParcela.PENDENTE) {
                    parcela.setStatus(StatusParcela.PAGO);
                    parcela.setDataPagamento(dataPag);
                }
            }
        }

        if (dto.getStatus() == StatusPagamento.CANCELADO) {
            // Propaga CANCELADO para as parcelas pendentes
            for (Parcela parcela : pagamento.getParcelas()) {
                if (parcela.getStatus() == StatusParcela.PENDENTE) {
                    parcela.setStatus(StatusParcela.CANCELADO);
                }
            }
        }

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public Pagamento updateParcelaStatus(UUID pagamentoId, UUID parcelaId, ParcelaStatusDTO dto) {
        Pagamento pagamento = pagamentoRepository
                .findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", pagamentoId));

        Parcela parcela = pagamento.getParcelas().stream()
                .filter(p -> p.getId().equals(parcelaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Parcela", parcelaId));

        parcela.setStatus(dto.getStatus());

        if (dto.getStatus() == StatusParcela.PAGO) {
            parcela.setDataPagamento(dto.getDataPagamento() != null ? dto.getDataPagamento() : LocalDateTime.now());
        }

        atualizarStatusPagamento(pagamento);

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public void deletePagamento(UUID id) {
        Pagamento pagamento =
                pagamentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pagamento", id));
        pagamento.setAtivo(false);
        pagamentoRepository.save(pagamento);
    }

    private void gerarParcelas(Pagamento pagamento) {
        int numParcelas = pagamento.getNumeroParcelas();
        BigDecimal valorTotal = pagamento.getValor();
        BigDecimal valorParcela = valorTotal.divide(BigDecimal.valueOf(numParcelas), 2, RoundingMode.DOWN);
        BigDecimal somaParcelasAnteriores = valorParcela.multiply(BigDecimal.valueOf(numParcelas - 1));

        for (int i = 1; i <= numParcelas; i++) {
            Parcela parcela = new Parcela();
            parcela.setPagamento(pagamento);
            parcela.setNumero(i);
            parcela.setDataVencimento(pagamento.getDataVencimento().plusMonths(i - 1));
            parcela.setStatus(StatusParcela.PENDENTE);

            if (i == numParcelas) {
                parcela.setValor(valorTotal.subtract(somaParcelasAnteriores));
            } else {
                parcela.setValor(valorParcela);
            }

            pagamento.getParcelas().add(parcela);
        }
    }

    private void atualizarStatusPagamento(Pagamento pagamento) {
        long totalParcelas = pagamento.getParcelas().size();
        long parcelasPagas = pagamento.getParcelas().stream()
                .filter(p -> p.getStatus() == StatusParcela.PAGO)
                .count();

        if (parcelasPagas == totalParcelas) {
            pagamento.setStatus(StatusPagamento.PAGO);
            pagamento.setDataPagamento(LocalDateTime.now());
        } else if (parcelasPagas > 0) {
            pagamento.setStatus(StatusPagamento.PARCIALMENTE_PAGO);
        }
    }

    private void validarTransicaoStatus(StatusPagamento atual, StatusPagamento novo) {
        boolean transicaoValida =
                switch (atual) {
                    case PENDENTE -> novo == StatusPagamento.PAGO || novo == StatusPagamento.CANCELADO;
                    case PARCIALMENTE_PAGO -> novo == StatusPagamento.PAGO || novo == StatusPagamento.CANCELADO;
                    case PAGO -> novo == StatusPagamento.REEMBOLSADO;
                    default -> false;
                };

        if (!transicaoValida) {
            throw new BusinessException("Transição de status inválida: " + atual + " → " + novo);
        }
    }
}
