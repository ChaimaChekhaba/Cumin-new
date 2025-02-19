package analyser;

import entities.PaprikaMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;

/**
 * Created by sarra on 20/02/17.
 */
public class MethodProcessor extends ExecutableProcessor<CtMethod> {
    private static final Logger logger = LoggerFactory.getLogger(MethodProcessor.class.getName());

    @Override
    protected void process(CtMethod ctMethod, PaprikaMethod paprikaMethod) {
        paprikaMethod.setSetter(checkSetter(ctMethod));
        paprikaMethod.setGetter(checkGetter(ctMethod));
        for (ModifierKind modifierKind : ctMethod.getModifiers()) {
            if (modifierKind.toString().toLowerCase().equals("static")) {
                paprikaMethod.setStatic(true);
                break;
            }
        }
    }

    private boolean checkGetter(CtMethod element) {
        if (element.getBody() == null) {
            return false;
        }
        if (element.getBody().getStatements().size() != 1) {
            return false;
        }
        CtStatement statement = element.getBody().getStatement(0);
        if (!(statement instanceof CtReturn)) {
            return false;
        }

        CtReturn retur = (CtReturn) statement;
        if (!(retur.getReturnedExpression() instanceof CtFieldRead)) {
            return false;
        }
        CtFieldRead returnedExpression = (CtFieldRead) retur.getReturnedExpression();

        CtType parent = element.getParent(CtType.class);
        if (parent == null) {
            return false;
        }
        try {
            if (parent.equals(returnedExpression.getVariable().getDeclaration().getDeclaringType())) {
                return true;
            }
        } catch (NullPointerException npe) {
            //logger.warn("Could not find declaring type for getter: " + returnedExpression.getVariable().toString() + " (" + npe.getMessage() + ")");
        }
        return false;

    }

    private boolean checkSetter(CtMethod element) {
        if (element.getBody() == null) {
            return false;
        }
        if (element.getBody().getStatements().size() != 1) {
            return false;
        }
        if (element.getParameters().size() != 1) {
            return false;
        }
        CtStatement statement = element.getBody().getStatement(0);
        // the last statement is an assignment
        if (!(statement instanceof CtAssignment)) {
            return false;
        }

        CtAssignment ctAssignment = (CtAssignment) statement;
        if (!(ctAssignment.getAssigned() instanceof CtFieldWrite)) {
            return false;
        }
        if (!(ctAssignment.getAssignment() instanceof CtVariableRead)) {
            return false;
        }
        CtVariableRead ctVariableRead = (CtVariableRead) ctAssignment.getAssignment();
        if (element.getParameters().size() != 1) {
            return false;
        }
        if (!(ctVariableRead.getVariable().getDeclaration().equals(element.getParameters().get(0)))) {
            return false;
        }
        CtFieldWrite returnedExpression = (CtFieldWrite) ((CtAssignment) statement).getAssigned();
        if (returnedExpression.getTarget() instanceof CtThisAccess) {
            return true;
        }

        return false;
    }
}
