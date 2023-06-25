# Beleg Verteile Systeme


## Code kompilieren
```
cd src
javac bootstrap/*.java  mandelbrot/*.java
```
<br>

## Bootstrap starten:
```
java bootstrap/BootstrapRegistration
```
## Server starten:

```
java mandelbrot/MandelbrotServer <ip oder localhost> + <servername>
```

wobei `<servername>` durch einen frei gewählten Namen ersetzt werden muss.

<br>
  
## Client starten:
  
```
java mandelbrot/MandelbrotClient <ip oder localhost>
```
  
