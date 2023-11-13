package info.kgeorgiy.ja.belozorov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {
    String getName() throws RemoteException;
    String getSurname() throws RemoteException;

    String getPassport() throws RemoteException;
}
