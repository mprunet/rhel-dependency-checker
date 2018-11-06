package eu.prunet.security.rhelchecker.eval.formatting;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;

import java.util.Stack;

import static j2html.TagCreator.*;

public class J2HtmlPresenter implements Presenter {

    private final Stack<ContainerTag> stack = new Stack<>();

    public J2HtmlPresenter() {
        stack.push(tag(null));
    }

    @Override
    public Presenter indent() {
        ContainerTag ul = ul().withClass("no-icon");
        stack.peek().with(ul);
        stack.push(ul);
        return this;
    }

    @Override
    public void writeLine(boolean style, String line) {
        stack.peek().with(li(line).withClass(style ? "check-solid" : "times-solid"));
    }

    @Override
    public void close() {
        stack.pop();
    }

    public DomContent toDomContent() {
        return stack.peek();
    }

}
