package com.graficadora;

import com.graficadora.model.CalculusEngine;
import com.graficadora.model.MathParser;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBAS DE MOTOR MATEMÁTICO ===");

        // 1. MathParser
        assertApprox(MathParser.compile("2x + 1").applyAsDouble(3), 7.0, "2x + 1 evaluado en 3");
        assertApprox(MathParser.compile("sin(pi / 2)").applyAsDouble(0), 1.0, "sin(pi/2)");
        assertApprox(MathParser.compile("(x+2)(x-2)").applyAsDouble(4), 12.0, "Multiplicación implícita (x+2)(x-2)");
        assertApprox(MathParser.compile("exp(-x^2)").applyAsDouble(0), 1.0, "exp(-x^2)");
        assertApprox(MathParser.compile("sqrt(16) + abs(-5)").applyAsDouble(0), 9.0, "sqrt(16) + abs(-5)");

        // 2. CalculusEngine - Derivada
        DoubleUnaryOperator f1 = MathParser.compile("x^2");
        assertApprox(CalculusEngine.derivative(f1, 3.0), 6.0, "Derivada d/dx(x^2) en x=3");

        DoubleUnaryOperator f2 = MathParser.compile("sin(x)");
        assertApprox(CalculusEngine.derivative(f2, 0.0), 1.0, "Derivada d/dx(sin(x)) en x=0");

        // 3. CalculusEngine - Integral
        assertApprox(CalculusEngine.integrate(f1, 0.0, 3.0, 500), 9.0, "Integral ∫[0, 3] x^2 dx");
        assertApprox(CalculusEngine.integrate(f2, 0.0, Math.PI, 500), 2.0, "Integral ∫[0, pi] sin(x) dx");

        // 4. CalculusEngine - Raíces
        DoubleUnaryOperator fRoots = MathParser.compile("x^2 - 4");
        List<CalculusEngine.RootPoint> roots = CalculusEngine.findRoots(fRoots, -5.0, 5.0, 200);
        System.out.println("Raíces encontradas para x^2 - 4: " + roots.size());
        for (var r : roots) {
            System.out.println("  Raíz en x = " + r.x() + ", y = " + r.y());
        }
        if (roots.size() != 2) throw new RuntimeException("Se esperaban 2 raíces para x^2 - 4");

        // 5. CalculusEngine - Extremos
        DoubleUnaryOperator fExt = MathParser.compile("-x^2 + 4");
        List<CalculusEngine.ExtremumPoint> extrema = CalculusEngine.findExtrema(fExt, -5.0, 5.0, 200);
        System.out.println("Extremos encontrados para -x^2 + 4: " + extrema.size());
        for (var ep : extrema) {
            System.out.println("  Extremo: " + (ep.isMaximum() ? "Máx" : "Mín") + " en (" + ep.x() + ", " + ep.y() + ")");
        }
        if (extrema.isEmpty() || !extrema.get(0).isMaximum()) throw new RuntimeException("Se esperaba un máximo en x=0");

        System.out.println("=== ¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE! ===");
    }

    private static void assertApprox(double actual, double expected, String name) {
        if (Math.abs(actual - expected) > 1e-4) {
            throw new AssertionError("FALLO: " + name + " -> esperado " + expected + ", pero fue " + actual);
        }
        System.out.println("✔ OK: " + name + " = " + actual);
    }
}
