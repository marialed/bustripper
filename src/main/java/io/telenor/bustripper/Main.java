package io.telenor.bustripper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.HashSet;
import java.util.Set;

public class Main {


        private static class BustripWaiter implements TripsCallback {
            private boolean done = false;
            private Set<BusTrip> allTrips = new HashSet<BusTrip>();
            private static int maxtrips = 10;

            @Override
            public synchronized void gotTrips(Set<BusTrip> trips, boolean done) {
                allTrips.addAll(trips);
				
                if(done) {
                    if (allTrips.isEmpty()) {
                        System.out.println("No trips found!");
                    }

                    allTrips.stream().sorted(
                            (e1, e2) -> e1.getExpectedArrivalTime().compareTo(e2.getExpectedArrivalTime())
                    ).limit(maxtrips).forEach(t -> System.out.println(t));

                    this.done = true;
                    notify();
                }
            }

            @Override
            public synchronized void failedGettingTrips(IOException io) {
                System.out.println("Failed getting trips. " + io.getMessage());
                this.done = true;
                notify();
            }

            public synchronized boolean waitForCompletion() throws InterruptedException{
                while(!done) {
                    wait();
                }
                return done;
            }
        }

        private static class InputGatherer implements Runnable {
            public void run() {
                System.out.println("Welcome to the bus line lookup tool. Add a search phrase to look up a bus stop.");
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                boolean done = false;

                do {
                    System.out.print("> ");
                    try {
                        String searchterm = in.readLine().replaceAll("[^a-z A-Z]", "");
                        if(searchterm.equals("q") || searchterm.length() == 0) {
                            System.exit(0);
                        }
                        System.out.println("Looking up " + searchterm);
                        BustripWaiter waiter = new BustripWaiter();
                        new Thread(new FindBusStop(waiter, searchterm)).start();
                        waiter.waitForCompletion();
                    }
                    catch(IOException io) {
						System.err.println("IOException caught. Exiting...");
                        done = true;
                    }
                    catch(InterruptedException uh) {
						System.err.println("InterruptedException caught. Exiting...");
                        done = true;
                    }
					
                } while (!done);
            }
        }




        public static void main(String[] args) {
            new Thread(new InputGatherer()).start();
        }
    }
