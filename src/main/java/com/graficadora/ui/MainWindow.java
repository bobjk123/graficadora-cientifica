package com.graficadora.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Ventana principal que integra la barra de herramientas superior,
 * el panel lateral con pestañas, el lienzo de graficación central
 * y la barra de estado inferior.
 */
public class MainWindow extends BorderPane {

    private final GraphCanvas graphCanvas;
    private final SidebarPanel sidebarPanel;
    private final Stage primaryStage;

    public MainWindow(Stage stage) {
        this.primaryStage = stage;
        this.graphCanvas = new GraphCanvas();
        this.sidebarPanel = new SidebarPanel(graphCanvas);

        // Configuración de la estructura general
        setTop(createTopToolbar());

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(sidebarPanel, graphCanvas);
        splitPane.setDividerPositions(0.32);
        splitPane.setStyle("-fx-box-border: transparent; -fx-padding: 0;");
        setCenter(splitPane);

        setBottom(createStatusBar());

        // Inicializar con dos funciones de ejemplo interesantes
        sidebarPanel.addNewFunction("sin(x)", Color.web("#38BDF8"));
        sidebarPanel.addNewFunction("0.2 * x^2 - 3", Color.web("#F43F5E"));
    }

    private Node createTopToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 16, 10, 16));
        toolbar.setStyle("-fx-background-color: #12141A; -fx-border-color: #262A36; -fx-border-width: 0 0 1 0;");

        // Título del programa
        Label title = new Label("∫ Graficadora Científica");
        title.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', system-ui;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // Controles de navegación y zoom
        Button btnZoomIn = createToolbarButton("🔍+ Zoom In", e -> graphCanvas.zoomIn());
        Button btnZoomOut = createToolbarButton("🔍- Zoom Out", e -> graphCanvas.zoomOut());
        Button btnReset = createToolbarButton("⟲ Restablecer", e -> graphCanvas.resetView());

        // Botón exportar imagen
        Button btnExport = createToolbarButton("📷 Exportar PNG", e -> exportGraphSnapshot());
        btnExport.setStyle("-fx-background-color: #047857; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");

        // Botón ayuda / atajos
        Button btnHelp = createToolbarButton("❓ Atajos", e -> showHelpDialog());

        toolbar.getChildren().addAll(title, spacer1, btnZoomIn, btnZoomOut, btnReset, btnExport, btnHelp);
        return toolbar;
    }

    private Node createStatusBar() {
        HBox statusBar = new HBox(16);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(6, 16, 6, 16));
        statusBar.setStyle("-fx-background-color: #12141A; -fx-border-color: #262A36; -fx-border-width: 1 0 0 0;");

        Label lblCoords = new Label();
        lblCoords.textProperty().bind(graphCanvas.cursorCoordinatesTextProperty());
        lblCoords.setStyle("-fx-text-fill: #38BDF8; -fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblHelp = new Label("💡 Arrastra el lienzo para moverte • Rueda del ratón para Zoom • Doble clic para centrar");
        lblHelp.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Label lblEngine = new Label("JavaFX 17");
        lblEngine.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        statusBar.getChildren().addAll(lblCoords, spacer, lblHelp, lblEngine);
        return statusBar;
    }

    private Button createToolbarButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #212530; -fx-text-fill: #CBD5E1; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btn.setOnAction(action);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2E3445; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #212530; -fx-text-fill: #CBD5E1; -fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;"));
        return btn;
    }

    private void exportGraphSnapshot() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Imagen del Gráfico");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG (*.png)", "*.png"));
        fileChooser.setInitialFileName("grafico_cientifico.png");

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                WritableImage image = graphCanvas.snapshot(null, null);
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "El gráfico ha sido exportado exitosamente a:\n" + file.getAbsolutePath(), ButtonType.OK);
                alert.setTitle("Exportación Exitosa");
                alert.setHeaderText(null);
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "No se pudo guardar la imagen: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Atajos y Guía de Uso");
        alert.setHeaderText("Guía de la Graficadora Científica");

        String guide = """
                • Navegación en el Gráfico:
                  - Clic izquierdo y arrastrar: Desplazamiento por el plano (Pan)
                  - Rueda del ratón: Zoom centrado en la posición del cursor
                  - Doble clic: Centra el plano en las coordenadas cliqueadas
                  - Botón "Restablecer": Vuelve a la vista por defecto
                
                • Sintaxis Matemática Admitida:
                  - Operadores: +, -, *, /, ^ (potencia), % (módulo)
                  - Multiplicación implícita: 2x, 3sin(x), (x+1)(x-2)
                  - Constantes: pi (o π), e, tau, phi
                  - Funciones trigonométricas: sin, cos, tan, sec, csc, cot
                  - Funciones inversas: asin, acos, atan
                  - Funciones hiperbólicas: sinh, cosh, tanh
                  - Raíces y logaritmos: sqrt (o √), cbrt, ln, log, log2, exp
                  - Otras: abs, floor, ceil, round, sign, sinc
                
                • Análisis de Cálculo Numérico:
                  - En la pestaña 'Cálculo' puedes encontrar raíces f(x)=0,
                    máximos y mínimos locales, evaluar la derivada y
                    calcular el área bajo la curva mediante integrales de Simpson.
                """;

        alert.setContentText(guide);
        alert.getDialogPane().setPrefWidth(520);
        alert.showAndWait();
    }
}
