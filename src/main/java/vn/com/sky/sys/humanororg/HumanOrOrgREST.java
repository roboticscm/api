package vn.com.sky.sys.humanororg;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;

import java.util.LinkedHashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import vn.com.sky.Message;
import vn.com.sky.base.GenericREST;
import vn.com.sky.security.AuthenticationManager;
import vn.com.sky.security.PBKDF2Encoder;
import vn.com.sky.sys.auth.HumanOrOrgRepo;
import vn.com.sky.sys.model.HumanOrOrg;
import vn.com.sky.sys.model.HumanOrg;
import vn.com.sky.util.CustomRepoUtil;
import vn.com.sky.util.MyServerResponse;
import vn.com.sky.util.SaveOneToOneRelation;
import vn.com.sky.util.StringUtil;

@Configuration
@AllArgsConstructor
public class HumanOrOrgREST extends GenericREST {
    private CustomRepoUtil utilRepo;
    private HumanOrOrgRepo mainRepo;
    private CustomHumanOrOrgRepo customRepo;
    private AuthenticationManager auth;
    private PBKDF2Encoder encode;
    private HumanOrgRepo humanOrgRepo;

    @Bean
    public RouterFunction<?> humanOrOrgRoutes() {
        return route(GET(buildURL("human-or-org", this::sysGetUserListByOrgId)), this::sysGetUserListByOrgId)
            .andRoute(POST(buildURL("human-or-org", this::saveOrUpdate)), this::saveOrUpdate)
            .andRoute(GET(buildURL("human-or-org", this::sysGetUserInfoById)), this::sysGetUserInfoById);
    }

