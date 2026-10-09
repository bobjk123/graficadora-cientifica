package com.graficadora.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Motor de cálculo numérico para análisis de funciones científicas:
 * derivadas, integrales de Simpson, búsqueda de raíces, extremos locales y tablas de valores.
 */
public class CalculusEngine {

    public record RootPoint(double x, double y) {}
    public record ExtremumPoint(double x, double y, boolean isMaximum) {}
    public record TableRow(double x, double y, double derivative) {}

    private static final double H_DERIVATIVE = 1e-5;

    /**
     * Derivada numérica en un punto utilizando plantilla centrada de 5 puntos O(h^4).
     */
    public static double derivative(DoubleUnaryOperator f, double x) {
        double f_p2 = f.applyAsDouble(x + 2 * H_DERIVATIVE);
        double f_p1 = f.applyAsDouble(x + H_DERIVATIVE);
        double f_m1 = f.applyAsDouble(x - H_DERIVATIVE);
        double f_m2 = f.applyAsDouble(x - 2 * H_DERIVATIVE);

        if (Double.isNaN(f_p2) || Double.isNaN(f_p1) || Double.isNaN(f_m1) || Double.isNaN(f_m2)) {
            return Double.NaN;
        }

        return (-f_p2 + 8.0 * f_p1 - 8.0 * f_m1 + f_m2) / (12.0 * H_DERIVATIVE);
    }

    /**
     * Integral definida numérica en [a, b] mediante la regla compuesta de Simpson (1/3).
     */
    public static double integrate(DoubleUnaryOperator f, double a, double b, int n) {
        if (a == b) return 0.0;
        if (a > b) return -integrate(f, b, a, n);

        if (n % 2 != 0) n++; // Simpson requiere número par de intervalos
        if (n < 4) n = 4;

        double h = (b - a) / n;
        double fa = f.applyAsDouble(a);
        double fb = f.applyAsDouble(b);

        if (Double.isNaN(fa) || Double.isInfinite(fa) || Double.isNaN(fb) || Double.isInfinite(fb)) {
            return Double.NaN;
        }

        double sum = fa + fb;

        for (int i = 1; i < n; i++) {
            double x = a + i * h;
            double fx = f.applyAsDouble(x);
            if (Double.isNaN(fx) || Double.isInfinite(fx)) return Double.NaN;

            if (i % 2 == 1) {
                sum += 4.0 * fx;
            } else {
                sum += 2.0 * fx;
            }
        }

        return (h / 3.0) * sum;
    }

    /**
     * Encuentra raíces f(x) = 0 dentro del intervalo [a, b].
     */
    public static List<RootPoint> findRoots(DoubleUnaryOperator f, double a, double b, int samples) {
        List<RootPoint> roots = new ArrayList<>();
        if (a >= b || samples < 10) return roots;

        double step = (b - a) / samples;
        double prevX = a;
        double prevY = f.applyAsDouble(prevX);

        for (int i = 1; i <= samples; i++) {
            double currX = a + i * step;
            double currY = f.applyAsDouble(currX);

            if (Double.isNaN(prevY) || Double.isInfinite(prevY)) {
                prevX = currX;
                prevY = currY;
                continue;
            }

            if (Double.isNaN(currY) || Double.isInfinite(currY)) {
                prevX = currX;
                prevY = currY;
                continue;
            }

            // Detección de asíntota vertical (falso cambio de signo por discontinuidad de salto)
            if (Math.abs(currY - prevY) > 500.0 && Math.signum(currY) != Math.signum(prevY)) {
                prevX = currX;
                prevY = currY;
                continue;
            }

            // Si hay cambio de signo o valor cero
            if (prevY == 0.0) {
                addRootIfUnique(roots, prevX, 0.0);
            } else if (currY == 0.0) {
                addRootIfUnique(roots, currX, 0.0);
            } else if ((prevY > 0 && currY < 0) || (prevY < 0 && currY > 0)) {
                // Refinamiento por bisección
                double rootX = bisectRoot(f, prevX, currX, 60);
                if (!Double.isNaN(rootX)) {
                    double rootY = f.applyAsDouble(rootX);
                    if (Math.abs(rootY) < 1e-4) {
                        addRootIfUnique(roots, rootX, rootY);
                    }
                }
            }

            prevX = currX;
            prevY = currY;
        }

        return roots;
    }

