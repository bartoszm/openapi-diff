package org.openapitools.openapidiff.core;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.openapitools.openapidiff.core.compare.OpenApiDiff;
import org.openapitools.openapidiff.core.compare.SchemaDiff;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.ChangedSchema;
import org.openapitools.openapidiff.core.model.DiffContext;
import org.openapitools.openapidiff.core.model.GlobalChange;
import org.openapitools.openapidiff.core.model.GlobalChange.Operation;

public class FillteringDiff extends OpenApiDiff {

  private List<GlobalChange> ignoredChanges;
  private Set<GlobalChange> observedChanges = new HashSet<>();

  public FillteringDiff(OpenAPI oldSpecOpenApi,
      OpenAPI newSpecOpenApi) {
    super(oldSpecOpenApi, newSpecOpenApi);
    ignoredChanges = new LinkedList<>();

  }

  public FillteringDiff filter(GlobalChange change) {
    ignoredChanges.add(change);
    return this;
  }

  @Override
  public ChangedOpenApi compare() {
    overrideSchemaDiff();
    return super.compare();
  }

  public Set<GlobalChange> getObservedChanges() {
    return observedChanges;
  }

  protected void overrideSchemaDiff() {
    super.initializeFields();
    this.schemaDiff = new SchemaDiff(this) {
      @Override
      protected Optional<ChangedSchema> computeDiff(HashSet<String> refSet, Schema left,
          Schema right, DiffContext context) {
        return super.computeDiff(refSet, left, right, context)
            .map(diff -> {
              diff.setChangedProperties(filter(diff.getChangedProperties(), Operation.change));
              diff.setIncreasedProperties(filter(diff.getIncreasedProperties(), Operation.add));
              diff.setMissingProperties(filter(diff.getMissingProperties(), Operation.remove));
              return diff;
            });
      }
    };
  }

  private <V> Map<String, V> filter(Map<String, V> changedProperties,
      Operation oper) {
    var toFilter = ignoredChanges.stream()
        .filter(g -> g.getType() == oper)
        .map(GlobalChange::getAttributeName)
        .collect(Collectors.toSet());
    if(toFilter.isEmpty()) return changedProperties;

    toFilter.stream()
        .map(f -> new GlobalChange(f, oper))
        .forEach(f -> observedChanges.add(f));

    return changedProperties.entrySet().stream()
        .filter(e -> !toFilter.contains(e.getKey()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }
}
