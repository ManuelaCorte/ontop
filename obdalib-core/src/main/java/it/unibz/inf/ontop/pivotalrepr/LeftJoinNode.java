package it.unibz.inf.ontop.pivotalrepr;

import java.util.Optional;

import it.unibz.inf.ontop.model.ImmutableExpression;
import it.unibz.inf.ontop.model.VariableOrGroundTerm;
import it.unibz.inf.ontop.model.ImmutableSubstitution;

public interface LeftJoinNode extends JoinLikeNode, NonCommutativeOperatorNode {

    @Override
    LeftJoinNode clone();

    @Override
    LeftJoinNode acceptNodeTransformer(HomogeneousQueryNodeTransformer transformer)
            throws QueryNodeTransformationException;

    @Override
    LeftJoinNode changeOptionalFilterCondition(Optional<ImmutableExpression> newOptionalFilterCondition);

    SubstitutionResults<LeftJoinNode> applyAscendentSubstitution(
            ImmutableSubstitution<? extends VariableOrGroundTerm> substitution,
            QueryNode descendantNode, IntermediateQuery query);

    @Override
    SubstitutionResults<LeftJoinNode> applyDescendentSubstitution(
            ImmutableSubstitution<? extends VariableOrGroundTerm> substitution);
}
