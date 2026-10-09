package com.graficadora.model;

import javafx.beans.property.*;
import javafx.scene.paint.Color;
import java.util.function.DoubleUnaryOperator;

/**
 * Modelo reactivo que representa una función matemática en la graficadora.
 */
public class FunctionItem {
    private final String id;
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty expression = new SimpleStringProperty();
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
    private final BooleanProperty visible = new SimpleBooleanProperty(true);
    private final DoubleProperty strokeWidth = new SimpleDoubleProperty(2.5);

    private final BooleanProperty valid = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");

    private DoubleUnaryOperator evaluator = null;

    public FunctionItem(String id, String name, String expression, Color color) {
        this.id = id;
        this.name.set(name);
        this.color.set(color);
        this.expression.set(expression);

        this.expression.addListener((obs, oldVal, newVal) -> compile());
        compile();
    }

    public synchronized void compile() {
        String expr = expression.get();
        if (expr == null || expr.trim().isEmpty()) {
            evaluator = null;
            valid.set(false);
            errorMessage.set("Expresión vacía");
            return;
        }

        try {
            evaluator = MathParser.compile(expr);
            valid.set(true);
            errorMessage.set("");
        } catch (Exception ex) {
            evaluator = null;
            valid.set(false);
            errorMessage.set(ex.getMessage() != null ? ex.getMessage() : "Error de sintaxis");
        }
    }

    public double evaluate(double x) {
        if (!valid.get() || evaluator == null) {
            return Double.NaN;
        }
        try {
            return evaluator.applyAsDouble(x);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    public DoubleUnaryOperator getEvaluator() {
        return this::evaluate;
    }

    // Getters y Properties
    public String getId() { return id; }
    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String name) { this.name.set(name); }

    public String getExpression() { return expression.get(); }
    public StringProperty expressionProperty() { return expression; }
    public void setExpression(String expr) { this.expression.set(expr); }

    public Color getColor() { return color.get(); }
    public ObjectProperty<Color> colorProperty() { return color; }
    public void setColor(Color c) { this.color.set(c); }

    public boolean isVisible() { return visible.get(); }
    public BooleanProperty visibleProperty() { return visible; }
    public void setVisible(boolean v) { this.visible.set(v); }

    public double getStrokeWidth() { return strokeWidth.get(); }
    public DoubleProperty strokeWidthProperty() { return strokeWidth; }
    public void setStrokeWidth(double w) { this.strokeWidth.set(w); }

    public boolean isValid() { return valid.get(); }
    public BooleanProperty validProperty() { return valid; }

    public String getErrorMessage() { return errorMessage.get(); }
    public StringProperty errorMessageProperty() { return errorMessage; }
}
