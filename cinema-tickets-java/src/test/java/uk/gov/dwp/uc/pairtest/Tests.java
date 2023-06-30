package uk.gov.dwp.uc.pairtest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class Tests {

	private static TicketPaymentService payment;
	private static SeatReservationService seats;
	private static TicketService service;

	@Before
	public void InitTests() {
		payment = mock(TicketPaymentService.class);
		seats = mock(SeatReservationService.class);

		service = new TicketServiceImpl(payment, seats);
	}

	@Test(expected = InvalidPurchaseException.class)
	public void givenSingleRequest_whenNegativeAccountId_shouldThrowException() {
		service.purchaseTickets(-99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 1) });
	}

	@Test(expected = InvalidPurchaseException.class)
	public void givenSingleRequest_whenZeroAccountId_shouldThrowException() {
		service.purchaseTickets(0l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 1) });
	}

	@Test
	public void givenSingleRequest_whenPositiveAdultQuantity_shouldSellTickets() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 2) });
	}

	@Test(expected = InvalidPurchaseException.class)
	public void givenSingleRequest_whenNegativeAdultQuantity_shouldThrowException() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, -1) });
	}

	@Test(expected = InvalidPurchaseException.class)
	public void givenSingleRequest_whenZeroAdultQuantity_shouldThrowException() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 0) });
	}

	@Test
	public void givenMultipleRequest_whenPositiveQuantity_shouldSellTickets() {
		service.purchaseTickets(99l,
				new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 1), new TicketTypeRequest(Type.ADULT, 1) });
	}

	@Test(expected = InvalidPurchaseException.class)
	public void givenRequest_whenMoreThan20TicketsRequested_shouldSellTickets() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 15),
				new TicketTypeRequest(Type.CHILD, 8) });
	}

	@Test
	public void givenRequest_when20TicketsRequested_shouldSellTickets() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 2),
				new TicketTypeRequest(Type.ADULT, 15), new TicketTypeRequest(Type.ADULT, 3) });
	}

	@Test(expected = InvalidPurchaseException.class)
	public void givenRequest_whenMoreInfantsThanAdultsRequested_shouldThrowException() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.INFANT, 2),
				new TicketTypeRequest(Type.ADULT, 1), new TicketTypeRequest(Type.CHILD, 1) });
	}

	@Test
	public void givenRequest_whenSameAdultsAsInfantsRequested_shouldThrowException() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.INFANT, 2),
				new TicketTypeRequest(Type.ADULT, 2) });
	}

	@Test(expected = InvalidPurchaseException.class)
	public void givenRequest_whenNoAdults_shouldThrowException() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.CHILD, 2) });
	}

	@Test
	public void givenRequest_whenSingleAdults_shouldRequestCorrectPayment() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 1) });
		verify(payment, times(1)).makePayment(99l, Type.ADULT.Price);
	}

	@Test
	public void givenRequest_whenAdultsWithInfant_shouldRequestCorrectPayment() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 1), new TicketTypeRequest(Type.INFANT, 1) });
		verify(payment, times(1)).makePayment(99l, Type.ADULT.Price + Type.INFANT.Price);
	}

	@Test
	public void givenRequest_whenAdultWithChild_shouldRequestCorrectPayment() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.CHILD, 1), new TicketTypeRequest(Type.ADULT, 1) });
		verify(payment, times(1)).makePayment(99l, Type.CHILD.Price + Type.ADULT.Price);
	}

	@Test
	public void givenRequest_whenSingleAdult_shouldReserveSingleSeat() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.ADULT, 1) });
		verify(seats, times(1)).reserveSeat(99l, 1);
	}

	@Test
	public void givenRequest_whenAdultsWithInfant_shouldReserveSingleSeat() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.INFANT, 1), new TicketTypeRequest(Type.ADULT, 1) });
		verify(seats, times(1)).reserveSeat(99l, 1);
	}

	@Test
	public void givenRequest_whenAdultWithChild_shouldReserveTwoSeats() {
		service.purchaseTickets(99l, new TicketTypeRequest[] { new TicketTypeRequest(Type.CHILD, 1), new TicketTypeRequest(Type.ADULT, 1) });
		verify(seats, times(1)).reserveSeat(99l, 2);
	}
}
