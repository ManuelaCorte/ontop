package it.unibz.inf.ontop.model.term.functionsymbol.db.impl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import it.unibz.inf.ontop.model.term.ImmutableFunctionalTerm;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.TermFactory;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBBooleanFunctionSymbol;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBConcatFunctionSymbol;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBFunctionSymbol;
import it.unibz.inf.ontop.model.term.functionsymbol.db.NonDeterministicDBFunctionSymbol;
import it.unibz.inf.ontop.model.type.DBTermType;
import it.unibz.inf.ontop.model.type.DBTypeFactory;
import it.unibz.inf.ontop.model.type.TypeFactory;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unibz.inf.ontop.model.term.functionsymbol.db.impl.MySQLDBFunctionSymbolFactory.UUID_STR;

public class PostgreSQLDBFunctionSymbolFactory extends AbstractSQLDBFunctionSymbolFactory {

    private static final String RANDOM_STR = "RANDOM";

    @Inject
    protected PostgreSQLDBFunctionSymbolFactory(TypeFactory typeFactory) {
        super(createPostgreSQLRegularFunctionTable(typeFactory), typeFactory);
    }

    protected static ImmutableTable<String, Integer, DBFunctionSymbol> createPostgreSQLRegularFunctionTable(
            TypeFactory typeFactory) {
        DBTypeFactory dbTypeFactory = typeFactory.getDBTypeFactory();
        DBTermType abstractRootDBType = dbTypeFactory.getAbstractRootDBType();

        Table<String, Integer, DBFunctionSymbol> table = HashBasedTable.create(
                createDefaultRegularFunctionTable(typeFactory));

        DBFunctionSymbol nowFunctionSymbol = new WithoutParenthesesSimpleTypedDBFunctionSymbolImpl(
                CURRENT_TIMESTAMP_STR,
                dbTypeFactory.getDBDateTimestampType(), abstractRootDBType);
        table.put(CURRENT_TIMESTAMP_STR, 0, nowFunctionSymbol);
        table.remove(REGEXP_LIKE_STR, 2);
        table.remove(REGEXP_LIKE_STR, 3);

        return ImmutableTable.copyOf(table);
    }

    @Override
    protected DBConcatFunctionSymbol createNullRejectingDBConcat(int arity) {
        return createDBConcatOperator(arity);
    }

    @Override
    protected DBConcatFunctionSymbol createDBConcatOperator(int arity) {
        return new NullRejectingDBConcatFunctionSymbol(CONCAT_OP_STR, arity, dbStringType, abstractRootDBType,
                Serializers.getOperatorSerializer(CONCAT_OP_STR));
    }

    @Override
    protected DBConcatFunctionSymbol createRegularDBConcat(int arity) {
        return new NullToleratingDBConcatFunctionSymbol("CONCAT", arity, dbStringType, abstractRootDBType, false);
    }

    @Override
    protected String serializeDateTimeNorm(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        // Enforces ISO 8601: https://stackoverflow.com/questions/38834022/turn-postgres-date-representation-into-iso-8601-string
        return String.format("TO_JSON(%s)#>>'{}'", termConverter.apply(terms.get(0)));
    }

    @Override
    public DBFunctionSymbol getDBSubString2() {
        return getRegularDBFunctionSymbol(SUBSTR_STR, 2);
    }

    @Override
    public DBFunctionSymbol getDBSubString3() {
        return getRegularDBFunctionSymbol(SUBSTR_STR, 3);
    }

    /**
     * TODO: put dashes at the correct positions
     */
    @Override
    public NonDeterministicDBFunctionSymbol getDBUUID(UUID uuid) {
        return new DefaultNonDeterministicNullaryFunctionSymbol(UUID_STR, uuid, dbStringType,
                (terms, termConverter, termFactory) ->
                        "MD5(RANDOM()::text || CLOCK_TIMESTAMP()::text)::uuid");
    }

