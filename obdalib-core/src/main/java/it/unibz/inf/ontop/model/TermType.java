package it.unibz.inf.ontop.model;

import java.util.Optional;

/**
 * TODO: explain
 *
 * Immutable
 */
public interface TermType {

    Predicate.COL_TYPE getColType();

    Optional<LanguageTag> getLanguageTagConstant();

    Optional<Variable> getLanguageTagVariable();

    boolean isCompatibleWith(Predicate.COL_TYPE moreGeneralType);

    Optional<TermType> getCommonDenominator(TermType otherTermType);
}
