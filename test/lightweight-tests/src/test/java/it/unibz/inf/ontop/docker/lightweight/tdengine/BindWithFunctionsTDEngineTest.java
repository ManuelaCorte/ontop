package it.unibz.inf.ontop.docker.lightweight.tdengine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.docker.lightweight.AbstractBindTestWithFunctions;
import it.unibz.inf.ontop.docker.lightweight.TDEngineLightWeightTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

@TDEngineLightWeightTest
public class BindWithFunctionsTDEngineTest extends AbstractBindTestWithFunctions {

    private static final String PROPERTIES_FILE = "/tdengine/prop.properties";
    private static final String MAPPING_FILE = "/tdengine/mapping.obda";

    @BeforeAll
    public static void before() throws IOException, SQLException {
        initOBDA(MAPPING_FILE, null, PROPERTIES_FILE);
    }

    @AfterAll
    public static void after() throws SQLException {
        release();
    }

    @Test
    public void simpleTest() {

        String query = "SELECT (COUNT(?s) AS ?count) WHERE { ?s ?p ?o .} ";

        executeAndCompareValues(query, ImmutableSet.of("30"));
    }

}
