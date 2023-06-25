package mandelbrot;

import java.awt.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MandelbrotService extends Remote {
    Color[][] apfel_bild(double xmin, double xmax, double ymin, double ymax, int y_start, int y_stopp) throws RemoteException;
    public void workerCount(int anzahlWorker) throws RemoteException;
    public void workerID(int id) throws RemoteException;

}
