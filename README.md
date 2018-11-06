# rhel-dependency-checker

rhel-dependency-checker is an offline Redhat RPM Vulnerability scanner based on OVAL definition. 

This software is usefull when you cannot use [yum-plugin-security](https://access.redhat.com/solutions/10021).

rhel-dependency-checker can be launched on a different system. It can be either used in offline or online mode.

rhel-dependency-checker needs two file to operate :
- The list of rpm installed on the analyzed system (rpmlist.txt). This file must be generated with the command : 
```bash
rpm --nosignature --nodigest -qa --qf '%{N} %{epochnum} %{V} %{R} %{arch} <%{SIGPGP:pgpsig}>\n'  > rpmlist.txt
```
- The OVAL vulnerability file provided by redhat : 
   - In offline mode you must download the OVAL definition file with one of theses command (depending of the RHEL version):
      ```
      curl "https://www.redhat.com/security/data/oval/com.redhat.rhsa-RHEL5.xml" -O
      curl "https://www.redhat.com/security/data/oval/com.redhat.rhsa-RHEL6.xml" -O
      curl "https://www.redhat.com/security/data/oval/com.redhat.rhsa-RHEL7.xml" -O
      ```
   - In online mode, rhel-dependency-checker will download the file for you.  

rhel-dependency-checker generate three reports's format : 
   - [HTML report](http://htmlpreview.github.io/?https://github.com/mprunet/rhel-dependency-checker/blob/master/sample/report.html)
   - [CVE bulk list](sample/cve.txt)
   - [Text report](sample/text.txt)
   
## Installation
Precompiled package can be retreive in the [release section](https://github.com/mprunet/rhel-dependency-checker/releases).

## Requirements
    Java 8 or greater must be installed on your system

## Sample

- Generate htlm report in online mode :
     ```
     rhel-dependency-checker [OPTIONS] rpmlist.txt report.html
     ```
- Generate htlm report in online mode behind an http proxy :
     ```
     rhel-dependency-checker [OPTIONS] -p http://my.proxy.hostname.com:8080 rpmlist.txt report.html
     ```
- Generate htlm report in online mode, do not download new definition file if younger than 10 days :
     ```
     rhel-dependency-checker [OPTIONS] -r 10 rpmlist.txt report.html
     ```
- Generate htlm report in offline mode :
     ```
     rhel-dependency-checker [OPTIONS] -n rpmlist.txt report.html
     ```
- Generate htlm french (titles only not description) :
     ```
     rhel-dependency-checker [OPTIONS] -l fr rpmlist.txt report_fr.html
     ```
- Generate cve report :
     ```
     rhel-dependency-checker [OPTIONS] -f cve rpmlist.txt cve.txt
     ```
- Generate cve report on stdout:
     ```
     rhel-dependency-checker [OPTIONS] -O -f cve rpmlist.txt
     ```

## Compilation
### Prerequisite
   Java and maven must be on your system.
   
   Download the source code, then type : 
   ```
   maven package
   ```
   
   The generated file will be located in target\rhel-dependency-checker-0.1.jar

## Usage

### Linux 
    rhel-dependency-checker [OPTIONS] RPM_INPUT_FILE OUTPUT_REPORT_FILE
    rhel-dependency-checker [OPTIONS] -O RPM_INPUT_FILE

### Windows
    java -jar rhel-dependency-checker.jar [OPTIONS] RPM_INPUT_FILE OUTPUT_REPORT_FILE
    java -jar rhel-dependency-checker.jar [OPTIONS] -O RPM_INPUT_FILE
    or
    rhel-dependency-checker.exe [OPTIONS] RPM_INPUT_FILE OUTPUT_REPORT_FILE
    rhel-dependency-checker.exe [OPTIONS] -O RPM_INPUT_FILE
    

### MacOSX
    java -jar rhel-dependency-checker.jar [OPTIONS] RPM_INPUT_FILE OUTPUT_REPORT_FILE
    java -jar rhel-dependency-checker.jar [OPTIONS] -O RPM_INPUT_FILE

## Help
```man
NAME
    rhel-dependency-checker - is an offline Redhat RPM Vulnerability scanner based on OVAL definition

SYNOPSIS
    rhel-dependency-checker [OPTIONS] RPM_INPUT_FILE OUTPUT_REPORT_FILE
    rhel-dependency-checker [OPTIONS] -O RPM_INPUT_FILE

DESCRIPTION
    rhel-dependency-checker check for known RPM Vulnerabilities of an offline system.
    The tools can be launched on a different system, the only requirement is having Java 8 installed.

    RPM_INPUT_FILE is a file generated on the audited system with the following command :
      rpm --nosignature --nodigest -qa --qf '%{N} %{epochnum} %{V} %{R} %{arch} <%{SIGPGP:pgpsig}>\n'

    OUTPUT_REPORT_FILE is the filename's report.

OPTIONS
    -r RHEL_VERSION, --rhel-version=RHEL_VERSION
         Do not detect the RHEL version, use the OVAL definition file for the specified version. Valid values are 5, 6 or 7.

    -n , --offline
         In offline mode, the OVAL definition file must be present in the current directory or in the OVAL_DIR directory.
         Depending on the target platform, the file can be downloaded manually with one of the following command :
                wget -N "https://www.redhat.com/security/data/oval/com.redhat.rhsa-RHEL5.xml"
                wget -N "https://www.redhat.com/security/data/oval/com.redhat.rhsa-RHEL6.xml"
                wget -N "https://www.redhat.com/security/data/oval/com.redhat.rhsa-RHEL7.xml"
         Or
                curl "https://www.redhat.com/security/data/oval/com.redhat.rhsa-RHEL5.xml" -O
                curl "https://www.redhat.com/security/data/oval/com.redhat.rhsa-RHEL6.xml" -O
                curl "https://www.redhat.com/security/data/oval/com.redhat.rhsa-RHEL7.xml" -O

    -r DAYS, --refresh=DAYS
         Do not download definition if the local file has been downloaded less than DAYS ago.

    -d OVAL_DIR, --oval-dir=OVAL_DIR
         If specified, the oval definition files will be downloaded (or used) in the specified directory,
         otherwise the current directory will be used

    -f FORMAT
         Specify the output format. HTML is the default.
         Valid values are TEXT, CVE

    -O
         Output the report on stdout

    --keep-if-download-failed, -k
         Use the existing OVAL file if the download failed.

    -p PROTOCOL:PROXY:PORT, --proxy=PROTOCOL:PROXY:PORT
         Set the proxy, sample values :
         - http://127.0.0.1:8080
         - sock://127.0.0.1:3128
         - direct
         If -p and -P are not set, the system proxy is used.

    -P, --no-system-proxy
         Do not use the system proxy
    -l LANG, --lang=LANG
         Specify the language of the report. Default value: en, supported value: fr.

    --force
         Overwrite the output file, if it's already exists

EXIT CODES
    At the time of writing, the exit codes are :
    0      Report generated successfully
    1      Bad argument
    2      The downloaded OVAL file from Redhat(c), has a wrong format.
    3      Error during report generation, file a bug on GitHub
    4      Error during report generation, IO Error
    5      IO Error
    6      RPM input file has the wrong format
    7      BUG, probably a bug, file a bug on GitHub
    8      The RHEL is not recognized or not supported
    9      The OVAL file cannot be downloaded

```


