package com.graficadora.ui;

import com.graficadora.model.CalculusEngine;
import com.graficadora.model.FunctionItem;
import javafx.animation.AnimationTimer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lienzo interactivo de alto rendimiento para graficación científica en 2D.
 * Incluye rejilla adaptativa, navegación fluida (pan/zoom), trazado de múltiples curvas,
 * sombreado de integrales, rectas tangentes interactivas y detección de singularidades.
 */
public class GraphCanvas extends Pane {

    private final Canvas canvas;
    private final GraphicsContext gc;

    // Límites matemáticos del viewport
    private double xMin = -10.0;
    private double xMax = 10.0;
    private double yMin = -7.0;
    private double yMax = 7.0;

    // Lista de funciones a graficar
    private final ObservableList<FunctionItem> functions = FXCollections.observableArrayList();

    // Estado del ratón y navegación
    private double dragStartX, dragStartY;
    private double dragStartMinX, dragStartMaxX, dragStartMinY, dragStartMaxY;
    private double mouseScreenX = -1, mouseScreenY = -1;
    private boolean isMouseInside = false;

    // Herramientas analíticas activas
    private FunctionItem integralFunction = null;
    private double integralA = 0, integralB = 0;
    private boolean showIntegral = false;

    private FunctionItem tangentFunction = null;
    private double tangentX0 = 0;
    private boolean showTangent = false;

    private final List<CalculusEngine.RootPoint> markedRoots = new ArrayList<>();
    private final List<CalculusEngine.ExtremumPoint> markedExtrema = new ArrayList<>();

    // Propiedades para la barra de estado
    private final StringProperty cursorCoordinatesText = new SimpleStringProperty("X: 0.0000 | Y: 0.0000");

    // Formateadores numéricos
    private final DecimalFormat tickFormat;
    private final DecimalFormat coordFormat;

    private boolean needsRedraw = true;

