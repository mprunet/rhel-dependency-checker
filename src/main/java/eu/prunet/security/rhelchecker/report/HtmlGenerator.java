package eu.prunet.security.rhelchecker.report;

import eu.prunet.security.rhelchecker.eval.IEval;
import eu.prunet.schema.oval.def.DefinitionType;
import eu.prunet.schema.oval.def.MetadataType;
import eu.prunet.security.rhelchecker.eval.formatting.J2HtmlPresenter;
import eu.prunet.util.Pair;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.InlineStaticResource;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static j2html.TagCreator.*;


public class HtmlGenerator {
    private final AtomicInteger ai = new AtomicInteger();

    private final Map<String, String> CVSS3 = new HashMap<>();
    private final Map<String, String> CVSS2 = new HashMap<>();

    private ResourceBundle resourceBundle;

    public HtmlGenerator(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        CVSS3.put("AV:N", resourceBundle.getString("attack.vector.network"));
        CVSS3.put("AV:A", resourceBundle.getString("attack.vector.adjacent.network"));
        CVSS3.put("AV:L", resourceBundle.getString("attack.vector.local"));
        CVSS3.put("AV:P", resourceBundle.getString("attack.vector.physical"));
        CVSS3.put("AC:L", resourceBundle.getString("attack.complexity.low"));
        CVSS3.put("AC:H", resourceBundle.getString("attack.complexity.high"));
        CVSS3.put("PR:N", resourceBundle.getString("privileges.required.none"));
        CVSS3.put("PR:L", resourceBundle.getString("privileges.required.low"));
        CVSS3.put("PR:H", resourceBundle.getString("privileges.required.high"));
        CVSS3.put("UI:N", resourceBundle.getString("user.interaction.none"));
        CVSS3.put("UI:R", resourceBundle.getString("user.interaction.required"));
        CVSS3.put("S:U", resourceBundle.getString("scope.unchanged"));
        CVSS3.put("S:C", resourceBundle.getString("scope.changed"));
        CVSS3.put("C:N", resourceBundle.getString("confidentiality.none"));
        CVSS3.put("C:L", resourceBundle.getString("confidentiality.low"));
        CVSS3.put("C:H", resourceBundle.getString("confidentiality.high"));
        CVSS3.put("I:N", resourceBundle.getString("integrity.none"));
        CVSS3.put("I:L", resourceBundle.getString("integrity.low"));
        CVSS3.put("I:H", resourceBundle.getString("integrity.high"));
        CVSS3.put("A:N", resourceBundle.getString("availability.none"));
        CVSS3.put("A:L", resourceBundle.getString("availability.low"));
        CVSS3.put("A:H", resourceBundle.getString("availability.high"));

        CVSS2.put("AV:N", resourceBundle.getString("attack.vector.network"));
        CVSS2.put("AV:A", resourceBundle.getString("attack.vector.adjacent.network"));
        CVSS2.put("AV:L", resourceBundle.getString("attack.vector.local"));
        CVSS2.put("AC:L", resourceBundle.getString("attack.complexity.low"));
        CVSS2.put("AC:M", resourceBundle.getString("attack.complexity.medium"));
        CVSS2.put("AC:H", resourceBundle.getString("attack.complexity.high"));
        CVSS2.put("AU:M", resourceBundle.getString("authentication.multiple"));
        CVSS2.put("AU:S", resourceBundle.getString("authentication.single"));
        CVSS2.put("AU:N", resourceBundle.getString("authentication.none"));
        CVSS2.put("C:N", resourceBundle.getString("confidentiality.none"));
        CVSS2.put("C:P", resourceBundle.getString("confidentiality.partial"));
        CVSS2.put("C:C", resourceBundle.getString("confidentiality.complete"));
        CVSS2.put("I:N", resourceBundle.getString("integrity.none"));
        CVSS2.put("I:P", resourceBundle.getString("integrity.partial"));
        CVSS2.put("I:C", resourceBundle.getString("integrity.complete"));
        CVSS2.put("A:N", resourceBundle.getString("availability.none"));
        CVSS2.put("A:P", resourceBundle.getString("availability.partial"));
        CVSS2.put("A:C", resourceBundle.getString("availability.complete"));

    }

