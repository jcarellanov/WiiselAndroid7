package julioarellano.wiiselandroid7.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import julioarellano.wiiselandroid7.activity.interfaces.IRefreshable;

public class PhoneStateManager {

    private static volatile PhoneStateManager instance;
    volatile List<IRefreshable> list;

    private PhoneStateManager() {
        list = Collections.synchronizedList(new ArrayList<IRefreshable>());
    }

    public static PhoneStateManager getInstance() {
        PhoneStateManager localInstance = instance;
        if (localInstance == null) {
            synchronized (PhoneStateManager.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new PhoneStateManager();
                }
            }
        }
        return localInstance;
    }

    public void putFirstInsoleData(int count, int gyrInvX, int gyrInvY, int gyrInvZ, int accInvX, int accInvY, int accInvZ, int accStX,
        int accStY, int accStZ, int[] press) {

        int[] dataFirst = new int[10];

        dataFirst[0] = count;
        dataFirst[1] = gyrInvX;
        dataFirst[2] = gyrInvY;
        dataFirst[3] = gyrInvZ;
        dataFirst[4] = accInvX;
        dataFirst[5] = accInvY;
        dataFirst[6] = accInvZ;
        dataFirst[7] = accStX;
        dataFirst[8] = accStY;
        dataFirst[9] = accStZ;

        List<IRefreshable> localList = list;
        for (int i = 0; i < localList.size(); i++) {
            localList.get(i).putFirstInsoleData(dataFirst, press);
        }
    }

    public void putSecondInsoleData(int count, int gyrInvX, int gyrInvY, int gyrInvZ, int accInvX, int accInvY, int accInvZ, int accStX,
        int accStY, int accStZ, int[] press) {

        int[] dataSecond = new int[10];

        dataSecond[0] = count;
        dataSecond[1] = gyrInvX;
        dataSecond[2] = gyrInvY;
        dataSecond[3] = gyrInvZ;
        dataSecond[4] = accInvX;
        dataSecond[5] = accInvY;
        dataSecond[6] = accInvZ;
        dataSecond[7] = accStX;
        dataSecond[8] = accStY;
        dataSecond[9] = accStZ;

        List<IRefreshable> localList = list;
        for (int i = 0; i < localList.size(); i++) {
            localList.get(i).putSecondInsoleData(dataSecond, press);
        }
    }

    public void subscribe(IRefreshable iRefreshable) {
        list.add(iRefreshable);
    }

    public void unsubscribe(IRefreshable iRefreshable) {
        list.remove(iRefreshable);
    }
    
    public boolean isSubscribed(IRefreshable iRefreshable) {
        return list.contains(iRefreshable);
    }
}
