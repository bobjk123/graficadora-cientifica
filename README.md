# ⚡ Graficadora Científica FX

Una aplicación de escritorio moderna y reactiva desarrollada en **JavaFX 17** para la graficación de funciones matemáticas y análisis de cálculo científico en tiempo real.

---

## 🚀 Características Principales

### 1. 📈 Motor Gráfico Interactivo de Alto Rendimiento
- **Navegación Fluida:**
  - Desplazamiento por el plano cartesiano (*Pan*) haciendo clic y arrastrando con el ratón.
  - Zoom dinámico (*In / Out*) centrado exactamente en el cursor mediante la rueda del ratón.
  - Doble clic para re-centrar la vista en cualquier punto del plano.
  - Botón de restablecimiento a la vista predeterminada.
- **Cuadrícula y Ejes Adaptativos:**
  - División de rejilla mayor y menor con escala automática según el nivel de ampliación.
  - Graduación numérica inteligente en ejes $X$ e $Y$.
- **Trazado de Curvas Inteligente:**
  - Soporte de múltiples funciones simultáneas ($f_1(x), f_2(x), f_3(x), \dots$).
  - Selector de color individual y control de visibilidad por casilla de verificación.
  - Detección automática de discontinuidades y asíntotas verticales (ej. $\tan(x)$, $1/x$) para evitar líneas conectoras espurias.
- **HUD y Seguimiento en Tiempo Real:**
  - Retícula de seguimiento del cursor.
  - Marcadores circulares sobre las curvas con los valores numéricos correspondientes al punto $x$ del ratón.

---

### 2. 🧮 Parser Matemático Científico
- **Operaciones Estándar:** `+`, `-`, `*`, `/`, `^` (potencia), `%` (módulo).
- **Multiplicación Implícita Inteligente:** Permite sintaxis natural como `2x`, `3sin(x)`, `(x+1)(x-2)`, `2pi`.
- **Constantes:** `pi` ($\pi$), `e`, `tau` ($2\pi$), `phi` ($\phi$).
- **Funciones Soportadas:**
  - Trigonométricas: `sin`, `cos`, `tan`, `sec`, `csc`, `cot`
  - Inversas: `asin` (arcsin), `acos` (arccos), `atan` (arctan)
  - Hiperbólicas: `sinh`, `cosh`, `tanh`
  - Raíces y Potencias: `sqrt` ($\sqrt{}$), `cbrt`, `exp`, `x^2`, `x^n`
  - Logaritmos: `ln` (logaritmo natural), `log` (base 10), `log2`
  - Especiales: `abs` ($|x|$), `floor`, `ceil`, `round`, `sign`, `sinc` ($\sin(x)/x$)
- **Compatibilidad Unicode:** Admite caracteres como `²`, `³`, `π`, `√`, `×`, `÷`.

---

### 3. 🔬 Herramientas de Cálculo y Análisis Numérico
- **Raíces (Ceros):** Búsqueda de soluciones a la ecuación $f(x) = 0$ en cualquier intervalo $[a, b]$ mediante muestreo y bisección de alta precisión, con marcado visual sobre el eje $X$.
- **Extremos Locales:** Detección de máximos y mínimos locales con clasificación por segunda derivada y señalización en el gráfico.
- **Recta Tangente y Derivada:** Cálculo de la derivada numérica $f'(x_0)$ en cualquier punto con trazado de la recta tangente y su pendiente $m$.
- **Integral Definida:** Cálculo del área bajo la curva mediante el método compuesto de Simpson (1/3) con sombreado semitransparente de la región integrada en $[a, b]$.
- **Tabla de Valores:** Generación de tablas tabuladas de $x$, $f(x)$ y $f'(x)$ para un rango y paso configurables.
- **Galería de Ejemplos:** Acceso inmediato con un clic a funciones notables como la *Onda Amortiguada*, la *Campana de Gauss*, la *Función Sinc*, *Batido de Frecuencias*, entre otras.

---

### 4. 📷 Exportación
- Exportación de capturas del gráfico en alta resolución en formato **PNG**.

---

## 📁 Estructura del Proyecto

```
graficadora-cientifica/
├── bin/                             # Clases compiladas y recursos
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── graficadora/
│       │           ├── MainApp.java           # Punto de entrada de la aplicación JavaFX
│       │           ├── TestRunner.java        # Suite de pruebas unitarias del motor matemático
│       │           ├── model/
│       │           │   ├── CalculusEngine.java# Derivadas, integrales, raíces y extremos
│       │           │   ├── FunctionItem.java  # Modelo reactivo de cada función
│       │           │   └── MathParser.java    # Parser y árbol sintáctico (AST)
│       │           └── ui/
│       │               ├── GraphCanvas.java   # Lienzo interactivo de trazado 2D
│       │               ├── MainWindow.java    # Ventana principal y barras de herramientas
│       │               └── SidebarPanel.java  # Panel de funciones, cálculo y teclado
│       └── resources/
│           └── styles/
│               └── dark-theme.css             # Tema oscuro moderno
├── pom.xml                                    # Configuración para IDEs (VS Code, IntelliJ)
├── run.bat                                    # Script de ejecución para Windows (Doble clic)
├── run.ps1                                    # Script de ejecución para PowerShell
└── README.md
```

---

## 🛠️ Cómo Ejecutar el Proyecto

Dado que tu sistema cuenta con **Liberica JDK 17 Full** (el cual ya incluye los módulos de JavaFX de forma nativa), **no necesitas instalar nada adicional**.

### Opción 1: Ejecución Rápida con `run.bat` (Recomendado)
Haz doble clic sobre el archivo `run.bat` o ejecútalo desde la consola:
```cmd
run.bat
```

### Opción 2: Desde PowerShell
```powershell
.\run.ps1
```

### Opción 3: Compilación y Ejecución Manual
```powershell
javac -encoding UTF-8 -d bin src\main\java\com\graficadora\model\*.java src\main\java\com\graficadora\ui\*.java src\main\java\com\graficadora\MainApp.java
java -cp bin com.graficadora.MainApp
```