    private static double bisectRoot(DoubleUnaryOperator f, double xLeft, double xRight, int maxIter) {
        double yLeft = f.applyAsDouble(xLeft);
        double yRight = f.applyAsDouble(xRight);

        if (Double.isNaN(yLeft) || Double.isNaN(yRight)) return Double.NaN;
        if (yLeft * yRight > 0) return Double.NaN;

        for (int i = 0; i < maxIter; i++) {
            double mid = 0.5 * (xLeft + xRight);
            double yMid = f.applyAsDouble(mid);

            if (Math.abs(yMid) < 1e-12 || (xRight - xLeft) < 1e-12) {
                return mid;
            }

            if (yLeft * yMid < 0) {
                xRight = mid;
                yRight = yMid;
            } else {
                xLeft = mid;
                yLeft = yMid;
            }
        }
        return 0.5 * (xLeft + xRight);
    }

    private static void addRootIfUnique(List<RootPoint> list, double x, double y) {
        for (RootPoint r : list) {
            if (Math.abs(r.x() - x) < 1e-4) return;
        }
        list.add(new RootPoint(x, y));
    }

    public static double secondDerivative(DoubleUnaryOperator f, double x) {
        double h = 1e-4;
        double fxp = f.applyAsDouble(x + h);
        double fx = f.applyAsDouble(x);
        double fxm = f.applyAsDouble(x - h);
        return (fxp - 2.0 * fx + fxm) / (h * h);
    }

    /**
     * Encuentra máximos y mínimos locales en [a, b] analizando la derivada numérica.
     */
    public static List<ExtremumPoint> findExtrema(DoubleUnaryOperator f, double a, double b, int samples) {
        List<ExtremumPoint> extrema = new ArrayList<>();
        if (a >= b || samples < 20) return extrema;

        double step = (b - a) / samples;
        DoubleUnaryOperator df = x -> derivative(f, x);

        double prevX = a;
        double prevD = df.applyAsDouble(prevX);

        for (int i = 1; i <= samples; i++) {
            double currX = a + i * step;
            double currD = df.applyAsDouble(currX);

            if (Double.isNaN(prevD) || Double.isNaN(currD)) {
                prevX = currX;
                prevD = currD;
                continue;
            }

            // Descartar posibles discontinuidades donde la derivada salta abruptamente
            if (Math.abs(currD - prevD) > 1e4) {
                prevX = currX;
                prevD = currD;
                continue;
            }

            // Caso 1: Punto muestreado con derivada prácticamente nula
            if (Math.abs(currD) < 1e-4) {
                double y = f.applyAsDouble(currX);
                double d2 = secondDerivative(f, currX);
                if (Math.abs(d2) > 1e-5) {
                    addExtremumIfUnique(extrema, currX, y, d2 < 0);
                }
            }
            // Caso 2: Cambio de signo de la derivada (cruza por cero en (prevX, currX))
            else if ((prevD > 0 && currD < 0) || (prevD < 0 && currD > 0)) {
                double rootX = bisectRoot(df, prevX, currX, 40);
                if (!Double.isNaN(rootX)) {
                    double y = f.applyAsDouble(rootX);
                    if (!Double.isNaN(y) && !Double.isInfinite(y)) {
                        boolean isMax = (prevD > 0 && currD < 0);
                        addExtremumIfUnique(extrema, rootX, y, isMax);
                    }
                }
            }

            prevX = currX;
            prevD = currD;
        }

        return extrema;
    }

    private static void addExtremumIfUnique(List<ExtremumPoint> list, double x, double y, boolean isMax) {
        for (ExtremumPoint ep : list) {
            if (Math.abs(ep.x() - x) < 1e-4) return;
        }
        list.add(new ExtremumPoint(x, y, isMax));
    }

    /**
     * Genera tabla de valores discretos [start, end] con paso dado.
     */
    public static List<TableRow> generateTable(DoubleUnaryOperator f, double start, double end, double step) {
        List<TableRow> rows = new ArrayList<>();
        if (step <= 0 || start > end) return rows;

        int count = (int) Math.round((end - start) / step) + 1;
        if (count > 2000) count = 2000; // Limitar para evitar sobrecarga en UI

        for (int i = 0; i < count; i++) {
            double x = start + i * step;
            if (x > end + 1e-9) break;
            double y = f.applyAsDouble(x);
            double d = derivative(f, x);
            rows.add(new TableRow(x, y, d));
        }

        return rows;
    }
}
