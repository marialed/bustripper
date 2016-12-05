package io.telenor.bustripper;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.IOException;
import java.util.Arrays;

/**
 *
 */
public class BusStopsCallBack implements InvocationCallback<Response> {

    private ObjectMapper mapper = new ObjectMapper();

    private TripsCallback listener;

    public BusStopsCallBack(TripsCallback callback) {
        this.listener = callback;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void completed(Response response) {
        ObjectMapper mapper = new ObjectMapper();
		int stopsCount = 0;
		int stopsCounter = 0;
		
        try {
            BusStop[] stops = mapper.readValue(response.readEntity(String.class), BusStop[].class);		
			for (int i = 0; i< stops.length && stopsCount<10;i++) {	// Get useful stops' number
				if (stops[i].getPlaceType().equals("Stop")) {
					stopsCount++;
				}
			}
			System.out.println(String.format("Got %d busstops nearby", stopsCount));

            for(int j = 0; j< stops.length; j++) {
                BusStop stop = stops[j];
				if (!stop.getPlaceType().equals("Stop")) {
					continue;	// Not a bus stop, skip. 
								// Error fix for "Failed getting trips. No content to map due to end-of-input"
				}
				stopsCounter++;

				boolean isLast = stopsCounter == stopsCount;
				new Thread(new FindBusLinesForStop(stop.getId(), listener, isLast)).start();
				if (isLast) {
					break;
				}
            }			
        } catch (IOException e) {
            listener.failedGettingTrips(e);
        }

    }

    public void failed(Throwable throwable) {
        listener.failedGettingTrips((IOException) throwable);
    }
}
