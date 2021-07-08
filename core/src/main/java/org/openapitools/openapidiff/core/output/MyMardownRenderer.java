package org.openapitools.openapidiff.core.output;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.openapidiff.core.model.ChangedContent;
import org.openapitools.openapidiff.core.model.ChangedMediaType;
import org.openapitools.openapidiff.core.model.ChangedMetadata;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.ChangedResponse;
import org.openapitools.openapidiff.core.model.ChangedSchema;
import org.openapitools.openapidiff.core.model.DiffResult;
import org.openapitools.openapidiff.core.model.Endpoint;
import org.openapitools.openapidiff.core.model.GlobalChange;

public class MyMardownRenderer extends MarkdownRender {

  protected List<Predicate<Endpoint>> exclude = new LinkedList<>();
  private Set<GlobalChange> globalChanges;


  public MyMardownRenderer addExclude(
      Predicate<Endpoint> e) {
    this.exclude.add(Objects.requireNonNull(e));
    return this;
  }

  @Override
  public String render(ChangedOpenApi diff) {

    return super.render(diff) + globalChanges(diff);
  }

  private String globalChanges(ChangedOpenApi diff) {
    if(globalChanges.isEmpty()) {
      return "";
    }

    var title = sectionTitle("Global Changes");
    return globalChanges.stream()
        .sorted(GlobalChange::compareTo)
        .map(c -> String.format("- %s\n", c))
        .collect(Collectors.joining("", title, ""));

  }

  @Override
  protected String listEndpoints(String title, List<Endpoint> endpoints) {
    if (null == endpoints || endpoints.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(sectionTitle(title));
    sb.append("\n");
    endpoints.stream()
        .filter(e -> exclude.stream().noneMatch(p -> p.test(e)))
        .map(e -> itemEndpoint(e.getMethod().toString(), e.getPathUrl(), e.getSummary()))
        .forEach(sb::append);
    return sb
        .append("\n")
        .toString();

  }

  @Override
  protected String itemResponse(String code, ChangedResponse response) {

    var title = this.itemResponse(
        "Changed response",
        code,
        null == response.getNewApiResponse()
            ? ""
            : response.getNewApiResponse().getDescription());

    var content = Optional.ofNullable(response.getContent())
        .map(r -> bodyContent(LI, r))
        .orElse("");

    var header = headers(response.getHeaders());

    if(StringUtils.isBlank(header + content)) {
      return "";
    }

    return title
        + header
        + content;
  }

  @Override
  protected String bodyContent(String prefix, ChangedContent changedContent) {
    if (changedContent == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder("\n");
    sb.append(listContent(prefix, "New content type", changedContent.getIncreased()));
    sb.append(listContent(prefix, "Deleted content type", changedContent.getMissing()));
    final int deepness = StringUtils.isBlank(prefix) ? 0 : 1;

    changedContent.getChanged().entrySet().stream()
        .map(e -> this.itemContent(deepness, e.getKey(), e.getValue()))
        .filter(StringUtils::isNoneBlank)
        .forEach(e -> sb.append(prefix).append(e));
    return sb.toString();
  }

  @Override
  protected String itemContent(int deepness, String mediaType, ChangedMediaType content) {
    var schema = schema(deepness, content.getSchema());
    if(StringUtils.isBlank(schema)) {
      return "";
    }
    return itemContent("Changed content type", mediaType) + schema;
  }

  @Override
  protected String itemEndpoint(String method, String path, String summary) {
    return "- " + CODE + method + CODE +  bold(" ", path)   + summary(summary) + "\n";
  }

  private String bold(String pfix, String path) {
    if(StringUtils.isBlank(path)) {
      return path;
    }
    return pfix + "**"  + path + "**" + pfix;
  }

  @Override
  protected String sectionTitle(String title) {
    return H4 + title + '\n';
  }

  @Override
  protected String property(
      int deepness, String title, String name, String type, String description) {
    return format(
        "%s- %s `%s` (%s)\n",
        indent(deepness), title, name, type);
  }


  @Override
  protected String items(int deepness, String title, String type, String description) {
    return format(
        "%s%s (%s):" + "\n",
        indent(deepness), title, type);
  }

  @Override
  protected String property(int deepness, String name, ChangedSchema schema) {
    String type = type(schema.getNewSchema());
    if (schema.isChangedType()) {
      type = type(schema.getOldSchema()) + " -> " + type(schema.getNewSchema());
    }
    var toProcess = schema.getChangedElements().stream()
        .filter(Objects::nonNull)
        .filter(e -> !(e instanceof ChangedMetadata))
        .anyMatch(e -> e.isChanged().getWeight() > DiffResult.METADATA.getWeight());


    if(toProcess) {
      var embedded = schema(deepness + 1, schema);
      if(! StringUtils.isBlank(embedded)) {
        return property(deepness, "Changed property", name, type, schema.getNewSchema().getDescription())
            + embedded;
      }
    }
    return "";
  }

  @Override
  protected String items(int deepness, ChangedSchema schema) {
    var embedded = schema(deepness, schema);

    String type = type(schema.getNewSchema());
    if (schema.isChangedType()) {
      type = type(schema.getOldSchema()) + " -> " + type(schema.getNewSchema());
    }
    if(StringUtils.isBlank(embedded)) {
      return "";
    }
    return items(deepness, "Changed items", type, schema.getNewSchema().getDescription())
        + embedded;
  }

  @Override
  protected String itemResponse(String title, String code, String description) {
    StringBuilder sb = new StringBuilder();
    String status = "";
    if (!code.equals("default")) {
      status = HttpStatus.getStatusText(Integer.parseInt(code));
    }
    sb.append(format("%s : **%s %s**\n", title, code, status));
//    sb.append(metadata(description));
    return sb.toString();
  }

  protected String summary(String metadata) {
    if (StringUtils.isBlank(metadata)) {
      return "";
    }
    return metadata ;
  }

  public MyMardownRenderer addGlobal(Set<GlobalChange> globalChanges) {
    this.globalChanges = globalChanges;
    return this;
  }
}
