package br.com.clinicahumaniza.patient_service.service;

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
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PatientRepository patientRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PagamentoMapper pagamentoMapper;

    @Autowired
    public PagamentoService(PagamentoRepository pagamentoRepository,
                            PatientRepository patientRepository,
                            AssinaturaRepository assinaturaRepository,
                            AgendamentoRepository agendamentoRepository,
                            PagamentoMapper pagamentoMapper) {
        this.pagamentoRepository = pagamentoRepository;
        this.patientRepository = patientRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.pagamentoMapper = pagamentoMapper;
    }

    @Transactional
    public Pagamento createPagamento(PagamentoRequestDTO dto) {
        if (dto.getAssinaturaId() == null && dto.getAgendamentoId() == null) {
            throw new BusinessException("É necessário informar ao menos uma assinatura ou um agendamento");
        }

        Patient paciente = patientRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", dto.getPacienteId()));

        Assinatura assinatura = null;
        if (dto.getAssinaturaId() != null) {
            assinatura = assinaturaRepository.findById(dto.getAssinaturaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assinatura", dto.getAssinaturaId()));
        }

        Agendamento agendamento = null;
        if (dto.getAgendamentoId() != null) {
            agendamento = agendamentoRepository.findById(dto.getAgendamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agendamento", dto.getAgendamentoId()));
        }

        Pagamento pagamento = pagamentoMapper.toEntity(dto, paciente, assinatura, agendamento);
        pagamento.setStatus(StatusPagamento.PENDENTE);

        gerarParcelas(pagamento);

        return pagamentoRepository.save(pagamento);
    }

    public Pagamento getPagamentoById(UUID id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", id));
    }

    public List<Pagamento> getAllPagamentos() {
        return pagamentoRepository.findAll();
    }

    public List<Pagamento> getPagamentosByPaciente(UUID pacienteId) {
        return pagamentoRepository.findByPacienteId(pacienteId);
    }

    public List<Pagamento> getPagamentosByAssinatura(UUID assinaturaId) {
        return pagamentoRepository.findByAssinaturaId(assinaturaId);
    }

    public List<Pagamento> getPagamentosByAgendamento(UUID agendamentoId) {
        return pagamentoRepository.findByAgendamentoId(agendamentoId);
    }

    public List<Pagamento> getPagamentosByPeriodo(LocalDate inicio, LocalDate fim) {
        return pagamentoRepository.findByDataVencimentoBetween(inicio, fim);
    }

    @Transactional
    public Pagamento updatePagamento(UUID id, PagamentoUpdateDTO dto) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", id));

        pagamentoMapper.updateEntityFromDto(dto, pagamento);
        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public Pagamento updateStatus(UUID id, PagamentoStatusDTO dto) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", id));

        validarTransicaoStatus(pagamento.getStatus(), dto.getStatus());

        pagamento.setStatus(dto.getStatus());

        if (dto.getStatus() == StatusPagamento.PAGO) {
            pagamento.setDataPagamento(LocalDateTime.now());
        }

        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public Pagamento updateParcelaStatus(UUID pagamentoId, UUID parcelaId, ParcelaStatusDTO dto) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
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
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", id));
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
        boolean transicaoValida = switch (atual) {
            case PENDENTE -> novo == StatusPagamento.PAGO || novo == StatusPagamento.CANCELADO;
            case PARCIALMENTE_PAGO -> novo == StatusPagamento.PAGO || novo == StatusPagamento.CANCELADO;
            case PAGO -> novo == StatusPagamento.REEMBOLSADO;
            default -> false;
        };

        if (!transicaoValida) {
            throw new BusinessException(
                    "Transição de status inválida: " + atual + " → " + novo
            );
        }
    }
}
