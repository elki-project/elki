package de.lmu.ifi.dbs.utilities;

public class IntegerTriple implements Comparable<IntegerTriple> {

	private Integer first;
	private Integer second;
	private Integer last;

	public int getFirst() {
		return this.first;
	}

	public int getSecond() {
		return this.second;
	}

	public int getLast() {
		return this.last;
	}

	public IntegerTriple(int first, int second, int last) {
		this.first = first;
		this.second = second;
		this.last = last;
	}
	
	public IntegerTriple(){
		
	}

	public int compareTo(IntegerTriple o) {
		if (this.getFirst() > o.getFirst()) {
			return 1;
		} else if (this.getFirst() < o.getFirst()) {
			return -1;
		} else {// this.getFirst() == o.getFirst()
			if (this.getSecond() > o.getSecond()) {
				return 1;
			} else if (this.getSecond() < o.getSecond()) {
				return -1;
			} else {// this.getSecond() == o.getSecond()
				if (this.getLast() > o.getLast()) {
					return 1;
				} else if (this.getLast() < o.getLast()) {
					return -1;
				} else {
					return 0;
				}
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((last == null) ? 0 : last.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final IntegerTriple other = (IntegerTriple) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (last == null) {
			if (other.last != null)
				return false;
		} else if (!last.equals(other.last))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	public void setFirst(int first) {
		this.first = first;
	}

	public void setSecond(int second) {
		this.second = second;
	}

	public void setLast(int last) {
		this.last = last;
	}

}
