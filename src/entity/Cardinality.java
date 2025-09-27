package entity;

import java.util.Objects;

/**
 * An FD cardinality refers to the upper bound of the redundancy of an FD
 *
 */
public class Cardinality {
	private FD fd;
	private int card;
	
	public Cardinality() {}
	
	public Cardinality(FD fd, int card) {
		this.fd = fd;
		this.card = card;
	}

	public FD getFd() {
		return fd;
	}

	public void setFd(FD fd) {
		this.fd = fd;
	}

	public int getCard() {
		return card;
	}

	public void setCard(int card) {
		this.card = card;
	}

	@Override
	public String toString() {
		return fd.toString() +" : "+card;
	}

	@Override
	public int hashCode() {
		return Objects.hash(card, fd);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cardinality other = (Cardinality) obj;
		return card == other.card && fd.equals(other.fd);
	}
	
}
