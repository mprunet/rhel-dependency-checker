package eu.prunet.security.rhelchecker;

import eu.prunet.cmdline.*;
import eu.prunet.schema.oval.def.DefinitionType;
import eu.prunet.schema.oval.def.OvalDefinitions;
import eu.prunet.schema.oval.linux.RpminfoObject;
import eu.prunet.schema.oval.linux.RpminfoState;
import eu.prunet.schema.oval.linux.RpminfoTest;
import eu.prunet.security.rhelchecker.eval.IEval;
import eu.prunet.security.rhelchecker.report.HtmlGenerator;
import eu.prunet.security.rhelchecker.rpm.CriteriaEvaluator;
import eu.prunet.security.rhelchecker.rpm.Rpm;
import eu.prunet.util.Pair;
import eu.prunet.util.SimpleDownloader;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;


@SuppressWarnings("WeakerAccess")
public class Main {

    public static Pair<OvalDefinitions, CriteriaEvaluator> parse(InputStream ovalIS, Map<String, Rpm> rpms) throws JAXBException {
        OvalDefinitions od;
        JAXBContext jc = JAXBContext.newInstance(OvalDefinitions.class);
        Unmarshaller um = jc.createUnmarshaller();
        od = (OvalDefinitions) um.unmarshal(ovalIS);
        Map<String, String> objects = od.getObjects().getObject().stream().map(JAXBElement::getValue)
                .map(o -> ((RpminfoObject) o))
                .collect(Collectors.toMap(RpminfoObject::getId, info -> (String) info.getName().getValue()));
        Map<String, RpminfoState> states = od.getStates().getState().stream().map(JAXBElement::getValue)
                .map(o -> ((RpminfoState) o))
                .collect(Collectors.toMap(RpminfoState::getId, Function.identity()));
        Map<String, RpminfoTest> tests = od.getTests().getTest().stream().map(JAXBElement::getValue)
                .map(o -> ((RpminfoTest) o))
                .collect(Collectors.toMap(RpminfoTest::getId, Function.identity()));

        return Pair.of(od, new CriteriaEvaluator(rpms, objects, states, tests));
    }

