package clipper2.engine;

import clipper2.core.PathType;

public final class LocalMinima { // TODO record
	  
	public Vertex vertex;
	public PathType polytype;
	public boolean isOpen;


	public LocalMinima(Vertex vertex, PathType polytype) {
		this(vertex, polytype, false);
	}

	public LocalMinima() {
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public LocalMinima(Vertex vertex, PathType polytype, bool isOpen = false)
	public LocalMinima(Vertex vertex, PathType polytype, boolean isOpen) {
	  this.vertex = vertex;
	  this.polytype = polytype;
	  this.isOpen = isOpen;
	}

	public static boolean opEquals(LocalMinima lm1, LocalMinima lm2) {
	  return lm1.vertex == lm2.vertex;
	}

	public static boolean opNotEquals(LocalMinima lm1, LocalMinima lm2) {
	  return !opEquals(lm1.clone(), lm2.clone());
	}

	@Override
	public boolean equals(Object obj) { // TODO
	  boolean tempVar = obj instanceof LocalMinima;
	  LocalMinima minima = tempVar ? (LocalMinima)obj : null;
	  return tempVar && opEquals(this.clone(), minima);
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