    public void generate(Appendable writer, List<Pair<DefinitionType, IEval>> eval) throws IOException {

        html(
                head(
                        meta().attr("http-equiv", "content-type").attr("content", "text/html; charset=UTF-8"),
                        meta().attr("charset", "UTF-8"),
                        styleWithInlineFile("/datatables.min.css"),
                        scriptWithInlineFile("/datatables.min.js"),
                        styleWithInlineFile("/custom-style.css")
                ),
                body(
                        h1(resourceBundle.getString("title.summary")),
                        table(
                                thead(
                                        tr(
                                                th(resourceBundle.getString("title")),
                                                th(resourceBundle.getString("severity")),
                                                th(resourceBundle.getString("cve"))
                                        )
                                ),
                                tbody(
                                        each(eval, e -> tr(
                                                summary(e)
                                        ))
                                )
                        ).withId("summary").withClasses("table", "table-striped", "table-bordered").withStyle("width:100%")
                                .withData("data-order", "[[ 2, \"desc\" ]]"),
                        rawHtml(InlineStaticResource.getFileAsString("/datatables.ini.html"))

                ),
                p(),
                h1(resourceBundle.getString("vulnerabilities")),
                each(eval, this::detailed),
                h1(resourceBundle.getString("licenses")),
                h2(resourceBundle.getString("product.name")),
                div(    rawHtml(String.format(resourceBundle.getString("product.license.notice"), resourceBundle.getString("product.name"))))
                        .withClass("content-2"),
                h2("Third party"),
                div(
                        rawHtml(String.format(resourceBundle.getString("product.license.libs"),resourceBundle.getString("product.name"))
                        )).withClass("content-2"),
                footer(resourceBundle.getString("product.name") + " " + resourceBundle.getString("product.version"))
                        .withClass("footer")


        ).render(writer);
    }

    private DomContent summary(Pair<DefinitionType, IEval> p) {
        MetadataType meta = p.getOne().getMetadata();
        String severity = getAdvisoryChildStream(meta, "severity")
                .map(Node::getTextContent)
                .findFirst().orElse("");

        if (!"".equals(severity)) {
            try {
                severity = resourceBundle.getString("redhat." + severity.toLowerCase());
            } catch (MissingResourceException ex) { // Not translated
            }
        }
        return tag(null).with(
                td(a(meta.getTitle()).withHref("#" + p.getOne().getId())),
                td(severity),
                td(
                        join(
                                meta.getReference()
                                        .stream()
                                        .filter(r -> "cve".equalsIgnoreCase(r.getSource()))
                                        .map(r -> span(a(r.getRefId()).withHref(r.getRefUrl())))
                                        .toArray()
                        )
                )

        );
    }

    public static Stream<Node> getAdvisoryChildStream(MetadataType meta, String localName) {
        return meta.getAny().stream()
                .map(Node.class::cast)
                .filter(e -> "advisory".equals(e.getLocalName()))
                .flatMap(e ->
                        {
                            NodeList nl = e.getChildNodes();
                            return IntStream.range(0, nl.getLength())
                                    .mapToObj(nl::item);
                        }
                )
                .map(Node.class::cast)
                .filter(e -> localName.equals(e.getLocalName()));
    }


    private Collection<Node> getAdvisoryChildCollection(MetadataType meta, String localName) {
        return getAdvisoryChildStream(meta, localName).collect(Collectors.toList());

    }

    public static String attr(Node node, String attribute) {
        return Optional.ofNullable(node.getAttributes().getNamedItem(attribute)).map(Node::getTextContent).orElse("");
    }

    private DomContent references(Pair<DefinitionType, IEval> p) {
        MetadataType meta = p.getOne().getMetadata();

        return table(
                thead(
                        tr(
                                th(resourceBundle.getString("cve")),
                                th(resourceBundle.getString("cwe")),
                                th(resourceBundle.getString("impact")),
                                th(resourceBundle.getString("date")),
                                th(resourceBundle.getString("cvss")),
                                th(resourceBundle.getString("links"))
                        )
                ),
                tbody(
                        each(getAdvisoryChildCollection(meta, "cve"), e -> tr(
                                td(a(e.getTextContent()).withHref("https://cve.mitre.org/cgi-bin/cvename.cgi?name=" + e.getTextContent()).withTarget(e.getTextContent())),
                                cwe(e),
                                td(attr(e, "impact")),
                                td(attr(e, "public")),
                                cvss(e),
                                link(e)
                        ))
                )
        ).withClasses("cve", "table", "table-striped", "table-bordered", "content-3").withStyle("width:100%");

    }

    private Pair<String, String> extractCVSS3TextClass(String sScore) {
        double score = Double.parseDouble(sScore);
        String scoreText = sScore + " : ";
        String scoreClass;
        if (score < 4.0) {
            scoreText += resourceBundle.getString("cvss3.low");
            scoreClass = "btn-primary";
        } else if (score < 7.0) {
            scoreText += resourceBundle.getString("cvss3.medium");
            scoreClass = "btn-warning";
        } else if (score < 9.0) {
            scoreText += resourceBundle.getString("cvss3.high");
            scoreClass = "btn-danger";
        } else {
            scoreText += resourceBundle.getString("cvss3.critical");
            scoreClass = "btn-dark";
        }
        return Pair.of(scoreText, scoreClass);
    }