    public static String extractVersion(Map<String, Rpm> rpms) throws BugException {
        try {
            try (InputStream is = Main.class.getResourceAsStream("/version-check.xml")) {
                Pair<OvalDefinitions, CriteriaEvaluator> pair = parse(is, rpms);
                for (DefinitionType dt : pair.getOne().getDefinitions().getDefinition()) {
                    IEval eval = pair.getTwo().evaluate(dt.getCriteria());
                    if (eval.isTrue()) {
                        System.err.println(dt.getMetadata().getTitle() + " detected");
                        return dt.getId();
                    }
                }
                return null;
            }
        } catch (IOException | JAXBException ex) {
            throw new BugException(ex);
        }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        ExitCode retCode;
        try {
            execute(args);
            retCode = ExitCode.OK;
        } catch (BadArgumentException ex) {
            System.err.println(ex.getMessage());
            System.err.println("rhel-dependecy-checker --help");
            retCode = ExitCode.ARGUMENT_ERROR;
        } catch (UsageException e) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/usage.txt")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.err.println(line);
                }
            } catch (IOException e1) {
                System.err.println("The usage file cannot be read, see README.md");
            }
            retCode = ExitCode.ARGUMENT_ERROR;
        } catch (BadRpmInputException e) {
            System.err.println("The file " + e.getFileName() + " has the wrong format, make sure it is formatted with the command :");
            System.err.println("rpm --nosignature --nodigest -qa --qf '%{N} %{epochnum} %{V} %{R} %{arch} <%{SIGPGP:pgpsig}>\\n'");
            System.err.println();
            System.err.println("The expected format is : %{N} %{epochnum} %{V} %{R} %{arch} <%{SIGPGP:pgpsig}>");
            e.getErrors().stream().limit(10)
                    .forEach(System.err::println);
            retCode = ExitCode.BAD_RPM_INPUT_FILE_FORMAT;
        } catch (BugException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            retCode = ExitCode.BUG;
        } catch (VersionException e) {
            System.err.println("The version of the Redhat cannot be determined, make sure that the RPM_INPUT_FILE is generated with the command :");
            System.err.println("rpm --nosignature --nodigest -qa --qf '%{N} %{epochnum} %{V} %{R} %{arch} <%{SIGPGP:pgpsig}>\\n'");
            retCode = ExitCode.RHEL_NOT_SUPPORTED;
        } catch (DownloadManuallyOvalException e) {
            Path ovalFile = e.getOvalFile();
            System.err.println("The file " + ovalFile.getFileName() + " must be downloaded manually (offline mode) with one of the following command");
            System.err.println("wget -N \"https://www.redhat.com/security/data/oval/" + ovalFile.getFileName() + "\" -O " + ovalFile.toString());
            System.err.println("OR");
            System.err.println("curl \"https://www.redhat.com/security/data/oval/" + ovalFile.getFileName() + "\" -O " + ovalFile.toString());
            retCode = ExitCode.DOWNLOAD_OVAL_MANUALLY;
        }catch (TranslatedException e) {
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace(System.err);
            }
            retCode = e.getExitCode();
        }
        System.out.flush();
        System.err.flush();
        System.exit(retCode.ordinal());

    }

    public static void execute(String[] args) throws BadArgumentException, UsageException, BadRpmInputException, BugException, VersionException, DownloadManuallyOvalException, TranslatedException {
        ArgParser parser = new ArgParser();
        Arg rhelVersion = parser.optionalWithValue("rhel-version", 'r', "RHEL_VERSION");
        Arg offline = parser.optional("offline", 'n');
        Arg force = parser.optional("force", null);
        Arg ovalDir = parser.optionalWithDefaultValue("oval-dir", 'd', "OVAL_DIR", ".");
        Arg format = parser.optionalWithDefaultValue(null, 'f', "FORMAT", "html");
        Arg consoleMode = parser.optional(null, 'O');
        Arg rpmInputFile = parser.arg("RPM_INPUT_FILE", true);
        Arg outputReportFile = parser.arg("OUTPUT_REPORT_FILE", false);
        Arg proxyParam = parser.optionalWithValue("proxy", 'p', "PROTOCOL:PROXY:PORT");
        Arg noSystemProxy = parser.optional("no-system-proxy", 'P');
        Arg keepExisting = parser.optional("keep-if-download-failed", 'k');
        Arg lang = parser.optionalWithDefaultValue("lang", 'l', "LANG", "en");
        Arg refresh = parser.optionalWithDefaultValue("refresh", 'r', "REFRESH_DELAY_IN_DAYS", "1");
        Arg argVersion = parser.optional("version", 'V');

        String ovalFileName = "com.redhat.rhsa-RHEL%s.xml";
        Set<Arg> arguments = parser.parse(args);
        if (arguments.contains(argVersion)) {

            ResourceBundle rb = ResourceBundle.getBundle("i18n.translation", Locale.US);

            System.out.println(rb.getString("product.name")+ " " + rb.getString("product.version"));
            return;
        }
        checkUsage(arguments);

        Proxy proxy = checkAndGetProxy(proxyParam, noSystemProxy, arguments);
        checkLang(lang);

        boolean console = isConsole(consoleMode, outputReportFile, arguments);

        String version = checkAndGetVersion(rhelVersion, arguments);

        checkFormat(format);

        int refreshV = checkAndGetRefresh(refresh);


        Path pathRpmInputFile = checkRpmInputFile(rpmInputFile);
        Path outputFile = checkAndGetOutputFile(force, outputReportFile, arguments, console);

        Path pOvalDir = checkOvalDir(ovalDir);


        Map<String, Rpm> rpms = readRpm(rpmInputFile);

        if (version == null) {
            version = extractVersion(rpms);
            if (version == null) {
                throw new VersionException();
            }
        }

        Path ovalFile = pOvalDir.resolve(String.format(ovalFileName, version));


        checkAndDownloadOval(offline, keepExisting, arguments, proxy, refreshV, pOvalDir, ovalFile);

        Pair<OvalDefinitions, CriteriaEvaluator> pair = null;
        try (InputStream is = Files.newInputStream(ovalFile)) {
            pair = parse(is, rpms);
        } catch (JAXBException ex) {
            throw new TranslatedException(ExitCode.BAD_OVAL_FILE_FORMAT, "The file " + ovalFile + " has not a valid format.", ex);
        } catch (IOException e) {
            throw new TranslatedException(ExitCode.BAD_OVAL_FILE_FORMAT, "The oval definition file " + ovalFile + " cannot be read", e);
        }

        try {
            List<Pair<DefinitionType, IEval>> issues = new ArrayList<>();
            for (DefinitionType dt : pair.getOne().getDefinitions().getDefinition()) {
                IEval eval = pair.getTwo().evaluate(dt.getCriteria());
                if (eval.isTrue()) {
                    issues.add(Pair.of(dt, eval));
                }
            }
            ResourceBundle resourceBundle = ResourceBundle.getBundle("i18n.translation", "en".equals(lang.getValue()) ? Locale.US : Locale.FRANCE);

            try (MyAppendable bw = new MyAppendable(console ? System.out : Files.newBufferedWriter(outputFile))) {
                if ("html".equalsIgnoreCase(format.getValue())) {
                    new HtmlGenerator(resourceBundle).generate(bw, issues);
                } else if ("text".equalsIgnoreCase(format.getValue())) {
                    for (Pair<DefinitionType, IEval> issue : issues) {
                        bw.append(issue.getOne().getId() + " : " + issue.getOne().getMetadata().getTitle() + System.lineSeparator());
                        for (Node n : (Iterable<Node>) () -> HtmlGenerator.getAdvisoryChildStream(issue.getOne().getMetadata(), "cve").iterator()) {
                            bw.append("  - " + n.getTextContent() + System.lineSeparator());
                        }
                    }
                } else if ("cve".equalsIgnoreCase(format.getValue())) {
                    Set<String> cves = new TreeSet<>();
                    for (Pair<DefinitionType, IEval> issue : issues) {
                        for (Node n : (Iterable<Node>) () -> HtmlGenerator.getAdvisoryChildStream(issue.getOne().getMetadata(), "cve").iterator()) {
                            cves.add(n.getTextContent());
                        }
                    }
                    for (String cve : cves) {
                        bw.append(cve + System.lineSeparator());
                    }
                }
                System.err.println("Report generated successfully");
            } catch (IOException ex) {
                throw new TranslatedException(ExitCode.REPORT_GENERATION_IO_ERROR, "An error occured during writing the report", ex);
            }
        } catch (Exception ex) {
            throw new TranslatedException(ExitCode.REPORT_GENERATION_ISSUE, "An error occured during report generation, please file a bug with " + ovalFile + " and " + pathRpmInputFile, ex);
        }
    }

    private static void checkAndDownloadOval(Arg offline, Arg keepExisting, Set<Arg> arguments, Proxy proxy, int refreshV, Path pOvalDir, Path ovalFile) throws DownloadManuallyOvalException, TranslatedException, BadArgumentException {
        Instant refreshIfBefore = Instant.now().minus(Duration.ofDays(refreshV));
        boolean toDownload = false;
        if (!Files.exists(ovalFile) && arguments.contains(offline)) {
            throw new DownloadManuallyOvalException(ovalFile);
        }
        try {
            toDownload = !Files.exists(ovalFile) || (Files.exists(ovalFile)
                    && !arguments.contains(offline)
                    && Files.getLastModifiedTime(ovalFile).toInstant().isBefore(refreshIfBefore));
        } catch (IOException e) {
            throw new TranslatedException(ExitCode.IO_ERROR, "The last modification time of " + ovalFile + " cannot be read (IOError)", e);
        }

        if (Files.exists(ovalFile)) {
            if (!Files.isRegularFile(ovalFile)) {
                throw new BadArgumentException("The file " + ovalFile + " must be regular file.");
            }
            if (toDownload && !Files.isWritable(pOvalDir)) {
                throw new BadArgumentException("The file to " + ovalFile + " must be writable.");
            }
        }

        if (toDownload) {
            System.err.println("Download CVE definition https://www.redhat.com/security/data/oval/" + ovalFile.getFileName() + " ...");
            try {
                SimpleDownloader.downloadWithRetry(ovalFile, "https://www.redhat.com/security/data/oval/" + ovalFile.getFileName(), 3, 10, proxy);
            } catch (DownloadException ex) {
                System.err.println();
                System.err.println(ex.getMessage());
                System.err.println("Try to change your proxy settings (--proxy=http://proxyhost:proxyport). Note if your are behind an NTLM proxy use a proxy like cntlm");
                if (arguments.contains(keepExisting)) {
                    System.err.println();
                    System.err.println("Continue (" + keepExisting.getFullName() + " option)");
                } else {
                    throw new DownloadManuallyOvalException(ovalFile);
                }
            }
        } else {
            System.err.println(String.format("%s is a recent file (less than %d day), download skipped", ovalFile.toString(), refreshV));
        }
    }

    private static String checkAndGetVersion(Arg rhelVersion, Set<Arg> arguments) throws BadArgumentException {
        String version = null;
        if (arguments.contains(rhelVersion)) {
            version = rhelVersion.getValue();
            if (!("5".equals(version) || "6".equals(version) || "7".equals(version))) {
                throw new BadArgumentException(version + " is not a valid value for " + rhelVersion.getValueName());
            }
        }
        return version;
    }

    private static Map<String, Rpm> readRpm(Arg rpmInputFile) throws TranslatedException, BadRpmInputException {
        List<String> errors = new ArrayList<>();
        Map<String, Rpm> rpms = new HashMap<>();
        AtomicInteger lineNumber = new AtomicInteger(1);
        try {
            Files.lines(Paths.get(rpmInputFile.getValue()))
                    .filter(l -> !"".equals(l.trim())) // Filtering empty lines
                    .forEach(l -> {
                        try {
                            Rpm rpm = new Rpm(l);
                            rpms.put(rpm.getPackageName(), rpm);
                        } catch (Exception ex) {
                            errors.add("Line " + lineNumber.get() + " " + l);
                        }
                        lineNumber.incrementAndGet();
                    });
        } catch (IOException e) {
            throw new TranslatedException(ExitCode.IO_ERROR, "Rpm file list cannot be read " + rpmInputFile.getValue(), e);
        }
        if (rpms.size() == 0) {
            throw new BadRpmInputException(rpmInputFile.getValue(), errors);
        } else if (errors.size() > 0) {
            System.err.println("Some entry in the file " + rpmInputFile.getValue() + " has been skipped, make sure it is formatted with the command :");
            System.err.println("rpm --nosignature --nodigest -qa --qf '%{N} %{epochnum} %{V} %{R} %{arch} <%{SIGPGP:pgpsig}>\\n'");
            System.err.println("The following lines are poorly formatted, The expected format is : %{N} %{epochnum} %{V} %{R} %{arch} <%{SIGPGP:pgpsig}>");
            if (errors.size() > 0) {
                errors.stream().limit(10)
                        .forEach(System.err::println);
            }
            System.err.println();
        }
        return rpms;
    }

    private static Path checkAndGetOutputFile(Arg force, Arg outputReportFile, Set<Arg> arguments, boolean console) throws BadArgumentException {
        Path outputFile = null;
        if (!console) {
            outputFile = Paths.get(outputReportFile.getValue());
            if (Files.exists(outputFile)) {
                if (!arguments.contains(force)) {
                    throw new BadArgumentException("The file " + outputReportFile.getValue() + " already exists");
                } else if (!Files.isWritable(outputFile)) {
                    throw new BadArgumentException("The file " + outputReportFile.getValue() + " must be writable.");
                }
            } else {
                Path parentDir = outputFile.toAbsolutePath().getParent();
                if (!Files.exists(parentDir)) {
                    throw new BadArgumentException("The parent directory for the file " + outputFile + " does not exists");
                } else if (!Files.isWritable(parentDir) || !Files.isDirectory(parentDir)) {
                    throw new BadArgumentException("The parent directory for the file " + outputFile + " must be writable.");
                }
            }
        }
        return outputFile;
    }

    private static boolean isConsole(Arg consoleMode, Arg outputReportFile, Set<Arg> arguments) throws BadArgumentException {
        boolean console = arguments.contains(consoleMode);
        if (console && arguments.contains(outputReportFile)) {
            throw new BadArgumentException("Arguments -O and OUTPUT_REPORT_FILE are mutually exclusive");
        } else if (!console && !arguments.contains(outputReportFile)) {
            throw new BadArgumentException("Arguments OUTPUT_REPORT_FILE is mandatory");
        }
        return console;
    }

    private static void checkUsage(Set<Arg> arguments) throws UsageException {
        if (arguments.size() == 0) {
            throw new UsageException();
        }
    }

    private static void checkLang(Arg lang) throws BadArgumentException {
        if (!"en".equalsIgnoreCase(lang.getValue()) && !"fr".equalsIgnoreCase(lang.getValue())) {
            throw new BadArgumentException("The LANG value must be en or fr");
        }
    }

    private static Proxy checkAndGetProxy(Arg proxyParam, Arg noSystemProxy, Set<Arg> arguments) throws BadArgumentException {
        Proxy proxy = null;
        if (!arguments.contains(proxyParam)) {
            if (!arguments.contains(noSystemProxy)) {
                System.setProperty("java.net.useSystemProxies", "true");
            } else {
                System.setProperty("java.net.useSystemProxies", "false");
            }
        } else {
            System.setProperty("java.net.useSystemProxies", "false");
            String proxyVal = proxyParam.getValue();
            boolean proxyOk = false;
            if (proxyVal.startsWith("http://") || proxyVal.startsWith("sock://")) {
                String hostName;
                Integer port;
                int idxPort = proxyVal.indexOf(':', 7);
                if (idxPort != -1) {
                    try {
                        hostName = proxyVal.substring(7, idxPort);
                        port = Integer.valueOf(proxyVal.substring(idxPort + 1));
                        if (port > 0 && port < 65567) {
                            SocketAddress sa = new InetSocketAddress(hostName, port);
                            proxy = new Proxy(proxyVal.startsWith("http://") ? Proxy.Type.HTTP : Proxy.Type.SOCKS, sa);
                            proxyOk = true;
                        }
                    } catch (NumberFormatException ignore) {
                        throw new BadArgumentException("Proxy port not valid");
                    }
                }
            } else if ("direct".equalsIgnoreCase(proxyVal)) {
                proxy = Proxy.NO_PROXY;
                proxyOk = true;
            }
            if (!proxyOk) {
                throw new BadArgumentException("The proxy value must have the following format : \n " +
                        "-  http://proxyhostname:proxyport\")\n" +
                        "-  sock://proxyhostname:proxyport\n" +
                        "-  direct");
            }
        }
        return proxy;
    }

    private static Path checkRpmInputFile(Arg rpmInputFile) throws BadArgumentException {
        Path pathRpmInputFile = Paths.get(rpmInputFile.getValue());
        if (!Files.exists(pathRpmInputFile)) {
            throw new BadArgumentException("The file " + rpmInputFile.getValue() + " does not exists");
        } else if (!Files.isReadable(pathRpmInputFile) || !Files.isRegularFile(pathRpmInputFile)) {
            throw new BadArgumentException("The file " + rpmInputFile.getValue() + " must be readable.");
        }
        return pathRpmInputFile;
    }

    private static int checkAndGetRefresh(Arg refresh) throws BadArgumentException {
        int refreshV;
        try {
            refreshV = Integer.parseInt(refresh.getValue());
        } catch (NumberFormatException ex) {
            throw new BadArgumentException(refresh.getValue() + " is not a valid value for " + refresh.getValueName() + " must be an integer");
        }
        return refreshV;
    }

    private static void checkFormat(Arg format) throws BadArgumentException {
        if (!"html".equalsIgnoreCase(format.getValue()) && !"text".equalsIgnoreCase(format.getValue()) && !"cve".equalsIgnoreCase(format.getValue())) {
            throw new BadArgumentException(format.getValue() + " is not a valid value for " + format.getValueName());
        }
    }

    private static Path checkOvalDir(Arg ovalDir) throws BadArgumentException, TranslatedException {
        Path pOvalDir = Paths.get(ovalDir.getValue());
        if (!Files.exists(pOvalDir)) {
            throw new BadArgumentException("The directory " + pOvalDir + " does not exists");
        } else if (!Files.isDirectory(pOvalDir)) {
            throw new BadArgumentException("The path to store oval files " + pOvalDir + " must be a directory.");
        } else if (!Files.isReadable(pOvalDir)) {
            throw new BadArgumentException("The directory to store oval files " + pOvalDir + " must be readable.");
        } else if (!Files.isWritable(pOvalDir)) {
            throw new BadArgumentException("The directory to store oval files " + pOvalDir + " must be writable.");
        }
        try {
            pOvalDir = pOvalDir.toRealPath().toAbsolutePath();
        } catch (IOException e) {
            throw new TranslatedException(ExitCode.IO_ERROR, "An IO error occured during accessing to " + pOvalDir, e);
        }
        return pOvalDir;
    }

    private static class MyAppendable implements Closeable, Appendable {
        public MyAppendable(Appendable delegate) {
            this.delegate = delegate;
        }

        @Override
        public Appendable append(CharSequence csq) throws IOException {
            return delegate.append(csq);
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) throws IOException {
            return delegate.append(csq, start, end);
        }

        @Override
        public Appendable append(char c) throws IOException {
            return delegate.append(c);
        }

        private Appendable delegate;

        @Override
        public void close() throws IOException {
            if (delegate instanceof Closeable) {
                ((Closeable) delegate).close();
            }
        }
    }

}
