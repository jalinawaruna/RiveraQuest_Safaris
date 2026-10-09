package com.boatsafari.state.booking;

import com.boatsafari.exception.BusinessRuleException;
import com.boatsafari.model.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Booking Lifecycle State Design Pattern.
 */
public class BookingStatePatternTest {

    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("BK-TEST-1001");
        booking.setStatus("CONFIRMED");
        booking.setValidationStatus("PENDING_VALIDATION");
    }

    @Test
    @DisplayName("Should successfully transition CONFIRMED -> VALIDATED when Desk Officer checks in customer")
    void testConfirmedToValidatedTransition() {
        BookingStateContext context = new BookingStateContext(booking);
        assertTrue(context.getCurrentState() instanceof ConfirmedBookingState);

        context.validateBoarding("Desk Officer validated ticket at Jetty Counter 1");

        assertTrue(context.getCurrentState() instanceof ValidatedBookingState);
        assertEquals("CONFIRMED", booking.getStatus());
        assertEquals("VALIDATED", booking.getValidationStatus());
    }

    @Test
    @DisplayName("Should successfully transition VALIDATED -> COMPLETED after trip finishes")
    void testValidatedToCompletedTransition() {
        BookingStateContext context = new BookingStateContext(booking);
        context.validateBoarding("Boarded");
        context.complete();

        assertTrue(context.getCurrentState() instanceof CompletedBookingState);
        assertEquals("COMPLETED", booking.getStatus());
        assertEquals("VALIDATED", booking.getValidationStatus());
    }

    @Test
    @DisplayName("Should successfully transition CONFIRMED -> CANCELLED with cancellation reason")
    void testConfirmedToCancelledTransition() {
        BookingStateContext context = new BookingStateContext(booking);
        context.cancel("Customer requested cancellation 24h prior");

        assertTrue(context.getCurrentState() instanceof CancelledBookingState);
        assertEquals("CANCELLED", booking.getStatus());
        assertEquals("Customer requested cancellation 24h prior", booking.getCancellationReason());
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when attempting to cancel an already VALIDATED & boarded booking")
    void testCannotCancelValidatedBooking() {
        BookingStateContext context = new BookingStateContext(booking);
        context.validateBoarding("Customer already aboard");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            context.cancel("Late cancellation request");
        });

        assertTrue(ex.getMessage().contains("Cannot cancel a booking after the customer has already been validated"));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when attempting to validate an already CANCELLED booking")
    void testCannotValidateCancelledBooking() {
        booking.setStatus("CANCELLED");
        BookingStateContext context = new BookingStateContext(booking);

        assertTrue(context.getCurrentState() instanceof CancelledBookingState);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            context.validateBoarding("Attempting invalid validation");
        });

        assertTrue(ex.getMessage().contains("Cannot validate boarding for a cancelled booking"));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when modifying COMPLETED booking")
    void testCannotModifyCompletedBooking() {
        booking.setStatus("COMPLETED");
        BookingStateContext context = new BookingStateContext(booking);

        assertTrue(context.getCurrentState() instanceof CompletedBookingState);

        assertThrows(BusinessRuleException.class, () -> context.cancel("Refund request"));
        assertThrows(BusinessRuleException.class, () -> context.confirm());
        assertThrows(BusinessRuleException.class, () -> context.validateBoarding("Check-in"));
    }

    @Test
    @DisplayName("Pending state allows confirm or cancel, but rejects boarding validation")
    void testPendingBookingStateBehavior() {
        booking.setStatus("PENDING");
        BookingStateContext context = new BookingStateContext(booking);

        assertTrue(context.getCurrentState() instanceof PendingBookingState);

        // Boarding validation should fail on PENDING
        assertThrows(BusinessRuleException.class, () -> context.validateBoarding("Direct check-in"));

        // Confirming should transition to CONFIRMED
        context.confirm();
        assertTrue(context.getCurrentState() instanceof ConfirmedBookingState);
        assertEquals("CONFIRMED", booking.getStatus());
    }
}
