package idlreasonerchoco.analyzer.operations.oas;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import idlreasonerchoco.configuration.ErrorType;
import idlreasonerchoco.configuration.IDLException;
import idlreasonerchoco.mapper.OASMapper;
import idlreasonerchoco.model.ParameterType;
import idlreasonerchoco.utils.ExceptionManager;
import idlreasonerchoco.utils.Utils;
import io.swagger.v3.oas.models.parameters.Parameter;

public class OASRandomRequest implements RequestGenerationOperation {

    private static final Logger LOG = Logger.getLogger(OASRandomRequest.class);

    private final OASMapper mapper;
    private final boolean valid;

    public OASRandomRequest(OASMapper mapper, boolean valid) {
        this.mapper = mapper;
        this.valid = valid;
    }

    public Map<String, String> generate() throws IDLException {
        mapper.getChocoModel().getSolver().reset();
        if (valid) {
            return mapRequest();
        } else {
            Stream.of(mapper.getChocoModel().getCstrs()).forEach(x->{
                mapper.getChocoModel().unpost(x);
                x.getOpposite().post();
            });

            Map<String, String> request = mapRequest();

            Stream.of(mapper.getChocoModel().getCstrs()).forEach(x->{
                mapper.getChocoModel().unpost(x);
                x.getOpposite().post();
            });

            return request;
        }
    }

    private Map<String, String> mapRequest() throws IDLException {
        Map<String, String> request = null;
        OASConsistent consistent = new OASConsistent(mapper);
        if (consistent.analyze()) {
            request = new HashMap<>();
            for (Parameter parameter : mapper.getParameters()) {
                BoolVar varSet = mapper.getVariablesMap().get(Utils.parseIDLParamName(parameter.getName()) + "Set").asBoolVar();
                if (varSet.getValue() == 1) {
                    IntVar paramVar = mapper.getVariablesMap().get(Utils.parseIDLParamName(parameter.getName())).asIntVar();
                    request.put(parameter.getName(), mapConstraintToValue(paramVar.getValue(), parameter.getSchema().getType()));
                }
            }
        }
        return request;
    }

    private String mapConstraintToValue(Integer intValue, String type) throws IDLException {
        switch (ParameterType.valueOf(type.toUpperCase())) {
            case STRING:
            case ARRAY:
                return mapper.getStringToIntMap().inverse().get(intValue % mapper.getStringToIntMap().size());
            case NUMBER:
            case INTEGER:
                return intValue.toString();
            case BOOLEAN:
                return Boolean.toString(intValue == 1);
            default:
                ExceptionManager.rethrow(LOG, ErrorType.ERROR_IN_PARAMETER_TYPE.toString() + " :" + type);
                return null;
        }
    }
}
