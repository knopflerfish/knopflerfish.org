package org.knopflerfish.ant.taskdefs.util;

import java.io.File;
import java.io.IOException;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.knopflerfish.ant.taskdefs.bundle.FileUtil;

import java.util.List;
import java.util.Map;


public class Markdown2HtmlTask
  extends Task
{
  private File from;
  private File to;
  private String type;
  private final String HTML_TYPE_RELEASE_NOTES = "release_notes";
  
  public void setFromfile(File f) {
    from = f;
  }

  public void setTofile(File f) {
    to = f;
  }

  public void setType(String s) {
    type = s;
  }

  @Override
  public void execute() {
    if (from == null) {
      throw new BuildException("fromfile must be set");
    }
    if (to == null) {
      throw new BuildException("tofile must be set");
    }

    if (!from.exists()) {
      throw new BuildException("fromfile does not exist: " + from.getPath());
    }

    try {
      String src = FileUtil.loadFile(from.getPath());
      MutableDataSet options = new MutableDataSet();

      // uncomment to set optional extensions
      //options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

      // uncomment to convert soft-breaks to hard breaks
      //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

      Parser parser = Parser.builder(options).build();
      HtmlRenderer renderer = HtmlRenderer.builder(options).build();

      // You can re-use parser and renderer instances
      Node document = parser.parse(src);
      String html = renderer.render(document);
      FileUtil.writeStringToFile(to, html);
    }
    catch (final IOException e) {
      throw new BuildException("Failure in markdown2hhtml: " + e, e);
    }
  }
}	


/*
class MyToHtmlSerializer extends ToHtmlSerializer {

  public MyToHtmlSerializer(LinkRenderer linkRenderer) {
    super(linkRenderer);
  }

  public MyToHtmlSerializer(LinkRenderer linkRenderer, List<ToHtmlSerializerPlugin> plugins) {
    super(linkRenderer, plugins);
  }

  public MyToHtmlSerializer(LinkRenderer linkRenderer, Map<String, VerbatimSerializer> verbatimSerializers) {
    super(linkRenderer, verbatimSerializers);
  }

  public MyToHtmlSerializer(LinkRenderer linkRenderer, Map<String, VerbatimSerializer> verbatimSerializers, List<ToHtmlSerializerPlugin> plugins) {
    super(linkRenderer, verbatimSerializers, plugins);
  }


  public void visit(HeaderNode node) {
    if (node.getLevel() == 3) {
      printBreakBeforeTag(node, "div", "class=\"note_name\"");
    }
    else {
      printBreakBeforeTag(node, "h" + node.getLevel() , "class=\"release_notes\"");
    }
  }

  public void visit(ListItemNode node) {
    if (node instanceof TaskListNode) {
      super.visit(node);
    }
    else {
      printConditionallyIndentedTag(node, "div", "class=\"note_item\"");
    }
  }

  public void visit(BulletListNode node) {
    visitChildren(node);
    // printIndentedTag(node, "ul");
  }

  void printConditionallyIndentedTag(SuperNode node, String tag, String attributes) {
    if (node.getChildren().size() > 1) {
      printer.println().print('<').print(tag).print(' ').print(attributes).print('>').indent(+2);
      visitChildren(node);
      printer.indent(-2).println().print('<').print('/').print(tag).print('>');
    } else {
      boolean startWasNewLine = printer.endsWithNewLine();

      printer.println().print('<').print(tag).print(' ').print(attributes).print('>');
      visitChildren(node);
      printer.print('<').print('/').print(tag).print('>').printchkln(startWasNewLine);
    }
  }

  void printBreakBeforeTag(SuperNode node, String tag, String attributes) {
    boolean startWasNewLine = printer.endsWithNewLine();
    printer.println();
    printTag(node, tag, attributes);
    if (startWasNewLine) printer.println();
  }  

  void printTag(TextNode node, String tag, String attributes) {
    printer.print('<').print(tag).print(' ').print(attributes).print('>');
    printer.printEncoded(node.getText());
    printer.print('<').print('/').print(tag).print('>');
  }

  void printTag(SuperNode node, String tag, String attributes) {
    printer.print('<').print(tag).print(' ').print(attributes).print('>');
    visitChildren(node);
    printer.print('<').print('/').print(tag).print('>');
  }
}
*/
