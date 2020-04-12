package vn.com.sky.sys.role;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import vn.com.sky.base.GenericREST;
import vn.com.sky.util.MyServerResponse;

@Configuration
@AllArgsConstructor
public class RoleREST extends GenericREST {
    private CustomRoleRepo customRepo;

    @Bean
    public RouterFunction<?> roleRoutes() {
        return route(GET(buildURL("role", this::sysGetRoleListByOrgId)), this::sysGetRoleListByOrgId);
    }

    private Mono<ServerResponse> sysGetRoleListByOrgId(ServerRequest request) {
        // SYSTEM BLOCK CODE
        // PLEASE DO NOT EDIT
        if (request == null) {
            String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
            return Mono.just(new MyServerResponse(methodName));
        }
        // END SYSTEM BLOCK CODE

        var orgIdStr = request.queryParam("orgId").orElse(null);

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
            return customRepo.sysGetRoleListByOrgId(orgId, includeDeleted, includeDisabled).flatMap(item -> ok(item)).onErrorResume(e -> error(e));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
