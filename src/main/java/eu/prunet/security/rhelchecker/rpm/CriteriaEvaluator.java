package eu.prunet.security.rhelchecker.rpm;


import eu.prunet.schema.oval.common.OperationEnumeration;
import eu.prunet.schema.oval.common.OperatorEnumeration;
import eu.prunet.schema.oval.def.CriterionType;
import eu.prunet.schema.oval.def.EntityStateSimpleBaseType;
import eu.prunet.schema.oval.linux.RpminfoState;
import eu.prunet.schema.oval.linux.RpminfoTest;
import eu.prunet.security.rhelchecker.eval.*;
import eu.prunet.schema.oval.def.CriteriaType;
import eu.prunet.schema.oval.def.ExtendDefinitionType;
import jregex.Matcher;
import jregex.Pattern;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.*;

public class CriteriaEvaluator {

    private final Map<String, Rpm> rpms;
    private final Map<String, String> objects;
    private final Map<String, RpminfoState> states;
    private final Map<String , RpminfoTest> tests;
    private final Map<String, IEval> stateCache = new HashMap<>();

    public CriteriaEvaluator(Map<String, Rpm> rpms, Map<String, String> objects, Map<String, RpminfoState> states, Map<String , RpminfoTest> tests) {
        this.rpms = rpms;
        this.objects = objects;
        this.states = states;
        this.tests = tests;
    }

    public IEval evaluate(CriteriaType criteriaType) {
        if (Boolean.FALSE.equals(criteriaType.isApplicabilityCheck())) {
            return new NAEval();
        }
        IEval ret;
//        System.out.println(criteriaType.getOperator());
        ret = LogicOperation.evaluate(criteriaType.getOperator(), criteriaType.isNegate(),
                criteriaType.getCriteriaOrCriterionOrExtendDefinition().stream(),
                c->{
                    if (c instanceof CriteriaType) {
                        return evaluate((CriteriaType)c);
                    } else if (c instanceof CriterionType) {
                        return evaluate((CriterionType)c);
                    } else {
                        return evaluate((ExtendDefinitionType)c);
                    }
                });
        return ret;
    }

    private IEval evaluate(final Rpm rpm, final RpminfoState state) {
        return stateCache.computeIfAbsent(rpm.getPackageName() + ":"+state.getId(),
                cState-> LogicOperation.evaluate(state.getOperator(), false,
                        state.getRest().stream(),
                        c->evaluate(rpm, c)));
    }

    private IEval evaluate(final Rpm rpm, JAXBElement<? extends EntityStateSimpleBaseType> rpmCond) {
        QName name = rpmCond.getName();
        EndEval ret = new EndEval();
        if ("http://oval.mitre.org/XMLSchema/oval-definitions-5#linux".equals(name.getNamespaceURI())) {
            String rpmCondValue = (String)rpmCond.getValue().getValue();
            ret.setEvalType(name.getLocalPart());
            boolean bRet;
            ret.setPackageName(rpm.getPackageName());
            switch (name.getLocalPart()) {
                case "evr":
                    if (rpmCond.getValue().getOperation() != OperationEnumeration.LESS_THAN) {
                        throw new UnsupportedOperationException("Unsupported Operation file a bug : " + rpmCond.getValue().getOperation());
                    }
                    ret.setEvalType("EVR <");
                    bRet = RpmUtils.compare(rpm.getEpoch(),rpm.getVersion(),rpm.getRelease(), rpmCondValue) == -1;
                    ret.setOperationResult(bRet);
                    ret.debug("%s %d:%s-%s < %s", rpm.getPackageName(), rpm.getEpoch(), rpm.getVersion(), rpm.getRelease(), rpmCondValue);
                    break;
                case "arch":
                    switch (rpmCond.getValue().getOperation()) {
                        case PATTERN_MATCH:
                            ret.setEvalType("ARCH ~=");
                            Pattern p=new Pattern(rpmCondValue); //a word pattern
                            Matcher m=p.matcher(rpm.getArch());
                            ret.setOperationResult(m.find());
                            ret.debug("%s %s ~= %s", rpm.getPackageName(), rpm.getArch(), rpmCondValue);
                            break;
                        case EQUALS:
                            ret.setEvalType("ARCH =");
                            ret.setOperationResult(Objects.equals(rpm.getArch(), rpmCondValue));
                            ret.debug("%s %s ~= %s", rpm.getPackageName(), rpm.getArch(), rpmCondValue);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported Operation file a bug : " + rpmCond.getValue().getOperation());
                    }
                    break;
                case "version":
                    if (rpmCond.getValue().getOperation() != OperationEnumeration.PATTERN_MATCH) {
                        throw new UnsupportedOperationException("Unsupported Operation file a bug : " + rpmCond.getValue().getOperation());
                    }
                    ret.setEvalType("VERSION ~=");
                    ret.debug("%s %s ~= %s", rpm.getPackageName(), rpm.getVersion(), rpmCondValue);
                    Pattern p=new Pattern(rpmCondValue); //a word pattern
                    Matcher m=p.matcher(rpm.getVersion());
                    ret.setOperationResult(m.find());
                    break;
                case "signature_keyid":
                    if (rpmCond.getValue().getOperation() != OperationEnumeration.EQUALS) {
                        throw new UnsupportedOperationException("Unsupported Operation file a bug : " + rpmCond.getValue().getOperation());
                    }
                    ret.setEvalType("SIG =");
                    ret.debug("%s %s = %s", rpm.getPackageName(), rpm.getSigid(), rpmCondValue);
                    ret.setOperationResult(Objects.equals(rpm.getSigid(), rpmCondValue));
                    break;
                 default:
                    throw new UnsupportedOperationException("Unsupported Type file a bug : " + rpmCond.getName());

            }
        } else {
            throw new UnsupportedOperationException("Unsupported Type file a bug  : " + rpmCond.getName());
        }
        return ret;
    }

    private IEval evaluate(CriterionType criterionType) {
        if (Boolean.FALSE.equals(criterionType.isApplicabilityCheck())) {
            return new NAEval();
        }
        RpminfoTest test = tests.get(criterionType.getTestRef());
//        System.out.println(objects.get(test.getObject().getObjectRef()));
        String packageName = Optional.ofNullable(objects.get(test.getObject().getObjectRef()))
                .orElseThrow(() -> new IllegalArgumentException("PackageName cannot be found for " +test.getObject().getObjectRef()));
        Rpm rpm = rpms.get(packageName);
        if (rpm == null) { // Not applicable
            return new RPMNotFound(packageName);
        }
        return LogicOperation.evaluate(OperatorEnumeration.AND, criterionType.isNegate(), test.getState().stream(),
                s->evaluate(rpm, states.get(s.getStateRef())));
    }

    private IEval evaluate(ExtendDefinitionType extendDefinitionType) {
        return new NAEval();
    }


}
