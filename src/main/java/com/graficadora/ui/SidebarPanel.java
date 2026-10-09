package com.graficadora.ui;

import com.graficadora.model.CalculusEngine;
import com.graficadora.model.FunctionItem;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Panel lateral interactivo con pestañas:
 * - Funciones matemáticas y teclado científico
 * - Herramientas de cálculo (Raíces, Extremos, Derivadas, Integrales)
 * - Tabla de valores discretos
 * - Galería de presets matemáticos
 */
public class SidebarPanel extends VBox {

    private final GraphCanvas graphCanvas;
    private final VBox functionListContainer;
    private TextField activeTextField = null;

    private int functionCounter = 1;
    private final Color[] palette = {
            Color.web("#38BDF8"), // Azul cielo
            Color.web("#F43F5E"), // Rosa/Rojo
            Color.web("#10B981"), // Verde
            Color.web("#F59E0B"), // Ámbar
            Color.web("#8B5CF6"), // Púrpura
            Color.web("#06B6D4")  // Cyan
    };

    // Herramientas de cálculo
    private final ComboBox<FunctionItem> calculusFunctionCombo = new ComboBox<>();
    private final DecimalFormat df;

    public SidebarPanel(GraphCanvas graphCanvas) {
        this.graphCanvas = graphCanvas;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        this.df = new DecimalFormat("0.####", symbols);

        setPrefWidth(380);
        setMinWidth(320);
        setMaxWidth(460);
        getStyleClass().add("sidebar-panel");
        setStyle("-fx-background-color: #1A1D26; -fx-border-color: #2D3242; -fx-border-width: 0 1 0 0;");

        // Pestañas principales
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // 1. Pestaña Funciones
        functionListContainer = new VBox(10);
        functionListContainer.setPadding(new Insets(12));

        Tab tabFunctions = new Tab("Funciones", createFunctionsTabContent());
        Tab tabCalculus = new Tab("Cálculo", createCalculusTabContent());
        Tab tabTable = new Tab("Tabla", createTableTabContent());
        Tab tabPresets = new Tab("Ejemplos", createPresetsTabContent());

        tabPane.getTabs().addAll(tabFunctions, tabCalculus, tabTable, tabPresets);
        getChildren().add(tabPane);

        // Actualizar combo de cálculo cada vez que cambien las funciones
        graphCanvas.getFunctions().addListener((javafx.collections.ListChangeListener<FunctionItem>) c -> {
            updateCalculusCombo();
        });
    }

