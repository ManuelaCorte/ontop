package it.unibz.inf.ontop.spec.mapping.transformer;

import it.unibz.inf.ontop.constraints.ImmutableCQContainmentCheck;
import it.unibz.inf.ontop.constraints.impl.ExtensionalDataNodeListContainmentCheck;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.model.atom.RelationPredicate;

public interface MappingCQCOptimizer {
    IQ optimize(ExtensionalDataNodeListContainmentCheck cqContainmentCheck, IQ query);
}