    private Pair<String, String> extractCVSS2TextClass(String sScore) {
        double score = Double.parseDouble(sScore);
        String scoreText = sScore + " : ";
        String scoreClass;
        if (score < 4.0) {
            scoreText += resourceBundle.getString("cvss2.low");
            scoreClass = "btn-primary";
        } else if (score < 7.0) {
            scoreText += resourceBundle.getString("cvss2.medium");
            scoreClass = "btn-warning";
        } else {
            scoreText += resourceBundle.getString("cvss2.high");
            scoreClass = "btn-danger";
        }
        return Pair.of(scoreText, scoreClass);
    }

    private ContainerTag toCVSS(String cvss, Function<String, Pair<String, String>> extractTextClassFct, Map<String, String> cvssMap) {
        String idContent = "cvss-" + ai.incrementAndGet();
        String[] splitted = cvss.split("/");
        Pair<String, String> textClass = extractTextClassFct.apply(splitted[0]);
        ContainerTag ret = p(a(textClass.getOne())
                .withHref("#" + idContent)
                .withClasses("btn", textClass.getTwo())
                .attr("data-toggle", "collapse")
                .attr("role", "button")
                .attr("aria-expanded", "false")
                .attr("aria-controls", idContent));
        ContainerTag cvssContent = div();
        for (int i = 1; i < splitted.length; i++) {
            String content = cvssMap.getOrDefault(splitted[i].toUpperCase(), splitted[i]);
            cvssContent.with(text(content), br());
        }

        ret = td(ret,
                div(
                        cvssContent.withClass("card card-body")
                ).withClass("collapse").withId(idContent)
        );
        return ret;
    }

    private ContainerTag cvss(Node e) {
        String cvss3 = attr(e, "cvss3");
        String cvss2 = attr(e, "cvss2");
        ContainerTag a;
        if (!"".equals(cvss3)) {
            a = toCVSS(cvss3, this::extractCVSS3TextClass, CVSS3);
        } else if (!"".equals(cvss2)) {
            a = toCVSS(cvss2, this::extractCVSS2TextClass, CVSS2);
        } else {
            a = td();
        }
        return a;
    }

    private ContainerTag link(Node e) {
        String link = attr(e, "href");
        ContainerTag a;
        if (!"".equals(link)) {
            a = a(link)
                    .withHref(link)
                    .withTarget("redhat-" + e.getTextContent());
            a = td(a);
        } else {
            a = td();
        }
        return a;
    }


    private ContainerTag cwe(Node e) {
        String cwe = attr(e, "cwe");
        ContainerTag a;
        if (!"".equals(cwe)) {
            a = a(cwe).withTarget(cwe);
            if (cwe.startsWith("CWE-")) {
                a.withHref("https://cwe.mitre.org/data/definitions/" + cwe.substring("CWE-".length()) + ".html");
            }
            a = td(a);
        } else {
            a = td();
        }
        return a;
    }

    private DomContent detailed(Pair<DefinitionType, IEval> p) {
        MetadataType meta = p.getOne().getMetadata();
        return tag(null).with(
                h2(meta.getTitle()).withId(p.getOne().getId()),
                h3(resourceBundle.getString("detailed.description")),
                div(formattedText(meta.getDescription())).withClass("content-3"),
                h3(resourceBundle.getString("detailed.references")),
                references(p),
                h3(resourceBundle.getString("detailed.evidence")),
                div(debug(p.getTwo())).withClass("content-3"),
                p()
        );
    }

    private DomContent debug(IEval eval) {
        J2HtmlPresenter presenter = new J2HtmlPresenter();
        presenter.indent();
        try {
            eval.print(presenter);
        } catch (IOException skipped) { // Impossible dans cette conf
        }
        return presenter.toDomContent();
    }

    private DomContent formattedText(String text) {
        boolean startWithStart = false;
        List<DomContent> paragraph = new ArrayList<>();
        for (String s : text.split("\n")) {
            if (!(startWithStart && s.length() == 0)) {
                paragraph.add(text(s));
                paragraph.add(br());
            }
            startWithStart = s.startsWith("*") || s.startsWith("Security Fix(es):");
        }
        return tag(null).with(paragraph.toArray(new DomContent[0]));
    }

}
