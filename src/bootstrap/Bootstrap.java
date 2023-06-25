package bootstrap;

import mandelbrot.MandelbrotService;

import java.rmi.*;
import java.util.*;

public interface Bootstrap extends Remote
{
    public void registerWorker(String name, Remote stub) throws RemoteException;
    public Remote getWorker()                            throws RemoteException;
    public List<Remote> getWorkerList()                  throws RemoteException;
    public List<String> getList()                        throws RemoteException;
    public void clearList()                              throws RemoteException;
    public String getVersion()                           throws RemoteException;

    MandelbrotService MandelbrotService()                throws RemoteException;
}
