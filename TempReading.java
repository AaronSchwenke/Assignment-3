import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import java.util.*;
import java.lang.*;

public class TempReading {
    public static int numThreads = 8;
	public static Unit[] units = new Unit[numThreads];
	public static Thread[] threads = new Thread[numThreads];
    public static double[] lowestTemps = new double[5];
    public static double[] highestTemps = new double[5];
    public static double[] largestInterval = new double[6];

    public static void main(String[] args) {
        for (int i=0; i<numThreads; i++) {
            double startTime = System.nanoTime() * 1E-9;
            units[i] = new Unit(startTime);
			threads[i] = new Thread(units[i]);
			threads[i].start();
        }
    }
}

class Unit implements Runnable {
    public double lastReading;
    public static int readInterval = 3, numIntervals = 0;
    public ArrayList<Double> temps = new ArrayList<>();
    public static boolean hourNotOver = true, stillSorting = true;
    public static Lock highLock = new ReentrantLock();
    public static Lock lowLock = new ReentrantLock();


    public Unit(double startTime) {
        this.lastReading = startTime;
    }

    public void run() {    
        temps.add(randTemp());

        while(hourNotOver || stillSorting) {
            if (timeForReading())
                temps.add(randTemp());

            while (temps.size() > 0) {
                if (timeForReading())
                    temps.add(randTemp());

                boolean hasLock = highLock.tryLock(100, TimeUnit.MILLISECONDS);
                if (hasLock) {
                    try {

                    }
                    finally {
                        highLock.unlock();
                    }
                }
            }
        }
    }

    public boolean timeForReading() {
        double curTime = System.nanoTime() * 1E-9;
        if (Math.abs(curTime - this.lastReading) < 0.05)
            return true;

        return false;
    }

    public double randTemp() {
        return -100 + (int)(Math.random() * 171);
    }

    public void addHighest(double temp) {
        double highTemp = temp;
        for(int i=0; i<5; i++) {
            if (highTemp > TempReading.highestTemps[i]) {
                double newTemp = highTemp;
                highTemp = TempReading.highestTemps[i];
                TempReading.highestTemps[i] = newTemp;
            }
        }
    }

    public void addLowest(double temp) {
        double lowTemp = temp;
        for(int i=0; i<5; i++) {
            if (lowTemp < TempReading.highestTemps[i]) {
                double newTemp = lowTemp;
                lowTemp = TempReading.highestTemps[i];
                TempReading.highestTemps[i] = newTemp;
            }
        }
    }
}
