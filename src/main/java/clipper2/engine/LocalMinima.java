package clipper2.engine;

import clipper2.core.PathType;

public final class LocalMinima { // TODO record
	  
	public Vertex vertex;
	public PathType polytype;
	public boolean isOpen = false;


	public LocalMinima(Vertex vertex, PathType polytype) {
		this(vertex, polytype, false);
	}

	public LocalMinima() {
	}

	public LocalMinima(Vertex vertex, PathType polytype, boolean isOpen) {
	  this.vertex = vertex;
	  this.polytype = polytype;
	  this.isOpen = isOpen;
	}
	
	public boolean opEquals(LocalMinima o) {
		return vertex == o.vertex; // NOTE reference equals
	}
	
	public boolean opNotEquals(LocalMinima o) {
		return vertex != o.vertex;
	}


	@Override
	public boolean equals(Object obj) { // TODO
	  boolean tempVar = obj instanceof LocalMinima;
	  LocalMinima minima = tempVar ? (LocalMinima)obj : null;
	  return tempVar && this == minima;
	}

	@Override
	public int hashCode() {
	  return vertex.hashCode();
	}


	  public LocalMinima clone() {
		  LocalMinima varCopy = new LocalMinima();

		  varCopy.vertex = this.vertex;
		  varCopy.polytype = this.polytype;
		  varCopy.isOpen = this.isOpen;

		  return varCopy;
	  }
  }