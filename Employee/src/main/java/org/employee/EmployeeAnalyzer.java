package org.employee;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EmployeeAnalyzer {
    private final Map<Integer, Employee> employees = new HashMap<>();
    private final Map<Integer, List<Employee>> managerToEmployees = new HashMap<>();
    private Integer ceoId = null;

    public record Employee(int id, String firstName, String lastName, double salary, Integer managerId) {
        public String getFullName() { return firstName + " " + lastName; }
    }

    public void loadEmployees(String filePath) throws IOException {
        try (var lines = Files.lines(Path.of(filePath))) {
            lines.skip(1).forEach(line -> {
                var parts = line.split(",");
                var id = Integer.parseInt(parts[0]);
                var firstName = parts[1];
                var lastName = parts[2];
                var salary = Double.parseDouble(parts[3]);
                var managerId = parts.length > 4 && !parts[4].isEmpty() ? Integer.parseInt(parts[4]) : null;

                var emp = new Employee(id, firstName, lastName, salary, managerId);
                employees.put(id, emp);
                if (managerId == null) ceoId = id;
                managerToEmployees.computeIfAbsent(managerId, k -> Collections.synchronizedList(new ArrayList<>())).add(emp);
            });
        }
    }

    public void analyzeSalaries() {
        managerToEmployees.entrySet().parallelStream().forEach(entry -> {
            var managerId = entry.getKey();
            if (managerId == null) return;

            var manager = employees.get(managerId);
            var subordinates = entry.getValue();
            var avgSubordinateSalary = subordinates.stream().mapToDouble(Employee::salary).average().orElse(0);
            var minSalary = avgSubordinateSalary * 1.2;
            var maxSalary = avgSubordinateSalary * 1.5;

            if (manager.salary() < minSalary) {
                System.out.printf("Manager %s earns %.2f less than required (%.2f min).%n",
                        manager.getFullName(), minSalary - manager.salary(), minSalary);
            } else if (manager.salary() > maxSalary) {
                System.out.printf("Manager %s earns %.2f more than allowed (%.2f max).%n",
                        manager.getFullName(), manager.salary() - maxSalary, maxSalary);
            }
        });
    }

    public void analyzeReportingLines() {
        if (ceoId == null) return;

        var queue = new ArrayDeque<Employee>();
        var depthMap = new ConcurrentHashMap<Integer, Integer>();
        queue.add(employees.get(ceoId));
        depthMap.put(ceoId, 0);

        while (!queue.isEmpty()) {
            var current = queue.poll();
            var currentDepth = depthMap.get(current.id());

            if (managerToEmployees.containsKey(current.id())) {
                for (var subordinate : managerToEmployees.get(current.id())) {
                    depthMap.put(subordinate.id(), currentDepth + 1);
                    queue.add(subordinate);

                    if (currentDepth + 1 > 4) {
                        System.out.printf("Employee %s has %d managers (too long by %d levels).%n",
                                subordinate.getFullName(), currentDepth + 1, (currentDepth + 1) - 4);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        var analyzer = new EmployeeAnalyzer();
        analyzer.loadEmployees("employees1.csv");
        System.out.println("=== Salary Analysis ===");
        analyzer.analyzeSalaries();
        System.out.println("\n=== Reporting Line Analysis ===");
        analyzer.analyzeReportingLines();
    }
}