    private Node createFunctionsTabContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Botón añadir función
        Button btnAdd = new Button("➕ Nueva Función");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> addNewFunction("", getNextColor()));

        // Scroll para lista de funciones
        ScrollPane scroll = new ScrollPane(functionListContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1A1D26; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Teclado matemático rápido
        VBox mathKeyboard = createMathKeyboard();

        root.getChildren().addAll(btnAdd, scroll, mathKeyboard);
        return root;
    }

    public void addNewFunction(String defaultExpr, Color color) {
        String id = "f" + functionCounter;
        String name = "f" + functionCounter + "(x)";
        functionCounter++;

        FunctionItem item = new FunctionItem(id, name, defaultExpr, color);
        graphCanvas.getFunctions().add(item);

        VBox card = createFunctionCard(item);
        functionListContainer.getChildren().add(card);
    }

    private VBox createFunctionCard(FunctionItem item) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #212530; -fx-background-radius: 8; -fx-border-color: #2E3445; -fx-border-radius: 8;");

        // Fila superior: Checkbox, Nombre, ColorPicker, Eliminar
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox chkVisible = new CheckBox();
        chkVisible.selectedProperty().bindBidirectional(item.visibleProperty());
        chkVisible.setStyle("-fx-cursor: hand;");

        Label lblName = new Label(item.getName() + " =");
        lblName.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ColorPicker colorPicker = new ColorPicker(item.getColor());
        colorPicker.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        colorPicker.valueProperty().addListener((obs, oldVal, newVal) -> item.setColor(newVal));

        Button btnDelete = new Button("✕");
        btnDelete.setStyle("-fx-background-color: #3F1D24; -fx-text-fill: #F87171; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> {
            graphCanvas.getFunctions().remove(item);
            functionListContainer.getChildren().remove(card);
        });

        topRow.getChildren().addAll(chkVisible, lblName, spacer, colorPicker, btnDelete);

        // Campo de texto de la fórmula
        TextField txtExpr = new TextField(item.getExpression());
        txtExpr.setPromptText("Ej. sin(x) * x o 2x^2 - 4");
        txtExpr.setStyle("-fx-background-color: #14161D; -fx-text-fill: #F8FAFC; -fx-font-family: 'Consolas'; -fx-font-size: 13px; -fx-padding: 8; -fx-border-color: #33394B; -fx-border-radius: 6; -fx-background-radius: 6;");

        txtExpr.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) activeTextField = txtExpr;
        });

        txtExpr.textProperty().addListener((obs, oldVal, newVal) -> item.setExpression(newVal));

        // Label de error en caso de sintaxis errónea
        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #F87171; -fx-font-size: 11px;");
        lblError.textProperty().bind(item.errorMessageProperty());
        lblError.visibleProperty().bind(item.validProperty().not());
        lblError.managedProperty().bind(item.validProperty().not());

        card.getChildren().addAll(topRow, txtExpr, lblError);
        return card;
    }

    private VBox createMathKeyboard() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: #161821; -fx-background-radius: 8; -fx-border-color: #252A38; -fx-border-radius: 8;");

        Label title = new Label("Símbolos y Funciones Rápidas");
        title.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);

        String[][] buttons = {
                {"sin", "cos", "tan", "sqrt", "x²"},
                {"ln", "log", "e", "pi", "^"},
                {"abs", "(", ")", "/", "*"}
        };

        for (int r = 0; r < buttons.length; r++) {
            for (int c = 0; c < buttons[r].length; c++) {
                String token = buttons[r][c];
                Button btn = new Button(token);
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setStyle("-fx-background-color: #212532; -fx-text-fill: #CBD5E1; -fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-padding: 5; -fx-background-radius: 4; -fx-cursor: hand;");
                GridPane.setHgrow(btn, Priority.ALWAYS);

                btn.setOnAction(e -> insertMathToken(token));
                grid.add(btn, c, r);
            }
        }

        box.getChildren().addAll(title, grid);
        return box;
    }

    private void insertMathToken(String token) {
        if (activeTextField == null) {
            // Si no hay ninguno enfocado, enfocar el primero
            for (javafx.scene.Node node : functionListContainer.getChildren()) {
                if (node instanceof VBox card) {
                    for (javafx.scene.Node sub : card.getChildren()) {
                        if (sub instanceof TextField tf) {
                            activeTextField = tf;
                            break;
                        }
                    }
                }
            }
        }

        if (activeTextField != null) {
            String insert = switch (token) {
                case "sin" -> "sin(";
                case "cos" -> "cos(";
                case "tan" -> "tan(";
                case "sqrt" -> "sqrt(";
                case "ln" -> "ln(";
                case "log" -> "log(";
                case "abs" -> "abs(";
                case "x²" -> "^2";
                case "pi" -> "pi";
                default -> token;
            };

            int caret = activeTextField.getCaretPosition();
            String current = activeTextField.getText();
            String updated = current.substring(0, caret) + insert + current.substring(caret);
            activeTextField.setText(updated);
            activeTextField.positionCaret(caret + insert.length());
            activeTextField.requestFocus();
        }
    }

    private Node createCalculusTabContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        Label lblSelect = new Label("Seleccionar Función a Analizar:");
        lblSelect.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        calculusFunctionCombo.setMaxWidth(Double.MAX_VALUE);
        calculusFunctionCombo.setStyle("-fx-background-color: #212530; -fx-text-fill: white; -fx-padding: 4;");
        calculusFunctionCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FunctionItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + ": " + item.getExpression());
            }
        });
        calculusFunctionCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(FunctionItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sin función" : item.getName() + ": " + item.getExpression());
            }
        });

        // 1. Raíces f(x) = 0
        VBox rootBox = new VBox(6);
        rootBox.setPadding(new Insets(8));
        rootBox.setStyle("-fx-background-color: #212530; -fx-background-radius: 6;");

        Label lblRootTitle = new Label("Raíces (Ceros) en [a, b]");
        lblRootTitle.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");

        HBox rootInputs = new HBox(6);
        TextField txtRootA = new TextField("-10");
        TextField txtRootB = new TextField("10");
        styleMiniInput(txtRootA);
        styleMiniInput(txtRootB);
        Button btnCalcRoots = new Button("Calcular");
        styleActionButton(btnCalcRoots, "#059669");

        rootInputs.getChildren().addAll(new Label("a:"), txtRootA, new Label("b:"), txtRootB, btnCalcRoots);

        TextArea txtRootResult = new TextArea();
        txtRootResult.setPrefRowCount(3);
        txtRootResult.setEditable(false);
        styleResultArea(txtRootResult);

        btnCalcRoots.setOnAction(e -> {
            FunctionItem item = calculusFunctionCombo.getValue();
            if (item != null && item.isValid()) {
                try {
                    double a = Double.parseDouble(txtRootA.getText());
                    double b = Double.parseDouble(txtRootB.getText());
                    List<CalculusEngine.RootPoint> roots = CalculusEngine.findRoots(item.getEvaluator(), a, b, 600);
                    graphCanvas.setMarkedRoots(roots);

                    if (roots.isEmpty()) {
                        txtRootResult.setText("No se encontraron raíces en el intervalo.");
                    } else {
                        StringBuilder sb = new StringBuilder("Raíces encontradas (" + roots.size() + "):\n");
                        for (CalculusEngine.RootPoint r : roots) {
                            sb.append(String.format(Locale.US, "• x = %s\n", df.format(r.x())));
                        }
                        txtRootResult.setText(sb.toString().trim());
                    }
                } catch (Exception ex) {
                    txtRootResult.setText("Error en los límites del intervalo.");
                }
            }
        });

        rootBox.getChildren().addAll(lblRootTitle, rootInputs, txtRootResult);

        // 2. Extremos (Máximos y Mínimos)
        VBox extBox = new VBox(6);
        extBox.setPadding(new Insets(8));
        extBox.setStyle("-fx-background-color: #212530; -fx-background-radius: 6;");

        Label lblExtTitle = new Label("Extremos Locales en [a, b]");
        lblExtTitle.setStyle("-fx-text-fill: #EC4899; -fx-font-weight: bold;");

        HBox extInputs = new HBox(6);
        TextField txtExtA = new TextField("-10");
        TextField txtExtB = new TextField("10");
        styleMiniInput(txtExtA);
        styleMiniInput(txtExtB);
        Button btnCalcExt = new Button("Calcular");
        styleActionButton(btnCalcExt, "#DB2777");

        extInputs.getChildren().addAll(new Label("a:"), txtExtA, new Label("b:"), txtExtB, btnCalcExt);

        TextArea txtExtResult = new TextArea();
        txtExtResult.setPrefRowCount(3);
        txtExtResult.setEditable(false);
        styleResultArea(txtExtResult);

        btnCalcExt.setOnAction(e -> {
            FunctionItem item = calculusFunctionCombo.getValue();
            if (item != null && item.isValid()) {
                try {
                    double a = Double.parseDouble(txtExtA.getText());
                    double b = Double.parseDouble(txtExtB.getText());
                    List<CalculusEngine.ExtremumPoint> extrema = CalculusEngine.findExtrema(item.getEvaluator(), a, b, 600);
                    graphCanvas.setMarkedExtrema(extrema);

                    if (extrema.isEmpty()) {
                        txtExtResult.setText("No se encontraron extremos locales.");
                    } else {
                        StringBuilder sb = new StringBuilder("Extremos encontrados (" + extrema.size() + "):\n");
                        for (CalculusEngine.ExtremumPoint ep : extrema) {
                            String type = ep.isMaximum() ? "Máx" : "Mín";
                            sb.append(String.format(Locale.US, "• %s en (%s, %s)\n", type, df.format(ep.x()), df.format(ep.y())));
                        }
                        txtExtResult.setText(sb.toString().trim());
                    }
                } catch (Exception ex) {
                    txtExtResult.setText("Error en los límites del intervalo.");
                }
            }
        });

        extBox.getChildren().addAll(lblExtTitle, extInputs, txtExtResult);

        // 3. Recta Tangente y Derivada
        VBox tanBox = new VBox(6);
        tanBox.setPadding(new Insets(8));
        tanBox.setStyle("-fx-background-color: #212530; -fx-background-radius: 6;");

        Label lblTanTitle = new Label("Derivada y Recta Tangente en x₀");
        lblTanTitle.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");

        HBox tanInputs = new HBox(6);
        TextField txtX0 = new TextField("1.0");
        styleMiniInput(txtX0);
        Button btnCalcTan = new Button("Trazar Tangente");
        styleActionButton(btnCalcTan, "#D97706");

        tanInputs.getChildren().addAll(new Label("x₀:"), txtX0, btnCalcTan);

        Label lblTanResult = new Label("f'(x₀) = —");
        lblTanResult.setStyle("-fx-text-fill: #FDE68A; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");

        btnCalcTan.setOnAction(e -> {
            FunctionItem item = calculusFunctionCombo.getValue();
            if (item != null && item.isValid()) {
                try {
                    double x0 = Double.parseDouble(txtX0.getText());
                    double slope = CalculusEngine.derivative(item.getEvaluator(), x0);
                    double y0 = item.evaluate(x0);
                    lblTanResult.setText(String.format(Locale.US, "f(%s) = %s\nf'(%s) = %s (Pendiente)", df.format(x0), df.format(y0), df.format(x0), df.format(slope)));
                    graphCanvas.setTangentTool(item, x0, true);
                } catch (Exception ex) {
                    lblTanResult.setText("Error al evaluar x₀.");
                }
            }
        });

        tanBox.getChildren().addAll(lblTanTitle, tanInputs, lblTanResult);

        // 4. Integral Definida
        VBox intBox = new VBox(6);
        intBox.setPadding(new Insets(8));
        intBox.setStyle("-fx-background-color: #212530; -fx-background-radius: 6;");

        Label lblIntTitle = new Label("Integral Definida ∫ f(x) dx");
        lblIntTitle.setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold;");

        HBox intInputs = new HBox(6);
        TextField txtIntA = new TextField("0");
        TextField txtIntB = new TextField("3.1416");
        styleMiniInput(txtIntA);
        styleMiniInput(txtIntB);
        Button btnCalcInt = new Button("Calcular Área");
        styleActionButton(btnCalcInt, "#0284C7");

        intInputs.getChildren().addAll(new Label("a:"), txtIntA, new Label("b:"), txtIntB, btnCalcInt);

        Label lblIntResult = new Label("∫ = —");
        lblIntResult.setStyle("-fx-text-fill: #BAE6FD; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");

        btnCalcInt.setOnAction(e -> {
            FunctionItem item = calculusFunctionCombo.getValue();
            if (item != null && item.isValid()) {
                try {
                    double a = Double.parseDouble(txtIntA.getText());
                    double b = Double.parseDouble(txtIntB.getText());
                    double result = CalculusEngine.integrate(item.getEvaluator(), a, b, 1000);
                    lblIntResult.setText(String.format(Locale.US, "∫[%s a %s] = %s", df.format(a), df.format(b), df.format(result)));
                    graphCanvas.setIntegralTool(item, a, b, true);
                } catch (Exception ex) {
                    lblIntResult.setText("Error en los límites de integración.");
                }
            }
        });

        intBox.getChildren().addAll(lblIntTitle, intInputs, lblIntResult);

        // Botón limpiar marcadores
        Button btnClearMarkers = new Button("Limpiar Análisis Gráfico");
        btnClearMarkers.setMaxWidth(Double.MAX_VALUE);
        btnClearMarkers.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-padding: 7; -fx-background-radius: 6; -fx-cursor: hand;");
        btnClearMarkers.setOnAction(e -> graphCanvas.clearMarkers());

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1A1D26; -fx-background-color: transparent; -fx-border-color: transparent;");

        root.getChildren().addAll(lblSelect, calculusFunctionCombo, rootBox, extBox, tanBox, intBox, btnClearMarkers);
        return scroll;
    }

    private Node createTableTabContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        Label lblTitle = new Label("Generación de Tabla de Valores");
        lblTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        HBox controls = new HBox(6);
        controls.setAlignment(Pos.CENTER_LEFT);

        TextField txtStart = new TextField("-5");
        TextField txtEnd = new TextField("5");
        TextField txtStep = new TextField("0.5");
        styleMiniInput(txtStart);
        styleMiniInput(txtEnd);
        styleMiniInput(txtStep);

        Button btnGenerate = new Button("Generar");
        styleActionButton(btnGenerate, "#2563EB");

        controls.getChildren().addAll(new Label("Inicio:"), txtStart, new Label("Fin:"), txtEnd, new Label("Paso:"), txtStep, btnGenerate);

        TableView<CalculusEngine.TableRow> table = new TableView<>();
        table.setStyle("-fx-background-color: #212530;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<CalculusEngine.TableRow, String> colX = new TableColumn<>("x");
        colX.setCellValueFactory(data -> new SimpleStringProperty(df.format(data.getValue().x())));
        colX.setPrefWidth(90);

        TableColumn<CalculusEngine.TableRow, String> colY = new TableColumn<>("f(x)");
        colY.setCellValueFactory(data -> new SimpleStringProperty(df.format(data.getValue().y())));
        colY.setPrefWidth(120);

        TableColumn<CalculusEngine.TableRow, String> colD = new TableColumn<>("f'(x)");
        colD.setCellValueFactory(data -> new SimpleStringProperty(df.format(data.getValue().derivative())));
        colD.setPrefWidth(110);

        table.getColumns().addAll(colX, colY, colD);

        btnGenerate.setOnAction(e -> {
            FunctionItem item = calculusFunctionCombo.getValue();
            if (item != null && item.isValid()) {
                try {
                    double start = Double.parseDouble(txtStart.getText());
                    double end = Double.parseDouble(txtEnd.getText());
                    double step = Double.parseDouble(txtStep.getText());
                    List<CalculusEngine.TableRow> rows = CalculusEngine.generateTable(item.getEvaluator(), start, end, step);
                    table.setItems(FXCollections.observableArrayList(rows));
                } catch (Exception ex) {
                    // Ignore
                }
            }
        });

        root.getChildren().addAll(lblTitle, controls, table);
        return root;
    }

    private Node createPresetsTabContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        Label lblTitle = new Label("Galería de Funciones Notables");
        lblTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        VBox list = new VBox(8);
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1A1D26; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        record Preset(String title, String desc, String expr, double xMin, double xMax, double yMin, double yMax) {}

        List<Preset> presets = List.of(
                new Preset("Onda Amortiguada", "Oscilación con decaimiento exponencial", "exp(-0.2*x) * sin(3*x)", -2, 12, -1.5, 1.5),
                new Preset("Campana de Gauss", "Distribución normal estándar", "exp(-x^2)", -4, 4, -0.2, 1.2),
                new Preset("Función Sinc", "Procesamiento de señales sen(x)/x", "sin(x) / x", -15, 15, -0.5, 1.2),
                new Preset("Polinomio Cúbico", "Tres raíces y dos puntos críticos", "x^3 - 3*x", -3, 3, -4, 4),
                new Preset("Batido de Ondas", "Interferencia constructiva y destructiva", "sin(5*x) + sin(5.5*x)", -10, 10, -2.5, 2.5),
                new Preset("Asíntotas de Tangente", "Discontinuidades periódicas", "tan(x)", -8, 8, -6, 6),
                new Preset("Función Racional", "Asíntota horizontal y simetría par", "(x^2 - 1) / (x^2 + 1)", -6, 6, -1.5, 1.5),
                new Preset("Modulación Absoluta", "Puntas angulosas no diferenciables", "abs(x) * cos(x)", -10, 10, -8, 8)
        );

        for (Preset p : presets) {
            VBox item = new VBox(4);
            item.setPadding(new Insets(8));
            item.setStyle("-fx-background-color: #212530; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #2E3445; -fx-border-radius: 6;");

            Label name = new Label(p.title);
            name.setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold; -fx-font-size: 12px;");

            Label desc = new Label(p.desc);
            desc.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

            Label expr = new Label("f(x) = " + p.expr);
            expr.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'Consolas'; -fx-font-size: 11px;");

            item.getChildren().addAll(name, desc, expr);

            item.setOnMouseClicked(e -> {
                graphCanvas.getFunctions().clear();
                functionListContainer.getChildren().clear();
                functionCounter = 1;

                addNewFunction(p.expr, palette[0]);
                graphCanvas.setViewport(p.xMin, p.xMax, p.yMin, p.yMax);
            });

            list.getChildren().add(item);
        }

        root.getChildren().addAll(lblTitle, scroll);
        return root;
    }

    private void updateCalculusCombo() {
        FunctionItem selected = calculusFunctionCombo.getValue();
        calculusFunctionCombo.getItems().setAll(graphCanvas.getFunctions());
        if (!graphCanvas.getFunctions().isEmpty()) {
            if (selected != null && graphCanvas.getFunctions().contains(selected)) {
                calculusFunctionCombo.setValue(selected);
            } else {
                calculusFunctionCombo.setValue(graphCanvas.getFunctions().get(0));
            }
        }
    }

    private Color getNextColor() {
        return palette[(functionCounter - 1) % palette.length];
    }

    private void styleMiniInput(TextField tf) {
        tf.setPrefWidth(55);
        tf.setStyle("-fx-background-color: #14161D; -fx-text-fill: white; -fx-font-family: 'Consolas'; -fx-padding: 4; -fx-border-color: #33394B; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void styleActionButton(Button btn, String colorHex) {
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand;");
    }

    private void styleResultArea(TextArea ta) {
        ta.setStyle("-fx-control-inner-background: #14161D; -fx-text-fill: #CBD5E1; -fx-font-family: 'Consolas'; -fx-font-size: 11px;");
    }
}
