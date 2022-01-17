package it.unibz.inf.ontop.iq.optimizer;

import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.dbschema.*;
import it.unibz.inf.ontop.dbschema.impl.DatabaseTableDefinition;
import it.unibz.inf.ontop.dbschema.impl.OfflineMetadataProviderBuilder;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.iq.exception.EmptyQueryException;
import it.unibz.inf.ontop.iq.node.*;
import it.unibz.inf.ontop.model.atom.AtomPredicate;
import it.unibz.inf.ontop.model.atom.DistinctVariableOnlyDataAtom;
import it.unibz.inf.ontop.model.term.DBConstant;
import it.unibz.inf.ontop.model.term.ImmutableExpression;
import it.unibz.inf.ontop.model.term.Variable;
import it.unibz.inf.ontop.model.type.DBTermType;
import org.junit.Test;

import java.util.Optional;

import static it.unibz.inf.ontop.OptimizationTestingTools.*;
import static junit.framework.TestCase.assertEquals;

public class FlattenLiftTest {

    private final static NamedRelationDefinition TABLE1;
    private final static NamedRelationDefinition TABLE2;
    private final static NamedRelationDefinition TABLE3;
    private final static NamedRelationDefinition TABLE4;


    private final static Variable A = TERM_FACTORY.getVariable("A");
    private final static Variable A1 = TERM_FACTORY.getVariable("A1");
    private final static Variable A2 = TERM_FACTORY.getVariable("A2");
    private final static Variable B = TERM_FACTORY.getVariable("B");
    private final static Variable B1 = TERM_FACTORY.getVariable("B1");
    private final static Variable B2 = TERM_FACTORY.getVariable("B2");
    private final static Variable C = TERM_FACTORY.getVariable("C");
    private final static Variable C1 = TERM_FACTORY.getVariable("C1");
    private final static Variable C2 = TERM_FACTORY.getVariable("C2");
    private final static Variable C3 = TERM_FACTORY.getVariable("C3");
    private final static Variable C4 = TERM_FACTORY.getVariable("C4");
    private final static Variable D = TERM_FACTORY.getVariable("D");
    private final static Variable D1 = TERM_FACTORY.getVariable("D1");
    private final static Variable D2 = TERM_FACTORY.getVariable("D2");
    private final static Variable E = TERM_FACTORY.getVariable("E");
    private final static Variable F = TERM_FACTORY.getVariable("F");
    private final static Variable G = TERM_FACTORY.getVariable("G");
    private final static Variable X = TERM_FACTORY.getVariable("X");
    private final static Variable Y = TERM_FACTORY.getVariable("Y");
    private final static Variable Z = TERM_FACTORY.getVariable("Z");

    private final static DBConstant ONE = TERM_FACTORY.getDBConstant("1", TYPE_FACTORY.getDBTypeFactory().getDBLargeIntegerType());
    private final static DBConstant TWO = TERM_FACTORY.getDBConstant("2", TYPE_FACTORY.getDBTypeFactory().getDBLargeIntegerType());

    static {




        OfflineMetadataProviderBuilder builder = createMetadataProviderBuilder();
        DBTermType integerDBType = builder.getDBTypeFactory().getDBLargeIntegerType();
        DBTermType arrayDBType = builder.getDBTypeFactory().getArrayDBType();

        TABLE1 = builder.createDatabaseRelation( "TABLE1",
                "pk", integerDBType, false,
                "arr", arrayDBType, true,
                "col3", integerDBType, true);
        UniqueConstraint.primaryKeyOf(TABLE1.getAttribute(1));

        TABLE2 = builder.createDatabaseRelation( "TABLE2",
                "pk", integerDBType, false,
                "col2", integerDBType, true);
        UniqueConstraint.primaryKeyOf(TABLE1.getAttribute(1));

        TABLE3 = builder.createDatabaseRelation( "TABLE3",
                "pk", integerDBType, false,
                "arr", arrayDBType, true);
        UniqueConstraint.primaryKeyOf(TABLE3.getAttribute(1));

        TABLE4 = builder.createDatabaseRelation( "TABLE4",
                "pk", integerDBType, false,
                "arr1", arrayDBType, true,
                "arr2", arrayDBType, true,
                "arr3", arrayDBType, true,
                "arr4", arrayDBType, true);
        UniqueConstraint.primaryKeyOf(TABLE3.getAttribute(1));

    }


