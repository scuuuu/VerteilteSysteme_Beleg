package mandelbrot;

import bootstrap.Bootstrap;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class MandelbrotClient extends UnicastRemoteObject implements MandelbrotService {
    static MandelbrotService service;
    static Bootstrap bs;
    static List<MandelbrotService> services;

    public MandelbrotClient() throws RemoteException {
        super();
    }
    public static void main(String[] args) {
        try {
            String host = args[0];
            String port = "1099";
            String srv = "Bootstrap";
            String url = "rmi://" + host + ":" + port + "/" + srv;

            bs = (Bootstrap) Naming.lookup(url);
            bs.MandelbrotService();
            bs.getWorker();
            //System.out.println("Looking-up Bootstrap done...");
            List<String> list = bs.getList();
            //System.out.println("Serverlist: " + list.toString());
            List<Remote> workers = bs.getWorkerList();
            //System.out.println("Workerlist: " + workers.toString());
            MandelbrotClient.service = bs.MandelbrotService();
            MandelbrotClient client = new MandelbrotClient();
            int workerCount = list.size();
            services = new ArrayList<>();
            // Den Workern ID und Anzahl Worker mitteilen
            for (int i = 0; i< workerCount; i++) {
                service = (MandelbrotService) workers.get(i);
                services.add(service);
                //service.workerCount(i);
                service.workerCount(workerCount);
                service.workerID(i+1);
            }
            System.out.println("Anzahl verbundener Worker: " + workerCount);
            // Client starten
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //
    public void run() {
        try {
            ApfelPresenter p = new ApfelPresenter();
            ApfelView v = new ApfelView(p);
            p.setServiceAndView(this, v);
            p.erstesImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


   @Override
    public Color[][] apfel_bild(double xmin, double xmax, double ymin, double ymax, int y_start, int y_stopp) throws RemoteException {
        return service.apfel_bild(xmin, xmax, ymin, ymax, y_start, y_stopp);
    }
    @Override
    public void workerCount(int anzahlWorker) throws RemoteException {
        service.workerCount(anzahlWorker);
    }
    @Override
    public void workerID(int id) throws RemoteException {
        service.workerID(id);
    }

    /* ************************** Presenter ********************** */
    class ApfelPresenter {
        protected MandelbrotService service;
        protected ApfelView v;

        double xmin = -1.666, xmax = 1, ymin = -1, ymax = 1; // Parameter des Ausschnitts
        double cr = -0.743643887036151, ci = 0.131825904205330;
        double zoomRate = 1.5;
        int xpix = 800, ypix = 600;
        int y_start, y_stopp;

        volatile boolean continueIterieren = true; //neu

        public void setServiceAndView(MandelbrotService service, ApfelView v) {
            this.service = service;
            this.v = v;
            v.setDim(xpix, ypix);
        }

        void erstesImage() throws RemoteException {
            xmin = -1.666;
            xmax = 1;
            ymin = -1;
            ymax = 1;
            Color[][] c = service.apfel_bild(xmin, xmax, ymin, ymax, y_start, y_stopp);
            v.update(c);

        }
        /**
         * Komplette Berechnung aller Bilder
         */
        void apfel(int anzahlIteration) throws RemoteException, InterruptedException {
            // Liste mit allen worker Services
            List<MandelbrotService> workers = services;
            // Anzahl der worker
            int workerCount = workers.size();
            // Liste, um Threads zu speichern
            List<Thread> threads = new ArrayList<>();
            // Synchronisierte Liste, um Ergebnisse zu speichern. Die größe der Liste entpricht der Anzahl der Iterationen
            List<Color[][]> results = Collections.synchronizedList(new ArrayList<>(Collections.nCopies(anzahlIteration, null)));
            // Zähler für die nächste Iteration und das nächste Ergebnis
            AtomicInteger nextIteration = new AtomicInteger();
            AtomicInteger nextToDisplay = new AtomicInteger();

            // Erstelle für jeden worker einen Thread
            for (int i = 0; i < workerCount; i++) {
                Thread thread = new Thread(() -> {
                    try {
                        while (true) {
                            // Erhalte die nächste Iteration und brich die Schleife ab, wenn alle Iterationen abgeschlossen sind
                            int iteration = nextIteration.getAndIncrement();
                            if (iteration >= anzahlIteration) {
                                break;
                            }
                             // Aufruf der apfel_bild-Methode auf dem aktuellen Worker auf und speichern des Ergebnisses
                            Color[][] result = workers.get(iteration % workerCount).apfel_bild(xmin, xmax, ymin, ymax, 0, ypix);
                            results.set(iteration, result);

                            // Update xmin, xmax, ymin, ymax für die nächste Iteration
                            if (iteration < anzahlIteration && continueIterieren) {
                                double xdim = xmax - xmin;
                                double ydim = ymax - ymin;
                                xmin = cr - xdim / 2 / zoomRate;
                                xmax = cr + xdim / 2 / zoomRate;
                                ymin = ci - ydim / 2 / zoomRate;
                                ymax = ci + ydim / 2 / zoomRate;
                            }
                            synchronized (results) {
                                displayResults(results, nextToDisplay);
                            }
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
                // Threads starten
                threads.add(thread);
                thread.start();
            }
            for (int i = 0; i < threads.size(); i++) {
                // Warten bis alle Threads fertig sind
                threads.get(i).join();
            }
            // Anzeigen der restlichen Bilder
            displayResults(results, nextToDisplay);
        }

        void displayResults(List<Color[][]> results, AtomicInteger nextToDisplay) {
            while (true) {
                // Das nächste Bild wird aus der Liste geholt
                int current = nextToDisplay.get();
                // Wenn das Bild noch nicht berechnet wurde, wird gewartet
                if (current >= results.size() || results.get(current) == null) {
                    break;
                }
                // Das Bild wird angezeigt
                v.update(results.get(current));
                // Angezeigte Bilder werden aus der Liste entfernt
                results.set(current, null);
                // Nächstes Bild wird angezeigt
                nextToDisplay.incrementAndGet();
            }
        }
    }
        /* ************************* View *************************** */
    class ApfelView {
        private ApfelPresenter p;
        private ApfelPanel ap = new ApfelPanel();
        public JTextField tfi;
        public JTextField tfr;
        int xpix, ypix;
        BufferedImage image;

        public ApfelView(ApfelPresenter p) {
            this.p = p;
        }
        public void setDim(int xpix, int ypix) {
            this.xpix = xpix;
            this.ypix = ypix;
            image = new BufferedImage(xpix, ypix, BufferedImage.TYPE_INT_RGB);
            initView();
        }

        private void initView() {
            JFrame f = new JFrame();
            JPanel sp = new JPanel(new FlowLayout());
            JPanel itPanel = new JPanel(new BorderLayout());
            JLabel itLabel = new JLabel("Iterationen: ");
            JTextField itField = new JTextField("15");
            JButton sb = new JButton("Start");
            JButton stoppButton = new JButton("Stopp");
            JButton resetButton = new JButton("Reset"); //neu

            sp.add(itPanel);
            itPanel.add(itLabel, BorderLayout.NORTH);
            itPanel.add(itField, BorderLayout.CENTER);

            tfr = new JTextField("-0.743643887037151");
            tfi = new JTextField("0.131825904205330");
            sp.add(tfr);
            sp.add(tfi);
            sp.add(sb);
            sp.add(stoppButton);
            sp.add(resetButton); //neu


            stoppButton.setEnabled(false); //neu
            resetButton.setEnabled(false); //neu

            // Start-Button
            sb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Separater Thread, um nicht die GUI zu blockieren
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                p.cr = Double.parseDouble(tfr.getText());
                                p.ci = Double.parseDouble(tfi.getText());
                                long startTime = System.currentTimeMillis();
                                int anzahlIteration = Integer.parseInt(itField.getText());
/*                                p.xmin = -1.666;
                                p.xmax = 1;
                                p.ymin = -1;
                                p.ymax = 1;*/
                                p.continueIterieren = true;
                                sb.setEnabled(false);
                                stoppButton.setEnabled(true);
                                resetButton.setEnabled(false);
                                p.apfel(anzahlIteration);

                                System.out.println("cr: " + p.cr + ", ci: " + p.ci);

                                if (!p.continueIterieren) {
                                    sb.setEnabled(false);
                                    resetButton.setEnabled(true);
                                    stoppButton.setEnabled(false);
                                    long endTime = System.currentTimeMillis();
                                    System.out.println("Zeit: " + (endTime - startTime) + " ms");
                                }
                            } catch (NumberFormatException nfe) {
                                JOptionPane.showMessageDialog(f, "Bitte geben Sie gültige Zahlen ein.");
                            } catch (RemoteException | InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }).start();
                }
            });
            // Stopp-Button
            stoppButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    sb.setEnabled(true);  // Aktiviert den Start-Button wieder
                    stoppButton.setEnabled(false); // Deaktiviert den Stopp-Button
                    resetButton.setEnabled(true); // Aktiviert den Reset-Button
                    p.continueIterieren = false; // Setzt die Variable auf false
                }
            });
            // Reset-Button
            resetButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    sb.setEnabled(true);  // Aktiviert den Start-Button wieder
                    stoppButton.setEnabled(false); // Deaktiviert den Stopp-Button
                    try {
                        p.erstesImage(); // Zeigt das erste Bild wieder an //neu
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                    resetButton.setEnabled(false); // Deaktiviert den Reset-Button
                }
            });

            ap.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        double xdim = p.xmax - p.xmin;
                        double ydim = p.ymax - p.ymin;
                        double x = p.xmin + e.getX() * xdim / xpix;
                        double y = p.ymin + e.getY() * ydim / ypix;
                        tfr.setText("" + x);
                        tfi.setText("" + y);
                    }
                }
            });
            f.add(ap, BorderLayout.CENTER);
            f.add(sp, BorderLayout.SOUTH);
            f.setSize(xpix, ypix + 100);
            f.setVisible(true);
        }
        public void update(Color[][] c) {
            for (int y = 0; y < ypix; y++) {
                for (int x = 0; x < xpix; x++) {
                    if (x < c.length && y < c[x].length && c[x][y] != null) {
                        image.setRGB(x, y, c[x][y].getRGB());
                    }
                }
            }
            ap.repaint();
        }
        class ApfelPanel extends JPanel {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, null);
            }
        }
    }
}

