# Laboratorio 3 de Teoría de la Computación

**Asignatura:** INFO1148 · **Autor:** Álvaro Valenzuela Paillacar

Diseño y validación de una gramática libre de contexto para programas con asignaciones simples, operaciones binarias, salidas y declaraciones de variables.

## Entrega

- Informe_Laboratorio_3_Alvaro_Valenzuela.pdf: informe final.
- Gramatica_Lab3.jff: gramática principal para programas completos.
- Gramatica_Lab3_Instrucciones.jff: variante auxiliar para instrucciones aisladas. Solo cambia S → bpiLf por S → I; las otras 21 producciones se conservan.
- El ZIP disponible en el repositorio contiene la carpeta completa, incluyendo evidencias, verificadores y fuentes. Descomprimirlo para ejecutar las comprobaciones.

Los comentarios y espacios se descartan en la fase léxica. Los tokens id y num se codifican como i y d; no se introducen los lexemas directamente en el analizador de JFLAP.

## Reproducir los árboles en JFLAP 7.1

1. Abrir el archivo principal con File → Open.
2. Elegir Input → Brute Force Parse.
3. Usar bpii=i+i#f para el programa que contiene z = x + y #.
4. Avanzar con Step hasta la derivación completa.
5. Abrir la variante auxiliar para i=i+i#, si# o ri#.

Equivalencias: b=Begin, f=End, p=Programa, s=salida, n=entero, r=real, i=id, d=num. Los operadores y # no cambian.

No terminales: S=programa, L=lista_instrucciones, I=instruccion, A=asignacion, B=asig_simple, C=asig_expr, E=expresion, V=valor, O=operador, Y=salida, D=declaracion, P=tipo. En el archivo auxiliar, S es únicamente la raíz auxiliar que produce I.

## Verificación independiente

Desde la carpeta descomprimida:

    python verificar_entrega.py

Requiere Python 3 y solo usa su biblioteca estándar. Lee los JFF entregados, comprueba la equivalencia de sus producciones, valida los ocho casos del enunciado y la extensión, y contrasta un reconocedor Earley con un clasificador independiente.

Resultado: 4.362 cadenas de instrucciones (25 aceptadas, 4.337 rechazadas), cero discrepancias; 625 programas de dos instrucciones aceptados; programa de referencia de 91 tokens aceptado; diez casos adicionales correctos.

## Evidencias nativas

Los árboles y capturas se generaron con el analizador grammar.parse.BruteParser, el códec file.XMLCodec y los componentes Swing originales de JFLAP 7.1, renderizados programáticamente. No se sustituyeron por dibujos de otra herramienta.

El archivo evidencias/pruebas_jflap.csv contiene doce pruebas nativas, todas correctas. Los archivos de derivación muestran las trazas automáticas de JFLAP; las derivaciones formales por la izquierda se desarrollan por separado en el informe.

Para regenerar las imágenes, colocar una copia de JFLAP7.1.jar proporcionada para la asignatura en esta carpeta y, con un JDK instalado, ejecutar en Windows:

    javac -cp JFLAP7.1.jar herramientas/CompletarJflap.java
    java -cp "JFLAP7.1.jar;herramientas" CompletarJflap .
    python verificar_entrega.py

El JAR de terceros no se redistribuye en este paquete.
