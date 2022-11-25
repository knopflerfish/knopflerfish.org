/*
 * Copyright (c) 2016-2022, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knopflerfish.ant.taskdefs.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.knopflerfish.ant.taskdefs.bundle.FileUtil;


class RunChildren implements Runnable {

  private Node node;
  private NodeRendererContext context;

  public RunChildren(Node node, NodeRendererContext ctx) {
    this.node = node;
    this.context = ctx;
  }

  @Override
  public void run() {
    context.renderChildren(node);
  }
}


class KfReleaseNotesRenderer implements NodeRenderer {

  public KfReleaseNotesRenderer(@SuppressWarnings("unused") DataHolder options) {
  }

  @Override
  @SuppressWarnings("Convert2Lambda")
  public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
    return new HashSet<>(Arrays.asList(
        new NodeRenderingHandler<>(Heading.class, new CustomNodeRenderer<Heading>() {
          @Override
          public void render(final Heading node, final NodeRendererContext context, final HtmlWriter html) {
            if (node.getLevel() == 3) {
              html.attr("class", "note_name").withAttr().tagLine("div", new RunChildren(node, context));
            } else {
              html.srcPos(node.getText()).attr("class", "release_notes").withAttr().tagLine("h" + node.getLevel(), new RunChildren(node, context));
            }
          }
        }),
        new NodeRenderingHandler<>(BulletList.class, new CustomNodeRenderer<BulletList>() {
          @Override
          public void render(final BulletList node, final NodeRendererContext context, final HtmlWriter html) {
            if (node.getParent() instanceof BulletListItem) {
              html.withAttr().tagIndent("ul", new RunChildren(node, context));
            } else {
              html.raw("");
              context.renderChildren(node);
            }
          }
        }),
        new NodeRenderingHandler<>(BulletListItem.class, new CustomNodeRenderer<BulletListItem>() {
          @Override
          public void render(final BulletListItem node, final NodeRendererContext context, final HtmlWriter html) {
            if (node.getParent().getParent() instanceof BulletListItem) {
              html.withAttr().tagIndent("li", new RunChildren(node, context));
            } else {
              html.attr("class", "note_item").withAttr().tagIndent("div", new RunChildren(node, context));
            }
          }
        })
    ));
  }

  public static class Factory implements NodeRendererFactory {
    @Override
    public NodeRenderer create(final DataHolder options) {
      return new KfReleaseNotesRenderer(options);
    }
  }
}


class KfReleaseNotesExtension implements HtmlRenderer.HtmlRendererExtension {

  private KfReleaseNotesExtension() {
  }

  public static Extension create() {
    return new KfReleaseNotesExtension();
  }

  @Override
  public void rendererOptions(final MutableDataHolder options) {
  }

  @Override
  public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
    if (rendererType.equals("HTML")) {
      rendererBuilder.nodeRendererFactory(new KfReleaseNotesRenderer.Factory());
    }
  }
}


public class Markdown2HtmlTask
  extends Task
{
  private File from;
  private File to;

  public void setFromfile(File f) {
    from = f;
  }

  public void setTofile(File f) {
    to = f;
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

      options.set(Parser.EXTENSIONS, Collections.singletonList(KfReleaseNotesExtension.create()));
      Parser parser = Parser.builder(options).build();
      HtmlRenderer renderer = HtmlRenderer.builder(options).build();

      Node document = parser.parse(src);
      String html = renderer.render(document);
      FileUtil.writeStringToFile(to, html);
    }
    catch (final IOException e) {
      throw new BuildException("Failure in Markdown2HtmlTask: " + e, e);
    }
  }
}	