    @Test
    public void testFlattenWithoutFilteringCondition1() throws EmptyQueryException {

        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(
                ATOM_FACTORY.getRDFAnswerPredicate(2), X, Y);

        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        InnerJoinNode joinNode = IQ_FACTORY.createInnerJoinNode();
        ExtensionalDataNode leftDataNode = createExtensionalDataNode(TABLE2, ImmutableList.of(X,B));
        FlattenNode flattenNode = IQ_FACTORY.createFlattenNode(Y, F, Optional.empty(), true);
        ExtensionalDataNode rightDataNode = createExtensionalDataNode(TABLE1, ImmutableList.of(X,F,C));

        IQ initialIQ = IQ_FACTORY.createIQ(projectionAtom,
                IQ_FACTORY.createUnaryIQTree(
                        rootNode,
                        IQ_FACTORY.createNaryIQTree(
                                joinNode,
                                ImmutableList.of(
                                        leftDataNode,
                                        IQ_FACTORY.createUnaryIQTree(
                                                flattenNode,
                                                rightDataNode
                                        )))));

        IQ expectedIQ = IQ_FACTORY.createIQ(projectionAtom,
                IQ_FACTORY.createUnaryIQTree(
                        rootNode,
                        IQ_FACTORY.createUnaryIQTree(
                                flattenNode,
                                IQ_FACTORY.createNaryIQTree(
                                        joinNode,
                                        ImmutableList.of(
                                        leftDataNode,
                                                rightDataNode
                                        )))));

        optimizeAndCompare(initialIQ, expectedIQ);
    }