    public GraphCanvas() {
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();

        getChildren().add(canvas);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        this.tickFormat = new DecimalFormat("0.###", symbols);
        this.coordFormat = new DecimalFormat("0.0000", symbols);

        // Autoajuste de tamaño
        widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            requestRedraw();
        });
        heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            requestRedraw();
        });

        // Eventos de ratón para Pan y Zoom
        setupMouseInteractions();

        // Escuchar cambios en la lista de funciones
        functions.addListener((ListChangeListener<FunctionItem>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (FunctionItem f : c.getAddedSubList()) {
                        f.expressionProperty().addListener((o, ov, nv) -> requestRedraw());
                        f.colorProperty().addListener((o, ov, nv) -> requestRedraw());
                        f.visibleProperty().addListener((o, ov, nv) -> requestRedraw());
                        f.strokeWidthProperty().addListener((o, ov, nv) -> requestRedraw());
                    }
                }
            }
            requestRedraw();
        });

        // Bucle de renderizado eficiente (solo cuando es necesario)
        AnimationTimer renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (needsRedraw) {
                    draw();
                    needsRedraw = false;
                }
            }
        };
        renderLoop.start();
    }

    private void setupMouseInteractions() {
        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.MIDDLE) {
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragStartMinX = xMin;
                dragStartMaxX = xMax;
                dragStartMinY = yMin;
                dragStartMaxY = yMax;
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.MIDDLE) {
                double w = getWidth();
                double h = getHeight();
                if (w <= 0 || h <= 0) return;

                double dxPixels = e.getX() - dragStartX;
                double dyPixels = e.getY() - dragStartY;

                double mathDx = dxPixels * (dragStartMaxX - dragStartMinX) / w;
                double mathDy = dyPixels * (dragStartMaxY - dragStartMinY) / h;

                xMin = dragStartMinX - mathDx;
                xMax = dragStartMaxX - mathDx;
                yMin = dragStartMinY + mathDy;
                yMax = dragStartMaxY + mathDy;

                updateMouseCoordinates(e.getX(), e.getY());
                requestRedraw();
            }
        });

        canvas.setOnScroll((ScrollEvent e) -> {
            double zoomFactor = (e.getDeltaY() > 0) ? 0.85 : 1.18;
            zoomAt(e.getX(), e.getY(), zoomFactor);
        });

        canvas.setOnMouseMoved(e -> {
            isMouseInside = true;
            mouseScreenX = e.getX();
            mouseScreenY = e.getY();
            updateMouseCoordinates(mouseScreenX, mouseScreenY);
            requestRedraw();
        });

        canvas.setOnMouseExited(e -> {
            isMouseInside = false;
            mouseScreenX = -1;
            mouseScreenY = -1;
            requestRedraw();
        });

        canvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                // Doble clic: centrar en el punto cliqueado
                double mathX = toMathX(e.getX());
                double mathY = toMathY(e.getY());
                centerAt(mathX, mathY);
            }
        });
    }

    private void updateMouseCoordinates(double screenX, double screenY) {
        double mx = toMathX(screenX);
        double my = toMathY(screenY);
        cursorCoordinatesText.set(String.format(Locale.US, "X: %s | Y: %s", coordFormat.format(mx), coordFormat.format(my)));
    }

    public void zoomAt(double pivotScreenX, double pivotScreenY, double factor) {
        double pivotMathX = toMathX(pivotScreenX);
        double pivotMathY = toMathY(pivotScreenY);

        xMin = pivotMathX + (xMin - pivotMathX) * factor;
        xMax = pivotMathX + (xMax - pivotMathX) * factor;
        yMin = pivotMathY + (yMin - pivotMathY) * factor;
        yMax = pivotMathY + (yMax - pivotMathY) * factor;

        requestRedraw();
    }

    public void zoomIn() {
        zoomAt(getWidth() / 2.0, getHeight() / 2.0, 0.75);
    }

    public void zoomOut() {
        zoomAt(getWidth() / 2.0, getHeight() / 2.0, 1.33);
    }

    public void centerAt(double cx, double cy) {
        double halfW = (xMax - xMin) / 2.0;
        double halfH = (yMax - yMin) / 2.0;
        xMin = cx - halfW;
        xMax = cx + halfW;
        yMin = cy - halfH;
        yMax = cy + halfH;
        requestRedraw();
    }

    public void resetView() {
        double w = getWidth();
        double h = getHeight();
        double aspect = (w > 0 && h > 0) ? (w / h) : (16.0 / 9.0);

        double halfY = 8.0;
        double halfX = halfY * aspect;

        xMin = -halfX;
        xMax = halfX;
        yMin = -halfY;
        yMax = halfY;
        requestRedraw();
    }

    public void setViewport(double minX, double maxX, double minY, double maxY) {
        this.xMin = minX;
        this.xMax = maxX;
        this.yMin = minY;
        this.yMax = maxY;
        requestRedraw();
    }

    public void requestRedraw() {
        this.needsRedraw = true;
    }

    // Conversiones de coordenadas
    public double toScreenX(double mathX) {
        double w = getWidth();
        return (mathX - xMin) / (xMax - xMin) * w;
    }

    public double toScreenY(double mathY) {
        double h = getHeight();
        return (yMax - mathY) / (yMax - yMin) * h;
    }

    public double toMathX(double screenX) {
        double w = getWidth();
        return xMin + (screenX / w) * (xMax - xMin);
    }

    public double toMathY(double screenY) {
        double h = getHeight();
        return yMax - (screenY / h) * (yMax - yMin);
    }

    // Renderizado principal
    private void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Fondo oscuro moderno
        gc.setFill(Color.web("#14161D"));
        gc.fillRect(0, 0, w, h);

        // 2. Cuadrícula y ejes
        drawGridAndAxes(w, h);

        // 3. Sombreado de integral si está activo
        if (showIntegral && integralFunction != null && integralFunction.isValid()) {
            drawIntegralArea(w, h);
        }

        // 4. Trazado de las funciones matemáticas
        for (FunctionItem f : functions) {
            if (f.isVisible() && f.isValid()) {
                drawFunctionCurve(f, w, h);
            }
        }

        // 5. Recta tangente si está activa
        if (showTangent && tangentFunction != null && tangentFunction.isValid()) {
            drawTangentLine(w, h);
        }

        // 6. Marcadores analíticos (raíces y extremos)
        drawAnalyticalMarkers();

        // 7. Retícula y coordenadas del cursor (HUD)
        if (isMouseInside && mouseScreenX >= 0 && mouseScreenY >= 0) {
            drawCursorCrosshairAndValues(w, h);
        }
    }

    private void drawGridAndAxes(double w, double h) {
        double rangeX = xMax - xMin;
        double rangeY = yMax - yMin;

        double stepX = calculateStep(rangeX, w);
        double stepY = calculateStep(rangeY, h);

        // Sub-rejilla fina (opcional pero aporta elegancia visual)
        double subStepX = stepX / 5.0;
        double subStepY = stepY / 5.0;

        gc.setLineWidth(0.6);
        gc.setStroke(Color.web("#1F232E"));

        double firstSubX = Math.floor(xMin / subStepX) * subStepX;
        for (double x = firstSubX; x <= xMax; x += subStepX) {
            double sx = toScreenX(x);
            gc.strokeLine(sx, 0, sx, h);
        }

        double firstSubY = Math.floor(yMin / subStepY) * subStepY;
        for (double y = firstSubY; y <= yMax; y += subStepY) {
            double sy = toScreenY(y);
            gc.strokeLine(0, sy, w, sy);
        }

        // Rejilla principal
        gc.setLineWidth(1.0);
        gc.setStroke(Color.web("#2C3242"));

        double firstMajorX = Math.floor(xMin / stepX) * stepX;
        for (double x = firstMajorX; x <= xMax; x += stepX) {
            double sx = toScreenX(x);
            gc.strokeLine(sx, 0, sx, h);
        }

        double firstMajorY = Math.floor(yMin / stepY) * stepY;
        for (double y = firstMajorY; y <= yMax; y += stepY) {
            double sy = toScreenY(y);
            gc.strokeLine(0, sy, w, sy);
        }

        // Ejes X e Y
        double screenY0 = toScreenY(0.0);
        double screenX0 = toScreenX(0.0);

        gc.setLineWidth(2.0);
        gc.setStroke(Color.web("#7A869E"));

        // Eje X
        if (screenY0 >= 0 && screenY0 <= h) {
            gc.strokeLine(0, screenY0, w, screenY0);
        }
        // Eje Y
        if (screenX0 >= 0 && screenX0 <= w) {
            gc.strokeLine(screenX0, 0, screenX0, h);
        }

        // Etiquetas numéricas de los ejes
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        gc.setFill(Color.web("#94A3B8"));

        // Etiquetas en X
        double labelY = Math.min(Math.max(screenY0 + 16, 20), h - 10);
        for (double x = firstMajorX; x <= xMax; x += stepX) {
            if (Math.abs(x) < stepX * 0.1) continue; // No dibujar 0 dos veces
            double sx = toScreenX(x);
            String text = tickFormat.format(x);
            gc.fillText(text, sx - 10, labelY);
        }

        // Etiquetas en Y
        double labelX = Math.min(Math.max(screenX0 + 8, 10), w - 50);
        for (double y = firstMajorY; y <= yMax; y += stepY) {
            if (Math.abs(y) < stepY * 0.1) continue;
            double sy = toScreenY(y);
            String text = tickFormat.format(y);
            gc.fillText(text, labelX, sy + 4);
        }

        // Etiqueta origen (0,0)
        if (screenX0 >= 0 && screenX0 <= w && screenY0 >= 0 && screenY0 <= h) {
            gc.fillText("0", screenX0 + 5, screenY0 + 14);
        }
    }

    private double calculateStep(double range, double pixelSize) {
        if (range <= 0 || pixelSize <= 0) return 1.0;
        double targetDivisions = Math.max(4, pixelSize / 90.0);
        double rawStep = range / targetDivisions;
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double residual = rawStep / magnitude;

        double factor;
        if (residual < 1.5) {
            factor = 1.0;
        } else if (residual < 3.5) {
            factor = 2.0;
        } else if (residual < 7.5) {
            factor = 5.0;
        } else {
            factor = 10.0;
        }

        return factor * magnitude;
    }

    private void drawFunctionCurve(FunctionItem f, double w, double h) {
        gc.setLineWidth(f.getStrokeWidth());
        gc.setStroke(f.getColor());

        double stepPx = 1.0; // Muestreo de 1 píxel por paso
        boolean inPath = false;
        double prevY = Double.NaN;
        double prevSy = Double.NaN;

        gc.beginPath();

        for (double sx = 0; sx <= w; sx += stepPx) {
            double mathX = toMathX(sx);
            double mathY = f.evaluate(mathX);

            if (Double.isNaN(mathY) || Double.isInfinite(mathY)) {
                if (inPath) {
                    gc.stroke();
                    gc.beginPath();
                    inPath = false;
                }
                prevY = Double.NaN;
                prevSy = Double.NaN;
                continue;
            }

            double sy = toScreenY(mathY);

            // Detección de salto/asíntota vertical (evita conectar líneas falsas de -inf a +inf)
            boolean isDiscontinuity = false;
            if (!Double.isNaN(prevY)) {
                double dy = Math.abs(mathY - prevY);
                double dSy = Math.abs(sy - prevSy);
                // Si el salto en pantalla es mayor al 80% de la altura y los signos de Y son opuestos
                if (dSy > h * 0.7 && (mathY * prevY < 0 || dy > (yMax - yMin) * 2.0)) {
                    isDiscontinuity = true;
                }
            }

            if (isDiscontinuity) {
                gc.stroke();
                gc.beginPath();
                gc.moveTo(sx, sy);
                inPath = true;
            } else if (!inPath) {
                gc.moveTo(sx, sy);
                inPath = true;
            } else {
                gc.lineTo(sx, sy);
            }

            prevY = mathY;
            prevSy = sy;
        }

        if (inPath) {
            gc.stroke();
        }
    }

    private void drawIntegralArea(double w, double h) {
        double startX = Math.min(integralA, integralB);
        double endX = Math.max(integralA, integralB);

        double screenStartX = toScreenX(startX);
        double screenEndX = toScreenX(endX);
        double screenY0 = toScreenY(0.0);

        Color baseColor = integralFunction.getColor();
        gc.setFill(Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.3));

        gc.beginPath();
        gc.moveTo(screenStartX, screenY0);

        double stepPx = 1.5;
        for (double sx = Math.max(0, screenStartX); sx <= Math.min(w, screenEndX); sx += stepPx) {
            double mx = toMathX(sx);
            double my = integralFunction.evaluate(mx);
            if (!Double.isNaN(my) && !Double.isInfinite(my)) {
                gc.lineTo(sx, toScreenY(my));
            }
        }

        gc.lineTo(screenEndX, screenY0);
        gc.closePath();
        gc.fill();

        // Líneas límite verticales
        gc.setLineWidth(1.5);
        gc.setStroke(Color.web("#38BDF8"));
        gc.strokeLine(screenStartX, 0, screenStartX, h);
        gc.strokeLine(screenEndX, 0, screenEndX, h);

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#38BDF8"));
        gc.fillText("a = " + tickFormat.format(startX), screenStartX + 5, 25);
        gc.fillText("b = " + tickFormat.format(endX), screenEndX + 5, 45);
    }

    private void drawTangentLine(double w, double h) {
        double y0 = tangentFunction.evaluate(tangentX0);
        if (Double.isNaN(y0) || Double.isInfinite(y0)) return;

        double m = CalculusEngine.derivative(tangentFunction.getEvaluator(), tangentX0);
        if (Double.isNaN(m) || Double.isInfinite(m)) return;

        // Recta: y = y0 + m * (x - x0)
        double yLeft = y0 + m * (xMin - tangentX0);
        double yRight = y0 + m * (xMax - tangentX0);

        double sx1 = toScreenX(xMin);
        double sy1 = toScreenY(yLeft);
        double sx2 = toScreenX(xMax);
        double sy2 = toScreenY(yRight);

        // Línea tangente punteada brillante
        gc.setLineWidth(2.0);
        gc.setStroke(Color.web("#F59E0B"));
        gc.setLineDashes(6.0, 4.0);
        gc.strokeLine(sx1, sy1, sx2, sy2);
        gc.setLineDashes(null); // Restaurar sólido

        // Punto de tangencia
        double ptX = toScreenX(tangentX0);
        double ptY = toScreenY(y0);

        gc.setFill(Color.web("#F59E0B"));
        gc.fillOval(ptX - 6, ptY - 6, 12, 12);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(ptX - 6, ptY - 6, 12, 12);

        // Texto con la pendiente
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#FDE68A"));
        gc.fillText(String.format(Locale.US, "m = f'(x₀) = %s", tickFormat.format(m)), ptX + 10, ptY - 10);
    }

    private void drawAnalyticalMarkers() {
        // Raíces f(x) = 0
        for (CalculusEngine.RootPoint r : markedRoots) {
            double sx = toScreenX(r.x());
            double sy = toScreenY(r.y());

            gc.setFill(Color.web("#10B981")); // Verde esmeralda
            gc.fillOval(sx - 5, sy - 5, 10, 10);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(sx - 5, sy - 5, 10, 10);

            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
            gc.setFill(Color.web("#A7F3D0"));
            gc.fillText("x₀ = " + tickFormat.format(r.x()), sx + 8, sy - 8);
        }

        // Extremos (máximos y mínimos)
        for (CalculusEngine.ExtremumPoint ep : markedExtrema) {
            double sx = toScreenX(ep.x());
            double sy = toScreenY(ep.y());

            Color col = ep.isMaximum() ? Color.web("#EC4899") : Color.web("#8B5CF6");
            gc.setFill(col);
            gc.fillOval(sx - 6, sy - 6, 12, 12);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(sx - 6, sy - 6, 12, 12);

            String label = ep.isMaximum() ? "Máx" : "Mín";
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
            gc.setFill(Color.WHITE);
            gc.fillText(String.format(Locale.US, "%s (%s, %s)", label, tickFormat.format(ep.x()), tickFormat.format(ep.y())), sx + 8, sy - 8);
        }
    }

    private void drawCursorCrosshairAndValues(double w, double h) {
        double mathX = toMathX(mouseScreenX);

        // Líneas guía tenues
        gc.setLineWidth(1.0);
        gc.setStroke(Color.color(1, 1, 1, 0.25));
        gc.setLineDashes(4.0, 4.0);
        gc.strokeLine(mouseScreenX, 0, mouseScreenX, h);
        gc.strokeLine(0, mouseScreenY, w, mouseScreenY);
        gc.setLineDashes(null);

        // Puntos sobre cada función visible y datos para la tarjeta HUD
        List<String> hudLines = new ArrayList<>();
        hudLines.add("x = " + coordFormat.format(mathX));

        for (FunctionItem f : functions) {
            if (f.isVisible() && f.isValid()) {
                double valY = f.evaluate(mathX);
                if (!Double.isNaN(valY) && !Double.isInfinite(valY)) {
                    double sy = toScreenY(valY);

                    // Punto brillante sobre la curva
                    gc.setFill(f.getColor());
                    gc.fillOval(mouseScreenX - 5, sy - 5, 10, 10);
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(1.5);
                    gc.strokeOval(mouseScreenX - 5, sy - 5, 10, 10);

                    hudLines.add(String.format(Locale.US, "%s = %s", f.getName(), coordFormat.format(valY)));
                }
            }
        }

        // Dibujar caja HUD flotante
        double boxW = 180;
        double lineH = 18;
        double boxH = hudLines.size() * lineH + 16;
        double boxX = mouseScreenX + 15;
        double boxY = mouseScreenY + 15;

        // Mantener dentro del lienzo
        if (boxX + boxW > w - 10) boxX = mouseScreenX - boxW - 15;
        if (boxY + boxH > h - 10) boxY = mouseScreenY - boxH - 15;

        gc.setFill(Color.rgb(20, 24, 33, 0.92));
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        gc.setStroke(Color.rgb(100, 116, 139, 0.4));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 8, 8);

        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        double textY = boxY + 18;
        for (int i = 0; i < hudLines.size(); i++) {
            gc.setFill(i == 0 ? Color.web("#94A3B8") : Color.web("#38BDF8"));
            gc.fillText(hudLines.get(i), boxX + 10, textY);
            textY += lineH;
        }
    }

    // Configuración de herramientas
    public void setIntegralTool(FunctionItem f, double a, double b, boolean active) {
        this.integralFunction = f;
        this.integralA = a;
        this.integralB = b;
        this.showIntegral = active;
        requestRedraw();
    }

    public void setTangentTool(FunctionItem f, double x0, boolean active) {
        this.tangentFunction = f;
        this.tangentX0 = x0;
        this.showTangent = active;
        requestRedraw();
    }

    public void setMarkedRoots(List<CalculusEngine.RootPoint> roots) {
        this.markedRoots.clear();
        if (roots != null) this.markedRoots.addAll(roots);
        requestRedraw();
    }

    public void setMarkedExtrema(List<CalculusEngine.ExtremumPoint> extrema) {
        this.markedExtrema.clear();
        if (extrema != null) this.markedExtrema.addAll(extrema);
        requestRedraw();
    }

    public void clearMarkers() {
        this.markedRoots.clear();
        this.markedExtrema.clear();
        this.showIntegral = false;
        this.showTangent = false;
        requestRedraw();
    }

    // Getters y Colecciones
    public ObservableList<FunctionItem> getFunctions() {
        return functions;
    }

    public StringProperty cursorCoordinatesTextProperty() {
        return cursorCoordinatesText;
    }

    public double getXMin() { return xMin; }
    public double getXMax() { return xMax; }
    public double getYMin() { return yMin; }
    public double getYMax() { return yMax; }
}
