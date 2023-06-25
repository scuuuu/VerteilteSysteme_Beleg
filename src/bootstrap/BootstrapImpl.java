package bootstrap;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import mandelbrot.MandelbrotService;

@SuppressWarnings("serial")
public class BootstrapImpl extends UnicastRemoteObject implements Bootstrap
{
    private Remote worker;
    private String name;
    private List<Remote> workers  = new ArrayList<Remote>();
    private List<String> nameList = new ArrayList<String>();
    private String  version = "0.1";

    public BootstrapImpl() throws RemoteException
    {
          super();
    }

    public void registerWorker(String name, Remote stub) throws RemoteException {
        System.out.println("Server: Methode register wurde aufgerufen: " + name);
        System.out.println("Server: Check Monitor ");
        synchronized(this) {
          worker = stub;
          this.name = name;
  
          for (int i = 0; i < nameList.size(); i++) {  
              if ( nameList.get(i).equals(name) ) {
                 workers.set(i, stub); 
                 System.out.println("Server: update existing worker " + name );
                 return;  // update worker
              }
          }
          System.out.println("Server: new worker registration " + name);
          workers.add(stub);
          nameList.add(name);
        }
        System.out.println("Server: Listenelemente: " + nameList.size() );
        return;
    }

    public Remote getWorker() throws RemoteException {
        System.out.println("Server: Methode getWorker wurde aufgerufen");
        return worker;
    }

    public List<Remote> getWorkerList() throws RemoteException {
        System.out.println("Server: Methode getWorkerList wurde aufgerufen");
        return workers;
    }

    public List<String> getList() throws RemoteException {
        System.out.println("Server: Methode getList() wurde aufgerufen");
        return nameList;
    }

    public void clearList() throws RemoteException {
        System.out.println("Server: Methode clearList() wurde aufgerufen");
        workers.clear();
        nameList.clear();
        return;
    }

    public String getVersion() throws RemoteException {
        System.out.println("Server: Methode getVersion() wurde aufgerufen");
        return version;
    }

    @Override
    public MandelbrotService MandelbrotService() throws RemoteException {
        return null;
    }
}