    @Test
    public void testFlattenWithoutFilteringCondition2() throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(
                ATOM_FACTORY.getRDFAnswerPredicate(2), X, Y);

        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());

        InnerJoinNode joinNode = IQ_FACTORY.createInnerJoinNode(TERM_FACTORY.getStrictEquality(C, ONE));
        ExtensionalDataNode leftDataNode = createExtensionalDataNode(TABLE2, ImmutableList.of(X,B));
        FlattenNode flattenNode = IQ_FACTORY.createFlattenNode(Y, F, Optional.empty(), true);
        ExtensionalDataNode rightDataNode = createExtensionalDataNode(TABLE1, ImmutableList.of(X, F, C));

        IQ initialIQ = IQ_FACTORY.createIQ(projectionAtom,
                IQ_FACTORY.createUnaryIQTree(
                        rootNode,
                        IQ_FACTORY.createNaryIQTree(
                                joinNode,
                                ImmutableList.of(
                                        leftDataNode,
                                        IQ_FACTORY.createUnaryIQTree(
                                                flattenNode,
                                                rightDataNode
                                        )))));

        IQ expectedIQ = IQ_FACTORY.createIQ(projectionAtom,
                IQ_FACTORY.createUnaryIQTree(
                        rootNode,
                        IQ_FACTORY.createUnaryIQTree(
                                flattenNode,
                                IQ_FACTORY.createNaryIQTree(
                                        joinNode,
                                        ImmutableList.of(
                                                leftDataNode,
                                                rightDataNode
                                        )))));

        optimizeAndCompare(initialIQ, expectedIQ);
    }

    @Test
    public void testFlattenWithFilteringCondition1() throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS2_PREDICATE, X, Y);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        ImmutableExpression expression = TERM_FACTORY.getStrictEquality(E, ONE);
        InnerJoinNode joinNode = IQ_FACTORY.createInnerJoinNode(expression);
        ExtensionalDataNode leftDataNode = createExtensionalDataNode(TABLE2, ImmutableList.of(X,B));
        FlattenNode flattenNode = IQ_FACTORY.createFlattenNode(Y, F, Optional.empty(), true);
        ExtensionalDataNode rightDataNode = createExtensionalDataNode(TABLE1, ImmutableList.of(X, F, C));

        IQ initialIQ = IQ_FACTORY.createIQ(projectionAtom,
                IQ_FACTORY.createUnaryIQTree(
                        rootNode,
                        IQ_FACTORY.createNaryIQTree(
                                joinNode,
                                ImmutableList.of(
                                        leftDataNode,
                                        IQ_FACTORY.createUnaryIQTree(
                                                flattenNode,
                                                rightDataNode
                                        )))));

        FilterNode filterNode = IQ_FACTORY.createFilterNode(expression);
        InnerJoinNode newJoinNode = IQ_FACTORY.createInnerJoinNode();

        IQ expectedIQ = IQ_FACTORY.createIQ(projectionAtom,
                IQ_FACTORY.createUnaryIQTree(
                        rootNode,
                        IQ_FACTORY.createUnaryIQTree(
                                filterNode,
                                IQ_FACTORY.createUnaryIQTree(
                                flattenNode,
                                        IQ_FACTORY.createNaryIQTree(
                                                newJoinNode,
                                                ImmutableList.of(
                                                        leftDataNode,
                                                        rightDataNode
                                        ))))));

        optimizeAndCompare(initialIQ, expectedIQ);
    }

    @Test
    public void testDoubleFlattenWithoutFilteringCondition1() throws EmptyQueryException {

        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS2_PREDICATE, X, Y);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        InnerJoinNode joinNode = IQ_FACTORY.createInnerJoinNode();
        ExtensionalDataNode leftDataNode = createExtensionalDataNode(TABLE2, ImmutableList.of(X,B));
        FlattenNode level2FlattenNode = IQ_FACTORY.createFlattenNode(Y, F1, Optional.empty(), true);
        FlattenNode level1FlattenNode = IQ_FACTORY.createFlattenNode(F1,F2,Optional.empty(), true);
        ExtensionalDataNode rightDataNode = createExtensionalDataNode(TABLE2, ImmutableList.of(X, F2, C));

        IQ initialIQ = IQ_FACTORY.createIQ(projectionAtom,
                IQ_FACTORY.createUnaryIQTree(
                        rootNode,
                        IQ_FACTORY.createNaryIQTree(
                                joinNode,
                                ImmutableList.of(
                                        leftDataNode,
                                        IQ_FACTORY.createUnaryIQTree(
                                                level2FlattenNode,
                                                IQ_FACTORY.createUnaryIQTree(
                                                        level1FlattenNode,
                                                        rightDataNode
                                        ))))));

        IQ expectedIQ = IQ_FACTORY.createIQ(projectionAtom,
                IQ_FACTORY.createUnaryIQTree(
                        rootNode,
                        IQ_FACTORY.createUnaryIQTree(
                                level2FlattenNode,
                                IQ_FACTORY.createUnaryIQTree(
                                        level1FlattenNode,
                                        IQ_FACTORY.createNaryIQTree(
                                                joinNode,
                                                ImmutableList.of(
                                                        leftDataNode,
                                                        rightDataNode
                                                ))))));

        optimizeAndCompare(initialIQ, expectedIQ);
    }

    @Test
    public void testFlattenJoinFilterOnLeft1() throws EmptyQueryException {
        testFlattenJoinFilterOnLeft(F);
    }

    @Test
    public void testFlattenJoinFilterOnLeft2() throws EmptyQueryException {
        testFlattenJoinFilterOnLeft(D);
    }

    private void testFlattenJoinFilterOnLeft(Variable rightNestedVariable) throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS2_PREDICATE, X, Y);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        ImmutableExpression expression = TERM_FACTORY.getStrictEquality(E, ONE);
        InnerJoinNode joinNode = IQ_FACTORY.createInnerJoinNode(expression);
        FlattenNode leftFlattenNode = IQ_FACTORY.createFlattenNode(A,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR3, Y, D, E));
        ExtensionalDataNode leftDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE1_PREDICATE, X, A, C));
        FlattenNode rightFlattenNode = IQ_FACTORY.createFlattenNode(B,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, Z, rightNestedVariable));
        ExtensionalDataNode rightDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE3_PREDICATE, X, B));

        queryBuilder.init(projectionAtom, rootNode);

        queryBuilder.addChild(rootNode, joinNode);

        queryBuilder.addChild(joinNode, leftFlattenNode);

        queryBuilder.addChild(leftFlattenNode, leftDataNode);

        queryBuilder.addChild(joinNode, rightFlattenNode);

        queryBuilder.addChild(rightFlattenNode, rightDataNode);

        IntermediateQuery query = queryBuilder.build();


        FilterNode filterNode = IQ_FACTORY.createFilterNode(expression);
        InnerJoinNode newJoinNode = IQ_FACTORY.createInnerJoinNode();
        expectedQueryBuilder.addChild(rootNode, rightFlattenNode);

        expectedQueryBuilder.addChild(rightFlattenNode, filterNode);

        expectedQueryBuilder.addChild(filterNode, leftFlattenNode);

        expectedQueryBuilder.addChild(leftFlattenNode, newJoinNode);
        expectedQueryBuilder.addChild(newJoinNode, leftDataNode);
        expectedQueryBuilder.addChild(newJoinNode, rightDataNode);


        optimizeAndCompare(query, expectedQuery);
    }

    @Test
    public void testFlattenJoinNonBlockingFilterOnLeft() throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS2_PREDICATE, X, Y);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        InnerJoinNode joinNode = IQ_FACTORY.createInnerJoinNode(TERM_FACTORY.getStrictEquality(C, ONE));
        FlattenNode leftFlattenNode = IQ_FACTORY.createFlattenNode(A,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR3, Y, D, C));
        ExtensionalDataNode leftDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE1_PREDICATE, X, A, C));
        FlattenNode rightFlattenNode = IQ_FACTORY.createFlattenNode(B,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, Z, F));
        ExtensionalDataNode rightDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE3_PREDICATE, X, B));
        queryBuilder.init(projectionAtom, rootNode);

        queryBuilder.addChild(rootNode, joinNode);

        queryBuilder.addChild(joinNode, leftFlattenNode);

        queryBuilder.addChild(leftFlattenNode, leftDataNode);

        queryBuilder.addChild(joinNode, rightFlattenNode);

        queryBuilder.addChild(rightFlattenNode, rightDataNode);



        expectedQueryBuilder.addChild(rootNode, rightFlattenNode );
        expectedQueryBuilder.addChild(rightFlattenNode, leftFlattenNode);

        expectedQueryBuilder.addChild(leftFlattenNode, joinNode);
        expectedQueryBuilder.addChild(joinNode, leftDataNode);
        expectedQueryBuilder.addChild(joinNode, rightDataNode);

        IntermediateQuery expectedQuery = expectedQueryBuilder.build();

        optimizeAndCompare(query, expectedQuery);
    }


    @Test
    public void testFlattenLeftJoinNoImplicitExpression() throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS2_PREDICATE, X, Y);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        LeftJoinNode leftJoinNode = IQ_FACTORY.createLeftJoinNode();
        FlattenNode leftFlattenNode = IQ_FACTORY.createFlattenNode(A,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR3, Y, D, E));
        ExtensionalDataNode leftDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE1_PREDICATE, X, A, C));
        FlattenNode rightFlattenNode = IQ_FACTORY.createFlattenNode(B,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, Z, F));
        ExtensionalDataNode rightDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE3_PREDICATE, X, B));

        queryBuilder.init(projectionAtom, rootNode);

        queryBuilder.addChild(rootNode, leftJoinNode);

        queryBuilder.addChild(leftJoinNode, leftFlattenNode, LEFT);

        queryBuilder.addChild(leftFlattenNode, leftDataNode);

        queryBuilder.addChild(leftJoinNode, rightFlattenNode, RIGHT);

        queryBuilder.addChild(rightFlattenNode, rightDataNode);



        expectedQueryBuilder.init(projectionAtom, rootNode);
        expectedQueryBuilder.addChild(rootNode, leftFlattenNode);

        expectedQueryBuilder.addChild(leftFlattenNode, leftJoinNode);

        expectedQueryBuilder.addChild(leftJoinNode,  leftDataNode, LEFT);
        expectedQueryBuilder.addChild(leftJoinNode, rightFlattenNode, RIGHT);
        expectedQueryBuilder.addChild(rightFlattenNode, rightDataNode);

        IntermediateQuery expectedQuery = expectedQueryBuilder.build();

        optimizeAndCompare(query, expectedQuery);
    }

    @Test
    public void testFlattenLeftJoinImplicitExpression1() throws EmptyQueryException {
        testFlattenLeftJoinImplicitExpression(E, X);
    }

    @Test
    public void testFlattenLeftJoinImplicitExpression2() throws EmptyQueryException {
        testFlattenLeftJoinImplicitExpression(F, Y);
    }

    private void testFlattenLeftJoinImplicitExpression(Variable v1, Variable v2) throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS2_PREDICATE, X, Y);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        LeftJoinNode leftJoinNode = IQ_FACTORY.createLeftJoinNode();
        FlattenNode leftFlattenNode = IQ_FACTORY.createFlattenNode(A,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR3, Y, D, E));
        ExtensionalDataNode leftDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE1_PREDICATE, X, A, C));
        FlattenNode rightFlattenNode = IQ_FACTORY.createFlattenNode(B,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, Z, v1));
        ExtensionalDataNode rightDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE3_PREDICATE, v2, B));
        queryBuilder.init(projectionAtom, rootNode);

        queryBuilder.addChild(rootNode, leftJoinNode);

        queryBuilder.addChild(leftJoinNode, leftFlattenNode, LEFT);

        queryBuilder.addChild(leftFlattenNode, leftDataNode);

        queryBuilder.addChild(leftJoinNode, rightFlattenNode, RIGHT);

        queryBuilder.addChild(rightFlattenNode, rightDataNode);

        IntermediateQuery query = queryBuilder.build();

        IntermediateQuery expectedQuery = query.createSnapshot();

        optimizeAndCompare(query, expectedQuery);
    }

    @Test
    public void testFlattenLeftJoinNonBlockingImplicitExpression() throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS2_PREDICATE, X, Y);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        LeftJoinNode leftJoinNode = IQ_FACTORY.createLeftJoinNode();
        FlattenNode leftFlattenNode = IQ_FACTORY.createFlattenNode(A,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR3, Y, D, C));
        ExtensionalDataNode leftDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE1_PREDICATE, X, A, C));
        FlattenNode rightFlattenNode = IQ_FACTORY.createFlattenNode(B,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, Z, C));
        ExtensionalDataNode rightDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE3_PREDICATE, X, B));
        queryBuilder.init(projectionAtom, rootNode);

        queryBuilder.addChild(rootNode, leftJoinNode);

        queryBuilder.addChild(leftJoinNode, leftFlattenNode, LEFT);

        queryBuilder.addChild(leftFlattenNode, leftDataNode);

        queryBuilder.addChild(leftJoinNode, rightFlattenNode, RIGHT);

        queryBuilder.addChild(rightFlattenNode, rightDataNode);

        IntermediateQuery query = queryBuilder.build();

        expectedQueryBuilder.init(projectionAtom, rootNode);
        expectedQueryBuilder.addChild(rootNode, leftFlattenNode);

        expectedQueryBuilder.addChild(leftFlattenNode, leftJoinNode);

        expectedQueryBuilder.addChild(leftJoinNode,  leftDataNode, LEFT);
        expectedQueryBuilder.addChild(leftJoinNode, rightFlattenNode, RIGHT);
        expectedQueryBuilder.addChild(rightFlattenNode, rightDataNode);

        IntermediateQuery expectedQuery = expectedQueryBuilder.build();

        optimizeAndCompare(query, expectedQuery);
    }

    @Test
    public void testFlattenLeftJoinNonBlockingExpression() throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS2_PREDICATE, X, Y);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        LeftJoinNode leftJoinNode = IQ_FACTORY.createLeftJoinNode(TERM_FACTORY.getStrictEquality(C, ONE));
        FlattenNode leftFlattenNode = IQ_FACTORY.createFlattenNode(A,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR3, Y, D, E));
        ExtensionalDataNode leftDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE1_PREDICATE, X, A, C));
        FlattenNode rightFlattenNode = IQ_FACTORY.createFlattenNode(B,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, Z, F));
        ExtensionalDataNode rightDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE3_PREDICATE, X, B));
        queryBuilder.init(projectionAtom, rootNode);

        queryBuilder.addChild(rootNode, leftJoinNode);

        queryBuilder.addChild(leftJoinNode, leftFlattenNode, LEFT);

        queryBuilder.addChild(leftFlattenNode, leftDataNode);

        queryBuilder.addChild(leftJoinNode, rightFlattenNode, RIGHT);

        queryBuilder.addChild(rightFlattenNode, rightDataNode);



        IntermediateQueryBuilder expectedQueryBuilder = createQueryBuilder();
        expectedQueryBuilder.init(projectionAtom, rootNode);
        expectedQueryBuilder.addChild(rootNode, leftFlattenNode);

        expectedQueryBuilder.addChild(leftFlattenNode, leftJoinNode);

        expectedQueryBuilder.addChild(leftJoinNode,  leftDataNode, LEFT);
        expectedQueryBuilder.addChild(leftJoinNode, rightFlattenNode, RIGHT);
        expectedQueryBuilder.addChild(rightFlattenNode, rightDataNode);

        IntermediateQuery expectedQuery = expectedQueryBuilder.build();

        optimizeAndCompare(query, expectedQuery);
    }

    @Test
    public void testFlattenLeftJoinBlockingExpression1() throws EmptyQueryException {
        testFlattenLeftJoinBlockingExpression(TERM_FACTORY.getStrictEquality(X, E));
    }

    @Test
    public void testFlattenLeftJoinBlockingExpression2() throws EmptyQueryException {
        testFlattenLeftJoinBlockingExpression(TERM_FACTORY.getStrictEquality(Y, E));
    }

    @Test
    public void testFlattenLeftJoinBlockingExpression3() throws EmptyQueryException {
        testFlattenLeftJoinBlockingExpression(TERM_FACTORY.getStrictEquality(Z, E));
    }

    @Test
    public void testConsecutiveFlatten1() throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS4_PREDICATE, A2, B1, C4, D1);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        FilterNode filter = IQ_FACTORY.createFilterNode(TERM_FACTORY.getStrictEquality(A1, C3));
        FlattenNode flatten1 = IQ_FACTORY.createFlattenNode(
                A,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, A1, A2)
        );
        FlattenNode flatten2 = IQ_FACTORY.createFlattenNode(
                B,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, B1, B2)
        );
        FlattenNode flatten3 = IQ_FACTORY.createFlattenNode(
                C1,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, C3, C4)
        );
        FlattenNode flatten4 = IQ_FACTORY.createFlattenNode(
                D,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, D1, D2)
        );
        FlattenNode flatten5 = IQ_FACTORY.createFlattenNode(
                C,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, C1, C2)
        );

        ExtensionalDataNode dataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE4_PREDICATE, X, A, B, C, D));

        queryBuilder.init(projectionAtom, rootNode);

        queryBuilder.addChild(rootNode, filter);
        queryBuilder.addChild(filter, flatten1);
        queryBuilder.addChild(flatten1, flatten2);
        queryBuilder.addChild(flatten2, flatten3);
        queryBuilder.addChild(flatten3, flatten4);
        queryBuilder.addChild(flatten4, flatten5);
        queryBuilder.addChild(flatten5, dataNode);



        IntermediateQueryBuilder expectedQueryBuilder = createQueryBuilder();
        expectedQueryBuilder.init(projectionAtom, rootNode);
        expectedQueryBuilder.addChild(rootNode, flatten4);
        expectedQueryBuilder.addChild(flatten4, flatten2);
        expectedQueryBuilder.addChild(flatten2, filter);
        expectedQueryBuilder.addChild(filter, flatten3);
        expectedQueryBuilder.addChild(flatten3, flatten5);
        expectedQueryBuilder.addChild(flatten5, flatten1);
        expectedQueryBuilder.addChild(flatten1, dataNode);

        IntermediateQuery expectedQuery = expectedQueryBuilder.build();

        optimizeAndCompare(query, expectedQuery);
    }


    @Test
    public void testConsecutiveFlatten2() throws EmptyQueryException {

        ImmutableExpression exp1 = TERM_FACTORY.getStrictEquality(A1, ONE);
        ImmutableExpression exp2 = TERM_FACTORY.getStrictEquality(C3, TWO);
        ImmutableExpression exp3 = TERM_FACTORY.getConjunction(exp1, exp2);

        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS4_PREDICATE, A2, B1, C4, D1);

        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());

        FilterNode filter3 = IQ_FACTORY.createFilterNode(exp3);
        FlattenNode flatten1 = IQ_FACTORY.createFlattenNode(
                A,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, A1, A2)
        );
        FlattenNode flatten2 = IQ_FACTORY.createFlattenNode(
                B,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, B1, B2)
        );
        FlattenNode flatten3 = IQ_FACTORY.createFlattenNode(
                C1,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, C3, C4)
        );
        FlattenNode flatten4 = IQ_FACTORY.createFlattenNode(
                D,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, D1, D2)
        );
        FlattenNode flatten5 = IQ_FACTORY.createFlattenNode(
                C,
                0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, C1, C2)
        );

        ExtensionalDataNode dataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE4_PREDICATE, X, A, B, C, D));

        queryBuilder.init(projectionAtom, rootNode);

        queryBuilder.addChild(rootNode, filter3);
        queryBuilder.addChild(filter3, flatten1);
        queryBuilder.addChild(flatten1, flatten2);
        queryBuilder.addChild(flatten2, flatten3);
        queryBuilder.addChild(flatten3, flatten4);
        queryBuilder.addChild(flatten4, flatten5);
        queryBuilder.addChild(flatten5, dataNode);




        FilterNode filter1 = IQ_FACTORY.createFilterNode(exp1);
        FilterNode filter2 = IQ_FACTORY.createFilterNode(exp2);

        IntermediateQueryBuilder expectedQueryBuilder = createQueryBuilder();
        expectedQueryBuilder.init(projectionAtom, rootNode);
        expectedQueryBuilder.addChild(rootNode, flatten4);
        expectedQueryBuilder.addChild(flatten4, flatten2);
        expectedQueryBuilder.addChild(flatten2, filter1);
        expectedQueryBuilder.addChild(filter1, flatten1);
        expectedQueryBuilder.addChild(flatten1, filter2);
        expectedQueryBuilder.addChild(filter2, flatten3);
        expectedQueryBuilder.addChild(flatten3, flatten5);
        expectedQueryBuilder.addChild(flatten5, dataNode);

        IntermediateQuery expectedQuery = expectedQueryBuilder.build();

        optimizeAndCompare(query, expectedQuery);
    }

    private void testFlattenLeftJoinBlockingExpression(ImmutableExpression expression) throws EmptyQueryException {
        DistinctVariableOnlyDataAtom projectionAtom = ATOM_FACTORY.getDistinctVariableOnlyDataAtom(ANS2_PREDICATE, X, Y);
        ConstructionNode rootNode = IQ_FACTORY.createConstructionNode(projectionAtom.getVariables());
        LeftJoinNode leftJoinNode = IQ_FACTORY.createLeftJoinNode(expression);
        FlattenNode leftFlattenNode = IQ_FACTORY.createFlattenNode(A,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR3, Y, D, E));
        ExtensionalDataNode leftDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE1_PREDICATE, X, A, C));
        FlattenNode rightFlattenNode = IQ_FACTORY.createFlattenNode(B,0,
                ATOM_FACTORY.getDataAtom(FLATTEN_NODE_PRED_AR2, Z, F));
        ExtensionalDataNode rightDataNode = IQ_FACTORY.createExtensionalDataNode(
                ATOM_FACTORY.getDataAtom(TABLE3_PREDICATE, X, B));
        queryBuilder.init(projectionAtom, rootNode);

        queryBuilder.addChild(rootNode, leftJoinNode);

        queryBuilder.addChild(leftJoinNode, leftFlattenNode, LEFT);

        queryBuilder.addChild(leftFlattenNode, leftDataNode);

        queryBuilder.addChild(leftJoinNode, rightFlattenNode, RIGHT);

        queryBuilder.addChild(rightFlattenNode, rightDataNode);

        optimizeAndCompare(query, query.createSnapshot());
    }

    private static void optimizeAndCompare(IQ query, IQ expectedQuery) {
        System.out.println("\nBefore optimization: \n" +  query);
        System.out.println("\nExpected: \n" +  expectedQuery);

        IQ optimizedIQ = FLATTEN_LIFTER.optimize(query);
        System.out.println("\nAfter optimization: \n" +  optimizedIQ);

        assertEquals(expectedQuery, optimizedIQ);
    }
}
