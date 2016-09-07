package edu.levifranca.meguia;

import org.altbeacon.beacon.Beacon;

import java.util.Comparator;

/**
 * Created by levifranca on 06/09/16.
 */
public class NearestBeaconComparator implements Comparator<Beacon>{

    @Override
    public int compare(Beacon b1, Beacon b2) {
        if (b1 == null) {
            return 1;
        }
        if (b2 == null) {
            return -1;
        }

        return (int) Math.ceil(b1.getDistance() - b2.getDistance());
    }
}
