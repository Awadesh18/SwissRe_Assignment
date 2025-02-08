package org.employee;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeAnalyzerTest {
    private EmployeeAnalyzer analyzer;

    @BeforeEach
    void setUp() throws IOException {
        analyzer = new EmployeeAnalyzer();
        Path tempFile = Files.createTempFile("employeesTest", ".csv");
        String sampleCsv = """
                Id,firstName,lastName,salary,managerId
                123,Joe,Doe,60000,
                124,Martin,checkov,45000,123
                125,Bob,Ronstad,40000,123
                126,Bob,Doe,38000,123
                127,Boby,checkov,22000,126
                128,Robert,Martin,50000,127
                129,Anderson,jack,51000,128
                130,John,son,52000,129
                131,Michell,marsh,53000,123
                132,Shoun,marsh,54000,123
                133,Virat,sharma,55000,123
                134,Alice,Hasacat,50000,124
                135,Brett,Hardleaf,34000,125
                """;
        Files.writeString(tempFile, sampleCsv);
        analyzer.loadEmployees(tempFile.toString());
    }

    @Test
    @DisplayName("Test if EmployeeAnalyzer loads employees correctly")
    void testLoadEmployees() {
        assertNotNull(analyzer);
    }

    @Test
    @DisplayName("Test if salary analysis detects issues correctly")
    void testAnalyzeSalaries() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        analyzer.analyzeSalaries();

        String output = outputStream.toString();
        assertTrue(output.contains("Manager Bob Doe earns 5000.00 more than allowed (33000.00 max)."));
    }

    @Test
    @DisplayName("Test if reporting line analysis detects deep hierarchies")
    void testAnalyzeReportingLines() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        analyzer.analyzeReportingLines();

        String output = outputStream.toString();
        assertTrue(output.contains("Employee John son has 5 managers (too long by 1 levels)."), "Reporting line analysis should detect excessive depth.");
    }
}

