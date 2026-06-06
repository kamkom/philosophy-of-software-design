package com.psd.ch09.after;

import com.psd.ch09.SeatService;

/**
 * Workspace step: reserves a number of seats. The seat count is configured by
 * the policy layer when it builds the step, so the same step type can be reused
 * with different counts.
 */
public final class ReserveSeatsStep implements Step<ProvisioningContext> {
    private final SeatService seatService;
    private final int seats;

    public ReserveSeatsStep(SeatService seatService, int seats) {
        this.seatService = seatService;
        this.seats = seats;
    }

    @Override
    public void execute(ProvisioningContext ctx) {
        ctx.seatReservationId = seatService.reserve(ctx.name, seats);
    }

    @Override
    public void compensate(ProvisioningContext ctx) {
        seatService.release(ctx.seatReservationId);
    }
}
