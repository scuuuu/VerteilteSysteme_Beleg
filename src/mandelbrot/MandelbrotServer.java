package mandelbrot;

import bootstrap.Bootstrap;

import java.awt.Color;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MandelbrotServer extends UnicastRemoteObject implements MandelbrotService {

    private final int xpix;
    private final int ypix;
    private double xmin, xmax, ymin, ymax;
    private Color[][] bild;
    static Remote stub;
    private int workerCount;
    private int y_start;
    private int y_stopp;
    private int workerID;

    public MandelbrotServer(int xpix, int ypix) throws RemoteException {
        super();
        this.xpix = 800;
        this.ypix = 600;
        this.bild = new Color[xpix][ypix];
    }
    public static void main(String[] args) {
        try {
            int xpix = 800;
            int ypix = 600;
            MandelbrotServer server = new MandelbrotServer(xpix, ypix);
            String host = args[0];
            String port = "1099";
            String srv = "Bootstrap";
            String url = "rmi://" + host + ":" + port + "/" + srv;

            Bootstrap bs = (Bootstrap) Naming.lookup(url);
            stub = (Remote) server;

            // Neuen Worker mit benutzerdefinierten Namen registrieren
            bs.registerWorker("MandelbrotServer " + args[1] + " "  + InetAddress.getLocalHost().getHostName(), stub);
            System.out.println("Der Worker ist gestartet und läuft");
        } catch (Exception e) {
            System.out.println("Ausnahme: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /* *********** Model **************************** */
    final int max_iter = 5000;
    final double max_betrag2 = 4;

    /** Erzeuge ein Bild mittels Multithreading, wobei jedes Bild auf y-Achse in Anzahl von Threads aufgeteilt wird */
    // Anzahl der Worker
    @Override
    public void workerCount(int anzahlWorker) throws RemoteException {
        this.workerCount = anzahlWorker;
    }
    // Jeder Worker bekommt eine ID
    @Override
    public void workerID(int id) throws RemoteException {
        this.workerID = id;
    }
    // Die Methode apfel_bild berechnet das Mandelbrot-Set und gibt ein Bild davon zurück
    @Override
    public Color[][] apfel_bild(double xmin, double xmax, double ymin, double ymax, int y_start, int y_stopp) throws RemoteException {

        // Setzen der Grenzen für das Bild des Mandelbrot-Sets
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        System.out.println("Workeranzahl insgesamt:" + workerCount);

        // Der y-Bereich des Bildes, der berechnet werden muss, wird bestimmt
        int y_bereich = ypix - y_start;
        // Anzahl der Threads
        int numThreads = 8;
        System.out.println("Worker " + workerID + " startet " );
        // Erzeugen der Threads
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            // Jeder Thread bekommt einen eigenen y-Bereich zugewiesen, den er berechnet
            y_start = this.y_start + i * y_bereich / numThreads;
            y_stopp = this.y_start + (i + 1) * y_bereich / numThreads;
            // Ein neuer Worker wird mit dem zugewiesenen y-Bereich erstellt
            ApfelWorker worker = new ApfelWorker(y_start, y_stopp);
            // Der neue Worker wird in einem Thread gestartet
            threads[i] = new Thread(worker);
            threads[i].start();
            System.out.println("Thread " + i + " gestartet");
        }
        // Warten auf alle Threads, um ihre Arbeit zu beenden
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
                return bild;
    }

    // Threads and writing to arrays
    // http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.6

    /** @author jvogt lokale Klasse zum Thread-Handling */
    class ApfelWorker implements Runnable {
        int y_sta, y_sto;

        public ApfelWorker(int y_start, int y_stopp) {
            this.y_sta = y_start;
            this.y_sto = y_stopp;
        }

        @Override
        public void run() {
            double c_re, c_im;
            for (int y = y_sta; y < y_sto; y++) {
                c_im = ymin + (ymax - ymin) * y / ypix;

                for (int x = 0; x < xpix; x++) {
                    c_re = xmin + (xmax - xmin) * x / xpix;
                    int iter = calc(c_re, c_im);
                    Color pix = farbwert(iter); // Farbberechnung
                    synchronized(bild) {
                        bild[x][y] = pix;
                    }
                }
            }
        }
        /**
         * @param cr Realteil
         * @param ci ImaginÃ¤rteil
         * @return Iterationen
         */
        public int calc(double cr, double ci) {
            int iter;
            double zr, zi, zr2 = 0, zi2 = 0, zri = 0, betrag2 = 0;
            //  z_{n+1} = zÂ²_n + c
            //  zÂ²  = xÂ² - yÂ² + i(2xy)
            // |z|Â² = xÂ² + yÂ²
            for (iter = 0; iter < max_iter && betrag2 <= max_betrag2; iter++) {
                zr = zr2 - zi2 + cr;
                zi = zri + zri + ci;

                zr2 = zr * zr;
                zi2 = zi * zi;
                zri = zr * zi;
                betrag2 = zr2 + zi2;
            }
            return iter;
        }

        /**
         * @param iter Iterationszahl
         * @return Farbwert nsmooth = n + 1 - Math.log(Math.log(zn.abs()))/Math.log(2)
         *     Color.HSBtoRGB(0.95f + 10 * smoothcolor ,0.6f,1.0f);
         */
        public Color farbwert(int iter) {
            float hue = iter % 256 / 256.0f;
            return Color.getHSBColor(hue, 1f, 1f);
        }
    }
}
