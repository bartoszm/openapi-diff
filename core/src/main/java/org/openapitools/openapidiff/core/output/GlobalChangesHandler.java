package org.openapitools.openapidiff.core.output;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openapitools.openapidiff.core.model.Changed;
import org.openapitools.openapidiff.core.model.ChangedApiResponse;
import org.openapitools.openapidiff.core.model.ChangedOperation;
import org.openapitools.openapidiff.core.model.ChangedResponse;
import org.openapitools.openapidiff.core.model.ChangedSchema;
import org.openapitools.openapidiff.core.model.ComposedChanged;
import org.openapitools.openapidiff.core.model.DiffResult;
import org.openapitools.openapidiff.core.model.GlobalChange;
import org.openapitools.openapidiff.core.model.GlobalChange.Operation;

public class GlobalChangesHandler {
  private final Set<String> increase = new HashSet<>();
  private final Set<String> removal = new HashSet<>();
  private final Set<String> change = new HashSet<>();

  public GlobalChangesHandler addChageOfInterest(GlobalChange globalChange) {
    switch(globalChange.getType()) {
      case add:
        increase.add(globalChange.getAttributeName());
        break;
      case remove:
        removal.add(globalChange.getAttributeName());
        break;
      default:
        change.add(globalChange.getAttributeName());
    }
    return this;
  }

  public Set<GlobalChange> identifyChanges(List<ChangedOperation> operations) {

    return operations.stream().flatMap(this::process)
        .collect(Collectors.toSet());
  }

  private Stream<GlobalChange> process(ChangedOperation operation) {
    return Stream.of(
        process(operation.resultRequestBody()),
        process(operation.getApiResponses())
    ).flatMap(s -> s);
  }

  private Stream<GlobalChange> process(ChangedApiResponse responses) {
    if(! responses.isDifferent()) return Stream.empty();
    return responses.getChanged().values()
        .stream().flatMap(this::process);
  }

  private Stream<GlobalChange> process(ChangedResponse resp) {
    return resp.getChangedElements().stream()
        .filter(c -> c instanceof ComposedChanged)
        .flatMap(c -> process(c));
  }

  private Stream<GlobalChange> process(Changed c) {
    if(c instanceof ChangedSchema) {
      return process((ChangedSchema)c);
    }
    if(c instanceof ComposedChanged) {
      return ((ComposedChanged) c).getChangedElements().stream()
          .flatMap(this::process);
    }

    return Stream.empty();
  }

  private Stream<GlobalChange> process(ChangedSchema c) {
    var additions = c.getIncreasedProperties().keySet().stream()
        .filter(increase::contains)
        .map(e -> new GlobalChange(e, Operation.add));
    var removals = c.getMissingProperties().keySet().stream()
        .filter(removal::contains)
        .map(e -> new GlobalChange(e, Operation.remove));

    var changes = c.getChangedProperties().keySet().stream()
        .filter(change::contains)
        .map(e -> new GlobalChange(e, Operation.change));

    return Stream.concat(Stream.concat(additions, removals), changes);
  }

  private Stream<GlobalChange> process(DiffResult request) {
    return null;
  }

}
