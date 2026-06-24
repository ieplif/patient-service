package br.com.clinicahumaniza.patient_service.spec;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import br.com.clinicahumaniza.patient_service.util.BuscaNome;

/** Predicado de busca flexível por nome (acento/caixa/espaço-insensível, por tokens). */
final class NomeBusca {

    private NomeBusca() {}

    /**
     * Cada palavra do termo precisa aparecer no nome normalizado (LIKE combinados em AND).
     * Assim "tania alves" encontra "Tânia Mara Barreto Alves", mesmo fora de ordem.
     * O caller deve garantir termo não-vazio; com termo só de espaços, retorna conjunção vazia.
     */
    static Predicate predicado(CriteriaBuilder cb, Expression<String> nomeNormalizado, String termo) {
        List<Predicate> predicados = new ArrayList<>();
        for (String token : BuscaNome.tokens(termo)) {
            predicados.add(cb.like(nomeNormalizado, "%" + token + "%"));
        }
        return cb.and(predicados.toArray(new Predicate[0]));
    }
}
