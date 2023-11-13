package info.kgeorgiy.ja.belozorov.bank;

import java.rmi.RemoteException;

public class RemotePerson implements Person {

    private String name;
    private String surname;
    private String passport;

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
