package uk.gov.dwp.uc.pairtest;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

	private static final Long MAX_NUMBER_OF_TICKETS = 20l;

	private final TicketPaymentService paymentService;
	private final SeatReservationService seatsService;

	public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService seatsService) {
		this.paymentService = paymentService;
		this.seatsService = seatsService;
	}

	/**
	 * Should only have private methods other than the one below.
	 */

	@Override
	public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
		try {
			if (!ValidateAccount(accountId))
				throw new InvalidPurchaseException();

			final var ticketRequests = ConsolidateRequests(ticketTypeRequests);

			if (!ValidateTotalTicketQuantity(ticketRequests))
				throw new InvalidPurchaseException();

			if (!ValidateMinorSupervision(ticketRequests))
				throw new InvalidPurchaseException();

			if (!ValidateInfantSeats(ticketRequests))
				throw new InvalidPurchaseException();

			final var cost = CalculateCost(ticketRequests);
			paymentService.makePayment(accountId, cost);

			final var seats = CalculateNumberOfSeats(ticketRequests);
			seatsService.reserveSeat(accountId, seats);
		} catch (InvalidPurchaseException e) {
			throw e;
		} catch (Throwable e) {
			throw new InvalidPurchaseException();
		}
	}

	private Boolean ValidateAccount(Long accountId) {
		return accountId > 0;
	}

	private Map<Type, Integer> ConsolidateRequests(TicketTypeRequest[] ticketTypeRequests) {
		final var ticketRequests = Stream.of(ticketTypeRequests).collect(
				Collectors.groupingBy(
						TicketTypeRequest::getTicketType,
						Collectors.reducing(0, TicketTypeRequest::getNoOfTickets, Integer::sum)));
		return ticketRequests;
	}

	private Boolean ValidateMinorSupervision(Map<Type, Integer> ticketRequests) {
		final var adultTickets = ticketRequests.getOrDefault(Type.ADULT, 0);

		return adultTickets >= 1;
	}

	private Boolean ValidateTotalTicketQuantity(Map<Type, Integer> ticketRequests) {
		final var totalTickets = ticketRequests.values().stream().reduce(Integer::sum).orElse(0);
		return totalTickets > 0 && totalTickets <= MAX_NUMBER_OF_TICKETS;
	}

	private boolean ValidateInfantSeats(Map<Type, Integer> ticketRequests) {
		final var infantTickets = ticketRequests.getOrDefault(Type.INFANT, 0);
		final var adultTickets = ticketRequests.getOrDefault(Type.ADULT, 0);

		return infantTickets <= adultTickets;
	}

	private int CalculateCost(Map<Type, Integer> ticketRequests) {
		final var cost = ticketRequests.entrySet().stream()
				.collect(Collectors.summingInt((request) -> { return request.getKey().Price * request.getValue(); }));

		return cost;
	}

	private int CalculateNumberOfSeats(Map<Type, Integer> ticketRequests) {
		final var seatRequests = ticketRequests.entrySet().stream()
				.filter((request) -> { return request.getKey() != Type.INFANT; });

		final var totalSeats = seatRequests.collect(Collectors.summingInt(Map.Entry<Type, Integer>::getValue));

		return totalSeats;
	}
}