    private Mono<ServerResponse> sysGetUserInfoById(ServerRequest request) {
        // SYSTEM BLOCK CODE
        // PLEASE DO NOT EDIT
        if (request == null) {
            String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
            return Mono.just(new MyServerResponse(methodName));
        }
        // END SYSTEM BLOCK CODE

        Long userId = null;
        try {
            userId = getLongParam(request, "userId");
        } catch (Exception e1) {}
        if (userId == null) userId = auth.getUserId();

        try {
            return customRepo.sysGetUserInfoById(userId).flatMap(item -> ok(item)).onErrorResume(e -> error(e));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Mono<ServerResponse> sysGetUserListByOrgId(ServerRequest request) {
        // SYSTEM BLOCK CODE
        // PLEASE DO NOT EDIT
        if (request == null) {
            String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
            return Mono.just(new MyServerResponse(methodName));
        }
        // END SYSTEM BLOCK CODE
        var orgIdStr = getParam(request, "orgId");

        Long orgId = null;
        Boolean includeDeleted = false, includeDisabled = false;

        try {
            if (orgIdStr != null && !"null".equals(orgIdStr)) orgId = Long.parseLong(orgIdStr);
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest().bodyValue("SYS.MSG.INVILID_ORG_ID");
        }

        try {
            includeDeleted = super.getIncludeDeleted(request);
            includeDisabled = super.getIncludeDisabled(request);
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest().bodyValue(e.getMessage());
        }

        try {
            return customRepo
                .sysGetUserListByOrgId(orgId, includeDeleted, includeDisabled)
                .flatMap(item -> ok(item))
                .onErrorResume(e -> error(e));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Mono<ServerResponse> save(LinkedHashMap<String, String> serverError, HumanOrOrgReq humanReq) {
        return utilRepo
            .isTextValueExisted("human_or_org", "username", humanReq.getUsername())
            .flatMap(
                usernameExisted -> {
                    if (usernameExisted) {
                        serverError.put("username", Message.USERNAME_EXISTED);
                    }
                    return utilRepo
                        .isTextValueExisted("human_or_org", "email", humanReq.getEmail())
                        .flatMap(
                            emailExisted -> {
                                if (emailExisted) {
                                    serverError.put("email", Message.EMAIL_EXISTED);
                                }

                                if (serverError.size() > 0) {
                                    return error(serverError);
                                } else {
                                    // encode and password
                                    var encodedPassword = encode.encode(humanReq.getPassword());
                                    humanReq.setPassword(encodedPassword);
                                    return saveEntity(mainRepo, humanReq, auth)
                                        .flatMap(
                                            saved -> {
                                                return saveManyRelation(
                                                        humanOrgRepo,
                                                        saved.getId(),
                                                        humanReq.getInsertDepartmentIds(),
                                                        new SaveRelation(),
                                                        auth
                                                    )
                                                    .flatMap(item -> ok(item, HumanOrOrg.class))
                                                    .then(
                                                        deleteManyRelation(
                                                                humanOrgRepo,
                                                                saved.getId(),
                                                                humanReq.getRemoveDepartmentIds()
                                                            )
                                                            .flatMap(res -> ok(res, List.class))
                                                    );
                                            }
                                        );
                                }
                            }
                        );
                }
            );
    }

    private Mono<ServerResponse> update(LinkedHashMap<String, String> serverError, HumanOrOrgReq humanReq) {
        return utilRepo
            .isTextValueDuplicated("human_or_org", "username", humanReq.getUsername(), humanReq.getId())
            .flatMap(
                usernameExisted -> {
                    if (usernameExisted) {
                        serverError.put("username", Message.USERNAME_EXISTED);
                    }
                    return utilRepo
                        .isTextValueDuplicated("human_or_org", "email", humanReq.getEmail(), humanReq.getId())
                        .flatMap(
                            emailExisted -> {
                                if (emailExisted) {
                                    serverError.put("email", Message.EMAIL_EXISTED);
                                }

                                if (serverError.size() > 0) {
                                    return error(serverError);
                                } else {
                                    // If password is blank. Do not update password
                                    // Else. encode and password
                                    return mainRepo
                                        .findById(humanReq.getId())
                                        .flatMap(
                                            found -> {
                                                String encodedPassword = found.getPassword();

                                                if (!StringUtil.isBlank(humanReq.getPassword())) {
                                                    encodedPassword = encode.encode(humanReq.getPassword());
                                                }

                                                humanReq.setPassword(encodedPassword);
                                                return updateEntity(mainRepo, humanReq, auth)
                                                    .flatMap(
                                                        updated -> {
                                                            return saveManyRelation(
                                                                    humanOrgRepo,
                                                                    updated.getId(),
                                                                    humanReq.getInsertDepartmentIds(),
                                                                    new SaveRelation(),
                                                                    auth
                                                                )
                                                                .flatMap(item -> ok(item, HumanOrOrg.class))
                                                                .then(
                                                                    deleteManyRelation(
                                                                            humanOrgRepo,
                                                                            updated.getId(),
                                                                            humanReq.getRemoveDepartmentIds()
                                                                        )
                                                                        .flatMap(res -> ok(res, List.class))
                                                                );
                                                        }
                                                    );
                                            }
                                        );
                                }
                            }
                        );
                }
            );
    }

    private Mono<ServerResponse> saveOrUpdate(ServerRequest request) {
        // SYSTEM BLOCK CODE
        // PLEASE DO NOT EDIT
        if (request == null) {
            String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
            return Mono.just(new MyServerResponse(methodName));
        }
        // END SYSTEM BLOCK CODE

        //		return request.body(BodyExtractors.toFormData()).flatMap(item->{
        //			System.out.println(item);
        //			return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue("OK");
        //		});

        return request
            .bodyToMono(HumanOrOrgReq.class)
            .flatMap(
                humanReq -> {
                    var clientErrors = validate(humanReq);
                    if (clientErrors != null) return clientErrors;

                    var serverError = new LinkedHashMap<String, String>();

                    if (humanReq.getId() == null) { // save
                        return save(serverError, humanReq);
                    } else {
                        return update(serverError, humanReq);
                    }
                }
            );
    }

    private class SaveRelation implements SaveOneToOneRelation<HumanOrg> {

        @Override
        public Mono<HumanOrg> saveEntity(Long mainId, Long subId) {
            var humanOrg = new HumanOrg();
            humanOrg.setHumanId(mainId);
            humanOrg.setOrgId(subId);
            return HumanOrOrgREST.this.saveEntity(HumanOrOrgREST.this.humanOrgRepo, humanOrg, HumanOrOrgREST.this.auth);
        }
    }
}
