package uk.gov.hmcts.reform.ccd.documentam.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;
import uk.gov.hmcts.reform.ccd.documentam.controller.endpoints.CaseDocumentAmController;

import java.util.Arrays;

@Configuration
@EnableSwagger2WebMvc
public class SwaggerConfiguration {
    public static final String STRING = "string";
    public static final String HEADER = "header";
    @Value("${swaggerUrl}")
    private  String host;

    @Bean
    public Docket apiV2() {
        return new Docket(DocumentationType.SWAGGER_2)
            .tags(new Tag("get", "Get data endpoints"),
                  new Tag("patch", "Patch data related endpoints"),
                  new Tag("delete", "Delete endpoints"),
                  new Tag("upload", "Upload documents"))
            .select()
            .apis(RequestHandlerSelectors.basePackage(CaseDocumentAmController.class.getPackage().getName()))
            .build()
            .useDefaultResponseMessages(false)
            .apiInfo(apiV2Info())
            .host(host)
            .globalOperationParameters(Arrays.asList(
                headerServiceAuthorization(),
                headerAuthorization(),
                headerUserId(),
                headerUserRoles()
            ));
    }

    private ApiInfo apiV2Info() {
        return new ApiInfoBuilder()
            .title("CCD Case Document AM API")
            .description("download, upload")
            .version("2-beta")
            .build();
    }

    private Parameter headerServiceAuthorization() {
        return new ParameterBuilder()
            .name("ServiceAuthorization")
            .description("Valid Service-to-Service JWT token for a whitelisted micro-service")
            .modelRef(new ModelRef(STRING))
            .parameterType(HEADER)
            .required(true)
            .build();
    }

    private Parameter headerAuthorization() {
        return new ParameterBuilder()
            .name("Authorization")
            .description("Keyword `Bearer` followed by a valid IDAM user token")
            .modelRef(new ModelRef(STRING))
            .parameterType(HEADER)
            .required(true)
            .build();
    }

    private Parameter headerUserId() {
        return new ParameterBuilder()
            .name("user-id")
            .description("User-id of the currently authenticated user. If provided will be used to populate the creator field of a document and"
                             + " will be used for authorisation.")
            .modelRef(new ModelRef(STRING))
            .parameterType(HEADER)
            .required(false)
            .build();
    }

    private Parameter headerUserRoles() {
        return new ParameterBuilder()
            .name("user-roles")
            .description("Comma-separated list of roles of the currently authenticated user. If provided will be used for authorisation.")
            .modelRef(new ModelRef(STRING))
            .parameterType(HEADER)
            .required(false)
            .build();
    }

}
