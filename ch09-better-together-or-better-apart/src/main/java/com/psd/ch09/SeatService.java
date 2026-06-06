package com.psd.ch09;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simulated backing service: reserves and releases licensed seats.
 */
public final class SeatService {
    private final Map<String, Integer> seatsByReservationId = new LinkedHashMap<>();

    public String reserve(String workspace, int seats) {
        String reservationId = "seats:" + workspace;
        seatsByReservationId.put(reservationId, seats);
        System.out.println("  seats reserved:    " + reservationId + " (" + seats + ")");
        return reservationId;
    }

    public void release(String reservationId) {
        seatsByReservationId.remove(reservationId);
        System.out.println("  seats released:    " + reservationId);
    }

    public Map<String, Integer> contents() {
        return Collections.unmodifiableMap(seatsByReservationId);
    }
}
