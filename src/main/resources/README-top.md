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
Precompiled package can be retrieved in [release section](https://github.com/mprunet/rhel-dependency-checker/releases).

## Requirements
    Java 8 or greater must be installed on your system

## Sample

- Generate htlm report in online mode :
     ```
     rhel-dependency-checker rpmlist.txt report.html
     ```
- Generate htlm report in online mode behind an http proxy :
     ```
     rhel-dependency-checker -p http://my.proxy.hostname.com:8080 rpmlist.txt report.html
     ```
- Generate htlm report in online mode, do not download new definition file if younger than 10 days :
     ```
     rhel-dependency-checker -r 10 rpmlist.txt report.html
     ```
- Generate htlm report in offline mode :
     ```
     rhel-dependency-checker -n rpmlist.txt report.html
     ```
- Generate htlm french (titles only not description) :
     ```
     rhel-dependency-checker -l fr rpmlist.txt report_fr.html
     ```
- Generate cve report :
     ```
     rhel-dependency-checker -f cve rpmlist.txt cve.txt
     ```
- Generate cve report on stdout:
     ```
     rhel-dependency-checker -O -f cve rpmlist.txt
     ```

## Compilation
### Prerequisite
   Java and maven must be on your system.
   
   Download the source code, then type : 
   ```
   maven package
   ```
   
   The generated file will be located in target\rhel-dependency-checker-${project.version}.jar

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
