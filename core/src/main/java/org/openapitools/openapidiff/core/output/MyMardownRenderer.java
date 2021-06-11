package org.openapitools.openapidiff.core.output;

import static java.lang.String.format;

import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.openapidiff.core.model.ChangedSchema;
import org.openapitools.openapidiff.core.model.DiffContext;
import org.openapitools.openapidiff.core.model.Endpoint;

public class MyMardownRenderer extends MarkdownRender {

  @Override
  protected String listEndpoints(String title, List<Endpoint> endpoints) {
    if (null == endpoints || endpoints.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(sectionTitle(title));
    sb.append("\n");
    endpoints.stream()
        .map(e -> itemEndpoint(e.getMethod().toString(), e.getPathUrl(), e.getSummary()))
        .forEach(sb::append);
    return sb
        .append("\n")
        .toString();

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
        "%s* %s `%s` (%s)\n",
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
    StringBuilder sb = new StringBuilder();
    String type = type(schema.getNewSchema());
    if (schema.isChangedType()) {
      type = type(schema.getOldSchema()) + " -> " + type(schema.getNewSchema());
    }
    if(schema.isIncompatible()) {
      sb.append(
          property(deepness, "Changed property", name, type, schema.getNewSchema().getDescription()));
      sb.append(schema(++deepness, schema));
    }
    return sb.toString();
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
    return "\n>" + metadata ;
  }
}