    @Override
    protected String getRandNameInDialect() {
        return RANDOM_STR;
    }

    @Override
    protected String getUUIDNameInDialect() {
        throw new UnsupportedOperationException("Should not be used for PostgreSQL");
    }

    @Override
    protected String serializeContains(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("(POSITION(%s IN %s) > 0)",
                termConverter.apply(terms.get(1)),
                termConverter.apply(terms.get(0)));
    }

    @Override
    protected String serializeStrBefore(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        String str = termConverter.apply(terms.get(0));
        String before = termConverter.apply(terms.get(1));
        return String.format("LEFT(%s,CAST (SIGN(POSITION(%s IN %s))*(POSITION(%s IN %s)-1) AS INTEGER))", str, before, str, before, str);
    }

    @Override
    protected String serializeStrAfter(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        String str = termConverter.apply(terms.get(0));
        String after = termConverter.apply(terms.get(1));
        return String.format("SUBSTRING(%s,POSITION(%s IN %s) + LENGTH(%s), CAST( SIGN(POSITION(%s IN %s)) * LENGTH(%s) AS INTEGER))",
                str, after, str , after , after, str, str);
    }

    @Override
    protected String serializeMD5(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("MD5(%s)", termConverter.apply(terms.get(0)));
    }

    /**
     * Requires pgcrypto to be enabled (CREATE EXTENSION pgcrypto)
     */
    @Override
    protected String serializeSHA1(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("encode(digest(%s, 'sha1'), 'hex')", termConverter.apply(terms.get(0)));
    }

    /**
     * Requires pgcrypto to be enabled (CREATE EXTENSION pgcrypto)
     */
    @Override
    protected String serializeSHA256(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("encode(digest(%s, 'sha256'), 'hex')", termConverter.apply(terms.get(0)));
    }

    /**
     * Requires pgcrypto to be enabled (CREATE EXTENSION pgcrypto)
     */
    @Override
    protected String serializeSHA512(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("encode(digest(%s, 'sha512'), 'hex')", termConverter.apply(terms.get(0)));
    }

    /**
     * TODO: force the use of two digits for the hour and for the minute
     */
    @Override
    protected String serializeTz(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        String str = termConverter.apply(terms.get(0));
        return String.format("(EXTRACT(TIMEZONE_HOUR FROM %s) || ':' || EXTRACT(TIMEZONE_MINUTE FROM %s))", str, str);
    }

    @Override
    public DBBooleanFunctionSymbol getDBRegexpMatches2() {
        return new DBBooleanFunctionSymbolWithSerializerImpl(REGEXP_LIKE_STR + "2",
                ImmutableList.of(abstractRootDBType, abstractRootDBType), dbBooleanType, false,
                Serializers.getOperatorSerializer("~"));
    }

    @Override
    public DBBooleanFunctionSymbol getDBRegexpMatches3() {
        return new DBBooleanFunctionSymbolWithSerializerImpl(REGEXP_LIKE_STR + "3",
                ImmutableList.of(abstractRootDBType, abstractRootDBType, abstractRootType), dbBooleanType, false,
                /*
                 * TODO: is it safe to assume the flags are not empty?
                 */
                ((terms, termConverter, termFactory) -> {
                    /*
                     * Normalizes the flag
                     *   - DOT_ALL: s -> n
                     */
                    ImmutableTerm flagTerm = termFactory.getDBReplace(terms.get(2),
                            termFactory.getDBStringConstant("s"),
                            termFactory.getDBStringConstant("n"));

                    ImmutableTerm extendedPatternTerm = termFactory.getNullRejectingDBConcatFunctionalTerm(ImmutableList.of(
                            termFactory.getDBStringConstant("(?"),
                            flagTerm,
                            termFactory.getDBStringConstant(")"),
                            terms.get(1)))
                            .simplify();


                    return String.format("%s ~ %s",
                            termConverter.apply(terms.get(0)),
                            termConverter.apply(extendedPatternTerm));
                }));
    }
}
