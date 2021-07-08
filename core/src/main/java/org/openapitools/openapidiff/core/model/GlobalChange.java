package org.openapitools.openapidiff.core.model;


public class GlobalChange implements Comparable<GlobalChange> {
  public enum Operation {
    add, remove, change
  }

  private final String attributeName;
  private final Operation type;

  public GlobalChange(String attributeName,
      Operation type) {
    this.attributeName = attributeName;
    this.type = type;
  }

  public String getAttributeName() {
    return attributeName;
  }

  public Operation getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GlobalChange that = (GlobalChange) o;

    if (!attributeName.equals(that.attributeName)) {
      return false;
    }
    return type == that.type;
  }

  @Override
  public int hashCode() {
    int result = attributeName.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }

  @Override
	public String toString() {
	switch (type) {
	case add:
		return String.format("Added property `%s`", attributeName);
	case remove:
		return String.format("Deleted property `%s`", attributeName);
	case change:
		return String.format("Changed property `%s`", attributeName);
	default:
		return super.toString();
	}
  }

  @Override
  public int compareTo(GlobalChange change) {
    final int typeDiff = type.ordinal() - change.type.ordinal();
    if (typeDiff != 0) {
      return typeDiff;
    } else {
      return attributeName.compareTo(change.attributeName);
    }
  }
}
