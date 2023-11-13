package info.kgeorgiy.ja.belozorov.bank;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalPerson implements Person {
    private String name;
    private String surname;
    private String passport;

    ConcurrentLinkedQueue<Account> data = new ConcurrentLinkedQueue<Account>();

    public LocalPerson(){

    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public String getSurname() throws RemoteException {
        return surname;
    }

    @Override
    public String getPassport() throws RemoteException {
        return passport;
    }
}
