package uk.gov.dwp.uc.pairtest.domain;

/**
 * Immutable Object
 */

public class TicketTypeRequest {

	private final int noOfTickets;
	private final Type type;

	public TicketTypeRequest(Type type, int noOfTickets) {
		this.type = type;
		this.noOfTickets = noOfTickets;
	}

	public int getNoOfTickets() {
		return noOfTickets;
	}

	public Type getTicketType() {
		return type;
	}

	public enum Type {
		ADULT(20), CHILD(10), INFANT(0);

		public final int Price;

		private Type(int price) {
			Price = price;
		}
	}
}
